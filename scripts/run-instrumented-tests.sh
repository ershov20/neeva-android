#!/bin/sh

arch=${1:-arm}
project=${2:-app}
scripts_dir=$(dirname $0)

$scripts_dir/install-weblayer-support-apk.sh $arch || exit 1
(cd $scripts_dir/.. && ./gradlew :$project:connectedDebugAndroidTest --info || exit 1)
