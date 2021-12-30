Neeva for Android
=================

# Useful links

While the repo solidifies, please look look at the following links:

* [Ramping up on Android](https://paper.dropbox.com/doc/How-to-Setting-up-an-Android-dev-environment--BYJBnfCPxBpUgE75mLGunuctAg-f3fGXdiAUv3QJSIYiMu83)
* [Building WebLayerSupport.apk](https://paper.dropbox.com/doc/Building-WebLayer-for-Android--BY7mVRhdJHnxtRItajJMVaYRAg-SLtR2sjydyPDmUbIo1hO8)
* [CircleCI dashboard (for test results)](https://app.circleci.com/pipelines/github/neevaco/neeva-android?filter=all)
* [Play Store console](https://play.google.com/console/u/0/developers/6544928132232754928/app-list)

Before opening Android Studio, run:
```
$ ./bootstrap.sh
```
Or do "File > Sync Project with Gradle Files" within Android Studio after running the above command.

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
