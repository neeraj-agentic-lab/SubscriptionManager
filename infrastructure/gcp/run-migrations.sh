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
    
    # Get Cloud SQL instance public IP
    DB_HOST=$(gcloud sql instances describe "$GCP_DB_INSTANCE_NAME" \
        --project="$GCP_PROJECT_ID" \
        --format='value(ipAddresses[0].ipAddress)')
    
    log "Database host: $DB_HOST"
    
    # Build standard JDBC URL using public IP
    DB_URL="jdbc:postgresql://${DB_HOST}:5432/${GCP_DB_NAME}"
    
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
