#!/usr/bin/env bash
set -euo pipefail

require() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require java
require javac
require mvn
require git

java_version="$(java -version 2>&1 | head -n 1)"
maven_version="$(mvn -version | head -n 1)"

echo "Java:  ${java_version}"
echo "Maven: ${maven_version}"
echo "Git:   $(git --version)"
