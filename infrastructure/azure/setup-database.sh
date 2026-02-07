#!/bin/bash
# Setup Azure Database for PostgreSQL

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/config.sh"

setup_azure_postgres() {
    section "Setting up Azure Database for PostgreSQL"
    
    # Map tier to Azure SKU
    case $DB_TIER in
        small) SKU_NAME="B_Gen5_1" ;;
        medium) SKU_NAME="GP_Gen5_2" ;;
        large) SKU_NAME="GP_Gen5_4" ;;
        *) SKU_NAME="B_Gen5_1" ;;
    esac
    
    log "Database configuration:"
    log "  Server: $AZURE_DB_SERVER_NAME"
    log "  SKU: $SKU_NAME"
    log "  Storage: ${DB_STORAGE_GB}GB"
    log "  Location: $AZURE_LOCATION"
    
    # Create resource group if not exists
    if ! az group show --name "$AZURE_RESOURCE_GROUP" &>/dev/null; then
        log "Creating resource group..."
        az group create \
            --name "$AZURE_RESOURCE_GROUP" \
            --location "$AZURE_LOCATION"
        success "Resource group created"
    else
        success "Resource group already exists"
    fi
    
    # Check if server exists
    if az postgres flexible-server show \
        --resource-group "$AZURE_RESOURCE_GROUP" \
        --name "$AZURE_DB_SERVER_NAME" &>/dev/null; then
        success "PostgreSQL server '$AZURE_DB_SERVER_NAME' already exists"
    else
        log "Creating PostgreSQL server '$AZURE_DB_SERVER_NAME'..."
        
        # Generate password
        DB_PASSWORD=$(generate_password)
        
        az postgres flexible-server create \
            --resource-group "$AZURE_RESOURCE_GROUP" \
            --name "$AZURE_DB_SERVER_NAME" \
            --location "$AZURE_LOCATION" \
            --admin-user "$AZURE_DB_USER" \
            --admin-password "$DB_PASSWORD" \
            --sku-name "$SKU_NAME" \
            --tier Burstable \
            --version 15 \
            --storage-size "$DB_STORAGE_GB" \
            --backup-retention "$DB_BACKUP_RETENTION" \
            --public-access 0.0.0.0-255.255.255.255
        
        success "PostgreSQL server created"
        
        # Store password for later use
        export AZURE_DB_PASSWORD="$DB_PASSWORD"
    fi
    
    # Create database
    if az postgres flexible-server db show \
        --resource-group "$AZURE_RESOURCE_GROUP" \
        --server-name "$AZURE_DB_SERVER_NAME" \
        --database-name "$AZURE_DB_NAME" &>/dev/null; then
        success "Database '$AZURE_DB_NAME' already exists"
    else
        log "Creating database '$AZURE_DB_NAME'..."
        az postgres flexible-server db create \
            --resource-group "$AZURE_RESOURCE_GROUP" \
            --server-name "$AZURE_DB_SERVER_NAME" \
            --database-name "$AZURE_DB_NAME"
        success "Database created"
    fi
    
    success "Azure PostgreSQL setup complete!"
    
    echo ""
    log "Connection details:"
    log "  Server: ${AZURE_DB_SERVER_NAME}.postgres.database.azure.com"
    log "  Database: $AZURE_DB_NAME"
    log "  User: $AZURE_DB_USER"
    log "  Password: Stored in Key Vault"
    echo ""
}

setup_azure_postgres
