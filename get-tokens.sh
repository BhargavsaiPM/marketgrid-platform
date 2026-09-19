#!/usr/bin/env bash
# ==============================================================================
# MarketGrid Commerce — Authentication & Token Retrieval (get-tokens.sh)
# ==============================================================================
# This script does ONLY authentication — no vendor/product/cart/order testing.
# Its ONLY job is to register test users and retrieve their real JWT tokens.
#
# Steps:
#   1. Confirm api-gateway is reachable (http://localhost:8080/actuator/health)
#   2. Register test users (customer1, vendorA, vendorB) via POST /api/users/register
#   3. Log in as admin, customer1, vendorA, vendorB
#   4. For EACH login, print clearly:
#      - The full raw JSON response
#      - The extracted token
#      - The DECODED JWT payload (claims: role, userId, sub, etc.)
#   5. At the end, print all 4 tokens clearly labeled in copy-paste format:
#      ADMIN_TOKEN="..."
#      CUSTOMER_TOKEN="..."
#      VENDOR_A_TOKEN="..."
#      VENDOR_B_TOKEN="..."
# ==============================================================================

set -u

C_RESET='\033[0m'
C_BOLD='\033[1m'
C_CYAN='\033[0;36m'
C_GREEN='\033[0;32m'
C_RED='\033[0;31m'
C_YELLOW='\033[0;33m'
C_MAGENTA='\033[0;35m'
C_BLUE='\033[0;34m'
C_GRAY='\033[0;90m'

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"

if command -v curl.exe >/dev/null 2>&1; then
    CURL_EXEC="curl.exe"
else
    CURL_EXEC="curl"
fi

# Resolve Python Executable
if command -v python3 >/dev/null 2>&1 && python3 -c "import sys" 2>/dev/null; then
    PYTHON_EXEC="python3"
elif command -v python >/dev/null 2>&1 && python -c "import sys" 2>/dev/null; then
    PYTHON_EXEC="python"
elif command -v python.exe >/dev/null 2>&1 && python.exe -c "import sys" 2>/dev/null; then
    PYTHON_EXEC="python.exe"
elif [ -f "/c/Users/bharg/AppData/Local/Programs/Python/Python314/python.exe" ]; then
    PYTHON_EXEC="/c/Users/bharg/AppData/Local/Programs/Python/Python314/python.exe"
elif [ -f "C:/Users/bharg/AppData/Local/Programs/Python/Python314/python.exe" ]; then
    PYTHON_EXEC="C:/Users/bharg/AppData/Local/Programs/Python/Python314/python.exe"
else
    echo -e "${C_RED}Error: Python (python or python3) is required to decode JWT tokens.${C_RESET}"
    exit 1
fi

ADMIN_TOKEN=""
CUSTOMER_TOKEN=""
VENDOR_A_TOKEN=""
VENDOR_B_TOKEN=""

echo -e "\n${C_BOLD}${C_MAGENTA}================================================================================"
echo "         MARKETGRID COMMERCE — AUTHENTICATION & TOKEN RETRIEVAL                 "
echo "================================================================================${C_RESET}"
echo -e " ${C_GRAY}Gateway URL : $GATEWAY_URL${C_RESET}"
echo -e " ${C_GRAY}Curl Exec   : $CURL_EXEC${C_RESET}"
echo -e " ${C_GRAY}Python Exec : $PYTHON_EXEC${C_RESET}"
echo -e "${C_BOLD}${C_MAGENTA}================================================================================"

# Helper to pretty print json
format_json() {
    local raw="$1"
    "$PYTHON_EXEC" -c "
import sys, json
try:
    data = json.loads(sys.argv[1])
    print(json.dumps(data, indent=2))
except Exception:
    print(sys.argv[1])
" "$raw" 2>/dev/null || echo "$raw"
}

# Helper to decode JWT payload safely (with base64url padding)
decode_jwt() {
    local token="$1"
    "$PYTHON_EXEC" -c "
import sys, json, base64

token = sys.argv[1].strip()
parts = token.split('.')
if len(parts) >= 2:
    payload_b64 = parts[1].replace('-', '+').replace('_', '/')
    remainder = len(payload_b64) % 4
    if remainder > 0:
        payload_b64 += '=' * (4 - remainder)
    try:
        decoded = base64.b64decode(payload_b64).decode('utf-8')
        data = json.loads(decoded)
        print(json.dumps(data, indent=2))
    except Exception as e:
        print(f'Decoding error: {e}')
else:
    print('Invalid JWT structure')
" "$token" 2>/dev/null || echo "(unable to decode token)"
}

# Extract JSON field
json_extract() {
    local json="$1"
    local key="$2"
    "$PYTHON_EXEC" -c "
import sys, json
try:
    data = json.loads(sys.argv[1])
    val = data.get(sys.argv[2], '')
    print(val if val is not None else '')
except Exception:
    print('')
" "$json" "$key" 2>/dev/null
}

# ------------------------------------------------------------------------------
# STEP 1: Gateway Reachability Check
# ------------------------------------------------------------------------------
echo -e "\n${C_BOLD}${C_YELLOW}[STEP 1] Verifying API Gateway reachability...${C_RESET}"

CHECK_URL="$GATEWAY_URL/actuator/health"
HEALTH_RESPONSE=$("$CURL_EXEC" -s -w "\n%{http_code}" --connect-timeout 3 -m 5 "$CHECK_URL" 2>/dev/null || true)
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
BODY=$(echo "$HEALTH_RESPONSE" | sed '$d')

# Fallback check on 127.0.0.1 if localhost didn't respond
if [ "$HTTP_CODE" != "200" ] && [[ "$GATEWAY_URL" == *"localhost"* ]]; then
    FALLBACK_URL="http://127.0.0.1:8080"
    HEALTH_RESPONSE=$("$CURL_EXEC" -s -w "\n%{http_code}" --connect-timeout 3 -m 5 "$FALLBACK_URL/actuator/health" 2>/dev/null || true)
    FALLBACK_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
    if [ "$FALLBACK_CODE" = "200" ]; then
        GATEWAY_URL="$FALLBACK_URL"
        HTTP_CODE="200"
        BODY=$(echo "$HEALTH_RESPONSE" | sed '$d')
    fi
fi

if [ "$HTTP_CODE" = "200" ] && echo "$BODY" | grep -q "UP"; then
    echo -e "  ${C_GREEN}✓ API Gateway is UP and reachable at $GATEWAY_URL${C_RESET}"
else
    echo -e "  ${C_RED}✗ API Gateway is NOT reachable at $GATEWAY_URL (HTTP Status: $HTTP_CODE)${C_RESET}"
    if [ -n "$BODY" ]; then
        echo -e "  Response: $BODY"
    fi
    echo -e "\n${C_YELLOW}Please start all services first before running this script:${C_RESET}"
    echo -e "  ${C_BOLD}bash run-services.sh${C_RESET}\n"
    exit 1
fi

# ------------------------------------------------------------------------------
# STEP 2: Register Test Users
# ------------------------------------------------------------------------------
echo -e "\n${C_BOLD}${C_YELLOW}[STEP 2] Registering test accounts via API Gateway...${C_RESET}"

register_user() {
    local username="$1"
    local password="$2"
    local email="$3"
    local role="$4"

    local payload="{\"username\":\"$username\",\"password\":\"$password\",\"email\":\"$email\",\"role\":\"$role\"}"
    
    local resp
    resp=$("$CURL_EXEC" -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/api/users/register" \
        -H "Content-Type: application/json" \
        -d "$payload")

    local status
    status=$(echo "$resp" | tail -n1)
    local body
    body=$(echo "$resp" | sed '$d')

    if [ "$status" = "201" ]; then
        echo -e "  ${C_GREEN}[CREATED]${C_RESET} Registered $username ($role)"
    elif [ "$status" = "400" ]; then
        echo -e "  ${C_YELLOW}[EXISTS]${C_RESET}  User '$username' already exists, proceeding to login."
    elif [ "$status" = "500" ]; then
        echo -e "\n${C_RED}${C_BOLD}================================================================================${C_RESET}"
        echo -e "${C_RED}${C_BOLD}CRITICAL ERROR: Registration of user '$username' returned 500 INTERNAL SERVER ERROR${C_RESET}"
        echo -e "${C_RED}${C_BOLD}================================================================================${C_RESET}"
        echo -e "${C_BLUE}METHOD   :${C_RESET} POST"
        echo -e "${C_BLUE}URL      :${C_RESET} $GATEWAY_URL/api/users/register"
        echo -e "${C_BLUE}PAYLOAD  :${C_RESET} $payload"
        echo -e "${C_BLUE}RESPONSE :${C_RESET}"
        format_json "$body"
        echo -e "\n${C_YELLOW}Stopping script immediately. Inspect logs/user-service.log for stacktrace.${C_RESET}\n"
        exit 1
    else
        echo -e "\n${C_RED}Unexpected status ($status) registering user '$username':${C_RESET}"
        format_json "$body"
        exit 1
    fi
}

register_user "customer1" "pass123" "customer1@test.com" "CUSTOMER"
register_user "vendorA"   "pass123" "vendorA@test.com"   "VENDOR"
register_user "vendorB"   "pass123" "vendorB@test.com"   "VENDOR"

# ------------------------------------------------------------------------------
# STEP 3 & 4: Log in and Inspect JWT Tokens
# ------------------------------------------------------------------------------
echo -e "\n${C_BOLD}${C_YELLOW}[STEP 3 & 4] Logging in and inspecting JWT tokens...${C_RESET}"

login_user() {
    local persona="$1"
    local username="$2"
    local password="$3"
    local output_var="$4"

    echo -e "\n${C_CYAN}--------------------------------------------------------------------------------${C_RESET}"
    echo -e "${C_BOLD}Persona : ${C_YELLOW}$persona${C_RESET} (username: '${C_BOLD}$username${C_RESET}')"
    echo -e "${C_CYAN}--------------------------------------------------------------------------------${C_RESET}"

    local payload="{\"username\":\"$username\",\"password\":\"$password\"}"
    local resp
    resp=$("$CURL_EXEC" -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/api/users/login" \
        -H "Content-Type: application/json" \
        -d "$payload")

    local status
    status=$(echo "$resp" | tail -n1)
    local body
    body=$(echo "$resp" | sed '$d')

    if [ "$status" != "200" ]; then
        echo -e "${C_RED}Login FAILED for '$username' with HTTP $status!${C_RESET}"
        echo -e "${C_BLUE}RAW RESPONSE:${C_RESET}"
        format_json "$body"
        echo -e "\n${C_YELLOW}Stopping script immediately.${C_RESET}\n"
        exit 1
    fi

    echo -e "${C_BLUE}${C_BOLD}RAW JSON RESPONSE:${C_RESET}"
    format_json "$body"
    echo ""

    local token
    token=$(json_extract "$body" "token")

    if [ -z "$token" ]; then
        echo -e "${C_RED}Error: No 'token' field found in login response!${C_RESET}"
        exit 1
    fi

    echo -e "${C_BLUE}${C_BOLD}EXTRACTED TOKEN:${C_RESET}"
    echo -e "${C_GRAY}$token${C_RESET}\n"

    echo -e "${C_BLUE}${C_BOLD}DECODED JWT PAYLOAD (Claims):${C_RESET}"
    decode_jwt "$token"

    # Store token in output variable
    eval "$output_var=\"\$token\""
}

login_user "System Administrator" "admin"     "Admin@12345" "ADMIN_TOKEN"
login_user "Customer One"         "customer1" "pass123"     "CUSTOMER_TOKEN"
login_user "Vendor A"             "vendorA"   "pass123"     "VENDOR_A_TOKEN"
login_user "Vendor B"             "vendorB"   "pass123"     "VENDOR_B_TOKEN"

# ------------------------------------------------------------------------------
# STEP 5: Copy-Paste Tokens Summary
# ------------------------------------------------------------------------------
echo -e "\n${C_BOLD}${C_MAGENTA}================================================================================"
echo "                       SAVED JWT TOKENS FOR MANUAL TESTING                      "
echo "================================================================================${C_RESET}"
echo -e "# Copy-paste these export lines directly into your terminal or Postman:\n"

echo "ADMIN_TOKEN=\"$ADMIN_TOKEN\""
echo ""
echo "CUSTOMER_TOKEN=\"$CUSTOMER_TOKEN\""
echo ""
echo "VENDOR_A_TOKEN=\"$VENDOR_A_TOKEN\""
echo ""
echo "VENDOR_B_TOKEN=\"$VENDOR_B_TOKEN\""

echo -e "\n${C_BOLD}${C_MAGENTA}================================================================================"
echo -e "${C_GREEN}${C_BOLD}✓ Authentication completed successfully. All 4 tokens retrieved & verified!${C_RESET}\n"
