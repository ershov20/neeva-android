#!/bin/sh

tools_dir="$(dirname $0)/../tools"

exec java -jar "$tools_dir/bundletool-all-1.10.0.jar" $*
