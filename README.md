# AlkisuDataHub Android App

A native WebView-wrapper Android app for alkisudatahub.com.ng, built from scratch
(no third-party wrapper tool like WebIntoApp). This fixes the earlier PDF/download
issue and gives full control over permissions, downloads, and future notification
support.

## What's included
- **Your real logo** is now the app icon (adaptive icon + legacy icon, all densities)
  and appears on the splash screen — generated from the logo you sent.
- **Splash screen with fingerprint lock**: on open, your logo animates in
  (fades and slides up), then the phone's real fingerprint prompt appears.
  On success, the app continues into the site. If a phone has no fingerprint
  hardware or none enrolled, it skips the lock and continues straight in —
  it never traps someone who can't use fingerprint.
- **Downloads fixed**: PDF slips/receipts now go through Android's DownloadManager
  with a proper notification, instead of failing silently in the WebView.
- **File uploads work**: `<input type="file">` fields (BVN modification documents,
  NIN enrollment photos) open the device's file picker / camera.
- **External links open properly**: anything not on `alkisudatahub.com.ng` (e.g. a
  payment gateway redirect, WhatsApp link, `tel:`/`mailto:` links) opens in the
  system browser/app instead of breaking inside the WebView.
- **Pull-to-refresh**, a slim gold progress bar, and back-button navigation that
  respects WebView history.
- App theme uses your site palette (navy `#0A192F` / gold `#F3C93E`).

## Before you build
1. Open the project folder in **Android Studio** (Giraffe or newer), or use
   the Codespaces / GitHub Actions route below — Gradle syncs automatically.
2. If you want a different package/application ID than
   `com.alkisudatahub.app`, change it in `app/build.gradle` (`applicationId`) and
   `AndroidManifest.xml`.

## Building this entirely from your phone (no PC needed)
Android Studio itself needs a computer, but you don't need Android Studio to
get an APK — a GitHub Actions workflow is already included in this project
(`.github/workflows/build.yml`) that builds the APK in the cloud. Everything
below can be done from your phone's browser or the GitHub app.

1. **Unzip this file on your phone** — any file manager app (the built-in
   Files app, or one like ZArchiver) can extract a `.zip`.
2. **Create a free GitHub account** at github.com if you don't have one.
3. **Create a new repository** (top right → "New repository"). Name it
   something like `alkisudatahub-app`. Leave it empty (no README).
4. **Upload the project**: open the new repo → **Add file → Upload files**,
   then use "choose your files" and select the whole extracted
   `AlkisuDataHubApp` folder (Chrome's file picker lets you pick a folder and
   keeps the structure). Commit the upload.
5. Go to the **Actions** tab of your repo. You should see "Build APK" running
   automatically (or tap **Run workflow** to start it manually). It takes a
   few minutes.
6. Once it finishes (green check), open that run, scroll to **Artifacts**,
   and download `AlkisuDataHub-debug-apk` — that's a zip containing your
   `app-debug.apk`. Unzip it and install it directly on your phone (you may
   need to allow "install unknown apps" for your browser/file manager once).

This gives you a debug build, which is perfect for testing on your own phone.
For a Play Store release, you'd eventually need a signed release build (see
the section above) — that step is easier from a PC, but I can also walk you
through doing it with a cloud CI signing step if you want to stay phone-only.

## Getting a signed release build — 100% from your phone
This is for when you're ready to submit to the Play Store (or share a
"final" APK, not just a debug test build). Two more workflows are already
included for this, so generating a signing key, storing it safely, and
producing a signed APK never touches Android Studio or a PC.

**Step 1 — Generate your keystore (do this once, ever)**
1. In your GitHub repo, go to the **Actions** tab.
2. Select **"1. Generate Release Keystore (run this once)"** in the left list.
3. Tap **Run workflow**. Fill in:
   - **alias**: a short name, e.g. `alkisu`
   - **store_password** / **key_password**: pick strong passwords and
     **write them down somewhere safe** — you cannot recover them later, and
     losing this keystore means you can never update your app on the Play
     Store again under the same listing.
   - **full_name** / **org**: your name and Geraldimer ICT (or leave defaults)
4. Run it. When it finishes, open the run → **Artifacts** →
   `release-keystore-KEEP-SAFE`. Download and unzip it. Keep `release.keystore`
   backed up somewhere private (e.g. your own Google Drive) — never commit it
   to the repo.

**Step 2 — Add your keystore as repo secrets**
1. Open `release.keystore.base64.txt` from that download in any text app,
   select all, and copy it.
2. In your repo, go to **Settings → Secrets and variables → Actions** (you
   may need to tap "Desktop site" in your browser menu if the mobile layout
   hides this).
3. Tap **New repository secret** four times, creating:
   - `KEYSTORE_BASE64` → paste the long base64 text
   - `KEYSTORE_PASSWORD` → your store_password from Step 1
   - `KEY_ALIAS` → your alias from Step 1
   - `KEY_PASSWORD` → your key_password from Step 1

**Step 3 — Build the signed APK**
1. Back in **Actions**, select **"2. Build Signed Release APK"**.
2. Tap **Run workflow**.
3. When it finishes, open the run → **Artifacts** → download
   `AlkisuDataHub-release-apk`. That's your signed, Play-Store-ready APK.

From here, submitting to the Play Store is a web form (Google Play Console,
$25 one-time fee) that also works fine from a phone browser — you'd upload
this APK (or I can add a step to build an `.aab` instead, which the Store
prefers) along with screenshots, a description, and your privacy policy
link. Happy to walk through that when you're ready.

## Push notifications (OneSignal) — already wired in
The app now initializes OneSignal on launch and asks for notification
permission the first time it opens. To finish the setup:

1. Go to https://dashboard.onesignal.com/apps/ and create a new app (choose
   **Google Android (FCM)** as the platform). OneSignal will walk you through
   connecting a Firebase project — if you don't have one yet, it's free and
   takes a few minutes on the same screen.
2. Once created, go to **Settings → Keys & IDs** and copy your **OneSignal
   App ID** (a 36-character ID, not the REST API key).
3. Open `app/src/main/java/com/alkisudatahub/app/ApplicationClass.kt` and
   replace `"YOUR_ONESIGNAL_APP_ID"` with that ID.
4. Rebuild and run the app on a real device (or an emulator with Google Play
   Services) — you should see the notification permission prompt, and the
   device will show up under **Audience → Subscriptions** in the dashboard.
5. To actually send a push, use **Messages → New Push** in the dashboard, or
   the API with your **REST API Key** (also on the Keys & IDs page) — keep
   that key secret, it's not something to put in the app itself.

Once it's working, a couple of things worth adding later:
- **Notification icon**: right now pushes use OneSignal's default bell icon.
  Drop a monochrome icon named `ic_stat_onesignal_default` into each
  `res/drawable-*` density folder to use your own branding instead.
- **Linking notifications to a customer's account**: if you want to target
  a specific customer (e.g. "your wallet funding was successful"), call
  `OneSignal.login("their_user_id")` after they log in on your site. That
  needs a small JS bridge between the WebView and the app — ask me if you
  want that built.

## Other next steps you may want
- **Splash screen**: currently loads straight into the WebView; easy to add a
  branded splash screen if you want one.
- **Play Store listing**: you'll need a signed release APK/AAB, a privacy policy
  URL (you already have one), screenshots, and a Google Play Console developer
  account (one-time $25 fee).
