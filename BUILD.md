# Build Instructions

## Server Build
```bash
javac -cp . *.java
java -cp . Main
```

## Android Build
```bash
cd android-client
./gradlew assembleDebug
```

## Run Web Server
```bash
java -cp . Main
```
Server will start on http://0.0.0.0:8080

## Quick Start
1. Configure Firebase credentials in web-admin/admin.js and android-client/app/google-services.json
2. Update Firebase database rules using firebase-database-rules.json
3. Build and run server: `javac -cp . *.java && java -cp . Main`
4. Install Android APK on target device
5. Access web admin at server URL
