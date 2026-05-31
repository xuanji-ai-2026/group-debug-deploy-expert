//
//  APIService.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  API服务 - 网络请求统一入口
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Alamofire
import Combine

/// API服务类
/// 
/// 封装所有API请求，提供统一的请求入口、错误处理、响应解析等功能。
class APIService {
    
    // MARK: - 单例
    
    static let shared = APIService()
    
    // MARK: - 属性
    
    /// 当前租户ID
    private var currentTenantId: String? {
        UserDefaults.standard.string(forKey: "currentTenantId")
    }
    
    /// 网络请求会话
    private let session: Session
    
    /// 请求拦截器
    private let interceptor: APIRequestInterceptor
    
    // MARK: - 初始化
    
    private init() {
        // 配置拦截器
        interceptor = APIRequestInterceptor()
        
        // 配置会话
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 30
        configuration.timeoutIntervalForResource = 60
        configuration.httpAdditionalHeaders = HTTPHeaders.default.dictionary
        
        session = Session(
            configuration: configuration,
            interceptor: interceptor
        )
    }
    
    // MARK: - 公开方法
    
    /// GET请求
    /// - Parameters:
    ///   - path: 请求路径
    ///   - parameters: 请求参数
    ///   - config: 请求配置
    /// - Returns: API响应
    func get<T: Decodable>(
        _ path: String,
        parameters: Parameters? = nil,
        config: RequestConfig = .default
    ) async throws -> APIResponse<T> {
        return try await request(
            path: path,
            method: .get,
            parameters: parameters,
            config: config
        )
    }
    
    /// POST请求
    func post<T: Decodable>(
        _ path: String,
        parameters: Parameters? = nil,
        config: RequestConfig = .default
    ) async throws -> APIResponse<T> {
        return try await request(
            path: path,
            method: .post,
            parameters: parameters,
            config: config
        )
    }
    
    /// PUT请求
    func put<T: Decodable>(
        _ path: String,
        parameters: Parameters? = nil,
        config: RequestConfig = .default
    ) async throws -> APIResponse<T> {
        return try await request(
            path: path,
            method: .put,
            parameters: parameters,
            config: config
        )
    }
    
    /// DELETE请求
    func delete<T: Decodable>(
        _ path: String,
        parameters: Parameters? = nil,
        config: RequestConfig = .default
    ) async throws -> APIResponse<T> {
        return try await request(
            path: path,
            method: .delete,
            parameters: parameters,
            config: config
        )
    }
    
    /// 上传文件
    func upload<T: Decodable>(
        _ path: String,
        fileData: Data,
        fileName: String,
        mimeType: String,
        parameters: Parameters? = nil
    ) async throws -> APIResponse<T> {
        let url = buildURL(path: path)
        
        return try await withCheckedThrowingContinuation { continuation in
            session.upload(
                multipartFormData: { formData in
                    // 添加文件
                    formData.append(
                        fileData,
                        withName: "file",
                        fileName: fileName,
                        mimeType: mimeType
                    )
                    
                    // 添加其他参数
                    parameters?.forEach { key, value in
                        if let data = "\(value)".data(using: .utf8) {
                            formData.append(data, withName: key)
                        }
                    }
                },
                to: url,
                method: .post
            )
            .responseDecodable(of: APIResponse<T>.self) { response in
                switch response.result {
                case .success(let apiResponse):
                    if apiResponse.isSuccess {
                        continuation.resume(returning: apiResponse)
                    } else {
                        continuation.resume(throwing: APIException.from(response: apiResponse)!)
                    }
                case .failure(let error):
                    continuation.resume(throwing: self.mapError(error))
                }
            }
        }
    }
    
    /// 下载文件
    func download(
        _ path: String,
        parameters: Parameters? = nil,
        to destination: URL
    ) async throws -> URL {
        let url = buildURL(path: path)
        
        return try await withCheckedThrowingContinuation { continuation in
            session.download(
                url,
                method: .get,
                parameters: parameters,
                to: { _, _ in
                    return (destination, [.removePreviousFile, .createIntermediateDirectories])
                }
            )
            .response { response in
                switch response.result {
                case .success(let fileURL):
                    if let fileURL = fileURL {
                        continuation.resume(returning: fileURL)
                    } else {
                        continuation.resume(throwing: NetworkError.parseError)
                    }
                case .failure(let error):
                    continuation.resume(throwing: self.mapError(error))
                }
            }
        }
    }
    
    /// 获取仪表盘统计数据
    func fetchDashboardStatistics() async throws -> DashboardStatistics {
        let response: APIResponse<DashboardStatistics> = try await get("/data/dashboard")
        guard let data = response.data else {
            throw APIException(code: response.code, message: response.message)
        }
        return data
    }
    
    // MARK: - 私有方法
    
    /// 发送请求
    private func request<T: Decodable>(
        path: String,
        method: HTTPMethod,
        parameters: Parameters?,
        encoding: ParameterEncoding = JSONEncoding.default,
        config: RequestConfig
    ) async throws -> APIResponse<T> {
        let url = buildURL(path: path)
        
        // 设置超时
        let request = try await session.request(
            url,
            method: method,
            parameters: parameters,
            encoding: encoding,
            headers: buildHeaders(),
            requestModifier: { request in
                request.timeoutInterval = config.timeout
            }
        )
        .validate()
        .serializingDecodable(APIResponse<T>.self)
        .value
        
        // 检查响应状态
        if !request.isSuccess {
            throw APIException(
                code: request.code,
                message: request.message,
                requestId: request.requestId
            )
        }
        
        return request
    }
    
    /// 构建URL
    private func buildURL(path: String) -> String {
        let baseURL = NetworkConfig.baseURL
        return baseURL + path
    }
    
    /// 构建请求头
    private func buildHeaders() -> HTTPHeaders {
        var headers = HTTPHeaders()
        
        // Content-Type
        headers.add(.contentType("application/json"))
        
        // Accept
        headers.add(.accept("application/json"))
        
        // Authorization
        if let token = KeychainManager.shared.getToken() {
            headers.add(.authorization(bearerToken: token))
        }
        
        // Tenant ID
        if let tenantId = currentTenantId {
            headers.add(name: "X-Tenant-ID", value: tenantId)
        }
        
        // Device ID
        headers.add(name: "X-Device-ID", value: DeviceManager.shared.getDeviceIdentifier())
        
        // App Version
        if let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String {
            headers.add(name: "X-App-Version", value: version)
        }
        
        // Platform
        headers.add(name: "X-Platform", value: "iOS")
        
        return headers
    }
    
    /// 映射错误
    private func mapError(_ error: AFError) -> Error {
        switch error {
        case .sessionTaskFailed(let urlError as URLError):
            switch urlError.code {
            case .notConnectedToInternet, .networkConnectionLost:
                return NetworkError.noConnection
            case .timedOut:
                return NetworkError.timeout
            case .serverCertificateUntrusted, .secureConnectionFailed:
                return NetworkError.sslError
            default:
                return NetworkError.unknown(urlError)
            }
        case .responseSerializationFailed:
            return NetworkError.parseError
        case .responseValidationFailed(let reason):
            if case .unacceptableStatusCode(let code) = reason {
                return NetworkError.serverError(code)
            }
            return NetworkError.parseError
        default:
            return NetworkError.unknown(error)
        }
    }
}

// MARK: - 网络配置

/// 网络配置
struct NetworkConfig {
    
    /// API基础URL - 与后端对齐，Android/PC端统一
    /// 注意：后端 Spring Boot 默认无 /api 前缀，此处通过 nginx/网关映射
    static var baseURL: String {
        #if DEBUG
        return "http://43.160.237.122:8080"
        #else
        if let customURL = Bundle.main.object(forInfoDictionaryKey: "API_BASE_URL") as? String {
            return customURL
        }
        return "https://www.beijixing-ai.com"
        #endif
    }
    
    /// WebSocket URL
    static var websocketURL: String {
        #if DEBUG
        return "ws://43.160.237.122:8080/ws"
        #else
        return "wss://www.beijixing-ai.com/ws"
        #endif
    }
    
    /// 文件上传URL
    static var uploadURL: String {
        baseURL + "/upload"
    }
    
    /// 文件下载URL前缀
    static var downloadURLPrefix: String {
        baseURL + "/download"
    }
}

// MARK: - 请求拦截器

/// API请求拦截器
class APIRequestInterceptor: RequestInterceptor {
    
    /// 请求适配
    func adapt(
        _ urlRequest: URLRequest,
        for session: Session,
        completion: @escaping (Result<URLRequest, Error>) -> Void
    ) {
        // 添加公共参数等
        completion(.success(urlRequest))
    }
    
    /// 请求重试
    func retry(
        _ request: Request,
        for session: Session,
        dueTo error: Error,
        completion: @escaping (RetryResult) -> Void
    ) {
        // 根据错误类型判断是否重试
        if let networkError = error as? NetworkError, networkError.canRetry {
            // 重试次数限制
            if (request.retryCount ?? 0) < 3 {
                completion(.retryWithDelay(1.0))
            } else {
                completion(.doNotRetry)
            }
        } else {
            completion(.doNotRetry)
        }
    }
}
