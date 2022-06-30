#!/bin/bash

TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
OUTPUT_DIRECTORY=/tmp/$TIMESTAMP

mkdir $OUTPUT_DIRECTORY
tar --directory $OUTPUT_DIRECTORY -xvzpf "$1"
pushd $OUTPUT_DIRECTORY/app/build/outputs/
grep "TestRunner: " -R
popd
