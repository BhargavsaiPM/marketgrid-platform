# ==============================================================================
# MarketGrid Commerce — Shutdown Script (PowerShell)
# ==============================================================================
# Stops all MarketGrid services by closing active listeners on their respective ports:
#   8761 (eureka-server), 8888 (config-server), 8080 (api-gateway),
#   8081 (user-service), 8082 (vendor-service), 8083 (product-service), 8084 (order-service)
# ==============================================================================

Write-Host ""
Write-Host "Stopping all MarketGrid microservices..." -ForegroundColor Yellow

$targets = @(
    @{ Name = "eureka-server";   Port = 8761 },
    @{ Name = "config-server";   Port = 8888 },
    @{ Name = "api-gateway";     Port = 8080 },
    @{ Name = "user-service";    Port = 8081 },
    @{ Name = "vendor-service";  Port = 8082 },
    @{ Name = "product-service"; Port = 8083 },
    @{ Name = "order-service";   Port = 8084 }
)

$stoppedCount = 0

foreach ($t in $targets) {
    $conn = Get-NetTCPConnection -LocalPort $t.Port -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        $pids = $conn.OwningProcess | Select-Object -Unique
        foreach ($p in $pids) {
            try {
                $proc = Get-Process -Id $p -ErrorAction SilentlyContinue
                $procName = if ($proc) { $proc.ProcessName } else { "PID $p" }
                Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
                Write-Host "  [STOPPED] $($t.Name) on port $($t.Port) ($procName, PID $p)" -ForegroundColor Green
                $stoppedCount++
            } catch {
                Write-Host "  [WARN] Could not stop PID $p on port $($t.Port)" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "  [IDLE] $($t.Name) (port $($t.Port)) is not running." -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "Done. Stopped $stoppedCount process(es)." -ForegroundColor Cyan
Write-Host ""
