import { describe, expect, it } from "vitest";
import { handleApiRequest } from "../src/http/apiRoutes";
import type { Env } from "../src/types";

describe("service sync route", () => {
  it("is registered instead of falling through to 404", async () => {
    const response = await handleApiRequest(
      new Request("https://service.test/api/v1/installations/me/service-sync", {
        method: "GET",
      }),
      {} as Env,
    );

    expect(response.status).toBe(405);
    expect(await response.json()).toEqual({
      error: {
        code: "METHOD_NOT_ALLOWED",
        message: "Method not allowed",
      },
    });
  });
});
