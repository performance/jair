# test_hair_fall_logs.py
import json
import time
from datetime import datetime
from test_harness_base import OpenAPITestHarness, TestResult

class HairFallLogTests(OpenAPITestHarness):
    def test_get_hair_fall_logs(self):
        """Test GET /api/v1/me/hair-fall-logs with pagination"""
        if not self.access_token:
            self.log_test("Get Hair Fall Logs", TestResult.SKIP, "No access token")
            return
            
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

    def test_delete_hair_fall_log(self):
        """Test DELETE /api/v1/me/hair-fall-logs/{id}"""
        if not self.access_token or not self.created_hair_fall_log_id:
            self.log_test("Delete Hair Fall Log", TestResult.SKIP,
                          "No access token or hair fall log ID to delete")
            return

        response = self.make_request("DELETE",
                                   f"/api/v1/me/hair-fall-logs/{self.created_hair_fall_log_id}",
                                   use_auth=True)

        if not response:
            self.log_test("Delete Hair Fall Log", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            self.log_test("Delete Hair Fall Log", TestResult.PASS,
                          f"Hair fall log {self.created_hair_fall_log_id} deleted successfully")
            self.created_hair_fall_log_id = None # Clear ID after deletion
        else:
            self.log_test("Delete Hair Fall Log", TestResult.FAIL,
                          f"Failed to delete hair fall log: {response.status_code}")

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
            "startDate": "2025-05-01", # Example dates
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
