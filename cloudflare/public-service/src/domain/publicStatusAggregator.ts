import type { ReportSample, WhitelistReportState } from "../types";

export interface AggregationPolicy {
  primaryWindowMinutes: number;
  fallbackWindowMinutes: number;
  minUniqueInstallations: number;
  consensusNumerator: number;
  consensusDenominator: number;
}

export type PublicStatusKind =
  | "FRESH_CONSENSUS"
  | "FALLBACK_CONSENSUS"
  | "MIXED"
  | "INSUFFICIENT_SAMPLE"
  | "NO_DATA";

export interface PublicStatusResult {
  kind: PublicStatusKind;
  regionCode: string;
  operatorCode: string;
  windowMinutes: number;
  stale: boolean;
  sampleSize: number;
  enabledCount: number;
  disabledCount: number;
  inconclusiveCount: number;
  latestCheckedAt: number | null;
  consensusState: "LIKELY_ENABLED" | "LIKELY_DISABLED" | null;
}

export class PublicStatusAggregator {
  aggregate(params: {
    regionCode: string;
    operatorCode: string;
    currentTime: number;
    primarySamples: ReportSample[];
    fallbackSamples: ReportSample[];
    policy: AggregationPolicy;
  }): PublicStatusResult {
    const primary = this.evaluateWindow(
      params.regionCode,
      params.operatorCode,
      params.policy.primaryWindowMinutes,
      false,
      params.primarySamples,
      params.policy,
    );
    if (primary.kind !== "INSUFFICIENT_SAMPLE" && primary.kind !== "NO_DATA") {
      return primary;
    }
    const fallback = this.evaluateWindow(
      params.regionCode,
      params.operatorCode,
      params.policy.fallbackWindowMinutes,
      true,
      params.fallbackSamples,
      params.policy,
    );
    if (fallback.kind === "FRESH_CONSENSUS") {
      return { ...fallback, kind: "FALLBACK_CONSENSUS", stale: true };
    }
    if (fallback.kind === "MIXED") {
      return { ...fallback, stale: true };
    }
    if (fallback.sampleSize > 0) {
      return { ...fallback, kind: "INSUFFICIENT_SAMPLE", stale: true };
    }
    return primary.sampleSize > 0 ? primary : fallback;
  }

  private evaluateWindow(
    regionCode: string,
    operatorCode: string,
    windowMinutes: number,
    stale: boolean,
    samples: ReportSample[],
    policy: AggregationPolicy,
  ): PublicStatusResult {
    const unique = latestByInstallation(samples);
    const counts = countStates(unique);
    const sampleSize = unique.length;
    const latestCheckedAt = unique.reduce<number | null>(
      (latest, sample) => (latest == null ? sample.checkedAt : Math.max(latest, sample.checkedAt)),
      null,
    );
    const base = {
      regionCode,
      operatorCode,
      windowMinutes,
      stale,
      sampleSize,
      enabledCount: counts.enabled,
      disabledCount: counts.disabled,
      inconclusiveCount: counts.inconclusive,
      latestCheckedAt,
      consensusState: null,
    } satisfies Omit<PublicStatusResult, "kind">;

    if (sampleSize === 0) return { ...base, kind: "NO_DATA" };
    if (sampleSize < policy.minUniqueInstallations) return { ...base, kind: "INSUFFICIENT_SAMPLE" };

    const denominator = counts.enabled + counts.disabled;
    if (denominator === 0) return { ...base, kind: "MIXED" };

    if (hasConsensus(counts.enabled, denominator, policy)) {
      return { ...base, kind: "FRESH_CONSENSUS", consensusState: "LIKELY_ENABLED" };
    }
    if (hasConsensus(counts.disabled, denominator, policy)) {
      return { ...base, kind: "FRESH_CONSENSUS", consensusState: "LIKELY_DISABLED" };
    }
    return { ...base, kind: "MIXED" };
  }
}

export function latestByInstallation(samples: ReportSample[]): ReportSample[] {
  const byInstallation = new Map<string, ReportSample>();
  for (const sample of samples) {
    const previous = byInstallation.get(sample.installationId);
    if (!previous || sample.checkedAt > previous.checkedAt) {
      byInstallation.set(sample.installationId, sample);
    }
  }
  return [...byInstallation.values()];
}

function countStates(samples: ReportSample[]): { enabled: number; disabled: number; inconclusive: number } {
  return samples.reduce(
    (acc, sample) => {
      const vote = voteState(sample.whitelistState);
      if (vote === "LIKELY_ENABLED") acc.enabled += 1;
      else if (vote === "LIKELY_DISABLED") acc.disabled += 1;
      else acc.inconclusive += 1;
      return acc;
    },
    { enabled: 0, disabled: 0, inconclusive: 0 },
  );
}

export function voteState(state: WhitelistReportState): "LIKELY_ENABLED" | "LIKELY_DISABLED" | null {
  if (state === "LIKELY_ENABLED") return "LIKELY_ENABLED";
  if (state === "LIKELY_DISABLED") return "LIKELY_DISABLED";
  return null;
}

function hasConsensus(count: number, denominator: number, policy: AggregationPolicy): boolean {
  return count * policy.consensusDenominator >= denominator * policy.consensusNumerator;
}
