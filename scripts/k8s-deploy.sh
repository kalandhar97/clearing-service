#!/usr/bin/env bash
# Apply / sync Kubernetes manifests (kustomize) for local/dev clusters.
# Usage:
#   ./scripts/k8s-deploy.sh                 # platform base (ns + vault + obs + clearing-service)
#   ./scripts/k8s-deploy.sh observability   # observability only
#   ./scripts/k8s-deploy.sh clearing        # clearing-service kustomize overlay
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="${1:-base}"

case "$TARGET" in
  base|platform)
    PATH_DIR="$ROOT/k8s/base"
    ;;
  observability|obs)
    PATH_DIR="$ROOT/k8s/observability"
    ;;
  clearing|clearing-service)
    PATH_DIR="$ROOT/k8s/services/clearing-service"
    ;;
  *)
    echo "Unknown target: $TARGET (base|observability|clearing)" >&2
    exit 1
    ;;
esac

kubectl apply -k "$PATH_DIR"
echo "Applied $PATH_DIR"
