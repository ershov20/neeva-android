#!/bin/sh

arch=${1:-arm}
scripts_dir=$(dirname $0)

$scripts_dir/install-weblayer-support-apk.sh $arch || exit 1
(cd $scripts_dir/.. && ./gradlew :app:connectedDebugAndroidTest --info || exit 1)
