//
//  SettingsView.swift
//  n8nMobileManager
//

import SwiftUI
import LocalAuthentication

struct SettingsView: View {
    @EnvironmentObject var appState: AppState
    @EnvironmentObject var themeManager: ThemeManager
    @State private var showingAddInstance = false
    @State private var showingDeleteAlert = false
    @State private var instanceToDelete: N8nInstance?
    @State private var requireBiometrics = UserDefaults.standard.bool(forKey: "requireBiometrics")
    
    var body: some View {
        NavigationStack {
            List {
                instancesSection
                appearanceSection
                securitySection
                aboutSection
            }
            .navigationTitle("Paramètres")
            .sheet(isPresented: $showingAddInstance) {
                AddInstanceView().environmentObject(appState)
            }
            .alert("Supprimer l'instance ?", isPresented: $showingDeleteAlert) {
                Button("Annuler", role: .cancel) { }
                Button("Supprimer", role: .destructive) {
                    if let instance = instanceToDelete {
                        appState.removeInstance(instance)
                    }
                }
            } message: {
                Text("Cette action est irréversible.")
            }
        }
    }
    
    private var instancesSection: some View {
        Section {
            ForEach(appState.instances) { instance in
                InstanceRow(instance: instance, isSelected: appState.currentInstance?.id == instance.id) {
                    appState.currentInstance = instance
                    HapticFeedback.selection.trigger()
                }
                .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                    Button(role: .destructive) {
                        instanceToDelete = instance
                        showingDeleteAlert = true
                    } label: { Label("Supprimer", systemImage: "trash") }
                }
            }
            Button(action: { showingAddInstance = true }) {
                Label("Ajouter une instance", systemImage: "plus.circle.fill").foregroundColor(.n8nOrange)
            }
        } header: { Text("Instances") }
    }
    
    private var appearanceSection: some View {
        Section {
            Toggle(isOn: $themeManager.isDarkMode) {
                Label("Mode sombre", systemImage: "moon.fill")
            }.tint(.n8nOrange)
        } header: { Text("Apparence") }
    }
    
    private var securitySection: some View {
        Section {
            Toggle(isOn: $requireBiometrics) {
                Label("Authentification biométrique", systemImage: "faceid")
            }
            .tint(.n8nOrange)
            .onChange(of: requireBiometrics) { _, newValue in
                UserDefaults.standard.set(newValue, forKey: "requireBiometrics")
            }
        } header: { Text("Sécurité") } footer: { Text("Requiert Face ID ou Touch ID pour accéder aux credentials") }
    }
    
    private var aboutSection: some View {
        Section {
            HStack {
                Text("Version")
                Spacer()
                Text("1.0.0").foregroundColor(.secondary)
            }
            Link(destination: URL(string: "https://n8n.io")!) {
                HStack {
                    Label("Site officiel n8n", systemImage: "globe")
                    Spacer()
                    Image(systemName: "arrow.up.right").font(.caption).foregroundColor(.secondary)
                }
            }
            Link(destination: URL(string: "https://docs.n8n.io/api/")!) {
                HStack {
                    Label("Documentation API", systemImage: "doc.text")
                    Spacer()
                    Image(systemName: "arrow.up.right").font(.caption).foregroundColor(.secondary)
                }
            }
        } header: { Text("À propos") }
    }
}

struct InstanceRow: View {
    let instance: N8nInstance
    let isSelected: Bool
    let onSelect: () -> Void
    
    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: 12) {
                ZStack {
                    Circle().fill(isSelected ? Color.n8nOrange : Color.gray.opacity(0.2)).frame(width: 44, height: 44)
                    Image(systemName: "server.rack").foregroundColor(isSelected ? .white : .gray)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(instance.name).font(.subheadline.weight(.medium)).foregroundColor(.primary)
                    Text(instance.baseURL).font(.caption).foregroundColor(.secondary).lineLimit(1)
                }
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill").foregroundColor(.n8nOrange)
                }
            }
        }
    }
}
