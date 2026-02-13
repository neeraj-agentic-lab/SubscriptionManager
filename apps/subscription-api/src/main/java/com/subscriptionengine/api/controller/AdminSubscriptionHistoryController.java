package com.subscriptionengine.api.controller;

import com.subscriptionengine.generated.tables.pojos.SubscriptionHistory;
import com.subscriptionengine.subscriptions.dto.SubscriptionHistoryResponse;
import com.subscriptionengine.subscriptions.service.SubscriptionHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin controller for subscription history and audit trail.
 * 
 * @author Neeraj Yadav
 */
@RestController
@RequestMapping("/v1/admin/subscriptions/{subscriptionId}/history")
@Tag(name = "Admin - Subscription History", description = "Admin endpoints for subscription audit trail")
public class AdminSubscriptionHistoryController {
    
    private final SubscriptionHistoryService subscriptionHistoryService;
    
    public AdminSubscriptionHistoryController(SubscriptionHistoryService subscriptionHistoryService) {
        this.subscriptionHistoryService = subscriptionHistoryService;
    }
    
    /**
     * Get subscription history with pagination.
     */
    @GetMapping
    @Operation(summary = "Get subscription history", description = "Retrieve audit trail for a subscription with pagination")
    public ResponseEntity<HistoryListResponse> getSubscriptionHistory(
            @PathVariable UUID subscriptionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<SubscriptionHistory> history = subscriptionHistoryService.getSubscriptionHistory(subscriptionId, page, size);
        long totalCount = subscriptionHistoryService.getHistoryCount(subscriptionId);
        
        List<SubscriptionHistoryResponse> historyResponses = history.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        HistoryListResponse response = new HistoryListResponse();
        response.setHistory(historyResponses);
        response.setPage(page);
        response.setSize(size);
        response.setTotalCount(totalCount);
        response.setTotalPages((int) Math.ceil((double) totalCount / size));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get full subscription history (no pagination).
     */
    @GetMapping("/all")
    @Operation(summary = "Get all subscription history", description = "Retrieve complete audit trail for a subscription")
    public ResponseEntity<List<SubscriptionHistoryResponse>> getAllSubscriptionHistory(
            @PathVariable UUID subscriptionId) {
        
        List<SubscriptionHistory> history = subscriptionHistoryService.getSubscriptionHistory(subscriptionId);
        
        List<SubscriptionHistoryResponse> historyResponses = history.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(historyResponses);
    }
    
    /**
     * Map SubscriptionHistory entity to SubscriptionHistoryResponse DTO.
     */
    private SubscriptionHistoryResponse mapToResponse(SubscriptionHistory history) {
        SubscriptionHistoryResponse response = new SubscriptionHistoryResponse();
        response.setId(history.getId());
        response.setTenantId(history.getTenantId());
        response.setSubscriptionId(history.getSubscriptionId());
        response.setAction(history.getAction());
        response.setPerformedBy(history.getPerformedBy());
        response.setPerformedByType(history.getPerformedByType());
        response.setPerformedAt(history.getPerformedAt() != null ? 
            OffsetDateTime.parse(history.getPerformedAt().toString()) : null);
        response.setNotes(history.getNotes());
        response.setCreatedAt(history.getCreatedAt() != null ? 
            OffsetDateTime.parse(history.getCreatedAt().toString()) : null);
        
        // Parse metadata JSONB if present
        if (history.getMetadata() != null) {
            try {
                String metadataJson = history.getMetadata().data();
                // Simple parsing - in production use Jackson
                response.setMetadata(java.util.Map.of("raw", metadataJson));
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        
        return response;
    }
    
    /**
     * Response DTO for paginated history list.
     */
    public static class HistoryListResponse {
        private List<SubscriptionHistoryResponse> history;
        private int page;
        private int size;
        private long totalCount;
        private int totalPages;
        
        public List<SubscriptionHistoryResponse> getHistory() {
            return history;
        }
        
        public void setHistory(List<SubscriptionHistoryResponse> history) {
            this.history = history;
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
        
        public long getTotalCount() {
            return totalCount;
        }
        
        public void setTotalCount(long totalCount) {
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
