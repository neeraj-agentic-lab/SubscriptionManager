#!/bin/bash
# Azure-specific configuration

# Azure Configuration
export AZURE_SUBSCRIPTION_ID="${AZURE_SUBSCRIPTION_ID:-$(az account show --query id -o tsv 2>/dev/null)}"
export AZURE_RESOURCE_GROUP="${AZURE_RESOURCE_GROUP:-subscription-rg}"
export AZURE_LOCATION="${REGION:-eastus}"

# Database Configuration
export AZURE_DB_SERVER_NAME="${DB_NAME:-subscription-db-server}"
export AZURE_DB_NAME="${DB_NAME:-subscription_db}"
export AZURE_DB_USER="${DB_USER:-subscription_app}"

# Container Apps Configuration
export AZURE_CONTAINER_ENV="subscription-env"
export AZURE_API_APP_NAME="${API_NAME:-subscription-api}"
export AZURE_WORKER_JOB_NAME="${WORKER_NAME:-subscription-worker}"

# Container Registry
export AZURE_ACR_NAME="subscriptionacr${RANDOM}"
export AZURE_API_IMAGE="${AZURE_ACR_NAME}.azurecr.io/${AZURE_API_APP_NAME}"
export AZURE_WORKER_IMAGE="${AZURE_ACR_NAME}.azurecr.io/${AZURE_WORKER_JOB_NAME}"

# Key Vault
export AZURE_KEYVAULT_NAME="subscription-kv-${RANDOM}"
export AZURE_DB_PASSWORD_SECRET="db-password"

# Validate Azure subscription is set
if [ -z "$AZURE_SUBSCRIPTION_ID" ]; then
    echo "Error: AZURE_SUBSCRIPTION_ID is not set. Please configure Azure CLI."
    exit 1
fi

echo "Azure Configuration:"
echo "  Subscription ID: $AZURE_SUBSCRIPTION_ID"
echo "  Resource Group: $AZURE_RESOURCE_GROUP"
echo "  Location: $AZURE_LOCATION"
echo "  Database Server: $AZURE_DB_SERVER_NAME"
echo "  API App: $AZURE_API_APP_NAME"
echo "  Worker Job: $AZURE_WORKER_JOB_NAME"
