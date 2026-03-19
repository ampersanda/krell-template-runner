#!/usr/bin/env bash
set -euo pipefail

info()  { printf '\033[1;34m=>\033[0m %s\n' "$1"; }
ok()    { printf '\033[1;32m=>\033[0m %s\n' "$1"; }
warn()  { printf '\033[1;33m=>\033[0m %s\n' "$1"; }
fail()  { printf '\033[1;31m=>\033[0m %s\n' "$1"; exit 1; }

rm -rf krell-template-runner
git clone https://github.com/ampersanda/krell-template-runner
cd krell-template-runner

if ! command -v bb &>/dev/null; then
  info "Babashka is not installed. Installing babashka..."
  bash <(curl -s https://raw.githubusercontent.com/borkdude/babashka/master/install)
  ok "Babashka installed."
fi

all_args=("$@")
first_args=$1
rest_args=("${all_args[@]:1}")

bb runner.clj "${first_args}" "${rest_args[@]}"

if [[ $# -eq 0 ]]; then
  cd ..
  rm -rf krell-template-runner
  exit 0
fi

mv "$1" ..
cd ..
rm -rf krell-template-runner
