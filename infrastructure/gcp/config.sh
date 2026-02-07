#!/bin/bash
# GCP-specific configuration

# GCP Project Configuration
export GCP_PROJECT_ID="${GCP_PROJECT_ID:-$(gcloud config get-value project 2>/dev/null)}"
export GCP_REGION="${REGION:-us-central1}"

# Database Configuration
export GCP_DB_INSTANCE_NAME="${DB_NAME:-subscription-db}"
export GCP_DB_NAME="${DB_NAME:-subscription_db}"
export GCP_DB_USER="${DB_USER:-subscription_app}"

# API Configuration
export GCP_API_SERVICE_NAME="${API_NAME:-subscription-api}"
export GCP_API_IMAGE="gcr.io/${GCP_PROJECT_ID}/${GCP_API_SERVICE_NAME}"

# Worker Configuration
export GCP_WORKER_JOB_NAME="${WORKER_NAME:-subscription-worker}"
export GCP_WORKER_IMAGE="gcr.io/${GCP_PROJECT_ID}/${GCP_WORKER_JOB_NAME}"

# Secret Manager
export GCP_DB_PASSWORD_SECRET="db-password"

# Networking
export GCP_VPC_CONNECTOR="${GCP_VPC_CONNECTOR:-cloud-run-connector}"

# Validate GCP project is set
if [ -z "$GCP_PROJECT_ID" ]; then
    echo "Error: GCP_PROJECT_ID is not set. Please set it or configure gcloud."
    exit 1
fi

echo "GCP Configuration:"
echo "  Project ID: $GCP_PROJECT_ID"
echo "  Region: $GCP_REGION"
echo "  Database Instance: $GCP_DB_INSTANCE_NAME"
echo "  API Service: $GCP_API_SERVICE_NAME"
echo "  Worker Job: $GCP_WORKER_JOB_NAME"
