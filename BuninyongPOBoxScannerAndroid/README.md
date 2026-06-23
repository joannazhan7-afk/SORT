# Buninyong PO Box Scanner

Android app prototype for scanning parcel names/addresses and matching them to Buninyong PO Box records.

## What is included

- Android source code
- Built-in PO Box database: `app/src/main/assets/pobox_data.json`
- Camera scanning with Google ML Kit OCR
- English voice output, for example: `P O Box one two three`
- GitHub Actions workflow to build an APK automatically

## How to generate the APK with GitHub

1. Create a new GitHub repository.
2. Upload **all files and folders from this project**, not the ZIP file itself.
3. Open the repository on GitHub.
4. Click **Actions**.
5. Click **Build Android APK**.
6. Click **Run workflow**.
7. Wait a few minutes until the build finishes.
8. Open the completed workflow run.
9. Download the artifact named **BuninyongPOBoxScanner-debug-apk**.
10. Unzip it and install `app-debug.apk` on an Android phone.

## Android phone installation note

Android may warn that the APK is from an unknown source. Allow installation only if this is your own GitHub build.

## Future improvement

For daily post office use, the best next version should read an Excel or CSV file from the phone storage, so PO Box customer changes do not require rebuilding the APK.
