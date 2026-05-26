#!/usr/bin/env bash
set -euo pipefail

luciole_zip="${1:-}"
hack_zip="${2:-}"
font_dir="src/main/resources/fr/alescis/aelia/fonts"

mkdir -p "${font_dir}"

extract_font() {
  local zip_file="$1"
  local pattern="$2"
  local output_name="$3"
  if [[ -z "${zip_file}" || ! -f "${zip_file}" ]]; then
    echo "Skipping ${output_name}: archive not provided"
    return 0
  fi
  local match
  match="$(unzip -Z1 "${zip_file}" | grep -E "${pattern}" | head -n 1 || true)"
  if [[ -z "${match}" ]]; then
    echo "Missing ${output_name} in ${zip_file}" >&2
    exit 1
  fi
  unzip -p "${zip_file}" "${match}" > "${font_dir}/${output_name}"
  echo "Imported ${output_name}"
}

extract_font "${luciole_zip}" 'Luciole-Regular\.ttf$' 'Luciole-Regular.ttf'
extract_font "${luciole_zip}" 'Luciole-Bold\.ttf$' 'Luciole-Bold.ttf'
extract_font "${hack_zip}" 'Hack-Regular\.ttf$' 'Hack-Regular.ttf'
extract_font "${hack_zip}" 'Hack-Bold\.ttf$' 'Hack-Bold.ttf'
