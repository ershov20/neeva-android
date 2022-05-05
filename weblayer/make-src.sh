#!/bin/sh

# Run this script to create or update the `src` directory. Existing contents of
# that directory will be deleted.
#
# This takes the output from a release in the chromium/ directory and uses that
# to build the client library.
#
# For example:
#   $ ./make-src.sh ../chromium/release-99
#

if [ -z "$1" ]; then
    echo "Usage: $(basename $0) path-to-release"
    exit 1
fi

release_dir="$1"
version=$(basename $release_dir | cut -d'-' -f2-)

src_dir="src"

rm -fr $src_dir
mkdir -p "$src_dir/main/java"
mkdir -p "$src_dir/main/aidl"

cat "$release_dir/AndroidManifest.xml" | \
    sed 's/package=.*/package="org.chromium.weblayer">/' > \
        "$src_dir/main/AndroidManifest.xml"

unzip "$release_dir/client-java.zip" -d "$src_dir/main/java"
unzip "$release_dir/client-aidl.zip" -d "$src_dir/main/aidl"
unzip "$release_dir/client-res.zip" -d "$src_dir/main"
