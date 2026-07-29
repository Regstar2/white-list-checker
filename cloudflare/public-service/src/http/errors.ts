import type { ApiErrorBody } from "../types";

export class ApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly status = 400,
  ) {
    super(message);
  }
}

export function errorResponse(error: ApiError): Response {
  const body: ApiErrorBody = {
    error: {
      code: error.code,
      message: error.message,
    },
  };
  return jsonResponse(body, error.status);
}

export function jsonResponse(
  body: unknown,
  statusOrInit: number | ResponseInit = 200,
): Response {
  const init = typeof statusOrInit === "number" ? { status: statusOrInit } : statusOrInit;
  const headers = new Headers(init.headers);
  headers.set("content-type", "application/json; charset=utf-8");
  if (!headers.has("cache-control")) headers.set("cache-control", "no-store");
  return new Response(JSON.stringify(body), {
    ...init,
    headers,
  });
}

export function methodNotAllowed(): Response {
  return errorResponse(new ApiError("METHOD_NOT_ALLOWED", "Method not allowed", 405));
}

export function notFound(): Response {
  return errorResponse(new ApiError("NOT_FOUND", "Endpoint not found", 404));
}
