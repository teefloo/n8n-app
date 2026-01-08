//
//  CredentialsViewModel.swift
//  n8nMobileManager
//

import Foundation

@MainActor
class CredentialsViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var credentials: [Credential] = []
    @Published var errorMessage: String?
    
    private var apiService: N8nAPIService?
    private var currentInstance: N8nInstance?
    
    func load(instance: N8nInstance) async {
        currentInstance = instance
        apiService = N8nAPIService(instance: instance)
        await refresh()
    }
    
    func refresh() async {
        guard let service = apiService else { return }
        isLoading = true
        errorMessage = nil
        
        do {
            let response = try await service.getCredentials(limit: 200)
            credentials = response.data.sorted { $0.updatedAt > $1.updatedAt }
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func getCredentialDetail(id: String) async -> CredentialDetail? {
        guard let service = apiService else { return nil }
        
        do {
            return try await service.getCredential(id: id, includeData: false)
        } catch {
            errorMessage = error.localizedDescription
            return nil
        }
    }
    
    func testCredential(id: String) async -> CredentialTestResult? {
        guard let service = apiService else { return nil }
        
        do {
            return try await service.testCredential(id: id)
        } catch {
            errorMessage = error.localizedDescription
            return CredentialTestResult(status: "error", message: error.localizedDescription)
        }
    }
}
