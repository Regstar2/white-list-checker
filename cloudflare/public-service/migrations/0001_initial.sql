CREATE TABLE IF NOT EXISTS installations (
    installation_id TEXT PRIMARY KEY,
    device_token_hash TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    last_seen_at INTEGER,
    revoked_at INTEGER,
    app_version TEXT,
    schema_version INTEGER NOT NULL,
    region_code TEXT,
    operator_code TEXT,
    device_alias TEXT,
    share_reports INTEGER NOT NULL DEFAULT 0,
    allow_remote_checks INTEGER NOT NULL DEFAULT 0,
    last_active_session_id TEXT,
    last_service_started_at INTEGER,
    last_service_state TEXT
);

CREATE TABLE IF NOT EXISTS telegram_users (
    chat_id TEXT PRIMARY KEY,
    telegram_user_id TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    selected_region_code TEXT,
    selected_operator_code TEXT,
    language_code TEXT,
    is_blocked INTEGER NOT NULL DEFAULT 0,
    last_interaction_at INTEGER
);

CREATE TABLE IF NOT EXISTS installation_links (
    link_id TEXT PRIMARY KEY,
    installation_id TEXT NOT NULL,
    chat_id TEXT NOT NULL,
    device_alias TEXT,
    created_at INTEGER NOT NULL,
    revoked_at INTEGER,
    created_by_link_code_id TEXT,
    FOREIGN KEY(installation_id) REFERENCES installations(installation_id),
    FOREIGN KEY(chat_id) REFERENCES telegram_users(chat_id)
);

CREATE TABLE IF NOT EXISTS link_codes (
    link_code_id TEXT PRIMARY KEY,
    link_code_hash TEXT NOT NULL,
    installation_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    used_at INTEGER,
    used_by_chat_id TEXT,
    FOREIGN KEY(installation_id) REFERENCES installations(installation_id)
);

CREATE TABLE IF NOT EXISTS reports (
    report_id TEXT PRIMARY KEY,
    installation_id TEXT NOT NULL,
    schema_version INTEGER NOT NULL,
    app_version TEXT NOT NULL,
    checked_at INTEGER NOT NULL,
    received_at INTEGER NOT NULL,
    trigger_type TEXT NOT NULL,
    region_code TEXT NOT NULL,
    operator_code TEXT NOT NULL,
    whitelist_state TEXT NOT NULL,
    foreign_available INTEGER NOT NULL,
    foreign_total INTEGER NOT NULL,
    local_available INTEGER NOT NULL,
    local_total INTEGER NOT NULL,
    result_quality TEXT NOT NULL,
    is_conclusive INTEGER NOT NULL,
    source_command_id TEXT,
    FOREIGN KEY(installation_id) REFERENCES installations(installation_id)
);

CREATE TABLE IF NOT EXISTS report_targets (
    report_id TEXT NOT NULL,
    target_code TEXT NOT NULL,
    target_group TEXT NOT NULL,
    target_status TEXT NOT NULL,
    latency_bucket TEXT,
    PRIMARY KEY(report_id, target_code),
    FOREIGN KEY(report_id) REFERENCES reports(report_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS commands (
    command_id TEXT PRIMARY KEY,
    installation_id TEXT NOT NULL,
    requested_by_chat_id TEXT NOT NULL,
    command_type TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    claimed_at INTEGER,
    completed_at INTEGER,
    service_session_id TEXT,
    telegram_chat_id TEXT NOT NULL,
    telegram_message_id TEXT,
    result_status TEXT,
    error_code TEXT,
    telegram_result_sent_at INTEGER,
    FOREIGN KEY(installation_id) REFERENCES installations(installation_id)
);

CREATE TABLE IF NOT EXISTS command_results (
    command_id TEXT PRIMARY KEY,
    received_at INTEGER NOT NULL,
    outcome TEXT NOT NULL,
    whitelist_state TEXT,
    foreign_available INTEGER,
    foreign_total INTEGER,
    local_available INTEGER,
    local_total INTEGER,
    error_code TEXT,
    checked_at INTEGER,
    FOREIGN KEY(command_id) REFERENCES commands(command_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS telegram_preferences (
    chat_id TEXT PRIMARY KEY,
    selected_region_code TEXT,
    selected_operator_code TEXT,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS processed_telegram_updates (
    update_id INTEGER PRIMARY KEY,
    processed_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS feedback (
    feedback_id TEXT PRIMARY KEY,
    chat_id TEXT NOT NULL,
    telegram_user_id TEXT,
    message TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    command_context TEXT
);

CREATE TABLE IF NOT EXISTS rate_limits (
    rate_key TEXT PRIMARY KEY,
    window_start INTEGER NOT NULL,
    request_count INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reports_region_operator_checked_at ON reports(region_code, operator_code, checked_at);
CREATE INDEX IF NOT EXISTS idx_reports_installation_checked_at ON reports(installation_id, checked_at);
CREATE INDEX IF NOT EXISTS idx_reports_received_at ON reports(received_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_reports_report_id ON reports(report_id);

CREATE INDEX IF NOT EXISTS idx_installations_last_seen_at ON installations(last_seen_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_installations_device_token_hash ON installations(device_token_hash);

CREATE INDEX IF NOT EXISTS idx_installation_links_chat_revoked ON installation_links(chat_id, revoked_at);
CREATE INDEX IF NOT EXISTS idx_installation_links_installation_revoked ON installation_links(installation_id, revoked_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_installation_links_active_pair
    ON installation_links(installation_id, chat_id)
    WHERE revoked_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_link_codes_hash ON link_codes(link_code_hash);
CREATE INDEX IF NOT EXISTS idx_link_codes_expires_at ON link_codes(expires_at);

CREATE INDEX IF NOT EXISTS idx_commands_installation_status_expires ON commands(installation_id, status, expires_at);
CREATE INDEX IF NOT EXISTS idx_commands_chat_created ON commands(requested_by_chat_id, created_at);
CREATE INDEX IF NOT EXISTS idx_commands_expires ON commands(expires_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_processed_telegram_updates_update_id ON processed_telegram_updates(update_id);
