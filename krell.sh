#!/bin/bash

rm -rf krell-template-runner
git clone https://github.com/ampersanda/krell-template-runner
cd krell-template-runner

if ! type bb >/dev/null; then
  echo "👉 Babashka is not installed. Installing babashka..."
  bash <(curl -s https://raw.githubusercontent.com/borkdude/babashka/master/install)
  echo "👉 Babashka installed."
fi

all_args=("$@")
first_arg=$1
rest_args=("${all_args[@]:1}")

bb runner.clj "${first_arg}" "${rest_args[@]}"

if [[ $# -eq 0 ]]; then
  cd ..
  rm -rf krell-template-runner
  exit 0
fi

mv ${first_arg} ..
cd ..
rm -rf krell-template-runner
