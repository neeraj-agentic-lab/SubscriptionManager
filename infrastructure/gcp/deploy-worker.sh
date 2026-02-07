#!/bin/bash
# Deploy Worker to GCP Cloud Run Jobs

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/../common/docker-build.sh"
source "$SCRIPT_DIR/config.sh"

deploy_worker() {
    section "Deploying Worker to Cloud Run Jobs"
    
    # Build Docker image
    log "Building Worker Docker image..."
    IMAGE_TAG="${ENVIRONMENT:-latest}"
    build_docker_image "worker" "$IMAGE_TAG"
    
    # Tag for GCR
    LOCAL_IMAGE="subscription-worker:$IMAGE_TAG"
    REMOTE_IMAGE="${GCP_WORKER_IMAGE}:${IMAGE_TAG}"
    
    log "Tagging image for GCR..."
    docker tag "$LOCAL_IMAGE" "$REMOTE_IMAGE"
    
    # Configure Docker for GCR
    gcloud auth configure-docker --quiet
    
    # Push to GCR
    log "Pushing image to GCR..."
    docker push "$REMOTE_IMAGE"
    success "Image pushed to GCR"
    
    # Map compute size to Cloud Run CPU/memory
    case ${WORKER_SIZE:-small} in
        small)
            CPU="1"
            MEMORY="2Gi"
            ;;
        medium)
            CPU="2"
            MEMORY="4Gi"
            ;;
        large)
            CPU="4"
            MEMORY="8Gi"
            ;;
        *)
            CPU="1"
            MEMORY="2Gi"
            ;;
    esac
    
    # Build database URL for Cloud SQL
    DB_URL="jdbc:postgresql:///${GCP_DB_NAME}?cloudSqlInstance=${GCP_PROJECT_ID}:${GCP_REGION}:${GCP_DB_INSTANCE_NAME}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
    
    # Check if job exists
    if gcloud run jobs describe "$GCP_WORKER_JOB_NAME" \
        --project="$GCP_PROJECT_ID" \
        --region="$GCP_REGION" &>/dev/null; then
        log "Updating existing Cloud Run Job..."
        
        gcloud run jobs update "$GCP_WORKER_JOB_NAME" \
            --image="$REMOTE_IMAGE" \
            --project="$GCP_PROJECT_ID" \
            --region="$GCP_REGION" \
            --cpu="$CPU" \
            --memory="$MEMORY" \
            --max-retries=3 \
            --task-timeout=3600 \
            --add-cloudsql-instances="${GCP_PROJECT_ID}:${GCP_REGION}:${GCP_DB_INSTANCE_NAME}" \
            --set-env-vars="DATABASE_URL=${DB_URL}" \
            --set-env-vars="DATABASE_USER=${GCP_DB_USER}" \
            --set-env-vars="SPRING_PROFILES_ACTIVE=${ENVIRONMENT:-production}" \
            --update-secrets="DATABASE_PASSWORD=${GCP_DB_PASSWORD_SECRET}:latest"
    else
        log "Creating new Cloud Run Job..."
        
        gcloud run jobs create "$GCP_WORKER_JOB_NAME" \
            --image="$REMOTE_IMAGE" \
            --project="$GCP_PROJECT_ID" \
            --region="$GCP_REGION" \
            --cpu="$CPU" \
            --memory="$MEMORY" \
            --max-retries=3 \
            --task-timeout=3600 \
            --add-cloudsql-instances="${GCP_PROJECT_ID}:${GCP_REGION}:${GCP_DB_INSTANCE_NAME}" \
            --set-env-vars="DATABASE_URL=${DB_URL}" \
            --set-env-vars="DATABASE_USER=${GCP_DB_USER}" \
            --set-env-vars="SPRING_PROFILES_ACTIVE=${ENVIRONMENT:-production}" \
            --update-secrets="DATABASE_PASSWORD=${GCP_DB_PASSWORD_SECRET}:latest"
    fi
    
    # Create Cloud Scheduler job for periodic execution
    SCHEDULER_JOB_NAME="${GCP_WORKER_JOB_NAME}-scheduler"
    SCHEDULE="${WORKER_SCHEDULE:-0 */1 * * *}"
    
    log "Setting up Cloud Scheduler..."
    
    if gcloud scheduler jobs describe "$SCHEDULER_JOB_NAME" \
        --project="$GCP_PROJECT_ID" \
        --location="$GCP_REGION" &>/dev/null; then
        log "Updating existing scheduler job..."
        
        gcloud scheduler jobs update http "$SCHEDULER_JOB_NAME" \
            --project="$GCP_PROJECT_ID" \
            --location="$GCP_REGION" \
            --schedule="$SCHEDULE" \
            --uri="https://${GCP_REGION}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${GCP_PROJECT_ID}/jobs/${GCP_WORKER_JOB_NAME}:run" \
            --http-method=POST \
            --oauth-service-account-email="${GCP_PROJECT_ID}@appspot.gserviceaccount.com" \
            2>/dev/null || warn "Scheduler update may have failed"
    else
        log "Creating new scheduler job..."
        
        gcloud scheduler jobs create http "$SCHEDULER_JOB_NAME" \
            --project="$GCP_PROJECT_ID" \
            --location="$GCP_REGION" \
            --schedule="$SCHEDULE" \
            --uri="https://${GCP_REGION}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${GCP_PROJECT_ID}/jobs/${GCP_WORKER_JOB_NAME}:run" \
            --http-method=POST \
            --oauth-service-account-email="${GCP_PROJECT_ID}@appspot.gserviceaccount.com" \
            2>/dev/null || warn "Scheduler creation may have failed"
    fi
    
    success "Worker deployed to Cloud Run Jobs!"
    echo ""
    log "Worker Details:"
    log "  Job: $GCP_WORKER_JOB_NAME"
    log "  Region: $GCP_REGION"
    log "  CPU: $CPU"
    log "  Memory: $MEMORY"
    log "  Schedule: $SCHEDULE"
    echo ""
    log "Manual execution:"
    log "  gcloud run jobs execute $GCP_WORKER_JOB_NAME --project=$GCP_PROJECT_ID --region=$GCP_REGION"
    echo ""
}

deploy_worker
