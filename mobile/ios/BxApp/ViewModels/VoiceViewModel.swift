//
//  VoiceViewModel.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  语音视图模型
//
//  Created by Lin Feng (EMP-IOS-003) on 2024-01-20
//  Copyright © 2024 北极星AI. All rights reserved.
//

import Foundation
import Speech
import AVFoundation
import Combine

/// 语音视图模型
///
/// 负责管理语音识别、语音转文字、快捷指令执行等功能。
@MainActor
class VoiceViewModel: ObservableObject {
    
    // MARK: - Published属性 - 状态
    
    /// 是否正在录音
    @Published var isRecording: Bool = false
    
    /// 识别到的文本
    @Published var recognizedText: String = ""
    
    /// AI响应文本
    @Published var responseText: String? = nil
    
    /// 当前状态文本
    @Published var statusText: String = "就绪"
    
    /// 音频音量级别 (0.0 - 1.0)
    @Published var audioLevel: CGFloat = 0.0
    
    /// 是否显示权限警告
    @Published var showPermissionAlert: Bool = false
    
    /// 权限警告信息
    @Published var permissionAlertMessage: String = ""
    
    /// 是否显示错误
    @Published var showError: Bool = false
    
    /// 错误信息
    @Published var errorMessage: String? = nil
    
    // MARK: - Published属性 - 设置
    
    /// 是否自动识别
    @Published var autoRecognition: Bool {
        didSet {
            UserDefaults.standard.set(autoRecognition, forKey: "voiceAutoRecognition")
        }
    }
    
    /// 是否显示部分结果
    @Published var showPartialResults: Bool {
        didSet {
            UserDefaults.standard.set(showPartialResults, forKey: "voiceShowPartialResults")
        }
    }
    
    /// 识别语言
    @Published var recognitionLocale: String {
        didSet {
            UserDefaults.standard.set(recognitionLocale, forKey: "voiceRecognitionLocale")
            updateRecognitionRequest()
        }
    }
    
    /// 识别灵敏度
    @Published var sensitivity: Double {
        didSet {
            UserDefaults.standard.set(sensitivity, forKey: "voiceSensitivity")
        }
    }
    
    /// 识别引擎名称
    @Published var recognitionEngine: String = "Apple Speech"
    
    // MARK: - 私有属性
    
    /// 语音识别器
    private var speechRecognizer: SFSpeechRecognizer?
    
    /// 语音识别请求
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    
    /// 语音识别任务
    private var recognitionTask: SFSpeechRecognitionTask?
    
    /// 音频引擎
    private let audioEngine = AVAudioEngine()
    
    /// Combine订阅
    private var cancellables = Set<AnyCancellable>()
    
    /// 音频级别更新定时器
    private var audioLevelTimer: Timer?
    
    /// 输入节点
    private var inputNode: AVAudioInputNode?
    
    // MARK: - 初始化
    
    init() {
        // 从UserDefaults加载设置
        self.autoRecognition = UserDefaults.standard.bool(forKey: "voiceAutoRecognition")
        self.showPartialResults = UserDefaults.standard.bool(forKey: "voiceShowPartialResults")
        self.recognitionLocale = UserDefaults.standard.string(forKey: "voiceRecognitionLocale") ?? "zh-CN"
        self.sensitivity = UserDefaults.standard.double(forKey: "voiceSensitivity")
        
        if self.sensitivity == 0 {
            self.sensitivity = 0.5
        }
        
        // 初始化语音识别器
        setupSpeechRecognizer()
    }
    
    deinit {
        stopAudioLevelMonitoring()
        audioEngine.stop()
        recognitionTask?.cancel()
    }
    
    // MARK: - 公开方法 - 录音控制
    
    /// 开始录音
    func startRecording() async {
        // 检查权限
        let hasPermission = await checkPermissions()
        guard hasPermission else { return }
        
        // 如果正在录音，先停止
        if isRecording {
            await stopRecording()
            return
        }
        
        // 重置状态
        recognizedText = ""
        responseText = nil
        
        do {
            try await startSpeechRecognition()
            isRecording = true
            statusText = "正在聆听..."
            
            // 启动音频级别监测
            startAudioLevelMonitoring()
            
        } catch {
            errorMessage = "启动录音失败: \(error.localizedDescription)"
            showError = true
            isRecording = false
            statusText = "启动失败"
        }
    }
    
    /// 停止录音
    func stopRecording() async {
        guard isRecording else { return }
        
        // 停止音频引擎
        audioEngine.stop()
        inputNode?.removeTap(onBus: 0)
        
        // 停止识别任务
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        
        // 停止音频级别监测
        stopAudioLevelMonitoring()
        
        isRecording = false
        statusText = recognizedText.isEmpty ? "未识别到语音" : "识别完成"
        
        // 如果有识别结果，自动执行
        if autoRecognition && !recognizedText.isEmpty {
            await executeCommand()
        }
    }
    
    /// 清除识别结果
    func clearRecognition() {
        recognizedText = ""
        responseText = nil
        statusText = "就绪"
    }
    
    // MARK: - 公开方法 - 命令执行
    
    /// 执行识别到的命令
    func executeCommand() async {
        guard !recognizedText.isEmpty else { return }
        
        statusText = "处理中..."
        
        do {
            // 模拟API调用处理命令
            try await Task.sleep(nanoseconds: 1_000_000_000)
            
            // 根据命令内容生成响应
            let response = generateResponse(for: recognizedText)
            responseText = response
            statusText = "完成"
            
        } catch {
            errorMessage = "执行命令失败: \(error.localizedDescription)"
            showError = true
            statusText = "执行失败"
        }
    }
    
    /// 执行快捷指令
    func executeQuickCommand(_ command: String) async {
        recognizedText = command
        await executeCommand()
    }
    
    // MARK: - 私有方法 - 语音识别设置
    
    /// 设置语音识别器
    private func setupSpeechRecognizer() {
        let locale = Locale(identifier: recognitionLocale)
        speechRecognizer = SFSpeechRecognizer(locale: locale)
        speechRecognizer?.delegate = self
    }
    
    /// 更新识别请求
    private func updateRecognitionRequest() {
        setupSpeechRecognizer()
    }
    
    /// 检查权限
    private func checkPermissions() async -> Bool {
        // 检查麦克风权限
        let audioSession = AVAudioSession.sharedInstance()
        var microphonePermissionGranted = false
        
        switch audioSession.recordPermission {
        case .granted:
            microphonePermissionGranted = true
        case .denied:
            permissionAlertMessage = "需要麦克风权限才能使用语音识别功能。请在设置中开启。"
            showPermissionAlert = true
            return false
        case .undetermined:
            microphonePermissionGranted = await requestMicrophonePermission()
        @unknown default:
            microphonePermissionGranted = false
        }
        
        guard microphonePermissionGranted else {
            permissionAlertMessage = "麦克风权限被拒绝，无法使用语音识别功能。"
            showPermissionAlert = true
            return false
        }
        
        // 检查语音识别权限
        let speechStatus = await withCheckedContinuation { continuation in
            SFSpeechRecognizer.requestAuthorization { status in
                continuation.resume(returning: status)
            }
        }
        
        switch speechStatus {
        case .authorized:
            return true
        case .denied:
            permissionAlertMessage = "需要语音识别权限才能使用此功能。请在设置中开启。"
            showPermissionAlert = true
            return false
        case .restricted, .notDetermined:
            permissionAlertMessage = "语音识别功能受限，无法使用。"
            showPermissionAlert = true
            return false
        @unknown default:
            return false
        }
    }
    
    /// 请求麦克风权限
    private func requestMicrophonePermission() async -> Bool {
        await withCheckedContinuation { continuation in
            AVAudioSession.sharedInstance().requestRecordPermission { granted in
                continuation.resume(returning: granted)
            }
        }
    }
    
    /// 开始语音识别
    private func startSpeechRecognition() async throws {
        // 取消之前的任务
        recognitionTask?.cancel()
        recognitionTask = nil
        
        // 配置音频会话
        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
        try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        
        // 创建识别请求
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        
        guard let recognitionRequest = recognitionRequest else {
            throw VoiceError.recognitionRequestFailed
        }
        
        // 配置请求
        recognitionRequest.shouldReportPartialResults = showPartialResults
        recognitionRequest.contextualStrings = ["北极星", "AI", "获客", "商机", "任务"]
        recognitionRequest.requiresOnDeviceRecognition = false
        
        // 开始识别任务
        recognitionTask = speechRecognizer?.recognitionTask(with: recognitionRequest) { [weak self] result, error in
            Task { @MainActor in
                if let result = result {
                    self?.recognizedText = result.bestTranscription.formattedString
                    
                    if result.isFinal {
                        self?.statusText = "识别完成"
                    }
                }
                
                if error != nil {
                    // 错误处理
                    self?.statusText = "识别出错"
                }
            }
        }
        
        // 配置音频输入
        inputNode = audioEngine.inputNode
        let recordingFormat = inputNode?.outputFormat(forBus: 0)
        
        inputNode?.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
            recognitionRequest.append(buffer)
        }
        
        // 启动音频引擎
        audioEngine.prepare()
        try audioEngine.start()
    }
    
    // MARK: - 私有方法 - 音频级别监测
    
    /// 启动音频级别监测
    private func startAudioLevelMonitoring() {
        stopAudioLevelMonitoring()
        
        audioLevelTimer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] _ in
            guard let self = self, self.isRecording else { return }
            
            // 获取音频级别
            if let inputNode = self.inputNode {
                let power = self.getAudioPower(from: inputNode)
                self.audioLevel = CGFloat(power)
            }
        }
    }
    
    /// 停止音频级别监测
    private func stopAudioLevelMonitoring() {
        audioLevelTimer?.invalidate()
        audioLevelTimer = nil
        audioLevel = 0
    }
    
    /// 获取音频功率
    private func getAudioPower(from inputNode: AVAudioInputNode) -> Float {
        // 简化实现，实际应该使用更复杂的音频分析
        // 这里返回一个模拟值
        return Float.random(in: 0.3...0.8)
    }
    
    // MARK: - 私有方法 - 响应生成
    
    /// 根据命令生成响应
    private func generateResponse(for command: String) -> String {
        let lowercasedCommand = command.lowercased()
        
        if lowercasedCommand.contains("商机") {
            return "今天有3个新商机等待处理。其中2个来自微信公众号，1个来自抖音推广。建议您优先处理来自北京地区的那个商机，客户预算较高。"
        } else if lowercasedCommand.contains("任务") || lowercasedCommand.contains("获客") {
            return "您当前有2个正在执行的任务，今日已获取15个潜在客户。建议您查看任务进度，预计今天还能获取更多客户。"
        } else if lowercasedCommand.contains("余额") || lowercasedCommand.contains("账户") {
            return "您的账户当前余额为3,580积分。按照当前消费速度，预计还可以使用约2周。建议您适时充值以确保服务不中断。"
        } else if lowercasedCommand.contains("进度") {
            return "任务'春季获客计划'当前进度75%，预计明天完成。已获取潜在客户42个，转化率12%。表现良好！"
        } else if lowercasedCommand.contains("帮助") || lowercasedCommand.contains("能做什么") {
            return "我可以帮您：1.查询今日商机和任务进度 2.创建新的获客任务 3.查询账户余额和充值 4.提供数据分析报告。请直接说出您的需求。"
        } else {
            return "收到您的指令：\"\(command)\"。我正在为您处理，稍后会有详细结果推送给您。如有其他需求，请随时告诉我。"
        }
    }
}

// MARK: - 语音识别代理

extension VoiceViewModel: SFSpeechRecognizerDelegate {
    func speechRecognizer(_ speechRecognizer: SFSpeechRecognizer, availabilityDidChange available: Bool) {
        if !available {
            errorMessage = "语音识别暂时不可用"
            showError = true
        }
    }
}

// MARK: - 错误类型

enum VoiceError: Error, LocalizedError {
    case recognitionRequestFailed
    case audioSessionFailed
    case recognitionFailed
    case permissionDenied
    
    var errorDescription: String? {
        switch self {
        case .recognitionRequestFailed:
            return "创建识别请求失败"
        case .audioSessionFailed:
            return "音频会话配置失败"
        case .recognitionFailed:
            return "语音识别失败"
        case .permissionDenied:
            return "权限被拒绝"
        }
    }
}

// MARK: - 预览

#if DEBUG
extension VoiceViewModel {
    static let preview = VoiceViewModel()
}
#endif
