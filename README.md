# Matix the Math Club - Android (Kotlin)

This is an EXACT 1:1 copy of the club web app. The real `app.html` is bundled
inside the APK and runs in a native Kotlin shell, so every page, feature and
pixel matches the original: login, workspace, AI, learn, games hub, chat,
ideas, points, notifications, roles, video calls, all of it.

## Open it

1. Android Studio - File > Open - choose this folder.
2. First sync downloads Gradle 8.7 automatically (the wrapper JAR is not
   bundled). Command line alternative: `gradle wrapper --gradle-version 8.7`
   then `./gradlew assembleDebug`.
3. Run on a device or emulator with Android 8.0 (API 26) or newer.

## How it works

- `MainActivity.kt` hosts a full-screen WebView and serves the bundled
  `app/src/main/assets/app.html` from a proper https app origin, so
  localStorage and sign-ins persist between launches.
- Camera and microphone requests from the page (video calls) are forwarded to
  Android runtime permissions.
- File pickers (uploads) open the native chooser.
- External links open in the browser; the club app itself stays in-app.
- The app talks to your Firebase Realtime Database directly, exactly like the
  website. Internet is required, same as the website.

## Updating the app when the HTML changes

Replace `app/src/main/assets/app.html` with the new file and rebuild. Nothing
else to change.
## download:
you can download this [here](https://bzl2ejd7c5wn5h9j.public.blob.vercel-storage.com/matix-android.apk)
