#!/bin/sh

# Can also specify "debug" on the command line.
mode=${1:-"release"}

scripts_dir=$(dirname $0)
root_dir="$scripts_dir/.."

out_dir="$root_dir/build/$mode"

$scripts_dir/bundletool.sh install-apks --apks=$out_dir/neeva.apks --adb=$HOME/Library/Android/sdk/platform-tools/adb
