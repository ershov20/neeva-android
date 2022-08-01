#!/bin/sh

CLEAN_BUILD=1
while getopts "nh" option; do
  case $option in
    n) # no clean option
       echo "Not cleaning build"
       CLEAN_BUILD=0
       ;;
    h) # help option
       echo "Usage: $(basename $0) [options]"
       echo ""
       echo "  -h    Print this message."
       echo "  -n    Don't do a clean build. Useful for debugging subsequent steps."
       exit 0
  esac
done

root_dir="$(dirname $0)/.."

pushd $root_dir > /dev/null

if [ $CLEAN_BUILD = 1 ]; then
    ./gradlew clean task || exit 1
fi

./gradlew :app:assembleStaging :weblayer_support:assembleStaging task || exit 1

./scripts/make-bundle.sh staging
