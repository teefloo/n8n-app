//
//  CredentialsView.swift
//  n8nMobileManager
//

import SwiftUI
import LocalAuthentication

struct CredentialsView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var viewModel = CredentialsViewModel()
    @State private var searchText = ""
    @State private var selectedCredential: Credential?
    @State private var isAuthenticated = false
    
    var filteredCredentials: [Credential] {
        if searchText.isEmpty { return viewModel.credentials }
        return viewModel.credentials.filter { $0.name.localizedCaseInsensitiveContains(searchText) || $0.type.localizedCaseInsensitiveContains(searchText) }
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                if !isAuthenticated {
                    authenticationRequired
                } else if viewModel.isLoading && viewModel.credentials.isEmpty {
                    ProgressView("Chargement...")
                } else if viewModel.credentials.isEmpty {
                    EmptyStateView(icon: "key.fill", message: "Aucun credential")
                } else {
                    credentialsList
                }
            }
            .background(Color(UIColor.systemGroupedBackground))
            .navigationTitle("Credentials")
            .searchable(text: $searchText, prompt: "Rechercher...")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    if isAuthenticated {
                        Button(action: { Task { await viewModel.refresh() } }) {
                            Image(systemName: "arrow.clockwise")
                        }
                    }
                }
            }
            .refreshable { if isAuthenticated { await viewModel.refresh() } }
            .task {
                if let instance = appState.currentInstance, isAuthenticated {
                    await viewModel.load(instance: instance)
                }
            }
            .sheet(item: $selectedCredential) { credential in
                CredentialDetailView(credential: credential, viewModel: viewModel)
            }
        }
    }
    
    private var authenticationRequired: some View {
        VStack(spacing: 24) {
            ZStack {
                Circle().fill(Color.n8nGradient).frame(width: 100, height: 100)
                Image(systemName: "faceid").font(.system(size: 45)).foregroundColor(.white)
            }
            VStack(spacing: 8) {
                Text("Authentification requise").font(.title2.bold())
                Text("Les credentials contiennent des données sensibles").font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
            }
            Button(action: authenticate) {
                HStack { Image(systemName: "faceid"); Text("S'authentifier") }
            }.buttonStyle(PrimaryButtonStyle()).padding(.horizontal, 40)
        }.padding()
    }
    
    private var credentialsList: some View {
        List {
            ForEach(filteredCredentials) { credential in
                CredentialListItem(credential: credential)
                    .contentShape(Rectangle())
                    .onTapGesture { selectedCredential = credential }
            }
        }.listStyle(.insetGrouped)
    }
    
    private func authenticate() {
        let context = LAContext()
        var error: NSError?
        
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: "Accéder aux credentials") { success, _ in
                DispatchQueue.main.async {
                    if success {
                        isAuthenticated = true
                        HapticFeedback.success.trigger()
                        if let instance = appState.currentInstance {
                            Task { await viewModel.load(instance: instance) }
                        }
                    } else {
                        HapticFeedback.error.trigger()
                    }
                }
            }
        } else {
            // Fallback - no biometrics available
            isAuthenticated = true
            if let instance = appState.currentInstance {
                Task { await viewModel.load(instance: instance) }
            }
        }
    }
}

struct CredentialListItem: View {
    let credential: Credential
    
    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(Color.n8nPurple.opacity(0.15)).frame(width: 44, height: 44)
                Image(systemName: credential.icon).foregroundColor(.n8nPurple)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(credential.name).font(.subheadline.weight(.medium)).lineLimit(1)
                Text(credential.displayType).font(.caption).foregroundColor(.secondary)
            }
            Spacer()
            Text(credential.updatedAt.timeAgo()).font(.caption2).foregroundColor(.secondary)
        }.padding(.vertical, 4)
    }
}

struct CredentialDetailView: View {
    let credential: Credential
    @ObservedObject var viewModel: CredentialsViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var isTesting = false
    @State private var testResult: CredentialTestResult?
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    headerCard
                    infoSection
                    actionsSection
                    if let result = testResult { testResultCard(result) }
                }.padding()
            }
            .background(Color(UIColor.systemGroupedBackground))
            .navigationTitle(credential.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Fermer") { dismiss() } } }
        }
    }
    
    private var headerCard: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle().fill(Color.n8nPurple.opacity(0.15)).frame(width: 60, height: 60)
                Image(systemName: credential.icon).font(.title2).foregroundColor(.n8nPurple)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(credential.name).font(.title3.bold())
                Text(credential.displayType).font(.subheadline).foregroundColor(.secondary)
            }
            Spacer()
        }.padding().background(Color(UIColor.secondarySystemBackground)).clipShape(RoundedRectangle(cornerRadius: 16))
    }
    
    private var infoSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Informations").font(.headline)
            InfoRow(label: "Type", value: credential.type)
            InfoRow(label: "Créé le", value: credential.createdAt.formattedDate())
            InfoRow(label: "Modifié le", value: credential.updatedAt.formattedDate())
        }.padding().background(Color(UIColor.secondarySystemBackground)).clipShape(RoundedRectangle(cornerRadius: 16))
    }
    
    private var actionsSection: some View {
        Button(action: testCredential) {
            HStack {
                if isTesting { ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white)) }
                else { Image(systemName: "checkmark.seal.fill") }
                Text("Tester la connexion")
            }
        }.buttonStyle(PrimaryButtonStyle()).disabled(isTesting)
    }
    
    private func testResultCard(_ result: CredentialTestResult) -> some View {
        HStack(spacing: 12) {
            Image(systemName: result.isSuccess ? "checkmark.circle.fill" : "xmark.circle.fill")
                .foregroundColor(result.isSuccess ? .n8nGreen : .n8nRed).font(.title2)
            VStack(alignment: .leading, spacing: 2) {
                Text(result.isSuccess ? "Connexion réussie" : "Échec de connexion").font(.subheadline.weight(.medium))
                if let message = result.message { Text(message).font(.caption).foregroundColor(.secondary) }
            }
            Spacer()
        }.padding().background((result.isSuccess ? Color.n8nGreen : Color.n8nRed).opacity(0.1)).clipShape(RoundedRectangle(cornerRadius: 12))
    }
    
    private func testCredential() {
        isTesting = true
        testResult = nil
        HapticFeedback.medium.trigger()
        Task {
            testResult = await viewModel.testCredential(id: credential.id)
            isTesting = false
            HapticFeedback.success.trigger()
        }
    }
}

struct InfoRow: View {
    let label: String; let value: String
    var body: some View {
        HStack {
            Text(label).foregroundColor(.secondary)
            Spacer()
            Text(value).font(.subheadline.weight(.medium))
        }.padding(.vertical, 4)
    }
}
