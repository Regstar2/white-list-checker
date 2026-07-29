import type { OperatorCode } from "../types";

export interface RegionInfo {
  code: string;
  label: string;
}

export interface OperatorInfo {
  code: OperatorCode;
  label: string;
}

export interface CityInfo {
  code: string;
  regionCode: string;
  label: string;
}

export const REGIONS: RegionInfo[] = [
  { code: "RU-MOW", label: "Москва" },
  { code: "RU-MOS", label: "Московская область" },
  { code: "RU-SPE", label: "Санкт-Петербург" },
  { code: "RU-RYA", label: "Рязанская область" },
  { code: "RU-NIZ", label: "Нижегородская область" },
  { code: "RU-KDA", label: "Краснодарский край" },
  { code: "RU-SVE", label: "Свердловская область" },
  { code: "RU-OTHER", label: "Другой регион" },
  { code: "UNKNOWN", label: "Регион не указан" },
];

export const OPERATORS: OperatorInfo[] = [
  { code: "MEGAFON", label: "МегаФон" },
  { code: "MTS", label: "МТС" },
  { code: "BEELINE", label: "Билайн" },
  { code: "T2", label: "T2" },
  { code: "YOTA", label: "Yota" },
  { code: "ROSTELECOM", label: "Ростелеком" },
  { code: "SBERMOBILE", label: "СберМобайл" },
  { code: "TMOBILE", label: "Т-Мобайл" },
  { code: "GAZPROMBANK_MOBILE", label: "Газпромбанк Мобайл" },
  { code: "OTHER", label: "Другой оператор" },
  { code: "UNKNOWN", label: "Оператор не указан" },
];

export const CITIES: CityInfo[] = [
  { code: "RU-MOW-MOSCOW", regionCode: "RU-MOW", label: "Москва" },
  { code: "RU-SPE-SAINT-PETERSBURG", regionCode: "RU-SPE", label: "Санкт-Петербург" },
  { code: "RU-RYA-RYAZAN", regionCode: "RU-RYA", label: "Рязань" },
  { code: "RU-NIZ-NIZHNY-NOVGOROD", regionCode: "RU-NIZ", label: "Нижний Новгород" },
  { code: "RU-KDA-KRASNODAR", regionCode: "RU-KDA", label: "Краснодар" },
  { code: "RU-SVE-YEKATERINBURG", regionCode: "RU-SVE", label: "Екатеринбург" },
];

export function isKnownRegion(code: string): boolean {
  return REGIONS.some((region) => region.code === code);
}

export function isKnownOperator(code: string): code is OperatorCode {
  return OPERATORS.some((operator) => operator.code === code);
}

export function isKnownCity(regionCode: string, cityCode: string | null | undefined): boolean {
  if (cityCode == null || cityCode === "") return true;
  return CITIES.some((city) => city.regionCode === regionCode && city.code === cityCode);
}

export function regionLabel(code: string | null | undefined): string {
  return REGIONS.find((region) => region.code === code)?.label ?? "Неизвестный регион";
}

export function operatorLabel(code: string | null | undefined): string {
  return OPERATORS.find((operator) => operator.code === code)?.label ?? "Неизвестный оператор";
}
