Chromium Integration
====================


# Overview

This directory contains a copy of a pre-built WebLayerSupport.apk (plus other
build artifacts from Chromium) and scripts to convert that into a set of gradle
projects that can be used to construct a single APK build of the Neeva app using
the pre-built WebLayer implementation.

Run `./make_projects.sh` from this directory to populate the `gen/` directory.

Optionally pass a parameter to specify an alternative Chromium build to use as
input to the script, e.g.: `./make_projects.sh some/path`


# Targets

There are Arm and x64 variants of Chromium snapshots. The x64 variants have the
`-x64` suffix, while Arm variants have no suffix.


# `CURRENT_VERSION` file

This file indicates the current version of Chromium being used and is referenced
by scripts / used by CI. This file indicates which Chromium snapshot should be
treated as the default.
