import Foundation
import Combine

class CrawlAPIService {
    static let shared = CrawlAPIService()
    
    private let baseURL = "http://43.160.237.122"
    private let session: URLSession
    private var cancellables = Set<AnyCancellable>()
    
    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        self.session = URLSession(configuration: config)
    }
    
    private func getAuthToken() -> String? {
        UserDefaults.standard.string(forKey: "auth_token")
    }
    
    func getCrawlTasks() -> AnyPublisher<[CrawlTask], Error> {
        let url = URL(string: "\(baseURL)/api/crawl/task/list")!
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = getAuthToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        return session.dataTaskPublisher(for: request)
            .map(\.data)
            .decode(type: APIResponse<[CrawlTask]>.self, decoder: JSONDecoder())
            .map { $0.data ?? [] }
            .eraseToAnyPublisher()
    }
    
    func createTask(_ request: CreateTaskRequest) -> AnyPublisher<CrawlTask, Error> {
        let url = URL(string: "\(baseURL)/api/crawl/task/create")!
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = getAuthToken() {
            urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        do {
            let jsonData = try JSONEncoder().encode(request)
            urlRequest.httpBody = jsonData
        } catch {
            return Fail(error: error).eraseToAnyPublisher()
        }
        
        return session.dataTaskPublisher(for: urlRequest)
            .map(\.data)
            .decode(type: APIResponse<CrawlTask>.self, decoder: JSONDecoder())
            .compactMap { $0.data }
            .eraseToAnyPublisher()
    }
    
    func getComments(taskId: Int64, 
                     page: Int = 1,
                     size: Int = 20,
                     minScore: Int? = nil,
                     level: String? = nil,
                     onlyHighIntent: Bool = false,
                     onlyWithContact: Bool = false) -> AnyPublisher<CommentFilterResult, Error> {
        var components = URLComponents(string: "\(baseURL)/api/crawl/task/\(taskId)/comments")!
        var queryItems = [
            URLQueryItem(name: "page", value: String(page)),
            URLQueryItem(name: "size", value: String(size)),
            URLQueryItem(name: "onlyHighIntent", value: String(onlyHighIntent)),
            URLQueryItem(name: "onlyWithContact", value: String(onlyWithContact))
        ]
        
        if let minScore = minScore {
            queryItems.append(URLQueryItem(name: "minScore", value: String(minScore)))
        }
        
        if let level = level {
            queryItems.append(URLQueryItem(name: "level", value: level))
        }
        
        components.queryItems = queryItems
        
        guard let url = components.url else {
            return Fail(error: URLError(.badURL)).eraseToAnyPublisher()
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        if let token = getAuthToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        return session.dataTaskPublisher(for: request)
            .map(\.data)
            .decode(type: CommentFilterResult.self, decoder: JSONDecoder())
            .eraseToAnyPublisher()
    }
    
    func analyzeComments(taskId: Int64) -> AnyPublisher<Bool, Error> {
        let url = URL(string: "\(baseURL)/api/crawl/task/\(taskId)/analyze")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = getAuthToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        return session.dataTaskPublisher(for: request)
            .map(\.data)
            .tryMap { data in
                let response = try JSONDecoder().decode(APIResponse<String>.self, from: data)
                return response.success ?? false
            }
            .eraseToAnyPublisher()
    }
    
    func generateLeads(taskId: Int64, criteria: GenerateLeadsCriteria) -> AnyPublisher<LeadResult, Error> {
        let url = URL(string: "\(baseURL)/api/crawl/task/\(taskId)/generate-leads")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = getAuthToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        do {
            let jsonData = try JSONEncoder().encode(criteria)
            request.httpBody = jsonData
        } catch {
            return Fail(error: error).eraseToAnyPublisher()
        }
        
        return session.dataTaskPublisher(for: request)
            .map(\.data)
            .decode(type: LeadResult.self, decoder: JSONDecoder())
            .eraseToAnyPublisher()
    }
    
    func sendMessage(request: SendMessageRequest) -> AnyPublisher<MessageResult, Error> {
        let url = URL(string: "\(baseURL)/api/message/send")!
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = getAuthToken() {
            urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        do {
            let jsonData = try JSONEncoder().encode(request)
            urlRequest.httpBody = jsonData
        } catch {
            return Fail(error: error).eraseToAnyPublisher()
        }
        
        return session.dataTaskPublisher(for: urlRequest)
            .map(\.data)
            .decode(type: MessageResult.self, decoder: JSONDecoder())
            .eraseToAnyPublisher()
    }
    
    func batchSendMessage(commentIds: [Int64], templateId: Int64?) -> AnyPublisher<MessageResult, Error> {
        let url = URL(string: "\(baseURL)/api/message/batch-send")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = getAuthToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        let body: [String: Any] = [
            "commentIds": commentIds,
            "templateId": templateId as Any,
            "maxConcurrent": 5,
            "intervalMs": 30000
        ]
        
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: body)
            request.httpBody = jsonData
        } catch {
            return Fail(error: error).eraseToAnyPublisher()
        }
        
        return session.dataTaskPublisher(for: request)
            .map(\.data)
            .decode(type: MessageResult.self, decoder: JSONDecoder())
            .eraseToAnyPublisher()
    }
    
    func getMessageTemplates(platformCode: String? = nil, intentLevel: String? = nil) -> AnyPublisher<[MessageTemplate], Error> {
        var components = URLComponents(string: "\(baseURL)/api/message/templates")!
        var queryItems: [URLQueryItem] = []
        
        if let platformCode = platformCode {
            queryItems.append(URLQueryItem(name: "platformCode", value: platformCode))
        }
        
        if let intentLevel = intentLevel {
            queryItems.append(URLQueryItem(name: "intentLevel", value: intentLevel))
        }
        
        components.queryItems = queryItems.isEmpty ? nil : queryItems
        
        guard let url = components.url else {
            return Fail(error: URLError(.badURL)).eraseToAnyPublisher()
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        if let token = getAuthToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        return session.dataTaskPublisher(for: request)
            .map(\.data)
            .decode(type: [MessageTemplate].self, decoder: JSONDecoder())
            .eraseToAnyPublisher()
    }
}

struct CreateTaskRequest: Codable {
    let platformCode: String
    let targetType: String
    let targetId: String
    var keywords: String?
    var maxCrawlCount: Int = 500
    var crawlIntervalSeconds: Int = 3
}

struct GenerateLeadsCriteria: Codable {
    var minScore: Int?
    var requiredLevels: [String]?
    var requireContactInfo: Bool = false
    var minFollowerCount: Int?
    var autoAssign: Bool = true
    var generateFollowUpTask: Bool = true
}

struct SendMessageRequest: Codable {
    let commentId: Int64
    var templateId: Int64?
    let content: String
}

struct CommentFilterResult: Codable {
    let totalCount: Int
    let filteredCount: Int
    let comments: [SocialComment]
    let statistics: FilterStatistics?
}

struct FilterStatistics: Codable {
    let totalCount: Int
    let highIntentCount: Int
    let withPhoneCount: Int
    let withWechatCount: Int
    let avgIntentScore: Double
    let scoreDistribution: [String: Int]?
    let levelDistribution: [String: Int]?
    let platformDistribution: [String: Int]?
}
