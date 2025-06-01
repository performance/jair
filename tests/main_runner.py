# main_runner.py
#!/usr/bin/env python3
"""
Main test runner for OpenAPI-driven backend testing.
Orchestrates tests from various modules.
"""

import argparse
import sys
from test_harness_base import OpenAPITestHarness, TestResult
from test_auth_endpoints import AuthTests
from test_hair_fall_logs import HairFallLogTests
from test_interventions import InterventionTests
from test_medical_access import MedicalAccessTests
from test_medical_sharing import MedicalSharingTests
from test_photo_metadata import PhotoMetadataTests
from test_user_endpoints import UserTests
from test_dev_endpoints import DevTests

class ComprehensiveTestRunner(
    AuthTests,
    HairFallLogTests,
    InterventionTests,
    MedicalAccessTests,
    MedicalSharingTests,
    PhotoMetadataTests,
    UserTests,
    DevTests
):
    """
    Combines all test classes into a single runner.
    Methods from all inherited classes become available.
    """
    def run_all_tests(self):
        print("\n--- Running Core Health & Public Endpoint Tests ---")
        self.test_health_endpoint()
        self.test_public_endpoint()
        self.test_protected_endpoint_unauthorized()

        print("\n--- Running Authentication Tests ---")
        self.test_authentication_security() # Run security tests before main auth flow
        self.test_user_registration()
        self.test_user_login()
        self.test_get_current_user()
        self.test_token_refresh()

        print("\n--- Running Hair Fall Log Tests ---")
        self.test_create_hair_fall_log()
        self.test_get_hair_fall_logs()
        self.test_get_hair_fall_log_by_id()
        self.test_update_hair_fall_log()
        self.test_get_hair_fall_stats()
        self.test_get_hair_fall_logs_by_date_range()
        self.test_delete_hair_fall_log() # Delete after other tests to ensure data exists for them

        print("\n--- Running Intervention Tests ---")
        self.test_create_intervention()
        self.test_get_interventions()
        self.test_get_intervention_by_id()
        self.test_update_intervention()
        self.test_log_intervention_application()
        self.test_get_intervention_applications()
        self.test_get_intervention_adherence_stats()
        self.test_deactivate_intervention()

        print("\n--- Running Medical Sharing Tests ---")
        self.test_create_medical_sharing_session()
        self.test_get_medical_sharing_sessions()
        self.test_get_medical_sharing_session_by_id()
        self.test_revoke_medical_sharing_session()

        print("\n--- Running Photo Metadata Tests ---")
        self.test_request_upload_url()
        self.test_finalize_photo_upload()
        self.test_get_progress_photos()
        self.test_get_progress_photo_by_id()
        self.test_delete_progress_photo()

        print("\n--- Running User Endpoint Tests ---")
        # These might be less critical for a full functional test and can be run independently
        # self.test_create_test_user() # This creates a new user, might interfere with main flow
        self.test_get_user_by_id() # Relies on `self.user_id` from registration
        self.test_get_current_user_me()
        self.test_update_current_user_me()
        # self.test_delete_current_user_me() # This deletes the main test user, run with caution or at end

        print("\n--- Running Professional Medical Access Tests ---")
        # These typically involve two sides (patient sharing, professional accessing)
        # For a simplified harness, we only test the professional side as if a session exists.
        self.test_professional_request_access()
        self.test_get_professional_medical_access_sessions()
        self.test_get_professional_medical_access_session_by_id()
        self.test_professional_approve_access()
        self.test_professional_deny_access()

        print("\n--- Running Development / Setup Endpoints Tests ---")
        # These are usually for setting up test data, not part of regular API functionality
        self.test_setup_test_user()
        self.test_setup_photo_data()
        self.test_setup_intervention_data()

        print("\n--- Running Final Logout ---")
        self.test_logout() # Ensure logout works at the end

    def print_summary(self, strict_mode: bool):
        print("\n" + "=" * 60)
        print("📊 TEST HARNESS SUMMARY")
        print("=" * 60)

        total_tests = len(self.test_results)
        passed = sum(1 for test in self.test_results if test.result == TestResult.PASS)
        failed = sum(1 for test in self.test_results if test.result == TestResult.FAIL)
        skipped = sum(1 for test in self.test_results if test.result == TestResult.SKIP)
        warned = sum(1 for test in self.test_results if test.result == TestResult.WARN)
        security_issues = sum(1 for test in self.test_results if "Security" in test.name and test.result == TestResult.FAIL)

        print(f"Total Tests: {total_tests}")
        print(f"✅ Passed: {passed}")
        print(f"❌ Failed: {failed}")
        print(f"⏭️ Skipped: {skipped}")
        print(f"⚠️ Warned: {warned}")

        if failed > 0:
            print("\n🚨 FAILED TESTS:")
            for test in self.test_results:
                if test.result == TestResult.FAIL:
                    print(f"   - {test.name}: {test.message}")
        
        if warned > 0:
            print("\n🔶 WARNINGS:")
            for test in self.test_results:
                if test.result == TestResult.WARN:
                    print(f"   - {test.name}: {test.message}")

        print("\n--- Recommendations ---")
        if failed == 0 and security_issues == 0:
            print("   • All critical tests passed!")
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
    harness = ComprehensiveTestRunner(args.url)
    harness.run_all_tests()
    success = harness.print_summary(args.strict)
    
    # Exit with appropriate code
    if args.strict:
        warnings = sum(1 for test in harness.test_results if test.result == TestResult.WARN)
        success = success and warnings == 0
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()