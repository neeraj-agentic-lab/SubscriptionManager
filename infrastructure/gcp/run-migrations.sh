#!/bin/bash
# Run database migrations using Gradle Flyway on GCP Cloud SQL

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/config.sh"

run_migrations() {
    section "Running Database Migrations"
    
    log "Connecting to Cloud SQL instance: $GCP_DB_INSTANCE_NAME"
    
    # Get database password from Secret Manager
    DB_PASSWORD=$(gcloud secrets versions access latest \
        --secret="$GCP_DB_PASSWORD_SECRET" \
        --project="$GCP_PROJECT_ID")
    
    # Build database URL for Cloud SQL (using Unix socket)
    DB_URL="jdbc:postgresql:///${GCP_DB_NAME}?cloudSqlInstance=${GCP_PROJECT_ID}:${GCP_REGION}:${GCP_DB_INSTANCE_NAME}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
    
    log "Running Gradle Flyway migrations..."
    
    cd "$PROJECT_ROOT"
    
    # Run Flyway migrations via Gradle
    ./gradlew flywayMigrate \
        -Dflyway.url="$DB_URL" \
        -Dflyway.user="$GCP_DB_USER" \
        -Dflyway.password="$DB_PASSWORD" \
        -Dflyway.locations="filesystem:db/migrations"
    
    success "Database migrations completed successfully!"
}

run_migrations
