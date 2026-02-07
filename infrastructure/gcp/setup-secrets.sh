#!/bin/bash
# Setup Secret Manager on GCP

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/config.sh"

setup_secrets() {
    section "Setting up Secret Manager"
    
    # Get or generate database password
    if [ -z "$GCP_DB_PASSWORD" ]; then
        # Try to get existing password from Secret Manager
        if gcloud secrets describe "$GCP_DB_PASSWORD_SECRET" \
            --project="$GCP_PROJECT_ID" &>/dev/null; then
            log "Secret '$GCP_DB_PASSWORD_SECRET' already exists"
            GCP_DB_PASSWORD=$(gcloud secrets versions access latest \
                --secret="$GCP_DB_PASSWORD_SECRET" \
                --project="$GCP_PROJECT_ID")
        else
            # Generate new password
            log "Generating new database password..."
            GCP_DB_PASSWORD=$(generate_password)
        fi
    fi
    
    # Create or update secret
    if gcloud secrets describe "$GCP_DB_PASSWORD_SECRET" \
        --project="$GCP_PROJECT_ID" &>/dev/null; then
        log "Updating secret '$GCP_DB_PASSWORD_SECRET'..."
        echo -n "$GCP_DB_PASSWORD" | gcloud secrets versions add "$GCP_DB_PASSWORD_SECRET" \
            --project="$GCP_PROJECT_ID" \
            --data-file=-
    else
        log "Creating secret '$GCP_DB_PASSWORD_SECRET'..."
        echo -n "$GCP_DB_PASSWORD" | gcloud secrets create "$GCP_DB_PASSWORD_SECRET" \
            --project="$GCP_PROJECT_ID" \
            --replication-policy="automatic" \
            --data-file=-
    fi
    
    success "Secret created/updated"
    
    # Grant Cloud Run default service account access to secrets
    # Cloud Run uses the Compute Engine default service account
    # Get project number using gcloud (Cloud Resource Manager API must be enabled)
    log "Fetching project number..."
    PROJECT_NUMBER=$(gcloud projects describe "$GCP_PROJECT_ID" --format='value(projectNumber)')
    COMPUTE_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
    
    log "Granting Cloud Run (Compute Engine) service account access to secrets..."
    log "  Secret: $GCP_DB_PASSWORD_SECRET"
    log "  Service account: $COMPUTE_SA"
    log "  Role: roles/secretmanager.secretAccessor"
    
    # Grant the permission
    if gcloud secrets add-iam-policy-binding "$GCP_DB_PASSWORD_SECRET" \
        --project="$GCP_PROJECT_ID" \
        --member="serviceAccount:${COMPUTE_SA}" \
        --role="roles/secretmanager.secretAccessor"; then
        success "Permission granted successfully"
    else
        warn "Failed to grant permission or permission already exists"
    fi
    
    # Verify the permission was granted
    log "Verifying IAM policy..."
    gcloud secrets get-iam-policy "$GCP_DB_PASSWORD_SECRET" \
        --project="$GCP_PROJECT_ID" \
        --format="table(bindings.members)" | grep -q "$COMPUTE_SA" && \
        success "Verified: $COMPUTE_SA has access to secret" || \
        warn "Warning: Could not verify service account has access"
    
    # Also grant to App Engine service account (for compatibility)
    APP_ENGINE_SA="${GCP_PROJECT_ID}@appspot.gserviceaccount.com"
    gcloud secrets add-iam-policy-binding "$GCP_DB_PASSWORD_SECRET" \
        --project="$GCP_PROJECT_ID" \
        --member="serviceAccount:${APP_ENGINE_SA}" \
        --role="roles/secretmanager.secretAccessor" \
        2>/dev/null || true
    
    success "Secret Manager setup complete!"
}

setup_secrets
