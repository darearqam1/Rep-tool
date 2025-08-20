
# Learner Tool - Remote Device Monitoring System

A comprehensive remote device monitoring and management system built with Java backend, Android client, and modern web administration interface.

## 🏗️ Architecture Overview

### Components
- **Java HTTP Server**: Serves web admin interface and handles requests
- **Android Client**: Monitors device activity and executes remote commands
- **Web Admin Interface**: Modern Firebase-powered dashboard for device management
- **Firebase Backend**: Real-time database for device data and command distribution

## 📋 Features

### Device Monitoring
- ✅ Real-time device status and connectivity
- ✅ Call log monitoring and upload
- ✅ SMS monitoring and logging
- ✅ Device profile information
- ✅ Location tracking (with permissions)
- ✅ System information collection

### Remote Commands
- ✅ Send SMS messages remotely
- ✅ Real-time command execution feedback
- ✅ Command history and results

### Web Administration
- ✅ Live device dashboard
- ✅ Real-time connection status
- ✅ Device selection and detailed views
- ✅ SMS sending interface
- ✅ Call and SMS history viewing

## 🚀 Quick Start

### Prerequisites
- Java 11+ installed
- Android Studio for mobile app development
- Firebase account with project setup
- Modern web browser

### 1. Firebase Setup
1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Enable Authentication (Anonymous sign-in)
3. Enable Realtime Database
4. Update database rules with the provided `firebase-database-rules.json`
5. Update Firebase configuration in both web and Android code

### 2. Server Setup
```bash
# Compile Java files
javac -cp . *.java

# Start the web server
java -cp . Main
```
The server will start on `http://0.0.0.0:8080`

### 3. Android App Setup
1. Open `android-client` in Android Studio
2. Sync project with Gradle files
3. Update `google-services.json` with your Firebase configuration
4. Build and install on target device
5. Grant all required permissions when prompted

### 4. Web Admin Access
Navigate to `http://your-server-ip:8080` to access the administration interface.

## 📁 Project Structure

```
learner-tool/
├── Main.java                           # Entry point, starts HTTP server
├── SimpleHttpServer.java               # HTTP server implementation
├── web-admin/                          # Web administration interface
│   ├── index.html                      # Main dashboard HTML
│   ├── admin.js                        # Firebase integration & UI logic
│   └── styles.css                      # Modern responsive styling
├── android-client/                     # Android monitoring application
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/wains/app/     # Java source code
│   │   │   │   ├── MainActivity.java   # Main activity & Firebase integration
│   │   │   │   ├── SmsReceiver.java    # SMS broadcast receiver
│   │   │   │   └── BootReceiver.java   # Auto-start on boot
│   │   │   ├── res/                    # Android resources
│   │   │   │   ├── layout/             # UI layouts
│   │   │   │   ├── values/             # Strings, colors, themes
│   │   │   │   └── drawable/           # Graphics and icons
│   │   │   └── AndroidManifest.xml     # App permissions & components
│   │   ├── build.gradle                # App dependencies
│   │   └── google-services.json        # Firebase configuration
│   └── build.gradle                    # Project-level build config
└── firebase-database-rules.json        # Firebase security rules
```

## 🔧 Configuration

### Firebase Database Rules
The system requires specific Firebase database rules for security:

```json
{
  "rules": {
    "devices": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "commands": {
      ".read": "auth != null", 
      ".write": "auth != null"
    }
  }
}
```

### Android Permissions
The app requires these permissions for full functionality:
- `CAMERA` - Camera access
- `RECORD_AUDIO` - Microphone access
- `ACCESS_FINE_LOCATION` - Precise location
- `ACCESS_COARSE_LOCATION` - Approximate location
- `READ_CALL_LOG` - Call history access
- `READ_SMS` - SMS reading
- `RECEIVE_SMS` - SMS receiving
- `SEND_SMS` - SMS sending
- `READ_PHONE_STATE` - Phone state information

## 📊 Data Flow

### Device Registration
1. Android app authenticates with Firebase anonymously
2. Device uploads profile information and capabilities
3. Regular check-ins update last-seen timestamp
4. Call logs and SMS data uploaded to Firebase

### Command Execution
1. Admin sends command through web interface
2. Command stored in Firebase under `/commands/{device_uid}/`
3. Android app receives command via Firebase listener
4. Command executed locally on device
5. Result uploaded to `/commands/{device_uid}/last_result`
6. Web interface displays execution result

### Real-time Updates
- Device status updates every few minutes
- Web interface receives real-time Firebase updates
- Connection status monitored via Firebase `.info/connected`

## 🔐 Security Considerations

### Firebase Security
- Anonymous authentication prevents unauthorized access
- Database rules restrict access to authenticated users only
- API keys should be environment-specific in production

### Android Security
- Sensitive permissions clearly declared in manifest
- User consent required for all privacy-sensitive operations
- Local data encrypted where possible

### Network Security
- HTTPS should be used in production deployments
- Network security config allows cleartext for development
- Server should implement proper CORS and security headers

## 🚀 Deployment

### Development Deployment (Replit)
The project is configured for easy deployment on Replit:

1. Import project to Replit
2. Run button automatically compiles and starts server
3. Web interface accessible via Replit's generated URL
4. Configure Firebase with Replit environment variables

### Production Deployment

#### Server Deployment
```bash
# Build production JAR
javac -cp . *.java
jar cvfe learner-tool.jar Main *.class web-admin/

# Run with production settings
java -Xmx512m -jar learner-tool.jar
```

#### Firebase Configuration
- Use Firebase environment configuration
- Enable proper security rules
- Configure custom domains if needed
- Set up monitoring and alerting

#### Android Distribution
- Build signed APK for production
- Use Firebase App Distribution for beta testing
- Configure ProGuard for code obfuscation
- Test across different Android versions

## 📱 Supported Platforms

### Server Requirements
- Java 11+ (tested with OpenJDK 11, 17, 21)
- Linux/Windows/macOS
- Minimum 256MB RAM
- Network connectivity for Firebase

### Android Requirements
- Android 5.0+ (API level 21+)
- ARM/x86 architectures
- Internet connectivity (WiFi or mobile data)
- 50MB+ storage space

### Web Admin Requirements
- Modern browsers (Chrome 80+, Firefox 75+, Safari 13+)
- JavaScript enabled
- Internet connectivity for Firebase
- Minimum 1024x768 screen resolution

## 🛠️ Development

### Building from Source

#### Java Server
```bash
# Compile
javac -cp . *.java

# Run development server
java -cp . Main
```

#### Android App
```bash
# Using Gradle wrapper
cd android-client
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug
```

### Testing

#### Unit Testing
```bash
# Run Android unit tests
cd android-client
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest
```

#### Integration Testing
1. Start local server
2. Install debug APK on test device
3. Verify Firebase connectivity
4. Test command execution flow
5. Validate data synchronization

### Code Style
- Java: Follow Oracle Java conventions
- JavaScript: Use ES6+ modern syntax
- Android: Follow Android coding standards
- Use meaningful variable names and comments

## 🐛 Troubleshooting

### Common Issues

#### Firebase Connection Errors
```
Error: permission_denied at /devices
```
**Solution**: Check Firebase database rules and authentication

#### Android Permission Denied
```
Permission denied for READ_SMS
```
**Solution**: Manually grant permissions in Android Settings

#### Server Port Conflicts
```
Address already in use: 8080
```
**Solution**: Change port in Main.java or kill existing process

#### WebView Not Loading
```
ERR_CLEARTEXT_NOT_PERMITTED
```
**Solution**: Check network security config allows cleartext traffic

### Debug Mode
Enable debug logging by setting log level:
```java
// In MainActivity.java
private static final boolean DEBUG = true;
```

### Performance Monitoring
- Monitor Firebase usage in Firebase Console
- Check Android app memory usage in Android Studio
- Monitor server resources with system tools

## 📄 License

This project is for educational and research purposes. Please ensure compliance with local laws and regulations regarding device monitoring and privacy.

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Implement changes with tests
4. Submit pull request with detailed description
5. Ensure all CI checks pass

## 📞 Support

For issues and questions:
1. Check troubleshooting section above
2. Review Firebase Console for backend issues
3. Use Android Studio debugger for app issues
4. Check browser console for web interface problems

## 🔄 Version History

### v1.0.0 (Current)
- Initial release with core functionality
- Firebase integration
- Modern web interface
- Android 5.0+ support
- Real-time device monitoring
- Remote SMS commands

### Roadmap
- [ ] File transfer capabilities
- [ ] Screen capture functionality
- [ ] Location tracking enhancements
- [ ] Multi-language support
- [ ] Advanced command scripting
- [ ] Device grouping and bulk operations
