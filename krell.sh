#!/bin/bash

rm -rf krell-template-runner
git clone https://github.com/ampersanda/krell-template-runner
cd krell-template-runner
bb runner.clj $1
mv $1 ..
rm -rf krell-template-runner
