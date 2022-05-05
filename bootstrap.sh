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

chromium_version=$(cat chromium/CURRENT_VERSION)
if [ "x$1" = "x--for-ci" ]; then
    release="release-$chromium_version-x64"
else
    release=${1:-"release-$chromium_version"}
fi
echo "Using chromium/$release"
(cd chromium && ./make-projects.sh "$release")
(cd weblayer && ./make-src.sh "../chromium/$release")
