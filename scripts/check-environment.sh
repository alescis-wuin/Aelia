#!/usr/bin/env bash
set -euo pipefail

missing=0
check() {
    local command_name="$1"
    local label="$2"
    if command -v "$command_name" >/dev/null 2>&1; then
        printf 'ok   %s: %s
' "$label" "$($command_name --version 2>/dev/null | head -n 1)"
    else
        printf 'miss %s: command not found
' "$label" >&2
        missing=1
    fi
}

check java Java
check mvn Maven
check git Git
check make Make

if command -v gh >/dev/null 2>&1; then
    printf 'ok   GitHub CLI: %s
' "$(gh --version | head -n 1)"
else
    printf 'warn GitHub CLI: command not found; remote bootstrap will be unavailable
' >&2
fi

exit "$missing"
