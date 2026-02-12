#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="${1:-src/main/java}"

if [[ ! -d "$ROOT_DIR" ]]; then
  echo "Directory not found: $ROOT_DIR" >&2
  exit 2
fi

status=0

while IFS= read -r -d '' file; do
  base_name="$(basename "$file" .java)"

  # Finds first public top-level type declaration in a POSIX-tool-friendly way.
  declared_name="$({
    sed -E 's://.*$::' "$file" |
      tr -d '\r' |
      grep -Eom1 '^[[:space:]]*public[[:space:]]+(class|interface|enum|record)[[:space:]]+[A-Za-z_][A-Za-z0-9_]*' |
      sed -E 's/^[[:space:]]*public[[:space:]]+(class|interface|enum|record)[[:space:]]+([A-Za-z_][A-Za-z0-9_]*).*$/\2/'
  } || true)"

  if [[ -n "$declared_name" && "$declared_name" != "$base_name" ]]; then
    echo "Mismatch: $file declares public type '$declared_name' but filename is '$base_name.java'"
    status=1
  fi
done < <(find "$ROOT_DIR" -type f -name '*.java' -print0)

if [[ "$status" -eq 0 ]]; then
  echo "OK: no public type/file name mismatches found in $ROOT_DIR"
else
  echo "ERROR: found public type/file name mismatches"
fi

exit "$status"
