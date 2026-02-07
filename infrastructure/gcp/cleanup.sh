#!/bin/bash
# Cleanup all GCP resources

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/config.sh"

cleanup_gcp() {
    section "Cleaning up GCP Resources"
    
    warn "This will delete ALL resources including the database!"
    warn "This action cannot be undone."
    echo ""
    
    if ! confirm "Are you sure you want to delete all GCP resources?" "n"; then
        log "Cleanup cancelled"
        exit 0
    fi
    
    # Delete Cloud Scheduler job
    SCHEDULER_JOB_NAME="${GCP_WORKER_JOB_NAME}-scheduler"
    if gcloud scheduler jobs describe "$SCHEDULER_JOB_NAME" \
        --project="$GCP_PROJECT_ID" \
        --location="$GCP_REGION" &>/dev/null; then
        log "Deleting Cloud Scheduler job..."
        gcloud scheduler jobs delete "$SCHEDULER_JOB_NAME" \
            --project="$GCP_PROJECT_ID" \
            --location="$GCP_REGION" \
            --quiet
        success "Scheduler job deleted"
    fi
    
    # Delete Cloud Run Job
    if gcloud run jobs describe "$GCP_WORKER_JOB_NAME" \
        --project="$GCP_PROJECT_ID" \
        --region="$GCP_REGION" &>/dev/null; then
        log "Deleting Cloud Run Job..."
        gcloud run jobs delete "$GCP_WORKER_JOB_NAME" \
            --project="$GCP_PROJECT_ID" \
            --region="$GCP_REGION" \
            --quiet
        success "Worker job deleted"
    fi
    
    # Delete Cloud Run Service
    if gcloud run services describe "$GCP_API_SERVICE_NAME" \
        --project="$GCP_PROJECT_ID" \
        --region="$GCP_REGION" &>/dev/null; then
        log "Deleting Cloud Run Service..."
        gcloud run services delete "$GCP_API_SERVICE_NAME" \
            --project="$GCP_PROJECT_ID" \
            --region="$GCP_REGION" \
            --quiet
        success "API service deleted"
    fi
    
    # Delete Cloud SQL instance
    if gcloud sql instances describe "$GCP_DB_INSTANCE_NAME" \
        --project="$GCP_PROJECT_ID" &>/dev/null; then
        log "Deleting Cloud SQL instance..."
        gcloud sql instances delete "$GCP_DB_INSTANCE_NAME" \
            --project="$GCP_PROJECT_ID" \
            --quiet
        success "Database deleted"
    fi
    
    # Delete secrets
    if gcloud secrets describe "$GCP_DB_PASSWORD_SECRET" \
        --project="$GCP_PROJECT_ID" &>/dev/null; then
        log "Deleting secrets..."
        gcloud secrets delete "$GCP_DB_PASSWORD_SECRET" \
            --project="$GCP_PROJECT_ID" \
            --quiet
        success "Secrets deleted"
    fi
    
    # Delete container images
    log "Deleting container images..."
    gcloud container images delete "${GCP_API_IMAGE}" \
        --project="$GCP_PROJECT_ID" \
        --quiet 2>/dev/null || warn "API image may not exist"
    
    gcloud container images delete "${GCP_WORKER_IMAGE}" \
        --project="$GCP_PROJECT_ID" \
        --quiet 2>/dev/null || warn "Worker image may not exist"
    
    success "All GCP resources cleaned up!"
}

cleanup_gcp
