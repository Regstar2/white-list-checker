import { OPERATORS, REGIONS, type OperatorInfo, type RegionInfo } from "../domain/catalog";

interface RegionAvailabilityRow {
  region_code: string;
}

interface OperatorAvailabilityRow {
  operator_code: string;
}

export class D1ReportAvailabilityRepository {
  constructor(private readonly db: D1Database) {}

  async listAvailableRegions(sinceMillis: number): Promise<RegionInfo[]> {
    const rows = await this.db
      .prepare(
        `
        SELECT DISTINCT region_code
        FROM reports
        WHERE checked_at >= ?1
        `,
      )
      .bind(sinceMillis)
      .all<RegionAvailabilityRow>();
    const availableCodes = new Set(rows.results.map((row) => row.region_code));
    return REGIONS.filter((region) => availableCodes.has(region.code));
  }

  async listAvailableOperators(sinceMillis: number, regionCode?: string | null): Promise<OperatorInfo[]> {
    const regionFilter = regionCode ? "AND region_code = ?2" : "";
    const rows = await this.db
      .prepare(
        `
        SELECT DISTINCT operator_code
        FROM reports
        WHERE checked_at >= ?1
          ${regionFilter}
        `,
      )
      .bind(...(regionCode ? [sinceMillis, regionCode] : [sinceMillis]))
      .all<OperatorAvailabilityRow>();
    const availableCodes = new Set(rows.results.map((row) => row.operator_code));
    return OPERATORS.filter((operator) => availableCodes.has(operator.code));
  }
}
