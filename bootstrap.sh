#!/bin/sh

if ! which git-lfs > /dev/null; then
    echo "Oops, git-lfs needs to be installed! Run this command on your Mac:"
    echo
    echo "    \$ brew install git-lfs"
    echo
    exit 1
fi

git lfs install
git lfs pull

snapshots_dir="weblayer_support/snapshots"
chromium_version=$(cat $snapshots_dir/CURRENT_VERSION)
if [ "x$1" = "x--for-ci" ]; then
    release="$chromium_version-x64"
else
    release=${1:-"$chromium_version-arm"}
fi
echo "Using $snapshots_dir/$release"
(cd weblayer && ./make-src.sh "../$snapshots_dir/$release")
