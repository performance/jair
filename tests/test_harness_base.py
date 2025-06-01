# test_harness_base.py
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
        self.created_medical_sharing_session_id = None
        self.created_medical_access_session_id = None # for professional medical access
        
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
                if len(data) > 0: # Check if list is not empty
                    # For list of objects, check schema of first object
                    if isinstance(data[0], dict):
                        for field in expected_fields:
                            if field not in data[0]:
                                missing_fields.append(field)
                    else:
                        return False, ["Response is a list, but its elements are not dictionaries, cannot validate schema."]
            elif isinstance(data, dict):
                for field in expected_fields:
                    if field not in data:
                        missing_fields.append(field)
            else:
                return False, ["Response is not a dictionary or list of dictionaries."]
            
            return len(missing_fields) == 0, missing_fields
        except json.JSONDecodeError:
            return False, ["Invalid JSON response"]
        except Exception as e:
            return False, [f"Schema validation error: {str(e)}"]

    # --- Core Health and Public Endpoint Tests (Moved from original harness) ---
    def test_health_endpoint(self):
        """Test GET /api/v1/health"""
        response = self.make_request("GET", "/api/v1/health")
        if response and response.status_code == 200:
            self.log_test("Health Endpoint", TestResult.PASS, "Backend is healthy")
        else:
            status = response.status_code if response else "No Response"
            self.log_test("Health Endpoint", TestResult.FAIL, 
                          f"Backend health check failed: Status {status}")

    def test_public_endpoint(self):
        """Test GET /api/v1/public (should be accessible without auth)"""
        response = self.make_request("GET", "/api/v1/public")
        if response and response.status_code == 200:
            self.log_test("Public Endpoint", TestResult.PASS, 
                          "Public endpoint accessible as expected")
        else:
            status = response.status_code if response else "No Response"
            self.log_test("Public Endpoint", TestResult.FAIL, 
                          f"Public endpoint access failed: Status {status}")

    def test_protected_endpoint_unauthorized(self):
        """Test GET /api/v1/protected (should require auth)"""
        response = self.make_request("GET", "/api/v1/protected", use_auth=False)
        if response and response.status_code == 403:
            self.log_test("Protected Endpoint (Unauthorized)", TestResult.PASS, 
                          "Protected endpoint correctly denied unauthorized access")
        else:
            status = response.status_code if response else "No Response"
            self.log_test("Protected Endpoint (Unauthorized)", TestResult.FAIL, 
                          f"Protected endpoint allowed unauthorized access or returned unexpected status: Status {status}")