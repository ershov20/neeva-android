#!/bin/sh

sdk_dir="$HOME/Library/Android/sdk"

if [ ! -d $sdk_dir ]; then
    echo "ERROR: Android SDK not found at: $sdk_dir"
    exit 1
fi

build_tools_dir="$sdk_dir/build-tools"
if [ ! -d $build_tools_dir ]; then
    echo "ERROR: build-tools not found at: $build_tools_dir"
    exit 1
fi

# The default lexicographical sort order works well enough here.
version=$(ls -1 $build_tools_dir | tail -1)
echo $build_tools_dir/$version
