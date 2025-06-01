# test_interventions.py
import json
import time
import uuid
from datetime import datetime, timedelta
from test_harness_base import OpenAPITestHarness, TestResult

class InterventionTests(OpenAPITestHarness):
    def test_get_interventions(self):
        """Test GET /api/v1/me/interventions"""
        if not self.access_token:
            self.log_test("Get Interventions", TestResult.SKIP, "No access token")
            return
            
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
            self.log_test("Get Intervention by ID", TestResult.SKIP, "No access token or intervention ID")
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

    def test_update_intervention(self):
        """Test PUT /api/v1/me/interventions/{id}"""
        if not self.access_token or not self.created_intervention_id:
            self.log_test("Update Intervention", TestResult.SKIP,
                          "No access token or intervention ID to update")
            return

        update_data = {
            "productName": f"Updated Minoxidil {int(time.time())}",
            "dosageAmount": "1.5ml",
            "frequency": "Once Daily"
        }

        response = self.make_request("PUT",
                                   f"/api/v1/me/interventions/{self.created_intervention_id}",
                                   update_data, use_auth=True)

        if not response:
            self.log_test("Update Intervention", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            self.log_test("Update Intervention", TestResult.PASS,
                          f"Intervention {self.created_intervention_id} updated successfully")
        else:
            self.log_test("Update Intervention", TestResult.FAIL,
                          f"Failed to update intervention: {response.status_code}")

    def test_log_intervention_application(self):
        """Test POST /api/v1/me/interventions/{id}/log-application"""
        if not self.access_token or not self.created_intervention_id:
            self.log_test("Log Intervention Application", TestResult.SKIP, "No access token or intervention ID")
            return
            
        application_data = {
            "timestamp": datetime.now().isoformat(),
            "notes": "Automated test application"
        }
        
        response = self.make_request("POST", 
                                   f"/api/v1/me/interventions/{self.created_intervention_id}/log-application", 
                                   application_data, use_auth=True)
        
        if not response:
            self.log_test("Log Intervention Application", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response, 
                ["id", "interventionId", "userId", "timestamp", "createdAt"])
            
            if valid:
                self.log_test("Log Intervention Application", TestResult.PASS, 
                            "Intervention application logged")
            else:
                self.log_test("Log Intervention Application", TestResult.FAIL, 
                            f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Log Intervention Application", TestResult.FAIL, 
                        f"Failed to log intervention application: {response.status_code}")

    def test_get_intervention_applications(self):
        """Test GET /api/v1/me/interventions/{id}/applications"""
        if not self.access_token or not self.created_intervention_id:
            self.log_test("Get Intervention Applications", TestResult.SKIP, "No access token or intervention ID")
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
                                f"Retrieved {len(data)} intervention applications")
                else:
                    self.log_test("Get Intervention Applications", TestResult.FAIL, 
                                "Response is not a list")
            except json.JSONDecodeError:
                self.log_test("Get Intervention Applications", TestResult.FAIL, 
                            "Invalid JSON response")
        else:
            self.log_test("Get Intervention Applications", TestResult.FAIL, 
                        f"Failed to get intervention applications: {response.status_code}")

    def test_deactivate_intervention(self):
        """Test POST /api/v1/me/interventions/{id}/deactivate"""
        if not self.access_token or not self.created_intervention_id:
            self.log_test("Deactivate Intervention", TestResult.SKIP,
                          "No access token or intervention ID to deactivate")
            return

        response = self.make_request("POST",
                                   f"/api/v1/me/interventions/{self.created_intervention_id}/deactivate",
                                   use_auth=True)

        if not response:
            self.log_test("Deactivate Intervention", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            self.log_test("Deactivate Intervention", TestResult.PASS,
                          f"Intervention {self.created_intervention_id} deactivated successfully")
        else:
            self.log_test("Deactivate Intervention", TestResult.FAIL,
                          f"Failed to deactivate intervention: {response.status_code}")

    def test_get_intervention_adherence_stats(self):
        """Test GET /api/v1/me/interventions/{id}/adherence-stats"""
        if not self.access_token or not self.created_intervention_id:
            self.log_test("Get Intervention Adherence Stats", TestResult.SKIP,
                          "No access token or intervention ID")
            return

        response = self.make_request("GET",
                                   f"/api/v1/me/interventions/{self.created_intervention_id}/adherence-stats",
                                   use_auth=True)

        if not response:
            self.log_test("Get Intervention Adherence Stats", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response,
                ["actualApplications", "adherenceLevel", "adherencePercentage",
                 "adherenceRate", "daysSinceStart", "expectedApplications", "interventionId"])
            if valid:
                self.log_test("Get Intervention Adherence Stats", TestResult.PASS,
                              "Intervention adherence stats retrieved")
            else:
                self.log_test("Get Intervention Adherence Stats", TestResult.FAIL,
                              f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Get Intervention Adherence Stats", TestResult.FAIL,
                          f"Failed to get adherence stats: {response.status_code}")