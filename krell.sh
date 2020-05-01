#!/bin/bash

rm -rf krell-template-runner
git clone https://github.com/ampersanda/krell-template-runner
cd krell-template-runner

if ! type bb > /dev/null; then
  echo "👉 Babashka is not installed. Installing babashka..."
  bash <(curl -s https://raw.githubusercontent.com/borkdude/babashka/master/install)
  echo "👉 Babashka installed."
fi

bb runner.clj $1
mv $1 ..
cd ..
rm -rf krell-template-runner
