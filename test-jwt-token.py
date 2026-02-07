#!/usr/bin/env python3
"""
Generate a test JWT token for subscription API testing.
This creates a valid JWT with tenant information for development/testing purposes.
"""

import jwt
import json
from datetime import datetime, timedelta
import uuid

# Test tenant and user information
TENANT_ID = str(uuid.uuid4())
USER_ID = str(uuid.uuid4())

# JWT payload
payload = {
    "sub": USER_ID,
    "tenant_id": TENANT_ID,
    "org_id": TENANT_ID,  # Alternative tenant claim
    "email": "test@example.com",
    "name": "Test User",
    "iat": datetime.utcnow(),
    "exp": datetime.utcnow() + timedelta(hours=24),
    "iss": "https://dev-placeholder.local",
    "aud": "subscription-engine"
}

# Secret key for signing (for development only)
SECRET_KEY = "dev-secret-key-not-for-production"

# Generate JWT token
token = jwt.encode(payload, SECRET_KEY, algorithm="HS256")

print("=== Test JWT Token Generated ===")
print(f"Tenant ID: {TENANT_ID}")
print(f"User ID: {USER_ID}")
print(f"Token: {token}")
print()
print("=== Usage Examples ===")
print("# Create Plan:")
print(f'curl -X POST http://localhost:8080/api/v1/plans \\')
print(f'  -H "Authorization: Bearer {token}" \\')
print(f'  -H "Content-Type: application/json" \\')
print(f'  -H "Idempotency-Key: {str(uuid.uuid4())}" \\')
print(f'  -d \'{{"name": "Premium Plan", "basePriceCents": 2999, "currency": "USD", "billingInterval": "month", "planType": "SUBSCRIPTION", "status": "ACTIVE"}}\'')
print()
print("# Create Subscription:")
print(f'curl -X POST http://localhost:8080/api/v1/subscriptions \\')
print(f'  -H "Authorization: Bearer {token}" \\')
print(f'  -H "Content-Type: application/json" \\')
print(f'  -H "Idempotency-Key: {str(uuid.uuid4())}" \\')
print(f'  -d \'{{"planId": "PLAN_ID_FROM_ABOVE", "customerEmail": "customer@example.com", "customerFirstName": "John", "customerLastName": "Doe", "paymentMethodRef": "pm_test_123"}}\'')
print()
print("=== Decoded Token ===")
print(json.dumps(payload, indent=2, default=str))
