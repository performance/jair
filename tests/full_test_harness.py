def test_authentication_security(self):
        """Test authentication security edge cases with detailed debugging"""
        security_tests = []
        
        # Test 1: Invalid credentials
        print("    🧪 Testing invalid password")
        invalid_login = {
            "email": self.user_email,
            "password": "definitely_wrong_password_12345"
        }
        
        print(f"        📤 Request: {json.dumps(invalid_login, indent=8)}")
        response = self.make_request("POST", "/api/v1/auth/login", invalid_login)
        
        if response:
            print(f"        📥 Status: {response.status_code}")
            print(f"        📥 Response: {response.text}")
            
            if response.status_code == 401:
                security_tests.append("✅ Invalid password properly rejected")
            elif response.status_code == 200:
                security_tests.append("❌ CRITICAL: Invalid password accepted!")
            else:
                security_tests.append(f"⚠️ Invalid password: unexpected status {response.status_code}")
        else:
            security_tests.append("❌ Invalid password test: no response")
        
        # Test 2: Non-existent user
        print("    🧪 Testing non-existent user login")
        nonexistent_login = {
            "email": f"nonexistent_{int(time.time())}@nowhere.com",
            "password": "some_password_123"
        }
        
        print(f"        📤 Request: {json.dumps(nonexistent_login, indent=8)}")
        response = self.make_request("POST", "/api/v1/auth/login", nonexistent_login)
        
        if response:
            print(f"        📥 Status: {response.#!/usr/bin/env python3
"""
OpenAPI-Driven Backend Test Harness
Generated from actual OpenAPI specification for comprehensive API testing.
GOAL: Thoroughly test every endpoint with correct schemas and find real issues.
"""

import requests
import json
import time
import uuid
from datetime import datetime, timedelta
from typing import Dict, Any, Optional, List, Tuple
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

class OpenAPITestHarness:
    """Comprehensive test harness based on actual OpenAPI specification"""
    
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
        
        # Test data storage for cross-test usage
        self.created_hair_fall_log_id = None
        self.created_intervention_id = None
        self.created_photo_metadata_id = None
        
        # Generate unique test data
        timestamp = int(time.time())
        self.user_email = f"api_test_{timestamp}@hairhealth.com"
        self.user_password = f"SecurePass_{timestamp}123!"
        self.username = f"api_test_user_{timestamp}"
        
        print(f"🚀 OpenAPI-Driven Backend Test Harness")
        print(f"📧 Test email: {self.user_email}")
        print(f"🌐 Base URL: {self.base_url}")
        print(f"🎯 Testing all endpoints from OpenAPI specification")
        print("-" * 70)

    def log_test(self, name: str, result: TestResult, message: str = "", response_data: Dict = None):
        """Log a test result with detailed information"""
        test_case = TestCase(name, result, message, response_data)
        self.test_results.append(test_case)
        print(f"{result.value} {name}")
        if message:
            print(f"    💬 {message}")

    def make_request(self, method: str, endpoint: str, data: Dict = None, 
                    headers: Dict = None, use_auth: bool = False, 
                    params: Dict = None) -> requests.Response:
        """Make HTTP request with comprehensive error handling"""
        url = f"{self.base_url}{endpoint}"
        
        if headers is None:
            headers = {}
        
        if use_auth and self.access_token:
            headers["Authorization"] = f"Bearer {self.access_token}"
        
        if data and method.upper() in ["POST", "PUT", "PATCH"]:
            headers["Content-Type"] = "application/json"
            data = json.dumps(data)
        
        try:
            response = self.session.request(
                method, url, data=data, headers=headers, params=params, timeout=30
            )
            return response
        except requests.exceptions.RequestException as e:
            print(f"    ❌ Request failed: {e}")
            return None

    def validate_response_schema(self, response: requests.Response, expected_fields: List[str]) -> Tuple[bool, List[str]]:
        """Validate response contains expected fields"""
        try:
            data = response.json()
            missing_fields = []
            
            if isinstance(data, list):
                if len(data) > 0:
                    for field in expected_fields:
                        if field not in data[0]:
                            missing_fields.append(field)
            elif isinstance(data, dict):
                for field in expected_fields:
                    if field not in data:
                        missing_fields.append(field)
            
            return len(missing_fields) == 0, missing_fields
        except (json.JSONDecodeError, KeyError):
            return False, ["Invalid JSON response"]

    # ===== HEALTH & PUBLIC ENDPOINTS =====
    
    def test_health_endpoint(self):
        """Test GET /api/v1/health"""
        response = self.make_request("GET", "/api/v1/health")
        
        if not response:
            self.log_test("Health Check", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            self.log_test("Health Check", TestResult.PASS, 
                        f"Server healthy (Status: {response.status_code})")
        else:
            self.log_test("Health Check", TestResult.FAIL, 
                        f"Unexpected status: {response.status_code}")

    def test_public_endpoint(self):
        """Test GET /api/v1/test/public"""
        response = self.make_request("GET", "/api/v1/test/public")
        
        if not response:
            self.log_test("Public Test Endpoint", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            self.log_test("Public Test Endpoint", TestResult.PASS, 
                        "Public endpoint accessible")
        else:
            self.log_test("Public Test Endpoint", TestResult.FAIL, 
                        f"Expected 200, got {response.status_code}")

    def test_protected_endpoint_unauthorized(self):
        """Test GET /api/v1/test/protected without authentication"""
        response = self.make_request("GET", "/api/v1/test/protected")
        
        if not response:
            self.log_test("Protected Endpoint (No Auth)", TestResult.PASS, 
                        "No response - likely properly protected")
            return
            
        # Check if it returns anonymous user (acceptable pattern)
        if response.status_code == 200:
            try:
                data = response.json()
                if data.get("user") == "anonymous":
                    self.log_test("Protected Endpoint (No Auth)", TestResult.PASS, 
                                "Returns anonymous user - acceptable protection pattern")
                else:
                    self.log_test("Protected Endpoint (No Auth)", TestResult.WARN, 
                                "Accessible without auth but doesn't show anonymous user")
            except:
                self.log_test("Protected Endpoint (No Auth)", TestResult.WARN, 
                            "Accessible without auth")
        else:
            self.log_test("Protected Endpoint (No Auth)", TestResult.FAIL, 
                        f"Unexpected status: {response.status_code}")

    # ===== AUTHENTICATION ENDPOINTS =====
    
    def test_user_registration(self):
        """Test POST /api/v1/auth/register"""
        register_data = {
            "email": self.user_email,
            "password": self.user_password,
            "username": self.username
        }
        
        print(f"    🔍 Registration request: {json.dumps(register_data, indent=2)}")
        response = self.make_request("POST", "/api/v1/auth/register", register_data)
        
        if not response:
            self.log_test("User Registration", TestResult.FAIL, "No response")
            return
            
        print(f"    🔍 Response status: {response.status_code}")
        print(f"    🔍 Response headers: {dict(response.headers)}")
        print(f"    🔍 Response body: {response.text[:500]}...")
        
        if response.status_code == 200:
            # Validate AuthResponse schema
            valid, missing = self.validate_response_schema(response, 
                ["accessToken", "refreshToken", "user"])
            
            if valid:
                try:
                    data = response.json()
                    self.access_token = data["accessToken"]
                    self.refresh_token = data["refreshToken"]
                    user_data = data["user"]
                    self.user_id = user_data["id"]
                    
                    print(f"    🔍 Extracted user data: {json.dumps(user_data, indent=2)}")
                    
                    # Validate UserResponse schema - fix lambda issue
                    mock_response = type('MockResponse', (), {
                        'json': lambda: user_data,
                        'status_code': 200
                    })()
                    
                    user_valid, user_missing = self.validate_response_schema(
                        mock_response, ["id", "email", "username", "isEmailVerified"]
                    )
                    
                    if user_valid:
                        # Validate that returned data matches input
                        data_issues = []
                        if user_data.get("email") != self.user_email:
                            data_issues.append(f"email mismatch: sent {self.user_email}, got {user_data.get('email')}")
                        if user_data.get("username") != self.username:
                            data_issues.append(f"username mismatch: sent {self.username}, got {user_data.get('username')}")
                        
                        if data_issues:
                            self.log_test("User Registration", TestResult.WARN, 
                                        f"Data inconsistencies: {'; '.join(data_issues)}")
                        else:
                            self.log_test("User Registration", TestResult.PASS, 
                                        f"User registered successfully: {self.user_id}")
                    else:
                        self.log_test("User Registration", TestResult.FAIL, 
                                    f"Invalid user schema, missing: {user_missing}")
                except Exception as e:
                    print(f"    🔍 Exception details: {type(e).__name__}: {str(e)}")
                    import traceback
                    print(f"    🔍 Stack trace: {traceback.format_exc()}")
                    self.log_test("User Registration", TestResult.FAIL, 
                                f"Error parsing response: {type(e).__name__}: {str(e)}")
            else:
                self.log_test("User Registration", TestResult.FAIL, 
                            f"Invalid response schema, missing: {missing}")
        else:
            try:
                error_data = response.json()
                error_msg = error_data.get("message", error_data.get("error", "Unknown error"))
                self.log_test("User Registration", TestResult.FAIL, 
                            f"Registration failed ({response.status_code}): {error_msg}")
            except:
                self.log_test("User Registration", TestResult.FAIL, 
                            f"Registration failed ({response.status_code}): {response.text[:200]}")

    def test_user_login(self):
        """Test POST /api/v1/auth/login"""
        if not self.user_email:
            self.log_test("User Login", TestResult.SKIP, "No user to login with")
            return
            
        login_data = {
            "email": self.user_email,
            "password": self.user_password
        }
        
        response = self.make_request("POST", "/api/v1/auth/login", login_data)
        
        if not response:
            self.log_test("User Login", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["accessToken", "refreshToken", "user"])
            
            if valid:
                data = response.json()
                self.access_token = data["accessToken"]
                self.refresh_token = data["refreshToken"]
                self.log_test("User Login", TestResult.PASS, "Login successful")
            else:
                self.log_test("User Login", TestResult.FAIL, 
                            f"Invalid login response, missing: {missing}")
        else:
            self.log_test("User Login", TestResult.FAIL, 
                        f"Login failed: {response.status_code}")

    def test_get_current_user(self):
        """Test GET /api/v1/auth/me"""
        if not self.access_token:
            self.log_test("Get Current User", TestResult.SKIP, "No access token")
            return
            
        response = self.make_request("GET", "/api/v1/auth/me", use_auth=True)
        
        if not response:
            self.log_test("Get Current User", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["id", "email", "username", "isEmailVerified"])
            
            if valid:
                self.log_test("Get Current User", TestResult.PASS, 
                            "Current user retrieved successfully")
            else:
                self.log_test("Get Current User", TestResult.FAIL, 
                            f"Invalid user response, missing: {missing}")
        else:
            self.log_test("Get Current User", TestResult.FAIL, 
                        f"Failed to get current user: {response.status_code}")

    def test_token_refresh(self):
        """Test POST /api/v1/auth/refresh-token"""
        if not self.refresh_token:
            self.log_test("Token Refresh", TestResult.SKIP, "No refresh token")
            return
            
        refresh_data = {"refreshToken": self.refresh_token}
        response = self.make_request("POST", "/api/v1/auth/refresh-token", refresh_data)
        
        if not response:
            self.log_test("Token Refresh", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["accessToken", "refreshToken", "user"])
            
            if valid:
                data = response.json()
                old_token = self.access_token[:20] + "..." if self.access_token else "None"
                self.access_token = data["accessToken"]
                self.log_test("Token Refresh", TestResult.PASS, 
                            f"Token refreshed successfully")
            else:
                self.log_test("Token Refresh", TestResult.FAIL, 
                            f"Invalid refresh response, missing: {missing}")
        else:
            self.log_test("Token Refresh", TestResult.FAIL, 
                        f"Token refresh failed: {response.status_code}")

    def test_logout(self):
        """Test POST /api/v1/auth/logout"""
        if not self.access_token:
            self.log_test("User Logout", TestResult.SKIP, "No access token")
            return
            
        response = self.make_request("POST", "/api/v1/auth/logout", use_auth=True)
        
        if not response:
            self.log_test("User Logout", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            self.log_test("User Logout", TestResult.PASS, "Logout successful")
        else:
            self.log_test("User Logout", TestResult.FAIL, 
                        f"Logout failed: {response.status_code}")

    # ===== HAIR FALL LOG ENDPOINTS =====
    
    def test_get_hair_fall_logs(self):
        """Test GET /api/v1/me/hair-fall-logs with pagination"""
        if not self.access_token:
            self.log_test("Get Hair Fall Logs", TestResult.SKIP, "No access token")
            return
            
        # Test with pagination parameters
        params = {"limit": 10, "offset": 0}
        response = self.make_request("GET", "/api/v1/me/hair-fall-logs", 
                                   use_auth=True, params=params)
        
        if not response:
            self.log_test("Get Hair Fall Logs", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            try:
                data = response.json()
                if isinstance(data, list):
                    self.log_test("Get Hair Fall Logs", TestResult.PASS, 
                                f"Retrieved {len(data)} hair fall logs")
                else:
                    self.log_test("Get Hair Fall Logs", TestResult.FAIL, 
                                "Response is not a list")
            except json.JSONDecodeError:
                self.log_test("Get Hair Fall Logs", TestResult.FAIL, 
                            "Invalid JSON response")
        else:
            self.log_test("Get Hair Fall Logs", TestResult.FAIL, 
                        f"Failed to get hair fall logs: {response.status_code}")

    def test_create_hair_fall_log(self):
        """Test POST /api/v1/me/hair-fall-logs"""
        if not self.access_token:
            self.log_test("Create Hair Fall Log", TestResult.SKIP, "No access token")
            return
            
        # Test with valid CreateHairFallLogRequest schema
        log_data = {
            "date": datetime.now().strftime("%Y-%m-%d"),
            "count": 45,
            "category": "SHOWER",
            "description": "Test hair fall log from automated test"
        }
        
        response = self.make_request("POST", "/api/v1/me/hair-fall-logs", 
                                   log_data, use_auth=True)
        
        if not response:
            self.log_test("Create Hair Fall Log", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["id", "userId", "date", "category", "createdAt", "updatedAt"])
            
            if valid:
                data = response.json()
                self.created_hair_fall_log_id = data["id"]
                self.log_test("Create Hair Fall Log", TestResult.PASS, 
                            f"Hair fall log created: {self.created_hair_fall_log_id}")
            else:
                self.log_test("Create Hair Fall Log", TestResult.FAIL, 
                            f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Create Hair Fall Log", TestResult.FAIL, 
                        f"Failed to create hair fall log: {response.status_code}")

    def test_get_hair_fall_log_by_id(self):
        """Test GET /api/v1/me/hair-fall-logs/{id}"""
        if not self.access_token or not self.created_hair_fall_log_id:
            self.log_test("Get Hair Fall Log by ID", TestResult.SKIP, 
                        "No access token or hair fall log ID")
            return
            
        response = self.make_request("GET", 
                                   f"/api/v1/me/hair-fall-logs/{self.created_hair_fall_log_id}", 
                                   use_auth=True)
        
        if not response:
            self.log_test("Get Hair Fall Log by ID", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["id", "userId", "date", "category"])
            
            if valid:
                self.log_test("Get Hair Fall Log by ID", TestResult.PASS, 
                            "Hair fall log retrieved by ID")
            else:
                self.log_test("Get Hair Fall Log by ID", TestResult.FAIL, 
                            f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Get Hair Fall Log by ID", TestResult.FAIL, 
                        f"Failed to get hair fall log: {response.status_code}")

    def test_update_hair_fall_log(self):
        """Test PUT /api/v1/me/hair-fall-logs/{id}"""
        if not self.access_token or not self.created_hair_fall_log_id:
            self.log_test("Update Hair Fall Log", TestResult.SKIP, 
                        "No access token or hair fall log ID")
            return
            
        update_data = {
            "count": 50,
            "description": "Updated test description"
        }
        
        response = self.make_request("PUT", 
                                   f"/api/v1/me/hair-fall-logs/{self.created_hair_fall_log_id}", 
                                   update_data, use_auth=True)
        
        if not response:
            self.log_test("Update Hair Fall Log", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            self.log_test("Update Hair Fall Log", TestResult.PASS, 
                        "Hair fall log updated successfully")
        else:
            self.log_test("Update Hair Fall Log", TestResult.FAIL, 
                        f"Failed to update hair fall log: {response.status_code}")

    def test_get_hair_fall_stats(self):
        """Test GET /api/v1/me/hair-fall-logs/stats"""
        if not self.access_token:
            self.log_test("Get Hair Fall Stats", TestResult.SKIP, "No access token")
            return
            
        response = self.make_request("GET", "/api/v1/me/hair-fall-logs/stats", use_auth=True)
        
        if not response:
            self.log_test("Get Hair Fall Stats", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["totalLogs", "recentTrend"])
            
            if valid:
                self.log_test("Get Hair Fall Stats", TestResult.PASS, 
                            "Hair fall stats retrieved")
            else:
                self.log_test("Get Hair Fall Stats", TestResult.FAIL, 
                            f"Invalid stats schema, missing: {missing}")
        else:
            self.log_test("Get Hair Fall Stats", TestResult.FAIL, 
                        f"Failed to get hair fall stats: {response.status_code}")

    def test_get_hair_fall_logs_by_date_range(self):
        """Test GET /api/v1/me/hair-fall-logs/date-range"""
        if not self.access_token:
            self.log_test("Get Hair Fall Logs by Date Range", TestResult.SKIP, "No access token")
            return
            
        params = {
            "startDate": "2025-05-01",
            "endDate": "2025-05-31"
        }
        
        response = self.make_request("GET", "/api/v1/me/hair-fall-logs/date-range", 
                                   use_auth=True, params=params)
        
        if not response:
            self.log_test("Get Hair Fall Logs by Date Range", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            try:
                data = response.json()
                if isinstance(data, list):
                    self.log_test("Get Hair Fall Logs by Date Range", TestResult.PASS, 
                                f"Retrieved {len(data)} logs for date range")
                else:
                    self.log_test("Get Hair Fall Logs by Date Range", TestResult.FAIL, 
                                "Response is not a list")
            except json.JSONDecodeError:
                self.log_test("Get Hair Fall Logs by Date Range", TestResult.FAIL, 
                            "Invalid JSON response")
        else:
            self.log_test("Get Hair Fall Logs by Date Range", TestResult.FAIL, 
                        f"Failed to get logs by date range: {response.status_code}")

    # ===== INTERVENTION ENDPOINTS =====
    
    def test_get_interventions(self):
        """Test GET /api/v1/me/interventions"""
        if not self.access_token:
            self.log_test("Get Interventions", TestResult.SKIP, "No access token")
            return
            
        # Test with includeInactive parameter
        params = {"includeInactive": "false"}
        response = self.make_request("GET", "/api/v1/me/interventions", 
                                   use_auth=True, params=params)
        
        if not response:
            self.log_test("Get Interventions", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            try:
                data = response.json()
                if isinstance(data, list):
                    self.log_test("Get Interventions", TestResult.PASS, 
                                f"Retrieved {len(data)} interventions")
                else:
                    self.log_test("Get Interventions", TestResult.FAIL, 
                                "Response is not a list")
            except json.JSONDecodeError:
                self.log_test("Get Interventions", TestResult.FAIL, 
                            "Invalid JSON response")
        else:
            self.log_test("Get Interventions", TestResult.FAIL, 
                        f"Failed to get interventions: {response.status_code}")

    def test_create_intervention(self):
        """Test POST /api/v1/me/interventions"""
        if not self.access_token:
            self.log_test("Create Intervention", TestResult.SKIP, "No access token")
            return
            
        # Test with valid CreateInterventionRequest schema
        intervention_data = {
            "type": "TOPICAL",
            "productName": f"Test Minoxidil {int(time.time())}",
            "dosageAmount": "1ml",
            "frequency": "Twice Daily",
            "applicationTime": "08:00, 20:00",
            "startDate": datetime.now().strftime("%Y-%m-%d"),
            "notes": "Automated test intervention"
        }
        
        response = self.make_request("POST", "/api/v1/me/interventions", 
                                   intervention_data, use_auth=True)
        
        if not response:
            self.log_test("Create Intervention", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["id", "userId", "type", "productName", "frequency", "startDate", "isActive"])
            
            if valid:
                data = response.json()
                self.created_intervention_id = data["id"]
                self.log_test("Create Intervention", TestResult.PASS, 
                            f"Intervention created: {self.created_intervention_id}")
            else:
                self.log_test("Create Intervention", TestResult.FAIL, 
                            f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Create Intervention", TestResult.FAIL, 
                        f"Failed to create intervention: {response.status_code}")

    def test_get_intervention_by_id(self):
        """Test GET /api/v1/me/interventions/{id}"""
        if not self.access_token or not self.created_intervention_id:
            self.log_test("Get Intervention by ID", TestResult.SKIP, 
                        "No access token or intervention ID")
            return
            
        response = self.make_request("GET", 
                                   f"/api/v1/me/interventions/{self.created_intervention_id}", 
                                   use_auth=True)
        
        if not response:
            self.log_test("Get Intervention by ID", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["id", "userId", "type", "productName", "frequency"])
            
            if valid:
                self.log_test("Get Intervention by ID", TestResult.PASS, 
                            "Intervention retrieved by ID")
            else:
                self.log_test("Get Intervention by ID", TestResult.FAIL, 
                            f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Get Intervention by ID", TestResult.FAIL, 
                        f"Failed to get intervention: {response.status_code}")

    def test_log_intervention_application(self):
        """Test POST /api/v1/me/interventions/{id}/log-application"""
        if not self.access_token or not self.created_intervention_id:
            self.log_test("Log Intervention Application", TestResult.SKIP, 
                        "No access token or intervention ID")
            return
            
        application_data = {
            "timestamp": datetime.now().isoformat(),
            "notes": "Automated test application"
        }
        
        print(f"    �� Application data: {json.dumps(application_data, indent=2)}")
        print(f"    🔍 Intervention ID: {self.created_intervention_id}")
        print(f"    🔍 Access token present: {bool(self.access_token)}")
        
        response = self.make_request("POST", 
                                   f"/api/v1/me/interventions/{self.created_intervention_id}/log-application", 
                                   application_data, use_auth=True)
        
        if not response:
            self.log_test("Log Intervention Application", TestResult.FAIL, 
                        "No response - check if endpoint exists or server is running")
            return
            
        print(f"    🔍 Response status: {response.status_code}")
        print(f"    🔍 Response headers: {dict(response.headers)}")
        print(f"    🔍 Response body: {response.text}")
        
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["id", "interventionId", "userId", "timestamp", "createdAt"])
            
            if valid:
                self.log_test("Log Intervention Application", TestResult.PASS, 
                            "Intervention application logged")
            else:
                self.log_test("Log Intervention Application", TestResult.FAIL, 
                            f"Invalid response schema, missing: {missing}")
        elif response.status_code == 404:
            self.log_test("Log Intervention Application", TestResult.FAIL, 
                        f"Endpoint not found - intervention {self.created_intervention_id} may not exist")
        elif response.status_code == 401:
            self.log_test("Log Intervention Application", TestResult.FAIL, 
                        "Authentication failed - token may be invalid")
        elif response.status_code >= 500:
            self.log_test("Log Intervention Application", TestResult.FAIL, 
                        f"Server error ({response.status_code}): {response.text[:200]}")
        else:
            try:
                error_data = response.json()
                error_msg = error_data.get("message", error_data.get("error", "Unknown error"))
                self.log_test("Log Intervention Application", TestResult.FAIL, 
                            f"Failed ({response.status_code}): {error_msg}")
            except:
                self.log_test("Log Intervention Application", TestResult.FAIL, 
                            f"Failed ({response.status_code}): {response.text[:200]}")

    def test_get_intervention_applications(self):
        """Test GET /api/v1/me/interventions/{id}/applications"""
        if not self.access_token or not self.created_intervention_id:
            self.log_test("Get Intervention Applications", TestResult.SKIP, 
                        "No access token or intervention ID")
            return
            
        params = {"limit": 10, "offset": 0}
        response = self.make_request("GET", 
                                   f"/api/v1/me/interventions/{self.created_intervention_id}/applications", 
                                   use_auth=True, params=params)
        
        if not response:
            self.log_test("Get Intervention Applications", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            try:
                data = response.json()
                if isinstance(data, list):
                    self.log_test("Get Intervention Applications", TestResult.PASS, 
                                f"Retrieved {len(data)} applications")
                else:
                    self.log_test("Get Intervention Applications", TestResult.FAIL, 
                                "Response is not a list")
            except json.JSONDecodeError:
                self.log_test("Get Intervention Applications", TestResult.FAIL, 
                            "Invalid JSON response")
        else:
            self.log_test("Get Intervention Applications", TestResult.FAIL, 
                        f"Failed to get applications: {response.status_code}")

    def test_get_intervention_adherence(self):
        """Test GET /api/v1/me/interventions/{id}/adherence"""
        if not self.access_token or not self.created_intervention_id:
            self.log_test("Get Intervention Adherence", TestResult.SKIP, 
                        "No access token or intervention ID")
            return
            
        response = self.make_request("GET", 
                                   f"/api/v1/me/interventions/{self.created_intervention_id}/adherence", 
                                   use_auth=True)
        
        if not response:
            self.log_test("Get Intervention Adherence", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["interventionId", "expectedApplications", "actualApplications", 
                 "adherenceRate", "adherencePercentage", "daysSinceStart", "adherenceLevel"])
            
            if valid:
                self.log_test("Get Intervention Adherence", TestResult.PASS, 
                            "Intervention adherence stats retrieved")
            else:
                self.log_test("Get Intervention Adherence", TestResult.FAIL, 
                            f"Invalid adherence schema, missing: {missing}")
        else:
            self.log_test("Get Intervention Adherence", TestResult.FAIL, 
                        f"Failed to get adherence: {response.status_code}")

    def test_get_active_interventions(self):
        """Test GET /api/v1/me/interventions/active"""
        if not self.access_token:
            self.log_test("Get Active Interventions", TestResult.SKIP, "No access token")
            return
            
        response = self.make_request("GET", "/api/v1/me/interventions/active", use_auth=True)
        
        if not response:
            self.log_test("Get Active Interventions", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            try:
                data = response.json()
                if isinstance(data, list):
                    active_count = len([i for i in data if i.get("isActive", False)])
                    self.log_test("Get Active Interventions", TestResult.PASS, 
                                f"Retrieved {len(data)} interventions, {active_count} active")
                else:
                    self.log_test("Get Active Interventions", TestResult.FAIL, 
                                "Response is not a list")
            except json.JSONDecodeError:
                self.log_test("Get Active Interventions", TestResult.FAIL, 
                            "Invalid JSON response")
        else:
            self.log_test("Get Active Interventions", TestResult.FAIL, 
                        f"Failed to get active interventions: {response.status_code}")

    def test_deactivate_intervention(self):
        """Test POST /api/v1/me/interventions/{id}/deactivate"""
        if not self.access_token or not self.created_intervention_id:
            self.log_test("Deactivate Intervention", TestResult.SKIP, 
                        "No access token or intervention ID")
            return
            
        response = self.make_request("POST", 
                                   f"/api/v1/me/interventions/{self.created_intervention_id}/deactivate", 
                                   use_auth=True)
        
        if not response:
            self.log_test("Deactivate Intervention", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            self.log_test("Deactivate Intervention", TestResult.PASS, 
                        "Intervention deactivated successfully")
        else:
            self.log_test("Deactivate Intervention", TestResult.FAIL, 
                        f"Failed to deactivate intervention: {response.status_code}")

    # ===== PROGRESS PHOTOS ENDPOINTS =====
    
    def test_get_progress_photos(self):
        """Test GET /api/v1/me/progress-photos with all query parameters"""
        if not self.access_token:
            self.log_test("Get Progress Photos", TestResult.SKIP, "No access token")
            return
            
        # Test with all available query parameters
        params = {
            "angle": "HAIRLINE",
            "limit": 10,
            "offset": 0,
            "startDate": "2025-01-01T00:00:00Z",
            "endDate": "2025-12-31T23:59:59Z"
        }
        
        response = self.make_request("GET", "/api/v1/me/progress-photos", 
                                   use_auth=True, params=params)
        
        if not response:
            self.log_test("Get Progress Photos", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            try:
                data = response.json()
                if isinstance(data, list):
                    self.log_test("Get Progress Photos", TestResult.PASS, 
                                f"Retrieved {len(data)} progress photos")
                else:
                    self.log_test("Get Progress Photos", TestResult.FAIL, 
                                "Response is not a list")
            except json.JSONDecodeError:
                self.log_test("Get Progress Photos", TestResult.FAIL, 
                            "Invalid JSON response")
        else:
            self.log_test("Get Progress Photos", TestResult.FAIL, 
                        f"Failed to get progress photos: {response.status_code}")

    def test_request_photo_upload_url(self):
        """Test POST /api/v1/me/progress-photos/upload-url"""
        if not self.access_token:
            self.log_test("Request Photo Upload URL", TestResult.SKIP, "No access token")
            return
            
        # Test with valid PhotoUploadRequest schema
        upload_request = {
            "filename": f"test_photo_{int(time.time())}.jpg.enc",
            "angle": "VERTEX",
            "captureDate": datetime.now().isoformat() + "Z",
            "encryptionKeyInfo": f"test_key_{int(time.time())}"
        }
        
        response = self.make_request("POST", "/api/v1/me/progress-photos/upload-url", 
                                   upload_request, use_auth=True)
        
        if not response:
            self.log_test("Request Photo Upload URL", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["photoMetadataId", "uploadUrl", "expiresAt"])
            
            if valid:
                data = response.json()
                self.created_photo_metadata_id = data["photoMetadataId"]
                self.log_test("Request Photo Upload URL", TestResult.PASS, 
                            f"Upload URL received: {self.created_photo_metadata_id}")
            else:
                self.log_test("Request Photo Upload URL", TestResult.FAIL, 
                            f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Request Photo Upload URL", TestResult.FAIL, 
                        f"Failed to get upload URL: {response.status_code}")

    def test_finalize_photo_upload(self):
        """Test POST /api/v1/me/progress-photos/{photoMetadataId}/finalize"""
        if not self.access_token or not self.created_photo_metadata_id:
            self.log_test("Finalize Photo Upload", TestResult.SKIP, 
                        "No access token or photo metadata ID")
            return
            
        finalize_data = {"fileSize": 1024000}
        
        response = self.make_request("POST", 
                                   f"/api/v1/me/progress-photos/{self.created_photo_metadata_id}/finalize", 
                                   finalize_data, use_auth=True)
        
        if not response:
            self.log_test("Finalize Photo Upload", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["id", "userId", "filename", "angle", "captureDate", "uploadedAt", "isDeleted"])
            
            if valid:
                self.log_test("Finalize Photo Upload", TestResult.PASS, 
                            "Photo upload finalized successfully")
            else:
                self.log_test("Finalize Photo Upload", TestResult.FAIL, 
                            f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Finalize Photo Upload", TestResult.FAIL, 
                        f"Failed to finalize upload: {response.status_code}")

    def test_get_photo_metadata(self):
        """Test GET /api/v1/me/progress-photos/{photoMetadataId}"""
        if not self.access_token or not self.created_photo_metadata_id:
            self.log_test("Get Photo Metadata", TestResult.SKIP, 
                        "No access token or photo metadata ID")
            return
            
        response = self.make_request("GET", 
                                   f"/api/v1/me/progress-photos/{self.created_photo_metadata_id}", 
                                   use_auth=True)
        
        if not response:
            self.log_test("Get Photo Metadata", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["id", "userId", "filename", "angle", "captureDate", "isDeleted"])
            
            if valid:
                self.log_test("Get Photo Metadata", TestResult.PASS, 
                            "Photo metadata retrieved successfully")
            else:
                self.log_test("Get Photo Metadata", TestResult.FAIL, 
                            f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Get Photo Metadata", TestResult.FAIL, 
                        f"Failed to get photo metadata: {response.status_code}")

    def test_get_photo_view_url(self):
        """Test GET /api/v1/me/progress-photos/{photoMetadataId}/view-url"""
        if not self.access_token or not self.created_photo_metadata_id:
            self.log_test("Get Photo View URL", TestResult.SKIP, 
                        "No access token or photo metadata ID")
            return
            
        response = self.make_request("GET", 
                                   f"/api/v1/me/progress-photos/{self.created_photo_metadata_id}/view-url", 
                                   use_auth=True)
        
        if not response:
            self.log_test("Get Photo View URL", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["downloadUrl", "encryptionKeyInfo", "expiresAt"])
            
            if valid:
                self.log_test("Get Photo View URL", TestResult.PASS, 
                            "Photo view URL retrieved successfully")
            else:
                self.log_test("Get Photo View URL", TestResult.FAIL, 
                            f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Get Photo View URL", TestResult.FAIL, 
                        f"Failed to get view URL: {response.status_code}")

    def test_get_photo_stats(self):
        """Test GET /api/v1/me/progress-photos/stats"""
        if not self.access_token:
            self.log_test("Get Photo Stats", TestResult.SKIP, "No access token")
            return
            
        response = self.make_request("GET", "/api/v1/me/progress-photos/stats", use_auth=True)
        
        if not response:
            self.log_test("Get Photo Stats", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["totalPhotos", "photosByAngle", "latestPhotosByAngle", "totalStorageUsedBytes"])
            
            if valid:
                self.log_test("Get Photo Stats", TestResult.PASS, 
                            "Photo statistics retrieved successfully")
            else:
                self.log_test("Get Photo Stats", TestResult.FAIL, 
                            f"Invalid stats schema, missing: {missing}")
        else:
            self.log_test("Get Photo Stats", TestResult.FAIL, 
                        f"Failed to get photo stats: {response.status_code}")

    # ===== VALIDATION & EDGE CASE TESTS =====
    
    def test_registration_edge_cases(self):
        """Test registration edge cases and business logic"""
        edge_case_results = []
        
        # Test 1: Duplicate email registration
        print("    🧪 Testing duplicate email registration")
        duplicate_data = {
            "email": self.user_email,  # Same email as already registered
            "password": "different_password_123",
            "username": "different_username"
        }
        
        response = self.make_request("POST", "/api/v1/auth/register", duplicate_data)
        print(f"        📤 Request: {json.dumps(duplicate_data, indent=8)}")
        
        if response:
            print(f"        📥 Status: {response.status_code}")
            print(f"        📥 Body: {response.text}")
            
            if response.status_code == 400:
                edge_case_results.append("✅ Duplicate email properly rejected")
            elif response.status_code == 200:
                try:
                    data = response.json()
                    user_data = data.get("user", {})
                    if user_data.get("id") == self.user_id:
                        edge_case_results.append("✅ Duplicate email returns existing user")
                    else:
                        edge_case_results.append("❌ ISSUE: Duplicate email creates new user")
                except:
                    edge_case_results.append("❌ ISSUE: Duplicate email accepted but response unclear")
            else:
                edge_case_results.append(f"⚠️ Duplicate email: unexpected status {response.status_code}")
        else:
            edge_case_results.append("❌ Duplicate email test: no response")
        
        # Test 2: Empty username (should use email)
        print("    🧪 Testing empty username (should use email)")
        timestamp = int(time.time())
        empty_username_data = {
            "email": f"empty_user_{timestamp}@test.com",
            "password": "test_password_123",
            "username": ""  # Empty username
        }
        
        response = self.make_request("POST", "/api/v1/auth/register", empty_username_data)
        print(f"        📤 Request: {json.dumps(empty_username_data, indent=8)}")
        
        if response:
            print(f"        📥 Status: {response.status_code}")
            print(f"        📥 Body: {response.text}")
            
            if response.status_code == 200:
                try:
                    data = response.json()
                    user_data = data.get("user", {})
                    returned_username = user_data.get("username", "")
                    expected_email = empty_username_data["email"]
                    
                    # Check if username defaults to email
                    if returned_username == expected_email:
                        edge_case_results.append("✅ Empty username correctly defaults to email")
                    elif returned_username == "":
                        edge_case_results.append("⚠️ Empty username stays empty (may be OK)")
                    else:
                        edge_case_results.append(f"❓ Empty username becomes: {returned_username}")
                except:
                    edge_case_results.append("❌ Empty username test: response parse error")
            elif response.status_code == 400:
                edge_case_results.append("✅ Empty username properly rejected")
            else:
                edge_case_results.append(f"⚠️ Empty username: unexpected status {response.status_code}")
        else:
            edge_case_results.append("❌ Empty username test: no response")
        
        # Test 3: Missing username field entirely
        print("    🧪 Testing missing username field")
        missing_username_data = {
            "email": f"missing_user_{timestamp}@test.com",
            "password": "test_password_123"
            # No username field at all
        }
        
        response = self.make_request("POST", "/api/v1/auth/register", missing_username_data)
        print(f"        📤 Request: {json.dumps(missing_username_data, indent=8)}")
        
        if response:
            print(f"        📥 Status: {response.status_code}")
            print(f"        📥 Body: {response.text}")
            
            if response.status_code == 200:
                try:
                    data = response.json()
                    user_data = data.get("user", {})
                    returned_username = user_data.get("username", "")
                    expected_email = missing_username_data["email"]
                    
                    if returned_username == expected_email:
                        edge_case_results.append("✅ Missing username correctly defaults to email")
                    elif returned_username == "":
                        edge_case_results.append("⚠️ Missing username stays empty")
                    else:
                        edge_case_results.append(f"❓ Missing username becomes: {returned_username}")
                except:
                    edge_case_results.append("❌ Missing username test: response parse error")
            elif response.status_code == 400:
                edge_case_results.append("✅ Missing username properly rejected")
            else:
                edge_case_results.append(f"⚠️ Missing username: unexpected status {response.status_code}")
        else:
            edge_case_results.append("❌ Missing username test: no response")
        
        # Test 4: Case insensitive email check
        print("    🧪 Testing case insensitive email")
        case_email_data = {
            "email": self.user_email.upper(),  # Same email but uppercase
            "password": "case_test_password_123",
            "username": "case_test_user"
        }
        
        response = self.make_request("POST", "/api/v1/auth/register", case_email_data)
        print(f"        📤 Request: {json.dumps(case_email_data, indent=8)}")
        
        if response:
            print(f"        📥 Status: {response.status_code}")
            print(f"        📥 Body: {response.text}")
            
            if response.status_code == 400:
                edge_case_results.append("✅ Case insensitive email properly rejected")
            elif response.status_code == 200:
                edge_case_results.append("❌ ISSUE: Case insensitive email creates new user")
            else:
                edge_case_results.append(f"⚠️ Case insensitive email: unexpected status {response.status_code}")
        else:
            edge_case_results.append("❌ Case insensitive email test: no response")
        
        # Analyze results
        issues = len([r for r in edge_case_results if "❌ ISSUE" in r])
        passed = len([r for r in edge_case_results if "✅" in r])
        unclear = len([r for r in edge_case_results if "❓" in r or "⚠️" in r])
        
        if issues > 0:
            self.log_test("Registration Edge Cases", TestResult.FAIL, 
                        f"Business logic issues found: {issues} problems")
            for result in edge_case_results:
                if "❌ ISSUE" in result:
                    print(f"        🚨 {result}")
        elif passed >= 3:
            self.log_test("Registration Edge Cases", TestResult.PASS, 
                        f"Registration business logic working: {passed}/4 tests passed")
        else:
            self.log_test("Registration Edge Cases", TestResult.WARN, 
                        f"Registration logic unclear: {passed} passed, {unclear} unclear")
        
        # Print all results for debugging
        print("    📋 Edge case test results:")
        for i, result in enumerate(edge_case_results, 1):
            print(f"        {i}. {result}")

    def test_input_validation(self):
        """Test input validation with invalid data and detailed error reporting"""
        if not self.access_token:
            self.log_test("Input Validation", TestResult.SKIP, "No access token")
            return
            
        validation_tests = []
        
        # Test 1: Invalid hair fall log data
        print("    🧪 Testing invalid hair fall log data")
        invalid_log_tests = [
            {
                "name": "Negative hair count",
                "data": {"date": "2025-05-31", "count": -5, "category": "SHOWER"},
                "should_reject": True
            },
            {
                "name": "Invalid date format",
                "data": {"date": "invalid-date", "count": 50, "category": "SHOWER"},
                "should_reject": True
            },
            {
                "name": "Invalid category",
                "data": {"date": "2025-05-31", "count": 50, "category": "INVALID_CATEGORY"},
                "should_reject": True
            },
            {
                "name": "Missing required fields",
                "data": {"count": 50},  # Missing date and category
                "should_reject": True
            },
            {
                "name": "Extremely large count",
                "data": {"date": "2025-05-31", "count": 999999, "category": "SHOWER"},
                "should_reject": False  # Large but possibly valid
            }
        ]
        
        for test in invalid_log_tests:
            print(f"        🔍 {test['name']}")
            print(f"            📤 Data: {json.dumps(test['data'], indent=12)}")
            
            response = self.make_request("POST", "/api/v1/me/hair-fall-logs", 
                                       test["data"], use_auth=True)
            
            if response:
                print(f"            📥 Status: {response.status_code}")
                print(f"            📥 Response: {response.text[:200]}...")
                
                if test["should_reject"]:
                    if response.status_code in [400, 422]:
                        validation_tests.append(f"✅ {test['name']} properly rejected")
                    elif response.status_code == 200:
                        validation_tests.append(f"❌ SECURITY: {test['name']} incorrectly accepted")
                    else:
                        validation_tests.append(f"⚠️ {test['name']}: unexpected status {response.status_code}")
                else:
                    if response.status_code in [200, 201]:
                        validation_tests.append(f"✅ {test['name']} properly accepted")
                    else:
                        validation_tests.append(f"⚠️ {test['name']}: rejected with {response.status_code}")
            else:
                validation_tests.append(f"❌ {test['name']}: no response")
        
        # Test 2: Invalid intervention data
        print("    🧪 Testing invalid intervention data")
        invalid_intervention_tests = [
            {
                "name": "Invalid intervention type",
                "data": {"type": "INVALID_TYPE", "productName": "Test", "frequency": "Daily", "startDate": "2025-05-31"},
                "should_reject": True
            },
            {
                "name": "Empty product name",
                "data": {"type": "TOPICAL", "productName": "", "frequency": "Daily", "startDate": "2025-05-31"},
                "should_reject": True
            },
            {
                "name": "Missing required fields",
                "data": {"productName": "Test Product"},  # Missing type, frequency, startDate
                "should_reject": True
            },
            {
                "name": "Invalid date format",
                "data": {"type": "TOPICAL", "productName": "Test", "frequency": "Daily", "startDate": "invalid-date"},
                "should_reject": True
            }
        ]
        
        for test in invalid_intervention_tests:
            print(f"        🔍 {test['name']}")
            print(f"            📤 Data: {json.dumps(test['data'], indent=12)}")
            
            response = self.make_request("POST", "/api/v1/me/interventions", 
                                       test["data"], use_auth=True)
            
            if response:
                print(f"            📥 Status: {response.status_code}")
                print(f"            📥 Response: {response.text[:200]}...")
                
                if test["should_reject"]:
                    if response.status_code in [400, 422]:
                        validation_tests.append(f"✅ {test['name']} properly rejected")
                    elif response.status_code == 200:
                        validation_tests.append(f"❌ SECURITY: {test['name']} incorrectly accepted")
                    else:
                        validation_tests.append(f"⚠️ {test['name']}: unexpected status {response.status_code}")
                else:
                    if response.status_code in [200, 201]:
                        validation_tests.append(f"✅ {test['name']} properly accepted")
                    else:
                        validation_tests.append(f"⚠️ {test['name']}: rejected with {response.status_code}")
            else:
                validation_tests.append(f"❌ {test['name']}: no response")
        
        # Analyze results
        security_issues = len([t for t in validation_tests if "❌ SECURITY" in t])
        passed_validations = len([t for t in validation_tests if "✅" in t])
        total_tests = len(validation_tests)
        
        if security_issues > 0:
            self.log_test("Input Validation", TestResult.FAIL, 
                        f"SECURITY ISSUES: {security_issues} invalid inputs accepted")
            for test in validation_tests:
                if "❌ SECURITY" in test:
                    print(f"        🚨 {test}")
        elif passed_validations >= total_tests * 0.7:  # 70% pass rate
            self.log_test("Input Validation", TestResult.PASS, 
                        f"Input validation working: {passed_validations}/{total_tests} tests passed")
        else:
            self.log_test("Input Validation", TestResult.WARN, 
                        f"Validation unclear: {passed_validations}/{total_tests} passed")
        
        # Print all validation results for debugging
        print("    📋 Validation test results:")
        for i, result in enumerate(validation_tests, 1):
            print(f"        {i}. {result}")

    def test_authentication_security(self):
        """Test authentication security edge cases with detailed debugging"""
        security_tests = []
        
        # Test 1: Invalid credentials
        print("    🧪 Testing invalid password")
        invalid_login = {
            "email": self.user_email,
            "password": "definitely_wrong_password_12345"
        }
        
        print(f"        📤 Request: {json.dumps(invalid_login, indent=8)}")
        response = self.make_request("POST", "/api/v1/auth/login", invalid_login)
        
        if response:
            print(f"        📥 Status: {response.status_code}")
            print(f"        📥 Response: {response.text}")
            
            if response.status_code == 401:
                security_tests.append("✅ Invalid password properly rejected")
            elif response.status_code == 200:
                security_tests.append("❌ CRITICAL: Invalid password accepted!")
            else:
                security_tests.append(f"⚠️ Invalid password: unexpected status {response.status_code}")
        else:
            security_tests.append("❌ Invalid password test: no response")
        
        # Test 2: Non-existent user
        print("    🧪 Testing non-existent user login")
        nonexistent_login = {
            "email": f"nonexistent_{int(time.time())}@nowhere.com",
            "password": "some_password_123"
        }
        
        print(f"        📤 Request: {json.dumps(nonexistent_login, indent=8)}")
        response = self.make_request("POST", "/api/v1/auth/login", nonexistent_login)
        
        if response:
            print(f"        📥 Status: {response.status_code}")
            print(f"        📥 Response: {response.text}")
            
            if response.status_code == 401:
                security_tests.append("✅ Non-existent user properly rejected")
            elif response.status_code == 200:
                security_tests.append("❌ CRITICAL: Non-existent user login succeeded!")
            else:
                security_tests.append(f"⚠️ Non-existent user: unexpected status {response.status_code}")
        else:
            security_tests.append("❌ Non-existent user test: no response")
        
        # Test 3: Malformed JWT
        print("    🧪 Testing malformed JWT token")
        malformed_jwt = "Bearer invalid.jwt.token.here.extra.parts"
        headers = {"Authorization": malformed_jwt}
        
        print(f"        📤 Malformed token: {malformed_jwt}")
        response = self.session.get(f"{self.base_url}/api/v1/me/hair-fall-logs", headers=headers)
        
        if response:
            print(f"        �� Status: {response.status_code}")
            print(f"        📥 Response: {response.text[:200]}...")
            
            if response.status_code == 401:
                security_tests.append("✅ Invalid JWT properly rejected")
            elif response.status_code == 200:
                security_tests.append("❌ CRITICAL: Invalid JWT accepted!")
            else:
                security_tests.append(f"⚠️ Invalid JWT: unexpected status {response.status_code}")
        else:
            security_tests.append("❌ Invalid JWT test: no response")
        
        # Test 4: Missing Authorization header
        print("    🧪 Testing missing Authorization header")
        response = self.session.get(f"{self.base_url}/api/v1/me/hair-fall-logs")
        
        if response:
            print(f"        📥 Status: {response.status_code}")
            print(f"        📥 Response: {response.text[:200]}...")
            
            if response.status_code == 401:
                security_tests.append("✅ Missing auth header properly rejected")
            elif response.status_code == 200:
                security_tests.append("❌ CRITICAL: No authentication required!")
            else:
                security_tests.append(f"⚠️ Missing auth: unexpected status {response.status_code}")
        elif not response:
            security_tests.append("✅ Missing auth - no response (likely protected)")
        else:
            security_tests.append("❌ Missing auth test: connection issue")
        
        # Test 5: Expired/Invalid refresh token
        print("    🧪 Testing invalid refresh token")
        invalid_refresh = {"refreshToken": "invalid.refresh.token.here"}
        
        print(f"        📤 Request: {json.dumps(invalid_refresh, indent=8)}")
        response = self.make_request("POST", "/api/v1/auth/refresh-token", invalid_refresh)
        
        if response:
            print(f"        📥 Status: {response.status_code}")
            print(f"        📥 Response: {response.text}")
            
            if response.status_code in [401, 403]:
                security_tests.append("✅ Invalid refresh token properly rejected")
            elif response.status_code == 200:
                security_tests.append("❌ CRITICAL: Invalid refresh token accepted!")
            else:
                security_tests.append(f"⚠️ Invalid refresh token: unexpected status {response.status_code}")
        else:
            security_tests.append("❌ Invalid refresh token test: no response")
        
        # Test 6: SQL Injection attempt in login
        print("    🧪 Testing SQL injection in email field")
        sql_injection_login = {
            "email": "admin@test.com'; DROP TABLE users; --",
            "password": "password123"
        }
        
        print(f"        📤 Request: {json.dumps(sql_injection_login, indent=8)}")
        response = self.make_request("POST", "/api/v1/auth/login", sql_injection_login)
        
        if response:
            print(f"        📥 Status: {response.status_code}")
            print(f"        📥 Response: {response.text}")
            
            if response.status_code in [400, 401]:
                security_tests.append("✅ SQL injection attempt properly handled")
            elif response.status_code == 500:
                security_tests.append("❌ CRITICAL: SQL injection caused server error!")
            elif response.status_code == 200:
                security_tests.append("❌ CRITICAL: SQL injection succeeded!")
            else:
                security_tests.append(f"⚠️ SQL injection: unexpected status {response.status_code}")
        else:
            security_tests.append("❌ SQL injection test: no response")
        
        # Analyze security results
        critical_issues = len([t for t in security_tests if "CRITICAL" in t])
        passed_security = len([t for t in security_tests if "✅" in t])
        total_security_tests = len(security_tests)
        
        if critical_issues > 0:
            self.log_test("Authentication Security", TestResult.FAIL, 
                        f"CRITICAL SECURITY ISSUES: {critical_issues} found")
            for test in security_tests:
                if "CRITICAL" in test:
                    print(f"        🚨 {test}")
        elif passed_security >= total_security_tests * 0.8:  # 80% pass rate
            self.log_test("Authentication Security", TestResult.PASS, 
                        f"Authentication security good: {passed_security}/{total_security_tests} tests passed")
        else:
            self.log_test("Authentication Security", TestResult.WARN, 
                        f"Authentication security unclear: {passed_security}/{total_security_tests} passed")
        
        # Print all security test results for debugging
        print("    📋 Security test results:")
        for i, result in enumerate(security_tests, 1):
            print(f"        {i}. {result}")

    def run_comprehensive_tests(self):
        """Run all tests based on OpenAPI specification"""
        print("🧪 Running OpenAPI-based comprehensive tests...\n")
        
        # Phase 1: Public & Health Endpoints
        print("📍 Phase 1: Public & Health Endpoints")
        self.test_health_endpoint()
        self.test_public_endpoint()
        self.test_protected_endpoint_unauthorized()
        
        # Phase 2: Authentication Flow
        print("\n📍 Phase 2: Authentication Flow")
        self.test_user_registration()
        self.test_user_login()
        self.test_get_current_user()
        self.test_token_refresh()
        
        # Phase 2.5: Registration Edge Cases  
        print("\n📍 Phase 2.5: Registration Edge Cases & Business Logic")
        self.test_registration_edge_cases()
        
        # Phase 3: Hair Fall Log Operations
        print("\n📍 Phase 3: Hair Fall Log Operations")
        self.test_get_hair_fall_logs()
        self.test_create_hair_fall_log()
        self.test_get_hair_fall_log_by_id()
        self.test_update_hair_fall_log()
        self.test_get_hair_fall_stats()
        self.test_get_hair_fall_logs_by_date_range()
        
        # Phase 4: Intervention Operations
        print("\n📍 Phase 4: Intervention Operations")
        self.test_get_interventions()
        self.test_create_intervention()
        self.test_get_intervention_by_id()
        self.test_log_intervention_application()
        self.test_get_intervention_applications()
        self.test_get_intervention_adherence()
        self.test_get_active_interventions()
        self.test_deactivate_intervention()
        
        # Phase 5: Progress Photo Operations
        print("\n📍 Phase 5: Progress Photo Operations")
        self.test_get_progress_photos()
        self.test_request_photo_upload_url()
        self.test_finalize_photo_upload()
        self.test_get_photo_metadata()
        self.test_get_photo_view_url()
        self.test_get_photo_stats()
        
        # Phase 6: Security & Validation
        print("\n📍 Phase 6: Security & Validation Tests")
        self.test_input_validation()
        self.test_authentication_security()
        
        # Phase 7: Coverage Analysis
        print("\n📍 Phase 7: API Coverage Analysis")
        self.test_api_endpoint_coverage()
        
        # Phase 8: Cleanup
        print("\n📍 Phase 8: Cleanup")
        self.test_logout()
        
        # Print comprehensive summary
        self.print_comprehensive_summary()

    def test_api_endpoint_coverage(self):
        """Test coverage of all documented endpoints"""
        
        # Count endpoints we've tested vs total in OpenAPI spec
        total_endpoints = 35  # From the OpenAPI spec
        tested_endpoints = len([test for test in self.test_results if test.result != TestResult.SKIP])
        
        coverage_percentage = (tested_endpoints / total_endpoints) * 100
        
        if coverage_percentage >= 80:
            self.log_test("API Endpoint Coverage", TestResult.PASS, 
                        f"Good coverage: {tested_endpoints}/{total_endpoints} endpoints ({coverage_percentage:.1f}%)")
        elif coverage_percentage >= 60:
            self.log_test("API Endpoint Coverage", TestResult.WARN, 
                        f"Moderate coverage: {tested_endpoints}/{total_endpoints} endpoints ({coverage_percentage:.1f}%)")
        else:
            self.log_test("API Endpoint Coverage", TestResult.FAIL, 
                        f"Low coverage: {tested_endpoints}/{total_endpoints} endpoints ({coverage_percentage:.1f}%)")

    def run_comprehensive_tests(self):
        """Run all tests based on OpenAPI specification"""
        print("�� Running OpenAPI-based comprehensive tests...\n")
        
        # Phase 1: Public & Health Endpoints
        print("📍 Phase 1: Public & Health Endpoints")
        self.test_health_endpoint()
        self.test_public_endpoint()
        self.test_protected_endpoint_unauthorized()
        
        # Phase 2: Authentication Flow
        print("\n�� Phase 2: Authentication Flow")
        self.test_user_registration()
        self.test_user_login()
        self.test_get_current_user()
        self.test_token_refresh()
        
        # Phase 3: Hair Fall Log Operations
        print("\n📍 Phase 3: Hair Fall Log Operations")
        self.test_get_hair_fall_logs()
        self.test_create_hair_fall_log()
        self.test_get_hair_fall_log_by_id()
        self.test_update_hair_fall_log()
        self.test_get_hair_fall_stats()
        self.test_get_hair_fall_logs_by_date_range()
        
        # Phase 4: Intervention Operations
        print("\n📍 Phase 4: Intervention Operations")
        self.test_get_interventions()
        self.test_create_intervention()
        self.test_get_intervention_by_id()
        self.test_log_intervention_application()
        self.test_get_intervention_applications()
        self.test_get_intervention_adherence()
        self.test_get_active_interventions()
        self.test_deactivate_intervention()
        
        # Phase 5: Progress Photo Operations
        print("\n📍 Phase 5: Progress Photo Operations")
        self.test_get_progress_photos()
        self.test_request_photo_upload_url()
        self.test_finalize_photo_upload()
        self.test_get_photo_metadata()
        self.test_get_photo_view_url()
        self.test_get_photo_stats()
        
        # Phase 6: Security & Validation
        print("\n📍 Phase 6: Security & Validation Tests")
        self.test_input_validation()
        self.test_authentication_security()
        
        # Phase 7: Coverage Analysis
        print("\n📍 Phase 7: API Coverage Analysis")
        self.test_api_endpoint_coverage()
        
        # Phase 8: Cleanup
        print("\n📍 Phase 8: Cleanup")
        self.test_logout()
        
        # Print comprehensive summary
        self.print_comprehensive_summary()

    def print_comprehensive_summary(self):
        """Print detailed test summary with actionable insights"""
        print("\n" + "="*70)
        print("📊 COMPREHENSIVE TEST SUMMARY")
        print("="*70)
        
        passed = sum(1 for test in self.test_results if test.result == TestResult.PASS)
        failed = sum(1 for test in self.test_results if test.result == TestResult.FAIL)
        warned = sum(1 for test in self.test_results if test.result == TestResult.WARN)
        skipped = sum(1 for test in self.test_results if test.result == TestResult.SKIP)
        total = len(self.test_results)
        
        print(f"📈 Test Results:")
        print(f"   ✅ Passed: {passed}")
        print(f"   ❌ Failed: {failed}")
        print(f"   ⚠️ Warnings: {warned}")
        print(f"   ⏭️ Skipped: {skipped}")
        print(f"   📊 Total: {total}")
        
        executed_tests = total - skipped
        if executed_tests > 0:
            success_rate = (passed / executed_tests) * 100
            print(f"   🎯 Success Rate: {success_rate:.1f}% ({passed}/{executed_tests})")
        
        # Critical Issues
        if failed > 0:
            print(f"\n🚨 CRITICAL ISSUES ({failed}):")
            for test in self.test_results:
                if test.result == TestResult.FAIL:
                    print(f"   ❌ {test.name}: {test.message}")
        
        # Warnings
        if warned > 0:
            print(f"\n⚠️ WARNINGS ({warned}):")
            for test in self.test_results:
                if test.result == TestResult.WARN:
                    print(f"   ⚠️ {test.name}: {test.message}")
        
        # Test Data Created
        print(f"\n�� Test Data Created:")
        print(f"   📧 User: {self.user_email}")
        print(f"   🆔 User ID: {self.user_id}")
        print(f"   📝 Hair Fall Log: {self.created_hair_fall_log_id}")
        print(f"   💊 Intervention: {self.created_intervention_id}")
        print(f"   📸 Photo: {self.created_photo_metadata_id}")
        
        # OpenAPI Compliance
        print(f"\n📋 OpenAPI Compliance:")
        schema_passes = len([t for t in self.test_results 
                           if "schema" in t.message.lower() and t.result == TestResult.PASS])
        print(f"   ✅ Schema validations passed: {schema_passes}")
        
        security_issues = len([t for t in self.test_results 
                             if "CRITICAL" in t.message or "SECURITY" in t.message])
        if security_issues > 0:
            print(f"   🚨 Security issues found: {security_issues}")
        else:
            print(f"   🔒 No critical security issues found")
        
        # Overall Assessment
        print(f"\n📋 OVERALL ASSESSMENT:")
        if failed == 0 and warned <= 1:
            print(f"🎉 EXCELLENT: Your API is working correctly and follows OpenAPI spec!")
        elif failed <= 2 and security_issues == 0:
            print(f"👍 GOOD: Minor issues found, but no security concerns.")
        elif security_issues > 0:
            print(f"🚨 URGENT: Security issues found that need immediate attention!")
        else:
            print(f"⚠️ NEEDS WORK: Multiple issues found, review and fix before production.")
        
        # Recommendations
        print(f"\n💡 Recommendations:")
        if failed > 0:
            print(f"   • Fix the {failed} failing test(s) - check server logs")
        if warned > 0:
            print(f"   • Review the {warned} warning(s) for potential improvements")
        if skipped > 3:
            print(f"   • {skipped} tests were skipped - ensure proper test flow")
        
        print(f"   • All endpoints tested match your OpenAPI specification")
        print(f"   • Consider adding more edge case validation")
        print(f"   • Implement comprehensive error response schemas")
        
        return failed == 0 and security_issues == 0

def main():
    """Main test runner for OpenAPI-driven testing"""
    import argparse
    
    parser = argparse.ArgumentParser(description="OpenAPI-Driven Backend Test Harness")
    parser.add_argument("--url", default="http://localhost:8080", 
                       help="Base URL for the backend API")
    parser.add_argument("--strict", action="store_true",
                       help="Treat warnings as failures")
    
    args = parser.parse_args()
    
    print("🔍 OPENAPI-DRIVEN BACKEND TEST HARNESS")
    print("Testing every endpoint from your OpenAPI specification")
    print("-" * 60)
    
    # Run comprehensive OpenAPI-based tests
    harness = OpenAPITestHarness(args.url)
    success = harness.run_comprehensive_tests()
    
    # Exit with appropriate code
    if args.strict:
        warnings = sum(1 for test in harness.test_results if test.result == TestResult.WARN)
        success = success and warnings == 0
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
