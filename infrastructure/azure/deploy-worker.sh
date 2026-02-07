#!/bin/bash
# Deploy Worker to Azure Container Apps Jobs

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/../common/docker-build.sh"
source "$SCRIPT_DIR/config.sh"

deploy_worker() {
    section "Deploying Worker to Azure Container Apps Jobs"
    
    warn "Azure Container Apps Jobs deployment requires additional setup."
    warn "This is a simplified version. For production, use Azure Bicep or Terraform."
    
    # Build Docker image
    log "Building Worker Docker image..."
    IMAGE_TAG="${ENVIRONMENT:-latest}"
    build_docker_image "worker" "$IMAGE_TAG"
    
    # Create ACR if not exists
    if ! az acr show --name "$AZURE_ACR_NAME" --resource-group "$AZURE_RESOURCE_GROUP" &>/dev/null; then
        log "Creating Azure Container Registry..."
        az acr create \
            --resource-group "$AZURE_RESOURCE_GROUP" \
            --name "$AZURE_ACR_NAME" \
            --sku Basic \
            --location "$AZURE_LOCATION"
        success "ACR created"
    fi
    
    # Login to ACR
    log "Logging in to ACR..."
    az acr login --name "$AZURE_ACR_NAME"
    
    # Tag and push image
    LOCAL_IMAGE="subscription-worker:$IMAGE_TAG"
    REMOTE_IMAGE="${AZURE_WORKER_IMAGE}:${IMAGE_TAG}"
    
    docker tag "$LOCAL_IMAGE" "$REMOTE_IMAGE"
    docker push "$REMOTE_IMAGE"
    
    success "Image pushed to ACR"
    
    log "Worker schedule: ${WORKER_SCHEDULE}"
    log "Image: $REMOTE_IMAGE"
    
    success "Worker deployment prepared!"
    echo ""
    warn "Next steps for Azure Container Apps Jobs:"
    warn "1. Create Container Apps environment"
    warn "2. Create Container Apps Job with image: $REMOTE_IMAGE"
    warn "3. Configure schedule: ${WORKER_SCHEDULE}"
    warn "4. Set up environment variables"
    echo ""
    log "For full automation, consider using Azure Bicep or Terraform."
}

deploy_worker
