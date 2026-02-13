package com.subscriptionengine.api.controller;

import com.subscriptionengine.api.dto.CreateUserRequest;
import com.subscriptionengine.api.dto.UpdateUserRequest;
import com.subscriptionengine.api.dto.UserResponse;
import com.subscriptionengine.generated.tables.pojos.Users;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.subscriptionengine.generated.tables.Users.USERS;

/**
 * Admin controller for user management.
 * 
 * @author Neeraj Yadav
 */
@RestController
@RequestMapping("/v1/admin/users")
@Tag(name = "Admin - Users", description = "Admin endpoints for user management")
public class AdminUsersController {
    
    private final DSLContext dsl;
    private final BCryptPasswordEncoder passwordEncoder;
    
    public AdminUsersController(DSLContext dsl) {
        this.dsl = dsl;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }
    
    /**
     * Create a new user.
     */
    @PostMapping
    @Operation(summary = "Create user", description = "Create a new user account")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        
        // Check if email already exists
        boolean emailExists = dsl.fetchExists(
            dsl.selectOne()
                .from(USERS)
                .where(USERS.EMAIL.eq(request.getEmail()))
        );
        
        if (emailExists) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        // Hash password
        String passwordHash = passwordEncoder.encode(request.getPassword());
        
        // Create user
        LocalDateTime now = LocalDateTime.now();
        UUID userId = UUID.randomUUID();
        
        String fullName = request.getFirstName() + " " + request.getLastName();
        
        dsl.insertInto(USERS)
            .set(USERS.ID, userId)
            .set(USERS.EMAIL, request.getEmail())
            .set(USERS.PASSWORD_HASH, passwordHash)
            .set(USERS.FIRST_NAME, request.getFirstName())
            .set(USERS.LAST_NAME, request.getLastName())
            .set(USERS.FULL_NAME, fullName)
            .set(USERS.ROLE, request.getRole())
            .set(USERS.STATUS, "ACTIVE")
            .set(USERS.CREATED_AT, now)
            .set(USERS.UPDATED_AT, now)
            .execute();
        
        Users user = dsl.selectFrom(USERS)
            .where(USERS.ID.eq(userId))
            .fetchOneInto(Users.class);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(user));
    }
    
    /**
     * Get user by ID.
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user", description = "Retrieve user details by ID")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        
        Users user = dsl.selectFrom(USERS)
            .where(USERS.ID.eq(userId))
            .fetchOneInto(Users.class);
        
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(mapToResponse(user));
    }
    
    /**
     * List all users with pagination.
     */
    @GetMapping
    @Operation(summary = "List users", description = "List all users with pagination and filtering")
    public ResponseEntity<UserListResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {
        
        // Build and execute query based on filters
        List<Users> users;
        int totalCount;
        
        if (status != null && role != null) {
            totalCount = dsl.fetchCount(
                dsl.selectFrom(USERS)
                    .where(USERS.STATUS.eq(status).and(USERS.ROLE.eq(role)))
            );
            users = dsl.selectFrom(USERS)
                .where(USERS.STATUS.eq(status).and(USERS.ROLE.eq(role)))
                .orderBy(USERS.CREATED_AT.desc())
                .limit(size)
                .offset(page * size)
                .fetchInto(Users.class);
        } else if (status != null) {
            totalCount = dsl.fetchCount(
                dsl.selectFrom(USERS).where(USERS.STATUS.eq(status))
            );
            users = dsl.selectFrom(USERS)
                .where(USERS.STATUS.eq(status))
                .orderBy(USERS.CREATED_AT.desc())
                .limit(size)
                .offset(page * size)
                .fetchInto(Users.class);
        } else if (role != null) {
            totalCount = dsl.fetchCount(
                dsl.selectFrom(USERS).where(USERS.ROLE.eq(role))
            );
            users = dsl.selectFrom(USERS)
                .where(USERS.ROLE.eq(role))
                .orderBy(USERS.CREATED_AT.desc())
                .limit(size)
                .offset(page * size)
                .fetchInto(Users.class);
        } else {
            totalCount = dsl.fetchCount(dsl.selectFrom(USERS));
            users = dsl.selectFrom(USERS)
                .orderBy(USERS.CREATED_AT.desc())
                .limit(size)
                .offset(page * size)
                .fetchInto(Users.class);
        }
        
        List<UserResponse> userResponses = users.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        
        UserListResponse response = new UserListResponse();
        response.setUsers(userResponses);
        response.setPage(page);
        response.setSize(size);
        response.setTotalCount(totalCount);
        response.setTotalPages((int) Math.ceil((double) totalCount / size));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update user.
     */
    @PatchMapping("/{userId}")
    @Operation(summary = "Update user", description = "Update user information")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        
        Users user = dsl.selectFrom(USERS)
            .where(USERS.ID.eq(userId))
            .fetchOneInto(Users.class);
        
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        var updateQuery = dsl.update(USERS)
            .set(USERS.UPDATED_AT, LocalDateTime.now());
        
        if (request.getFirstName() != null) {
            updateQuery = updateQuery.set(USERS.FIRST_NAME, request.getFirstName());
        }
        if (request.getLastName() != null) {
            updateQuery = updateQuery.set(USERS.LAST_NAME, request.getLastName());
        }
        if (request.getRole() != null) {
            updateQuery = updateQuery.set(USERS.ROLE, request.getRole());
        }
        if (request.getStatus() != null) {
            updateQuery = updateQuery.set(USERS.STATUS, request.getStatus());
        }
        
        updateQuery.where(USERS.ID.eq(userId)).execute();
        
        Users updatedUser = dsl.selectFrom(USERS)
            .where(USERS.ID.eq(userId))
            .fetchOneInto(Users.class);
        
        return ResponseEntity.ok(mapToResponse(updatedUser));
    }
    
    /**
     * Suspend user.
     */
    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspend user", description = "Suspend a user account")
    public ResponseEntity<UserResponse> suspendUser(@PathVariable UUID userId) {
        
        int updated = dsl.update(USERS)
            .set(USERS.STATUS, "SUSPENDED")
            .set(USERS.UPDATED_AT, LocalDateTime.now())
            .where(USERS.ID.eq(userId))
            .execute();
        
        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }
        
        Users user = dsl.selectFrom(USERS)
            .where(USERS.ID.eq(userId))
            .fetchOneInto(Users.class);
        
        return ResponseEntity.ok(mapToResponse(user));
    }
    
    /**
     * Activate user.
     */
    @PostMapping("/{userId}/activate")
    @Operation(summary = "Activate user", description = "Activate a suspended user account")
    public ResponseEntity<UserResponse> activateUser(@PathVariable UUID userId) {
        
        int updated = dsl.update(USERS)
            .set(USERS.STATUS, "ACTIVE")
            .set(USERS.UPDATED_AT, LocalDateTime.now())
            .where(USERS.ID.eq(userId))
            .execute();
        
        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }
        
        Users user = dsl.selectFrom(USERS)
            .where(USERS.ID.eq(userId))
            .fetchOneInto(Users.class);
        
        return ResponseEntity.ok(mapToResponse(user));
    }
    
    /**
     * Delete user (soft delete).
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Soft delete a user account")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        
        int updated = dsl.update(USERS)
            .set(USERS.STATUS, "DELETED")
            .set(USERS.UPDATED_AT, LocalDateTime.now())
            .where(USERS.ID.eq(userId))
            .execute();
        
        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Map Users entity to UserResponse DTO.
     */
    private UserResponse mapToResponse(Users user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole(),
            user.getStatus(),
            user.getLastLoginAt() != null ? 
                OffsetDateTime.of(user.getLastLoginAt(), ZoneOffset.UTC) : null,
            user.getCreatedAt() != null ? 
                OffsetDateTime.of(user.getCreatedAt(), ZoneOffset.UTC) : null,
            user.getUpdatedAt() != null ? 
                OffsetDateTime.of(user.getUpdatedAt(), ZoneOffset.UTC) : null
        );
    }
    
    /**
     * Response DTO for paginated user list.
     */
    public static class UserListResponse {
        private List<UserResponse> users;
        private int page;
        private int size;
        private int totalCount;
        private int totalPages;
        
        public List<UserResponse> getUsers() {
            return users;
        }
        
        public void setUsers(List<UserResponse> users) {
            this.users = users;
        }
        
        public int getPage() {
            return page;
        }
        
        public void setPage(int page) {
            this.page = page;
        }
        
        public int getSize() {
            return size;
        }
        
        public void setSize(int size) {
            this.size = size;
        }
        
        public int getTotalCount() {
            return totalCount;
        }
        
        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }
        
        public int getTotalPages() {
            return totalPages;
        }
        
        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
    }
}
