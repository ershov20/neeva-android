Neeva for Android
=================

# Useful links

While the repo solidifies, please look look at the following links:

## Documentation
* [Ramping up on Android](https://paper.dropbox.com/doc/How-to-Setting-up-an-Android-dev-environment--BYJBnfCPxBpUgE75mLGunuctAg-f3fGXdiAUv3QJSIYiMu83)
* [Android best practices](https://paper.dropbox.com/doc/Pointers-and-best-practices--BZHtc5YBE~oSM6wSnXKkcXf_Ag-E7AvLtTjLLV3qvMljkuDc)
* [Building WebLayerSupport.apk](https://paper.dropbox.com/doc/Building-WebLayer-for-Android--BY7mVRhdJHnxtRItajJMVaYRAg-SLtR2sjydyPDmUbIo1hO8)
* [Uploading a signed release to the Play Store](https://paper.dropbox.com/doc/Uploading-a-signed-release-to-the-Play-Store--BZGrvGNK9AeFPeY50wcwYd9yAg-BUfGIWJtD2f646cik6q3n)

## Dashboards:
* [CircleCI dashboard (for test results)](https://app.circleci.com/pipelines/github/neevaco/neeva-android?filter=all)
* [Play Store console](https://play.google.com/console/u/0/developers/6544928132232754928/app-list)

# Repository setup

When you first checkout the repository, or when a new version of Chromium has been checked in, you must run this command before opening Android Studio:
Before opening Android Studio, run:
```
$ ./bootstrap.sh
```
Or do "File > Sync Project with Gradle Files" within Android Studio after running the above command.

# Repository structure

## General info

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

### Interacting with the backend
When the client wants to make a query or make a mutation, we use Apollo to fire a GraphQL query.
`SuggesstionsViewModel` is worth looking at to see an example of this.

## Directory structure
The directory structure is still in flux as we untangle and update how our browser works.  A few notable directories include:

* `app/` is where our browser code lives.

    * `main/graphql` contains the definitions of the GraphQL calls we use to communicate with the Backend.

    * `browsing` contains the bulk of the code we use to interface with WebLayer, which allows us to hook in with various callbacks.  `Browser` maintains a set of `Tab` instances that represent a single browser tab.  We maintain a list of `Tab`s ourselves so that we can keep track of tab ordering and other info required to display the tab (like favicons).

    * `card` contains logic for our tab switcher.  Tabs are displayed in a grid fashion.

    * `history` contains ViewModels that interact with our databases for recording visit history.

    * `neeva_menu` contains logic for showing our app's main menu, which you trigger by clicking on the Neeva logo in the bottom bar.

    * `settings` contains logic for displaying Settings.

    * `storage` contains logic for maintaining and accessing our database.

    * `suggestions` contains logic for asking the backend for results based on what the user has typed into the URL bar.

    * `urlbar` contains logic for implementing the URL bar at the top of the screen, including the autocomplete logic.

* `chromium/` and `weblayer/` are directories with source and pre-compiled libs we include so that we can use WebLayer (until they release it publicly).

# Updating GraphQL files and `schema.json`

You can get the build an updated `schema.json` file from the monorepo:
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
