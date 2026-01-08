//
//  DashboardViewModel.swift
//  n8nMobileManager
//

import Foundation
import Combine

@MainActor
class DashboardViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var instanceStatus: N8nInstance.InstanceStatus = .unknown
    @Published var stats = InstanceStats.placeholder
    @Published var workflows: [Workflow] = []
    @Published var recentExecutions: [Execution] = []
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
        
        // Check instance status
        instanceStatus = await service.checkHealth()
        
        guard instanceStatus == .online else {
            isLoading = false
            return
        }
        
        // Fetch workflows and executions in parallel
        async let workflowsTask = fetchWorkflows(service: service)
        async let executionsTask = fetchExecutions(service: service)
        
        let (fetchedWorkflows, fetchedExecutions) = await (workflowsTask, executionsTask)
        
        workflows = fetchedWorkflows
        recentExecutions = fetchedExecutions
        
        // Calculate stats
        stats = calculateStats(workflows: workflows, executions: recentExecutions)
        
        isLoading = false
    }
    
    private func fetchWorkflows(service: N8nAPIService) async -> [Workflow] {
        do {
            let response = try await service.getWorkflows(limit: 100)
            return response.data
        } catch {
            errorMessage = error.localizedDescription
            return []
        }
    }
    
    private func fetchExecutions(service: N8nAPIService) async -> [Execution] {
        do {
            let response = try await service.getExecutions(limit: 50)
            return response.data
        } catch {
            errorMessage = error.localizedDescription
            return []
        }
    }
    
    private func calculateStats(workflows: [Workflow], executions: [Execution]) -> InstanceStats {
        let activeCount = workflows.filter { $0.active }.count
        let successCount = executions.filter { $0.status == .success }.count
        let failedCount = executions.filter { $0.status == .error || $0.status == .crashed }.count
        
        let completedExecutions = executions.filter { $0.duration != nil }
        let avgTime = completedExecutions.isEmpty ? 0 : completedExecutions.compactMap { $0.duration }.reduce(0, +) / Double(completedExecutions.count)
        
        return InstanceStats(
            totalWorkflows: workflows.count,
            activeWorkflows: activeCount,
            totalExecutions: executions.count,
            successfulExecutions: successCount,
            failedExecutions: failedCount,
            averageExecutionTime: avgTime,
            lastExecutionDate: executions.first?.startedAt
        )
    }
}
