#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

USE_NATIVE=false
SKIP_TESTS=true
USE_CLEAN=false

usage() {
  cat <<'EOF'
Usage: ./build.sh [options]

Options:
  --native    Build native image 
  --tests     Run tests (default is to skip tests)
  --clean     Run clean before package
  -h, --help  Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --native)
      USE_NATIVE=true
      shift
      ;;
    --tests)
      SKIP_TESTS=false
      shift
      ;;
    --clean)
      USE_CLEAN=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

MAVEN_ARGS=(-pl standalone -am)
if [[ "$USE_NATIVE" == "true" ]]; then
  MAVEN_ARGS+=(-Pnative)
fi

if [[ "$SKIP_TESTS" == "true" ]]; then
  MAVEN_ARGS+=(-DskipTests)
fi

if [[ "$USE_CLEAN" == "true" ]]; then
  MAVEN_ARGS+=(clean package)
else
  MAVEN_ARGS+=(package)
fi

echo ">>> mvn ${MAVEN_ARGS[*]}"
mvn "${MAVEN_ARGS[@]}"

echo
echo "Build finished."
echo "JVM launcher: ./sl"
if [[ "$USE_NATIVE" == "true" ]]; then
  echo "Native binary: ./standalone/target/slnative"
fi
