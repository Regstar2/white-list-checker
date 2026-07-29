export interface Env {
  DB: D1Database;
  PUBLIC_BOT_TOKEN?: string;
  TELEGRAM_WEBHOOK_SECRET?: string;
  DEVICE_TOKEN_PEPPER?: string;
  ADMIN_API_SECRET?: string;
  REPORT_PRIMARY_WINDOW_MINUTES?: string;
  REPORT_FALLBACK_WINDOW_MINUTES?: string;
  MIN_UNIQUE_INSTALLATIONS?: string;
  CONSENSUS_NUMERATOR?: string;
  CONSENSUS_DENOMINATOR?: string;
  DEVICE_ONLINE_TIMEOUT_SECONDS?: string;
  COMMAND_TTL_SECONDS?: string;
  COMMAND_COOLDOWN_SECONDS?: string;
  DEVICE_POLL_INTERVAL_SECONDS?: string;
  REPORT_RETENTION_DAYS?: string;
  COMMAND_RETENTION_HOURS?: string;
  LINK_CODE_TTL_MINUTES?: string;
  MIN_UNIQUE_INSTALLATIONS_FOR_CITY?: string;
  PUBLIC_MODE_ENABLED?: string;
}

export type Platform = "ANDROID";

export type OperatorCode =
  | "MEGAFON"
  | "MTS"
  | "BEELINE"
  | "T2"
  | "YOTA"
  | "ROSTELECOM"
  | "SBERMOBILE"
  | "TMOBILE"
  | "GAZPROMBANK_MOBILE"
  | "OTHER"
  | "UNKNOWN";

export type AreaSource = "AUTOMATIC_LOCATION" | "MANUAL_SELECTION";

export type OperatorSource = "NETWORK_OPERATOR" | "SIM_OPERATOR" | "MANUAL" | "UNKNOWN";

export type WhitelistReportState =
  | "LIKELY_ENABLED"
  | "LIKELY_DISABLED"
  | "INCONCLUSIVE"
  | "NO_MOBILE_INTERNET"
  | "MOBILE_DNS_FAILURE"
  | "PARTIAL_PROBLEM"
  | "CELLULAR_NETWORK_UNAVAILABLE";

export type ResultQuality = "CONCLUSIVE" | "PARTIAL" | "INCONCLUSIVE";

export type CommandStatus = "PENDING" | "CLAIMED" | "COMPLETED" | "FAILED" | "EXPIRED" | "CANCELLED";

export type CommandType = "CHECK_NOW";

export type CommandResultOutcome = "SUCCESS" | "UNAVAILABLE" | "FAILED" | "BUSY" | "EXPIRED" | "UNAUTHORIZED";

export interface ApiErrorBody {
  error: {
    code: string;
    message: string;
  };
}

export interface JsonRequestBase {
  schemaVersion: number;
  requestId: string;
}

export interface RegisterInstallationRequest extends JsonRequestBase {
  platform: Platform;
  appVersion: string;
}

export interface InstallationSettingsRequest extends JsonRequestBase {
  shareReports: boolean;
  allowRemoteChecks: boolean;
  regionCode: string;
  cityCode?: string | null;
  customCityName?: string | null;
  operatorCode: OperatorCode;
  areaSource?: AreaSource;
  operatorSource?: OperatorSource;
  deviceAlias?: string;
}

export interface PublicReportRequest extends JsonRequestBase {
  reportId: string;
  checkedAt: number;
  triggerType: string;
  appVersion: string;
  regionCode: string;
  cityCode?: string | null;
  customCityName?: string | null;
  operatorCode: OperatorCode;
  areaSource?: AreaSource;
  operatorSource?: OperatorSource;
  whitelistState: WhitelistReportState;
  resultQuality: ResultQuality;
  foreign: CountPair;
  local: CountPair;
  targets?: ReportTargetRequest[];
  sourceCommandId?: string;
}

export interface CountPair {
  available: number;
  total: number;
}

export interface ReportTargetRequest {
  targetCode: string;
  targetGroup: "FOREIGN" | "LOCAL";
  targetStatus: "AVAILABLE" | "UNAVAILABLE" | "UNKNOWN";
  latencyBucket?: string;
}

export interface ServiceSyncRequest extends JsonRequestBase {
  serviceSessionId: string;
  serviceStartedAt: number;
  serviceState: string;
  checkInProgress: boolean;
  appVersion: string;
}

export interface CommandResultRequest extends JsonRequestBase {
  serviceSessionId: string;
  outcome: CommandResultOutcome;
  checkedAt: number;
  whitelistState?: WhitelistReportState;
  foreign?: CountPair;
  local?: CountPair;
  errorCode?: string;
}

export interface InstallationRecord {
  installationId: string;
  shareReports: boolean;
  allowRemoteChecks: boolean;
  revokedAt: number | null;
  regionCode: string | null;
  cityCode: string | null;
  customCityName: string | null;
  operatorCode: OperatorCode | null;
  areaSource: AreaSource | null;
  operatorSource: OperatorSource | null;
  appVersion: string | null;
  deviceAlias: string | null;
}

export interface LinkedDeviceRecord {
  linkId: string;
  installationId: string;
  deviceAlias: string;
  lastSeenAt: number | null;
  allowRemoteChecks: boolean;
  lastServiceState: string | null;
}

export interface ReportSample {
  installationId: string;
  checkedAt: number;
  whitelistState: WhitelistReportState;
  cityCode: string | null;
  isConclusive: boolean;
  foreignAvailable: number;
  foreignTotal: number;
  localAvailable: number;
  localTotal: number;
}

export interface ClaimedCommand {
  commandId: string;
  type: CommandType;
  expiresAt: number;
}

export interface CommandRecord {
  commandId: string;
  installationId: string;
  requestedByChatId: string;
  telegramChatId: string;
  telegramMessageId: string | null;
  status: CommandStatus;
  expiresAt: number;
  serviceSessionId: string | null;
  telegramResultSentAt: number | null;
}

export interface TelegramMessage {
  message_id: number;
  chat: {
    id: number | string;
    type?: string;
  };
  from?: {
    id?: number;
    language_code?: string;
  };
  text?: string;
}

export interface TelegramCallbackQuery {
  id: string;
  from: {
    id?: number;
    language_code?: string;
  };
  message?: TelegramMessage;
  data?: string;
}

export interface TelegramUpdate {
  update_id: number;
  message?: TelegramMessage;
  callback_query?: TelegramCallbackQuery;
}
