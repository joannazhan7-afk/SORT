Buninyong PO Box Scanner - Android source project

What it does:
- Native Android app, not a website
- Uses phone camera directly
- Uses Google ML Kit text recognition to OCR parcel name/address
- Matches against the built-in Excel data converted to app/src/main/assets/pobox_data.json
- Speaks English: "P O Box one two three" / "P O Box A B C"

How to build APK:
1. Install Android Studio on Windows/Mac.
2. Open this folder: BuninyongPOBoxScannerAndroid
3. Let Android Studio sync Gradle.
4. Plug in an Android phone and press Run, or choose Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Install the generated APK on the Android phone.

Note:
- The first version is a working prototype. In real post-office use, matching thresholds may need tuning after scanning real labels.
- The app currently ignores records marked cancelled.
- No HTTPS/browser is needed.
