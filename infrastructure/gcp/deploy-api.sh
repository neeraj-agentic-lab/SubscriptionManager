#!/bin/bash
# Deploy API to GCP Cloud Run

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/../common/docker-build.sh"
source "$SCRIPT_DIR/config.sh"

deploy_api() {
    section "Deploying API to Cloud Run"
    
    # Build Docker image
    log "Building API Docker image..."
    IMAGE_TAG="${ENVIRONMENT:-latest}"
    build_docker_image "api" "$IMAGE_TAG"
    
    # Tag for GCR
    LOCAL_IMAGE="subscription-api:$IMAGE_TAG"
    REMOTE_IMAGE="${GCP_API_IMAGE}:${IMAGE_TAG}"
    
    log "Tagging image for GCR..."
    docker tag "$LOCAL_IMAGE" "$REMOTE_IMAGE"
    
    # Configure Docker for GCR
    log "Configuring Docker authentication..."
    gcloud auth configure-docker --quiet
    
    # Push to GCR
    log "Pushing image to GCR..."
    docker push "$REMOTE_IMAGE"
    success "Image pushed to GCR"
    
    # Map compute size to Cloud Run CPU/memory
    case ${API_SIZE:-small} in
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
    
    # Deploy to Cloud Run
    log "Deploying to Cloud Run..."
    gcloud run deploy "$GCP_API_SERVICE_NAME" \
        --image="$REMOTE_IMAGE" \
        --project="$GCP_PROJECT_ID" \
        --region="$GCP_REGION" \
        --platform=managed \
        --allow-unauthenticated \
        --port=8080 \
        --cpu="$CPU" \
        --memory="$MEMORY" \
        --min-instances="${API_MIN_INSTANCES:-1}" \
        --max-instances="${API_MAX_INSTANCES:-10}" \
        --add-cloudsql-instances="${GCP_PROJECT_ID}:${GCP_REGION}:${GCP_DB_INSTANCE_NAME}" \
        --set-env-vars="DATABASE_URL=${DB_URL}" \
        --set-env-vars="DATABASE_USER=${GCP_DB_USER}" \
        --set-env-vars="SPRING_PROFILES_ACTIVE=${ENVIRONMENT:-production}" \
        --update-secrets="DATABASE_PASSWORD=${GCP_DB_PASSWORD_SECRET}:latest"
    
    # Get service URL
    SERVICE_URL=$(gcloud run services describe "$GCP_API_SERVICE_NAME" \
        --project="$GCP_PROJECT_ID" \
        --region="$GCP_REGION" \
        --format="value(status.url)")
    
    success "API deployed to Cloud Run!"
    echo ""
    log "API Details:"
    log "  Service: $GCP_API_SERVICE_NAME"
    log "  URL: $SERVICE_URL"
    log "  Region: $GCP_REGION"
    log "  CPU: $CPU"
    log "  Memory: $MEMORY"
    log "  Min Instances: ${API_MIN_INSTANCES:-1}"
    log "  Max Instances: ${API_MAX_INSTANCES:-10}"
    echo ""
    log "Health Check: ${SERVICE_URL}/actuator/health"
    log "Swagger UI: ${SERVICE_URL}/swagger-ui.html"
    echo ""
}

deploy_api
