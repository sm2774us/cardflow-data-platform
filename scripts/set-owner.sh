#!/usr/bin/env bash
# Replaces the sm2774us placeholder with your GitHub username (macOS/Linux/Git Bash).
# Usage: ./scripts/set-owner.sh sm2774us
set -euo pipefail
[ $# -eq 1 ] || { echo "usage: $0 <github-username>"; exit 1; }
cd "$(dirname "$0")/.."
export sm2774us_NAME="$1"
find . -type f \( -name '*.md' -o -name '*.yml' -o -name '*.yaml' -o -name '*.json' -o -name '*.ts' -o -name '*.tf' -o -name CODEsm2774usS \) \
  -not -path '*/node_modules/*' -not -path './.git/*' -not -path '*/target/*' -not -name package-lock.json -print0 |
  xargs -0 perl -pi -e 's/(?<![A-Za-z])sm2774us(?![A-Za-z])/$ENV{sm2774us_NAME}/g'
echo "done. remaining placeholders:"; grep -rnE '(^|[^A-Za-z])sm2774us([^A-Za-z]|$)' --exclude-dir=node_modules --exclude-dir=.git --exclude-dir=scripts . || echo "  none"
