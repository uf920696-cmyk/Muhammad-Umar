# 📱 Running JARVIS on Your Android Phone

Follow these straightforward steps to install and run JARVIS directly on your physical Android smartphone or tablet.

---

## 📥 Step 1: Export the Project from Google AI Studio

1. In the upper-right corner of this screen, click on the **three-dots menu (`...`)** or **Settings**.
2. Select **Export to ZIP** (or **Download as ZIP**).
3. Save the ZIP file to your computer and extract its contents into a folder (e.g., `C:\JARVIS` on Windows or `~/JARVIS` on Mac/Linux).

---

## 🛠️ Step 2: Build the APK File

You can build the `.apk` using either **Android Studio** (recommended if you prefer a graphical interface) or the **Terminal / Command Prompt**.

### Option A: Using Android Studio (Recommended)
1. Download and open [Android Studio](https://developer.android.com/studio).
2. Click **Open** and select the extracted `JARVIS` folder.
3. Wait for Gradle sync to complete (typically 1–2 minutes on first load).
4. In the top menu, click **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
5. When complete, a notification will appear saying *"APK(s) generated successfully"*. Click **locate** to reveal your `app-debug.apk`.

### Option B: Using Command Line (Terminal / PowerShell)
Open your terminal in the extracted `JARVIS` root directory and run:

- **On Windows (PowerShell or Command Prompt):**
  ```powershell
  .\gradlew assembleDebug
  ```
- **On macOS or Linux:**
  ```bash
  chmod +x ./gradlew
  ./gradlew assembleDebug
  ```

Your compiled `.apk` will be saved at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📲 Step 3: Install the APK on Your Phone

You can transfer and install the APK using any of the following methods:

### Method 1: Direct USB Sideload (Fastest)
1. On your phone, go to **Settings** > **About phone**, and tap **Build number** 7 times to enable Developer options.
2. In **Settings** > **System** > **Developer options**, enable **USB debugging**.
3. Plug your phone into your computer via a USB cable.
4. Run this command in your computer's terminal:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Method 2: Transfer via Google Drive, WhatsApp, or USB File Transfer
1. Send the `app-debug.apk` file to your phone via Google Drive, Telegram, email, or a USB cable.
2. Open the file on your phone using your device's **Files** app.
3. Tap on the file. If prompted, select **Allow from this source** to permit installing unknown apps.
4. Tap **Install**.

---

## 🚀 Step 4: First-Time Setup on Your Phone

Once installed, tap the **JARVIS** icon on your home screen:

1. **Grant Required Permissions**:
   - Go to the **Permission Center** in the app.
   - Tap **Allow Microphone** (`RECORD_AUDIO` is required for wake word detection).
   - Tap **Allow Notifications** (keeps the persistent service active in the background).
   - Tap **Allow Contacts** (required for "Call [Name] on WhatsApp").
   - Tap **Allow Write Settings** (required for screen brightness adjustment).
2. **Battery Optimization**:
   - Tap **Disable Battery Optimization** so Android does not close the microphone service when your screen turns off.
3. **Start Wake Word Service**:
   - On the main screen, tap **START WAKE SERVICE**.
   - You will see a small ongoing notification in your status bar indicating *"LOCAL WAKE MODE — Active"*.

---

## 🗣️ Step 5: Test JARVIS Voice Commands

Once the service is active:
1. **Activate JARVIS (Once only!):**
   - Say **"Jarvis"** (or tap **"ACTIVATE JARVIS"** on your screen or from the notification bar).
   - JARVIS will respond: **"Ji, boliye?"** (100% silent, zero annoying beeps!).
2. **Continuous Hands-Free Conversation Mode:**
   - JARVIS stays active continuously! You do NOT need to say "Jarvis" again for subsequent commands.
   - Speak commands back-to-back:
     - *"YouTube open karo"*
     - *"Dusre number wala song play karo"* (Clicks the 2nd song on screen!)
     - *"Alarm set karo 7 baje ka"*
     - *"Camera kholo selfie ke liye"*
     - *"Flashlight on karo"* / *"Flashlight band karo"*
     - *"Albert Einstein kaun tha?"* (ChatGPT-grade full artificial intelligence answer)
3. **Enhanced Noise-Resistant Microphone:**
   - Digital Signal Processing (DSP) hardware noise suppression, acoustic echo cancellation, and gain control are automatically applied to hear you clearly even in noisy environments.
4. **Deactivate JARVIS:**
   - When you are done, say: **"Deactivate"**, **"Deactivate Jarvis"**, **"Band ho jao"**, or **"So jao"** (or tap **"DEACTIVATE JARVIS"**).
   - JARVIS will confirm: *"Jarvis deactivate kar diya gaya hai. Main standby par hoon."* and rest in silent standby until you need it again!
