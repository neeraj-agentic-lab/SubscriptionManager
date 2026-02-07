#!/bin/bash
# Run database migrations using Gradle Flyway on Azure PostgreSQL

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/config.sh"

run_migrations() {
    section "Running Database Migrations"
    
    log "Connecting to Azure PostgreSQL server: $AZURE_DB_SERVER_NAME"
    
    # Get database password from Key Vault
    DB_PASSWORD=$(az keyvault secret show \
        --vault-name "$AZURE_KEYVAULT_NAME" \
        --name "$AZURE_DB_PASSWORD_SECRET" \
        --query 'value' -o tsv)
    
    # Build database URL
    DB_HOST="${AZURE_DB_SERVER_NAME}.postgres.database.azure.com"
    DB_URL="jdbc:postgresql://${DB_HOST}:5432/${AZURE_DB_NAME}?sslmode=require"
    
    log "Running Gradle Flyway migrations..."
    
    cd "$PROJECT_ROOT"
    
    # Run Flyway migrations via Gradle
    ./gradlew flywayMigrate \
        -Dflyway.url="$DB_URL" \
        -Dflyway.user="${AZURE_DB_USER}@${AZURE_DB_SERVER_NAME}" \
        -Dflyway.password="$DB_PASSWORD" \
        -Dflyway.locations="filesystem:db/migrations"
    
    success "Database migrations completed successfully!"
}

run_migrations
