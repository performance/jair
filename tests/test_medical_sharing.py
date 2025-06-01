# test_medical_sharing.py
import json
import uuid
import time
from test_harness_base import OpenAPITestHarness, TestResult

class MedicalSharingTests(OpenAPITestHarness):
    def test_create_medical_sharing_session(self):
        """Test POST /api/v1/me/medical-sharing/sessions"""
        if not self.access_token:
            self.log_test("Create Medical Sharing Session", TestResult.SKIP, "No access token")
            return
        
        sharing_data = {
            "professionalEmail": f"pro_{int(time.time())}@example.com",
            "accessDurationHours": 24,
            "notes": "Automated test sharing session"
        }

        response = self.make_request("POST", "/api/v1/me/medical-sharing/sessions",
                                   sharing_data, use_auth=True)

        if not response:
            self.log_test("Create Medical Sharing Session", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response,
                ["sessionId", "professionalId", "accessUrl", "expiresAt"])
            if valid:
                data = response.json()
                self.created_medical_sharing_session_id = data["sessionId"]
                self.log_test("Create Medical Sharing Session", TestResult.PASS,
                              f"Medical sharing session created: {self.created_medical_sharing_session_id}")
            else:
                self.log_test("Create Medical Sharing Session", TestResult.FAIL,
                              f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Create Medical Sharing Session", TestResult.FAIL,
                          f"Failed to create medical sharing session: {response.status_code}")

    def test_get_medical_sharing_sessions(self):
        """Test GET /api/v1/me/medical-sharing/sessions"""
        if not self.access_token:
            self.log_test("Get Medical Sharing Sessions", TestResult.SKIP, "No access token")
            return
            
        response = self.make_request("GET", "/api/v1/me/medical-sharing/sessions", use_auth=True)
        
        if not response:
            self.log_test("Get Medical Sharing Sessions", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            try:
                data = response.json()
                if isinstance(data, list):
                    self.log_test("Get Medical Sharing Sessions", TestResult.PASS, 
                                f"Retrieved {len(data)} medical sharing sessions")
                else:
                    self.log_test("Get Medical Sharing Sessions", TestResult.FAIL, 
                                "Response is not a list")
            except json.JSONDecodeError:
                self.log_test("Get Medical Sharing Sessions", TestResult.FAIL, 
                            "Invalid JSON response")
        else:
            self.log_test("Get Medical Sharing Sessions", TestResult.FAIL, 
                        f"Failed to get medical sharing sessions: {response.status_code}")

    def test_get_medical_sharing_session_by_id(self):
        """Test GET /api/v1/me/medical-sharing/sessions/{sessionId}"""
        if not self.access_token or not self.created_medical_sharing_session_id:
            self.log_test("Get Medical Sharing Session by ID", TestResult.SKIP,
                          "No access token or medical sharing session ID")
            return

        response = self.make_request("GET",
                                   f"/api/v1/me/medical-sharing/sessions/{self.created_medical_sharing_session_id}",
                                   use_auth=True)

        if not response:
            self.log_test("Get Medical Sharing Session by ID", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response,
                ["sessionId", "professionalId", "accessUrl", "expiresAt"])
            if valid:
                self.log_test("Get Medical Sharing Session by ID", TestResult.PASS,
                              "Medical sharing session retrieved by ID")
            else:
                self.log_test("Get Medical Sharing Session by ID", TestResult.FAIL,
                              f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Get Medical Sharing Session by ID", TestResult.FAIL,
                          f"Failed to get medical sharing session: {response.status_code}")

    def test_revoke_medical_sharing_session(self):
        """Test POST /api/v1/me/medical-sharing/sessions/{sessionId}/revoke"""
        if not self.access_token or not self.created_medical_sharing_session_id:
            self.log_test("Revoke Medical Sharing Session", TestResult.SKIP,
                          "No access token or medical sharing session ID to revoke")
            return
        
        revoke_data = {
            "reason": "Automated test revocation"
        }

        response = self.make_request("POST",
                                   f"/api/v1/me/medical-sharing/sessions/{self.created_medical_sharing_session_id}/revoke",
                                   revoke_data, use_auth=True)

        if not response:
            self.log_test("Revoke Medical Sharing Session", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            self.log_test("Revoke Medical Sharing Session", TestResult.PASS,
                          f"Medical sharing session {self.created_medical_sharing_session_id} revoked successfully")
            self.created_medical_sharing_session_id = None # Clear ID after revocation
        else:
            self.log_test("Revoke Medical Sharing Session", TestResult.FAIL,
                          f"Failed to revoke medical sharing session: {response.status_code}")