Neeva for Android
=================
The Neeva browser is built on top of [Chromium](https://cs.chromium.org/), utilizing Jetpack Compose as the foundation for its UI.

Available for download on the [Play Store](https://play.google.com/store/apps/details?id=com.neeva.app).

# Working with the repository

## Initial setup
This guide assumes that you are building in a Mac dev environment using Z shell.

### Install Android Studio
We use Beta or Canary versions of Android Studio so that we can use Compose Preview integrations that help with development.  You can get download those from https://developer.android.com/studio/preview.

#### Update Android Studio's settings
You will want to max out the amount of memory Android Studio is allowed to use in the **Memory Settings** section of the Android Studio preferences.  To get there, quickly double-tap the Shift key and type "Memory Settings" into the box, then select it from the dropdown.

#### Install the command line tools, too
In order to run our screenshot tests, you'll need to have the Android Studio command line tools installed. You can get them from Android Studio > Preferences > Appearance & Behavior > System Settings > Android SDK > SDK Tools. Your SDK Tools tab should look like this:

<img width="754" alt="Screen Shot 2022-08-24" src="https://user-images.githubusercontent.com/20916043/186373262-db6b218e-be7f-4a90-825a-0bb736fdc7e3.png">

Once those are installed, you can run a version of this command to add them to your `$PATH` and make sure they're accessible from anywhere:
```
# Assuming that you've installed the tools to the default directories:  
# To set the correct sdkmanager, avdmanager, emulator executables
echo "export ANDROID_HOME=\"$HOME/Library/Android/sdk\"" >> ~/.zshrc
echo "export PATH=\"\$PATH:$ANDROID_HOME/platform-tools\"" >> ~/.zshrc
echo "export PATH=\"\$PATH:$ANDROID_HOME/cmdline-tools/latest/bin\"" >> ~/.zshrc
echo "export PATH=\"\$PATH:$ANDROID_HOME/emulator\"" >> ~/.zshrc

source ~/.zshrc

# verify this is $HOME/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager
which sdkmanager
```

If you don't already have the Java runtime installed, you'll need to install that from: https://www.oracle.com/java/technologies/downloads/

### Setting up emulators
The minimum Android SDK level supported by the Neeva app is **28**.  For good testing coverage, it's a good idea to set up an emulator with API level 28 and another with the most recent API level available to you.

To create new emulated devices, go double-tap the Shift key and type **"Device Manager"**, then hit the **Create device** button on that pane.

Select any device definition (though I recommend selecting one with Play Store support and one with a smaller screen), then download an applicable system image.
Make sure you choose the correct architecture for your dev environment to ensure that the emulator is reasonably performant:
| Dev environment | Emulator architecture |
| --------------- | --------------------- |
| x86 Macbook     | **x86_64**            |
| M1 Macbook      | **arm64**             |

You can also run this script to create and start up the same emulator that our testing infrastructure uses:
```
scripts/create-emulator.sh
```
This is useful for debugging CI problems and required for updating our screenshot tests, but it's more limited than an emulator you set up manually.

### Setting up your shell
```
# Set the default for your pulls to perform rebases instead of merges to make diffing easier.
git config --global pull.rebase true

# Enable git branch autocompletion.
echo "autoload -Uz compinit && compinit" >> ~/.zshrc

# Install Homebrew if you don't already have it (https://brew.sh/)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install git-lfs (https://git-lfs.github.com/), which we use to store pre-compiled versions
# of Chromium outside of our Git repository.
brew install git-lfs
```

## Compiling and running the app
There are some commands that need to be run from the command line to set up your checkout:
```
# Set up the Gradle project so that it can find our pre-built version of Chromium.
# It's a good idea to run this whenever you fetch from the repository.
# For arm64 builds:
./bootstrap.sh

# For x86_64 builds:
./bootstrap.sh --for-ci

# Install a secondary APK that the debug builds need to run.
# This needs to be run whenever we upload a new build of Chromium.
scripts/install-weblayer-support-apk.sh
```

Whenever you fetch updates from the repository, make sure that you re-run `bootstrap.sh`, then trigger **Sync project with gradle files** in Android Studio.

You should now be able to build and run the project.  In the toolbar, you can select the emulator that you set up earlier, then hit the **Run 'app'** button (or select `Menu > Run > Run 'app'`).

## Building a release (or staging) version of the app
Building the `release` version will require you to have the environment variable `NEEVA_REPO` set to the location of your neeva repo checkout.

A release/staging version of the app includes `WebLayerSupport.apk` as a feature split within the Neeva app bundle (`neeva.aab`), so you don't need to install WebLayer separately. 

```
# Ensure that you have the proper environment variables set up:
export NEEVA_REPO="path/to/neeva"

# If you want to push your build to the Play Store, set these variables too:
export NEEVA_KEYSTORE_PATH="path/to/playstore_signing_key"
export NEEVA_KEYSTORE_PASS="playstore signing password"
```

```
# To generate the app bundle `build/release/neeva.aab`, run: 
scripts/build-release.sh

# If you only want to test the release build, kill the script when prompted if "Build uploaded to App/Play Store?"

# If you want to build staging:
scripts/build-staging.sh
```

You can then install the resulting app bundle to your device:
```
# To install the release bundle:
scripts/install-apks.sh release

# To install the staging bundle:
scripts/install-apks.sh staging
```

## Testing
Our project has three different types of tests: **unit tests**, **integration tests**, and **screenshot tests**.  These are run by our continuous integration for every submission to ensure that everything continues working as expected.

Contributors are encouraged to add new tests for any code they write.

### Unit tests
These test small parts of the project in isolation -- generally single classes or functions.  You may run these by:
```
./gradlew :app:testDebugUnitTest
```

#### Debugging unexpected coroutine behavior
If your test is failing, you should check if the `coroutineScope` is still active.  If it isn't, the scope likely crashed due to your changes.  If your `CoroutineScope` is a `TestScopeImpl` under the hood, you can run the debugger and look at its `_state` to get the `_rootCause` to see the crashing stack trace.

### Integration tests
These run on the device directly.  They will often start the whole app up and click on various things in order to check that functionality is working across multiple components (e.g. typing in a URL navigates to a new website and updates the title displayed in the tab switcher).  You may run these via:
```
./gradlew :app:connectedDebugAndroidTest --info
```

#### Mocked out behavior
To set up a more hermetic environment, we try to mock out as many network requests as possible.
* We run a custom web server directly on the device that serves web pages we supply in the `app/src/androidTest/assets/html` directory whenever the app visits `http://127.0.0.1`.  Various app constants are overridden so that we redirect the user to `http://127.0.0.1` when we try to visit `https://www.neeva.com`.  Our repo has several HTML files that are shared by various tests to load web sites, trigger new tabs to be created, and trigger full screen videos, among other things.
* GraphQL requests are mocked out by using the testing functions provided by the Apollo library.  Tests that require data returned by GraphQL queries or mutations can provide responses when specific queries or mutations are fired; check `FeedbackViewTest.kt` for examples.

### Screenshot tests
These run on the device directly and confirm that our UI doesn't change unintentionally.  To run these, you need to use the same emulator setup as CircleCI:
```
# Set up and run the emulator
scripts/create-emulator.sh

# In another terminal, after the emulator has finished running:
scripts/pull-new-golden-screenshots.sh
```

This script runs through all of the tests in the `screenshotTests` module, comparing what the app looks like after a pull request against screenshots we've previously saved in the repo.  Tests will fail if any differences are detected; when this happens the script pulls the new screenshots off of the device and into your checkout.  If the differences are intentional, you may add them to your pull request to update our expectations.

## Useful tools
### Accessing the developer settings
We have several flags hidden in a menu that are useful for development, which include the ability to take screenshots in Incognito or redirect the user to a development version of `neeva.com`.  These can be accessed by double-tapping the Neeva version number on the Settings page.

### Taking screenshots and recording videos
When submitting new pull requests, it can be helpful to add screenshots and videos that show the effect of your PR.
```
# Take a screenshot of the currently connected device.
scripts/save_screenshot.sh

# Record a video of the currently connected device.
# Follow the instructions to save the file.
scripts/save_video.sh
```

If you prefer, you can also do this from Android Studio by clicking the camera icon (for screenshots) or using the **Record and Playback** option from the emulator window's **Extended Controls**.

A more robust tool is [**scrcpy**](https://github.com/Genymobile/scrcpy), which allows you to mirror real devices onto your dev system and control it using your keyboard and mouse.  You can also use it to record videos more easily:
```
# Record a video of you interacting with the device.
scrcpy -m 1024 -r ~/Desktop/$(date +"%Y%m%d-%H%M%S").mp4
```

### Decompiling APKs
[**apktool**](https://ibotpeaches.github.io/Apktool/) allows you to decompile APKs and see how their `AndroidManifest.xml` files are set up (among other things).  This is useful for confirming that flags set in the manifest files are all set correctly between build variants.

## Updating GraphQL files and `schema.json`  (internal only)
If you need to update the Android repository with new GraphQL data, you can get an updated `schema.json` file from the Neeva monorepo:
```
# Sync up the monorepo.
cd /PATH/TO/YOUR/NEEVA/CHECKOUT
git checkout master
git pull --rebase

# Build the new schema.json file.
cd client/packages/neeva-lib
yarn install && yarn build

# Copy it into your neeva-android checkout.
cp gen/graphql/schema.json /PATH/TO/YOUR/NEEVA-ANDROID/CHECKOUT/app/src/main/graphql/com/neeva/app/schema.json
```

Once that's updated, you can update the relevant `*.graphql` files in the your `neeva-android` checkout.



## Repository structure

### Jetpack
We rely heavily on Compose and `@Preview`s to see that our UI behaves under different conditions.
The bulk of our UI is built using `Composable`s, but we still have to use regular `View`s when working with WebLayer.
These Composables are interspersed through the repo in function-specific modules (Settings composables live in `settings`, e.g.).
Users are sent to different screens using the `AppNavModel`, which can be asked to display different screens.

### Flows and coroutines
We're using Flows and coroutines rather than using RxJava.  Try to avoid using `LiveData` if you can, just so we're consistent and not converting back and forth between observables.

### ViewModels
The app has many `ViewModel`s that manage specific state and collect `Flow`s provided by the Room Database and (currently) other `ViewModel`s.
This allows a change in the URL to trigger a network fetch to get updated suggestion queries, which the UI collects.
They are still slightly tangled up, so if you are confused about the right way to trigger a tab navigation (e.g.), feel free to ask.

### Databases
We maintain a Room Database that exposes all of the user's history.  Start in `History.kt` if you want to dive in.

#### Importing and exporting databases
The developer settings have options for importing and exporting, which can be useful for testing the same database across different build variants (e.g. moving your history from your release build to your debug build to examine failures).

### Interacting with the backend
When the client wants to make a query or make a mutation, we use Apollo to fire a GraphQL query.
`SuggesstionsViewModel` is worth looking at to see an example of this.

## Directory structure
The directory structure is still in flux as we untangle and update how our browser works.  A few notable directories include:

* `app/` is where our browser code lives.

  * `main/graphql` contains the definitions of the GraphQL calls we use to communicate with the Backend.

  * `browsing` contains the bulk of the code we use to interface with WebLayer, which allows us to hook in with various callbacks.  `Browser` maintains a set of `Tab` instances that represent a single browser tab.  We maintain a list of `Tab`s ourselves so that we can keep track of tab ordering and other info required to display the tab (like favicons).

  * `cardgrid` contains logic for our tab switcher.  Tabs are displayed in a grid fashion.

  * `history` contains ViewModels that interact with our databases for recording visit history.

  * `neeva_menu` contains logic for showing our app's main menu, which you trigger by clicking on the Neeva logo in the bottom bar.

  * `settings` contains logic for displaying Settings.

  * `storage` contains logic for maintaining and accessing our database.

  * `suggestions` contains logic for asking the backend for results based on what the user has typed into the URL bar.

  * `urlbar` contains logic for implementing the URL bar at the top of the screen, including the autocomplete logic.

* `weblayer/` and `weblayer_support/` are directories with source and pre-compiled libs we include so that we can use WebLayer (until they release it publicly).

# Useful links

## Documentation
* 🔒[Android best practices](https://paper.dropbox.com/doc/Pointers-and-best-practices--BZHtc5YBE~oSM6wSnXKkcXf_Ag-E7AvLtTjLLV3qvMljkuDc)
* 🔒[Building WebLayerSupport.apk](https://paper.dropbox.com/doc/Building-WebLayer-for-Android--BY7mVRhdJHnxtRItajJMVaYRAg-SLtR2sjydyPDmUbIo1hO8)

## Dashboards:
* 🔒[CircleCI dashboard (for test results)](https://app.circleci.com/pipelines/github/neevaco/neeva-android)
* 🔒[Play Store console](https://play.google.com/console/u/0/developers/6544928132232754928/app-list)
