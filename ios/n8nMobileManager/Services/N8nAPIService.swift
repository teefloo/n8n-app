//
//  N8nAPIService.swift
//  n8nMobileManager
//

import Foundation
import Combine

// MARK: - API Error
enum APIError: LocalizedError {
    case invalidURL
    case invalidResponse
    case networkError(Error)
    case decodingError(Error)
    case unauthorized
    case serverError(Int, String?)
    case noData
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "URL invalide"
        case .invalidResponse:
            return "Réponse invalide du serveur"
        case .networkError(let error):
            return "Erreur réseau: \(error.localizedDescription)"
        case .decodingError(let error):
            return "Erreur de décodage: \(error.localizedDescription)"
        case .unauthorized:
            return "Non autorisé - Vérifiez votre clé API"
        case .serverError(let code, let message):
            return "Erreur serveur (\(code)): \(message ?? "Inconnu")"
        case .noData:
            return "Aucune donnée reçue"
        }
    }
}

// MARK: - API Service
actor N8nAPIService {
    private let instance: N8nInstance
    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder
    
    init(instance: N8nInstance) {
        self.instance = instance
        
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        self.session = URLSession(configuration: config)
        
        self.decoder = JSONDecoder()
        self.decoder.dateDecodingStrategy = .custom { decoder in
            let container = try decoder.singleValueContainer()
            let dateString = try container.decode(String.self)
            
            // Try ISO8601 with fractional seconds
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            if let date = formatter.date(from: dateString) {
                return date
            }
            
            // Try ISO8601 without fractional seconds
            formatter.formatOptions = [.withInternetDateTime]
            if let date = formatter.date(from: dateString) {
                return date
            }
            
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Cannot decode date: \(dateString)")
        }
        
        self.encoder = JSONEncoder()
        self.encoder.dateEncodingStrategy = .iso8601
    }
    
    // MARK: - Generic Request
    private func request<T: Decodable>(_ endpoint: String, method: String = "GET", body: Data? = nil) async throws -> T {
        guard var urlComponents = URLComponents(string: instance.baseURL) else {
            throw APIError.invalidURL
        }
        
        urlComponents.path += endpoint
        
        guard let url = urlComponents.url else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(instance.apiKey, forHTTPHeaderField: "X-N8N-API-KEY")
        request.httpBody = body
        
        do {
            let (data, response) = try await session.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.invalidResponse
            }
            
            switch httpResponse.statusCode {
            case 200...299:
                do {
                    return try decoder.decode(T.self, from: data)
                } catch {
                    throw APIError.decodingError(error)
                }
            case 401:
                throw APIError.unauthorized
            default:
                let message = String(data: data, encoding: .utf8)
                throw APIError.serverError(httpResponse.statusCode, message)
            }
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.networkError(error)
        }
    }
    
    private func requestNoResponse(_ endpoint: String, method: String = "GET", body: Data? = nil) async throws {
        guard var urlComponents = URLComponents(string: instance.baseURL) else {
            throw APIError.invalidURL
        }
        
        urlComponents.path += endpoint
        
        guard let url = urlComponents.url else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(instance.apiKey, forHTTPHeaderField: "X-N8N-API-KEY")
        request.httpBody = body
        
        do {
            let (_, response) = try await session.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.invalidResponse
            }
            
            switch httpResponse.statusCode {
            case 200...299:
                return
            case 401:
                throw APIError.unauthorized
            default:
                throw APIError.serverError(httpResponse.statusCode, nil)
            }
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.networkError(error)
        }
    }
    
    // MARK: - Health Check
    func checkHealth() async -> N8nInstance.InstanceStatus {
        do {
            let _: [String: AnyCodable] = try await request("/api/v1/workflows?limit=1")
            return .online
        } catch APIError.unauthorized {
            return .error
        } catch {
            return .offline
        }
    }
    
    // MARK: - Workflows
    func getWorkflows(limit: Int = 100, cursor: String? = nil, active: Bool? = nil) async throws -> WorkflowListResponse {
        var endpoint = "/api/v1/workflows?limit=\(limit)"
        if let cursor = cursor {
            endpoint += "&cursor=\(cursor)"
        }
        if let active = active {
            endpoint += "&active=\(active)"
        }
        return try await request(endpoint)
    }
    
    func getWorkflow(id: String) async throws -> Workflow {
        return try await request("/api/v1/workflows/\(id)")
    }
    
    func activateWorkflow(id: String) async throws -> Workflow {
        return try await request("/api/v1/workflows/\(id)/activate", method: "POST")
    }
    
    func deactivateWorkflow(id: String) async throws -> Workflow {
        return try await request("/api/v1/workflows/\(id)/deactivate", method: "POST")
    }
    
    func executeWorkflow(id: String) async throws -> ExecutionResponse {
        return try await request("/api/v1/workflows/\(id)/run", method: "POST")
    }
    
    func updateWorkflow(id: String, workflow: Workflow) async throws -> Workflow {
        let body = try encoder.encode(workflow)
        return try await request("/api/v1/workflows/\(id)", method: "PUT", body: body)
    }
    
    // MARK: - Executions
    func getExecutions(workflowId: String? = nil, status: ExecutionStatus? = nil, limit: Int = 50, cursor: String? = nil) async throws -> ExecutionListResponse {
        var endpoint = "/api/v1/executions?limit=\(limit)"
        if let workflowId = workflowId {
            endpoint += "&workflowId=\(workflowId)"
        }
        if let status = status {
            endpoint += "&status=\(status.rawValue)"
        }
        if let cursor = cursor {
            endpoint += "&cursor=\(cursor)"
        }
        return try await request(endpoint)
    }
    
    func getExecution(id: String) async throws -> ExecutionDetail {
        return try await request("/api/v1/executions/\(id)")
    }
    
    func retryExecution(id: String) async throws -> ExecutionResponse {
        return try await request("/api/v1/executions/\(id)/retry", method: "POST")
    }
    
    func deleteExecution(id: String) async throws {
        try await requestNoResponse("/api/v1/executions/\(id)", method: "DELETE")
    }
    
    // MARK: - Credentials
    func getCredentials(limit: Int = 100) async throws -> CredentialListResponse {
        return try await request("/api/v1/credentials?limit=\(limit)")
    }
    
    func getCredential(id: String, includeData: Bool = false) async throws -> CredentialDetail {
        var endpoint = "/api/v1/credentials/\(id)"
        if includeData {
            endpoint += "?includeData=true"
        }
        return try await request(endpoint)
    }
    
    func testCredential(id: String) async throws -> CredentialTestResult {
        return try await request("/api/v1/credentials/\(id)/test", method: "POST")
    }
    
    func updateCredential(id: String, credential: CredentialDetail) async throws -> CredentialDetail {
        let body = try encoder.encode(credential)
        return try await request("/api/v1/credentials/\(id)", method: "PATCH", body: body)
    }
    
    // MARK: - Tags
    func getTags() async throws -> TagListResponse {
        return try await request("/api/v1/tags")
    }
}

// MARK: - Response Types
struct WorkflowListResponse: Codable {
    let data: [Workflow]
    let nextCursor: String?
}

struct CredentialListResponse: Codable {
    let data: [Credential]
    let nextCursor: String?
}

struct TagListResponse: Codable {
    let data: [WorkflowTag]
}

struct ExecutionResponse: Codable {
    let data: ExecutionResponseData
    
    struct ExecutionResponseData: Codable {
        let executionId: String?
    }
}
