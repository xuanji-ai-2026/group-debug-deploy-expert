//
//  VoiceAssistantView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  语音助手视图
//
//  Created by Lin Feng (EMP-IOS-003) on 2024-01-20
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI
import Speech

/// 语音助手视图
///
/// 提供语音输入界面、语音识别动画、快捷指令等功能。
struct VoiceAssistantView: View {
    
    // MARK: - 状态
    
    /// 视图模型
    @StateObject private var viewModel = VoiceViewModel()
    
    /// 是否显示设置
    @State private var showSettings = false
    
    /// 输入文本
    @State private var inputText = ""
    
    /// 是否显示快捷指令
    @State private var showQuickCommands = true
    
    // MARK: - 快捷指令
    
    /// 快捷指令列表
    private let quickCommands = [
        QuickCommand(title: "查看今日商机", icon: "briefcase.fill", color: .orange, command: "今天有哪些新商机"),
        QuickCommand(title: "创建获客任务", icon: "plus.circle.fill", color: .green, command: "帮我创建一个获客任务"),
        QuickCommand(title: "查询账户余额", icon: "creditcard.fill", color: .blue, command: "我的账户余额是多少"),
        QuickCommand(title: "查看任务进度", icon: "chart.bar.fill", color: .purple, command: "查看正在执行的任务进度")
    ]
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ZStack {
                // 背景渐变
                backgroundGradient
                
                VStack(spacing: 0) {
                    // 顶部状态栏
                    statusBar
                    
                    Spacer()
                    
                    // 中间内容区
                    contentArea
                    
                    Spacer()
                    
                    // 底部控制区
                    bottomControlArea
                }
            }
            .navigationTitle("语音助手")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        showSettings = true
                    }) {
                        Image(systemName: "gear")
                    }
                }
            }
            .sheet(isPresented: $showSettings) {
                VoiceSettingsView(viewModel: viewModel)
            }
            .alert("需要权限", isPresented: $viewModel.showPermissionAlert) {
                Button("取消", role: .cancel) {}
                Button("去设置") {
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        UIApplication.shared.open(url)
                    }
                }
            } message: {
                Text(viewModel.permissionAlertMessage)
            }
            .alert("提示", isPresented: $viewModel.showError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "操作失败")
            }
        }
    }
    
    // MARK: - 组件 - 背景渐变
    
    /// 背景渐变
    private var backgroundGradient: some View {
        LinearGradient(
            gradient: Gradient(colors: [
                Color.blue.opacity(0.1),
                Color.purple.opacity(0.05),
                Color(.systemBackground)
            ]),
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
    }
    
    // MARK: - 组件 - 状态栏
    
    /// 状态栏
    private var statusBar: some View {
        HStack {
            // 语音识别状态
            HStack(spacing: 6) {
                Circle()
                    .fill(viewModel.isRecording ? Color.red : Color.green)
                    .frame(width: 8, height: 8)
                
                Text(viewModel.statusText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            // 语音识别引擎
            HStack(spacing: 4) {
                Image(systemName: "waveform")
                    .font(.caption)
                Text(viewModel.recognitionEngine)
                    .font(.caption)
            }
            .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.systemBackground).opacity(0.8))
    }
    
    // MARK: - 组件 - 内容区域
    
    /// 内容区域
    private var contentArea: some View {
        VStack(spacing: 24) {
            // 语音波形动画
            VoiceWaveformView(
                isRecording: viewModel.isRecording,
                audioLevel: viewModel.audioLevel
            )
            .frame(height: 120)
            
            // 识别结果或提示文本
            if viewModel.isRecording {
                // 正在识别中
                Text(viewModel.recognizedText.isEmpty ? "正在聆听..." : viewModel.recognizedText)
                    .font(.title3)
                    .foregroundColor(.primary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
                    .animation(.easeInOut, value: viewModel.recognizedText)
            } else if !viewModel.recognizedText.isEmpty {
                // 识别完成
                VStack(spacing: 12) {
                    Text(viewModel.recognizedText)
                        .font(.title3)
                        .foregroundColor(.primary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                    
                    HStack(spacing: 16) {
                        Button(action: {
                            viewModel.clearRecognition()
                        }) {
                            Image(systemName: "xmark.circle.fill")
                                .font(.title2)
                                .foregroundColor(.secondary)
                        }
                        
                        Button(action: {
                            Task {
                                await viewModel.executeCommand()
                            }
                        }) {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.title2)
                                .foregroundColor(.green)
                        }
                    }
                }
            } else {
                // 初始状态提示
                VStack(spacing: 8) {
                    Text("按住说话")
                        .font(.title2)
                        .fontWeight(.medium)
                    
                    Text("说出您想执行的操作")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
            
            // 识别结果/响应
            if let response = viewModel.responseText {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: "bubble.left.fill")
                            .foregroundColor(.blue)
                        Text("AI回复")
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(.blue)
                        Spacer()
                    }
                    
                    Text(response)
                        .font(.body)
                        .foregroundColor(.primary)
                        .padding()
                        .background(Color.blue.opacity(0.1))
                        .cornerRadius(12)
                }
                .padding(.horizontal)
            }
        }
    }
    
    // MARK: - 组件 - 底部控制区
    
    /// 底部控制区
    private var bottomControlArea: some View {
        VStack(spacing: 16) {
            // 快捷指令
            if showQuickCommands && !viewModel.isRecording && viewModel.recognizedText.isEmpty {
                quickCommandsSection
            }
            
            // 录音按钮
            recordButton
        }
        .padding()
        .background(
            Color(.systemBackground)
                .ignoresSafeArea(edges: .bottom)
        )
    }
    
    // MARK: - 组件 - 快捷指令
    
    /// 快捷指令区域
    private var quickCommandsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("快捷指令")
                    .font(.headline)
                
                Spacer()
                
                Button(action: {
                    withAnimation {
                        showQuickCommands.toggle()
                    }
                }) {
                    Image(systemName: "chevron.down")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .rotationEffect(.degrees(showQuickCommands ? 180 : 0))
                }
            }
            
            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 12) {
                ForEach(quickCommands) { command in
                    QuickCommandButton(command: command) {
                        inputText = command.command
                        Task {
                            await viewModel.executeQuickCommand(command.command)
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(16)
        .padding(.horizontal)
    }
    
    // MARK: - 组件 - 录音按钮
    
    /// 录音按钮
    private var recordButton: some View {
        Button(action: {
            // 空实现，使用长按手势
        }) {
            ZStack {
                // 外圈动画
                if viewModel.isRecording {
                    Circle()
                        .stroke(Color.red.opacity(0.3), lineWidth: 4)
                        .frame(width: 90, height: 90)
                        .scaleEffect(viewModel.isRecording ? 1.2 : 1.0)
                        .opacity(viewModel.isRecording ? 0 : 1)
                        .animation(
                            .easeInOut(duration: 1.0)
                            .repeatForever(autoreverses: true),
                            value: viewModel.isRecording
                        )
                }
                
                // 主按钮
                Circle()
                    .fill(viewModel.isRecording ? Color.red : Color.blue)
                    .frame(width: 80, height: 80)
                    .shadow(
                        color: (viewModel.isRecording ? Color.red : Color.blue).opacity(0.4),
                        radius: viewModel.isRecording ? 20 : 10,
                        x: 0,
                        y: viewModel.isRecording ? 10 : 5
                    )
                
                // 图标
                Image(systemName: viewModel.isRecording ? "stop.fill" : "mic.fill")
                    .font(.system(size: 32))
                    .foregroundColor(.white)
            }
        }
        .buttonStyle(PlainButtonStyle())
        .simultaneousGesture(
            LongPressGesture(minimumDuration: 0.1)
                .onEnded { _ in
                    Task {
                        await viewModel.startRecording()
                    }
                }
        )
        .onLongPressGesture(
            minimumDuration: 0.1,
            pressing: { isPressing in
                if !isPressing && viewModel.isRecording {
                    Task {
                        await viewModel.stopRecording()
                    }
                }
            },
            perform: {}
        )
    }
}

// MARK: - 快捷指令模型

struct QuickCommand: Identifiable {
    let id = UUID()
    let title: String
    let icon: String
    let color: Color
    let command: String
}

// MARK: - 快捷指令按钮

struct QuickCommandButton: View {
    let command: QuickCommand
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: command.icon)
                    .font(.system(size: 18))
                    .foregroundColor(command.color)
                    .frame(width: 32, height: 32)
                    .background(command.color.opacity(0.15))
                    .cornerRadius(8)
                
                Text(command.title)
                    .font(.subheadline)
                    .foregroundColor(.primary)
                    .lineLimit(1)
                
                Spacer()
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(12)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - 语音波形视图

struct VoiceWaveformView: View {
    let isRecording: Bool
    let audioLevel: CGFloat
    
    @State private var phase: CGFloat = 0
    
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // 背景圆环
                Circle()
                    .stroke(Color.blue.opacity(0.1), lineWidth: 2)
                    .frame(width: 200, height: 200)
                
                // 动画波形
                if isRecording {
                    ForEach(0..<3) { index in
                        WaveformRing(
                            phase: phase + CGFloat(index) * .pi / 3,
                            amplitude: 20 + audioLevel * 30,
                            frequency: 2
                        )
                        .stroke(
                            Color.blue.opacity(0.3 - Double(index) * 0.1),
                            lineWidth: 2
                        )
                        .frame(width: 200, height: 200)
                        .scaleEffect(1.0 + CGFloat(index) * 0.1)
                    }
                }
                
                // 中心麦克风图标
                Image(systemName: isRecording ? "waveform" : "mic.circle.fill")
                    .font(.system(size: isRecording ? 48 : 64))
                    .foregroundColor(isRecording ? .blue : .blue.opacity(0.5))
                    .symbolEffect(.variableColor, isActive: isRecording)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .onAppear {
            withAnimation(.linear(duration: 0.1).repeatForever(autoreverses: false)) {
                phase = .pi * 2
            }
        }
    }
}

// MARK: - 波形环

struct WaveformRing: Shape {
    var phase: CGFloat
    var amplitude: CGFloat
    var frequency: CGFloat
    
    var animatableData: CGFloat {
        get { phase }
        set { phase = newValue }
    }
    
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let radius = min(rect.width, rect.height) / 2 - 20
        
        for angle in stride(from: 0, through: .pi * 2, by: 0.05) {
            let x = center.x + (radius + amplitude * sin(angle * frequency + phase)) * cos(angle)
            let y = center.y + (radius + amplitude * sin(angle * frequency + phase)) * sin(angle)
            
            if angle == 0 {
                path.move(to: CGPoint(x: x, y: y))
            } else {
                path.addLine(to: CGPoint(x: x, y: y))
            }
        }
        
        path.closeSubpath()
        return path
    }
}

// MARK: - 语音设置视图

struct VoiceSettingsView: View {
    @ObservedObject var viewModel: VoiceViewModel
    @Environment(\.presentationMode) var presentationMode
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("语音识别")) {
                    Toggle("自动识别", isOn: $viewModel.autoRecognition)
                    
                    Toggle("实时显示结果", isOn: $viewModel.showPartialResults)
                    
                    Picker("识别语言", selection: $viewModel.recognitionLocale) {
                        Text("简体中文").tag("zh-CN")
                        Text("繁体中文").tag("zh-TW")
                        Text("English").tag("en-US")
                    }
                }
                
                Section(header: Text("灵敏度")) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("识别灵敏度")
                            .font(.subheadline)
                        
                        Slider(value: $viewModel.sensitivity, in: 0.1...1.0) {
                            Text("灵敏度")
                        }
                        
                        Text("数值越高，识别越灵敏但可能产生误触发")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                Section(header: Text("关于")) {
                    HStack {
                        Text("版本")
                        Spacer()
                        Text("1.0.0")
                            .foregroundColor(.secondary)
                    }
                    
                    HStack {
                        Text("引擎")
                        Spacer()
                        Text("Apple Speech")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("语音设置")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("完成") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
        }
    }
}

// MARK: - 预览

#if DEBUG
struct VoiceAssistantView_Previews: PreviewProvider {
    static var previews: some View {
        VoiceAssistantView()
    }
}
#endif
