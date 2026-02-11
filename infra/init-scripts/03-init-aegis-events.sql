-- Aegis Module - Rate Limit Events Audit Trail
-- Logs all rate limit decisions (blocked + optionally allowed) for analytics and security

-- Create events table
CREATE TABLE IF NOT EXISTS aegis.rate_limit_events (
    id BIGSERIAL PRIMARY KEY,
    
    -- Timestamp of the event
    event_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Request identification
    client_id VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,          -- Supports IPv4 and IPv6
    endpoint VARCHAR(500) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    
    -- Rate limit decision
    allowed BOOLEAN NOT NULL,
    current_count INTEGER NOT NULL,
    limit_threshold INTEGER NOT NULL,
    rule_source VARCHAR(20) NOT NULL,          -- ANNOTATION, DATABASE, YAML
    
    -- Severity (for blocked requests)
    severity VARCHAR(10),                       -- LOW, MEDIUM, HIGH, CRITICAL
    
    -- Additional context
    user_agent TEXT,
    request_headers JSONB,
    
    -- Performance tracking  
    decision_time_ms DOUBLE PRECISION,
    
    -- Metadata
    server_instance VARCHAR(100) DEFAULT 'aegis-1'
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_events_timestamp ON aegis.rate_limit_events(event_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_events_blocked ON aegis.rate_limit_events(allowed, event_timestamp DESC) WHERE allowed = false;
CREATE INDEX IF NOT EXISTS idx_events_client ON aegis.rate_limit_events(client_id, event_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_events_ip_blocked ON aegis.rate_limit_events(ip_address, event_timestamp DESC) WHERE allowed = false;
CREATE INDEX IF NOT EXISTS idx_events_endpoint ON aegis.rate_limit_events(endpoint, event_timestamp DESC) WHERE allowed = false;
CREATE INDEX IF NOT EXISTS idx_events_severity ON aegis.rate_limit_events(severity, event_timestamp DESC) WHERE severity IS NOT NULL;

-- Function to auto-clean old events (>90 days)
CREATE OR REPLACE FUNCTION aegis.cleanup_old_events()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM aegis.rate_limit_events 
    WHERE event_timestamp < CURRENT_TIMESTAMP - INTERVAL '90 days';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RAISE NOTICE 'Cleaned up % old rate limit events', deleted_count;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Analytics views

-- View: Top blocked IPs in last 24 hours
CREATE OR REPLACE VIEW aegis.v_top_blocked_ips AS
SELECT 
    ip_address,
    client_id,
    COUNT(*) as block_count,
    MAX(event_timestamp) as last_blocked_at,
    array_agg(DISTINCT endpoint) as targeted_endpoints,
    MAX(severity) as max_severity
FROM aegis.rate_limit_events
WHERE allowed = false 
    AND event_timestamp > CURRENT_TIMESTAMP - INTERVAL '24 hours'
GROUP BY ip_address, client_id
ORDER BY block_count DESC
LIMIT 100;

-- View: Hourly traffic summary
CREATE OR REPLACE VIEW aegis.v_hourly_traffic AS
SELECT 
    date_trunc('hour', event_timestamp) as hour,
    COUNT(*) FILTER (WHERE allowed = true) as allowed_count,
    COUNT(*) FILTER (WHERE allowed = false) as blocked_count,
    COUNT(*) as total_count,
    ROUND(100.0 * COUNT(*) FILTER (WHERE allowed = false) / NULLIF(COUNT(*), 0), 2) as block_percentage,
    COUNT(DISTINCT client_id) as unique_clients,
    AVG(decision_time_ms) as avg_decision_ms
FROM aegis.rate_limit_events
WHERE event_timestamp > CURRENT_TIMESTAMP - INTERVAL '7 days'
GROUP BY date_trunc('hour', event_timestamp)
ORDER BY hour DESC;

-- View: Suspicious activity summary
CREATE OR REPLACE VIEW aegis.v_suspicious_activity AS
SELECT
    client_id,
    ip_address,
    severity,
    COUNT(*) as violation_count,
    MIN(event_timestamp) as first_violation,
    MAX(event_timestamp) as last_violation,
    array_agg(DISTINCT endpoint) as targeted_endpoints,
    ROUND(AVG(current_count), 0) as avg_request_count,
    MAX(limit_threshold) as threshold
FROM aegis.rate_limit_events
WHERE allowed = false
    AND severity IN ('HIGH', 'CRITICAL')
    AND event_timestamp > CURRENT_TIMESTAMP - INTERVAL '24 hours'
GROUP BY client_id, ip_address, severity
HAVING COUNT(*) >= 3
ORDER BY violation_count DESC;

-- Grant permissions
GRANT SELECT, INSERT ON aegis.rate_limit_events TO titan_admin;
GRANT USAGE, SELECT ON SEQUENCE aegis.rate_limit_events_id_seq TO titan_admin;
GRANT SELECT ON aegis.v_top_blocked_ips TO titan_admin;
GRANT SELECT ON aegis.v_hourly_traffic TO titan_admin;
GRANT SELECT ON aegis.v_suspicious_activity TO titan_admin;

COMMENT ON TABLE aegis.rate_limit_events IS 'Audit trail for all rate limit decisions - used for analytics and security monitoring';
COMMENT ON VIEW aegis.v_top_blocked_ips IS 'Top blocked IPs in the last 24 hours with targeted endpoints';
COMMENT ON VIEW aegis.v_hourly_traffic IS 'Hourly breakdown of allowed/blocked traffic for the last 7 days';
COMMENT ON VIEW aegis.v_suspicious_activity IS 'Clients with high/critical violations exceeding 3 occurrences in 24 hours';
