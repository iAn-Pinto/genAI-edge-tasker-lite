#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${PROJECT_ROOT:-$(cd "$SCRIPT_DIR/../.." && pwd)}"
APP_ID="${APP_ID:-com.example.privytaskai}"
APP_ACTIVITY="${APP_ACTIVITY:-.presentation.MainActivity}"

export PROJECT_ROOT APP_ID APP_ACTIVITY
cd "$SCRIPT_DIR"

exec node index.js

