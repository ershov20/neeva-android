#!/bin/sh

root_dir="$(dirname $0)/.."

pushd $root_dir > /dev/null

./gradlew :app:assembleRelease :weblayer_support:assembleRelease task || exit 1

./scripts/make-bundle.sh release

# TODO(darin): Add signature to neeva.aab
