#!/bin/bash
# Run database migrations using Gradle Flyway on AWS RDS

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/config.sh"

run_migrations() {
    section "Running Database Migrations"
    
    log "Connecting to RDS instance: $AWS_DB_INSTANCE_ID"
    
    # Get database endpoint
    DB_ENDPOINT=$(aws rds describe-db-instances \
        --db-instance-identifier "$AWS_DB_INSTANCE_ID" \
        --region "$AWS_REGION" \
        --query 'DBInstances[0].Endpoint.Address' \
        --output text)
    
    # Get database password from Secrets Manager
    DB_PASSWORD=$(aws secretsmanager get-secret-value \
        --secret-id "$AWS_DB_PASSWORD_SECRET" \
        --region "$AWS_REGION" \
        --query 'SecretString' \
        --output text)
    
    # Build database URL
    DB_URL="jdbc:postgresql://${DB_ENDPOINT}:5432/${AWS_DB_NAME}"
    
    log "Running Gradle Flyway migrations..."
    
    cd "$PROJECT_ROOT"
    
    # Run Flyway migrations via Gradle
    ./gradlew flywayMigrate \
        -Dflyway.url="$DB_URL" \
        -Dflyway.user="$AWS_DB_USER" \
        -Dflyway.password="$DB_PASSWORD" \
        -Dflyway.locations="filesystem:db/migrations"
    
    success "Database migrations completed successfully!"
}

run_migrations
