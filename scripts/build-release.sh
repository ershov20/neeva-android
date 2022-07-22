#!/bin/sh

if [ -z ${NEEVA_REPO} ]; then
  echo 'Set $NEEVA_REPO to point to your neeva main repository'
  exit 1
fi

export CREATE_BRANCH=false
export SKIP_PROMPT=true
export BROWSER_PLATFORM=android

ANDROID_SCRIPTS_DIR=$(dirname $0)
SHARED_SCRIPTS_DIR=$NEEVA_REPO/client/browser/scripts/

. $SHARED_SCRIPTS_DIR/git-util.sh
. $SHARED_SCRIPTS_DIR/version-util.sh

CLEAN_BUILD=1
while getopts "bpnh" option; do
  case $option in
    b) # create branch
       CREATE_BRANCH=true
       ;;
    p) # prompt on every build step
       SKIP_PROMPT=false
       ;;
    n) # no clean option
       echo "Not cleaning build"
       CLEAN_BUILD=0
       ;;
    h) # help option
       echo "Usage: $(basename $0) [options]"
       echo ""
       echo "  -b    Create branch cut."
       echo "  -h    Print this message."
       echo "  -n    Don't do a clean build. Useful for debugging subsequent steps."
       echo "  -p    More prompts for each build step."
       exit 0
  esac
done

root_dir="$(dirname $0)/.."

pushd $root_dir > /dev/null

if [ $CLEAN_BUILD = 1 ]; then
    ./gradlew clean task || exit 1
fi

./gradlew :app:assembleRelease :weblayer_support:assembleRelease task || exit 1

./scripts/make-bundle.sh release

if [ -n "$NEEVA_KEYSTORE_PATH" ]; then
    if [ -n "$NEEVA_KEYSTORE_PASS" ]; then
        storepass_arg="-storepass $NEEVA_KEYSTORE_PASS"
    else
        echo "Optionally set NEEVA_KEYSTORE_PASS in the environment to automate."
    fi
    echo "Signing release with key0 from $NEEVA_KEYSTORE_PATH"
    jarsigner -keystore "$NEEVA_KEYSTORE_PATH" $storepass_arg build/release/neeva.aab key0

    # open the build directory in Finder
    open build/release
else
    echo "Warning: NEEVA_KEYSTORE_PATH not set in the environment; neeva.aab unsigned."
fi

# Confirm uploading build to app store
$SHARED_SCRIPTS_DIR/confirm-upload-binary.sh

# Generate tag for build
$SHARED_SCRIPTS_DIR/tag-release.sh

if $CREATE_BRANCH; then
  $SHARED_SCRIPTS_DIR/branch-release.sh
fi

read -r -p "Bump up the version for next build? [Y/n] " response
if [[ "$response" =~ ^([nN][oO]?)$ ]]
then
  continue
else
  if $CREATE_BRANCH; then
    $SHARED_SCRIPTS_DIR/prepare-for-next-release.sh
    # switch back to main for preparing next version
    git checkout main
  fi
  $SHARED_SCRIPTS_DIR/prepare-for-next-release.sh
fi

