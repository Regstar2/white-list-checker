import { ApiError } from "./errors";

const MAX_JSON_BYTES = 64 * 1024;

export async function readJsonBody<T>(request: Request, maxBytes = MAX_JSON_BYTES): Promise<T> {
  const text = await request.text();
  if (text.length > maxBytes) {
    throw new ApiError("PAYLOAD_TOO_LARGE", "Request body is too large", 413);
  }
  try {
    return JSON.parse(text) as T;
  } catch {
    throw new ApiError("INVALID_JSON", "Request body must be valid JSON", 400);
  }
}

export function requireSchemaVersion(value: unknown): asserts value is { schemaVersion: number; requestId: string } {
  if (!value || typeof value !== "object") {
    throw new ApiError("INVALID_REQUEST", "Request body is required", 400);
  }
  const body = value as { schemaVersion?: unknown; requestId?: unknown };
  if (body.schemaVersion !== 1) {
    throw new ApiError("UNSUPPORTED_SCHEMA", "Unsupported schema version", 400);
  }
  if (typeof body.requestId !== "string" || body.requestId.length < 8 || body.requestId.length > 80) {
    throw new ApiError("INVALID_REQUEST_ID", "requestId is invalid", 400);
  }
}
