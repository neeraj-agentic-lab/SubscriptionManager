#!/bin/bash
# Build environment variables from deployment-config.yaml

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/helpers.sh"

export_common_vars

build_env_vars() {
    log "Building environment variables from configuration..."
    
    # Read configuration values
    export TARGET_CLOUD=$(read_config "target_cloud")
    export ENVIRONMENT=$(read_config "app.environment")
    export APP_NAME=$(read_config "app.name")
    export APP_VERSION=$(read_config "app.version")
    
    # Database configuration
    export DB_NAME=$(read_config "database.name")
    export DB_USER=$(read_config "database.user")
    export DB_TIER=$(read_config "database.tier")
    export DB_STORAGE_GB=$(read_config "database.storage_gb")
    export DB_BACKUP_RETENTION=$(read_config "database.backup_retention_days")
    
    # API configuration
    export API_NAME=$(read_config "api.name")
    export API_PORT=$(read_config "api.port")
    export API_SIZE=$(read_config "api.compute.size")
    export API_MIN_INSTANCES=$(read_config "api.compute.min_instances")
    export API_MAX_INSTANCES=$(read_config "api.compute.max_instances")
    export API_MEMORY_GB=$(read_config "api.compute.memory_gb")
    export API_CPU=$(read_config "api.compute.cpu")
    
    # Worker configuration
    export WORKER_NAME=$(read_config "worker.name")
    export WORKER_SIZE=$(read_config "worker.compute.size")
    export WORKER_MEMORY_GB=$(read_config "worker.compute.memory_gb")
    export WORKER_CPU=$(read_config "worker.compute.cpu")
    export WORKER_SCHEDULE=$(read_config "worker.schedule")
    
    # Region configuration
    case $TARGET_CLOUD in
        gcp)
            export REGION=$(read_config "regions.gcp")
            ;;
        aws)
            export REGION=$(read_config "regions.aws")
            ;;
        azure)
            export REGION=$(read_config "regions.azure")
            ;;
    esac
    
    # Feature flags
    export RUN_MIGRATIONS=$(read_config "features.run_migrations")
    export ENABLE_MONITORING=$(read_config "features.enable_monitoring")
    export ENABLE_LOGGING=$(read_config "features.enable_logging")
    export ENABLE_AUTO_SCALING=$(read_config "features.enable_auto_scaling")
    
    success "Environment variables built successfully"
}

# Map size to cloud-specific instance types
map_compute_size() {
    local size=$1
    local cloud=$2
    
    case $cloud in
        gcp)
            case $size in
                small) echo "1" ;;   # 1 vCPU
                medium) echo "2" ;;  # 2 vCPU
                large) echo "4" ;;   # 4 vCPU
            esac
            ;;
        aws)
            case $size in
                small) echo "t3.small" ;;
                medium) echo "t3.medium" ;;
                large) echo "t3.large" ;;
            esac
            ;;
        azure)
            case $size in
                small) echo "B1ms" ;;
                medium) echo "B2s" ;;
                large) echo "B4ms" ;;
            esac
            ;;
    esac
}

# Map database tier to cloud-specific instance types
map_database_tier() {
    local tier=$1
    local cloud=$2
    
    case $cloud in
        gcp)
            case $tier in
                small) echo "db-f1-micro" ;;
                medium) echo "db-g1-small" ;;
                large) echo "db-n1-standard-1" ;;
            esac
            ;;
        aws)
            case $tier in
                small) echo "db.t3.micro" ;;
                medium) echo "db.t3.small" ;;
                large) echo "db.t3.medium" ;;
            esac
            ;;
        azure)
            case $tier in
                small) echo "B_Gen5_1" ;;
                medium) echo "GP_Gen5_2" ;;
                large) echo "GP_Gen5_4" ;;
            esac
            ;;
    esac
}

# Run if executed directly
if [ "${BASH_SOURCE[0]}" -ef "$0" ]; then
    build_env_vars
    echo "Environment variables:"
    echo "  TARGET_CLOUD=$TARGET_CLOUD"
    echo "  ENVIRONMENT=$ENVIRONMENT"
    echo "  REGION=$REGION"
    echo "  DB_TIER=$DB_TIER"
    echo "  API_SIZE=$API_SIZE"
fi
