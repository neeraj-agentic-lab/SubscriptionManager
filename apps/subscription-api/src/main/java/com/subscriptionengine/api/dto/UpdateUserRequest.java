package com.subscriptionengine.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating user information.
 * 
 * @author Neeraj Yadav
 */
public class UpdateUserRequest {
    
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;
    
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    
    @Size(max = 50, message = "Role must not exceed 50 characters")
    private String role;
    
    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status;
    
    public UpdateUserRequest() {}
    
    // Getters and Setters
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
