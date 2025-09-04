# Firebase Google Sign-In Setup Instructions

The Google Sign-In feature is currently not working because Firebase is not properly configured. Here's how to fix it:

## Current Issue
- The `strings.xml` file contains a placeholder: `YOUR_WEB_CLIENT_ID_HERE`
- The `google-services.json` file has an empty `oauth_client` array
- This prevents Google Sign-In from working properly

## Steps to Fix

### 1. Firebase Console Setup
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `herdmanager-8eeb7`
3. Go to **Authentication** → **Sign-in method**
4. Enable **Google** as a sign-in provider
5. Add your app's SHA-1 fingerprint (see step 2)

### 2. Get SHA-1 Fingerprint
Run this command in your project directory:
```bash
./gradlew signingReport
```
Copy the SHA-1 fingerprint from the debug keystore.

### 3. Update Firebase Project
1. In Firebase Console, go to **Project Settings**
2. Under **Your apps**, find your Android app
3. Click the gear icon → **Add fingerprint**
4. Paste your SHA-1 fingerprint
5. Download the updated `google-services.json` file

### 4. Replace Files
1. Replace `app/google-services.json` with the new file
2. The new file should have OAuth client entries like:
```json
"oauth_client": [
  {
    "client_id": "your-client-id.apps.googleusercontent.com",
    "client_type": 1,
    "android_info": {
      "package_name": "com.jumblemint.cows",
      "certificate_hash": "your-sha1-hash"
    }
  },
  {
    "client_id": "your-web-client-id.apps.googleusercontent.com",
    "client_type": 3
  }
]
```

### 5. Update strings.xml
The `default_web_client_id` in `strings.xml` should be automatically updated by the Google Services plugin, but if not, replace `YOUR_WEB_CLIENT_ID_HERE` with the web client ID from the JSON file.

## Current Demo Mode
Until Firebase is properly configured, the app shows:
- A warning about Firebase configuration
- A demo button that explains what would happen
- Debug information showing the current state

## Testing
Once configured:
1. The sign-in button will work normally
2. Users can sign in with their Google accounts
3. Data will sync across devices (when sync backend is implemented)
4. The debug card can be removed from the SignInScreen

## Note
The app works perfectly in local-only mode without Firebase configuration. Users can use all features with local storage.