# Security Best Practices - SubscriptionManager Console

## Authentication & Token Storage

### Current Implementation

The console uses **JWT tokens stored in localStorage** with the following security measures:

#### ✅ Security Features Implemented

1. **Token Expiration Validation**
   - Tokens are validated on app load
   - Expired tokens are automatically cleared
   - Automatic logout when token expires

2. **Automatic Session Timeout**
   - Sessions automatically expire based on JWT expiration time
   - User is redirected to login page on expiration

3. **Token Cleanup on Logout**
   - All auth data cleared from localStorage
   - User state reset in application

4. **HTTPS-Only in Production**
   - Tokens should only be transmitted over HTTPS
   - Configure production environment to enforce HTTPS

5. **CORS Protection**
   - API only accepts requests from whitelisted origins
   - Credentials required for cross-origin requests

6. **401 Auto-Logout**
   - Any 401 response triggers automatic logout
   - Prevents stale sessions

### Security Considerations

#### localStorage vs httpOnly Cookies

**Current Approach (localStorage):**
- ✅ Simple implementation for internal admin console
- ✅ Works across tabs
- ✅ No CSRF concerns
- ❌ Accessible via JavaScript (XSS risk)
- ❌ Requires manual expiration handling

**Alternative (httpOnly Cookies):**
- ✅ Not accessible via JavaScript
- ✅ Automatic browser handling
- ✅ Better XSS protection
- ❌ Requires backend cookie management
- ❌ CSRF protection needed
- ❌ More complex implementation

**Recommendation:** For an **internal admin console** with trusted users, localStorage is acceptable. For **public-facing applications**, use httpOnly cookies.

## Additional Security Recommendations

### 1. Content Security Policy (CSP)

Add CSP headers to prevent XSS attacks:

```html
<!-- Add to index.html -->
<meta http-equiv="Content-Security-Policy" 
      content="default-src 'self'; 
               script-src 'self'; 
               style-src 'self' 'unsafe-inline'; 
               img-src 'self' data: https:; 
               connect-src 'self' http://localhost:8080;">
```

### 2. Environment Variables

Never commit sensitive data:
- ✅ API URLs in `.env` (gitignored)
- ✅ Different configs per environment
- ❌ Never hardcode API keys or secrets

### 3. Input Validation

Always validate user input:
- Email format validation
- Password strength requirements
- Sanitize all user inputs

### 4. HTTPS Only (Production)

```typescript
// Enforce HTTPS in production
if (import.meta.env.PROD && window.location.protocol !== 'https:') {
  window.location.href = 'https:' + window.location.href.substring(window.location.protocol.length);
}
```

### 5. Audit Logging

Log all authentication events:
- Login attempts (success/failure)
- Logout events
- Token expiration
- Permission changes

### 6. Rate Limiting

Implement rate limiting on login endpoint:
- Prevent brute force attacks
- Limit failed login attempts
- Temporary account lockout after X failures

### 7. Session Management

- Short token expiration (24 hours default)
- Refresh token mechanism for long sessions
- Revoke tokens on password change
- Single session per user (optional)

### 8. Secure Headers

Backend should set security headers:
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000
```

## Development vs Production

### Development
- HTTP allowed for localhost
- Relaxed CORS for dev server
- Detailed error messages
- Console logging enabled

### Production
- HTTPS enforced
- Strict CORS policy
- Generic error messages
- No console logging
- Minified/obfuscated code

## Monitoring & Alerts

Set up monitoring for:
- Failed login attempts
- Unusual access patterns
- Token expiration rates
- API error rates
- CORS violations

## Incident Response

If security breach suspected:
1. Immediately revoke all active tokens
2. Force password reset for all users
3. Review audit logs
4. Patch vulnerability
5. Notify affected users

## Regular Security Tasks

- [ ] Review and rotate JWT secrets quarterly
- [ ] Update dependencies monthly
- [ ] Security audit annually
- [ ] Penetration testing before major releases
- [ ] Review access logs weekly

## Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [React Security Best Practices](https://reactjs.org/docs/security.html)
