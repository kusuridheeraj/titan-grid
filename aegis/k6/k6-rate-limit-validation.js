// K6 Performance Test: Rate Limit Accuracy Validation
// Sends exactly N+1 requests and validates that exactly 1 is blocked
//
// Run: k6 run k6/k6-rate-limit-validation.js

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const allowedCount = new Counter('validation_allowed');
const blockedCount = new Counter('validation_blocked');

export let options = {
    vus: 1,          // Single client for precise validation
    iterations: 15,  // Send exactly 15 requests (limit is 10 for /api/test/strict)
    thresholds: {
        'validation_blocked': ['count==5'],    // Exactly 5 should be blocked (15-10)
        'validation_allowed': ['count==10'],   // Exactly 10 should be allowed
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    const res = http.get(`${BASE_URL}/api/test/strict`, {
        headers: {
            'X-API-Key': 'validation-test-key',
        },
    });

    if (res.status === 200) {
        allowedCount.add(1);
    } else if (res.status === 429) {
        blockedCount.add(1);
    }

    check(res, {
        'valid status code': (r) => r.status === 200 || r.status === 429,
        'has rate limit headers': (r) => r.headers['X-Ratelimit-Limit'] !== undefined,
        'has remaining header': (r) => r.headers['X-Ratelimit-Remaining'] !== undefined,
        'limit header is 10': (r) => r.headers['X-Ratelimit-Limit'] === '10',
    });

    // Log each request
    const remaining = res.headers['X-Ratelimit-Remaining'] || 'N/A';
    console.log(`Request ${__ITER + 1}: Status=${res.status}, Remaining=${remaining}`);
}

export function handleSummary(data) {
    const allowed = data.metrics.validation_allowed ? data.metrics.validation_allowed.values.count : 0;
    const blocked = data.metrics.validation_blocked ? data.metrics.validation_blocked.values.count : 0;

    console.log('\n========================================');
    console.log('  RATE LIMIT ACCURACY VALIDATION');
    console.log('========================================');
    console.log(`Total Requests:     15`);
    console.log(`Allowed:            ${allowed} (expected: 10)`);
    console.log(`Blocked:            ${blocked} (expected: 5)`);
    console.log(`Accuracy:           ${allowed === 10 && blocked === 5 ? '✅ PERFECT' : '❌ MISMATCH'}`);
    console.log('========================================\n');

    return {};
}
