//
//  WorkflowsViewModel.swift
//  n8nMobileManager
//

import Foundation

@MainActor
class WorkflowsViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var workflows: [Workflow] = []
    @Published var errorMessage: String?
    @Published var successMessage: String?
    
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
            let response = try await service.getWorkflows(limit: 200)
            workflows = response.data.sorted { $0.updatedAt > $1.updatedAt }
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func activateWorkflow(id: String) async {
        guard let service = apiService else { return }
        
        do {
            let updated = try await service.activateWorkflow(id: id)
            if let index = workflows.firstIndex(where: { $0.id == id }) {
                workflows[index] = updated
            }
            successMessage = "Workflow activé"
        } catch {
            errorMessage = error.localizedDescription
        }
    }
    
    func deactivateWorkflow(id: String) async {
        guard let service = apiService else { return }
        
        do {
            let updated = try await service.deactivateWorkflow(id: id)
            if let index = workflows.firstIndex(where: { $0.id == id }) {
                workflows[index] = updated
            }
            successMessage = "Workflow désactivé"
        } catch {
            errorMessage = error.localizedDescription
        }
    }
    
    func executeWorkflow(id: String) async {
        guard let service = apiService else { return }
        
        do {
            _ = try await service.executeWorkflow(id: id)
            successMessage = "Workflow exécuté"
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
