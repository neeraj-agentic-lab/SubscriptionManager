#!/bin/bash
# AWS-specific configuration

# AWS Account Configuration
export AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-$(aws sts get-caller-identity --query Account --output text 2>/dev/null)}"
export AWS_REGION="${REGION:-us-east-1}"

# Database Configuration
export AWS_DB_INSTANCE_ID="${DB_NAME:-subscription-db}"
export AWS_DB_NAME="${DB_NAME:-subscription_db}"
export AWS_DB_USER="${DB_USER:-subscription_app}"

# ECS Configuration
export AWS_ECS_CLUSTER="subscription-cluster"
export AWS_API_SERVICE_NAME="${API_NAME:-subscription-api}"
export AWS_WORKER_TASK_NAME="${WORKER_NAME:-subscription-worker}"

# ECR Configuration
export AWS_ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
export AWS_API_IMAGE="${AWS_ECR_REGISTRY}/${AWS_API_SERVICE_NAME}"
export AWS_WORKER_IMAGE="${AWS_ECR_REGISTRY}/${AWS_WORKER_TASK_NAME}"

# Secrets Manager
export AWS_DB_PASSWORD_SECRET="subscription-db-password"

# VPC Configuration (will be created if not exists)
export AWS_VPC_NAME="subscription-vpc"

# Validate AWS account is set
if [ -z "$AWS_ACCOUNT_ID" ]; then
    echo "Error: AWS_ACCOUNT_ID is not set. Please configure AWS CLI."
    exit 1
fi

echo "AWS Configuration:"
echo "  Account ID: $AWS_ACCOUNT_ID"
echo "  Region: $AWS_REGION"
echo "  Database Instance: $AWS_DB_INSTANCE_ID"
echo "  ECS Cluster: $AWS_ECS_CLUSTER"
echo "  API Service: $AWS_API_SERVICE_NAME"
echo "  Worker Task: $AWS_WORKER_TASK_NAME"
