# Should work - public endpoint
curl http://localhost:8080/api/v1/test/public

# Should work - health check
curl http://localhost:8080/api/v1/health

# Should return 401 Unauthorized - protected endpoint
curl http://localhost:8080/api/v1/test/protected

# Test creating a user
curl -X POST http://localhost:8080/api/v1/users/test \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "username": "testuser"}'

# Test retrieving the user (use the ID from the response above)
curl http://localhost:8080/api/v1/users/{user-id-here}


# 1. Set up test user and data
curl -X POST http://localhost:8080/api/v1/dev/setup-test-user

# 2. Get the user ID from the response above, then test hair fall logs
# Use the actual user ID from the response
USER_ID="your-user-id-here"

# 3. Get hair fall logs
curl "http://localhost:8080/api/v1/me/hair-fall-logs?userId=$USER_ID"

# 4. Get hair fall stats
curl "http://localhost:8080/api/v1/me/hair-fall-logs/stats?userId=$USER_ID"

# 5. Get logs by date range
curl "http://localhost:8080/api/v1/me/hair-fall-logs/date-range?userId=$USER_ID&startDate=2025-05-20&endDate=2025-05-30"

# 6. Create a new hair fall log
curl -X POST http://localhost:8080/api/v1/me/hair-fall-logs \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"date\": \"2025-05-30\",
    \"count\": 45,
    \"category\": \"SHOWER\",
    \"description\": \"Normal shower hair fall\"
  }"



# 1. First, get a user ID from your existing test user or create a new one
USER_ID="your-user-id-here"

# 2. Set up intervention test data
curl -X POST "http://localhost:8080/api/v1/dev/setup-intervention-data?userId=$USER_ID"

# 3. Get all interventions for the user
curl "http://localhost:8080/api/v1/me/interventions?userId=$USER_ID"

# 4. Get only active interventions
curl "http://localhost:8080/api/v1/me/interventions/active?userId=$USER_ID"

# 5. Create a new intervention
curl -X POST http://localhost:8080/api/v1/me/interventions \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"type\": \"TOPICAL\",
    \"productName\": \"Ketoconazole Shampoo 2%\",
    \"dosageAmount\": \"Apply to scalp\",
    \"frequency\": \"Twice Weekly\",
    \"applicationTime\": \"Tuesday, Friday\",
    \"startDate\": \"$(date +%Y-%m-%d)\",
    \"notes\": \"Leave on for 5 minutes before rinsing\"
  }"

# 6. Get a specific intervention (use ID from response above)
INTERVENTION_ID="intervention-id-here"
curl "http://localhost:8080/api/v1/me/interventions/$INTERVENTION_ID"

# 7. Log an application
curl -X POST "http://localhost:8080/api/v1/me/interventions/$INTERVENTION_ID/log-application" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"notes\": \"Applied as scheduled\"
  }"

# 8. Get applications for an intervention
curl "http://localhost:8080/api/v1/me/interventions/$INTERVENTION_ID/applications"

# 9. Get adherence statistics
curl "http://localhost:8080/api/v1/me/interventions/$INTERVENTION_ID/adherence"

# 10. Get applications by date range
curl "http://localhost:8080/api/v1/me/interventions/applications/date-range?userId=$USER_ID&startDate=2025-05-20&endDate=2025-05-30"

# 11. Update an intervention
curl -X PUT "http://localhost:8080/api/v1/me/interventions/$INTERVENTION_ID" \
  -H "Content-Type: application/json" \
  -d "{
    \"frequency\": \"Once Daily\",
    \"notes\": \"Reduced frequency due to scalp sensitivity\"
  }"

# 12. Deactivate an intervention
curl -X POST "http://localhost:8080/api/v1/me/interventions/$INTERVENTION_ID/deactivate"



# 1. First, get a user ID from your existing test user
USER_ID="your-user-id-here"

# 2. Set up photo test data
curl -X POST "http://localhost:8080/api/v1/dev/setup-photo-data?userId=$USER_ID"

# 3. Get all photos for the user
curl "http://localhost:8080/api/v1/me/progress-photos?userId=$USER_ID"

# 4. Get photos by specific angle
curl "http://localhost:8080/api/v1/me/progress-photos?userId=$USER_ID&angle=VERTEX"

# 5. Get photo statistics
curl "http://localhost:8080/api/v1/me/progress-photos/stats?userId=$USER_ID"

# 6. Request upload URL for new photo
curl -X POST http://localhost:8080/api/v1/me/progress-photos/upload-url \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"filename\": \"hairline_progress_$(date +%Y%m%d).jpg.enc\",
    \"angle\": \"HAIRLINE\",
    \"captureDate\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
    \"encryptionKeyInfo\": \"test_key_$(date +%s)\"
  }"

# 7. Get a specific photo's metadata (use ID from response above)
PHOTO_ID="photo-id-here"
curl "http://localhost:8080/api/v1/me/progress-photos/$PHOTO_ID"

# 8. Get view URL for a photo
curl "http://localhost:8080/api/v1/me/progress-photos/$PHOTO_ID/view-url"


# 9. Get comparison set for multiple angles
curl "http://localhost:8080/api/v1/me/progress-photos/comparison-set?userId=$USER_ID&angles=VERTEX,HAIRLINE,TEMPLES&monthsBack=3"

# 10. Get photos by date range
START_DATE="2025-01-01T00:00:00Z"
END_DATE="2025-05-30T23:59:59Z"
curl "http://localhost:8080/api/v1/me/progress-photos?userId=$USER_ID&startDate=$START_DATE&endDate=$END_DATE"

# 11. Finalize a photo upload (use photo ID from step 6)
curl -X POST "http://localhost:8080/api/v1/me/progress-photos/$PHOTO_ID/finalize" \
 -H "Content-Type: application/json" \
 -d "{
   \"fileSize\": 1024000
 }"

# 12. Delete a photo (soft delete)
curl -X DELETE "http://localhost:8080/api/v1/me/progress-photos/$PHOTO_ID"

# 13. Hard delete a photo
curl -X DELETE "http://localhost:8080/api/v1/me/progress-photos/$PHOTO_ID?hardDelete=true"

# 14. Get photos filtered by angle and date range
curl "http://localhost:8080/api/v1/me/progress-photos?userId=$USER_ID&angle=VERTEX&startDate=$START_DATE&endDate=$END_DATE"





# 1. Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@hairhealth.com",
    "password": "testpassword123",
    "username": "testuser"
  }'

# Save the access token from the response
ACCESS_TOKEN="your-access-token-here"

# 2. Login with existing user
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@hairhealth.com",
    "password": "testpassword123"
  }'

# 3. Get current user info (using the access token)
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/auth/me

# 4. Test protected endpoint (should work with token)
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/me/hair-fall-logs"

# 5. Test protected endpoint without token (should return 401)
curl "http://localhost:8080/api/v1/me/hair-fall-logs"

# 6. Refresh token (use refresh token from login/register response)
REFRESH_TOKEN="your-refresh-token-here"
curl -X POST http://localhost:8080/api/v1/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}"

# 7. Logout
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN"



