# ==============================================================================
# MarketGrid Commerce — Microservices Startup Orchestrator (PowerShell)
# ==============================================================================
# Starts all 7 microservices in strict dependency order, polling /actuator/health
# before proceeding to downstream dependencies:
#   1. eureka-server (8761)
#   2. config-server (8888)
#   3. user-service (8081), vendor-service (8082), product-service (8083), order-service (8084) in parallel
#   4. api-gateway (8080)
# ==============================================================================

[CmdletBinding()]
param(
    [int]$TimeoutSeconds = 60,
    [switch]$ShowWindow
)

$ErrorActionPreference = "Continue"

$Root = $PSScriptRoot
$LogsDir = Join-Path $Root "logs"
if (!(Test-Path $LogsDir)) {
    New-Item -ItemType Directory -Path $LogsDir | Out-Null
}

$ServiceStatus = [ordered]@{}

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Magenta
Write-Host "         MARKETGRID COMMERCE — MICROSERVICES STARTUP ORCHESTRATOR               " -ForegroundColor Magenta
Write-Host "================================================================================" -ForegroundColor Magenta
Write-Host " Working Directory : $Root" -ForegroundColor DarkGray
Write-Host " Logs Directory    : $LogsDir" -ForegroundColor DarkGray
Write-Host " Service Timeout   : $TimeoutSeconds seconds" -ForegroundColor DarkGray
Write-Host "================================================================================" -ForegroundColor Magenta
Write-Host ""

# Helper to check if a port is currently in use
function Test-PortInUse {
    param([int]$Port)
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        return $true
    }
    return $false
}

# Helper to launch a Maven Spring Boot service
function Start-ServiceProcess {
    param(
        [string]$ServiceName,
        [string]$DirectoryName,
        [int]$Port
    )

    $workDir = Join-Path $Root $DirectoryName
    $logFile = Join-Path $LogsDir "$DirectoryName.log"

    if (Test-PortInUse -Port $Port) {
        Write-Host "  [INFO] $ServiceName is ALREADY running on port $Port." -ForegroundColor Yellow
        return
    }

    Write-Host "  Launching $ServiceName in background (logs: logs/$DirectoryName.log)..." -ForegroundColor Cyan

    if ($ShowWindow) {
        Start-Process -FilePath "cmd.exe" -ArgumentList "/c title $ServiceName && mvn spring-boot:run" -WorkingDirectory $workDir
    } else {
        $cmd = "mvn spring-boot:run > `"$logFile`" 2>&1"
        Start-Process -FilePath "cmd.exe" -ArgumentList "/c", $cmd -WorkingDirectory $workDir -WindowStyle Hidden
    }
}

# Helper to poll health endpoint until UP or timeout
function Wait-For-Health {
    param(
        [string]$ServiceName,
        [string]$Url,
        [int]$Port,
        [int]$Timeout = $TimeoutSeconds
    )

    $startTime = Get-Date
    Write-Host "  Waiting for $ServiceName ($Url)..." -NoNewline

    while (((Get-Date) - $startTime).TotalSeconds -lt $Timeout) {
        try {
            $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 2 -ErrorAction Stop
            if ($response.status -eq "UP" -or "$response" -match "UP") {
                $elapsed = [math]::Round(((Get-Date) - $startTime).TotalSeconds, 1)
                Write-Host "`r  [UP] $ServiceName is healthy! (${elapsed}s)                              " -ForegroundColor Green
                $script:ServiceStatus[$ServiceName] = "UP"
                return $true
            }
        }
        catch {
            # Connection refused, service starting up
        }
        Start-Sleep -Seconds 2
        $currentElapsed = [math]::Round(((Get-Date) - $startTime).TotalSeconds, 0)
        Write-Host "`r  Waiting for $ServiceName (${currentElapsed}s / ${Timeout}s)..." -NoNewline
    }

    Write-Host "`r  [FAILED] $ServiceName failed to report healthy within ${Timeout}s!             " -ForegroundColor Red
    $script:ServiceStatus[$ServiceName] = "FAILED"
    return $false
}

# ------------------------------------------------------------------------------
# STEP 1: Eureka Server (Port 8761)
# ------------------------------------------------------------------------------
Write-Host "[1/4] Starting Service Discovery (eureka-server:8761)..." -ForegroundColor Yellow
Start-ServiceProcess -ServiceName "eureka-server" -DirectoryName "eureka-server" -Port 8761
$eurekaOk = Wait-For-Health -ServiceName "eureka-server" -Url "http://localhost:8761/actuator/health" -Port 8761
if (!$eurekaOk) {
    Write-Host "  [!] Cannot continue: eureka-server failed to start. Inspect logs/eureka-server.log" -ForegroundColor Red
}

# ------------------------------------------------------------------------------
# STEP 2: Config Server (Port 8888)
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[2/4] Starting Centralized Configuration (config-server:8888)..." -ForegroundColor Yellow
Start-ServiceProcess -ServiceName "config-server" -DirectoryName "config-server" -Port 8888
$configOk = Wait-For-Health -ServiceName "config-server" -Url "http://localhost:8888/actuator/health" -Port 8888
if (!$configOk) {
    Write-Host "  [!] Warning: config-server failed to start. Downstream services will use local fallbacks." -ForegroundColor Yellow
}

# ------------------------------------------------------------------------------
# STEP 3: Domain Services (Ports 8081 - 8084) — Parallel Launch
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[3/4] Starting Core Domain Services (parallel launch)..." -ForegroundColor Yellow
Start-ServiceProcess -ServiceName "user-service"    -DirectoryName "user-service"    -Port 8081
Start-ServiceProcess -ServiceName "vendor-service"  -DirectoryName "vendor-service"  -Port 8082
Start-ServiceProcess -ServiceName "product-service" -DirectoryName "product-service" -Port 8083
Start-ServiceProcess -ServiceName "order-service"   -DirectoryName "order-service"   -Port 8084

Write-Host ""
Write-Host "  Verifying domain service health status..." -ForegroundColor DarkCyan
Wait-For-Health -ServiceName "user-service"    -Url "http://localhost:8081/actuator/health" -Port 8081
Wait-For-Health -ServiceName "vendor-service"  -Url "http://localhost:8082/actuator/health" -Port 8082
Wait-For-Health -ServiceName "product-service" -Url "http://localhost:8083/actuator/health" -Port 8083
Wait-For-Health -ServiceName "order-service"   -Url "http://localhost:8084/actuator/health" -Port 8084

# ------------------------------------------------------------------------------
# STEP 4: API Gateway (Port 8080)
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[4/4] Starting Edge API Gateway (api-gateway:8080)..." -ForegroundColor Yellow
Start-ServiceProcess -ServiceName "api-gateway" -DirectoryName "api-gateway" -Port 8080
Wait-For-Health -ServiceName "api-gateway" -Url "http://localhost:8080/actuator/health" -Port 8080

# ------------------------------------------------------------------------------
# STEP 5: Summary Report
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "================================================================================" -ForegroundColor Magenta
Write-Host "                       STARTUP SUMMARY REPORT                                   " -ForegroundColor Magenta
Write-Host "================================================================================" -ForegroundColor Magenta

$allOk = $true
$serviceMeta = @(
    @{ Name = "eureka-server";   Port = 8761; Log = "logs/eureka-server.log" },
    @{ Name = "config-server";   Port = 8888; Log = "logs/config-server.log" },
    @{ Name = "user-service";    Port = 8081; Log = "logs/user-service.log" },
    @{ Name = "vendor-service";  Port = 8082; Log = "logs/vendor-service.log" },
    @{ Name = "product-service"; Port = 8083; Log = "logs/product-service.log" },
    @{ Name = "order-service";   Port = 8084; Log = "logs/order-service.log" },
    @{ Name = "api-gateway";     Port = 8080; Log = "logs/api-gateway.log" }
)

foreach ($svc in $serviceMeta) {
    $status = $ServiceStatus[$svc.Name]
    if ($status -eq "UP") {
        Write-Host ("  {0,-16} [Port {1,4}] : {2,-7} (log: {3})" -f $svc.Name, $svc.Port, "UP", $svc.Log) -ForegroundColor Green
    } else {
        Write-Host ("  {0,-16} [Port {1,4}] : {2,-7} (log: {3})" -f $svc.Name, $svc.Port, "FAILED", $svc.Log) -ForegroundColor Red
        $allOk = $false
    }
}

Write-Host "================================================================================" -ForegroundColor Magenta

if ($allOk) {
    Write-Host "✓ ALL 7 SERVICES ARE HEALTHY AND RUNNING!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next step: Run the end-to-end verification suite:" -ForegroundColor Cyan
    Write-Host "  bash verify-marketgrid.sh" -ForegroundColor White
    Write-Host ""
    Write-Host "To view API Documentation (Swagger UI aggregator):" -ForegroundColor Cyan
    Write-Host "  http://localhost:8080/swagger-ui.html" -ForegroundColor White
    Write-Host ""
    Write-Host "To stop all running services later, run:" -ForegroundColor Cyan
    Write-Host "  .\stop-all.ps1" -ForegroundColor White
} else {
    Write-Host "✗ SOME SERVICES FAILED TO START." -ForegroundColor Red
    Write-Host "  Check the respective log files in the 'logs/' folder for detailed stacktraces." -ForegroundColor Yellow
    Write-Host "  Example: Get-Content logs/api-gateway.log -Tail 100" -ForegroundColor DarkGray
}
Write-Host ""
