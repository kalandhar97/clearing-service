#!/usr/bin/env bash
# Build clearing-service container image locally.
# Usage: ./scripts/docker-build.sh [tag]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TAG="${1:-clearing-service:local}"

cd "$ROOT"
docker build -f Dockerfile -t "$TAG" .
echo "Built $TAG"
