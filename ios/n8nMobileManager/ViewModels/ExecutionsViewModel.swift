//
//  ExecutionsViewModel.swift
//  n8nMobileManager
//

import Foundation

@MainActor
class ExecutionsViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var executions: [Execution] = []
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
            let response = try await service.getExecutions(limit: 100)
            executions = response.data
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func getExecutionDetail(id: String) async -> ExecutionDetail? {
        guard let service = apiService else { return nil }
        
        do {
            return try await service.getExecution(id: id)
        } catch {
            errorMessage = error.localizedDescription
            return nil
        }
    }
    
    func retryExecution(id: String) async {
        guard let service = apiService else { return }
        
        do {
            _ = try await service.retryExecution(id: id)
            await refresh()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
    
    func deleteExecution(id: String) async {
        guard let service = apiService else { return }
        
        do {
            try await service.deleteExecution(id: id)
            executions.removeAll { $0.id == id }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
