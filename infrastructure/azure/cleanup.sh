#!/bin/bash
# Cleanup all Azure resources

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/config.sh"

cleanup_azure() {
    section "Cleaning up Azure Resources"
    
    warn "This will delete ALL resources including the database!"
    warn "This action cannot be undone."
    echo ""
    
    if ! confirm "Are you sure you want to delete all Azure resources?" "n"; then
        log "Cleanup cancelled"
        exit 0
    fi
    
    # Delete entire resource group (deletes everything)
    if az group show --name "$AZURE_RESOURCE_GROUP" &>/dev/null; then
        log "Deleting resource group '$AZURE_RESOURCE_GROUP'..."
        log "This will delete: PostgreSQL server, Key Vault, Container Registry, Container Apps"
        
        az group delete \
            --name "$AZURE_RESOURCE_GROUP" \
            --yes \
            --no-wait
        
        success "Resource group deletion initiated"
        log "Deletion is running in the background. This may take several minutes."
    else
        warn "Resource group '$AZURE_RESOURCE_GROUP' does not exist"
    fi
    
    success "All Azure resources cleanup initiated!"
}

cleanup_azure
