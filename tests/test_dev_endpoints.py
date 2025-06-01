# test_dev_endpoints.py
import json
import uuid
from test_harness_base import OpenAPITestHarness, TestResult

class DevTests(OpenAPITestHarness):
    def test_setup_test_user(self):
        """Test POST /api/v1/dev/setup-test-user"""
        # This endpoint is likely for dev/testing and may not require auth.
        # Assuming it creates a default test user.
        response = self.make_request("POST", "/api/v1/dev/setup-test-user")
        
        if not response:
            self.log_test("Setup Test User (Dev)", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            try:
                data = response.json()
                if "userId" in data and "accessToken" in data:
                    # Update harness with this new test user's tokens/ID if useful
                    self.log_test("Setup Test User (Dev)", TestResult.PASS, 
                                  f"Dev test user set up: {data['userId']}")
                else:
                    self.log_test("Setup Test User (Dev)", TestResult.FAIL, 
                                  "Response missing expected fields (userId, accessToken)")
            except json.JSONDecodeError:
                self.log_test("Setup Test User (Dev)", TestResult.FAIL, "Invalid JSON response")
        else:
            self.log_test("Setup Test User (Dev)", TestResult.FAIL, 
                        f"Failed to setup test user: {response.status_code}")

    def test_setup_photo_data(self):
        """Test POST /api/v1/dev/setup-photo-data"""
        if not self.user_id:
            self.log_test("Setup Photo Data (Dev)", TestResult.SKIP, "No user ID to associate data with")
            return

        # This endpoint typically requires a userId, assuming it's passed as a query param
        params = {"userId": self.user_id}
        response = self.make_request("POST", "/api/v1/dev/setup-photo-data", params=params)
        
        if not response:
            self.log_test("Setup Photo Data (Dev)", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            self.log_test("Setup Photo Data (Dev)", TestResult.PASS, 
                        "Dev photo data set up successfully")
        else:
            self.log_test("Setup Photo Data (Dev)", TestResult.FAIL, 
                        f"Failed to setup photo data: {response.status_code}")

    def test_setup_intervention_data(self):
        """Test POST /api/v1/dev/setup-intervention-data"""
        if not self.user_id:
            self.log_test("Setup Intervention Data (Dev)", TestResult.SKIP, "No user ID to associate data with")
            return

        params = {"userId": self.user_id}
        response = self.make_request("POST", "/api/v1/dev/setup-intervention-data", params=params)
        
        if not response:
            self.log_test("Setup Intervention Data (Dev)", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            self.log_test("Setup Intervention Data (Dev)", TestResult.PASS, 
                        "Dev intervention data set up successfully")
        else:
            self.log_test("Setup Intervention Data (Dev)", TestResult.FAIL, 
                        f"Failed to setup intervention data: {response.status_code}")