#!/bin/sh

arch=${1:-arm}
root_dir="$(dirname $0)/.."

if [ -d "$HOME/Library/Android/sdk/platform-tools" ]; then
    platform_tools_dir="$HOME/Library/Android/sdk/platform-tools"
elif [ -d "/opt/android/sdk/platform-tools" ]; then
    platform_tools_dir="/opt/android/sdk/platform-tools"
else
    echo "ERROR: Android SDK platform-tools not found"
    exit 1
fi

snapshots_dir="$root_dir/weblayer_support/snapshots"
version=$(cat $snapshots_dir/CURRENT_VERSION)

echo "Installing WebLayerSupport.apk..."
exec $platform_tools_dir/adb install -r $snapshots_dir/$version-$arch/WebLayerSupport.apk
