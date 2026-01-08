# n8n Mobile Manager

Une application iOS native en SwiftUI pour gérer et superviser vos instances n8n depuis votre iPhone ou iPad.

![SwiftUI](https://img.shields.io/badge/SwiftUI-5.0-blue?logo=swift)
![iOS](https://img.shields.io/badge/iOS-17.0+-orange?logo=apple)
![License](https://img.shields.io/badge/License-MIT-green)

## 📱 Fonctionnalités

### Dashboard
- **Statut de l'instance** : Visualisez l'état de connexion en temps réel
- **Statistiques globales** : Nombre de workflows actifs, taux de succès, temps d'exécution moyen
- **Exécutions récentes** : Aperçu rapide des dernières exécutions
- **Workflows populaires** : Accès rapide aux workflows les plus utilisés

### Gestion des Workflows
- **Liste complète** avec recherche et filtres
- **Activation/Désactivation** en un tap
- **Exécution manuelle** directement depuis l'app
- **Vue détaillée** avec tous les nœuds et leurs configurations

### Suivi des Exécutions
- **Historique complet** avec pagination
- **Filtrage par statut** (succès, erreur, en cours)
- **Détails d'exécution** avec logs et données
- **Retry** des exécutions échouées

### Gestion des Credentials
- **Authentification biométrique** requise pour accéder aux données sensibles
- **Liste des credentials** avec recherche
- **Test de connexion** intégré
- **Affichage sécurisé** des informations

### Paramètres
- **Multi-instances** : Gérez plusieurs instances n8n
- **Thème** : Mode clair / sombre
- **Sécurité** : Authentification Face ID / Touch ID

## 🚀 Installation

### Prérequis
- macOS avec Xcode 15+
- iOS 17.0+
- Une instance n8n avec l'API activée

### Configuration

1. Clonez le repository
2. Ouvrez le projet dans Xcode
3. Sélectionnez votre appareil cible
4. Build & Run

## 🔧 Configuration de l'API n8n

Pour utiliser l'application, vous devez générer une clé API dans votre instance n8n :

1. Connectez-vous à votre instance n8n
2. Allez dans **Settings** → **n8n API**
3. Créez une nouvelle clé API
4. Copiez la clé générée
5. Dans l'app, ajoutez une nouvelle instance avec :
   - Le nom de votre instance
   - L'URL de base (ex: `https://n8n.votredomaine.com`)
   - La clé API

## 📁 Structure du Projet

```
n8nMobileManager/
├── App/
│   └── n8nMobileManagerApp.swift      # Point d'entrée
├── Models/
│   ├── Instance.swift                  # Modèle instance n8n
│   ├── Workflow.swift                  # Modèle workflow
│   ├── Execution.swift                 # Modèle exécution
│   └── Credential.swift                # Modèle credential
├── ViewModels/
│   ├── DashboardViewModel.swift        # Logique dashboard
│   ├── WorkflowsViewModel.swift        # Logique workflows
│   ├── ExecutionsViewModel.swift       # Logique exécutions
│   └── CredentialsViewModel.swift      # Logique credentials
├── Views/
│   ├── ContentView.swift               # Vue principale + TabView
│   ├── Dashboard/
│   │   └── DashboardView.swift         # Dashboard
│   ├── Workflows/
│   │   └── WorkflowsView.swift         # Liste & détail workflows
│   ├── Executions/
│   │   └── ExecutionsView.swift        # Historique exécutions
│   ├── Credentials/
│   │   └── CredentialsView.swift       # Gestion credentials
│   └── Settings/
│       ├── SettingsView.swift          # Paramètres
│       └── AddInstanceView.swift       # Ajout d'instance
├── Services/
│   └── N8nAPIService.swift             # Client API REST
├── Utils/
│   └── Extensions.swift                # Extensions SwiftUI
└── Assets.xcassets/                    # Couleurs et icônes
```

## 🔐 Sécurité

- **Authentification biométrique** pour les credentials
- **Stockage sécurisé** des clés API
- **Communication HTTPS** obligatoire
- **Pas de stockage** des données sensibles en clair

## 🎨 Design

L'application utilise les couleurs officielles de n8n :
- **Orange** : `#FF6D00` - Couleur principale
- **Pink** : `#FF4070` - Accents
- **Purple** : `#8640FF` - Actions secondaires
- **Green** : `#10B981` - Succès
- **Red** : `#EF4444` - Erreurs

## 📦 Dépendances

Aucune dépendance externe ! L'application utilise uniquement les frameworks natifs Apple :
- SwiftUI
- Combine
- Charts
- LocalAuthentication

## 🛠️ API n8n Utilisée

L'application utilise l'API REST v1 de n8n :

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/workflows` | Liste des workflows |
| `POST /api/v1/workflows/{id}/activate` | Activer un workflow |
| `POST /api/v1/workflows/{id}/deactivate` | Désactiver un workflow |
| `POST /api/v1/workflows/{id}/run` | Exécuter un workflow |
| `GET /api/v1/executions` | Historique des exécutions |
| `GET /api/v1/credentials` | Liste des credentials |
| `POST /api/v1/credentials/{id}/test` | Tester un credential |

## 📄 License

MIT License - Voir [LICENSE](LICENSE) pour plus de détails.

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à ouvrir une issue ou une pull request.

---

**n8n Mobile Manager** - Gérez vos automatisations où que vous soyez 🚀
