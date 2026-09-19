#!/usr/bin/env bash
# ==============================================================================
# MarketGrid Commerce — Service Stopper (stop-services.sh)
# ==============================================================================
# Stops all 7 microservices using saved PIDs from pids/*.pid and performs
# a safety sweep on listening ports:
#   8761 (eureka-server), 8888 (config-server), 8080 (api-gateway),
#   8081 (user-service), 8082 (vendor-service), 8083 (product-service), 8084 (order-service)
# ==============================================================================

set -u

C_RESET='\033[0m'
C_BOLD='\033[1m'
C_CYAN='\033[0;36m'
C_GREEN='\033[0;32m'
C_RED='\033[0;31m'
C_YELLOW='\033[0;33m'
C_GRAY='\033[0;90m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PIDS_DIR="$SCRIPT_DIR/pids"

echo -e "\n${C_BOLD}${C_YELLOW}Stopping MarketGrid microservices...${C_RESET}\n"

STOPPED_COUNT=0

# Helper to kill PID
kill_pid() {
    local target_pid="$1"
    local desc="$2"
    if [ -n "$target_pid" ] && [ "$target_pid" != "0" ] && [ "$target_pid" != "unknown" ]; then
        if command -v taskkill.exe >/dev/null 2>&1; then
            taskkill.exe /F /T /PID "$target_pid" >/dev/null 2>&1 || true
        elif command -v taskkill >/dev/null 2>&1; then
            taskkill //F //T //PID "$target_pid" >/dev/null 2>&1 || true
        else
            kill -9 "$target_pid" >/dev/null 2>&1 || true
        fi
        echo -e "  ${C_GREEN}[STOPPED]${C_RESET} $desc (PID: $target_pid)"
        STOPPED_COUNT=$((STOPPED_COUNT + 1))
    fi
}

# 1. Kill via stored PID files
if [ -d "$PIDS_DIR" ]; then
    for pid_file in "$PIDS_DIR"/*.pid; do
        if [ -f "$pid_file" ]; then
            svc_name=$(basename "$pid_file" .pid)
            pid=$(cat "$pid_file" 2>/dev/null | tr -d '\r\n' || echo "")
            if [ -n "$pid" ]; then
                kill_pid "$pid" "$svc_name (from $pid_file)"
            fi
            rm -f "$pid_file"
        fi
    done
fi

# 2. Safety port sweep to catch any orphaned Java listeners
if command -v netstat.exe >/dev/null 2>&1; then
    NETSTAT_CMD="netstat.exe"
else
    NETSTAT_CMD="netstat"
fi

PORTS=(8761 8888 8080 8081 8082 8083 8084)
NAMES=("eureka-server" "config-server" "api-gateway" "user-service" "vendor-service" "product-service" "order-service")

for i in "${!PORTS[@]}"; do
    port="${PORTS[$i]}"
    name="${NAMES[$i]}"

    pids=$("$NETSTAT_CMD" -ano 2>/dev/null | grep -E "(:$port|127.0.0.1:$port|0.0.0.0:$port)" | grep -i "LISTENING" | awk '{print $NF}' | tr -d '\r' | sort -u)

    for pid in $pids; do
        kill_pid "$pid" "$name on port $port"
    done
done

echo -e "\n${C_CYAN}Shutdown complete. Stopped $STOPPED_COUNT process(es).${C_RESET}\n"
