#!/bin/bash
# Setup AWS Secrets Manager

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/config.sh"

setup_secrets() {
    section "Setting up AWS Secrets Manager"
    
    # Get or generate database password
    if [ -z "$AWS_DB_PASSWORD" ]; then
        if aws secretsmanager describe-secret \
            --secret-id "$AWS_DB_PASSWORD_SECRET" \
            --region "$AWS_REGION" &>/dev/null; then
            log "Secret '$AWS_DB_PASSWORD_SECRET' already exists"
            AWS_DB_PASSWORD=$(aws secretsmanager get-secret-value \
                --secret-id "$AWS_DB_PASSWORD_SECRET" \
                --region "$AWS_REGION" \
                --query 'SecretString' \
                --output text)
        else
            log "Generating new database password..."
            AWS_DB_PASSWORD=$(generate_password)
        fi
    fi
    
    # Create or update secret
    if aws secretsmanager describe-secret \
        --secret-id "$AWS_DB_PASSWORD_SECRET" \
        --region "$AWS_REGION" &>/dev/null; then
        log "Updating secret '$AWS_DB_PASSWORD_SECRET'..."
        aws secretsmanager update-secret \
            --secret-id "$AWS_DB_PASSWORD_SECRET" \
            --secret-string "$AWS_DB_PASSWORD" \
            --region "$AWS_REGION"
    else
        log "Creating secret '$AWS_DB_PASSWORD_SECRET'..."
        aws secretsmanager create-secret \
            --name "$AWS_DB_PASSWORD_SECRET" \
            --secret-string "$AWS_DB_PASSWORD" \
            --region "$AWS_REGION"
    fi
    
    success "Secret created/updated"
    success "Secrets Manager setup complete!"
}

setup_secrets
