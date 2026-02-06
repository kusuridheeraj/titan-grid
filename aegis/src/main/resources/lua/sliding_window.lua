-- Sliding Window Rate Limiter Algorithm
-- Atomically checks and updates rate limit in Redis using sorted sets
--
-- KEYS[1] = rate limit key (e.g., "rate_limit:192.168.1.1:/api/test")
-- ARGV[1] = current timestamp (seconds)
-- ARGV[2] = window duration (seconds)
-- ARGV[3] = max requests allowed
-- ARGV[4] = unique request ID (UUID)
--
-- Returns: {allowed (1/0), current_count, ttl_seconds}

local key = KEYS[1]
local current_time = tonumber(ARGV[1])
local window_duration = tonumber(ARGV[2])
local max_requests = tonumber(ARGV[3])
local request_id = ARGV[4]

-- Calculate window start time
local window_start = current_time - window_duration

-- Remove expired entries (outside current window)
redis.call('ZREMRANGEBYSCORE', key, 0, window_start)

-- Count current requests in window
local current_count = redis.call('ZCARD', key)

-- Determine if request is allowed
local allowed = 0
if current_count < max_requests then
    -- Add new request to sorted set (score = timestamp, member = request_id)
    redis.call('ZADD', key, current_time, request_id)
    current_count = current_count + 1
    allowed = 1
    
    -- Set expiration on key (window_duration + 1 for safety)
    redis.call('EXPIRE', key, window_duration + 1)
end

-- Calculate TTL (time until window resets)
local ttl = redis.call('TTL', key)
if ttl < 0 then
    ttl = window_duration
end

-- Return: {allowed, current_count, ttl}
return {allowed, current_count, ttl}
