#!/bin/bash

if [[ $(sysctl -n machdep.cpu.brand_string) == *"Apple M1"* ]]; then
    echo Creating ARM emulator
    ARCH="arm64-v8a"
else
    echo Creating X64 emulator
    ARCH="x86_64"
fi

sdkmanager "system-images;android-28;default;$ARCH"
echo "no" | avdmanager --verbose create avd -n "CircleCI" -k "system-images;android-28;default;$ARCH" -d "pixel_2"
emulator -avd "CircleCI" -no-audio -no-boot-anim -verbose -no-snapshot -gpu swiftshader_indirect -partition-size 2048
