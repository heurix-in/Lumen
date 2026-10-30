# Lumen — private SMS app for Android

Lumen is a full replacement for your phone's Messages app, for normal SIM text messages (SMS). It has a glass-and-aurora design with fluid motion, and it has **no internet permission**, so your messages never leave the phone.

## Features

- **Conversations**: a glass card list with contact photos or initials and unread badges. Tap Search to search people and the text of every message. The All / Unread tabs switch with a sliding indicator.
- **Chat**: grouped bubbles. Sent bubbles are colored by a gradient fixed to the screen, so their color shifts as you scroll. New messages spring into view. Status icons animate through *sending → sent → delivered*.
- **Smart bits**:
  - One-time codes get a **Copy code** chip.
  - Links are tappable.
  - A character and SMS-count counter appears on long texts.
  - You can retry a failed send.
  - Each conversation keeps its own draft.
- **Dual SIM**: a SIM chip in the composer lets you pick which SIM to send from. Each conversation remembers the SIM it last used.
- **Notifications**: you can reply or mark messages as read directly from the notification.
- **Long-press**: on a conversation, it enters select mode, where you can mark as read or delete. On a message, it lets you copy, delete or retry.
- **Shared-element transition**: the contact avatar flies from the list into the chat header.
- **Real backdrop blur** on Android 12 and newer. Older phones get a frosted tint instead.
- **Light and dark** themes follow your phone's setting.
- **Reduced motion**: the background stops moving if you turn animations off in Developer options.

## Build the APK, option A: GitHub (nothing to install)

1. Sign in at github.com (a free account is fine) and create a **new private repository**, for example `Lumen`.
2. On your computer, unzip `Lumen.zip`.
3. In the new repository, click **"uploading an existing file"**. Drag in **everything inside** the `Lumen` folder, including the `.github` folder, then click **Commit changes**.
   - On a Mac, Finder hides folders that start with a dot. Press **Cmd + Shift + .** in Finder to show `.github`.
4. Open the **Actions** tab. A build called *Build Lumen APK* starts automatically and takes about 5 minutes.
5. When it shows a green tick, open it. Under **Artifacts**, download **Lumen-apk**. It's a zip that contains `Lumen.apk`.
6. Send `Lumen.apk` to your phone, open it, and allow "Install unknown apps" when asked.

If the `.github` folder didn't upload, the Actions tab will be empty. To fix it, go to **Actions → set up a workflow yourself**. Paste in the contents of `.github/workflows/build.yml` and commit. The build will then start.

## Build the APK, option B: Android Studio

1. Open Android Studio, choose **Open**, and select the `Lumen` folder. Wait for the sync to finish.
2. In Android Studio's **Terminal** tab, run:
   `./gradlew assembleRelease` (on Windows: `gradlew assembleRelease`)
3. The APK is created at `app/build/outputs/apk/release/app-release.apk`.

Use the **release** build. It is optimized and much smoother than a debug build.

## First launch

1. Open Lumen and tap **Set default**. Android asks you to make Lumen your SMS app.
2. Tap **Allow** to grant contacts and notifications.
3. Tap **Open Lumen**.

To switch back at any time, go to **Settings → Apps → Default apps → SMS app** and choose your old app. Your messages are stored by Android itself, so they stay put when you switch apps.

## Privacy

- The app's manifest *removes* the `INTERNET` permission. Without it, Android blocks the app from any network access.
- The GitHub build prints the APK's full permission list and **fails on purpose if INTERNET ever appears**.
- To check on your phone, go to **Settings → Apps → Lumen → Permissions**. You'll see SMS, Contacts, Phone and Notifications, and nothing network-related.
- App backup is turned off, so your messages aren't copied to cloud backups through this app.

## Current limitations

- **Picture and group messages (MMS)** can't be opened yet. When one arrives, Lumen shows a notification so you know. To view it, temporarily switch back to your phone's Messages app.
- **RCS / "Chat" features** (typing dots, Wi-Fi messaging) are not available to any third-party app. When you use Lumen, the people you text will receive normal SMS.
- **Signing key**: the key that signs the app lives in `app/lumen-release.jks`. This lets each new build install as an update. Keep the repository **private**.

## Project map

```
app/src/main/java/com/lumen/messages/
  data/     SMS database access, contacts, models
  sms/      receive / send / delivery reports / quick-reply service
  notify/   notifications with reply actions
  ui/glass  backdrop blur + aurora background
  ui/screens  onboarding, conversation list, chat, new message
```
