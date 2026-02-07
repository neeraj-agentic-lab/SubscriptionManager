#!/bin/bash
# Setup Cloud SQL PostgreSQL database on GCP

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"

# Set defaults if not provided via environment
export DB_TIER="${DB_TIER:-small}"
export DB_STORAGE_GB="${DB_STORAGE_GB:-10}"
export DB_BACKUP_RETENTION="${DB_BACKUP_RETENTION:-7}"

source "$SCRIPT_DIR/config.sh"

setup_cloud_sql() {
    section "Setting up Cloud SQL PostgreSQL"
    
    # Map tier to GCP instance type
    case $DB_TIER in
        small) INSTANCE_TIER="db-f1-micro" ;;
        medium) INSTANCE_TIER="db-g1-small" ;;
        large) INSTANCE_TIER="db-n1-standard-1" ;;
        *) INSTANCE_TIER="db-f1-micro" ;;
    esac
    
    log "Database configuration:"
    log "  Instance: $GCP_DB_INSTANCE_NAME"
    log "  Tier: $INSTANCE_TIER"
    log "  Storage: ${DB_STORAGE_GB}GB"
    log "  Region: $GCP_REGION"
    
    # Check if instance already exists
    if gcloud sql instances describe "$GCP_DB_INSTANCE_NAME" \
        --project="$GCP_PROJECT_ID" &>/dev/null; then
        success "Cloud SQL instance '$GCP_DB_INSTANCE_NAME' already exists"
    else
        log "Creating Cloud SQL instance '$GCP_DB_INSTANCE_NAME'..."
        
        gcloud sql instances create "$GCP_DB_INSTANCE_NAME" \
            --project="$GCP_PROJECT_ID" \
            --database-version=POSTGRES_15 \
            --tier="$INSTANCE_TIER" \
            --region="$GCP_REGION" \
            --storage-type=SSD \
            --storage-size="${DB_STORAGE_GB}GB" \
            --storage-auto-increase \
            --backup-start-time=03:00 \
            --maintenance-window-day=SUN \
            --maintenance-window-hour=4 \
            --retained-backups-count="$DB_BACKUP_RETENTION" \
            --authorized-networks=0.0.0.0/0
        
        success "Cloud SQL instance created"
    fi
    
    # Wait for instance to be ready
    wait_for_resource "Cloud SQL instance" \
        "gcloud sql instances describe $GCP_DB_INSTANCE_NAME --project=$GCP_PROJECT_ID --format='value(state)' | grep -q RUNNABLE"
    
    # Create database
    if gcloud sql databases describe "$GCP_DB_NAME" \
        --instance="$GCP_DB_INSTANCE_NAME" \
        --project="$GCP_PROJECT_ID" &>/dev/null; then
        success "Database '$GCP_DB_NAME' already exists"
    else
        log "Creating database '$GCP_DB_NAME'..."
        gcloud sql databases create "$GCP_DB_NAME" \
            --instance="$GCP_DB_INSTANCE_NAME" \
            --project="$GCP_PROJECT_ID"
        success "Database created"
    fi
    
    # Create user
    if gcloud sql users describe "$GCP_DB_USER" \
        --instance="$GCP_DB_INSTANCE_NAME" \
        --project="$GCP_PROJECT_ID" &>/dev/null; then
        success "Database user '$GCP_DB_USER' already exists"
    else
        log "Creating database user '$GCP_DB_USER'..."
        
        # Generate secure password
        DB_PASSWORD=$(generate_password)
        
        gcloud sql users create "$GCP_DB_USER" \
            --instance="$GCP_DB_INSTANCE_NAME" \
            --password="$DB_PASSWORD" \
            --project="$GCP_PROJECT_ID"
        
        success "Database user created"
        
        # Store password in Secret Manager (will be done in setup-secrets.sh)
        export GCP_DB_PASSWORD="$DB_PASSWORD"
    fi
    
    # Grant Cloud Run service account access to Cloud SQL
    CLOUD_RUN_SA="${GCP_PROJECT_ID}@appspot.gserviceaccount.com"
    
    log "Granting Cloud Run access to Cloud SQL..."
    gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
        --member="serviceAccount:${CLOUD_RUN_SA}" \
        --role="roles/cloudsql.client" \
        --condition=None \
        2>/dev/null || warn "Permission may already be granted"
    
    success "Cloud SQL setup complete!"
    
    # Display connection info
    echo ""
    log "Connection details:"
    log "  Instance: $GCP_PROJECT_ID:$GCP_REGION:$GCP_DB_INSTANCE_NAME"
    log "  Database: $GCP_DB_NAME"
    log "  User: $GCP_DB_USER"
    log "  Password: Stored in Secret Manager"
    echo ""
}

setup_cloud_sql
