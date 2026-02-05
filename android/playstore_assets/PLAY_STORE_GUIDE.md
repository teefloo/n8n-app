# 📱 Google Play Store Publication Guide - n8n Manager

## 📂 Assets in this folder

| File | Usage | Dimensions |
|------|-------|------------|
| `app_icon_512.png` | App icon (Hi-res) | 512x512 |
| `feature_graphic.png` | Feature Graphic | 1024x500 |
| `screenshot_1_dashboard.png` | Screenshot 1 | Phone |
| `screenshot_2_workflows.png` | Screenshot 2 | Phone |
| `screenshot_3_executions.png` | Screenshot 3 | Phone |
| `screenshot_4_credentials.png` | Screenshot 4 | Phone |
| `screenshot_5_settings.png` | Screenshot 5 | Phone |

## 📝 App Information

### Title (max 30 characters)
```
n8n Manager
```

### Short Description (max 80 characters)
```
Control and monitor your n8n automation workflows from anywhere
```

### Full Description (max 4000 characters)
```
n8n Manager is the ultimate mobile companion for n8n users. Monitor your automation workflows, track executions, and manage your n8n instances – all from your smartphone.

🎯 KEY FEATURES

📊 DASHBOARD
• Real-time instance status monitoring (online/offline)
• Global statistics: active workflows, executions, success rate
• Quick access to recent executions
• Filter by time period (24h, 7 days, 30 days, all time)

⚙️ WORKFLOW MANAGEMENT
• View all your workflows with search and filtering
• Enable/disable workflows with a single tap
• View workflow details and node configuration
• Trigger workflows manually on the go

📜 EXECUTION TRACKING
• Complete execution history
• Filter by status (success, error, running) and workflow
• Detailed execution logs and timeline
• Retry failed executions instantly

🔐 CREDENTIAL MANAGEMENT
• Secure access with biometric authentication
• View all configured credentials
• Check credential types and validity

⚡ SETTINGS
• Manage multiple n8n instances
• Light, dark, or system theme
• Biometric security
• Configurable push notifications for errors and success

🔒 SECURITY FIRST
• Secure API key storage
• Biometric authentication support
• HTTPS-only connections

💡 PERFECT FOR
• DevOps engineers managing automation workflows
• Business users monitoring integrations
• Developers testing and debugging workflows
• Anyone using n8n for automation

Requires an n8n instance (self-hosted or cloud) with API access enabled.
```

## 📋 Step-by-Step Guide

### 1. Create App on Play Console
1. Go to https://play.google.com/console
2. Click "Create app"
3. Fill in:
   - App name: `n8n Manager`
   - Default language: English (US)
   - App or game: App
   - Free or paid: Free
4. Accept declarations and create

### 2. Store Listing (Main store listing)
1. **App Details**
   - Short description: (copy from above)
   - Full description: (copy from above)

2. **Graphics**
   - App icon: Upload `app_icon_512.png`
   - Feature graphic: Upload `feature_graphic.png`
   - Phone screenshots: Upload all 5 screenshots

### 3. App Content
Fill out the questionnaires:
- Privacy policy URL (REQUIRED)
- App access: All functionality available without restrictions
- Ads: No ads
- Content rating: Complete questionnaire (likely Everyone)
- Target audience: 18+
- News apps: Not a news app
- Data safety: Fill based on app functionality

### 4. Release
1. Go to "Production" > "Create new release"
2. Upload AAB file: `app-debug.aab` (or release version)
3. Add release notes:
   ```
   Initial release of n8n Manager v1.0.0
   
   Features:
   • Dashboard with real-time statistics
   • Workflow management and control
   • Execution history and logs
   • Credential viewer
   • Multi-instance support
   • Dark theme
   • Push notifications
   ```
4. Review and start rollout

## ⚠️ Important Notes

1. **You need a release AAB**, not debug. Run:
   ```bash
   ./gradlew bundleRelease
   ```
   The signed AAB will be in `app/build/outputs/bundle/release/`

2. **Privacy Policy** is required. You can create one at:
   - https://app-privacy-policy-generator.firebaseapp.com/
   - Or host a simple page on GitHub Pages

3. **Developer fee**: $25 one-time (if not already paid)

4. **Review time**: Usually 1-3 days for new apps

---
Generated on: 2026-01-14
