#!/bin/bash
# Deploy API to Azure Container Apps

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/../common/docker-build.sh"
source "$SCRIPT_DIR/config.sh"

deploy_api() {
    section "Deploying API to Azure Container Apps"
    
    warn "Azure Container Apps deployment requires additional setup."
    warn "This is a simplified version. For production, use Azure Bicep or Terraform."
    
    # Build Docker image
    log "Building API Docker image..."
    IMAGE_TAG="${ENVIRONMENT:-latest}"
    build_docker_image "api" "$IMAGE_TAG"
    
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
    LOCAL_IMAGE="subscription-api:$IMAGE_TAG"
    REMOTE_IMAGE="${AZURE_API_IMAGE}:${IMAGE_TAG}"
    
    docker tag "$LOCAL_IMAGE" "$REMOTE_IMAGE"
    docker push "$REMOTE_IMAGE"
    
    success "Image pushed to ACR"
    
    # Get database connection details
    DB_HOST="${AZURE_DB_SERVER_NAME}.postgres.database.azure.com"
    DB_URL="jdbc:postgresql://${DB_HOST}:5432/${AZURE_DB_NAME}?sslmode=require"
    
    log "Database URL: $DB_URL"
    log "Image: $REMOTE_IMAGE"
    
    success "API deployment prepared!"
    echo ""
    warn "Next steps for Azure Container Apps:"
    warn "1. Create Container Apps environment"
    warn "2. Create Container App with image: $REMOTE_IMAGE"
    warn "3. Configure environment variables"
    warn "4. Set up ingress"
    echo ""
    log "For full automation, consider using Azure Bicep or Terraform."
}

deploy_api
