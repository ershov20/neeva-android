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

(cd chromium && ./make-projects.sh)
