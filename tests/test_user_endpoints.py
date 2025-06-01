# test_user_endpoints.py
import json
import time
import uuid
from test_harness_base import OpenAPITestHarness, TestResult

class UserTests(OpenAPITestHarness):
    def test_create_test_user(self):
        """Test POST /api/v1/users/test"""
        # This endpoint might not require auth, or might require admin auth. Assuming no auth for this specific dev endpoint.
        test_user_data = {
            "email": f"testuser_{int(time.time())}@example.com",
            "password": "TestPass123!",
            "username": f"testuser_{int(time.time())}"
        }
        response = self.make_request("POST", "/api/v1/users/test", test_user_data)
        
        if not response:
            self.log_test("Create Test User", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            try:
                data = response.json()
                if "id" in data and "email" in data:
                    self.log_test("Create Test User", TestResult.PASS, 
                                  f"Test user created: {data['id']}")
                else:
                    self.log_test("Create Test User", TestResult.FAIL, 
                                  "Response missing expected fields (id, email)")
            except json.JSONDecodeError:
                self.log_test("Create Test User", TestResult.FAIL, "Invalid JSON response")
        else:
            self.log_test("Create Test User", TestResult.FAIL, 
                        f"Failed to create test user: {response.status_code}")

    def test_get_user_by_id(self):
        """Test GET /api/v1/users/{id}"""
        if not self.access_token or not self.user_id:
            self.log_test("Get User by ID", TestResult.SKIP, "No access token or user ID")
            return
        
        response = self.make_request("GET", f"/api/v1/users/{self.user_id}", use_auth=True)
        
        if not response:
            self.log_test("Get User by ID", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response,
                ["id", "email", "username", "isEmailVerified"])
            if valid:
                self.log_test("Get User by ID", TestResult.PASS, "User retrieved by ID")
            else:
                self.log_test("Get User by ID", TestResult.FAIL, 
                            f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Get User by ID", TestResult.FAIL, 
                        f"Failed to get user by ID: {response.status_code}")

    def test_get_current_user_me(self):
        """Test GET /api/v1/users/me"""
        if not self.access_token:
            self.log_test("Get Current User (me)", TestResult.SKIP, "No access token")
            return
            
        response = self.make_request("GET", "/api/v1/users/me", use_auth=True)
        
        if not response:
            self.log_test("Get Current User (me)", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response,
                ["id", "email", "username", "isEmailVerified"])
            if valid:
                self.log_test("Get Current User (me)", TestResult.PASS, 
                            "Current user (me) retrieved successfully")
            else:
                self.log_test("Get Current User (me)", TestResult.FAIL, 
                            f"Invalid user (me) response, missing: {missing}")
        else:
            self.log_test("Get Current User (me)", TestResult.FAIL, 
                        f"Failed to get current user (me): {response.status_code}")

    def test_update_current_user_me(self):
        """Test PUT /api/v1/users/me"""
        if not self.access_token:
            self.log_test("Update Current User (me)", TestResult.SKIP, "No access token")
            return
            
        updated_username = f"updated_user_{int(time.time())}"
        update_data = {
            "username": updated_username
        }
        
        response = self.make_request("PUT", "/api/v1/users/me", update_data, use_auth=True)
        
        if not response:
            self.log_test("Update Current User (me)", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response,
                ["id", "email", "username", "isEmailVerified"])
            if valid:
                data = response.json()
                if data.get("username") == updated_username:
                    self.log_test("Update Current User (me)", TestResult.PASS, 
                                "Current user (me) updated successfully")
                else:
                    self.log_test("Update Current User (me)", TestResult.WARN, 
                                "Username not updated as expected in response")
            else:
                self.log_test("Update Current User (me)", TestResult.FAIL, 
                            f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Update Current User (me)", TestResult.FAIL, 
                        f"Failed to update current user (me): {response.status_code}")

    def test_delete_current_user_me(self):
        """Test DELETE /api/v1/users/me"""
        if not self.access_token:
            self.log_test("Delete Current User (me)", TestResult.SKIP, "No access token")
            return
            
        response = self.make_request("DELETE", "/api/v1/users/me", use_auth=True)
        
        if not response:
            self.log_test("Delete Current User (me)", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            self.log_test("Delete Current User (me)", TestResult.PASS, 
                        "Current user (me) deleted successfully")
            self.access_token = None # Invalidate token after deletion
            self.refresh_token = None
            self.user_id = None
            self.user_email = None
        else:
            self.log_test("Delete Current User (me)", TestResult.FAIL, 
                        f"Failed to delete current user (me): {response.status_code}")