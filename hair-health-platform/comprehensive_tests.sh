#!/bin/bash

# --- Configuration ---
# Ensure this matches your actual backend URL and base API path
BASE_URL="http://localhost:8080/api/v1"

PUBLIC_TEST_ENDPOINT="$BASE_URL/test/public"
HEALTH_ENDPOINT="$BASE_URL/health"

REGISTER_ENDPOINT="$BASE_URL/auth/register"
LOGIN_ENDPOINT="$BASE_URL/auth/login"
REFRESH_TOKEN_ENDPOINT="$BASE_URL/auth/refresh-token"
LOGOUT_ENDPOINT="$BASE_URL/auth/logout"
CURRENT_USER_ENDPOINT="$BASE_URL/auth/me" # Endpoint to get current user info

PROTECTED_TEST_ENDPOINT="$BASE_URL/test/protected"
HAIR_FALL_LOGS_ENDPOINT="$BASE_URL/me/hair-fall-logs"
INTERVENTIONS_ENDPOINT="$BASE_URL/me/interventions"
PROGRESS_PHOTOS_ENDPOINT="$BASE_URL/me/progress-photos"
UPLOAD_URL_ENDPOINT="$PROGRESS_PHOTOS_ENDPOINT/upload-url"

# --- Helper Functions ---
# Improved helper function to provide more context
function print_status {
    local test_name="$1"
    local curl_exit_code="$2" # $? from the curl command
    local http_status_code="$3"
    local expected_http_status_code="$4"
    local response_body="$5"

    echo "------------------------------------"
    echo "🧪 Test: $test_name"

    if [ "$http_status_code" -eq "$expected_http_status_code" ]; then
        echo "✅ PASSED (HTTP $http_status_code)"
        if [ ! -z "$response_body" ]; then
            echo "   Response (first 150 chars): $(echo "$response_body" | cut -c 1-150)..."
        fi
    else
        echo "❌ FAILED"
        echo "   Expected HTTP Status: $expected_http_status_code"
        echo "   Received HTTP Status: $http_status_code"
        echo "   Curl Exit Code: $curl_exit_code"
        if [ ! -z "$response_body" ]; then
            echo "   Full Response Body: $response_body"
        fi
        # Consider exiting the script on critical failures
        # if [[ "$test_name" == *"Register"* || "$test_name" == *"Login"* ]]; then
        #     echo "Critical test failed. Aborting."
        #     exit 1
        # fi
    fi
}

# Function to make a curl request and parse response
# Usage: make_request "TEST_NAME" "EXPECTED_HTTP_CODE" "METHOD" "ENDPOINT" "HEADERS_ARRAY" "DATA_PAYLOAD"
# Headers array should be like: HEADERS_ARRAY=("-H" "Content-Type: application/json" "-H" "Another-Header: value")
# For GET requests or no data, pass "" for DATA_PAYLOAD
function make_request {
    local test_name="$1"
    local expected_http_status_code="$2"
    local method="$3"
    local endpoint="$4"
    # Capture headers array correctly
    local num_headers=${#request_headers[@]}
    local headers_for_curl=()
    for (( i=0; i<num_headers; i++ )); do
        headers_for_curl+=("${request_headers[i]}")
    done

    local data_payload="$5" # Should be the last argument passed to the outer function call

    echo "------------------------------------"
    echo "🚀 Executing: $test_name ($method $endpoint)"

    local response
    local http_status_code
    local curl_exit_code

    if [ -z "$data_payload" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "${headers_for_curl[@]}" "$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "${headers_for_curl[@]}" -d "$data_payload" "$endpoint")
    fi
    curl_exit_code=$?
    http_status_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)

    # Store results globally for assertions if needed outside this function
    # Or pass them back if Bash supported complex return types easily
    # For now, using global vars for simplicity in this script
    # This is not ideal for pure functions but simplifies Bash scripting for sequential tests
    LAST_RESPONSE_BODY="$response_body"
    LAST_HTTP_STATUS_CODE="$http_status_code"
    LAST_CURL_EXIT_CODE="$curl_exit_code"

    print_status "$test_name" "$curl_exit_code" "$http_status_code" "$expected_http_status_code" "$response_body"
}


# --- Test Variables ---
TIMESTAMP=$(date +%s)
TEST_USER_EMAIL="testuser_$TIMESTAMP@hairhealth.com"
TEST_USER_USERNAME="testuser_$TIMESTAMP"
TEST_USER_PASSWORD="testpassword123"

ACCESS_TOKEN=""
REFRESH_TOKEN=""
USER_ID=""

CREATED_HAIR_FALL_LOG_ID=""
CREATED_INTERVENTION_ID=""
CREATED_PHOTO_ID=""


echo "🧪🧪🧪 Starting Comprehensive Backend API Tests ��🧪🧪"
echo "User for this run: $TEST_USER_EMAIL / $TEST_USER_USERNAME"
echo "===================================================="

# --- Step 1: Test Public Endpoints ---
request_headers=() # No specific headers needed
make_request "Public Endpoint: Health Check" "200" "GET" "$HEALTH_ENDPOINT" ""
make_request "Public Endpoint: Test Public" "200" "GET" "$PUBLIC_TEST_ENDPOINT" ""

# --- Step 2: Test Protected Endpoints (Should Return 401/403 Without Token) ---
request_headers=()
make_request "Protected Endpoint (No Token): Hair Fall Logs" "401" "GET" "$HAIR_FALL_LOGS_ENDPOINT" "" # Expect 401 or 403
make_request "Protected Endpoint (No Token): Interventions" "401" "GET" "$INTERVENTIONS_ENDPOINT" ""
make_request "Protected Endpoint (No Token): Progress Photos" "401" "GET" "$PROGRESS_PHOTOS_ENDPOINT" ""
make_request "Protected Endpoint (No Token): Test Protected" "401" "GET" "$PROTECTED_TEST_ENDPOINT" ""

# --- Step 3: Test User Registration ---
request_headers=("-H" "Content-Type: application/json")
REG_PAYLOAD="{\"email\": \"$TEST_USER_EMAIL\", \"password\": \"$TEST_USER_PASSWORD\", \"username\": \"$TEST_USER_USERNAME\"}"
make_request "Auth: User Registration" "201" "POST" "$REGISTER_ENDPOINT" "$REG_PAYLOAD"

if [ "$LAST_HTTP_STATUS_CODE" -eq 201 ]; then
    ACCESS_TOKEN=$(echo "$LAST_RESPONSE_BODY" | jq -r '.accessToken')
    REFRESH_TOKEN=$(echo "$LAST_RESPONSE_BODY" | jq -r '.refreshToken')
    USER_ID=$(echo "$LAST_RESPONSE_BODY" | jq -r '.user.id')
    echo "🔑 Registration successful. AccessToken extracted. UserID: $USER_ID"
    if [ "$ACCESS_TOKEN" == "null" ] || [ -z "$ACCESS_TOKEN" ]; then echo "❌ ERROR: AccessToken is null after registration."; exit 1; fi
    if [ "$REFRESH_TOKEN" == "null" ] || [ -z "$REFRESH_TOKEN" ]; then echo "❌ ERROR: RefreshToken is null after registration."; exit 1; fi
    if [ "$USER_ID" == "null" ] || [ -z "$USER_ID" ]; then echo "❌ ERROR: UserID is null after registration."; exit 1; fi
else
    echo "❌ CRITICAL: Registration failed. Aborting further tests."
    exit 1
fi

# --- Step 4: Test Login ---
# Note: Typically, after registration, you might already have tokens.
# This step tests the login flow independently if needed, or after a logout.
# For now, let's assume we use the tokens from registration. If we want to test login separately:
LOGIN_PAYLOAD="{\"email\": \"$TEST_USER_EMAIL\", \"password\": \"$TEST_USER_PASSWORD\"}"
make_request "Auth: User Login" "200" "POST" "$LOGIN_ENDPOINT" "$LOGIN_PAYLOAD"

if [ "$LAST_HTTP_STATUS_CODE" -eq 200 ]; then
    TEMP_ACCESS_TOKEN=$(echo "$LAST_RESPONSE_BODY" | jq -r '.accessToken')
    TEMP_REFRESH_TOKEN=$(echo "$LAST_RESPONSE_BODY" | jq -r '.refreshToken')
    echo "🔑 Login successful. Tokens received."
    if [ "$TEMP_ACCESS_TOKEN" == "null" ] || [ -z "$TEMP_ACCESS_TOKEN" ]; then echo "❌ ERROR: AccessToken is null after login."; exit 1; fi
    # Optionally, overwrite ACCESS_TOKEN and REFRESH_TOKEN here if this login is the primary source
    ACCESS_TOKEN="$TEMP_ACCESS_TOKEN"
    REFRESH_TOKEN="$TEMP_REFRESH_TOKEN"
else
    echo "❌ WARNING: Login test failed. Continuing with registration tokens if available."
    if [ -z "$ACCESS_TOKEN" ]; then echo "❌ CRITICAL: No valid ACCESS_TOKEN. Aborting."; exit 1; fi
fi


# --- Step 5: Test Authenticated Endpoints ---
request_headers=("-H" "Authorization: Bearer $ACCESS_TOKEN")
make_request "Authenticated Endpoint: Get Current User (/auth/me)" "200" "GET" "$CURRENT_USER_ENDPOINT" ""
make_request "Authenticated Endpoint: Test Protected" "200" "GET" "$PROTECTED_TEST_ENDPOINT" ""
make_request "Authenticated Endpoint: Get Hair Fall Logs (Initial)" "200" "GET" "$HAIR_FALL_LOGS_ENDPOINT" ""
make_request "Authenticated Endpoint: Get Interventions (Initial)" "200" "GET" "$INTERVENTIONS_ENDPOINT" ""
make_request "Authenticated Endpoint: Get Progress Photos (Initial)" "200" "GET" "$PROGRESS_PHOTOS_ENDPOINT" ""


# --- Step 6: Test Data Operations with Authentication ---
# Create a hair fall log
request_headers=("-H" "Authorization: Bearer $ACCESS_TOKEN" "-H" "Content-Type: application/json")
HAIR_FALL_PAYLOAD="{\"date\": \"$(date +%Y-%m-%d)\", \"count\": 45, \"category\": \"SHOWER\", \"description\": \"Automated test shower hair fall\"}"
make_request "Data Ops: Create Hair Fall Log" "201" "POST" "$HAIR_FALL_LOGS_ENDPOINT" "$HAIR_FALL_PAYLOAD"
if [ "$LAST_HTTP_STATUS_CODE" -eq 201 ]; then
    CREATED_HAIR_FALL_LOG_ID=$(echo "$LAST_RESPONSE_BODY" | jq -r '.id') # Assuming 'id' is returned
    echo "📄 Created Hair Fall Log ID: $CREATED_HAIR_FALL_LOG_ID"
    if [ "$CREATED_HAIR_FALL_LOG_ID" == "null" ] || [ -z "$CREATED_HAIR_FALL_LOG_ID" ]; then echo "❌ ERROR: Hair Fall Log ID is null."; fi
fi

# Create an intervention
INTERVENTION_PAYLOAD="{
    \"type\": \"TOPICAL\", \"productName\": \"Minoxidil 5% Test\", \"dosageAmount\": \"1ml\",
    \"frequency\": \"Twice Daily\", \"applicationTime\": \"08:00, 20:00\",
    \"startDate\": \"$(date +%Y-%m-%d)\", \"notes\": \"Automated test intervention\"
}"
make_request "Data Ops: Create Intervention" "201" "POST" "$INTERVENTIONS_ENDPOINT" "$INTERVENTION_PAYLOAD"
if [ "$LAST_HTTP_STATUS_CODE" -eq 201 ]; then
    CREATED_INTERVENTION_ID=$(echo "$LAST_RESPONSE_BODY" | jq -r '.id') # Assuming 'id' is returned
    echo "📄 Created Intervention ID: $CREATED_INTERVENTION_ID"
    if [ "$CREATED_INTERVENTION_ID" == "null" ] || [ -z "$CREATED_INTERVENTION_ID" ]; then echo "❌ ERROR: Intervention ID is null."; fi
fi

# Request photo upload URL
PHOTO_UPLOAD_PAYLOAD="{
    \"filename\": \"hairline_progress_$TIMESTAMP.jpg.enc\", \"angle\": \"HAIRLINE\",
    \"captureDate\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\", \"encryptionKeyInfo\": \"test_key_$TIMESTAMP\"
}"
make_request "Data Ops: Request Photo Upload URL" "200" "POST" "$UPLOAD_URL_ENDPOINT" "$PHOTO_UPLOAD_PAYLOAD"
# This endpoint might return an upload URL and a photo ID or metadata.
# For now, just checking it works. If it returns an ID, extract it for further tests.
if [ "$LAST_HTTP_STATUS_CODE" -eq 200 ]; then
    # Assuming the response might be like: {"id": "photoId", "uploadUrl": "..."}
    CREATED_PHOTO_ID=$(echo "$LAST_RESPONSE_BODY" | jq -r '.id') # Adjust if structure is different
    UPLOAD_PRESIGNED_URL=$(echo "$LAST_RESPONSE_BODY" | jq -r '.uploadUrl')
    echo "📄 Photo Upload URL requested. Photo ID: $CREATED_PHOTO_ID, URL: $UPLOAD_PRESIGNED_URL"
    if [ "$CREATED_PHOTO_ID" == "null" ] || [ -z "$CREATED_PHOTO_ID" ]; then echo "⚠️ WARNING: Progress Photo ID not extracted from upload URL response."; fi
    if [ "$UPLOAD_PRESIGNED_URL" == "null" ] || [ -z "$UPLOAD_PRESIGNED_URL" ]; then echo "❌ ERROR: Upload presigned URL is null."; fi
fi
# Actual file upload to presigned URL is complex for a simple bash script and often tested separately.

# --- Step 6b: Test GET, PUT, DELETE for created resources (if IDs were obtained) ---
if [ ! -z "$CREATED_HAIR_FALL_LOG_ID" ] && [ "$CREATED_HAIR_FALL_LOG_ID" != "null" ]; then
    request_headers=("-H" "Authorization: Bearer $ACCESS_TOKEN")
    make_request "Data Ops: Get Created Hair Fall Log" "200" "GET" "$HAIR_FALL_LOGS_ENDPOINT/$CREATED_HAIR_FALL_LOG_ID" ""

    request_headers=("-H" "Authorization: Bearer $ACCESS_TOKEN" "-H" "Content-Type: application/json")
    UPDATE_HAIR_FALL_PAYLOAD="{\"count\": 50, \"description\": \"Updated automated test description\"}"
    make_request "Data Ops: Update Hair Fall Log" "200" "PUT" "$HAIR_FALL_LOGS_ENDPOINT/$CREATED_HAIR_FALL_LOG_ID" "$UPDATE_HAIR_FALL_PAYLOAD"

    request_headers=("-H" "Authorization: Bearer $ACCESS_TOKEN")
    make_request "Data Ops: Delete Hair Fall Log" "204" "DELETE" "$HAIR_FALL_LOGS_ENDPOINT/$CREATED_HAIR_FALL_LOG_ID" "" # Or 200
fi
# Add similar GET/PUT/DELETE for Interventions and Photos if applicable and IDs are reliably obtained.

# --- Step 7: Test Token Refresh ---
if [ ! -z "$REFRESH_TOKEN" ] && [ "$REFRESH_TOKEN" != "null" ]; then
    request_headers=("-H" "Content-Type: application/json")
    REFRESH_PAYLOAD="{\"refreshToken\": \"$REFRESH_TOKEN\"}"
    make_request "Auth: Refresh Token" "200" "POST" "$REFRESH_TOKEN_ENDPOINT" "$REFRESH_PAYLOAD"
    if [ "$LAST_HTTP_STATUS_CODE" -eq 200 ]; then
        NEW_ACCESS_TOKEN=$(echo "$LAST_RESPONSE_BODY" | jq -r '.accessToken')
        echo "🔑 Token refresh successful. New AccessToken extracted."
        if [ "$NEW_ACCESS_TOKEN" == "null" ] || [ -z "$NEW_ACCESS_TOKEN" ]; then
             echo "❌ ERROR: New AccessToken is null after refresh."
        else
            ACCESS_TOKEN="$NEW_ACCESS_TOKEN" # Update for subsequent tests
        fi
    fi
else
    echo "⚠️ Skipping Token Refresh Test: REFRESH_TOKEN not available."
fi


# --- Step 8: Test Invalid Token Scenarios ---
request_headers=("-H" "Authorization: Bearer invalid-super-fake-token-123")
make_request "Auth (Invalid Token): Get Hair Fall Logs" "401" "GET" "$HAIR_FALL_LOGS_ENDPOINT" ""

request_headers=("-H" "Authorization: Malformed BearerToken") # Deliberately malformed
make_request "Auth (Malformed Header): Get Hair Fall Logs" "401" "GET" "$HAIR_FALL_LOGS_ENDPOINT" "" # Server might return 400 or 401


# --- Step 9: Test Registration Validation ---
request_headers=("-H" "Content-Type: application/json")
# Invalid email
make_request "Validation: Register with invalid email" "400" "POST" "$REGISTER_ENDPOINT" \
  "{\"email\": \"invalid-email\", \"password\": \"$TEST_USER_PASSWORD\", \"username\": \"validationuser1_$TIMESTAMP\"}"
# Short password
make_request "Validation: Register with short password" "400" "POST" "$REGISTER_ENDPOINT" \
  "{\"email\": \"validemail_$TIMESTAMP@example.com\", \"password\": \"short\", \"username\": \"validationuser2_$TIMESTAMP\"}"
# Duplicate email (using the one registered at the start of the script)
make_request "Validation: Register with duplicate email" "400" "POST" "$REGISTER_ENDPOINT" \
  "{\"email\": \"$TEST_USER_EMAIL\", \"password\": \"$TEST_USER_PASSWORD\", \"username\": \"duplicateuser_$TIMESTAMP\"}" # Expect 400 or 409

# --- Step 10: Logout ---
if [ ! -z "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
    request_headers=("-H" "Authorization: Bearer $ACCESS_TOKEN" "-H" "Content-Type: application/json")
    # Logout might require a refresh token in the body for some implementations
    # Assuming it only needs access token for invalidation for now, or no body.
    # If it needs refresh token: LOGOUT_PAYLOAD="{\"refreshToken\": \"$REFRESH_TOKEN\"}"
    LOGOUT_PAYLOAD="" # Adjust if your logout needs a body
    make_request "Auth: Logout" "200" "POST" "$LOGOUT_ENDPOINT" "$LOGOUT_PAYLOAD" # Or 204

    # Verify token is no longer valid
    if [ "$LAST_HTTP_STATUS_CODE" -eq 200 ] || [ "$LAST_HTTP_STATUS_CODE" -eq 204 ]; then
        echo " Verifying token after logout..."
        request_headers=("-H" "Authorization: Bearer $ACCESS_TOKEN")
        make_request "Auth (Post-Logout): Get Current User" "401" "GET" "$CURRENT_USER_ENDPOINT" ""
    fi
else
    echo "⚠️ Skipping Logout Test: ACCESS_TOKEN not available."
fi


echo "===================================================="
echo "🏁🏁🏁 All Automated Tests Completed 🏁🏁🏁"
echo "===================================================="
