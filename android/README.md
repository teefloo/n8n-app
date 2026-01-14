# n8n Mobile Manager

<p align="center">
  <img src="app/src/main/res/drawable/ic_splash_logo.xml" width="120" alt="n8n Mobile Manager Logo">
</p>

<p align="center">
  <strong>📱 Manage your n8n workflows from your mobile</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#installation">Installation</a> •
  <a href="#configuration">Configuration</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#contribution">Contribution</a>
</p>

---

## 🎯 About

**n8n Mobile Manager** is a native Android application designed for [n8n](https://n8n.io/) users, the open-source automation platform. It allows you to visualize, monitor, and remotely control your n8n instance from a smartphone or tablet.

## ✨ Features

### 📊 Dashboard
- Instance status (online / offline)
- Global statistics (active workflows, executions, success rate)
- Recent executions with quick access to details
- Quick actions to access main sections

### ⚙️ Workflow Management
- Complete list with search and filtering
- Enable/disable workflows
- View details (nodes, settings)
- Manual workflow triggering

### 📜 Execution Tracking
- Complete execution history
- Filter by status (success, error, running) and workflow
- Execution details with logs
- Actions: retry, stop

### 🔐 Credential Management
- Secure access with biometric authentication
- List of all configured credentials
- View details (types, dates)

### ⚡ Settings
- Manage multiple n8n instances
- Light/dark/system theme
- Biometric authentication
- Configurable push notifications

## 🚀 Installation

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** or newer
- **Android SDK** version 35
- **Device/Emulator** Android API 26+ (Android 8.0)

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/n8n-mobile-manager.git
   cd n8n-mobile-manager
   ```

2. **Open in Android Studio**
   ```
   File > Open > Select the project folder
   ```

3. **Sync Gradle**
   ```
   Android Studio will automatically sync dependencies
   ```

4. **Run the application**
   ```
   Run > Run 'app' or Shift+F10
   ```

## ⚙️ Configuration

### n8n Configuration

To use the application, you need:

1. **An accessible n8n instance** (self-hosted or cloud)
2. **An n8n API key**:
   - Go to `Settings > API > API Keys` in your n8n
   - Create a new API key
   - Copy the generated key

### Firebase Configuration (optional)

For push notifications:

1. Create a Firebase project
2. Add an Android app with the package `com.n8n.mobilemanager`
3. Download `google-services.json` and place it in `app/`
4. Enable Cloud Messaging

## 🏗️ Architecture

```
app/src/main/java/com/n8n/mobilemanager/
├── data/
│   ├── local/          # Room Database & DataStore
│   ├── model/          # Data models
│   ├── remote/         # Retrofit API & DTOs
│   └── repository/     # Repository pattern
├── di/                 # Hilt Dependency Injection
├── service/            # Services (Firebase Messaging)
├── ui/
│   ├── components/     # Reusable components
│   ├── navigation/     # Compose Navigation
│   ├── screens/        # Application screens
│   │   ├── dashboard/
│   │   ├── workflows/
│   │   ├── executions/
│   │   ├── credentials/
│   │   └── settings/
│   └── theme/          # Material 3 Theme
└── utils/              # Utilities
```

### Tech Stack

| Category | Technologies |
|----------|-------------|
| **UI** | Jetpack Compose, Material 3 |
| **Architecture** | MVVM, Repository Pattern |
| **DI** | Hilt |
| **Network** | Retrofit, OkHttp |
| **Database** | Room, DataStore |
| **Async** | Kotlin Coroutines, Flow |
| **Notifications** | Firebase Cloud Messaging |
| **Security** | BiometricPrompt, HTTPS |

## 📱 Screenshots

> *Coming soon*

## 🛠️ Development

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Tests
```bash
./gradlew test
```

### Lint
```bash
./gradlew lint
```

## 🔗 n8n API

The application uses the [n8n REST API v1](https://docs.n8n.io/api/). Endpoints used:

- `GET /healthz` - Health check
- `GET /api/v1/workflows` - List workflows
- `POST /api/v1/workflows/{id}/activate` - Activate a workflow
- `POST /api/v1/workflows/{id}/deactivate` - Deactivate a workflow
- `GET /api/v1/executions` - List executions
- `GET /api/v1/credentials` - List credentials

## 🤝 Contribution

Contributions are welcome! Feel free to:

1. Fork the project
2. Create a branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -m 'Add my feature'`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [n8n.io](https://n8n.io/) for their excellent automation platform
- [Material Design 3](https://m3.material.io/) for the design guidelines
- The Android and Kotlin community

---

<p align="center">
  Made with ❤️ for the n8n community
</p>
