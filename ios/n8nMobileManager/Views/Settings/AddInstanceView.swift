//
//  AddInstanceView.swift
//  n8nMobileManager
//

import SwiftUI

struct AddInstanceView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.dismiss) private var dismiss
    
    @State private var name = ""
    @State private var baseURL = "https://"
    @State private var apiKey = ""
    @State private var isLoading = false
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var connectionStatus: ConnectionStatus = .idle
    
    enum ConnectionStatus {
        case idle, testing, success, failed
    }
    
    var isValid: Bool {
        !name.isEmpty && !baseURL.isEmpty && baseURL.hasPrefix("http") && !apiKey.isEmpty
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color(UIColor.systemBackground).ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 24) {
                        headerSection
                        formSection
                        testConnectionButton
                        if connectionStatus == .success { addButton }
                    }
                    .padding()
                }
            }
            .navigationTitle("Nouvelle instance")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
            }
            .alert("Erreur", isPresented: $showError) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(errorMessage)
            }
        }
    }
    
    private var headerSection: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle().fill(Color.n8nGradient).frame(width: 80, height: 80)
                Image(systemName: "server.rack").font(.system(size: 35)).foregroundColor(.white)
            }
            Text("Connectez votre instance n8n").font(.headline).foregroundColor(.secondary)
        }.padding(.top)
    }
    
    private var formSection: some View {
        VStack(spacing: 16) {
            FormField(title: "Nom de l'instance", placeholder: "Production", text: $name, icon: "tag.fill")
            FormField(title: "URL de base", placeholder: "https://n8n.example.com", text: $baseURL, icon: "globe", keyboardType: .URL)
            FormField(title: "Clé API", placeholder: "n8n_api_xxx...", text: $apiKey, icon: "key.fill", isSecure: true)
            
            VStack(alignment: .leading, spacing: 8) {
                Text("Comment obtenir une clé API ?").font(.caption).foregroundColor(.secondary)
                Text("1. Allez dans Settings → n8n API\n2. Créez une nouvelle clé API\n3. Copiez la clé générée")
                    .font(.caption2).foregroundColor(.secondary)
            }.padding().background(Color(UIColor.tertiarySystemBackground)).clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }
    
    private var testConnectionButton: some View {
        Button(action: testConnection) {
            HStack {
                if isLoading {
                    ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Image(systemName: connectionStatusIcon)
                    Text(connectionStatusText)
                }
            }
        }
        .buttonStyle(connectionStatus == .success ? AnyButtonStyle(SecondaryButtonStyle()) : AnyButtonStyle(PrimaryButtonStyle()))
        .disabled(!isValid || isLoading)
    }
    
    private var addButton: some View {
        Button(action: addInstance) {
            HStack { Image(systemName: "checkmark.circle.fill"); Text("Ajouter l'instance") }
        }.buttonStyle(PrimaryButtonStyle())
    }
    
    private var connectionStatusIcon: String {
        switch connectionStatus {
        case .idle: return "network"
        case .testing: return "network"
        case .success: return "checkmark.circle.fill"
        case .failed: return "xmark.circle.fill"
        }
    }
    
    private var connectionStatusText: String {
        switch connectionStatus {
        case .idle: return "Tester la connexion"
        case .testing: return "Test en cours..."
        case .success: return "Connexion réussie !"
        case .failed: return "Réessayer"
        }
    }
    
    private func testConnection() {
        isLoading = true
        connectionStatus = .testing
        HapticFeedback.medium.trigger()
        
        let instance = N8nInstance(name: name, baseURL: baseURL.trimmingCharacters(in: .whitespacesAndNewlines), apiKey: apiKey)
        let service = N8nAPIService(instance: instance)
        
        Task {
            let status = await service.checkHealth()
            await MainActor.run {
                isLoading = false
                if status == .online {
                    connectionStatus = .success
                    HapticFeedback.success.trigger()
                } else {
                    connectionStatus = .failed
                    errorMessage = status == .error ? "Clé API invalide" : "Impossible de se connecter"
                    showError = true
                    HapticFeedback.error.trigger()
                }
            }
        }
    }
    
    private func addInstance() {
        let instance = N8nInstance(name: name, baseURL: baseURL.trimmingCharacters(in: .whitespacesAndNewlines), apiKey: apiKey)
        appState.addInstance(instance)
        HapticFeedback.success.trigger()
        dismiss()
    }
}

struct FormField: View {
    let title: String
    let placeholder: String
    @Binding var text: String
    let icon: String
    var keyboardType: UIKeyboardType = .default
    var isSecure: Bool = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.subheadline.weight(.medium))
            HStack {
                Image(systemName: icon).foregroundColor(.n8nOrange).frame(width: 24)
                if isSecure {
                    SecureField(placeholder, text: $text)
                } else {
                    TextField(placeholder, text: $text).keyboardType(keyboardType).autocapitalization(.none).autocorrectionDisabled()
                }
            }
            .padding()
            .background(Color(UIColor.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }
}

struct AnyButtonStyle: ButtonStyle {
    private let _makeBody: (Configuration) -> AnyView
    init<S: ButtonStyle>(_ style: S) { _makeBody = { AnyView(style.makeBody(configuration: $0)) } }
    func makeBody(configuration: Configuration) -> some View { _makeBody(configuration) }
}
