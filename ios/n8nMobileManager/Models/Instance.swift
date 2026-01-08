//
//  Instance.swift
//  n8nMobileManager
//

import Foundation

struct N8nInstance: Identifiable, Codable, Hashable {
    let id: UUID
    var name: String
    var baseURL: String
    var apiKey: String
    var isActive: Bool
    var lastConnected: Date?
    var status: InstanceStatus
    
    init(id: UUID = UUID(), name: String, baseURL: String, apiKey: String, isActive: Bool = true) {
        self.id = id
        self.name = name
        self.baseURL = baseURL
        self.apiKey = apiKey
        self.isActive = isActive
        self.lastConnected = nil
        self.status = .unknown
    }
    
    enum InstanceStatus: String, Codable {
        case online
        case offline
        case error
        case unknown
        
        var color: String {
            switch self {
            case .online: return "green"
            case .offline: return "gray"
            case .error: return "red"
            case .unknown: return "yellow"
            }
        }
        
        var icon: String {
            switch self {
            case .online: return "checkmark.circle.fill"
            case .offline: return "moon.circle.fill"
            case .error: return "exclamationmark.triangle.fill"
            case .unknown: return "questionmark.circle.fill"
            }
        }
    }
}

// MARK: - Instance Statistics
struct InstanceStats: Codable {
    let totalWorkflows: Int
    let activeWorkflows: Int
    let totalExecutions: Int
    let successfulExecutions: Int
    let failedExecutions: Int
    let averageExecutionTime: Double
    let lastExecutionDate: Date?
    
    var successRate: Double {
        guard totalExecutions > 0 else { return 0 }
        return Double(successfulExecutions) / Double(totalExecutions) * 100
    }
    
    static var placeholder: InstanceStats {
        InstanceStats(
            totalWorkflows: 0,
            activeWorkflows: 0,
            totalExecutions: 0,
            successfulExecutions: 0,
            failedExecutions: 0,
            averageExecutionTime: 0,
            lastExecutionDate: nil
        )
    }
}
