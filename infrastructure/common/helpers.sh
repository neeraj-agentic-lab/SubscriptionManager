#!/bin/bash
# Common helper functions for deployment scripts

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
    exit 1
}

# Check if command exists
check_command() {
    if ! command -v "$1" &> /dev/null; then
        error "$1 is not installed. Please install it first."
    fi
}

# Check if file exists
check_file() {
    if [ ! -f "$1" ]; then
        error "File not found: $1"
    fi
}

# Check if directory exists
check_dir() {
    if [ ! -d "$1" ]; then
        error "Directory not found: $1"
    fi
}

# Read value from YAML config
read_config() {
    local key=$1
    local config_file="${PROJECT_ROOT}/deployment-config.yaml"
    
    if ! command -v yq &> /dev/null; then
        error "yq is not installed. Please install it: https://github.com/mikefarah/yq"
    fi
    
    yq ".$key" "$config_file"
}

# Generate random password
generate_password() {
    openssl rand -base64 32 | tr -d "=+/" | cut -c1-25
}

# Wait for resource to be ready
wait_for_resource() {
    local resource_name=$1
    local check_command=$2
    local max_attempts=${3:-30}
    local sleep_seconds=${4:-10}
    
    log "Waiting for $resource_name to be ready..."
    
    local attempt=1
    while [ $attempt -le $max_attempts ]; do
        if eval "$check_command" &> /dev/null; then
            success "$resource_name is ready"
            return 0
        fi
        
        log "Attempt $attempt/$max_attempts - waiting ${sleep_seconds}s..."
        sleep $sleep_seconds
        ((attempt++))
    done
    
    error "$resource_name did not become ready in time"
}

# Confirm action
confirm() {
    local message=$1
    local default=${2:-n}
    
    if [ "$default" = "y" ]; then
        local prompt="$message [Y/n]: "
    else
        local prompt="$message [y/N]: "
    fi
    
    read -p "$prompt" -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        return 0
    else
        return 1
    fi
}

# Print section header
section() {
    echo ""
    echo "=========================================="
    echo "$1"
    echo "=========================================="
    echo ""
}

# Export common environment variables
export_common_vars() {
    export PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
    export INFRASTRUCTURE_DIR="$PROJECT_ROOT/infrastructure"
    export CONFIG_FILE="$PROJECT_ROOT/deployment-config.yaml"
}
