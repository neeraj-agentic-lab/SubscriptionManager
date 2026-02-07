#!/bin/bash
# Validate deployment-config.yaml

# Note: This script is meant to be sourced, not executed directly
# helpers.sh should already be sourced before this script

validate_deployment_config() {
    section "Validating Deployment Configuration"
    
    # Check if config file exists
    if [ ! -f "$CONFIG_FILE" ]; then
        error "Configuration file not found: $CONFIG_FILE"
    fi
    
    log "Reading configuration from: $CONFIG_FILE"
    
    # Validate target_cloud
    TARGET_CLOUD=$(read_config "target_cloud")
    if [[ ! "$TARGET_CLOUD" =~ ^(gcp|aws|azure)$ ]]; then
        error "Invalid target_cloud: $TARGET_CLOUD. Must be: gcp, aws, or azure"
    fi
    success "Target cloud: $TARGET_CLOUD"
    
    # Validate environment
    ENVIRONMENT=$(read_config "app.environment")
    if [[ ! "$ENVIRONMENT" =~ ^(production|staging|development)$ ]]; then
        error "Invalid environment: $ENVIRONMENT. Must be: production, staging, or development"
    fi
    success "Environment: $ENVIRONMENT"
    
    # Validate database tier
    DB_TIER=$(read_config "database.tier")
    if [[ ! "$DB_TIER" =~ ^(small|medium|large)$ ]]; then
        error "Invalid database.tier: $DB_TIER. Must be: small, medium, or large"
    fi
    success "Database tier: $DB_TIER"
    
    # Validate API compute size
    API_SIZE=$(read_config "api.compute.size")
    if [[ ! "$API_SIZE" =~ ^(small|medium|large)$ ]]; then
        error "Invalid api.compute.size: $API_SIZE. Must be: small, medium, or large"
    fi
    success "API compute size: $API_SIZE"
    
    # Validate Worker compute size
    WORKER_SIZE=$(read_config "worker.compute.size")
    if [[ ! "$WORKER_SIZE" =~ ^(small|medium|large)$ ]]; then
        error "Invalid worker.compute.size: $WORKER_SIZE. Must be: small, medium, or large"
    fi
    success "Worker compute size: $WORKER_SIZE"
    
    # Validate numeric values
    MIN_INSTANCES=$(read_config "api.compute.min_instances")
    if ! [[ "$MIN_INSTANCES" =~ ^[0-9]+$ ]] || [ "$MIN_INSTANCES" -lt 1 ]; then
        error "Invalid api.compute.min_instances: $MIN_INSTANCES. Must be >= 1"
    fi
    
    MAX_INSTANCES=$(read_config "api.compute.max_instances")
    if ! [[ "$MAX_INSTANCES" =~ ^[0-9]+$ ]] || [ "$MAX_INSTANCES" -lt "$MIN_INSTANCES" ]; then
        error "Invalid api.compute.max_instances: $MAX_INSTANCES. Must be >= min_instances ($MIN_INSTANCES)"
    fi
    
    success "Configuration validation passed!"
    echo ""
}

# Run validation if script is executed directly
if [ "${BASH_SOURCE[0]}" -ef "$0" ]; then
    validate_deployment_config
fi
