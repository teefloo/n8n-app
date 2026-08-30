# Conformité Google Play — n8n Manager

État de préparation au 30 août 2026. Ce document décrit l’état du dépôt et les opérations qui doivent encore être réalisées dans la Play Console. Il ne contient aucune clé privée.

## Synthèse

| Contrôle | État | Preuve / action restante |
| --- | --- | --- |
| Projet Android présent | Réalisé | `android/`, application Kotlin/Jetpack Compose |
| `compileSdk` | Réalisé | API 36 dans `android/app/build.gradle.kts` |
| `targetSdk` | Réalisé | API 36 dans `android/app/build.gradle.kts` |
| `minSdk` | Inchangé | API 26 |
| Version | Réalisé | `versionCode = 3`, `versionName = "1.1.0"` ; le code 2 est déjà publié sur le canal fermé Alpha et le code 3 a été envoyé pour examen |
| R8 / shrinking release | Réalisé dans la configuration | `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-android-optimize.txt` |
| Build release / bundle | Vérifié localement | `assembleRelease` et `bundleRelease` réussissent ; l’AAB est signé avec la clé d’upload locale configurée |
| Optimisation DEX | R8 exécuté ; métriques Play à confirmer | Le nouveau build produit les mappings R8 ; la couverture d’optimisation doit être contrôlée dans la Play Console après upload |
| Bitmaps applicatifs | Aucun problème évident détecté | `app/src/main/res` ne contient pas de PNG/JPEG/WebP/GIF ; les ressources visuelles sont vectorielles |
| Permission notifications | Réalisé | `POST_NOTIFICATIONS` est déclarée et demandée à partir d’Android 13 |
| Stockage / services en arrière-plan | Audité | Pas de permission de stockage ni de service foreground utilisé par le code applicatif ; le manifeste fusionné ajoute la permission WorkManager correspondante |
| Clé de release du dépôt | Non présente | Aucune clé privée, keystore ou fichier de signature n’est versionné |
| Signature de l’artefact local | Debug et release vérifiés | L’APK debug utilise le keystore debug local ; l’APK release et l’AAB release sont signés par la clé d’upload locale |
| Enregistrement du package | Réalisé dans Play Console | Le compte affiche que toutes les applications sont enregistrées pour la validation des développeurs Android |

## Identité Android et variantes

- Nom de package Play / `applicationId` : `com.n8n.mobilemanager`.
- Namespace Kotlin/Android : `com.n8n.mobilemanager`.
- Flavors : aucun `flavorDimensions` ni `productFlavors` détecté.
- Variantes déclarées : `debug` et `release`.
- Identifiants effectifs attendus : `com.n8n.mobilemanager` pour les deux variantes ; aucun `applicationIdSuffix` n’est configuré.
- Le dépôt contient une seule application Android. Le projet iOS n’ajoute aucun package Android à enregistrer.

Paramètres de build actuels :

```
compileSdk = 36
targetSdk = 36
minSdk = 26
versionCode = 3
versionName = 1.1.0
AGP = 8.13.2
Gradle wrapper = 8.13
Kotlin = 2.0.21
Java source/target = 17
```

Le `versionCode` a été augmenté à `3` après vérification de la Play Console : le code `2` est déjà publié sur le canal fermé Alpha. La release `3 (1.1.0)` a été envoyée pour examen le 30 août 2026.

## Certificats et rôles de signature

### Certificat public disponible

Un certificat public a été extrait sans exposer de clé privée de `android/app-release.aab`, l’archive de release déjà présente localement :

```
SHA-256 : BB:FA:E0:1B:BD:EC:E9:E9:66:D1:F1:19:AA:4A:CC:D3:EB:44:14:81:49:5B:C4:89:CC:8F:E7:02:8B:FA:0C:11
Sujet   : CN=Esteban, OU=Teeflo, L=Cranves-Sales, ST=Haute-Savoie, C=FR
```

Ce certificat est celui qui a signé l’AAB local. Sans accès à la Play Console, il est impossible de déterminer avec certitude s’il correspond à la clé d’upload ou à une clé de signature Play. Il faut donc le comparer dans la Play Console avant de l’enregistrer ou de l’utiliser pour une preuve de possession.

### Clés à enregistrer

| Rôle | État dans le dépôt | Action |
| --- | --- | --- |
| Clé de signature Play | Non disponible | Vérifier le certificat dans `Play Console > Configuration > Intégrité de l'application` |
| Clé d’upload | Certificat candidat ci-dessus, clé privée absente du dépôt | Comparer le SHA-256 à la Play Console et utiliser la clé privée conservée hors dépôt |
| Clé debug | Keystore local non versionné ; SHA-256 `D5:D1:AD:E5:7B:83:91:9A:B1:89:2F:05:AD:1A:5E:78:95:D0:F9:6B:64:1D:A1:FE:98:B9:0D:A9:F8:BF:74:7D` | APK debug uniquement ; ne pas enregistrer comme clé de production |
| Clés hors Google Play | Aucune configuration détectée | Déclarer séparément toute clé utilisée pour une distribution alternative |

Ne jamais ajouter de keystore, clé privée, mot de passe ou secret dans Git. La clé d’upload doit être conservée dans un coffre ou dans la configuration sécurisée de la CI.

## Modifications effectuées

- `compileSdk` et `targetSdk` passent de 35 à 36.
- `tools:targetApi` du manifeste passe à 36.
- AGP, Gradle, Kotlin et Java n’ont pas été montés de version : les versions présentes supportent déjà cette cible.
- Le chemin JDK Windows personnel a été retiré de `gradle.properties`. Le wrapper utilise désormais `JAVA_HOME` ou le `PATH` ; un JDK 17 ou plus récent reste requis.
- La dépendance Coil a été retirée : aucune API Coil ni image raster n’est utilisée par le code Android, ce qui réduit le code embarqué et le risque de rétention de bitmaps.
- La signature release est maintenant préparée de façon optionnelle : Gradle lit `android/keystore.properties` ou les variables `N8N_RELEASE_*`, sans jamais générer ni versionner de clé. Un modèle non secret est disponible dans [`android/keystore.properties.example`](../android/keystore.properties.example).
- Les deux offsets Compose pilotés par un état utilisent désormais l’overload lambda et les deux adaptive icons déclarent une couche monochrome.
- Les paramètres XML obsolètes de couleur des barres système ont été retirés ; l’activité utilise `enableEdgeToEdge()` et `WindowInsetsController` pour la gestion moderne des encarts et des icônes système.
- Les identifiants Credentials et les clés API sont chiffrés avec des clés dédiées Android Keystore ; les valeurs historiques en clair sont migrées à l’ouverture de la base ou au premier accès, et les logs sensibles (clé API, cookie, mot de passe et contenu des notifications) ne sont plus écrits.
- Le guide Play indique maintenant de téléverser un AAB release, jamais un artefact debug.

## Résultats de validation locale

Validation exécutée le 28 août 2026 avec Gradle 8.13, AGP 8.13.2, JDK 17 et le SDK Android API 36 :

- `./gradlew -p android test` : succès ; les suites `testDebugUnitTest` et `testReleaseUnitTest` s’exécutent sans échec.
- `./gradlew -p android lint` et `./gradlew -p android lintRelease` : succès, aucune erreur ni erreur fatale. Le rapport contient 126 avertissements existants ou informatifs : 91 ressources inutilisées, 17 recommandations `GradleDependency`, 17 recommandations `NewerVersionAvailable` et une recommandation de version AGP. Les quatre avertissements actionnables sur les offsets et les icônes ont été corrigés.
- `./gradlew -p android assembleDebug assembleRelease bundleRelease signingReport` : succès ; la tâche `minifyReleaseWithR8` s’exécute et les mappings sont générés.
- `aapt2 dump badging` sur les APK debug et release : `com.n8n.mobilemanager`, `versionCode 2`, `versionName 1.0.0`, `minSdkVersion 26`, `targetSdkVersion 36`.
- `apksigner verify` : l’APK debug est vérifié avec la clé debug locale ; l’APK release est explicitement non signé, comme attendu en l’absence de valeurs de clé release dans `keystore.properties` ou `N8N_RELEASE_*`. Le nouvel artefact expose `versionCode 2`, `compileSdk 36` et `targetSdk 36`.

Artefacts issus de cette validation :

```
android/app/build/outputs/apk/debug/app-debug.apk
android/app/build/outputs/apk/release/app-release-unsigned.apk
android/app/build/outputs/bundle/release/app-release.aab
```

Les avertissements non bloquants observés pendant Gradle concernent la version du format XML du SDK utilisée par l’outil et l’impossibilité de stripper deux bibliothèques natives, qui sont donc conservées telles quelles. Ils ne masquent aucun échec de compilation ou de test dans cette validation. Aucun test instrumenté `androidTest` n’existe dans le dépôt.

## Authentification et restauration de session

L’application possède deux mécanismes distincts :

1. L’écran principal enregistre une URL d’instance n8n et une clé API dans Room ; la clé API est chiffrée avant l’écriture avec une clé dédiée Android Keystore, puis déchiffrée uniquement en mémoire pour les appels réseau.
2. L’écran Credentials peut envoyer un identifiant et un mot de passe au endpoint de connexion n8n, puis réutiliser un cookie `n8n-auth`. Ces identifiants sont maintenant chiffrés avec une clé AES/GCM détenue par Android Keystore avant d’être conservés dans DataStore ; les anciennes valeurs en clair sont migrées au premier accès.

Les anciennes clés API en clair sont chiffrées automatiquement à l’ouverture de la base. La clé Keystore est liée à l’installation et n’est pas sauvegardée avec les données applicatives ; une restauration sur un appareil où elle n’existe plus peut donc nécessiter une nouvelle saisie de la clé API.

Le second flux est bien une connexion utilisateur, mais le dépôt ne contient pas de serveur d’application ni d’endpoint WebAuthn/FIDO capable de fournir les options de création/récupération, d’enregistrer la clé publique de restauration et de vérifier une connexion sur un nouvel appareil. L’API Restore Credentials ne peut donc pas être ajoutée correctement côté Android seul : appeler Credential Manager sans ce backend ne restaurerait pas la session et pourrait donner une fausse conformité.

À réaliser avant l’échéance Zero-Tap Sign-In :

1. Décider si n8n ou un backend intermédiaire devient le relying party de ce compte utilisateur.
2. Ajouter côté serveur les endpoints WebAuthn de création, d’enregistrement et de connexion des restore keys, distincts des passkeys utilisateur.
3. Ajouter ensuite dans l’application `androidx.credentials` version 1.5.0 ou ultérieure, générer une restore key après une connexion réussie et appeler `GetRestoreCredentialOption` au premier démarrage.
4. Après restauration, recréer la session n8n côté serveur et renvoyer le nouveau token FCM au backend si les notifications sont conservées.
5. Tester un transfert appareil-à-appareil et une restauration cloud avec chiffrement de bout en bout.

Le simple `android:allowBackup="true"` déjà présent ne constitue pas cette intégration. Aucune implémentation incomplète ni clé WebAuthn fictive n’a été ajoutée.

## Permissions, stockage et arrière-plan

Le manifeste source déclare `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `USE_BIOMETRIC`, `VIBRATE` et `RECEIVE_BOOT_COMPLETED`. Le manifeste release fusionné ajoute des éléments de bibliothèques, notamment `WAKE_LOCK`, `USE_FINGERPRINT`, la réception FCM et `android.permission.FOREGROUND_SERVICE` apportée par WorkManager. Le code applicatif n’appelle pas `setForeground` et ne déclare pas de service foreground propre ; il utilise un `CoroutineWorker` périodique. Cette permission fusionnée doit néanmoins être vérifiée dans le questionnaire Play Console si Google la demande.

Les connexions en clair sont refusées par défaut, avec une exception limitée à `localhost` et `10.0.2.2` pour le développement.

Les notifications sont préparées avec des channels et la permission runtime Android 13+. Le contrôle périodique est un `WorkManager` unique de 15 minutes ; aucun service foreground applicatif, alarme exacte ou permission de stockage n’a été détecté. Une validation sur appareils Android 15/16 reste nécessaire pour les insets edge-to-edge et le comportement réel des notifications.

## Échéances officielles

- **31 août 2026** : les nouvelles applications et les mises à jour Google Play doivent cibler Android 16 / API 36.
- **30 septembre 2026** : les packages distribués sur Google Play doivent être enregistrés par un développeur vérifié.
- **Février 2027** : début annoncé de l’application des seuils de mémoire dynamique, bitmaps et optimisation DEX ; les métriques doivent être suivies dans Android vitals.
- **Avril 2027** : le standard Zero-Tap Sign-In / Restore Credentials doit être respecté pour conserver toutes les capacités de publication et la visibilité optimale, pour les applications qui prennent en charge la connexion utilisateur.

## Étapes restantes dans la Play Console

1. Ouvrir la Play Console avec un compte ayant les droits de gestion de l’application.
2. Vérifier que l’identité du développeur est validée dans les paramètres du compte.
3. Ouvrir la page Android developer verification et contrôler l’état d’auto-enregistrement de `com.n8n.mobilemanager` ; lors du dernier contrôle, Play indiquait que toutes les applications du compte étaient déjà enregistrées.
4. Si Play demande néanmoins une preuve de possession, copier le snippet fourni dans un fichier local `android/app/src/main/assets/adi-registration.properties`, construire un APK release signé avec la clé correspondante, puis le téléverser dans le parcours prévu. Ne pas committer ce snippet sans validation de sa sensibilité.
5. Vérifier séparément les certificats de clé d’upload et de signature Play ; ne pas confondre le SHA-256 de l’AAB local avec celui délivré par Google Play App Signing.
6. Configurer ou confirmer Play App Signing et conserver la clé d’upload hors du dépôt. Pour produire localement un artefact signé, copier [`android/keystore.properties.example`](../android/keystore.properties.example) vers `android/keystore.properties` et renseigner la vraie clé, ou fournir les quatre variables `N8N_RELEASE_STORE_FILE`, `N8N_RELEASE_STORE_PASSWORD`, `N8N_RELEASE_KEY_ALIAS` et `N8N_RELEASE_KEY_PASSWORD`.
7. Suivre les vérifications automatiques et l’examen de la release `3 (1.1.0)` dans l’activité des envois Play ; l’AAB a été téléversé sur le canal fermé Alpha.
8. Ajouter au moins 12 testeurs au canal fermé et les maintenir inscrits pendant au moins 14 jours continus si l’objectif est de demander ensuite l’accès à la production ; Play indique actuellement 0 testeur inscrit.
9. Compléter les déclarations Play Console : fiche, politique de confidentialité, accès à l’application, contenu, sécurité des données et notifications selon le comportement réel de la version publiée.
10. Examiner Android vitals après installation de test : RSS/swap, bitmaps, OOM/ANR et couverture d’optimisation DEX.

## Vérifications reproductibles

À lancer depuis `android/` avec un JDK 17+ et un SDK Android contenant la plateforme API 36 :

```
./gradlew test
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleRelease
./gradlew signingReport
```

Les rapports et artefacts attendus sont notamment :

```
app/build/reports/
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
app/build/outputs/bundle/release/app-release.aab
```

Le dépôt ne contient pas de source `androidTest` ; les tests disponibles sont des tests unitaires sous `app/src/test`. Le bundle `android/app-release.aab` trouvé avant cette mise à jour est un artefact local historique et ne doit pas être considéré comme le bundle final API 36. Le nouvel AAB `app-release.aab` produit par `bundleRelease` est signé localement avec la clé d’upload configurée ; la signature Google Play reste gérée par Play App Signing.

### Mise à jour du 30 août 2026

- `./gradlew bundleRelease` : succès avec `com.n8n.mobilemanager`, `versionCode 3`, `versionName 1.1.0`, `targetSdk 36` et la tâche `signReleaseBundle` exécutée.
- L’AAB signé a été importé dans `Tests fermés → Alpha`, puis envoyé pour examen à Google Play.
- Play affiche actuellement **Modifications en cours d’examen** pour `3 (1.1.0)`.
- Play signale un seul avertissement non bloquant : les symboles de débogage natifs ne sont pas joints.
- La production publique reste verrouillée par la règle Play affichée dans le compte : au moins 12 testeurs inscrits pendant 14 jours ; le canal compte actuellement 0 testeur inscrit.

## Conclusion opérationnelle

La mise à niveau technique vers API 36 et le `versionCode 3` sont préparés, vérifiés localement et soumis à Google Play sur le canal fermé Alpha. L’examen Google et les vérifications automatiques sont encore en cours ; la production exige actuellement 12 testeurs inscrits pendant 14 jours. Le chiffrement local des clés API est en place ; il reste à tester la migration et le comportement de réauthentification sur appareil. Le flux Restore Credentials reste conditionné au backend WebAuthn décrit plus haut ; aucune intégration client incomplète n’a été livrée.

## Sources officielles

- [Exigences liées au niveau d’API cible pour les applications Google Play](https://support.google.com/googleplay/android-developer/answer/11926878?hl=fr)
- [Enregistrer les noms de packages Play](https://support.google.com/googleplay/android-developer/answer/16984799?hl=fr)
- [Android Developers Blog — Elevating app quality: Reducing memory usage and improving device migration](https://android-developers.googleblog.com/2026/08/app-quality-memory-optimization-secure-onboarding.html)
- [Android Developers — Implémenter la restauration des identifiants avec Credential Manager](https://developer.android.com/identity/sign-in/restore-credentials-implementation?hl=fr)
- [Android Developers — À propos de Restore Credentials](https://developer.android.com/identity/sign-in/restore-credentials?hl=fr)
