#!/bin/sh

if [ -z ${NEEVA_REPO} ]; then
  echo 'Set $NEEVA_REPO to point to your neeva main repository'
  exit 1
fi

export BROWSER_PLATFORM=android
export CLIENT_APP_TYPE=browser
export BUNDLE_DIR=build/release
export BUNDLE_NAME=neeva.aab

. $NEEVA_REPO/client/browser/scripts/build-android-release.sh $@
