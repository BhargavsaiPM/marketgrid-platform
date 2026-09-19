#!/usr/bin/env bash
# ==============================================================================
# MarketGrid Commerce — Service Runner (run-services.sh)
# ==============================================================================
# Starts all 7 microservices in strict dependency order using nohup:
#   1. eureka-server   (8761)
#   2. config-server   (8888)
#   3. user-service    (8081)
#   4. vendor-service  (8082)
#   5. product-service (8083)
#   6. order-service   (8084)
#   7. api-gateway     (8080)
#
# Survives terminal closure/reuse via nohup.
# Redirects output to logs/<service-name>.log.
# Records process IDs in pids/<service-name>.pid.
# Polls /actuator/health every 2 seconds (30s timeout) before proceeding.
# Prints a final status table showing UP/DOWN for all 7 services.
# ==============================================================================

set -u

C_RESET='\033[0m'
C_BOLD='\033[1m'
C_CYAN='\033[0;36m'
C_GREEN='\033[0;32m'
C_RED='\033[0;31m'
C_YELLOW='\033[0;33m'
C_MAGENTA='\033[0;35m'
C_GRAY='\033[0;90m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS_DIR="$SCRIPT_DIR/logs"
PIDS_DIR="$SCRIPT_DIR/pids"

mkdir -p "$LOGS_DIR"
mkdir -p "$PIDS_DIR"

TIMEOUT_SECONDS=35

# Java & Maven Environment Resolution
if command -v cygpath >/dev/null 2>&1; then
    if [ -n "${JAVA_HOME:-}" ]; then
        export JAVA_HOME="$(cygpath -u "$JAVA_HOME")"
    fi
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -d "$JAVA_HOME" ]; then
    if [ -d "/c/Program Files/Java/jdk-25.0.3" ]; then
        export JAVA_HOME="/c/Program Files/Java/jdk-25.0.3"
    elif [ -d "C:/Program Files/Java/jdk-25.0.3" ]; then
        export JAVA_HOME="C:/Program Files/Java/jdk-25.0.3"
    fi
fi

if command -v mvn.cmd >/dev/null 2>&1; then
    MVN_EXEC="mvn.cmd"
elif [ -f "C:/apache-maven-3.9.6/bin/mvn.cmd" ]; then
    MVN_EXEC="C:/apache-maven-3.9.6/bin/mvn.cmd"
elif [ -f "/c/apache-maven-3.9.6/bin/mvn.cmd" ]; then
    MVN_EXEC="/c/apache-maven-3.9.6/bin/mvn.cmd"
else
    MVN_EXEC="mvn"
fi

if command -v curl.exe >/dev/null 2>&1; then
    CURL_EXEC="curl.exe"
else
    CURL_EXEC="curl"
fi

if command -v netstat.exe >/dev/null 2>&1; then
    NETSTAT_EXEC="netstat.exe"
else
    NETSTAT_EXEC="netstat"
fi

declare -A SERVICE_STATUS
declare -A SERVICE_PIDS

echo -e "\n${C_BOLD}${C_MAGENTA}================================================================================"
echo "         MARKETGRID COMMERCE — MICROSERVICES RUNNER (NOHUP)                     "
echo "================================================================================${C_RESET}"
echo -e " ${C_GRAY}Root Directory : $SCRIPT_DIR${C_RESET}"
echo -e " ${C_GRAY}Logs Directory : $LOGS_DIR${C_RESET}"
echo -e " ${C_GRAY}PIDs Directory : $PIDS_DIR${C_RESET}"
echo -e " ${C_GRAY}Maven Command  : $MVN_EXEC${C_RESET}"
echo -e " ${C_GRAY}Health Timeout : ${TIMEOUT_SECONDS}s per service (polling every 2s)${C_RESET}"
echo -e "${C_BOLD}${C_MAGENTA}================================================================================${C_RESET}\n"

# Helper to check if a port is in use
is_port_in_use() {
    local port="$1"
    if [ -n "$NETSTAT_EXEC" ]; then
        "$NETSTAT_EXEC" -ano 2>/dev/null | grep -E "(:$port|127.0.0.1:$port|0.0.0.0:$port)" | grep -qi "LISTENING" && return 0
    fi
    if command -v lsof >/dev/null 2>&1; then
        lsof -i ":$port" -sTCP:LISTEN >/dev/null 2>&1 && return 0
    fi
    return 1
}

# Helper to get listening PID for a port
get_listening_pid() {
    local port="$1"
    if [ -n "$NETSTAT_EXEC" ]; then
        "$NETSTAT_EXEC" -ano 2>/dev/null | grep -E "(:$port|127.0.0.1:$port|0.0.0.0:$port)" | grep -i "LISTENING" | awk '{print $NF}' | tr -d '\r' | head -n1
    fi
}

# Helper to launch a service via nohup and capture PID
launch_service() {
    local svc_name="$1"
    local dir_name="$2"
    local port="$3"
    local pid_file="$PIDS_DIR/$dir_name.pid"
    local log_file="$LOGS_DIR/$dir_name.log"

    if is_port_in_use "$port"; then
        echo -e "  ${C_YELLOW}[ALREADY RUNNING]${C_RESET} $svc_name is already active on port $port."
        local active_pid
        active_pid=$(get_listening_pid "$port")
        if [ -n "$active_pid" ]; then
            echo "$active_pid" > "$pid_file"
            SERVICE_PIDS["$svc_name"]="$active_pid"
        else
            SERVICE_PIDS["$svc_name"]="active"
        fi
        return 0
    fi

    echo -e "  Launching ${C_CYAN}$svc_name${C_RESET} on port $port (logs: logs/$dir_name.log)..."

    cd "$SCRIPT_DIR/$dir_name"
    nohup mvn spring-boot:run > "$log_file" 2>&1 &
    local spawned_pid=$!
    echo "$spawned_pid" > "$pid_file"
    SERVICE_PIDS["$svc_name"]="$spawned_pid"
    cd "$SCRIPT_DIR"

    sleep 1 && ps -p "$spawned_pid" > /dev/null 2>&1 && echo "  [OK] Process $spawned_pid is alive" || echo "  [WARNING] Process $spawned_pid died immediately - check $log_file"
}

# Helper to poll /actuator/health every 2s until ready or timeout
poll_health() {
    local svc_name="$1"
    local dir_name="$2"
    local url="$3"
    local port="$4"
    local timeout="$TIMEOUT_SECONDS"
    local elapsed=0
    local pid_file="$PIDS_DIR/$dir_name.pid"

    printf "  Polling %s (timeout: %ds)...\n" "$url" "$timeout"

    while [ "$elapsed" -lt "$timeout" ]; do
        local response
        response=$("$CURL_EXEC" -s --connect-timeout 2 -m 2 "$url" 2>/dev/null || true)

        if echo "$response" | grep -q '"status":"UP"' || echo "$response" | grep -q "UP"; then
            # Update PID file with the exact listening Java PID if available
            local listening_pid
            listening_pid=$(get_listening_pid "$port")
            if [ -n "$listening_pid" ]; then
                echo "$listening_pid" > "$pid_file"
                SERVICE_PIDS["$svc_name"]="$listening_pid"
            fi

            printf "  ${C_GREEN}[UP]${C_RESET} %s is healthy on port %s! (%ds)\n" "$svc_name" "$port" "$elapsed"
            SERVICE_STATUS["$svc_name"]="UP"
            return 0
        fi

        sleep 2
        elapsed=$((elapsed + 2))
    done

    printf "  ${C_RED}[DOWN]${C_RESET} %s failed to report healthy within %ds! (Port: %s)\n" "$svc_name" "$timeout" "$port"
    SERVICE_STATUS["$svc_name"]="DOWN"
    return 1
}

# Start and poll a single service
start_and_wait() {
    local step="$1"
    local svc_name="$2"
    local dir_name="$3"
    local port="$4"
    local health_url="http://localhost:$port/actuator/health"

    echo -e "${C_BOLD}${C_YELLOW}[$step/7] Starting $svc_name (port $port)...${C_RESET}"
    launch_service "$svc_name" "$dir_name" "$port"
    poll_health "$svc_name" "$dir_name" "$health_url" "$port"
    echo ""
}

# ------------------------------------------------------------------------------
# Launch in strict dependency order:
# 1. eureka-server (8761)
# 2. config-server (8888)
# 3. user-service (8081)
# 4. vendor-service (8082)
# 5. product-service (8083)
# 6. order-service (8084)
# 7. api-gateway (8080)
# ------------------------------------------------------------------------------

start_and_wait "1" "eureka-server"   "eureka-server"   8761
start_and_wait "2" "config-server"   "config-server"   8888
start_and_wait "3" "user-service"    "user-service"    8081
start_and_wait "4" "vendor-service"  "vendor-service"  8082
start_and_wait "5" "product-service" "product-service" 8083
start_and_wait "6" "order-service"   "order-service"   8084
start_and_wait "7" "api-gateway"     "api-gateway"     8080

# ------------------------------------------------------------------------------
# Final Status Table
# ------------------------------------------------------------------------------
echo -e "${C_BOLD}${C_MAGENTA}================================================================================"
echo "                       SERVICES STATUS REPORT                                   "
echo "================================================================================${C_RESET}"

ALL_HEALTHY=true
SERVICES=(
    "eureka-server:8761:eureka-server.log"
    "config-server:8888:config-server.log"
    "user-service:8081:user-service.log"
    "vendor-service:8082:vendor-service.log"
    "product-service:8083:product-service.log"
    "order-service:8084:order-service.log"
    "api-gateway:8080:api-gateway.log"
)

printf "  %-16s %-6s %-8s %-10s %-25s\n" "SERVICE" "PORT" "STATUS" "PID" "LOG FILE"
echo -e "  ----------------------------------------------------------------------"

for entry in "${SERVICES[@]}"; do
    IFS=":" read -r name port log <<< "$entry"
    status="${SERVICE_STATUS[$name]:-DOWN}"
    pid="${SERVICE_PIDS[$name]:-unknown}"

    if [ "$status" = "UP" ]; then
        printf "  %-16s %-6s ${C_GREEN}%-8s${C_RESET} %-10s %-25s\n" "$name" "$port" "UP" "$pid" "logs/$log"
    else
        printf "  %-16s %-6s ${C_RED}%-8s${C_RESET} %-10s %-25s\n" "$name" "$port" "DOWN" "$pid" "logs/$log"
        ALL_HEALTHY=false
    fi
done

echo -e "${C_BOLD}${C_MAGENTA}================================================================================${C_RESET}"

if [ "$ALL_HEALTHY" = true ]; then
    echo -e "${C_GREEN}${C_BOLD}✓ ALL 7 SERVICES CONFIRMED UP AND READY!${C_RESET}\n"
    echo -e "${C_CYAN}Next Steps:${C_RESET}"
    echo -e "  1. Run authentication & JWT token inspection:"
    echo -e "     ${C_BOLD}bash get-tokens.sh${C_RESET}"
    echo -e "  2. Access Gateway Swagger UI Aggregator:"
    echo -e "     ${C_BOLD}http://localhost:8080/swagger-ui.html${C_RESET}"
    echo -e "  3. Stop all services when finished:"
    echo -e "     ${C_BOLD}bash stop-services.sh${C_RESET}\n"
    exit 0
else
    echo -e "${C_RED}${C_BOLD}✗ ONE OR MORE SERVICES FAILED TO RESPOND (DOWN).${C_RESET}"
    echo -e "${C_YELLOW}  Inspect the corresponding log file in logs/ to view errors:${C_RESET}"
    echo -e "  Example: ${C_GRAY}tail -n 50 logs/<service-name>.log${C_RESET}\n"
    exit 1
fi
