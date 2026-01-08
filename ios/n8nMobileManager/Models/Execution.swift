//
//  Execution.swift
//  n8nMobileManager
//

import Foundation

struct Execution: Identifiable, Codable, Hashable {
    let id: String
    let workflowId: String
    let workflowName: String?
    let mode: ExecutionMode
    let status: ExecutionStatus
    let startedAt: Date
    let stoppedAt: Date?
    let finished: Bool
    let retryOf: String?
    let retrySuccessId: String?
    
    var duration: TimeInterval? {
        guard let stoppedAt = stoppedAt else { return nil }
        return stoppedAt.timeIntervalSince(startedAt)
    }
    
    var formattedDuration: String {
        guard let duration = duration else { return "En cours..." }
        if duration < 1 {
            return String(format: "%.0f ms", duration * 1000)
        } else if duration < 60 {
            return String(format: "%.1f s", duration)
        } else if duration < 3600 {
            let minutes = Int(duration) / 60
            let seconds = Int(duration) % 60
            return "\(minutes)m \(seconds)s"
        } else {
            let hours = Int(duration) / 3600
            let minutes = (Int(duration) % 3600) / 60
            return "\(hours)h \(minutes)m"
        }
    }
}

// MARK: - Execution Mode
enum ExecutionMode: String, Codable {
    case manual
    case trigger
    case webhook
    case retry
    case integrated
    case cli
    case evaluation
    case error
    
    var displayName: String {
        switch self {
        case .manual: return "Manuel"
        case .trigger: return "Trigger"
        case .webhook: return "Webhook"
        case .retry: return "Retry"
        case .integrated: return "Intégré"
        case .cli: return "CLI"
        case .evaluation: return "Évaluation"
        case .error: return "Erreur"
        }
    }
    
    var icon: String {
        switch self {
        case .manual: return "hand.tap.fill"
        case .trigger: return "bolt.fill"
        case .webhook: return "link"
        case .retry: return "arrow.clockwise"
        case .integrated: return "puzzlepiece.fill"
        case .cli: return "terminal.fill"
        case .evaluation: return "checkmark.seal.fill"
        case .error: return "exclamationmark.triangle.fill"
        }
    }
}

// MARK: - Execution Status
enum ExecutionStatus: String, Codable {
    case success
    case error
    case running
    case waiting
    case canceled
    case crashed
    case new
    case unknown
    
    var displayName: String {
        switch self {
        case .success: return "Succès"
        case .error: return "Erreur"
        case .running: return "En cours"
        case .waiting: return "En attente"
        case .canceled: return "Annulé"
        case .crashed: return "Crash"
        case .new: return "Nouveau"
        case .unknown: return "Inconnu"
        }
    }
    
    var color: String {
        switch self {
        case .success: return "green"
        case .error, .crashed: return "red"
        case .running: return "blue"
        case .waiting: return "orange"
        case .canceled: return "gray"
        case .new: return "purple"
        case .unknown: return "secondary"
        }
    }
    
    var icon: String {
        switch self {
        case .success: return "checkmark.circle.fill"
        case .error: return "xmark.circle.fill"
        case .running: return "play.circle.fill"
        case .waiting: return "clock.fill"
        case .canceled: return "stop.circle.fill"
        case .crashed: return "exclamationmark.triangle.fill"
        case .new: return "sparkle"
        case .unknown: return "questionmark.circle.fill"
        }
    }
}

// MARK: - Execution Detail
struct ExecutionDetail: Identifiable, Codable {
    let id: String
    let workflowId: String
    let workflowName: String?
    let mode: ExecutionMode
    let status: ExecutionStatus
    let startedAt: Date
    let stoppedAt: Date?
    let finished: Bool
    let data: ExecutionData?
    
    struct ExecutionData: Codable {
        let resultData: ResultData?
        let executionData: [String: Any]?
        
        struct ResultData: Codable {
            let runData: [String: [NodeExecutionData]]?
            let error: ExecutionError?
            
            struct NodeExecutionData: Codable {
                let startTime: Double?
                let executionTime: Double?
                let data: NodeData?
                let error: ExecutionError?
                
                struct NodeData: Codable {
                    let main: [[OutputItem]]?
                    
                    struct OutputItem: Codable {
                        let json: [String: AnyCodable]?
                        let binary: [String: BinaryData]?
                        
                        struct BinaryData: Codable {
                            let mimeType: String?
                            let fileName: String?
                            let fileSize: Int?
                        }
                    }
                }
            }
            
            struct ExecutionError: Codable {
                let message: String?
                let description: String?
                let stack: String?
                let node: String?
            }
        }
        
        enum CodingKeys: String, CodingKey {
            case resultData
        }
        
        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            resultData = try container.decodeIfPresent(ResultData.self, forKey: .resultData)
            executionData = nil
        }
        
        func encode(to encoder: Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)
            try container.encodeIfPresent(resultData, forKey: .resultData)
        }
    }
}

// MARK: - Execution List Response
struct ExecutionListResponse: Codable {
    let data: [Execution]
    let nextCursor: String?
}
