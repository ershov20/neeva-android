#!/bin/bash
adb root
sleep 5

rm -rf /tmp/cache_screenshots
adb pull "/data/data/com.neeva.app.debug/cache/" /tmp/cache_screenshots
mv /tmp/cache_screenshots/*png app/src/androidTest/assets/golden/
git add app/src/androidTest/assets/golden/*
