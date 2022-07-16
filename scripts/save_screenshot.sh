#!/bin/bash
SCREENSHOT_NAME=$(date +"%Y%m%d-%H%M%S")
adb exec-out screencap -p > ~/Desktop/$SCREENSHOT_NAME.png
echo "Screenshot saved to ~/Desktop/"$SCREENSHOT_NAME".png"

