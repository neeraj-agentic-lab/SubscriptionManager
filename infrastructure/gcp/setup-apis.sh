#!/bin/bash
# Enable all required GCP APIs

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/config.sh"

enable_apis() {
    section "Enabling Required GCP APIs"
    
    log "Enabling all required APIs for Subscription Manager..."
    log "This may take a few minutes..."
    
    # Enable all required APIs in one command
    gcloud services enable \
        run.googleapis.com \
        sql-component.googleapis.com \
        sqladmin.googleapis.com \
        secretmanager.googleapis.com \
        artifactregistry.googleapis.com \
        cloudscheduler.googleapis.com \
        cloudresourcemanager.googleapis.com \
        compute.googleapis.com \
        servicenetworking.googleapis.com \
        --project="$GCP_PROJECT_ID"
    
    success "All required APIs enabled!"
    
    log "Enabled APIs:"
    log "  ✓ Cloud Run (run.googleapis.com)"
    log "  ✓ Cloud SQL Admin (sqladmin.googleapis.com)"
    log "  ✓ Cloud SQL Component (sql-component.googleapis.com)"
    log "  ✓ Secret Manager (secretmanager.googleapis.com)"
    log "  ✓ Artifact Registry (artifactregistry.googleapis.com)"
    log "  ✓ Cloud Scheduler (cloudscheduler.googleapis.com)"
    log "  ✓ Cloud Resource Manager (cloudresourcemanager.googleapis.com)"
    log "  ✓ Compute Engine (compute.googleapis.com)"
    log "  ✓ Service Networking (servicenetworking.googleapis.com)"
    
    log "Waiting for API enablement to propagate..."
    sleep 5
    
    success "API setup complete!"
}

enable_apis
