// K6 Performance Test: Baseline Load Test
// Target: >10,000 requests/second sustained throughput
// Duration: 3 minutes with ramp-up and cool-down
//
// Run: k6 run k6/k6-baseline.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
const rateLimitAllowed = new Counter('rate_limit_allowed');
const rateLimitBlocked = new Counter('rate_limit_blocked');
const rateLimitBlockRate = new Rate('rate_limit_block_rate');
const responseTimeTrend = new Trend('response_time_ms');

// Test configuration
export let options = {
    stages: [
        { duration: '30s', target: 500 },    // Ramp up to 500 VUs
        { duration: '1m', target: 1000 },    // Ramp up to 1000 VUs
        { duration: '1m', target: 1000 },    // Sustained load
        { duration: '30s', target: 0 },      // Ramp down
    ],
    thresholds: {
        'http_req_duration': ['p(95)<50', 'p(99)<100'],    // p95 < 50ms, p99 < 100ms
        'http_req_failed': ['rate<0.01'],                   // <1% errors
        'rate_limit_block_rate': ['rate<0.15'],             // <15% block rate (many VUs share limits)
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Simulate different clients
const API_KEYS = [
    'test-key-001', 'test-key-002', 'test-key-003',
    'test-key-004', 'test-key-005', 'test-key-006',
    'test-key-007', 'test-key-008', 'test-key-009',
    'test-key-010'
];

export default function () {
    // Randomly select an API key to simulate different clients
    const apiKey = API_KEYS[Math.floor(Math.random() * API_KEYS.length)];

    const params = {
        headers: {
            'X-API-Key': apiKey,
            'Content-Type': 'application/json',
        },
    };

    // Mix of endpoints
    const endpoints = [
        '/api/test/limited',
        '/api/test/strict',
    ];

    const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
    const res = http.get(`${BASE_URL}${endpoint}`, params);

    // Record custom metrics
    responseTimeTrend.add(res.timings.duration);

    if (res.status === 200) {
        rateLimitAllowed.add(1);
        rateLimitBlockRate.add(false);
    } else if (res.status === 429) {
        rateLimitBlocked.add(1);
        rateLimitBlockRate.add(true);
    }

    // Validate response
    check(res, {
        'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
        'has rate limit headers': (r) => r.headers['X-Ratelimit-Limit'] !== undefined,
        'response time < 100ms': (r) => r.timings.duration < 100,
    });

    sleep(0.01); // 10ms think time
}

export function handleSummary(data) {
    console.log('\n========================================');
    console.log('  AEGIS RATE LIMITER - BASELINE TEST');
    console.log('========================================');
    console.log(`Total Requests:     ${data.metrics.http_reqs.values.count}`);
    console.log(`Throughput:         ${data.metrics.http_reqs.values.rate.toFixed(0)} req/s`);
    console.log(`p95 Latency:        ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`);
    console.log(`p99 Latency:        ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms`);
    console.log(`Allowed Requests:   ${data.metrics.rate_limit_allowed ? data.metrics.rate_limit_allowed.values.count : 0}`);
    console.log(`Blocked Requests:   ${data.metrics.rate_limit_blocked ? data.metrics.rate_limit_blocked.values.count : 0}`);
    console.log(`Error Rate:         ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`);
    console.log('========================================\n');

    return {};
}
