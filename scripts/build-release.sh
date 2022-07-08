#!/bin/sh

root_dir="$(dirname $0)/.."

pushd $root_dir > /dev/null

clean_build=1
while [ $# -gt 0 ]; do
    if [ $1 = "--no-clean" ]; then
        echo "Not cleaning build"
        clean_build=0
    fi
    if [ $1 = "--help" ]; then
        echo "Usage: $(basename $0) [options]"
        echo ""
        echo "  --no-clean    Don't do a clean build. Useful for debugging subsequent steps."
        echo "  --help        Print this message."
        exit 0
    fi
    shift
done

if [ $clean_build = 1 ]; then
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
else
    echo "Warning: NEEVA_KEYSTORE_PATH not set in the environment; neeva.aab unsigned."
fi
