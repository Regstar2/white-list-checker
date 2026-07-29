import type { Env } from "./types";

export interface ServiceConfig {
  reportPrimaryWindowMinutes: number;
  reportFallbackWindowMinutes: number;
  minUniqueInstallations: number;
  minUniqueInstallationsForCity: number;
  consensusNumerator: number;
  consensusDenominator: number;
  deviceOnlineTimeoutSeconds: number;
  commandTtlSeconds: number;
  commandCooldownSeconds: number;
  devicePollIntervalSeconds: number;
  reportRetentionDays: number;
  commandRetentionHours: number;
  linkCodeTtlMinutes: number;
  publicModeEnabled: boolean;
}

export function loadConfig(env: Env): ServiceConfig {
  return {
    reportPrimaryWindowMinutes: readInt(env.REPORT_PRIMARY_WINDOW_MINUTES, 30, 1, 24 * 60),
    reportFallbackWindowMinutes: readInt(env.REPORT_FALLBACK_WINDOW_MINUTES, 120, 1, 7 * 24 * 60),
    minUniqueInstallations: readInt(env.MIN_UNIQUE_INSTALLATIONS, 3, 1, 1000),
    minUniqueInstallationsForCity: readInt(env.MIN_UNIQUE_INSTALLATIONS_FOR_CITY, 5, 1, 1000),
    consensusNumerator: readInt(env.CONSENSUS_NUMERATOR, 2, 1, 100),
    consensusDenominator: readInt(env.CONSENSUS_DENOMINATOR, 3, 1, 100),
    deviceOnlineTimeoutSeconds: readInt(env.DEVICE_ONLINE_TIMEOUT_SECONDS, 45, 5, 3600),
    commandTtlSeconds: readInt(env.COMMAND_TTL_SECONDS, 120, 10, 3600),
    commandCooldownSeconds: readInt(env.COMMAND_COOLDOWN_SECONDS, 60, 0, 3600),
    devicePollIntervalSeconds: readInt(env.DEVICE_POLL_INTERVAL_SECONDS, 15, 5, 300),
    reportRetentionDays: readInt(env.REPORT_RETENTION_DAYS, 7, 1, 365),
    commandRetentionHours: readInt(env.COMMAND_RETENTION_HOURS, 24, 1, 24 * 30),
    linkCodeTtlMinutes: readInt(env.LINK_CODE_TTL_MINUTES, 10, 1, 60),
    publicModeEnabled: env.PUBLIC_MODE_ENABLED !== "false",
  };
}

function readInt(value: string | undefined, fallback: number, min: number, max: number): number {
  const parsed = Number(value ?? fallback);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.max(min, Math.min(max, Math.trunc(parsed)));
}
