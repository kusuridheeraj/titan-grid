-- Titan Grid Database Initialization Script
-- This script runs automatically when PostgreSQL container starts for the first time

-- Create schemas for different modules
CREATE SCHEMA IF NOT EXISTS aegis;
CREATE SCHEMA IF NOT EXISTS cryptex;
CREATE SCHEMA IF NOT EXISTS nexus;

-- Note: aegis.rate_limit_events is created by 03-init-aegis-events.sql with full schema

CREATE TABLE IF NOT EXISTS aegis.blacklist (
    id SERIAL PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL UNIQUE,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

-- Cryptex: File Metadata Tables
CREATE TABLE IF NOT EXISTS cryptex.file_metadata (
    id SERIAL PRIMARY KEY,
    file_id UUID NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    encrypted_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    s3_bucket VARCHAR(100),
    s3_key VARCHAR(500),
    encryption_key_id VARCHAR(255),
    uploaded_by VARCHAR(255),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_file_id ON cryptex.file_metadata(file_id);
CREATE INDEX IF NOT EXISTS idx_uploaded_by ON cryptex.file_metadata(uploaded_by);

-- Nexus: AI Agent Logs
CREATE TABLE IF NOT EXISTS nexus.agent_activity (
    id SERIAL PRIMARY KEY,
    action_type VARCHAR(50) NOT NULL,
    tool_name VARCHAR(100),
    input_data JSONB,
    output_data JSONB,
    success BOOLEAN DEFAULT TRUE,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_action_type ON nexus.agent_activity(action_type);
CREATE INDEX IF NOT EXISTS idx_executed_at ON nexus.agent_activity(executed_at);

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA aegis TO titan_admin;
GRANT ALL PRIVILEGES ON SCHEMA cryptex TO titan_admin;
GRANT ALL PRIVILEGES ON SCHEMA nexus TO titan_admin;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA aegis TO titan_admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA cryptex TO titan_admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA nexus TO titan_admin;

-- Success confirmation
DO $$
BEGIN
    RAISE NOTICE 'Titan Grid database initialized successfully!';
    RAISE NOTICE 'Schemas created: aegis, cryptex, nexus';
END $$;
