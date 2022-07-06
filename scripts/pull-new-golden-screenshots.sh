#!/bin/bash

# Get into root mode so that we can access the emulator's data.
adb root
sleep 5

# Run all the screenshot tests locally.
adb logcat -c
./gradlew :screenshotTests:connectedDebugAndroidTest


# Save all the screenshots produced by the tests.
rm -rf /tmp/cache_screenshots
mkdir /tmp/cache_screenshots
pushd /tmp/cache_screenshots
adb shell "ls /data/data/com.neeva.app.debug/cache/*.png" | xargs -n1 adb pull
popd

# Copy them into the golden folder and add them to the current commit.
FAILED_TESTS=$(adb logcat -d | grep "TestRunner: failed" | sed -e "s/^.*failed: //g" -e "s/(.*$//g")
echo $FAILED_TESTS | xargs sh -c 'for arg do cp /tmp/cache_screenshots/*$arg* screenshotTests/src/main/assets/golden/; done' _
git add screenshotTests/src/main/assets/golden/*
