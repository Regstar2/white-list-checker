ALTER TABLE installations ADD COLUMN city_code TEXT;
ALTER TABLE installations ADD COLUMN custom_city_name TEXT;
ALTER TABLE installations ADD COLUMN area_source TEXT;
ALTER TABLE installations ADD COLUMN operator_source TEXT;

ALTER TABLE reports ADD COLUMN city_code TEXT;
ALTER TABLE reports ADD COLUMN custom_city_name TEXT;
ALTER TABLE reports ADD COLUMN area_source TEXT;
ALTER TABLE reports ADD COLUMN operator_source TEXT;

CREATE INDEX IF NOT EXISTS idx_reports_region_city_operator_checked_at
    ON reports(region_code, city_code, operator_code, checked_at);
