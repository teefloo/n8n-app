//
//  n8nMobileManagerApp.swift
//  n8nMobileManager
//
//  Created with SwiftUI
//

import SwiftUI

@main
struct n8nMobileManagerApp: App {
    @StateObject private var appState = AppState()
    @StateObject private var themeManager = ThemeManager()
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .environmentObject(themeManager)
                .preferredColorScheme(themeManager.isDarkMode ? .dark : .light)
        }
    }
}

// MARK: - App State
class AppState: ObservableObject {
    @Published var isAuthenticated: Bool = false
    @Published var currentInstance: N8nInstance?
    @Published var instances: [N8nInstance] = []
    
    init() {
        loadInstances()
    }
    
    func loadInstances() {
        // Load saved instances from UserDefaults or Keychain
        if let data = UserDefaults.standard.data(forKey: "savedInstances"),
           let decoded = try? JSONDecoder().decode([N8nInstance].self, from: data) {
            instances = decoded
            currentInstance = instances.first
        }
    }
    
    func saveInstances() {
        if let encoded = try? JSONEncoder().encode(instances) {
            UserDefaults.standard.set(encoded, forKey: "savedInstances")
        }
    }
    
    func addInstance(_ instance: N8nInstance) {
        instances.append(instance)
        if currentInstance == nil {
            currentInstance = instance
        }
        saveInstances()
    }
    
    func removeInstance(_ instance: N8nInstance) {
        instances.removeAll { $0.id == instance.id }
        if currentInstance?.id == instance.id {
            currentInstance = instances.first
        }
        saveInstances()
    }
}

// MARK: - Theme Manager
class ThemeManager: ObservableObject {
    @Published var isDarkMode: Bool {
        didSet {
            UserDefaults.standard.set(isDarkMode, forKey: "isDarkMode")
        }
    }
    
    init() {
        self.isDarkMode = UserDefaults.standard.bool(forKey: "isDarkMode")
    }
}
