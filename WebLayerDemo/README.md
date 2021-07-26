# WebLayerDemo

This directory contains a demonstration of using the [WebLayer API](https://source.chromium.org/chromium/chromium/src/+/main:weblayer/README.md).

Two modules:
* `app/` contains the demo app, which is just a copy of
  `WebLayerShellActivity.java` from the Chromium repository.
* `weblayer/` contains the client library for WebLayer. The client library uses a
  set of AIDL interfaces to talk to the WebLayer implementation.

An implementation of the WebLayer API is also required to run the demo app on a
device or simulator. That is provided by the WebView APK, but unfortunately
access is currently restricted by Google to a fixed set of apps. One day that
may change if Google decides to make WebLayer generally available. For now, we
have to ship a copy of it ourselves.

Fortunately, the Chromium project already provides a package containing just
the implementation of the WebLayer API. This is the WebLayerSupport APK, which
we have already built and can be downloaded from
[here](https://drive.google.com/drive/u/0/folders/1sBqLJ2euWXeekC34C10MIGpE79CtKvGb).

To run the demo app, first install `WebLayerSupport.apk`. You can do that using
`adb` like so:
`$ adb -d install -r -d WebLayerShell.apk`
