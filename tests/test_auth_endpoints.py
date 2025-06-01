# test_auth_endpoints.py
import json
import time
from test_harness_base import OpenAPITestHarness, TestResult

class AuthTests(OpenAPITestHarness):
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
                security_tests.append("❌ CRITICAL: Non-existent user accepted!")
            else:
                security_tests.append(f"⚠️ Non-existent user: unexpected status {response.status_code}")
        else:
            security_tests.append("❌ Non-existent user test: no response")
            
        if any("❌ CRITICAL" in s for s in security_tests):
            self.log_test("Authentication Security", TestResult.FAIL, 
                        "Critical security issues found:\n" + "\n".join(security_tests))
        elif any("⚠️" in s for s in security_tests):
            self.log_test("Authentication Security", TestResult.WARN, 
                        "Security warnings found:\n" + "\n".join(security_tests))
        else:
            self.log_test("Authentication Security", TestResult.PASS, 
                        "No critical security issues found:\n" + "\n".join(security_tests))

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
                    
                    mock_response = type('MockResponse', (), {
                        'json': lambda: user_data,
                        'status_code': 200
                    })()
                    
                    user_valid, user_missing = self.validate_response_schema(
                        mock_response, ["id", "email", "username", "isEmailVerified"]
                    )
                    
                    if user_valid:
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
                    import traceback
                    self.log_test("User Registration", TestResult.FAIL, 
                                f"Error parsing response: {type(e).__name__}: {str(e)}\n{traceback.format_exc()}")
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
                self.access_token = data["accessToken"]
                self.refresh_token = data["refreshToken"]
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
