//
//  ContentView.swift
//  n8nMobileManager
//

import SwiftUI

struct ContentView: View {
    @EnvironmentObject var appState: AppState
    @EnvironmentObject var themeManager: ThemeManager
    @State private var selectedTab: Tab = .dashboard
    @State private var showingAddInstance = false
    
    enum Tab: String, CaseIterable {
        case dashboard = "Dashboard"
        case workflows = "Workflows"
        case executions = "Exécutions"
        case credentials = "Credentials"
        case settings = "Paramètres"
        
        var icon: String {
            switch self {
            case .dashboard: return "chart.bar.fill"
            case .workflows: return "arrow.triangle.branch"
            case .executions: return "clock.arrow.circlepath"
            case .credentials: return "key.fill"
            case .settings: return "gearshape.fill"
            }
        }
    }
    
    var body: some View {
        Group {
            if appState.currentInstance == nil {
                WelcomeView(showingAddInstance: $showingAddInstance)
            } else {
                mainTabView
            }
        }
        .sheet(isPresented: $showingAddInstance) {
            AddInstanceView().environmentObject(appState)
        }
    }
    
    private var mainTabView: some View {
        TabView(selection: $selectedTab) {
            DashboardView()
                .tabItem { Label(Tab.dashboard.rawValue, systemImage: Tab.dashboard.icon) }
                .tag(Tab.dashboard)
            WorkflowsView()
                .tabItem { Label(Tab.workflows.rawValue, systemImage: Tab.workflows.icon) }
                .tag(Tab.workflows)
            ExecutionsView()
                .tabItem { Label(Tab.executions.rawValue, systemImage: Tab.executions.icon) }
                .tag(Tab.executions)
            CredentialsView()
                .tabItem { Label(Tab.credentials.rawValue, systemImage: Tab.credentials.icon) }
                .tag(Tab.credentials)
            SettingsView()
                .tabItem { Label(Tab.settings.rawValue, systemImage: Tab.settings.icon) }
                .tag(Tab.settings)
        }
        .tint(.n8nOrange)
    }
}

struct WelcomeView: View {
    @Binding var showingAddInstance: Bool
    @State private var animateGradient = false
    
    var body: some View {
        ZStack {
            LinearGradient(colors: [Color.n8nOrange.opacity(0.3), Color.n8nPink.opacity(0.2), Color.n8nPurple.opacity(0.3)],
                startPoint: animateGradient ? .topLeading : .bottomTrailing,
                endPoint: animateGradient ? .bottomTrailing : .topLeading)
            .ignoresSafeArea()
            .onAppear {
                withAnimation(.easeInOut(duration: 3).repeatForever(autoreverses: true)) {
                    animateGradient.toggle()
                }
            }
            
            VStack(spacing: 40) {
                Spacer()
                logoSection
                featuresSection
                Spacer()
                ctaButton
            }
        }
    }
    
    private var logoSection: some View {
        VStack(spacing: 20) {
            ZStack {
                Circle().fill(Color.n8nGradient).frame(width: 120, height: 120)
                    .shadow(color: .n8nOrange.opacity(0.5), radius: 20)
                Image(systemName: "arrow.triangle.branch")
                    .font(.system(size: 50, weight: .bold)).foregroundColor(.white)
            }
            VStack(spacing: 8) {
                Text("n8n Mobile Manager").font(.system(size: 32, weight: .bold, design: .rounded))
                Text("Gérez vos automatisations en mobilité").font(.subheadline).foregroundColor(.secondary)
            }
        }
    }
    
    private var featuresSection: some View {
        VStack(spacing: 16) {
            FeatureRow(icon: "chart.bar.fill", title: "Dashboard", description: "Vue d'ensemble")
            FeatureRow(icon: "play.circle.fill", title: "Workflows", description: "Gérez vos workflows")
            FeatureRow(icon: "clock.arrow.circlepath", title: "Exécutions", description: "Historique temps réel")
            FeatureRow(icon: "key.fill", title: "Credentials", description: "Connexions sécurisées")
        }.padding(.horizontal)
    }
    
    private var ctaButton: some View {
        Button(action: { HapticFeedback.medium.trigger(); showingAddInstance = true }) {
            HStack { Image(systemName: "plus.circle.fill"); Text("Connecter une instance") }
        }
        .buttonStyle(PrimaryButtonStyle())
        .padding(.horizontal, 40).padding(.bottom, 50)
    }
}

struct FeatureRow: View {
    let icon: String; let title: String; let description: String
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon).font(.title2).foregroundStyle(Color.n8nGradient).frame(width: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.headline)
                Text(description).font(.caption).foregroundColor(.secondary)
            }
            Spacer()
        }.padding().background(.ultraThinMaterial).clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

#Preview { ContentView().environmentObject(AppState()).environmentObject(ThemeManager()) }
