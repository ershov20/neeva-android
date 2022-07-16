#!/bin/bash
EMULATOR_RESOLUTION=$(adb shell wm size | grep -oE '[^ ]+$')
EMULATOR_WIDTH=$(echo $EMULATOR_RESOLUTION | sed 's/x.*$//g')
EMULATOR_HEIGHT=$(echo $EMULATOR_RESOLUTION | sed 's/^.*x//g')

VIDEO_NAME=$(date +"%Y%m%d-%H%M%S")
VIDEO_WIDTH=$(echo $EMULATOR_WIDTH/4 | bc)
VIDEO_HEIGHT=$(echo $EMULATOR_HEIGHT/4 | bc)

echo ----------------------------------------------------------------
echo Saving video to /sdcard/$VIDEO_NAME.mp4
echo To finish recording:
echo 1. Hit: Ctrl+C
echo "2. Run: adb pull /sdcard/$VIDEO_NAME.mp4 ~/Desktop; adb shell rm /sdcard/$VIDEO_NAME.mp4"
echo ----------------------------------------------------------------
echo
adb shell screenrecord --verbose --size ${VIDEO_WIDTH}x${VIDEO_HEIGHT} /sdcard/$VIDEO_NAME.mp4
