#!/bin/bash
adb root
sleep 5

rm -rf /tmp/cache_screenshots
adb pull "/data/data/com.neeva.app.debug/cache/*.png" /tmp/cache_screenshots
mv /tmp/cache_screenshots/*.png screenshotTests/src/main/assets/golden/
git add screenshotTests/src/main/assets/golden/*
