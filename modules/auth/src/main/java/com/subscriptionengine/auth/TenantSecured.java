package com.subscriptionengine.auth;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to secure methods with tenant validation.
 * Ensures that the current user has access to the specified tenant.
 * 
 * @author Neeraj Yadav
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@tenantSecurityService.hasAccess()")
public @interface TenantSecured {
}
