# AGENTS.md - n8n Mobile Manager iOS

## Build Commands

### Build Project
```bash
swift build
```

### Open in Xcode
```bash
open Package.swift  # Opens in Xcode
```

### Build for iOS Simulator (via Xcode)
```bash
xcodebuild -scheme n8nMobileManager -destination 'platform=iOS Simulator,name=iPhone 15' build
```

### Run Tests
No test target currently exists. When adding tests, create a `Tests` directory and add a test target to Package.swift.

### Swift Package Manager Commands
```bash
swift package resolve      # Resolve dependencies
swift package update       # Update dependencies
swift package clean        # Clean build artifacts
```

## Project Structure

```
n8nMobileManager/
├── App/               # App entry point (@main)
├── Models/            # Data models (Codable structs)
├── ViewModels/        # Business logic (@MainActor classes)
├── Views/             # SwiftUI views
│   ├── ContentView.swift
│   ├── Dashboard/
│   ├── Workflows/
│   ├── Executions/
│   ├── Credentials/
│   └── Settings/
├── Services/          # API clients (Swift actors)
└── Utils/             # Extensions and utilities
```

## Code Style Guidelines

### File Headers
All Swift files should have a header comment:
```swift
//
//  FileName.swift
//  n8nMobileManager
//
```

### MARK Comments
Use MARK comments to organize code sections:
```swift
// MARK: - Section Name
// MARK: - Private Methods
```

### Imports
Order imports alphabetically:
```swift
import Combine
import Foundation
import SwiftUI
```

### Models
- Use `struct` for models
- Conform to `Identifiable`, `Codable`, `Hashable`
- Use `let` for immutable properties, `var` for mutable
- Implement custom CodingKeys when needed
- Use `@Published var` inside ViewModel classes

```swift
struct Workflow: Identifiable, Codable, Hashable {
    let id: String
    var name: String
    var active: Bool
    
    enum CodingKeys: String, CodingKey {
        case id, name, active
    }
}
```

### ViewModels
- Annotate with `@MainActor`
- Inherit from `ObservableObject`
- Use `@Published` for state properties
- Use `async`/`await` for network calls

```swift
@MainActor
class WorkflowsViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var workflows: [Workflow] = []
    @Published var errorMessage: String?
    
    private var apiService: N8nAPIService?
    
    func refresh() async {
        // Implementation
    }
}
```

### Views
- Use `@StateObject` for creating ViewModels
- Use `@EnvironmentObject` for shared state
- Use `@State` for local view state
- Extract complex views into private computed properties
- Use `.task` modifier for async work on appear

```swift
struct WorkflowsView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var viewModel = WorkflowsViewModel()
    @State private var searchText = ""
    
    var body: some View {
        // View content
    }
    
    private var emptyState: some View {
        // Extracted view
    }
}
```

### Services (API Layer)
- Use `actor` for thread safety
- Create custom `Error` enum conforming to `LocalizedError`
- Use `async`/`await` for all network methods

```swift
enum APIError: LocalizedError {
    case invalidURL
    case networkError(Error)
    case unauthorized
    
    var errorDescription: String? {
        switch self {
        case .invalidURL: return "URL invalide"
        case .networkError(let error): return error.localizedDescription
        case .unauthorized: return "Non autorisé"
        }
    }
}

actor N8nAPIService {
    private let session: URLSession
    
    func getWorkflows() async throws -> [Workflow] {
        // Implementation
    }
}
```

### Naming Conventions
- **Types**: PascalCase (`Workflow`, `N8nInstance`)
- **Variables/Functions**: camelCase (`workflows`, `loadInstance`)
- **Constants**: PascalCase for static colors, camelCase otherwise
- **Enums**: PascalCase cases with camelCase raw values
- **Files**: Match the primary type name

### Color Palette
Use n8n brand colors defined in `Extensions.swift`:
```swift
Color.n8nOrange    // #FF6D00 - Primary
Color.n8nPink      // #FF4070 - Accent
Color.n8nPurple    // #8640FF - Secondary
Color.n8nGreen     // #10B981 - Success
Color.n8nRed       // #EF4444 - Error
Color.n8nBlue      // #409EFF - Info
Color.n8nYellow    // #F59E0B - Warning
```

### Button Styles
Use custom button styles from `Extensions.swift`:
```swift
PrimaryButtonStyle()    // Gradient orange fill
SecondaryButtonStyle()  // Orange outline
IconButtonStyle(color: .n8nOrange)  // Circular icon
```

### Error Handling
- Use `APIError` enum for network errors
- Display errors via `@Published var errorMessage: String?`
- Use `do-catch` blocks in ViewModels

### Localization
- UI strings are currently in French
- Use French for user-facing messages
- Error messages should be user-friendly

### SwiftUI Patterns
- Use `List` with `.listStyle(.insetGrouped)`
- Use `NavigationStack` for navigation
- Use `.sheet(item:)` for detail views
- Use `.refreshable` for pull-to-refresh
- Use `.searchable` for search functionality
- Apply `cardStyle()` or `glassmorphism()` modifiers for cards

### Haptic Feedback
```swift
HapticFeedback.medium.trigger()   // On actions
HapticFeedback.success.trigger()  // On success
HapticFeedback.error.trigger()    // On errors
```

### Date Formatting
Use extension methods:
```swift
date.timeAgo()          // "il y a 2h"
date.formattedDateTime() // "15 jan 2024 à 14:30"
date.formattedDate()     // "15 jan 2024"
```

### Async Patterns
```swift
// In View
.task { 
    await viewModel.load() 
}

// In ViewModel
func load() async {
    isLoading = true
    defer { isLoading = false }
    do {
        let result = try await apiService.fetch()
        self.items = result
    } catch {
        self.errorMessage = error.localizedDescription
    }
}
```

## Platform Requirements
- iOS 17.0+
- macOS 14.0+
- Swift 5.9+
- Xcode 15+

## Dependencies
No external dependencies. Uses only Apple frameworks:
- SwiftUI
- Combine
- Charts
- LocalAuthentication
