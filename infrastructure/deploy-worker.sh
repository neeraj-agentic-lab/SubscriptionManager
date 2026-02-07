#!/bin/bash
# Deploy Worker only

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Load common functions
source "$SCRIPT_DIR/common/helpers.sh"
source "$SCRIPT_DIR/common/validate-config.sh"
source "$SCRIPT_DIR/common/env-builder.sh"

export_common_vars

main() {
    section "Deploy Worker Only"
    
    # Validate and build environment
    validate_deployment_config
    build_env_vars
    
    log "Deploying Worker to $TARGET_CLOUD..."
    
    # Load cloud-specific configuration
    source "$SCRIPT_DIR/$TARGET_CLOUD/config.sh"
    
    # Deploy Worker
    "$SCRIPT_DIR/$TARGET_CLOUD/deploy-worker.sh"
    
    success "Worker deployed successfully!"
}

main "$@"
