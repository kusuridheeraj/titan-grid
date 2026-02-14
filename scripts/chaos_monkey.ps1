# 🐒 Titan Grid Chaos Monkey (Redis Failover Test)
# This script proves the High Availability (HA) of Aegis by killing the Redis Master.

$REDIS_PASSWORD = "titan_redis_secure_2026"

Write-Host "🚀 Starting Chaos Monkey: Redis Sentinel Failover Test" -ForegroundColor Cyan

# 1. Identify current master
Write-Host "🔍 Identifying current Redis Master..."
$master_info = docker exec titan-sentinel-1 redis-cli -p 26379 sentinel get-master-addr-by-name mymaster
$master_ip = $master_info[0]
Write-Host "Current Master IP: $master_ip" -ForegroundColor Yellow

# 2. Start a background load test (simulated)
Write-Host "📈 Starting background traffic to Aegis..."
$job = Start-Job -ScriptBlock {
    param($count=50)
    for ($i=1; $i -le $count; $i++) {
        try {
            $resp = Invoke-WebRequest -Uri "http://localhost:8080/api/test/limited" -UseBasicParsing -TimeoutSec 1 -SkipHttpErrorCheck
            Write-Output "Traffic $i: Status $($resp.StatusCode)"
        } catch {
            Write-Output "Traffic $i: FAILED"
        }
        Start-Sleep -Milliseconds 500
    }
}

Start-Sleep -Seconds 2

# 3. KILL THE MASTER
Write-Host "🔥 KILLING REDIS MASTER (titan-redis-master)..." -ForegroundColor Red
docker stop titan-redis-master

# 4. Monitor Failover
Write-Host "⏳ Waiting for Sentinel to detect failure and elect new master (approx 10-15s)..."
for ($i=1; $i -le 20; $i++) {
    $new_master = docker exec titan-sentinel-1 redis-cli -p 26379 sentinel get-master-addr-by-name mymaster
    if ($new_master[0] -ne $master_ip) {
        Write-Host "✅ FAILOVER COMPLETE! New Master IP: $($new_master[0])" -ForegroundColor Green
        break
    }
    Write-Host "Searching for new master... ($i/20)"
    Start-Sleep -Seconds 1
}

# 5. Check background traffic results
Write-Host "📊 Checking Aegis resilience during outage..."
Receive-Job -Job $job | Select-Object -Last 10

# 6. Cleanup & Restore
Write-Host "🔄 Restoring infrastructure..."
docker start titan-redis-master
Write-Host "✨ Chaos Monkey finished." -ForegroundColor Cyan
