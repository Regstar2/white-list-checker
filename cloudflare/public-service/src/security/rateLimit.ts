import { ApiError } from "../http/errors";

export interface RateLimitPolicy {
  maxRequests: number;
  windowSeconds: number;
}

export class D1RateLimiter {
  constructor(private readonly db: D1Database) {}

  async check(key: string, policy: RateLimitPolicy, now: number): Promise<void> {
    const windowStart = now - (now % (policy.windowSeconds * 1000));
    const row = await this.db
      .prepare("SELECT window_start, request_count FROM rate_limits WHERE rate_key = ?1")
      .bind(key)
      .first<{ window_start: number; request_count: number }>();

    if (!row || row.window_start !== windowStart) {
      await this.db
        .prepare(
          "INSERT OR REPLACE INTO rate_limits(rate_key, window_start, request_count) VALUES(?1, ?2, 1)",
        )
        .bind(key, windowStart)
        .run();
      return;
    }

    if (row.request_count >= policy.maxRequests) {
      throw new ApiError("RATE_LIMITED", "Слишком много запросов. Попробуйте позже.", 429);
    }

    await this.db
      .prepare("UPDATE rate_limits SET request_count = request_count + 1 WHERE rate_key = ?1")
      .bind(key)
      .run();
  }
}
