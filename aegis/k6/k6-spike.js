// K6 Performance Test: Spike Test
// Simulates sudden traffic surge to test rate limiter under extreme load
//
// Run: k6 run k6/k6-spike.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const rateLimitBlocked = new Counter('spike_blocked');
const responseTimeTrend = new Trend('spike_response_ms');

export let options = {
    stages: [
        { duration: '10s', target: 100 },     // Normal traffic
        { duration: '5s', target: 5000 },      // SPIKE!
        { duration: '30s', target: 5000 },     // Sustained spike
        { duration: '10s', target: 100 },      // Recovery
        { duration: '10s', target: 0 },        // Cool down
    ],
    thresholds: {
        'http_req_duration': ['p(95)<200'],     // p95 < 200ms even under spike
        'http_req_failed': ['rate<0.05'],        // <5% errors under spike
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    const clientId = `spike-client-${__VU % 50}`;  // 50 unique clients

    const res = http.get(`${BASE_URL}/api/test/limited`, {
        headers: { 'X-API-Key': clientId },
    });

    responseTimeTrend.add(res.timings.duration);

    if (res.status === 429) {
        rateLimitBlocked.add(1);
    }

    check(res, {
        'response received': (r) => r.status === 200 || r.status === 429,
        'no server errors': (r) => r.status !== 500,
        'latency acceptable': (r) => r.timings.duration < 500,
    });

    sleep(0.005);
}

export function handleSummary(data) {
    console.log('\n========================================');
    console.log('  AEGIS RATE LIMITER - SPIKE TEST');
    console.log('========================================');
    console.log(`Total Requests:     ${data.metrics.http_reqs.values.count}`);
    console.log(`Peak Throughput:    ${data.metrics.http_reqs.values.rate.toFixed(0)} req/s`);
    console.log(`p95 Latency:        ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`);
    console.log(`p99 Latency:        ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms`);
    console.log(`Blocked (429):      ${data.metrics.spike_blocked ? data.metrics.spike_blocked.values.count : 0}`);
    console.log(`Server Errors:      ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`);
    console.log('========================================\n');

    return {};
}
