# Troubleshooting Guide - Customers Page Error

## Issue
CustomersPage shows error: "An unexpected error occurred. Please contact support if this persists."

## Fixes Applied

### 1. Added Tenant Context to API Requests
**File:** `src/lib/api.ts`
- Added request interceptor to include `X-Tenant-Id` header
- Reads selected tenant from localStorage (Zustand persist)
- Automatically adds header to all tenant-scoped API calls

### 2. Added Zustand Persist Middleware
**File:** `src/store/tenantStore.ts`
- Added `persist` middleware to save tenant state to localStorage
- Store name: `tenant-store`
- This allows the API client to access the selected tenant

### 3. Added Tenant Selection Check
**File:** `src/pages/CustomersPage.tsx`
- Added check to ensure tenant is selected before API call
- Shows helpful error message if no tenant selected
- Enhanced error logging for debugging

### 4. Added Error Boundary
**File:** `src/components/ErrorBoundary.tsx`
- Created React Error Boundary to catch rendering errors
- Shows detailed error information for debugging
- Wrapped all routes in App.tsx

## Steps to Resolve

1. **Refresh the browser** (hard refresh: Cmd+Shift+R or Ctrl+Shift+F5)
2. **Clear browser cache and localStorage** if needed
3. **Select a tenant** from the dropdown in the header
4. **Navigate to Customers page**
5. **Check browser console** for detailed error logs

## Debugging Steps

### Check Browser Console
Open browser DevTools (F12) and look for:
- Error messages in Console tab
- Network tab for failed API requests
- Check the request headers (should include `X-Tenant-Id`)

### Check localStorage
In browser console, run:
```javascript
// Check if tenant is stored
JSON.parse(localStorage.getItem('tenant-store'))

// Check auth token
localStorage.getItem('auth_token')
```

### Check API Request
In Network tab, find the `/api/v1/admin/customers` request and verify:
- Request Headers include `Authorization: Bearer <token>`
- Request Headers include `X-Tenant-Id: <tenant-id>`
- Response status and error message

## Common Issues

### Issue 1: No Tenant Selected
**Symptom:** Error message "Please select a tenant from the dropdown to view customers"
**Solution:** Select a tenant from the dropdown in the header

### Issue 2: Missing X-Tenant-Id Header
**Symptom:** 403 Forbidden or 400 Bad Request
**Solution:** Ensure tenant is selected and localStorage has `tenant-store` data

### Issue 3: Invalid Auth Token
**Symptom:** 401 Unauthorized, redirected to login
**Solution:** Log in again to get a fresh token

### Issue 4: Backend Not Running
**Symptom:** Network error, ERR_CONNECTION_REFUSED
**Solution:** Start the backend API server on port 8080

## Expected API Endpoint
```
GET http://localhost:8080/api/v1/admin/customers
Headers:
  Authorization: Bearer <jwt-token>
  X-Tenant-Id: <tenant-uuid>
```

## Next Steps if Still Failing

1. Check the actual error in browser console (detailed logs added)
2. Verify backend API is running and accessible
3. Test the endpoint directly with curl:
```bash
curl -H "Authorization: Bearer <token>" \
     -H "X-Tenant-Id: <tenant-id>" \
     http://localhost:8080/api/v1/admin/customers
```
4. Check if the user has SUPER_ADMIN role (required for tenant switching)
5. Verify the tenant ID is valid in the database
