This directory contains a copy of a pre-built WebLayerSupport.apk and scripts to
convert that into a set of gradle projects that can be used to construct a
single APK build of the Neeva app using the pre-built WebLayer implementation.

Run `./make_projects.sh` from this directory to populate the `gen/` directory.

Optionally pass a parameter to specify an alternative Chromium build to use as
input to the script, e.g.: `./make_projects.sh some/path`
