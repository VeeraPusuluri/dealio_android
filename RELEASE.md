# Shipping Dealio to the Play Store

What the repo does for you, and what still needs a human decision. Nothing here
is wired to run automatically — a release is a deliberate act.

## 1. Signing

The release build type reads its key from `keystore.properties` at the repo
root. That file is gitignored, and so is any `.jks`/`.keystore` — the key must
never be committed.

```sh
cp keystore.properties.example keystore.properties

keytool -genkeypair -v -keystore dealio-upload.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias dealio-upload
```

Fill in `storePassword` and `keyPassword`, then confirm the wiring took:

```sh
./gradlew :app:bundleRelease
```

Without `keystore.properties` the build still succeeds and simply produces an
**unsigned** bundle, so nobody needs the key to compile the project. That also
means an unsigned artifact is not an error you will be warned about — check
before uploading:

```sh
jarsigner -verify -verbose app/build/outputs/bundle/release/app-release.aab
```

Back the `.jks` up somewhere durable. With Play App Signing enabled (recommended,
and the default for new apps) Google can reset a lost upload key; without it, a
lost key means the listing can never be updated again.

## 2. Backend URL — **not yet set**

`dealio.apiBaseUrl` currently defaults to the CloudFront host, which is the
**dev** stack. That stack stops nightly at midnight IST, so a public release
pointed at it would fail for every user overnight.

Set the production host before uploading anything to a public track:

```sh
./gradlew :app:bundleRelease -Pdealio.apiBaseUrl=https://api.example.com/api/
```

or put `dealio.apiBaseUrl=…` in `gradle.properties`. The trailing `/api/` matters.

## 3. Version

`app/build.gradle.kts` — `versionCode = 1`, `versionName = "1.0"`. Play rejects
a re-upload that does not raise `versionCode`, so bump it on every upload, even
a re-upload of the same code.

## 4. Before the first upload

- **Privacy policy URL** — required by Play, and doubly so here: the app reads
  contacts and phone numbers. It has to be a live public URL.
- **Data safety form** — declare phone number (auth), contacts (`READ_CONTACTS`,
  used by the CP "Import from phone" flow), and any analytics collected by
  Firebase. This is a declaration about real behaviour; check it against the
  code rather than filling it in from memory.
- **Store assets** — 512×512 icon, feature graphic, screenshots per form factor.
  The in-app launcher icon is an adaptive vector and needs no PNG densities
  (minSdk is 26), but the store icon is a separate upload.
- **Account deletion** — Play requires an in-app path *and* a web URL for
  deleting an account when an app has sign-in. Verify one exists before
  submitting; this is a common first-review rejection.
- **Test track first.** Push to internal testing, install from Play on a real
  device, and sign in on each of the three portals.

## 5. Deliberately not done

- **R8/minification is off.** `isMinifyEnabled = false`. The Gson models are
  kept by `proguard-rules.pro`, but the app has never been run end-to-end
  minified, and reflection-driven JSON is exactly what R8 breaks quietly.
  Turning it on is a change to make and then test, not to slip into a release.
- **No CI release job.** Uploading is manual and intentionally so.
