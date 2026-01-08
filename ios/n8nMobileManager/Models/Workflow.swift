//
//  Workflow.swift
//  n8nMobileManager
//

import Foundation

struct Workflow: Identifiable, Codable, Hashable {
    let id: String
    var name: String
    var active: Bool
    var createdAt: Date
    var updatedAt: Date
    var nodes: [WorkflowNode]
    var connections: [String: [[String: Any]]]?
    var settings: WorkflowSettings?
    var tags: [WorkflowTag]
    var staticData: String?
    
    enum CodingKeys: String, CodingKey {
        case id, name, active, createdAt, updatedAt, nodes, settings, tags, staticData
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        active = try container.decode(Bool.self, forKey: .active)
        createdAt = try container.decode(Date.self, forKey: .createdAt)
        updatedAt = try container.decode(Date.self, forKey: .updatedAt)
        nodes = try container.decodeIfPresent([WorkflowNode].self, forKey: .nodes) ?? []
        settings = try container.decodeIfPresent(WorkflowSettings.self, forKey: .settings)
        tags = try container.decodeIfPresent([WorkflowTag].self, forKey: .tags) ?? []
        staticData = try container.decodeIfPresent(String.self, forKey: .staticData)
        connections = nil
    }
    
    init(id: String, name: String, active: Bool, createdAt: Date = Date(), updatedAt: Date = Date(), nodes: [WorkflowNode] = [], tags: [WorkflowTag] = []) {
        self.id = id
        self.name = name
        self.active = active
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.nodes = nodes
        self.connections = nil
        self.settings = nil
        self.tags = tags
        self.staticData = nil
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(name, forKey: .name)
        try container.encode(active, forKey: .active)
        try container.encode(createdAt, forKey: .createdAt)
        try container.encode(updatedAt, forKey: .updatedAt)
        try container.encode(nodes, forKey: .nodes)
        try container.encodeIfPresent(settings, forKey: .settings)
        try container.encode(tags, forKey: .tags)
        try container.encodeIfPresent(staticData, forKey: .staticData)
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
    
    static func == (lhs: Workflow, rhs: Workflow) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Workflow Node
struct WorkflowNode: Identifiable, Codable, Hashable {
    let id: String
    var name: String
    var type: String
    var typeVersion: Int
    var position: [Double]
    var parameters: [String: AnyCodable]?
    var credentials: [String: NodeCredential]?
    var disabled: Bool?
    
    enum CodingKeys: String, CodingKey {
        case id, name, type, typeVersion, position, parameters, credentials, disabled
    }
    
    var displayType: String {
        type.replacingOccurrences(of: "n8n-nodes-base.", with: "")
            .replacingOccurrences(of: "n8n-nodes-", with: "")
    }
    
    var icon: String {
        switch displayType.lowercased() {
        case "webhook": return "link"
        case "httpRequest", "httprequest": return "network"
        case "slack": return "bubble.left.fill"
        case "gmail", "email": return "envelope.fill"
        case "googlesheets": return "tablecells"
        case "postgres", "mysql", "mongodb": return "cylinder.fill"
        case "code", "function": return "chevron.left.forwardslash.chevron.right"
        case "if", "switch": return "arrow.triangle.branch"
        case "set": return "square.and.pencil"
        case "merge": return "arrow.triangle.merge"
        case "schedule", "cron": return "clock.fill"
        default: return "gearshape.fill"
        }
    }
}

// MARK: - Node Credential
struct NodeCredential: Codable, Hashable {
    let id: String
    let name: String
}

// MARK: - Workflow Settings
struct WorkflowSettings: Codable, Hashable {
    var saveDataErrorExecution: String?
    var saveDataSuccessExecution: String?
    var saveManualExecutions: Bool?
    var saveExecutionProgress: Bool?
    var executionTimeout: Int?
    var errorWorkflow: String?
    var timezone: String?
}

// MARK: - Workflow Tag
struct WorkflowTag: Identifiable, Codable, Hashable {
    let id: String
    var name: String
    var createdAt: Date?
    var updatedAt: Date?
}

// MARK: - AnyCodable for dynamic parameters
struct AnyCodable: Codable, Hashable {
    let value: Any
    
    init(_ value: Any) {
        self.value = value
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let intValue = try? container.decode(Int.self) {
            value = intValue
        } else if let doubleValue = try? container.decode(Double.self) {
            value = doubleValue
        } else if let boolValue = try? container.decode(Bool.self) {
            value = boolValue
        } else if let stringValue = try? container.decode(String.self) {
            value = stringValue
        } else if let arrayValue = try? container.decode([AnyCodable].self) {
            value = arrayValue.map { $0.value }
        } else if let dictionaryValue = try? container.decode([String: AnyCodable].self) {
            value = dictionaryValue.mapValues { $0.value }
        } else {
            value = NSNull()
        }
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch value {
        case let intValue as Int:
            try container.encode(intValue)
        case let doubleValue as Double:
            try container.encode(doubleValue)
        case let boolValue as Bool:
            try container.encode(boolValue)
        case let stringValue as String:
            try container.encode(stringValue)
        case let arrayValue as [Any]:
            try container.encode(arrayValue.map { AnyCodable($0) })
        case let dictionaryValue as [String: Any]:
            try container.encode(dictionaryValue.mapValues { AnyCodable($0) })
        default:
            try container.encodeNil()
        }
    }
    
    static func == (lhs: AnyCodable, rhs: AnyCodable) -> Bool {
        String(describing: lhs.value) == String(describing: rhs.value)
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(String(describing: value))
    }
}
