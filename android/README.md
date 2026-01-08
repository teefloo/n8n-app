# n8n Mobile Manager

<p align="center">
  <img src="app/src/main/res/drawable/ic_splash_logo.xml" width="120" alt="n8n Mobile Manager Logo">
</p>

<p align="center">
  <strong>📱 Gérez vos workflows n8n depuis votre mobile</strong>
</p>

<p align="center">
  <a href="#fonctionnalités">Fonctionnalités</a> •
  <a href="#installation">Installation</a> •
  <a href="#configuration">Configuration</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#contribution">Contribution</a>
</p>

---

## 🎯 À propos

**n8n Mobile Manager** est une application Android native conçue pour les utilisateurs de [n8n](https://n8n.io/), la plateforme d'automatisation open-source. Elle permet de visualiser, superviser et contrôler à distance votre instance n8n depuis un smartphone ou une tablette.

## ✨ Fonctionnalités

### 📊 Tableau de bord
- Statut de l'instance (en ligne / hors ligne)
- Statistiques globales (workflows actifs, exécutions, taux de succès)
- Exécutions récentes avec accès rapide aux détails
- Actions rapides pour accéder aux sections principales

### ⚙️ Gestion des Workflows
- Liste complète avec recherche et filtrage
- Activation/désactivation des workflows
- Visualisation des détails (nœuds, paramètres)
- Déclenchement manuel de workflows

### 📜 Suivi des Exécutions
- Historique complet des exécutions
- Filtrage par statut (succès, erreur, en cours) et workflow
- Détails des exécutions avec logs
- Actions : réessayer, stopper

### 🔐 Gestion des Credentials
- Accès sécurisé avec authentification biométrique
- Liste de tous les credentials configurés
- Visualisation des détails (types, dates)

### ⚡ Paramètres
- Gestion de plusieurs instances n8n
- Thème clair/sombre/système
- Authentification biométrique
- Notifications push configurables

## 🚀 Installation

### Prérequis

- **Android Studio** Hedgehog (2023.1.1) ou plus récent
- **JDK 17** ou plus récent
- **Android SDK** version 35
- **Appareil/Émulateur** Android API 26+ (Android 8.0)

### Étapes

1. **Cloner le repository**
   ```bash
   git clone https://github.com/votre-username/n8n-mobile-manager.git
   cd n8n-mobile-manager
   ```

2. **Ouvrir dans Android Studio**
   ```
   File > Open > Sélectionner le dossier du projet
   ```

3. **Synchroniser Gradle**
   ```
   Android Studio synchronisera automatiquement les dépendances
   ```

4. **Lancer l'application**
   ```
   Run > Run 'app' ou Shift+F10
   ```

## ⚙️ Configuration

### Configuration n8n

Pour utiliser l'application, vous devez avoir :

1. **Une instance n8n accessible** (self-hosted ou cloud)
2. **Une clé API n8n** :
   - Allez dans `Settings > API > API Keys` dans votre n8n
   - Créez une nouvelle clé API
   - Copiez la clé générée

### Configuration Firebase (optionnel)

Pour les notifications push :

1. Créez un projet Firebase
2. Ajoutez une application Android avec le package `com.n8n.mobilemanager`
3. Téléchargez `google-services.json` et placez-le dans `app/`
4. Activez Cloud Messaging

## 🏗️ Architecture

```
app/src/main/java/com/n8n/mobilemanager/
├── data/
│   ├── local/          # Base de données Room & DataStore
│   ├── model/          # Modèles de données
│   ├── remote/         # API Retrofit & DTOs
│   └── repository/     # Repository pattern
├── di/                 # Injection de dépendances Hilt
├── service/            # Services (Firebase Messaging)
├── ui/
│   ├── components/     # Composants réutilisables
│   ├── navigation/     # Navigation Compose
│   ├── screens/        # Écrans de l'application
│   │   ├── dashboard/
│   │   ├── workflows/
│   │   ├── executions/
│   │   ├── credentials/
│   │   └── settings/
│   └── theme/          # Thème Material 3
└── utils/              # Utilitaires
```

### Stack Technique

| Catégorie | Technologies |
|-----------|-------------|
| **UI** | Jetpack Compose, Material 3 |
| **Architecture** | MVVM, Repository Pattern |
| **DI** | Hilt |
| **Réseau** | Retrofit, OkHttp |
| **Base de données** | Room, DataStore |
| **Async** | Kotlin Coroutines, Flow |
| **Notifications** | Firebase Cloud Messaging |
| **Sécurité** | BiometricPrompt, HTTPS |

## 📱 Screenshots

> *À venir*

## 🛠️ Développement

### Build Debug
```bash
./gradlew assembleDebug
```

### Build Release
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

## 🔗 API n8n

L'application utilise l'[API REST n8n v1](https://docs.n8n.io/api/). Endpoints utilisés :

- `GET /healthz` - Health check
- `GET /api/v1/workflows` - Liste des workflows
- `POST /api/v1/workflows/{id}/activate` - Activer un workflow
- `POST /api/v1/workflows/{id}/deactivate` - Désactiver un workflow
- `GET /api/v1/executions` - Liste des exécutions
- `GET /api/v1/credentials` - Liste des credentials

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à :

1. Fork le projet
2. Créer une branche (`git checkout -b feature/ma-feature`)
3. Commit vos changements (`git commit -m 'Ajoute ma feature'`)
4. Push sur la branche (`git push origin feature/ma-feature`)
5. Ouvrir une Pull Request

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails.

## 🙏 Remerciements

- [n8n.io](https://n8n.io/) pour leur excellente plateforme d'automatisation
- [Material Design 3](https://m3.material.io/) pour les guidelines de design
- La communauté Android et Kotlin

---

<p align="center">
  Fait avec ❤️ pour la communauté n8n
</p>
