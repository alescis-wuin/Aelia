#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 /path/to/Luciole_webfonts.zip /path/to/Hack-v3.003-ttf.zip" >&2
  exit 1
fi

luciole_zip="$1"
hack_zip="$2"
target_dir="src/main/resources/fr/alescis/aelia/fonts"

if [[ ! -f "${luciole_zip}" ]]; then
  echo "Luciole archive not found: ${luciole_zip}" >&2
  exit 1
fi

if [[ ! -f "${hack_zip}" ]]; then
  echo "Hack archive not found: ${hack_zip}" >&2
  exit 1
fi

mkdir -p "${target_dir}"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

unzip -q "${luciole_zip}" -d "${tmp_dir}/luciole"
unzip -q "${hack_zip}" -d "${tmp_dir}/hack"

cp "${tmp_dir}/luciole/Luciole_webfonts/Luciole-Regular/Luciole-Regular.ttf" "${target_dir}/Luciole-Regular.ttf"
cp "${tmp_dir}/luciole/Luciole_webfonts/Luciole-Bold/Luciole-Bold.ttf" "${target_dir}/Luciole-Bold.ttf"
cp "${tmp_dir}/hack/ttf/Hack-Regular.ttf" "${target_dir}/Hack-Regular.ttf"
cp "${tmp_dir}/hack/ttf/Hack-Bold.ttf" "${target_dir}/Hack-Bold.ttf"

echo "Fonts imported into ${target_dir}"
