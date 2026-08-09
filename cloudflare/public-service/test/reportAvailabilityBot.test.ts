import { describe, expect, it, vi } from "vitest";
import { OPERATORS, REGIONS } from "../src/domain/catalog";
import { D1ReportAvailabilityRepository } from "../src/repositories/d1ReportAvailabilityRepository";
import { operatorKeyboard, regionKeyboard } from "../src/telegram/keyboards";
import { aboutText } from "../src/telegram/publicBotFormatter";

const sinceMillis = 1_000_000;

describe("D1ReportAvailabilityRepository", () => {
  it("returns only catalog regions that have recent reports", async () => {
    const { db, bind } = fakeDb([{ region_code: "RU-RYA" }, { region_code: "RU-MOW" }]);
    const repository = new D1ReportAvailabilityRepository(db);

    const regions = await repository.listAvailableRegions(sinceMillis);

    expect(bind).toHaveBeenCalledWith(sinceMillis);
    expect(regions.map((region) => region.code)).toEqual(["RU-MOW", "RU-RYA"]);
  });

  it("filters available operators by the selected region", async () => {
    const { db, bind, prepare } = fakeDb([{ operator_code: "MEGAFON" }]);
    const repository = new D1ReportAvailabilityRepository(db);

    const operators = await repository.listAvailableOperators(sinceMillis, "RU-RYA");

    expect(prepare).toHaveBeenCalledWith(expect.stringContaining("AND region_code = ?2"));
    expect(bind).toHaveBeenCalledWith(sinceMillis, "RU-RYA");
    expect(operators.map((operator) => operator.code)).toEqual(["MEGAFON"]);
  });

  it("can list operators globally when a region has not been selected", async () => {
    const { db, bind } = fakeDb([{ operator_code: "MTS" }, { operator_code: "T2" }]);
    const repository = new D1ReportAvailabilityRepository(db);

    const operators = await repository.listAvailableOperators(sinceMillis);

    expect(bind).toHaveBeenCalledWith(sinceMillis);
    expect(operators.map((operator) => operator.code)).toEqual(["MTS", "T2"]);
  });
});

describe("Telegram availability UI", () => {
  it("builds region and operator keyboards from the supplied available items only", () => {
    const regions = REGIONS.filter((region) => region.code === "RU-RYA");
    const operators = OPERATORS.filter((operator) => operator.code === "MEGAFON");

    expect(regionKeyboard(regions).flat().map((button) => button.callback_data)).toEqual([
      "v1:region:RU-RYA",
      "v1:menu",
    ]);
    expect(operatorKeyboard(operators).flat().map((button) => button.callback_data)).toEqual([
      "v1:operator:MEGAFON",
      "v1:menu",
    ]);
  });

  it("keeps only the main menu action when no recent data exists", () => {
    expect(regionKeyboard([])).toEqual([[{ text: "Главное меню", callback_data: "v1:menu" }]]);
    expect(operatorKeyboard([])).toEqual([[{ text: "Главное меню", callback_data: "v1:menu" }]]);
  });

  it("includes the project repository in the about text", () => {
    expect(aboutText()).toContain("https://github.com/Regstar2/white-list-checker");
  });
});

function fakeDb(results: unknown[]): {
  db: D1Database;
  prepare: ReturnType<typeof vi.fn>;
  bind: ReturnType<typeof vi.fn>;
} {
  const all = vi.fn(async () => ({ results }));
  const bind = vi.fn(() => ({ all }));
  const prepare = vi.fn(() => ({ bind }));
  return {
    db: { prepare } as unknown as D1Database,
    prepare,
    bind,
  };
}
