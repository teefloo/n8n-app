@echo off
echo Arrêt de Gradle et nettoyage forcé du build...

:: Arrête le daemon Gradle pour libérer les verrous sur les fichiers
if exist "gradlew.bat" (
    call gradlew.bat --stop
)

:: Supprime le dossier build de l'application
if exist "app\build" (
    echo Suppression de app\build...
    rmdir /s /q "app\build"
    if exist "app\build" (
        echo [ERREUR] Impossible de supprimer completement app\build. Verifiez qu'aucun processus n'utilise ces fichiers.
    ) else (
        echo [OK] app\build supprime avec succes.
    )
) else (
    echo [INFO] Le dossier app\build n'existe pas deja.
)

:: Supprime le dossier build racine s'il existe
if exist "build" (
    echo Suppression du build racine...
    rmdir /s /q "build"
)

:: Supprime le dossier .gradle s'il existe
if exist ".gradle" (
    echo Suppression du dossier .gradle...
    rmdir /s /q ".gradle"
)

:: Supprime le dossier .idea s'il existe
if exist ".idea" (
    echo Suppression du dossier .idea...
    rmdir /s /q ".idea"
)

echo.
echo Nettoyage termine. Vous pouvez relancer le build dans Android Studio.
pause
