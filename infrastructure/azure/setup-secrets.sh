#!/bin/bash
# Setup Azure Key Vault

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/config.sh"

setup_keyvault() {
    section "Setting up Azure Key Vault"
    
    # Get or generate database password
    if [ -z "$AZURE_DB_PASSWORD" ]; then
        if az keyvault secret show \
            --vault-name "$AZURE_KEYVAULT_NAME" \
            --name "$AZURE_DB_PASSWORD_SECRET" &>/dev/null; then
            log "Secret '$AZURE_DB_PASSWORD_SECRET' already exists"
            AZURE_DB_PASSWORD=$(az keyvault secret show \
                --vault-name "$AZURE_KEYVAULT_NAME" \
                --name "$AZURE_DB_PASSWORD_SECRET" \
                --query 'value' -o tsv)
        else
            log "Generating new database password..."
            AZURE_DB_PASSWORD=$(generate_password)
        fi
    fi
    
    # Create Key Vault if not exists
    if ! az keyvault show --name "$AZURE_KEYVAULT_NAME" &>/dev/null; then
        log "Creating Key Vault '$AZURE_KEYVAULT_NAME'..."
        az keyvault create \
            --name "$AZURE_KEYVAULT_NAME" \
            --resource-group "$AZURE_RESOURCE_GROUP" \
            --location "$AZURE_LOCATION" \
            --enable-rbac-authorization false
        success "Key Vault created"
    else
        success "Key Vault already exists"
    fi
    
    # Store secret
    log "Storing database password in Key Vault..."
    az keyvault secret set \
        --vault-name "$AZURE_KEYVAULT_NAME" \
        --name "$AZURE_DB_PASSWORD_SECRET" \
        --value "$AZURE_DB_PASSWORD"
    
    success "Secret stored"
    success "Key Vault setup complete!"
}

setup_keyvault
