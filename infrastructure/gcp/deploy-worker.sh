#!/bin/bash
# Deploy Worker to GCP Cloud Run Jobs

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/../common/docker-build.sh"
source "$SCRIPT_DIR/config.sh"

deploy_worker() {
    section "Deploying Worker to Cloud Run Service"
    
    # Build Docker image
    log "Building Worker Docker image..."
    IMAGE_TAG="${ENVIRONMENT:-latest}"
    build_docker_image "worker" "$IMAGE_TAG"
    
    # Tag for Artifact Registry
    LOCAL_IMAGE="subscription-worker:$IMAGE_TAG"
    AR_LOCATION="${GCP_REGION}"
    AR_REPO="subscription-manager"
    REMOTE_IMAGE="${AR_LOCATION}-docker.pkg.dev/${GCP_PROJECT_ID}/${AR_REPO}/subscription-worker:${IMAGE_TAG}"
    
    log "Tagging image for Artifact Registry..."
    docker tag "$LOCAL_IMAGE" "$REMOTE_IMAGE"
    
    # Create Artifact Registry repository if it doesn't exist
    log "Ensuring Artifact Registry repository exists..."
    gcloud artifacts repositories describe "$AR_REPO" \
        --location="$AR_LOCATION" \
        --project="$GCP_PROJECT_ID" &>/dev/null || \
    gcloud artifacts repositories create "$AR_REPO" \
        --repository-format=docker \
        --location="$AR_LOCATION" \
        --project="$GCP_PROJECT_ID" \
        --description="Docker images for Subscription Manager"
    
    # Configure Docker for Artifact Registry
    log "Configuring Docker authentication..."
    gcloud auth configure-docker "${AR_LOCATION}-docker.pkg.dev" --quiet
    
    # Push to Artifact Registry
    log "Pushing image to Artifact Registry..."
    docker push "$REMOTE_IMAGE"
    success "Image pushed to Artifact Registry"
    
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
    
    # Deploy Worker as Cloud Run Service (always-on for @Scheduled jobs)
    log "Deploying Worker as Cloud Run Service (always-on)..."
    gcloud run deploy "$GCP_WORKER_SERVICE_NAME" \
        --image="$REMOTE_IMAGE" \
        --project="$GCP_PROJECT_ID" \
        --region="$GCP_REGION" \
        --platform=managed \
        --no-allow-unauthenticated \
        --ingress=internal \
        --port=8081 \
        --cpu="$CPU" \
        --memory="$MEMORY" \
        --min-instances=1 \
        --max-instances=1 \
        --add-cloudsql-instances="${GCP_PROJECT_ID}:${GCP_REGION}:${GCP_DB_INSTANCE_NAME}" \
        --set-env-vars="DATABASE_URL=${DB_URL}" \
        --set-env-vars="DATABASE_USER=${GCP_DB_USER}" \
        --set-env-vars="SPRING_PROFILES_ACTIVE=${ENVIRONMENT:-production}" \
        --update-secrets="DATABASE_PASSWORD=${GCP_DB_PASSWORD_SECRET}:latest"
    
    # Get service URL (for internal access only)
    SERVICE_URL=$(gcloud run services describe "$GCP_WORKER_SERVICE_NAME" \
        --project="$GCP_PROJECT_ID" \
        --region="$GCP_REGION" \
        --format="value(status.url)" 2>/dev/null || echo "N/A")
    
    success "Worker deployed to Cloud Run Service!"
    echo ""
    log "Worker Details:"
    log "  Service: $GCP_WORKER_SERVICE_NAME"
    log "  URL (internal): $SERVICE_URL"
    log "  Region: $GCP_REGION"
    log "  CPU: $CPU"
    log "  Memory: $MEMORY"
    log "  Min Instances: 1 (always running)"
    log "  Max Instances: 1 (single instance)"
    log "  Ingress: internal (no public access)"
    echo ""
    log "Worker runs continuously and executes @Scheduled jobs:"
    log "  - Task processing (every 30 seconds)"
    log "  - Lock cleanup (every 5 minutes)"
    log "  - Nonce cleanup (every 5 minutes)"
    log "  - Rate limit cleanup (every 1 hour)"
    log "  - Webhook delivery (every 5-10 seconds)"
    echo ""
    log "Health check:"
    log "  Internal URL: ${SERVICE_URL}/actuator/health"
    echo ""
}

deploy_worker
