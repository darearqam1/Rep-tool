
# Deployment Guide

## Replit Deployment (Recommended for Development)

### Autoscale Deployment
1. Click **Deploy** button in Replit header
2. Select **Autoscale Deployment**
3. Configure deployment settings:
   - **Build Command**: `javac -cp . *.java`
   - **Run Command**: `java -cp . Main`
   - **Machine Type**: Basic (0.25 vCPU, 0.5 GB RAM)
   - **Min Instances**: 0 (scales to zero when idle)
   - **Max Instances**: 1 (for development)

### Environment Configuration
Set up environment variables in Replit Secrets:
- `FIREBASE_API_KEY`: Your Firebase API key
- `FIREBASE_PROJECT_ID`: Your Firebase project ID
- `FIREBASE_DATABASE_URL`: Your Firebase database URL

### Custom Domain
- Configure custom domain in Deployment settings
- Update Firebase authorized domains
- Update Android app configuration

## Production Deployment Options

### Option 1: Cloud Platform (GCP/AWS/Azure)

#### Google Cloud Platform
```bash
# Create App Engine app.yaml
runtime: java11
env: standard

automatic_scaling:
  min_instances: 1
  max_instances: 10

# Deploy
gcloud app deploy
```

#### AWS Elastic Beanstalk
```bash
# Create JAR file
jar cvfe learner-tool.jar Main *.class web-admin/

# Deploy via EB CLI
eb init
eb create production
eb deploy
```

### Option 2: VPS/Dedicated Server

#### Requirements
- Ubuntu 20.04+ or CentOS 8+
- Java 11+ installed
- Nginx (recommended as reverse proxy)
- SSL certificate for HTTPS

#### Installation Steps
```bash
# 1. Install Java
sudo apt update
sudo apt install openjdk-11-jre-headless

# 2. Create application user
sudo useradd -r -s /bin/false learner-tool

# 3. Deploy application
sudo mkdir -p /opt/learner-tool
sudo cp *.java /opt/learner-tool/
sudo cp -r web-admin /opt/learner-tool/
sudo chown -R learner-tool:learner-tool /opt/learner-tool

# 4. Compile application
cd /opt/learner-tool
sudo -u learner-tool javac -cp . *.java

# 5. Create systemd service
sudo tee /etc/systemd/system/learner-tool.service > /dev/null << 'EOF'
[Unit]
Description=Learner Tool Server
After=network.target

[Service]
Type=simple
User=learner-tool
Group=learner-tool
WorkingDirectory=/opt/learner-tool
ExecStart=/usr/bin/java -Xmx256m -cp . Main
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

# 6. Start service
sudo systemctl daemon-reload
sudo systemctl enable learner-tool
sudo systemctl start learner-tool
```

#### Nginx Configuration
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Option 3: Docker Deployment

#### Dockerfile
```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy application files
COPY *.java ./
COPY web-admin ./web-admin/

# Compile application
RUN javac -cp . *.java

# Expose port
EXPOSE 8080

# Run application
CMD ["java", "-cp", ".", "Main"]
```

#### Docker Compose
```yaml
version: '3.8'
services:
  learner-tool:
    build: .
    ports:
      - "8080:8080"
    environment:
      - FIREBASE_API_KEY=${FIREBASE_API_KEY}
      - FIREBASE_PROJECT_ID=${FIREBASE_PROJECT_ID}
    restart: unless-stopped
    
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - learner-tool
    restart: unless-stopped
```

## Firebase Production Configuration

### Security Rules
```json
{
  "rules": {
    "devices": {
      ".read": "auth != null",
      ".write": "auth != null",
      "$uid": {
        ".validate": "auth != null && $uid == auth.uid"
      }
    },
    "commands": {
      ".read": "auth != null",
      ".write": "auth != null",
      "$uid": {
        ".validate": "auth != null"
      }
    }
  }
}
```

### Performance Optimization
1. Enable Firebase Hosting for web assets
2. Configure Firebase CDN
3. Set up Firebase Performance Monitoring
4. Enable compression in Realtime Database

## Android App Distribution

### Google Play Store
1. Generate signed release APK
2. Create Play Console developer account
3. Upload APK and complete store listing
4. Submit for review

### Firebase App Distribution
```bash
# Install Firebase CLI
npm install -g firebase-tools

# Distribute to testers
firebase appdistribution:distribute \
  app/build/outputs/apk/release/app-release.apk \
  --app 1:953487025826:android:78e8b0ae0d5389b4b4d9fb \
  --groups "qa-team" \
  --release-notes "Latest version with bug fixes"
```

### Enterprise Distribution
- Use Android Enterprise solutions
- Configure managed Play Store
- Implement mobile device management (MDM)

## Monitoring and Maintenance

### Health Checks
```bash
# Check server status
curl -f http://your-domain.com/health || exit 1

# Check Firebase connectivity
curl -f https://your-firebase-project.firebaseio.com/.json
```

### Logging
```java
// Add structured logging
import java.util.logging.Logger;
import java.util.logging.FileHandler;

Logger logger = Logger.getLogger("LearnerTool");
FileHandler fh = new FileHandler("/var/log/learner-tool.log");
logger.addHandler(fh);
```

### Backup Strategy
1. Firebase automatic backups
2. Regular database exports
3. Application code version control
4. Configuration file backups

### Updates and Rollbacks
```bash
# Zero-downtime deployment
sudo systemctl stop learner-tool
sudo cp new-version/*.java /opt/learner-tool/
sudo -u learner-tool javac -cp . *.java
sudo systemctl start learner-tool

# Rollback if needed
sudo systemctl stop learner-tool
sudo cp backup/*.class /opt/learner-tool/
sudo systemctl start learner-tool
```

## Security Hardening

### Server Security
```bash
# Firewall configuration
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# Regular updates
sudo apt update && sudo apt upgrade

# Security monitoring
sudo apt install fail2ban
sudo systemctl enable fail2ban
```

### SSL/TLS Configuration
```bash
# Let's Encrypt certificate
sudo apt install certbot
sudo certbot --nginx -d your-domain.com
```

### Application Security
1. Input validation on all endpoints
2. Rate limiting for API requests
3. Audit logging for sensitive operations
4. Regular security updates

## Performance Optimization

### JVM Tuning
```bash
# Production JVM flags
java -Xmx512m -Xms256m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -cp . Main
```

### Database Optimization
1. Index frequently queried fields
2. Implement data retention policies
3. Use Firebase offline persistence
4. Optimize query patterns

### Network Optimization
1. Enable gzip compression
2. Use CDN for static assets
3. Implement proper caching headers
4. Minimize payload sizes

This deployment guide covers various scenarios from development to production-ready deployments with proper security, monitoring, and maintenance practices.
