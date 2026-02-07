#!/bin/bash
# Cleanup all AWS resources

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../common/helpers.sh"
source "$SCRIPT_DIR/config.sh"

cleanup_aws() {
    section "Cleaning up AWS Resources"
    
    warn "This will delete ALL resources including the database!"
    warn "This action cannot be undone."
    echo ""
    
    if ! confirm "Are you sure you want to delete all AWS resources?" "n"; then
        log "Cleanup cancelled"
        exit 0
    fi
    
    # Delete RDS instance
    if aws rds describe-db-instances \
        --db-instance-identifier "$AWS_DB_INSTANCE_ID" \
        --region "$AWS_REGION" &>/dev/null; then
        log "Deleting RDS instance..."
        aws rds delete-db-instance \
            --db-instance-identifier "$AWS_DB_INSTANCE_ID" \
            --skip-final-snapshot \
            --region "$AWS_REGION"
        success "RDS instance deletion initiated"
    fi
    
    # Delete secrets
    if aws secretsmanager describe-secret \
        --secret-id "$AWS_DB_PASSWORD_SECRET" \
        --region "$AWS_REGION" &>/dev/null; then
        log "Deleting secrets..."
        aws secretsmanager delete-secret \
            --secret-id "$AWS_DB_PASSWORD_SECRET" \
            --force-delete-without-recovery \
            --region "$AWS_REGION"
        success "Secrets deleted"
    fi
    
    # Delete ECR repositories
    log "Deleting ECR repositories..."
    aws ecr delete-repository \
        --repository-name "$AWS_API_SERVICE_NAME" \
        --force \
        --region "$AWS_REGION" 2>/dev/null || warn "API repository may not exist"
    
    aws ecr delete-repository \
        --repository-name "$AWS_WORKER_TASK_NAME" \
        --force \
        --region "$AWS_REGION" 2>/dev/null || warn "Worker repository may not exist"
    
    success "All AWS resources cleaned up!"
    warn "Note: ECS cluster and task definitions may need manual cleanup"
}

cleanup_aws
