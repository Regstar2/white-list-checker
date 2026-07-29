import { PublicStatusAggregator, latestByInstallation, voteState } from "../src/domain/publicStatusAggregator";
import type { AggregationPolicy } from "../src/domain/publicStatusAggregator";
import type { ReportSample } from "../src/types";

const policy: AggregationPolicy = {
  primaryWindowMinutes: 30,
  fallbackWindowMinutes: 120,
  minUniqueInstallations: 3,
  consensusNumerator: 2,
  consensusDenominator: 3,
};

describe("PublicStatusAggregator", () => {
  it("returns no data for an empty window", () => {
    const result = aggregate([]);
    expect(result.kind).toBe("NO_DATA");
    expect(result.sampleSize).toBe(0);
  });

  it("requires the minimum number of independent installations", () => {
    const result = aggregate([
      sample("a", "LIKELY_ENABLED", 1000),
      sample("b", "LIKELY_ENABLED", 1000),
    ]);
    expect(result.kind).toBe("INSUFFICIENT_SAMPLE");
  });

  it("detects enabled consensus", () => {
    const result = aggregate([
      sample("a", "LIKELY_ENABLED", 1000),
      sample("b", "LIKELY_ENABLED", 1000),
      sample("c", "LIKELY_DISABLED", 1000),
    ]);
    expect(result.kind).toBe("FRESH_CONSENSUS");
    expect(result.consensusState).toBe("LIKELY_ENABLED");
  });

  it("detects disabled consensus", () => {
    const result = aggregate([
      sample("a", "LIKELY_DISABLED", 1000),
      sample("b", "LIKELY_DISABLED", 1000),
      sample("c", "LIKELY_ENABLED", 1000),
    ]);
    expect(result.kind).toBe("FRESH_CONSENSUS");
    expect(result.consensusState).toBe("LIKELY_DISABLED");
  });

  it("returns mixed when conclusive votes do not agree", () => {
    const result = aggregate([
      sample("a", "LIKELY_DISABLED", 1000),
      sample("b", "LIKELY_ENABLED", 1000),
      sample("c", "PARTIAL_PROBLEM", 1000),
    ]);
    expect(result.kind).toBe("MIXED");
    expect(result.inconclusiveCount).toBe(1);
  });

  it("does not vote inconclusive states as enabled", () => {
    expect(voteState("MOBILE_DNS_FAILURE")).toBeNull();
    expect(voteState("CELLULAR_NETWORK_UNAVAILABLE")).toBeNull();
    expect(voteState("PARTIAL_PROBLEM")).toBeNull();
  });

  it("uses one latest report per installation", () => {
    const latest = latestByInstallation([
      sample("a", "LIKELY_ENABLED", 1000),
      sample("a", "LIKELY_DISABLED", 2000),
      sample("b", "LIKELY_ENABLED", 1000),
    ]);
    expect(latest).toHaveLength(2);
    expect(latest.find((item) => item.installationId === "a")?.whitelistState).toBe("LIKELY_DISABLED");
  });

  it("falls back to the wider window and marks stale consensus", () => {
    const result = new PublicStatusAggregator().aggregate({
      regionCode: "RU-RYA",
      operatorCode: "MEGAFON",
      currentTime: 10_000,
      primarySamples: [],
      fallbackSamples: [
        sample("a", "LIKELY_ENABLED", 1000),
        sample("b", "LIKELY_ENABLED", 1000),
        sample("c", "LIKELY_DISABLED", 1000),
      ],
      policy,
    });
    expect(result.kind).toBe("FALLBACK_CONSENSUS");
    expect(result.stale).toBe(true);
  });

  it("twelve reports from one installation are one vote", () => {
    const reports = Array.from({ length: 12 }, (_, index) => sample("a", "LIKELY_ENABLED", index));
    const result = aggregate([
      ...reports,
      sample("b", "LIKELY_ENABLED", 1000),
      sample("c", "LIKELY_DISABLED", 1000),
    ]);
    expect(result.sampleSize).toBe(3);
    expect(result.enabledCount).toBe(2);
  });
});

function aggregate(primarySamples: ReportSample[]) {
  return new PublicStatusAggregator().aggregate({
    regionCode: "RU-RYA",
    operatorCode: "MEGAFON",
    currentTime: 10_000,
    primarySamples,
    fallbackSamples: primarySamples,
    policy,
  });
}

function sample(installationId: string, whitelistState: ReportSample["whitelistState"], checkedAt: number): ReportSample {
  return {
    installationId,
    checkedAt,
    cityCode: null,
    whitelistState,
    isConclusive: whitelistState === "LIKELY_ENABLED" || whitelistState === "LIKELY_DISABLED",
    foreignAvailable: whitelistState === "LIKELY_ENABLED" ? 0 : 8,
    foreignTotal: 8,
    localAvailable: 7,
    localTotal: 8,
  };
}
