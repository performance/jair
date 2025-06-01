# test_medical_access.py
import json
import uuid
from test_harness_base import OpenAPITestHarness, TestResult

class MedicalAccessTests(OpenAPITestHarness):
    def test_professional_request_access(self):
        """Test POST /api/v1/professionals/me/medical-access/sessions/{sessionId}/request-access"""
        if not self.access_token:
            self.log_test("Professional Request Access", TestResult.SKIP, "No access token")
            return
        
        # This test assumes a sessionId exists or can be faked for testing the endpoint
        # For a real scenario, this sessionId would typically come from another flow (e.g., patient shares)
        self.created_medical_access_session_id = str(uuid.uuid4())
        
        request_data = {
            "professionalId": str(uuid.uuid4()), # Example professional ID
            "reason": "Patient requested access for review"
        }

        response = self.make_request("POST",
                                   f"/api/v1/professionals/me/medical-access/sessions/{self.created_medical_access_session_id}/request-access",
                                   request_data, use_auth=True)

        if not response:
            self.log_test("Professional Request Access", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response,
                ["id", "status", "requestedAt", "professionalId"])
            if valid:
                self.log_test("Professional Request Access", TestResult.PASS,
                              f"Access requested for session {self.created_medical_access_session_id}")
            else:
                self.log_test("Professional Request Access", TestResult.FAIL,
                              f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Professional Request Access", TestResult.FAIL,
                          f"Failed to request access: {response.status_code}")

    def test_get_professional_medical_access_sessions(self):
        """Test GET /api/v1/professionals/me/medical-access/sessions"""
        if not self.access_token:
            self.log_test("Get Professional Medical Access Sessions", TestResult.SKIP, "No access token")
            return
            
        response = self.make_request("GET", "/api/v1/professionals/me/medical-access/sessions", use_auth=True)
        
        if not response:
            self.log_test("Get Professional Medical Access Sessions", TestResult.FAIL, "No response")
            return
            
        if response.status_code == 200:
            try:
                data = response.json()
                if isinstance(data, list):
                    self.log_test("Get Professional Medical Access Sessions", TestResult.PASS, 
                                f"Retrieved {len(data)} medical access sessions")
                else:
                    self.log_test("Get Professional Medical Access Sessions", TestResult.FAIL, 
                                "Response is not a list")
            except json.JSONDecodeError:
                self.log_test("Get Professional Medical Access Sessions", TestResult.FAIL, 
                            "Invalid JSON response")
        else:
            self.log_test("Get Professional Medical Access Sessions", TestResult.FAIL, 
                        f"Failed to get medical access sessions: {response.status_code}")

    def test_get_professional_medical_access_session_by_id(self):
        """Test GET /api/v1/professionals/me/medical-access/sessions/{sessionId}"""
        if not self.access_token or not self.created_medical_access_session_id:
            self.log_test("Get Professional Medical Access Session by ID", TestResult.SKIP,
                          "No access token or medical access session ID")
            return

        response = self.make_request("GET",
                                   f"/api/v1/professionals/me/medical-access/sessions/{self.created_medical_access_session_id}",
                                   use_auth=True)

        if not response:
            self.log_test("Get Professional Medical Access Session by ID", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response,
                ["id", "status", "requestedAt", "professionalId"])
            if valid:
                self.log_test("Get Professional Medical Access Session by ID", TestResult.PASS,
                              "Medical access session retrieved by ID")
            else:
                self.log_test("Get Professional Medical Access Session by ID", TestResult.FAIL,
                              f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Get Professional Medical Access Session by ID", TestResult.FAIL,
                          f"Failed to get medical access session: {response.status_code}")

    def test_professional_approve_access(self):
        """Test POST /api/v1/professionals/me/medical-access/sessions/{sessionId}/approve"""
        if not self.access_token or not self.created_medical_access_session_id:
            self.log_test("Professional Approve Access", TestResult.SKIP,
                          "No access token or medical access session ID to approve")
            return

        response = self.make_request("POST",
                                   f"/api/v1/professionals/me/medical-access/sessions/{self.created_medical_access_session_id}/approve",
                                   use_auth=True)

        if not response:
            self.log_test("Professional Approve Access", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            self.log_test("Professional Approve Access", TestResult.PASS,
                          f"Access approved for session {self.created_medical_access_session_id}")
        else:
            self.log_test("Professional Approve Access", TestResult.FAIL,
                          f"Failed to approve access: {response.status_code}")

    def test_professional_deny_access(self):
        """Test POST /api/v1/professionals/me/medical-access/sessions/{sessionId}/deny"""
        if not self.access_token or not self.created_medical_access_session_id:
            self.log_test("Professional Deny Access", TestResult.SKIP,
                          "No access token or medical access session ID to deny")
            return

        response = self.make_request("POST",
                                   f"/api/v1/professionals/me/medical-access/sessions/{self.created_medical_access_session_id}/deny",
                                   use_auth=True)

        if not response:
            self.log_test("Professional Deny Access", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            self.log_test("Professional Deny Access", TestResult.PASS,
                          f"Access denied for session {self.created_medical_access_session_id}")
        else:
            self.log_test("Professional Deny Access", TestResult.FAIL,
                          f"Failed to deny access: {response.status_code}")