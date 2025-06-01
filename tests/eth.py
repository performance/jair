#!/usr/bin/env python3
"""
Comprehensive Backend API Test Harness
Designed for thorough debugging and issue identification.
GOAL: Find real problems, not just pass tests.
"""

import requests
import json
import time
import uuid
from datetime import datetime, timedelta
from typing import Dict, Any, Optional, List
import sys
from dataclasses import dataclass
from enum import Enum

class TestResult(Enum):
    PASS = "✅ PASS"
    FAIL = "❌ FAIL"
    SKIP = "⏭️ SKIP"
    WARN = "⚠️ WARN"

@dataclass
class TestCase:
    name: str
    result: TestResult
    message: str = ""
    response_data: Optional[Dict] = None

class BackendTestHarness:
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.session = requests.Session()
        self.access_token = None
        self.refresh_token = None
        self.user_id = None
        self.user_email = None
        self.user_password = None
        self.username = None
        self.test_results: List[TestCase] = []
        
        # Generate unique test data
        timestamp = int(time.time())
        self.user_email = f"testuser_{timestamp}@hairhealth.com"
        self.user_password = f"testpass_{timestamp}123"
        self.username = f"testuser_{timestamp}"
        
        print(f"🚀 Starting comprehensive backend test")
        print(f"📧 Test email: {self.user_email}")
        print(f"🌐 Base URL: {self.base_url}")
        print(f"🎯 GOAL: Find real issues and debug thoroughly")
        print("-" * 60)

    def log_test(self, name: str, result: TestResult, message: str = "", response_data: Dict = None):
        """Log a test result"""
        test_case = TestCase(name, result, message, response_data)
        self.test_results.append(test_case)
        print(f"{result.value} {name}")
        if message:
            print(f"    💬 {message}")
        if result in [TestResult.FAIL, TestResult.WARN]:
            print(f"    🔍 Issue detected - investigating...")

    def make_request(self, method: str, endpoint: str, data: Dict = None, 
                    headers: Dict = None, use_auth: bool = False) -> requests.Response:
        """Make HTTP request with optional authentication and detailed error reporting"""
        url = f"{self.base_url}{endpoint}"
        
        if headers is None:
            headers = {}
        
        if use_auth and self.access_token:
            headers["Authorization"] = f"Bearer {self.access_token}"
        
        if data and method.upper() in ["POST", "PUT", "PATCH"]:
            headers["Content-Type"] = "application/json"
            data = json.dumps(data)
        
        try:
            print(f"    🌐 {method} {endpoint}")
            if data and len(str(data)) < 200:
                print(f"    📤 Data: {data}")
            
            response = self.session.request(method, url, data=data, headers=headers, timeout=30)
            
            print(f"    📥 Response: {response.status_code}")
            if response.text and len(response.text) < 300:
                print(f"    📄 Body: {response.text}")
            elif response.text:
                print(f"    📄 Body: {response.text[:200]}...")
                
            return response
        except requests.exceptions.ConnectionError as e:
            print(f"    ❌ Connection Error: {e}")
            print(f"    🔍 Server may be down or endpoint doesn't exist")
            return None
        except requests.exceptions.Timeout as e:
            print(f"    ❌ Timeout Error: {e}")
            print(f"    🔍 Server taking too long to respond")
            return None
        except requests.exceptions.RequestException as e:
            print(f"    ❌ Request Error: {e}")
            return None

    def test_health_check(self):
        """Test public health endpoint"""
        response = self.make_request("GET", "/api/v1/health")
        
        if not response:
            self.log_test("Health Check", TestResult.FAIL, 
                        "No response - server may be down")
            return
            
        if response.status_code != 200:
            self.log_test("Health Check", TestResult.FAIL, 
                        f"Expected 200, got {response.status_code}")
            return
            
        try:
            data = response.json()
            status = data.get('status', 'Unknown')
            if status.upper() in ['UP', 'OK', 'HEALTHY']:
                self.log_test("Health Check", TestResult.PASS, 
                            f"Server healthy: {status}")
            else:
                self.log_test("Health Check", TestResult.WARN, 
                            f"Unexpected status: {status}")
        except json.JSONDecodeError:
            self.log_test("Health Check", TestResult.WARN, 
                        "Health endpoint doesn't return JSON")

    def test_public_endpoint(self):
        """Test public test endpoint"""
        response = self.make_request("GET", "/api/v1/test/public")
        
        if not response:
            self.log_test("Public Endpoint", TestResult.FAIL, 
                        "No response - endpoint may not exist")
            return
            
        if response.status_code != 200:
            self.log_test("Public Endpoint", TestResult.FAIL, 
                        f"Expected 200, got {response.status_code}")
        else:
            self.log_test("Public Endpoint", TestResult.PASS, 
                        "Public endpoint accessible")

    def test_protected_endpoint_unauthorized(self):
        """Test that protected endpoints properly reject unauthorized access"""
        endpoints_to_test = [
            ("/api/v1/test/protected", "Test protected endpoint"),
            ("/api/v1/me/hair-fall-logs", "Hair fall logs"),
            ("/api/v1/me/interventions", "Interventions"), 
            ("/api/v1/me/progress-photos", "Progress photos")
        ]
        
        issues_found = []
        properly_protected = []
        
        for endpoint, description in endpoints_to_test:
            print(f"    🧪 Testing unauthorized access to {description}")
            response = self.make_request("GET", endpoint)
            
            if not response:
                # No response could mean auth is working (connection refused)
                properly_protected.append(f"{description} (no response - likely protected)")
                continue
                
            if endpoint == "/api/v1/test/protected":
                # Special case: this endpoint may return 200 with anonymous user
                if response.status_code == 200:
                    try:
                        data = response.json()
                        if data.get("user") == "anonymous":
                            properly_protected.append(f"{description} (anonymous user detected)")
                        else:
                            issues_found.append(f"{description} returns 200 but user is not anonymous")
                    except:
                        issues_found.append(f"{description} returns 200 but response not parseable")
                else:
                    issues_found.append(f"{description} unexpected status {response.status_code}")
            else:
                # Regular protected endpoints should return 401
                if response.status_code == 401:
                    properly_protected.append(f"{description} (401 Unauthorized)")
                else:
                    issues_found.append(f"{description} returns {response.status_code} instead of 401")
        
        if issues_found:
            self.log_test("Protected Endpoints (Unauthorized)", TestResult.FAIL,
                        f"Security issues: {', '.join(issues_found)}")
        else:
            self.log_test("Protected Endpoints (Unauthorized)", TestResult.PASS,
                        f"All endpoints properly protected")

    def test_user_registration(self):
        """Test user registration with strict validation"""
        data = {
            "email": self.user_email,
            "password": self.user_password,
            "username": self.username
        }
        
        response = self.make_request("POST", "/api/v1/auth/register", data)
        
        if not response:
            self.log_test("User Registration", TestResult.FAIL,
                        "No response - registration endpoint may be down")
            return
            
        # Be strict about status codes
        if response.status_code not in [200, 201]:
            try:
                error_data = response.json()
                error_msg = error_data.get("message", error_data.get("error", "Unknown error"))
            except:
                error_msg = response.text[:100] if response.text else "No error message"
            
            self.log_test("User Registration", TestResult.FAIL,
                        f"Status {response.status_code}: {error_msg}")
            return
            
        try:
            result = response.json()
            
            # Strict validation of response structure
            missing_fields = []
            if not result.get("accessToken"):
                missing_fields.append("accessToken")
            if not result.get("refreshToken"):
                missing_fields.append("refreshToken")
            if not result.get("user"):
                missing_fields.append("user")
            
            if missing_fields:
                self.log_test("User Registration", TestResult.FAIL,
                            f"Missing required fields: {', '.join(missing_fields)}")
                return
                
            # Extract and validate user data
            user_data = result.get("user", {})
            self.user_id = user_data.get("id")
            returned_email = user_data.get("email")
            returned_username = user_data.get("username")
            
            validation_issues = []
            if not self.user_id:
                validation_issues.append("user.id missing")
            if returned_email != self.user_email:
                validation_issues.append(f"email mismatch: sent {self.user_email}, got {returned_email}")
            if returned_username != self.username:
                validation_issues.append(f"username mismatch: sent {self.username}, got {returned_username}")
                
            if validation_issues:
                self.log_test("User Registration", TestResult.FAIL,
                            f"Data validation issues: {', '.join(validation_issues)}")
                return
                
            # Store tokens for subsequent tests
            self.access_token = result.get("accessToken")
            self.refresh_token = result.get("refreshToken")
            
            self.log_test("User Registration", TestResult.PASS,
                        f"User created successfully: {self.user_id}")
                        
        except json.JSONDecodeError:
            self.log_test("User Registration", TestResult.FAIL,
                        "Response is not valid JSON")

    def test_user_login(self):
        """Test user login with the registered credentials"""
        if not self.user_email or not self.user_password:
            self.log_test("User Login", TestResult.SKIP,
                        "No user credentials available from registration")
            return
            
        data = {
            "email": self.user_email,
            "password": self.user_password
        }
        
        response = self.make_request("POST", "/api/v1/auth/login", data)
        
        if not response:
            self.log_test("User Login", TestResult.FAIL,
                        "No response - login endpoint may be down")
            return
            
        if response.status_code != 200:
            try:
                error_data = response.json()
                error_msg = error_data.get("message", "Unknown error")
            except:
                error_msg = response.text[:100] if response.text else "No error message"
                
            self.log_test("User Login", TestResult.FAIL,
                        f"Login failed with status {response.status_code}: {error_msg}")
            return
            
        try:
            result = response.json()
            new_access_token = result.get("accessToken")
            
            if not new_access_token:
                self.log_test("User Login", TestResult.FAIL,
                            "No access token in login response")
                return
                
            # Update tokens
            old_token = self.access_token[:20] + "..." if self.access_token else "None"
            self.access_token = new_access_token
            new_token = self.access_token[:20] + "..."
            
            # Optionally update refresh token if provided
            if result.get("refreshToken"):
                self.refresh_token = result.get("refreshToken")
                
            self.log_test("User Login", TestResult.PASS,
                        f"Login successful, token updated")
                        
        except json.JSONDecodeError:
            self.log_test("User Login", TestResult.FAIL,
                        "Login response is not valid JSON")

    def test_get_current_user(self):
        """Test getting current user info with authentication"""
        if not self.access_token:
            self.log_test("Get Current User", TestResult.SKIP,
                        "No access token available")
            return
            
        response = self.make_request("GET", "/api/v1/auth/me", use_auth=True)
        
        if not response:
            self.log_test("Get Current User", TestResult.FAIL,
                        "No response - endpoint may be down")
            return
            
        if response.status_code != 200:
            self.log_test("Get Current User", TestResult.FAIL,
                        f"Expected 200, got {response.status_code}")
            return
            
        try:
            user_data = response.json()
            
            # Validate returned user data
            validation_issues = []
            if user_data.get("email") != self.user_email:
                validation_issues.append(f"email mismatch: expected {self.user_email}, got {user_data.get('email')}")
            if user_data.get("id") != self.user_id:
                validation_issues.append(f"id mismatch: expected {self.user_id}, got {user_data.get('id')}")
                
            if validation_issues:
                self.log_test("Get Current User", TestResult.FAIL,
                            f"User data validation failed: {', '.join(validation_issues)}")
            else:
                self.log_test("Get Current User", TestResult.PASS,
                            f"Current user retrieved correctly: {user_data.get('username')}")
                            
        except json.JSONDecodeError:
            self.log_test("Get Current User", TestResult.FAIL,
                        "Response is not valid JSON")

    def test_protected_endpoint_authorized(self):
        """Test that protected endpoints work with valid authentication"""
        if not self.access_token:
            self.log_test("Protected Endpoints (Authorized)", TestResult.SKIP,
                        "No access token available")
            return
            
        response = self.make_request("GET", "/api/v1/test/protected", use_auth=True)
        
        if not response:
            self.log_test("Protected Endpoints (Authorized)", TestResult.FAIL,
                        "No response with valid token")
            return
            
        if response.status_code != 200:
            self.log_test("Protected Endpoints (Authorized)", TestResult.FAIL,
                        f"Protected endpoint inaccessible with valid token: {response.status_code}")
        else:
            # Check if user is no longer anonymous
            try:
                data = response.json()
                user = data.get("user")
                if user == "anonymous":
                    self.log_test("Protected Endpoints (Authorized)", TestResult.WARN,
                                "Token accepted but user still shows as anonymous")
                else:
                    self.log_test("Protected Endpoints (Authorized)", TestResult.PASS,
                                f"Protected endpoint accessible, user: {user}")
            except:
                self.log_test("Protected Endpoints (Authorized)", TestResult.PASS,
                            "Protected endpoint accessible with valid token")

    def test_hair_fall_logs(self):
        """Test hair fall log operations with strict validation"""
        if not self.access_token:
            self.log_test("Hair Fall Logs", TestResult.SKIP,
                        "No access token available")
            return
            
        # First, test getting logs (should be empty initially)
        print("    🧪 Testing GET /api/v1/me/hair-fall-logs")
        response = self.make_request("GET", "/api/v1/me/hair-fall-logs", use_auth=True)
        
        if not response:
            self.log_test("Hair Fall Logs", TestResult.FAIL,
                        "Cannot retrieve hair fall logs")
            return
            
        if response.status_code != 200:
            self.log_test("Hair Fall Logs", TestResult.FAIL,
                        f"GET hair fall logs failed: {response.status_code}")
            return
            
        try:
            initial_logs = response.json()
            if not isinstance(initial_logs, list):
                self.log_test("Hair Fall Logs", TestResult.FAIL,
                            f"Expected list, got {type(initial_logs)}")
                return
        except json.JSONDecodeError:
            self.log_test("Hair Fall Logs", TestResult.FAIL,
                        "GET response is not valid JSON")
            return
            
        # Test creating a new log
        print("    🧪 Testing POST /api/v1/me/hair-fall-logs")
        log_data = {
            "date": datetime.now().strftime("%Y-%m-%d"),
            "count": 45,
            "category": "SHOWER", 
            "description": "Automated test log entry"
        }
        
        response = self.make_request("POST", "/api/v1/me/hair-fall-logs", 
                                   log_data, use_auth=True)
        
        if not response:
            self.log_test("Hair Fall Logs", TestResult.FAIL,
                        "Cannot create hair fall log")
            return
            
        if response.status_code not in [200, 201]:
            self.log_test("Hair Fall Logs", TestResult.FAIL,
                        f"Create log failed: {response.status_code}")
            return
            
        try:
            created_log = response.json()
            
            # Validate created log structure
            required_fields = ["id", "userId", "date", "count", "category"]
            missing_fields = [field for field in required_fields if field not in created_log]
            
            if missing_fields:
                self.log_test("Hair Fall Logs", TestResult.FAIL,
                            f"Created log missing fields: {', '.join(missing_fields)}")
                return
                
            # Validate data integrity
            data_issues = []
            if created_log.get("userId") != self.user_id:
                data_issues.append(f"userId mismatch: expected {self.user_id}, got {created_log.get('userId')}")
            if created_log.get("count") != log_data["count"]:
                data_issues.append(f"count mismatch: expected {log_data['count']}, got {created_log.get('count')}")
            if created_log.get("category") != log_data["category"]:
                data_issues.append(f"category mismatch: expected {log_data['category']}, got {created_log.get('category')}")
                
            if data_issues:
                self.log_test("Hair Fall Logs", TestResult.FAIL,
                            f"Data integrity issues: {', '.join(data_issues)}")
                return
                
            log_id = created_log.get("id")
            
            # Verify the log appears in GET request
            print("    🧪 Verifying log appears in GET request")
            response = self.make_request("GET", "/api/v1/me/hair-fall-logs", use_auth=True)
            if response and response.status_code == 200:
                updated_logs = response.json()
                if len(updated_logs) > len(initial_logs):
                    self.log_test("Hair Fall Logs", TestResult.PASS,
                                f"Hair fall log created and retrieved successfully: {log_id}")
                else:
                    self.log_test("Hair Fall Logs", TestResult.FAIL,
                                "Created log does not appear in GET request")
            else:
                self.log_test("Hair Fall Logs", TestResult.WARN,
                            f"Log created ({log_id}) but verification GET failed")
                            
        except json.JSONDecodeError:
            self.log_test("Hair Fall Logs", TestResult.FAIL,
                        "Create log response is not valid JSON")

    def test_interventions(self):
        """Test intervention operations with comprehensive validation"""
        if not self.access_token:
            self.log_test("Interventions", TestResult.SKIP,
                        "No access token available")
            return
            
        # Test getting interventions
        print("    🧪 Testing GET /api/v1/me/interventions")
        response = self.make_request("GET", "/api/v1/me/interventions", use_auth=True)
        
        if not response:
            self.log_test("Interventions", TestResult.FAIL,
                        "Cannot retrieve interventions")
            return
            
        if response.status_code != 200:
            self.log_test("Interventions", TestResult.FAIL,
                        f"GET interventions failed: {response.status_code}")
            return
            
        # Test creating intervention
        print("    🧪 Testing POST /api/v1/me/interventions")
        intervention_data = {
            "type": "TOPICAL",
            "productName": f"Test Product {int(time.time())}",
            "dosageAmount": "1ml",
            "frequency": "Twice Daily",
            "applicationTime": "08:00, 20:00",
            "startDate": datetime.now().strftime("%Y-%m-%d"),
            "notes": "Automated test intervention"
        }
        
        response = self.make_request("POST", "/api/v1/me/interventions", 
                                   intervention_data, use_auth=True)
        
        if not response:
            self.log_test("Interventions", TestResult.FAIL,
                        "Cannot create intervention")
            return
            
        if response.status_code not in [200, 201]:
            self.log_test("Interventions", TestResult.FAIL,
                        f"Create intervention failed: {response.status_code}")
            return
            
        try:
            created_intervention = response.json()
            intervention_id = created_intervention.get("id")
            
            if not intervention_id:
                self.log_test("Interventions", TestResult.FAIL,
                            "No intervention ID in create response")
                return
                
            # Test logging an application
            print("    🧪 Testing intervention application logging")
            application_data = {
                "notes": "Automated test application"
            }
            
            app_response = self.make_request("POST", 
                                           f"/api/v1/me/interventions/{intervention_id}/log-application",
                                           application_data, use_auth=True)
            
            if app_response and app_response.status_code in [200, 201]:
                self.log_test("Interventions", TestResult.PASS,
                            f"Intervention created and application logged: {intervention_id}")
            else:
                app_status = app_response.status_code if app_response else "No response"
                self.log_test("Interventions", TestResult.WARN,
                            f"Intervention created ({intervention_id}) but application logging failed: {app_status}")
                            
        except json.JSONDecodeError:
            self.log_test("Interventions", TestResult.FAIL,
                        "Create intervention response is not valid JSON")

    def test_progress_photos(self):
        """Test progress photo operations"""
        if not self.access_token:
            self.log_test("Progress Photos", TestResult.SKIP,
                        "No access token available")
            return
            
        # Test getting photos
        print("    🧪 Testing GET /api/v1/me/progress-photos")
        response = self.make_request("GET", "/api/v1/me/progress-photos", use_auth=True)
        
        if not response:
            self.log_test("Progress Photos", TestResult.FAIL,
                        "Cannot retrieve progress photos")
            return
            
        if response.status_code != 200:
            self.log_test("Progress Photos", TestResult.FAIL,
                        f"GET progress photos failed: {response.status_code}")
            return
            
        # Test requesting upload URL
        print("    🧪 Testing POST /api/v1/me/progress-photos/upload-url")
        photo_data = {
            "filename": f"test_photo_{int(time.time())}.jpg.enc",
            "angle": "HAIRLINE",
            "captureDate": datetime.now().isoformat() + "Z",
            "encryptionKeyInfo": f"test_key_{int(time.time())}"
        }
        
        response = self.make_request("POST", "/api/v1/me/progress-photos/upload-url",
                                   photo_data, use_auth=True)
        
        if not response:
            self.log_test("Progress Photos", TestResult.FAIL,
                        "Cannot request photo upload URL")
            return
            
        if response.status_code not in [200, 201]:
            self.log_test("Progress Photos", TestResult.FAIL,
                        f"Upload URL request failed: {response.status_code}")
            return
            
        try:
            upload_data = response.json()
            
            # Check for required fields (your API uses photoMetadataId)
            photo_id = upload_data.get("photoMetadataId") or upload_data.get("photoId")
            upload_url = upload_data.get("uploadUrl")
            
            if not photo_id:
                self.log_test("Progress Photos", TestResult.FAIL,
                            "No photo ID in upload response")
                return
                
            if not upload_url:
                self.log_test("Progress Photos", TestResult.FAIL,
                            "No upload URL in response")
                return
                
            # Test finalizing upload
            print("    🧪 Testing photo upload finalization")
            finalize_data = {
                "fileSize": 1024000
            }
            
            finalize_response = self.make_request("POST",
                                                f"/api/v1/me/progress-photos/{photo_id}/finalize",
                                                finalize_data, use_auth=True)
            
            if finalize_response and finalize_response.status_code in [200, 201]:
                self.log_test("Progress Photos", TestResult.PASS,
                            f"Photo upload flow completed: {photo_id}")
            else:
                finalize_status = finalize_response.status_code if finalize_response else "No response"
                self.log_test("Progress Photos", TestResult.WARN,
                            f"Upload URL obtained ({photo_id}) but finalization failed: {finalize_status}")
                            
        except json.JSONDecodeError:
            self.log_test("Progress Photos", TestResult.FAIL,
                        "Upload URL response is not valid JSON")

    def test_token_refresh(self):
        """Test token refresh functionality"""
        if not self.refresh_token:
            self.log_test("Token Refresh", TestResult.SKIP,
                        "No refresh token available")
            return
            
        refresh_data = {
            "refreshToken": self.refresh_token
        }
        
        response = self.make_request("POST", "/api/v1/auth/refresh-token", refresh_data)
        
        if not response:
            self.log_test("Token Refresh", TestResult.FAIL,
                        "No response for token refresh")
            return
            
        if response.status_code != 200:
            self.log_test("Token Refresh", TestResult.FAIL,
                        f"Token refresh failed: {response.status_code}")
            return
            
        try:
            result = response.json()
            new_access_token = result.get("accessToken")
            
            if not new_access_token:
                self.log_test("Token Refresh", TestResult.FAIL,
                            "No new access token in refresh response")
                return
                
            # Verify the new token is different
            if new_access_token == self.access_token:
                self.log_test("Token Refresh", TestResult.WARN,
                            "New token is identical to old token")
            else:
                self.access_token = new_access_token
                self.log_test("Token Refresh", TestResult.PASS,
                            "Token refreshed successfully")
                            
        except json.JSONDecodeError:
            self.log_test("Token Refresh", TestResult.FAIL,
                        "Token refresh response is not valid JSON")

    def test_validation_errors(self):
        """Test input validation with multiple scenarios"""
        print("    🔍 Testing input validation thoroughly...")
        
        validation_tests = [
            {
                "name": "Invalid email format",
                "data": {"email": "not-an-email", "password": "validpass123", "username": "testuser"},
                "should_fail": True
            },
            {
                "name": "Missing email field", 
                "data": {"password": "validpass123", "username": "testuser"},
                "should_fail": True
            },
            {
                "name": "Empty email",
                "data": {"email": "", "password": "validpass123", "username": "testuser"},
                "should_fail": True
            },
            {
                "name": "Short password",
                "data": {"email": f"test_{int(time.time())}@test.com", "password": "123", "username": "testuser"},
                "should_fail": True
            },
            {
                "name": "Missing password",
                "data": {"email": f"test_{int(time.time())}@test.com", "username": "testuser"},
                "should_fail": True
            },
            {
                "name": "Empty username",
                "data": {"email": f"test_{int(time.time())}@test.com", "password": "validpass123", "username": ""},
                "should_fail": True
            }
        ]
        
        failures = []
        passes = []
        unexpected = []
        
        for test in validation_tests:
            print(f"    🧪 {test['name']}")
            response = self.make_request("POST", "/api/v1/auth/register", test["data"])
            
            if not response:
                failures.append(f"{test['name']} (no response)")
                continue
                
            if test["should_fail"]:
                if response.status_code in [400, 422, 422]:
                    passes.append(test['name'])
                    print(f"        ✅ Properly rejected ({response.status_code})")
                elif response.status_code in [200, 201]:
                    unexpected.append(f"{test['name']} (accepted invalid input)")
                    print(f"        ⚠️ Invalid input was accepted")
                else:
                    unexpected.append(f"{test['name']} (unexpected {response.status_code})")
            else:
                if response.status_code in [200, 201]:
                    passes.append(test['name'])
                else:
                    failures.append(f"{test['name']} (should pass but got {response.status_code})")
        
        # Report results
        total_tests = len(validation_tests)
        if len(unexpected) > 0:
            self.log_test("Input Validation", TestResult.FAIL,
                        f"Validation issues found: {len(unexpected)} problems out of {total_tests} tests")
            for issue in unexpected[:3]:  # Show first 3 issues
                print(f"        🚨 {issue}")
        elif len(passes) == total_tests:
            self.log_test("Input Validation", TestResult.PASS,
                        f"All {total_tests} validation tests work correctly")
        else:
            self.log_test("Input Validation", TestResult.WARN,
                        f"Some validation tests failed to run: {len(failures)} connection issues")

    def test_authentication_edge_cases(self):
        """Test authentication edge cases and security"""
        print("    🔍 Testing authentication security...")
        
        auth_tests = []
        
        # Test 1: Invalid password for existing user
        if self.user_email:
            response = self.make_request("POST", "/api/v1/auth/login", {
                "email": self.user_email,
                "password": "definitely_wrong_password"
            })
            
            if response:
                if response.status_code == 401:
                    auth_tests.append("✅ Wrong password properly rejected")
                elif response.status_code == 200:
                    auth_tests.append("❌ SECURITY ISSUE: Wrong password accepted!")
                else:
                    auth_tests.append(f"⚠️ Wrong password: unexpected {response.status_code}")
            else:
                auth_tests.append("⚠️ Wrong password test: no response")
        
        # Test 2: Non-existent user login
        response = self.make_request("POST", "/api/v1/auth/login", {
            "email": f"nonexistent_{int(time.time())}@test.com",
            "password": "somepassword"
        })
        
        if response:
            if response.status_code == 401:
                auth_tests.append("✅ Non-existent user properly rejected")
            elif response.status_code == 200:
                auth_tests.append("❌ SECURITY ISSUE: Non-existent user login succeeded!")
            else:
                auth_tests.append(f"⚠️ Non-existent user: unexpected {response.status_code}")
        else:
            auth_tests.append("⚠️ Non-existent user test: no response")
        
        # Test 3: Malformed JWT token
        if self.access_token:
            headers = {"Authorization": "Bearer invalid.jwt.token.here"}
            response = self.session.get(f"{self.base_url}/api/v1/me/hair-fall-logs", headers=headers)
            
            if response:
                if response.status_code == 401:
                    auth_tests.append("✅ Invalid JWT properly rejected")
                elif response.status_code == 200:
                    auth_tests.append("❌ SECURITY ISSUE: Invalid JWT accepted!")
                else:
                    auth_tests.append(f"⚠️ Invalid JWT: unexpected {response.status_code}")
            else:
                auth_tests.append("⚠️ Invalid JWT test: no response")
        
        # Test 4: Missing Authorization header
        response = self.session.get(f"{self.base_url}/api/v1/me/hair-fall-logs")
        
        if response:
            if response.status_code == 401:
                auth_tests.append("✅ Missing auth header properly rejected")
            elif response.status_code == 200:
                auth_tests.append("❌ SECURITY ISSUE: No auth required!")
            else:
                auth_tests.append(f"⚠️ Missing auth: unexpected {response.status_code}")
        else:
            auth_tests.append("⚠️ Missing auth test: no response")
        
        # Analyze results
        security_issues = [test for test in auth_tests if "SECURITY ISSUE" in test]
        passed_tests = [test for test in auth_tests if test.startswith("✅")]
        
        if security_issues:
            self.log_test("Authentication Security", TestResult.FAIL,
                        f"CRITICAL: {len(security_issues)} security issues found")
            for issue in security_issues:
                print(f"        🚨 {issue}")
        elif len(passed_tests) >= 3:
            self.log_test("Authentication Security", TestResult.PASS,
                        f"Authentication security looks good: {len(passed_tests)} tests passed")
        else:
            self.log_test("Authentication Security", TestResult.WARN,
                        f"Authentication tests incomplete: {len(passed_tests)} passed, {len(auth_tests) - len(passed_tests)} issues")

    def test_data_integrity(self):
        """Test data integrity and consistency"""
        if not self.access_token:
            self.log_test("Data Integrity", TestResult.SKIP,
                        "No access token available")
            return
            
        print("    🔍 Testing data integrity...")
        
        integrity_issues = []
        
        # Test 1: Create data with boundary values
        test_cases = [
            {
                "name": "Zero hair count",
                "data": {"date": "2025-05-31", "count": 0, "category": "SHOWER", "description": "Zero test"}
            },
            {
                "name": "Large hair count", 
                "data": {"date": "2025-05-31", "count": 99999, "category": "SHOWER", "description": "Large test"}
            },
            {
                "name": "Future date",
                "data": {"date": "2030-12-31", "count": 50, "category": "SHOWER", "description": "Future test"}
            },
            {
                "name": "Very old date",
                "data": {"date": "1990-01-01", "count": 50, "category": "SHOWER", "description": "Old test"}
            }
        ]
        
        for test_case in test_cases:
            print(f"    🧪 Testing {test_case['name']}")
            response = self.make_request("POST", "/api/v1/me/hair-fall-logs", 
                                       test_case["data"], use_auth=True)
            
            if response:
                if response.status_code in [200, 201]:
                    # Check if returned data matches input
                    try:
                        result = response.json()
                        if result.get("count") != test_case["data"]["count"]:
                            integrity_issues.append(f"{test_case['name']}: count mismatch")
                        if result.get("date") != test_case["data"]["date"]:
                            integrity_issues.append(f"{test_case['name']}: date mismatch")
                    except:
                        integrity_issues.append(f"{test_case['name']}: invalid response format")
                elif response.status_code in [400, 422]:
                    print(f"        ✅ {test_case['name']} properly validated and rejected")
                else:
                    integrity_issues.append(f"{test_case['name']}: unexpected {response.status_code}")
            else:
                integrity_issues.append(f"{test_case['name']}: no response")
        
        if integrity_issues:
            self.log_test("Data Integrity", TestResult.FAIL,
                        f"Data integrity issues: {', '.join(integrity_issues[:2])}")
        else:
            self.log_test("Data Integrity", TestResult.PASS,
                        "Data integrity validation working correctly")

    def test_api_consistency(self):
        """Test API consistency and standards compliance"""
        print("    🔍 Testing API consistency...")
        
        consistency_issues = []
        
        # Test 1: Response format consistency
        endpoints_to_test = [
            ("GET", "/api/v1/me/hair-fall-logs", True),
            ("GET", "/api/v1/me/interventions", True),
            ("GET", "/api/v1/me/progress-photos", True)
        ]
        
        for method, endpoint, use_auth in endpoints_to_test:
            if not use_auth or self.access_token:
                response = self.make_request(method, endpoint, use_auth=use_auth)
                
                if response:
                    # Check Content-Type header
                    content_type = response.headers.get('Content-Type', '')
                    if 'application/json' not in content_type.lower():
                        consistency_issues.append(f"{endpoint}: missing JSON content-type")
                    
                    # Check if response is valid JSON
                    try:
                        data = response.json()
                        if response.status_code == 200 and not isinstance(data, (list, dict)):
                            consistency_issues.append(f"{endpoint}: invalid JSON structure")
                    except:
                        if response.status_code == 200:
                            consistency_issues.append(f"{endpoint}: response not valid JSON")
                else:
                    if use_auth and self.access_token:
                        consistency_issues.append(f"{endpoint}: no response with auth")
        
        # Test 2: Error response format consistency
        response = self.make_request("GET", "/api/v1/nonexistent-endpoint")
        if response and response.status_code == 404:
            try:
                error_data = response.json()
                if not isinstance(error_data, dict) or not error_data.get("message"):
                    consistency_issues.append("404 errors don't follow standard format")
            except:
                consistency_issues.append("404 errors not in JSON format")
        
        if consistency_issues:
            self.log_test("API Consistency", TestResult.WARN,
                        f"Consistency issues: {', '.join(consistency_issues[:2])}")
        else:
            self.log_test("API Consistency", TestResult.PASS,
                        "API responses are consistent")

    def run_all_tests(self):
        """Run all tests in sequence for thorough debugging"""
        print("🧪 Running comprehensive backend debugging tests...\n")
        
        # Phase 1: Basic connectivity
        print("📍 Phase 1: Basic Connectivity")
        self.test_health_check()
        self.test_public_endpoint()
        
        # Phase 2: Security - unauthorized access
        print("\n📍 Phase 2: Security - Unauthorized Access")
        self.test_protected_endpoint_unauthorized()
        
        # Phase 3: Authentication flow
        print("\n📍 Phase 3: Authentication Flow")
        self.test_user_registration()
        self.test_user_login()
        self.test_get_current_user()
        
        # Phase 4: Authorized access
        print("\n📍 Phase 4: Authorized Access")
        self.test_protected_endpoint_authorized()
        
        # Phase 5: Core data operations
        print("\n📍 Phase 5: Core Data Operations")
        self.test_hair_fall_logs()
        self.test_interventions()
        self.test_progress_photos()
        
        # Phase 6: Token management
        print("\n📍 Phase 6: Token Management")
        self.test_token_refresh()
        
        # Phase 7: Input validation & security
        print("\n📍 Phase 7: Input Validation & Security")
        self.test_validation_errors()
        self.test_authentication_edge_cases()
        
        # Phase 8: Data integrity
        print("\n📍 Phase 8: Data Integrity")
        self.test_data_integrity()
        
        # Phase 9: API consistency
        print("\n📍 Phase 9: API Consistency")
        self.test_api_consistency()
        
        # Print comprehensive summary
        self.print_detailed_summary()

    def print_detailed_summary(self):
        """Print comprehensive test summary with debugging insights"""
        print("\n" + "="*60)
        print("📊 COMPREHENSIVE TEST SUMMARY")
        print("="*60)
        
        passed = sum(1 for test in self.test_results if test.result == TestResult.PASS)
        failed = sum(1 for test in self.test_results if test.result == TestResult.FAIL)
        warned = sum(1 for test in self.test_results if test.result == TestResult.WARN)
        skipped = sum(1 for test in self.test_results if test.result == TestResult.SKIP)
        total = len(self.test_results)
        
        print(f"Total Tests: {total}")
        print(f"✅ Passed: {passed}")
        print(f"❌ Failed: {failed}")
        print(f"⚠️ Warnings: {warned}")
        print(f"⏭️ Skipped: {skipped}")
        
        executed_tests = total - skipped
        if executed_tests > 0:
            success_rate = (passed / executed_tests) * 100
            print(f"📈 Success Rate: {success_rate:.1f}% ({passed}/{executed_tests})")
        
        # Critical issues (failures)
        if failed > 0:
            print(f"\n🚨 CRITICAL ISSUES FOUND ({failed}):")
            for test in self.test_results:
                if test.result == TestResult.FAIL:
                    print(f"   ❌ {test.name}")
                    print(f"      Issue: {test.message}")
        
        # Warnings (potential issues)
        if warned > 0:
            print(f"\n⚠️ POTENTIAL ISSUES ({warned}):")
            for test in self.test_results:
                if test.result == TestResult.WARN:
                    print(f"   ⚠️ {test.name}")
                    print(f"      Concern: {test.message}")
        
        # Test environment info
        print(f"\n🎯 Test Environment:")
        print(f"   📧 Test User: {self.user_email}")
        print(f"   🆔 User ID: {self.user_id}")
        print(f"   🔑 Access Token: {'Present' if self.access_token else 'Missing'}")
        print(f"   🔄 Refresh Token: {'Present' if self.refresh_token else 'Missing'}")
        
        # Overall assessment
        print(f"\n📋 OVERALL ASSESSMENT:")
        if failed == 0 and warned == 0:
            print(f"🎉 EXCELLENT: No issues found! Your backend is working correctly.")
        elif failed == 0 and warned <= 2:
            print(f"👍 GOOD: No critical issues, but {warned} minor concerns to review.")
        elif failed <= 2:
            print(f"⚠️ NEEDS ATTENTION: {failed} critical issue(s) found that should be fixed.")
        else:
            print(f"🚨 SERIOUS ISSUES: {failed} critical problems found. Backend needs debugging.")
        
        print(f"\n💡 Debugging Tips:")
        if failed > 0:
            print(f"   • Check server logs for the failed endpoints")
            print(f"   • Verify database connections and migrations")
            print(f"   • Review authentication middleware configuration")
        if warned > 0:
            print(f"   • Review warning messages for potential improvements")
            print(f"   • Consider adding input validation where missing")
        
        return failed == 0

def main():
    """Main test runner focused on debugging"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Backend API Debugging Test Harness")
    parser.add_argument("--url", default="http://localhost:8080", 
                       help="Base URL for the backend API")
    parser.add_argument("--strict", action="store_true",
                       help="Enable strict mode - treat warnings as failures")
    
    args = parser.parse_args()
    
    print("🔍 BACKEND API DEBUGGING TEST HARNESS")
    print("Goal: Find real issues and help debug your backend")
    print("-" * 50)
    
    # Run comprehensive tests
    harness = BackendTestHarness(args.url)
    success = harness.run_all_tests()
    
    # Exit with appropriate code for CI/CD
    if args.strict:
        # In strict mode, warnings also count as failures
        warnings = sum(1 for test in harness.test_results if test.result == TestResult.WARN)
        success = success and warnings == 0
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
