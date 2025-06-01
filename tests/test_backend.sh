#!/bin/bash

# --- Configuration ---
BASE_URL="http://localhost:8080/api" # Adjust if your backend runs on a different port/host
REGISTER_ENDPOINT="$BASE_URL/auth/register"
LOGIN_ENDPOINT="$BASE_URL/auth/login"
HAIR_HEALTH_ENDPOINT="$BASE_URL/hair-health"

# --- Helper Functions ---
function print_status {
    if [ $1 -eq 0 ]; then
        echo "✅ Test Passed: $2"
    else
        echo "❌ Test Failed: $2 (Status Code: $3)"
        if [ ! -z "$4" ]; then
            echo "   Response: $4"
        fi
        # Consider exiting the script on failure if tests are dependent
        # exit 1
    fi
}

# --- Test Variables ---
# Generate unique user credentials for each test run to avoid conflicts
TIMESTAMP=$(date +%s)
TEST_USER_EMAIL="testuser_$TIMESTAMP@example.com"
TEST_USER_PASSWORD="password123"
JWT_TOKEN=""
CREATED_ENTRY_ID=""

echo "🧪 Starting Backend API Tests..."
echo "------------------------------------"
echo "Using Email: $TEST_USER_EMAIL"
echo "------------------------------------"

# --- 1. Authentication Tests ---

# Test 1.1: Register a new user
echo "[Auth] Testing User Registration..."
REG_RESPONSE=$(curl -s -w "%{http_code}" -X POST $REGISTER_ENDPOINT \
    -H "Content-Type: application/json" \
    -d "{\"email\": \"$TEST_USER_EMAIL\", \"password\": \"$TEST_USER_PASSWORD\", \"username\": \"testuser_$TIMESTAMP\"}")
REG_STATUS_CODE=$(echo "$REG_RESPONSE" | tail -n1)
REG_BODY=$(echo "$REG_RESPONSE" | sed '$ d')

print_status $? "User Registration" "$REG_STATUS_CODE" "$REG_BODY"
if [ "$REG_STATUS_CODE" -ne 201 ]; then
    echo "Critical error: Registration failed. Aborting further tests that depend on a user."
    exit 1
fi
echo "Registered User Response: $REG_BODY"
echo "------------------------------------"

# Test 1.2: Login with the new user
echo "[Auth] Testing User Login..."
LOGIN_RESPONSE=$(curl -s -w "%{http_code}" -X POST $LOGIN_ENDPOINT \
    -H "Content-Type: application/json" \
    -d "{\"email\": \"$TEST_USER_EMAIL\", \"password\": \"$TEST_USER_PASSWORD\"}")
LOGIN_STATUS_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)
LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed '$ d')

print_status $? "User Login" "$LOGIN_STATUS_CODE" "$LOGIN_BODY"
if [ "$LOGIN_STATUS_CODE" -ne 200 ]; then
    echo "Critical error: Login failed. Aborting further tests that depend on JWT."
    exit 1
fi

JWT_TOKEN=$(echo "$LOGIN_BODY" | jq -r '.token') # Assuming token is in a "token" field
if [ -z "$JWT_TOKEN" ] || [ "$JWT_TOKEN" == "null" ]; then
    echo "❌ Test Failed: JWT Token not found in login response."
    echo "   Response: $LOGIN_BODY"
    exit 1
else
    echo "🔑 Obtained JWT Token: $JWT_TOKEN"
fi
echo "------------------------------------"

# Test 1.3: Attempt to access protected route without JWT
echo "[Auth] Testing Access to Protected Route (Hair Health GET) WITHOUT JWT..."
NO_JWT_RESPONSE=$(curl -s -w "%{http_code}" -X GET $HAIR_HEALTH_ENDPOINT)
NO_JWT_STATUS_CODE=$(echo "$NO_JWT_RESPONSE" | tail -n1)
NO_JWT_BODY=$(echo "$NO_JWT_RESPONSE" | sed '$ d')

# Expecting 401 Unauthorized or 403 Forbidden
if [ "$NO_JWT_STATUS_CODE" -eq 401 ] || [ "$NO_JWT_STATUS_CODE" -eq 403 ]; then
    print_status 0 "Access Protected Route WITHOUT JWT (Expected Fail)" "$NO_JWT_STATUS_CODE"
else
    print_status 1 "Access Protected Route WITHOUT JWT (Expected Fail)" "$NO_JWT_STATUS_CODE" "$NO_JWT_BODY"
fi
echo "------------------------------------"

# Test 1.4: Attempt to access protected route with an INVALID JWT
echo "[Auth] Testing Access to Protected Route (Hair Health GET) WITH INVALID JWT..."
INVALID_JWT_RESPONSE=$(curl -s -w "%{http_code}" -X GET $HAIR_HEALTH_ENDPOINT \
    -H "Authorization: Bearer invalidtoken123")
INVALID_JWT_STATUS_CODE=$(echo "$INVALID_JWT_RESPONSE" | tail -n1)
INVALID_JWT_BODY=$(echo "$INVALID_JWT_RESPONSE" | sed '$ d')

if [ "$INVALID_JWT_STATUS_CODE" -eq 401 ] || [ "$INVALID_JWT_STATUS_CODE" -eq 403 ]; then
    print_status 0 "Access Protected Route WITH INVALID JWT (Expected Fail)" "$INVALID_JWT_STATUS_CODE"
else
    print_status 1 "Access Protected Route WITH INVALID JWT (Expected Fail)" "$INVALID_JWT_STATUS_CODE" "$INVALID_JWT_BODY"
fi
echo "------------------------------------"


# --- 2. Hair Health CRUD Tests (Protected by JWT) ---

# Test 2.1: Create a new hair health entry
echo "[CRUD] Testing Create Hair Health Entry..."
CREATE_PAYLOAD="{\"date\": \"2025-05-30\", \"hairCondition\": \"Good\", \"productsUsed\": [\"Shampoo A\", \"Conditioner B\"], \"notes\": \"Feeling healthy today\"}"
CREATE_RESPONSE=$(curl -s -w "%{http_code}" -X POST $HAIR_HEALTH_ENDPOINT \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -d "$CREATE_PAYLOAD")
CREATE_STATUS_CODE=$(echo "$CREATE_RESPONSE" | tail -n1)
CREATE_BODY=$(echo "$CREATE_RESPONSE" | sed '$ d')

print_status $? "Create Hair Health Entry" "$CREATE_STATUS_CODE" "$CREATE_BODY"
if [ "$CREATE_STATUS_CODE" -ne 201 ]; then
    echo "Error: Failed to create hair health entry. Some CRUD tests might fail."
else
    CREATED_ENTRY_ID=$(echo "$CREATE_BODY" | jq -r '.id') # Adjust if ID field is named differently
    if [ -z "$CREATED_ENTRY_ID" ] || [ "$CREATED_ENTRY_ID" == "null" ]; then
        echo "❌ Test Warning: Created Entry ID not found in response."
        echo "   Response: $CREATE_BODY"
    else
        echo "📄 Created Entry ID: $CREATED_ENTRY_ID"
    fi
fi
echo "------------------------------------"

# Test 2.2: Get all hair health entries for the user
echo "[CRUD] Testing Get All Hair Health Entries..."
GET_ALL_RESPONSE=$(curl -s -w "%{http_code}" -X GET $HAIR_HEALTH_ENDPOINT \
    -H "Authorization: Bearer $JWT_TOKEN")
GET_ALL_STATUS_CODE=$(echo "$GET_ALL_RESPONSE" | tail -n1)
GET_ALL_BODY=$(echo "$GET_ALL_RESPONSE" | sed '$ d')

print_status $? "Get All Hair Health Entries" "$GET_ALL_STATUS_CODE" # Don't print full body if too long
if [ "$GET_ALL_STATUS_CODE" -eq 200 ]; then
    echo "Retrieved entries (first 100 chars): $(echo $GET_ALL_BODY | cut -c 1-100)..."
    # You could add a jq check to see if the CREATED_ENTRY_ID is in the list
    CONTAINS_ID=$(echo "$GET_ALL_BODY" | jq --arg ID "$CREATED_ENTRY_ID" 'map(select(.id == $ID)) | length')
    if [ "$CONTAINS_ID" -ge 1 ]; then
        echo "✅ Verification: Newly created entry found in the list."
    elif [ ! -z "$CREATED_ENTRY_ID" ] && [ "$CREATED_ENTRY_ID" != "null" ]; then
        echo "⚠️ Verification Warning: Newly created entry NOT found in the list by ID. Body: $GET_ALL_BODY"
    fi
fi
echo "------------------------------------"

# Test 2.3: Get a specific hair health entry by ID
if [ ! -z "$CREATED_ENTRY_ID" ] && [ "$CREATED_ENTRY_ID" != "null" ]; then
    echo "[CRUD] Testing Get Specific Hair Health Entry by ID: $CREATED_ENTRY_ID..."
    GET_ONE_RESPONSE=$(curl -s -w "%{http_code}" -X GET "$HAIR_HEALTH_ENDPOINT/$CREATED_ENTRY_ID" \
        -H "Authorization: Bearer $JWT_TOKEN")
    GET_ONE_STATUS_CODE=$(echo "$GET_ONE_RESPONSE" | tail -n1)
    GET_ONE_BODY=$(echo "$GET_ONE_RESPONSE" | sed '$ d')
    print_status $? "Get Specific Hair Health Entry" "$GET_ONE_STATUS_CODE" "$GET_ONE_BODY"
else
    echo "⚠️ Skipping Get Specific Entry Test: CREATED_ENTRY_ID is not set."
fi
echo "------------------------------------"

# Test 2.4: Update a specific hair health entry
if [ ! -z "$CREATED_ENTRY_ID" ] && [ "$CREATED_ENTRY_ID" != "null" ]; then
    echo "[CRUD] Testing Update Specific Hair Health Entry by ID: $CREATED_ENTRY_ID..."
    UPDATE_PAYLOAD="{\"date\": \"2025-05-31\", \"hairCondition\": \"Excellent\", \"productsUsed\": [\"Shampoo A\", \"Conditioner B\", \"Hair Mask C\"], \"notes\": \"Updated notes, feeling super!\"}"
    UPDATE_RESPONSE=$(curl -s -w "%{http_code}" -X PUT "$HAIR_HEALTH_ENDPOINT/$CREATED_ENTRY_ID" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d "$UPDATE_PAYLOAD")
    UPDATE_STATUS_CODE=$(echo "$UPDATE_RESPONSE" | tail -n1)
    UPDATE_BODY=$(echo "$UPDATE_RESPONSE" | sed '$ d')
    print_status $? "Update Specific Hair Health Entry" "$UPDATE_STATUS_CODE" "$UPDATE_BODY"
    # You could add a verification step here by GETting the entry again and checking fields
else
    echo "⚠️ Skipping Update Specific Entry Test: CREATED_ENTRY_ID is not set."
fi
echo "------------------------------------"

# Test 2.5: Delete a specific hair health entry
if [ ! -z "$CREATED_ENTRY_ID" ] && [ "$CREATED_ENTRY_ID" != "null" ]; then
    echo "[CRUD] Testing Delete Specific Hair Health Entry by ID: $CREATED_ENTRY_ID..."
    DELETE_RESPONSE=$(curl -s -w "%{http_code}" -X DELETE "$HAIR_HEALTH_ENDPOINT/$CREATED_ENTRY_ID" \
        -H "Authorization: Bearer $JWT_TOKEN")
    DELETE_STATUS_CODE=$(echo "$DELETE_RESPONSE" | tail -n1)
    # DELETE_BODY=$(echo "$DELETE_RESPONSE" | sed '$ d') # Often no body or minimal body on 204/200 for DELETE

    print_status $? "Delete Specific Hair Health Entry" "$DELETE_STATUS_CODE"
    # Verification: Attempt to GET the deleted entry, should be 404
    if [ "$DELETE_STATUS_CODE" -eq 200 ] || [ "$DELETE_STATUS_CODE" -eq 204 ]; then
        echo "Verifying deletion..."
        VERIFY_DELETE_RESPONSE=$(curl -s -w "%{http_code}" -X GET "$HAIR_HEALTH_ENDPOINT/$CREATED_ENTRY_ID" \
            -H "Authorization: Bearer $JWT_TOKEN")
        VERIFY_DELETE_STATUS_CODE=$(echo "$VERIFY_DELETE_RESPONSE" | tail -n1)
        if [ "$VERIFY_DELETE_STATUS_CODE" -eq 404 ]; then
            echo "✅ Verification: Entry $CREATED_ENTRY_ID successfully deleted (Got 404)."
        else
            VERIFY_DELETE_BODY=$(echo "$VERIFY_DELETE_RESPONSE" | sed '$ d')
            echo "❌ Verification Failed: Entry $CREATED_ENTRY_ID still found or other error (Status: $VERIFY_DELETE_STATUS_CODE). Body: $VERIFY_DELETE_BODY"
        fi
    fi
else
    echo "⚠️ Skipping Delete Specific Entry Test: CREATED_ENTRY_ID is not set."
fi
echo "------------------------------------"

# Test 2.6: Attempt to get a non-existent entry
echo "[CRUD] Testing Get Non-Existent Hair Health Entry..."
NON_EXISTENT_ID="nonexistententry123" # Or some other ID format if numeric, e.g., 999999
GET_NON_EXISTENT_RESPONSE=$(curl -s -w "%{http_code}" -X GET "$HAIR_HEALTH_ENDPOINT/$NON_EXISTENT_ID" \
    -H "Authorization: Bearer $JWT_TOKEN")
GET_NON_EXISTENT_STATUS_CODE=$(echo "$GET_NON_EXISTENT_RESPONSE" | tail -n1)
GET_NON_EXISTENT_BODY=$(echo "$GET_NON_EXISTENT_RESPONSE" | sed '$ d')

if [ "$GET_NON_EXISTENT_STATUS_CODE" -eq 404 ]; then
    print_status 0 "Get Non-Existent Hair Health Entry (Expected 404)" "$GET_NON_EXISTENT_STATUS_CODE"
else
    print_status 1 "Get Non-Existent Hair Health Entry (Expected 404)" "$GET_NON_EXISTENT_STATUS_CODE" "$GET_NON_EXISTENT_BODY"
fi
echo "------------------------------------"


echo "🏁 All Tests Completed."
echo "------------------------------------"
