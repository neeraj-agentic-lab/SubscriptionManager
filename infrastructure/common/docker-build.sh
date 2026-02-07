#!/bin/bash
# Common Docker build logic

# Note: This script is meant to be sourced, not executed directly
# helpers.sh should already be sourced before this script

build_docker_image() {
    local component=$1  # api or worker
    local tag=${2:-latest}
    
    section "Building Docker Image: $component"
    
    local dockerfile="$PROJECT_ROOT/apps/subscription-$component/Dockerfile"
    local image_name="subscription-$component:$tag"
    
    check_file "$dockerfile"
    
    log "Building $image_name..."
    
    # Build from project root to include all modules
    docker build \
        -f "$dockerfile" \
        -t "$image_name" \
        "$PROJECT_ROOT"
    
    success "Built $image_name"
    
    # Return image name for use in deployment scripts
    echo "$image_name"
}

tag_and_push_image() {
    local local_image=$1
    local remote_image=$2
    
    log "Tagging image: $local_image -> $remote_image"
    docker tag "$local_image" "$remote_image"
    
    log "Pushing image: $remote_image"
    docker push "$remote_image"
    
    success "Pushed $remote_image"
}

# Run if executed directly
if [ "${BASH_SOURCE[0]}" -ef "$0" ]; then
    if [ $# -lt 1 ]; then
        error "Usage: $0 <component> [tag]"
    fi
    
    build_docker_image "$1" "${2:-latest}"
fi
