# n8n Mobile Manager - Agent Guidelines

## Project Overview

Native Android app for managing n8n workflows. Built with Kotlin, Jetpack Compose, and Material 3.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (minified, ProGuard enabled)
./gradlew assembleRelease

# Run all unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.n8n.mobilemanager.data.repository.N8nRepositoryTest"

# Run a single test method
./gradlew test --tests "com.n8n.mobilemanager.data.repository.N8nRepositoryTest.testConnection returns success when API is healthy"

# Lint check
./gradlew lint

# Clean build
./gradlew clean

# Build and install on device
./gradlew installDebug
```

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM + Repository Pattern |
| DI | Hilt |
| Network | Retrofit, OkHttp, Gson |
| Database | Room, DataStore |
| Async | Kotlin Coroutines, Flow |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |

## Code Style

### Imports

```kotlin
// Order: Android → AndroidX → Third-party → Kotlin/Coroutines → Java → Project
import android.util.Log
import androidx.lifecycle.ViewModel
import com.n8n.mobilemanager.data.model.*
import com.n8n.mobilemanager.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
```

- Use wildcard imports for model packages: `import com.n8n.mobilemanager.data.model.*`
- Alphabetical ordering within each group

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `DashboardViewModel`, `N8nRepository` |
| Functions | camelCase | `loadData()`, `testConnection()` |
| Properties | camelCase | `uiState`, `isOnline` |
| Constants | SCREAMING_SNAKE_CASE | `CACHE_DURATION_MS`, `DATABASE_NAME` |
| Composables | PascalCase | `DashboardScreen()`, `StatCard()` |
| Data classes | PascalCase | `N8nInstance`, `Execution` |
| Private TAG | `private const val TAG = "ClassName"` |

### File Structure

```kotlin
package com.n8n.mobilemanager.ui.screens.dashboard

// Imports grouped (see above)

private const val TAG = "DashboardViewModel"

// Enums and data classes at file level
enum class StatsPeriod(...) { ... }

data class DashboardUiState(...)

// Main class
@HiltViewModel
class DashboardViewModel @Inject constructor(...) : ViewModel() {
    
    // ==================== Properties ====================
    
    private val _uiState = MutableStateFlow(...)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    // ==================== Public Methods ====================
    
    fun refresh() { ... }
    
    // ==================== Private Methods ====================
    
    private fun loadData() { ... }
}
```

### Section Comments

Use section separators for logical grouping:

```kotlin
// ==================== Instances ====================
// ==================== Workflows ====================
// ==================== Executions ====================
// ==================== Private Methods ====================
```

### Error Handling

Use `Result<T>` for repository methods:

```kotlin
suspend fun getWorkflows(): Result<List<Workflow>> {
    return withContext(Dispatchers.IO) {
        try {
            // API call
            Result.success(workflows)
        } catch (e: Exception) {
            Log.e(TAG, "getWorkflows: Error", e)
            Result.failure(e)
        }
    }
}
```

### ViewModels

```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val repository: N8nRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExampleUiState())
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()
    
    init {
        observeActiveInstance()
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Load data
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
```

### Composables

```kotlin
@Composable
fun ExampleScreen(
    viewModel: ExampleViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = { /* ... */ }
    ) { paddingValues ->
        // Content
    }
}
```

### Data Classes

```kotlin
@Entity(tableName = "instances")
data class N8nInstance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val isActive: Boolean = false
)
```

### KDoc Comments

```kotlin
/**
 * Repository principal pour accéder aux données n8n
 */
@Singleton
class N8nRepository @Inject constructor(...) {
    
    /**
     * Récupère les workflows actifs
     * @param activeOnly Filtre pour ne retourner que les workflows actifs
     * @return Result contenant la liste des workflows ou une erreur
     */
    suspend fun getWorkflows(activeOnly: Boolean? = null): Result<List<Workflow>>
}
```

## Project Structure

```
app/src/main/java/com/n8n/mobilemanager/
├── data/
│   ├── local/          # Room Database & DataStore
│   ├── model/          # Data models (Models.kt)
│   ├── remote/         # Retrofit API & DTOs (Dtos.kt)
│   └── repository/     # Repository pattern
├── di/                 # Hilt modules (AppModule.kt)
├── service/            # Services (Firebase Messaging)
├── ui/
│   ├── components/     # Reusable Composables
│   ├── navigation/     # Compose Navigation
│   ├── screens/        # Feature screens with ViewModels
│   └── theme/          # Material 3 Theme
├── utils/              # Utility classes
└── worker/             # WorkManager workers
```

## Testing

- Framework: JUnit 4
- Mocking: MockK (`mockk()`, `coEvery`, `coVerify`)
- Coroutines: `kotlinx-coroutines-test`, `runTest`
- Flows: Turbine for testing Flow emissions

```kotlin
@Test
fun `testConnection returns success when API is healthy`() = runTest {
    // Given
    coEvery { apiService.healthCheck() } returns Response.success(mapOf("status" to "ok"))
    
    // When
    val result = repository.testConnection(instance)
    
    // Then
    assertTrue(result.isSuccess)
    coVerify { instanceDao.updateLastConnected(instance.id, any()) }
}
```

## Logging

```kotlin
private const val TAG = "ClassName"

Log.d(TAG, "methodName: Message with data=$data")
Log.e(TAG, "methodName: Error occurred", exception)
Log.w(TAG, "methodName: Warning message")
```

## Architecture Notes

- **MVVM**: ViewModel exposes `StateFlow<UiState>`, UI collects via `collectAsState()`
- **Repository**: Single source of truth, wraps API and local storage
- **Hilt**: Constructor injection with `@Inject`, modules in `di/` package
- **Coroutines**: Use `viewModelScope` for ViewModels, `withContext(Dispatchers.IO)` for IO operations
- **Result pattern**: Wrap API responses in `Result<T>` for structured error handling

## Common Patterns

### State Update Pattern
```kotlin
_uiState.update { it.copy(
    isLoading = false,
    data = newData,
    error = null
) }
```

### Parallel Loading
```kotlin
viewModelScope.launch {
    val deferred1 = async { repository.getWorkflows() }
    val deferred2 = async { repository.getExecutions() }
    
    val result1 = deferred1.await()
    val result2 = deferred2.await()
}
```

### Repository Helper
```kotlin
private suspend fun <T> withApiService(
    block: suspend (N8nApiService) -> Result<T>
): Result<T> {
    return withContext(Dispatchers.IO) {
        try {
            val apiService = apiServiceFactory.create(instance)
            block(apiService)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Pre-commit Checklist

1. Run `./gradlew test` - All tests must pass
2. Run `./gradlew lint` - No critical lint errors
3. Check for TODO/FIXME comments
4. Verify ProGuard rules for new models (if adding)
