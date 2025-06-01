# test_photo_metadata.py
import json
import time
import uuid
from test_harness_base import OpenAPITestHarness, TestResult

class PhotoMetadataTests(OpenAPITestHarness):
    def test_request_upload_url(self):
        """Test POST /api/v1/me/progress-photos/upload-url"""
        if not self.access_token:
            self.log_test("Request Upload URL", TestResult.SKIP, "No access token")
            return
        
        upload_request_data = {
            "fileName": f"test_photo_{int(time.time())}.jpg",
            "fileType": "image/jpeg",
            "fileSize": 1024 * 500 # 500 KB
        }

        response = self.make_request("POST", "/api/v1/me/progress-photos/upload-url",
                                   upload_request_data, use_auth=True)

        if not response:
            self.log_test("Request Upload URL", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response,
                ["photoMetadataId", "uploadUrl", "fileName", "fileType"])
            if valid:
                data = response.json()
                self.created_photo_metadata_id = data["photoMetadataId"]
                self.log_test("Request Upload URL", TestResult.PASS,
                              f"Upload URL requested: {self.created_photo_metadata_id}")
                # Optional: You could simulate uploading the file here to the 'uploadUrl'
            else:
                self.log_test("Request Upload URL", TestResult.FAIL,
                              f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Request Upload URL", TestResult.FAIL,
                          f"Failed to request upload URL: {response.status_code}")

    def test_finalize_photo_upload(self):
        """Test POST /api/v1/me/progress-photos/{photoMetadataId}/finalize"""
        if not self.access_token or not self.created_photo_metadata_id:
            self.log_test("Finalize Photo Upload", TestResult.SKIP,
                          "No access token or photo metadata ID")
            return
        
        # This test assumes the file has been successfully uploaded to the pre-signed URL
        finalize_data = {
            "status": "COMPLETED", # Or "FAILED"
            "checksum": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6" # Example checksum
        }

        response = self.make_request("POST",
                                   f"/api/v1/me/progress-photos/{self.created_photo_metadata_id}/finalize",
                                   finalize_data, use_auth=True)

        if not response:
            self.log_test("Finalize Photo Upload", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response,
                ["id", "userId", "fileName", "fileType", "status", "uploadDate"])
            if valid:
                self.log_test("Finalize Photo Upload", TestResult.PASS,
                              f"Photo upload finalized: {self.created_photo_metadata_id}")
            else:
                self.log_test("Finalize Photo Upload", TestResult.FAIL,
                              f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Finalize Photo Upload", TestResult.FAIL,
                          f"Failed to finalize photo upload: {response.status_code}")

    def test_get_progress_photos(self):
        """Test GET /api/v1/me/progress-photos"""
        if not self.access_token:
            self.log_test("Get Progress Photos", TestResult.SKIP, "No access token")
            return
            
        response = self.make_request("GET", "/api/v1/me/progress-photos", use_auth=True)
        
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

    def test_get_progress_photo_by_id(self):
        """Test GET /api/v1/me/progress-photos/{photoMetadataId}"""
        if not self.access_token or not self.created_photo_metadata_id:
            self.log_test("Get Progress Photo by ID", TestResult.SKIP,
                          "No access token or photo metadata ID")
            return

        response = self.make_request("GET",
                                   f"/api/v1/me/progress-photos/{self.created_photo_metadata_id}",
                                   use_auth=True)

        if not response:
            self.log_test("Get Progress Photo by ID", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            valid, missing = self.validate_response_schema(response,
                ["id", "userId", "fileName", "fileType", "status", "uploadDate"])
            if valid:
                self.log_test("Get Progress Photo by ID", TestResult.PASS,
                              "Progress photo retrieved by ID")
            else:
                self.log_test("Get Progress Photo by ID", TestResult.FAIL,
                              f"Invalid response schema, missing: {missing}")
        else:
            self.log_test("Get Progress Photo by ID", TestResult.FAIL,
                          f"Failed to get progress photo: {response.status_code}")

    def test_delete_progress_photo(self):
        """Test DELETE /api/v1/me/progress-photos/{photoMetadataId}"""
        if not self.access_token or not self.created_photo_metadata_id:
            self.log_test("Delete Progress Photo", TestResult.SKIP,
                          "No access token or photo metadata ID to delete")
            return

        response = self.make_request("DELETE",
                                   f"/api/v1/me/progress-photos/{self.created_photo_metadata_id}",
                                   use_auth=True)

        if not response:
            self.log_test("Delete Progress Photo", TestResult.FAIL, "No response")
            return

        if response.status_code == 200:
            self.log_test("Delete Progress Photo", TestResult.PASS,
                          f"Progress photo {self.created_photo_metadata_id} deleted successfully")
            self.created_photo_metadata_id = None # Clear ID after deletion
        else:
            self.log_test("Delete Progress Photo", TestResult.FAIL,
                          f"Failed to delete progress photo: {response.status_code}")