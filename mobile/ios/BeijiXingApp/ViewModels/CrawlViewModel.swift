import Foundation
import Combine

class CrawlViewModel: ObservableObject {
    @Published var tasks: [CrawlTask] = []
    @Published var comments: [SocialComment] = []
    @Published var templates: [MessageTemplate] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    @Published var taskResult: Bool?
    @Published var messageResult: MessageResult?
    @Published var leadResult: LeadResult?
    
    private let apiService = CrawlAPIService.shared
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        loadTasks()
        loadTemplates()
    }
    
    func loadTasks() {
        isLoading = true
        errorMessage = nil
        
        apiService.getCrawlTasks()
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    self?.isLoading = false
                    if case .failure(let error) = completion {
                        self?.errorMessage = error.localizedDescription
                    }
                },
                receiveValue: { [weak self] tasks in
                    self?.tasks = tasks
                }
            )
            .store(in: &cancellables)
    }
    
    func createTask(_ request: CreateTaskRequest) {
        isLoading = true
        errorMessage = nil
        
        apiService.createTask(request)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    self?.isLoading = false
                    if case .failure(let error) = completion {
                        self?.errorMessage = error.localizedDescription
                        self?.taskResult = false
                    }
                },
                receiveValue: { [weak self] task in
                    self?.taskResult = true
                    self?.loadTasks()
                }
            )
            .store(in: &cancellables)
    }
    
    func loadComments(taskId: Int64,
                      page: Int = 1,
                      size: Int = 20,
                      minScore: Int? = nil,
                      level: String? = nil,
                      onlyHighIntent: Bool = false,
                      onlyWithContact: Bool = false) {
        isLoading = true
        errorMessage = nil
        
        apiService.getComments(taskId: taskId,
                               page: page,
                               size: size,
                               minScore: minScore,
                               level: level,
                               onlyHighIntent: onlyHighIntent,
                               onlyWithContact: onlyWithContact)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    self?.isLoading = false
                    if case .failure(let error) = completion {
                        self?.errorMessage = error.localizedDescription
                    }
                },
                receiveValue: { [weak self] result in
                    self?.comments = result.comments
                }
            )
            .store(in: &cancellables)
    }
    
    func analyzeComments(taskId: Int64) {
        isLoading = true
        
        apiService.analyzeComments(taskId: taskId)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    self?.isLoading = false
                    if case .failure(let error) = completion {
                        self?.errorMessage = error.localizedDescription
                    }
                },
                receiveValue: { [weak self] success in
                    if success {
                        self?.loadComments(taskId: taskId)
                    }
                }
            )
            .store(in: &cancellables)
    }
    
    func generateLeads(taskId: Int64, criteria: GenerateLeadsCriteria) {
        isLoading = true
        errorMessage = nil
        
        apiService.generateLeads(taskId: taskId, criteria: criteria)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    self?.isLoading = false
                    if case .failure(let error) = completion {
                        self?.errorMessage = error.localizedDescription
                    }
                },
                receiveValue: { [weak self] result in
                    self?.leadResult = result
                }
            )
            .store(in: &cancellables)
    }
    
    func sendMessage(request: SendMessageRequest) {
        isLoading = true
        errorMessage = nil
        
        apiService.sendMessage(request: request)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    self?.isLoading = false
                    if case .failure(let error) = completion {
                        self?.errorMessage = error.localizedDescription
                    }
                },
                receiveValue: { [weak self] result in
                    self?.messageResult = result
                }
            )
            .store(in: &cancellables)
    }
    
    func batchSendMessage(commentIds: [Int64], templateId: Int64?) {
        isLoading = true
        errorMessage = nil
        
        apiService.batchSendMessage(commentIds: commentIds, templateId: templateId)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    self?.isLoading = false
                    if case .failure(let error) = completion {
                        self?.errorMessage = error.localizedDescription
                    }
                },
                receiveValue: { [weak self] result in
                    self?.messageResult = result
                }
            )
            .store(in: &cancellables)
    }
    
    func loadTemplates(platformCode: String? = nil, intentLevel: String? = nil) {
        apiService.getMessageTemplates(platformCode: platformCode, intentLevel: intentLevel)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { _ in },
                receiveValue: { [weak self] templates in
                    self?.templates = templates
                }
            )
            .store(in: &cancellables)
    }
}
