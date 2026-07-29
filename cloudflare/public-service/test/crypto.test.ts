import { generateLinkCode, normalizeLinkCode } from "../src/security/crypto";

describe("link codes", () => {
  it("generates user-facing one-time code shape", () => {
    expect(generateLinkCode()).toMatch(/^[A-Z2-9]{4}-[A-Z2-9]{4}$/);
  });

  it("normalizes pasted link codes", () => {
    expect(normalizeLinkCode("abcd efgh")).toBe("ABCD-EFGH");
    expect(normalizeLinkCode("ABCD-EFGH")).toBe("ABCD-EFGH");
    expect(normalizeLinkCode("bad")).toBe("");
  });
});
