#!/bin/bash
# Deploy API to AWS ECS Fargate

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/../common/docker-build.sh"
source "$SCRIPT_DIR/config.sh"

deploy_api() {
    section "Deploying API to ECS Fargate"
    
    warn "AWS ECS deployment requires additional setup (VPC, ECS cluster, task definitions)."
    warn "This is a simplified version. For production, use AWS CDK or Terraform."
    
    # Build Docker image
    log "Building API Docker image..."
    IMAGE_TAG="${ENVIRONMENT:-latest}"
    build_docker_image "api" "$IMAGE_TAG"
    
    # Create ECR repository if not exists
    if ! aws ecr describe-repositories \
        --repository-names "$AWS_API_SERVICE_NAME" \
        --region "$AWS_REGION" &>/dev/null; then
        log "Creating ECR repository..."
        aws ecr create-repository \
            --repository-name "$AWS_API_SERVICE_NAME" \
            --region "$AWS_REGION"
    fi
    
    # Login to ECR
    log "Logging in to ECR..."
    aws ecr get-login-password --region "$AWS_REGION" | \
        docker login --username AWS --password-stdin "$AWS_ECR_REGISTRY"
    
    # Tag and push image
    LOCAL_IMAGE="subscription-api:$IMAGE_TAG"
    REMOTE_IMAGE="${AWS_API_IMAGE}:${IMAGE_TAG}"
    
    docker tag "$LOCAL_IMAGE" "$REMOTE_IMAGE"
    docker push "$REMOTE_IMAGE"
    
    success "Image pushed to ECR"
    
    # Get database endpoint
    DB_ENDPOINT=$(aws rds describe-db-instances \
        --db-instance-identifier "$AWS_DB_INSTANCE_ID" \
        --region "$AWS_REGION" \
        --query 'DBInstances[0].Endpoint.Address' \
        --output text)
    
    DB_URL="jdbc:postgresql://${DB_ENDPOINT}:5432/${AWS_DB_NAME}"
    
    log "Database URL: $DB_URL"
    log "Image: $REMOTE_IMAGE"
    
    success "API deployment prepared!"
    echo ""
    warn "Next steps for AWS ECS:"
    warn "1. Create ECS cluster: aws ecs create-cluster --cluster-name $AWS_ECS_CLUSTER"
    warn "2. Create task definition with image: $REMOTE_IMAGE"
    warn "3. Create ECS service"
    warn "4. Configure load balancer"
    echo ""
    log "For full automation, consider using AWS CDK or Terraform."
}

deploy_api
