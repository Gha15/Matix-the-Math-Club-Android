# Matix the Math Club — Android

The Matix club workspace, wrapped in a native Android shell. The entire app is a
single self-contained file, **`app.html`**, which lives at the **root of this
project** and is served to a `WebView`.

---

## Project layout

```
Matix the Math Club Android/
├── app.html                  ← THE APP. Single source of truth. Edit this.
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew  /  gradlew.bat    ← Gradle wrapper scripts
├── local.properties.example   ← copy to local.properties, add your SDK path
├── gradle/wrapper/
└── app/
    ├── build.gradle.kts       ← contains the `syncWebApp` task
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/app.html    ← GENERATED copy, git-ignored. Do not edit.
        ├── java/club/matix/mathclub/MainActivity.kt
        └── res/
```

### How `app.html` reaches the app

1. You edit **`/app.html`** at the project root.
2. The `syncWebApp` Gradle task copies it to `app/src/main/assets/app.html`.
   It is wired to `preBuild`, so it runs automatically on every build.
3. `WebViewAssetLoader` serves that folder, and `MainActivity` loads
   `https://appassets.androidplatform.net/assets/app.html`.

You never edit two copies. The copy under `assets/` is generated and ignored by
git.

---

## Building

```bash
cp local.properties.example local.properties   # then set sdk.dir
./gradlew assembleDebug                        # APK in app/build/outputs/apk/debug/
./gradlew installDebug                         # build + install on a device
./gradlew clean
```

Requires **JDK 17+** and the Android SDK (compileSdk 34).

---

## Startup flow

Modelled on [labs.google](https://labs.google/) — the page loads first, and only
then does it ask who you are.

```
1. Loading screen   #mxBoot     animated wordmark + progress bar
2. Welcome screen   #gateWelcome  what Matix is, feature cards, one CTA
3. Sign in          #gateAuth     username + password
4. App              #app
```

If you are already signed in, steps 2 and 3 are skipped: the loading screen goes
straight into the app, and the session is validated in the background so a
"sign out of all devices" from another phone still kicks you out.

---

## Owner-managed content

The loading and welcome screens are **not hard-coded** — the owner writes them
from inside the app, and every member sees the result.

**Where:** sidebar → **✎ Welcome screen** (owner only), or the ✎ button on the
welcome screen itself.

**Editable:** wordmark, loading tagline, loading label, eyebrow chip, heading,
intro paragraph, button text, badge text, and up to six feature cards
(icon + title + body).

**Stored at:** `/siteContent/gate` in Firebase. If nothing has been saved yet,
the screens fall back to sensible defaults, so the app never renders blank.

Owners are defined by the `OWNERS` array in `app.html` plus any user whose
`/roles/<username>` value is `owner`.

---

## Notes

- `local.properties` is machine specific and is **not** committed.
- Build outputs (`*.apk`, `*.aab`, `app/release/`) are **not** committed.
- The native window background tracks light/dark so there is no white flash
  before the loading screen paints.
- `MatixNative.notify(title, body)` is exposed to the web app for local
  notifications.
