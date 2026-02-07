#!/bin/bash
# Main deployment orchestrator
# Detects cloud from config and deploys everything

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Load common functions
source "$SCRIPT_DIR/common/helpers.sh"
source "$SCRIPT_DIR/common/validate-config.sh"
source "$SCRIPT_DIR/common/env-builder.sh"

export_common_vars

main() {
    section "Multi-Cloud Deployment - Subscription Manager"
    
    # Step 1: Validate configuration
    validate_deployment_config
    
    # Step 2: Build environment variables
    build_env_vars
    
    log "Deployment Summary:"
    log "  Target Cloud: $TARGET_CLOUD"
    log "  Environment: $ENVIRONMENT"
    log "  Region: $REGION"
    log "  API: $API_NAME"
    log "  Worker: $WORKER_NAME"
    log "  Database: $DB_NAME"
    echo ""
    
    # Step 3: Load cloud-specific configuration
    CLOUD_CONFIG="$SCRIPT_DIR/$TARGET_CLOUD/config.sh"
    if [ ! -f "$CLOUD_CONFIG" ]; then
        error "Cloud configuration not found: $CLOUD_CONFIG"
    fi
    source "$CLOUD_CONFIG"
    
    # Step 3: Enable required APIs (GCP only)
    if [ "$TARGET_CLOUD" = "gcp" ]; then
        section "Step 0: Enable Required APIs"
        "$SCRIPT_DIR/$TARGET_CLOUD/setup-apis.sh"
    fi
    
    # Step 4: Setup database
    section "Step 1: Database Setup"
    "$SCRIPT_DIR/$TARGET_CLOUD/setup-database.sh"
    
    # Step 5: Setup secrets
    section "Step 2: Secrets Setup"
    "$SCRIPT_DIR/$TARGET_CLOUD/setup-secrets.sh"
    
    # Wait for IAM permissions to propagate
    log "Waiting for IAM permissions to propagate..."
    sleep 10
    
    # Step 6: Run migrations
    if [ "$RUN_MIGRATIONS" = "true" ]; then
        section "Step 3: Database Migrations"
        "$SCRIPT_DIR/$TARGET_CLOUD/run-migrations.sh"
    else
        warn "Skipping migrations (disabled in config)"
    fi
    
    # Step 7: Deploy API
    section "Step 4: Deploy API"
    "$SCRIPT_DIR/$TARGET_CLOUD/deploy-api.sh"
    
    # Step 8: Deploy Worker
    section "Step 5: Deploy Worker"
    "$SCRIPT_DIR/$TARGET_CLOUD/deploy-worker.sh"
    
    # Success!
    section "Deployment Complete!"
    success "All components deployed successfully to $TARGET_CLOUD"
    echo ""
    log "Deployment Details:"
    log "  Cloud: $TARGET_CLOUD"
    log "  Environment: $ENVIRONMENT"
    log "  Region: $REGION"
    log "  API: Deployed ✓"
    log "  Worker: Deployed ✓"
    log "  Database: Ready ✓"
    echo ""
}

# Run main function
main "$@"
