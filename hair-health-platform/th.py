import requests
import time
from datetime import datetime
import pprint

BASE_URL = "http://localhost:8080/api/v1"
pp = pprint.PrettyPrinter(indent=2)

def ts():
    return str(int(time.time()))

def print_section(title):
    print("\n" + "="*30)
    print(title)
    print("="*30)

def register_user():
    print_section("Register User")
    email = f"testuser_{ts()}@hairhealth.com"
    username = f"testuser_{ts()}"
    password = "testpassword123"
    response = requests.post(f"{BASE_URL}/auth/register", json={
        "email": email,
        "password": password,
        "username": username
    })
    response.raise_for_status()
    data = response.json()
    pp.pprint(data)
    return data["accessToken"], data["refreshToken"], data["user"]["id"]

def login_user(email, password):
    print_section("Login User")
    response = requests.post(f"{BASE_URL}/auth/login", json={
        "email": email,
        "password": password
    })
    response.raise_for_status()
    return response.json()

def auth_get(endpoint, token):
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.get(f"{BASE_URL}{endpoint}", headers=headers)
    response.raise_for_status()
    return response.json()

def auth_post(endpoint, token, payload):
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.post(f"{BASE_URL}{endpoint}", json=payload, headers=headers)
    response.raise_for_status()
    return response.json()

def test_flow():
    # Step 1: Register
    access_token, refresh_token, user_id = register_user()

    # Step 2: Login
    login_data = login_user(f"testuser_{ts()}@hairhealth.com", "testpassword123")
    # Step 3: Auth endpoints
    print_section("Get Current User")
    pp.pprint(auth_get("/auth/me", access_token))

    print_section("Test Protected Endpoint")
    pp.pprint(auth_get("/test/protected", access_token))

    # Step 4: Data Operations
    print_section("Create Hair Fall Log")
    log_data = {
        "date": datetime.now().strftime("%Y-%m-%d"),
        "count": 50,
        "category": "SHOWER",
        "description": "Post test log"
    }
    pp.pprint(auth_post("/me/hair-fall-logs", access_token, log_data))

    print_section("Create Intervention")
    intervention_data = {
        "type": "TOPICAL",
        "productName": "Minoxidil 5%",
        "dosageAmount": "1ml",
        "frequency": "Twice Daily",
        "applicationTime": "08:00, 20:00",
        "startDate": datetime.now().strftime("%Y-%m-%d"),
        "notes": "Apply as part of test"
    }
    pp.pprint(auth_post("/me/interventions", access_token, intervention_data))

    print_section("Request Photo Upload URL")
    upload_data = {
        "filename": f"progress_{ts()}.jpg.enc",
        "angle": "HAIRLINE",
        "captureDate": datetime.utcnow().isoformat() + "Z",
        "encryptionKeyInfo": "test_key_123"
    }
    pp.pprint(auth_post("/me/progress-photos/upload-url", access_token, upload_data))

    print_section("Refresh Token")
    resp = requests.post(f"{BASE_URL}/auth/refresh-token", json={"refreshToken": refresh_token})
    resp.raise_for_status()
    pp.pprint(resp.json())

    print_section("Invalid Token Test")
    try:
        resp = requests.get(f"{BASE_URL}/me/hair-fall-logs", headers={"Authorization": "Bearer invalid-token"})
        print("Unexpected Success:", resp.json())
    except requests.exceptions.HTTPError as e:
        print("Correctly failed with 401:", e.response.status_code)

if __name__ == "__main__":
    test_flow()

