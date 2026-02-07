#!/bin/bash
# Deploy Worker to AWS ECS Scheduled Tasks

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/../common/docker-build.sh"
source "$SCRIPT_DIR/config.sh"

deploy_worker() {
    section "Deploying Worker to ECS Scheduled Tasks"
    
    warn "AWS ECS deployment requires additional setup (VPC, ECS cluster, task definitions)."
    warn "This is a simplified version. For production, use AWS CDK or Terraform."
    
    # Build Docker image
    log "Building Worker Docker image..."
    IMAGE_TAG="${ENVIRONMENT:-latest}"
    build_docker_image "worker" "$IMAGE_TAG"
    
    # Create ECR repository if not exists
    if ! aws ecr describe-repositories \
        --repository-names "$AWS_WORKER_TASK_NAME" \
        --region "$AWS_REGION" &>/dev/null; then
        log "Creating ECR repository..."
        aws ecr create-repository \
            --repository-name "$AWS_WORKER_TASK_NAME" \
            --region "$AWS_REGION"
    fi
    
    # Login to ECR
    log "Logging in to ECR..."
    aws ecr get-login-password --region "$AWS_REGION" | \
        docker login --username AWS --password-stdin "$AWS_ECR_REGISTRY"
    
    # Tag and push image
    LOCAL_IMAGE="subscription-worker:$IMAGE_TAG"
    REMOTE_IMAGE="${AWS_WORKER_IMAGE}:${IMAGE_TAG}"
    
    docker tag "$LOCAL_IMAGE" "$REMOTE_IMAGE"
    docker push "$REMOTE_IMAGE"
    
    success "Image pushed to ECR"
    
    log "Worker schedule: ${WORKER_SCHEDULE}"
    log "Image: $REMOTE_IMAGE"
    
    success "Worker deployment prepared!"
    echo ""
    warn "Next steps for AWS ECS Scheduled Tasks:"
    warn "1. Create ECS cluster: aws ecs create-cluster --cluster-name $AWS_ECS_CLUSTER"
    warn "2. Create task definition with image: $REMOTE_IMAGE"
    warn "3. Create EventBridge rule with schedule: ${WORKER_SCHEDULE}"
    warn "4. Configure rule to run ECS task"
    echo ""
    log "For full automation, consider using AWS CDK or Terraform."
}

deploy_worker
