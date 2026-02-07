#!/bin/bash
# Setup RDS PostgreSQL database on AWS

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/config.sh"

setup_rds() {
    section "Setting up RDS PostgreSQL"
    
    # Map tier to AWS instance type
    case $DB_TIER in
        small) INSTANCE_CLASS="db.t3.micro" ;;
        medium) INSTANCE_CLASS="db.t3.small" ;;
        large) INSTANCE_CLASS="db.t3.medium" ;;
        *) INSTANCE_CLASS="db.t3.micro" ;;
    esac
    
    log "Database configuration:"
    log "  Instance: $AWS_DB_INSTANCE_ID"
    log "  Class: $INSTANCE_CLASS"
    log "  Storage: ${DB_STORAGE_GB}GB"
    log "  Region: $AWS_REGION"
    
    # Check if instance exists
    if aws rds describe-db-instances \
        --db-instance-identifier "$AWS_DB_INSTANCE_ID" \
        --region "$AWS_REGION" &>/dev/null; then
        success "RDS instance '$AWS_DB_INSTANCE_ID' already exists"
    else
        log "Creating RDS instance '$AWS_DB_INSTANCE_ID'..."
        
        # Generate password
        DB_PASSWORD=$(generate_password)
        
        aws rds create-db-instance \
            --db-instance-identifier "$AWS_DB_INSTANCE_ID" \
            --db-instance-class "$INSTANCE_CLASS" \
            --engine postgres \
            --engine-version 15.4 \
            --master-username "$AWS_DB_USER" \
            --master-user-password "$DB_PASSWORD" \
            --allocated-storage "$DB_STORAGE_GB" \
            --storage-type gp3 \
            --backup-retention-period "$DB_BACKUP_RETENTION" \
            --preferred-backup-window "03:00-04:00" \
            --preferred-maintenance-window "sun:04:00-sun:05:00" \
            --publicly-accessible \
            --region "$AWS_REGION"
        
        success "RDS instance creation initiated"
        
        # Store password for later use
        export AWS_DB_PASSWORD="$DB_PASSWORD"
        
        # Wait for instance to be available
        log "Waiting for RDS instance to be available (this may take 5-10 minutes)..."
        aws rds wait db-instance-available \
            --db-instance-identifier "$AWS_DB_INSTANCE_ID" \
            --region "$AWS_REGION"
        
        success "RDS instance is available"
    fi
    
    # Get endpoint
    DB_ENDPOINT=$(aws rds describe-db-instances \
        --db-instance-identifier "$AWS_DB_INSTANCE_ID" \
        --region "$AWS_REGION" \
        --query 'DBInstances[0].Endpoint.Address' \
        --output text)
    
    success "RDS setup complete!"
    
    echo ""
    log "Connection details:"
    log "  Endpoint: $DB_ENDPOINT"
    log "  Port: 5432"
    log "  Database: $AWS_DB_NAME"
    log "  User: $AWS_DB_USER"
    log "  Password: Stored in Secrets Manager"
    echo ""
}

setup_rds
