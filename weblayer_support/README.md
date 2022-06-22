Overview
========

This directory contains an empty project representing the `weblayer_support`
feature module.

This project is used to generate an appropriate `AndroidManifest.xml` file for
the `weblayer_support` feature module.

The generated `AndroidManifest.xml` is used in place of the manifest file from
`WebLayerSupport.apk` as part of converting `WebLayerSupport.apk` into a
suitable feature module.

See `scripts/make-bundle.sh` and `scripts/make-archive.sh` for the actual
mechanics of this conversion process.

The `snapshots/` directory contains Chromium build artifacts for Arm and x64
configurations.
