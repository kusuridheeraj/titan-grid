-- Aegis Module - Rate Limit Rules Table
-- Stores runtime-configurable rate limit rules (Priority 2 in hybrid system)

CREATE TABLE IF NOT EXISTS aegis.rate_limit_rules (
    id SERIAL PRIMARY KEY,
    
    -- Endpoint pattern (supports wildcards: /api/test/**, /api/admin/*)
    endpoint_pattern VARCHAR(255) NOT NULL,
    
    -- Rate limit configuration
    limit_count INTEGER NOT NULL CHECK (limit_count > 0),
    window_seconds INTEGER NOT NULL CHECK (window_seconds > 0),
    
    -- Client identification type
    client_type VARCHAR(20) NOT NULL DEFAULT 'IP' CHECK (client_type IN ('IP', 'API_KEY', 'USER_ID', 'CUSTOM')),
    custom_key VARCHAR(255),  -- For CUSTOM client type
    
    -- Control flags
    enabled BOOLEAN NOT NULL DEFAULT true,
    priority INTEGER NOT NULL DEFAULT 0,  -- Higher priority rules checked first
    
    -- Metadata
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    
    -- Constraints
    CONSTRAINT unique_endpoint_pattern UNIQUE (endpoint_pattern)
);

-- Indexes for fast lookup
CREATE INDEX idx_rate_limit_rules_enabled ON aegis.rate_limit_rules(enabled) WHERE enabled = true;
CREATE INDEX idx_rate_limit_rules_priority ON aegis.rate_limit_rules(priority DESC, id);
CREATE INDEX idx_rate_limit_rules_pattern ON aegis.rate_limit_rules(endpoint_pattern) WHERE enabled = true;

-- Trigger to auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION aegis.update_rate_limit_rules_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_rate_limit_rules_timestamp
BEFORE UPDATE ON aegis.rate_limit_rules
FOR EACH ROW
EXECUTE FUNCTION aegis.update_rate_limit_rules_timestamp();

-- Insert default rules
INSERT INTO aegis.rate_limit_rules (endpoint_pattern, limit_count, window_seconds, client_type, priority, description, created_by)
VALUES 
    ('/api/admin/**', 10, 60, 'API_KEY', 100, 'Strict limit for admin endpoints', 'SYSTEM'),
    ('/api/test/**', 100, 60, 'IP', 50, 'Standard limit for test endpoints', 'SYSTEM'),
    ('/api/public/**', 1000, 60, 'IP', 10, 'High limit for public endpoints', 'SYSTEM')
ON CONFLICT (endpoint_pattern) DO NOTHING;

-- Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON aegis.rate_limit_rules TO titan_admin;
GRANT USAGE, SELECT ON SEQUENCE aegis.rate_limit_rules_id_seq TO titan_admin;

COMMENT ON TABLE aegis.rate_limit_rules IS 'Runtime-configurable rate limiting rules - Priority 2 in hybrid system (after @RateLimit annotations)';
COMMENT ON COLUMN aegis.rate_limit_rules.endpoint_pattern IS 'Ant-style path pattern: /api/test/** or /api/admin/*';
COMMENT ON COLUMN aegis.rate_limit_rules.priority IS 'Higher numbers = higher priority when multiple patterns match';
COMMENT ON COLUMN aegis.rate_limit_rules.client_type IS 'How to identify clients: IP, API_KEY, USER_ID, or CUSTOM';
