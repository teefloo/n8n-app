//
//  Credential.swift
//  n8nMobileManager
//

import Foundation

struct Credential: Identifiable, Codable, Hashable {
    let id: String
    var name: String
    var type: String
    var createdAt: Date
    var updatedAt: Date
    var nodesAccess: [NodeAccess]?
    var sharedWith: [SharedWith]?
    
    struct NodeAccess: Codable, Hashable {
        let nodeType: String
        let date: Date?
    }
    
    struct SharedWith: Codable, Hashable {
        let id: String
        let email: String?
    }
    
    var displayType: String {
        type.replacingOccurrences(of: "Api", with: " API")
            .replacingOccurrences(of: "OAuth2", with: " OAuth2")
            .replacingOccurrences(of: "Oauth2", with: " OAuth2")
    }
    
    var icon: String {
        let lowercased = type.lowercased()
        if lowercased.contains("slack") {
            return "bubble.left.fill"
        } else if lowercased.contains("google") || lowercased.contains("gmail") {
            return "envelope.fill"
        } else if lowercased.contains("github") {
            return "chevron.left.forwardslash.chevron.right"
        } else if lowercased.contains("database") || lowercased.contains("postgres") || lowercased.contains("mysql") || lowercased.contains("mongodb") {
            return "cylinder.fill"
        } else if lowercased.contains("aws") || lowercased.contains("s3") {
            return "cloud.fill"
        } else if lowercased.contains("http") || lowercased.contains("api") {
            return "network"
        } else if lowercased.contains("ssh") || lowercased.contains("ftp") {
            return "server.rack"
        } else if lowercased.contains("telegram") || lowercased.contains("discord") {
            return "message.fill"
        } else if lowercased.contains("twitter") || lowercased.contains("facebook") || lowercased.contains("linkedin") {
            return "person.2.fill"
        } else {
            return "key.fill"
        }
    }
}

// MARK: - Credential Type
struct CredentialType: Identifiable, Codable, Hashable {
    let id: String
    let name: String
    let displayName: String
    let description: String?
    let properties: [CredentialProperty]?
    
    struct CredentialProperty: Codable, Hashable {
        let name: String
        let displayName: String
        let type: String
        let required: Bool?
        let default_: AnyCodable?
        let description: String?
        
        enum CodingKeys: String, CodingKey {
            case name, displayName, type, required, description
            case default_ = "default"
        }
    }
}

// MARK: - Credential Detail
struct CredentialDetail: Identifiable, Codable {
    let id: String
    var name: String
    var type: String
    var data: [String: AnyCodable]?
    var createdAt: Date
    var updatedAt: Date
    
    var maskedData: [String: String] {
        guard let data = data else { return [:] }
        var masked: [String: String] = [:]
        for (key, value) in data {
            if let stringValue = value.value as? String {
                if key.lowercased().contains("password") || 
                   key.lowercased().contains("secret") ||
                   key.lowercased().contains("token") ||
                   key.lowercased().contains("key") {
                    masked[key] = String(repeating: "•", count: min(stringValue.count, 20))
                } else {
                    masked[key] = stringValue
                }
            } else {
                masked[key] = String(describing: value.value)
            }
        }
        return masked
    }
}

// MARK: - Credential Test Result
struct CredentialTestResult: Codable {
    let status: String
    let message: String?
    
    var isSuccess: Bool {
        status.lowercased() == "ok" || status.lowercased() == "success"
    }
}
