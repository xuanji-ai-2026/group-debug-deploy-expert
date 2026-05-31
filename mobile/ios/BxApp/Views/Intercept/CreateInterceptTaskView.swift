//
//  CreateInterceptTaskView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  创建截客任务视图
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 创建截客任务视图
/// 
/// 提供表单界面用于创建新的截客任务。
struct CreateInterceptTaskView: View {
    
    // MARK: - 属性
    
    @ObservedObject var viewModel: InterceptTaskViewModel
    
    @Environment(\.dismiss) private var dismiss
    
    /// 当前步骤
    @State private var currentStep = 0
    
    /// 总步骤数
    private let totalSteps = 3
    
    /// 新关键词输入
    @State private var newKeyword = ""
    
    /// 排除关键词输入
    @State private var newExcludeKeyword = ""
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            Form {
                // 基本信息
                basicInfoSection
                
                // 平台配置
                platformSection
                
                // 关键词配置
                keywordsSection
                
                // 筛选配置
                filterSection
                
                // 执行配置
                executionSection
                
                // 触达配置
                reachOutSection
            }
            .navigationTitle("创建截客任务")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("取消") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("创建") {
                        Task {
                            let success = await viewModel.createTask()
                            if success {
                                dismiss()
                            }
                        }
                    }
                    .disabled(!viewModel.newTaskForm.isValid || viewModel.isCreating)
                }
            }
            .overlay {
                if viewModel.isCreating {
                    LoadingOverlay(message: "创建中...")
                }
            }
        }
    }
    
    // MARK: - 组件 - 基本信息
    
    private var basicInfoSection: some View {
        Section("基本信息") {
            TextField("任务名称", text: $viewModel.newTaskForm.name)
            
            TextField("任务描述（可选）", text: $viewModel.newTaskForm.description, axis: .vertical)
                .lineLimit(2...4)
            
            Picker("优先级", selection: $viewModel.newTaskForm.priority) {
                ForEach(TaskPriority.allCases, id: \.self) { priority in
                    HStack {
                        Image(systemName: priority.iconName)
                        Text(priority.name)
                    }
                    .tag(priority)
                }
            }
        }
    }
    
    // MARK: - 组件 - 平台配置
    
    private var platformSection: some View {
        Section("目标平台") {
            Picker("选择平台", selection: $viewModel.newTaskForm.platform) {
                ForEach([SourcePlatform.douyin, .xiaohongshu, .wechatChannels, .kuaishou], id: \.self) { platform in
                    HStack {
                        platformIcon(for: platform)
                        Text(platform.name)
                    }
                    .tag(platform)
                }
            }
            .pickerStyle(.inline)
        }
    }
    
    private func platformIcon(for platform: SourcePlatform) -> some View {
        Group {
            switch platform {
            case .douyin:
                Image(systemName: "music.note")
                    .foregroundColor(.pink)
            case .xiaohongshu:
                Image(systemName: "book.fill")
                    .foregroundColor(.red)
            case .wechatChannels:
                Image(systemName: "video.fill")
                    .foregroundColor(.green)
            case .kuaishou:
                Image(systemName: "bolt.fill")
                    .foregroundColor(.orange)
            default:
                Image(systemName: "globe")
                    .foregroundColor(.blue)
            }
        }
    }
    
    // MARK: - 组件 - 关键词配置
    
    private var keywordsSection: some View {
        Section("关键词配置") {
            // 关键词输入
            HStack {
                TextField("添加关键词", text: $newKeyword)
                    .onSubmit {
                        addKeyword()
                    }
                
                Button(action: addKeyword) {
                    Image(systemName: "plus.circle.fill")
                        .foregroundColor(.blue)
                }
                .disabled(newKeyword.isEmpty)
            }
            
            // 已添加的关键词
            if !viewModel.newTaskForm.keywords.isEmpty {
                FlowLayout(spacing: 8) {
                    ForEach(viewModel.newTaskForm.keywords, id: \.self) { keyword in
                        KeywordChip(
                            text: keyword,
                            onDelete: {
                                viewModel.removeKeyword(keyword)
                            }
                        )
                    }
                }
                .padding(.vertical, 4)
            }
            
            // 匹配模式
            Picker("匹配模式", selection: $viewModel.newTaskForm.keywordMatchMode) {
                ForEach(KeywordMatchMode.allCases, id: \.self) { mode in
                    Text(mode.name)
                        .tag(mode)
                }
            }
            
            // 排除关键词
            HStack {
                TextField("添加排除关键词", text: $newExcludeKeyword)
                    .onSubmit {
                        addExcludeKeyword()
                    }
                
                Button(action: addExcludeKeyword) {
                    Image(systemName: "plus.circle.fill")
                        .foregroundColor(.orange)
                }
                .disabled(newExcludeKeyword.isEmpty)
            }
            
            // 已添加的排除关键词
            if !viewModel.newTaskForm.excludeKeywords.isEmpty {
                FlowLayout(spacing: 8) {
                    ForEach(viewModel.newTaskForm.excludeKeywords, id: \.self) { keyword in
                        ExcludeKeywordChip(
                            text: keyword,
                            onDelete: {
                                viewModel.removeExcludeKeyword(keyword)
                            }
                        )
                    }
                }
                .padding(.vertical, 4)
            }
        }
    }
    
    private func addKeyword() {
        viewModel.addKeyword(newKeyword)
        newKeyword = ""
    }
    
    private func addExcludeKeyword() {
        viewModel.addExcludeKeyword(newExcludeKeyword)
        newExcludeKeyword = ""
    }
    
    // MARK: - 组件 - 筛选配置
    
    private var filterSection: some View {
        Section("筛选条件") {
            HStack {
                Text("最小点赞数")
                Spacer()
                TextField("不限", value: $viewModel.newTaskForm.minLikes, format: .number)
                    .keyboardType(.numberPad)
                    .multilineTextAlignment(.trailing)
                    .frame(width: 80)
            }
            
            HStack {
                Text("最小评论数")
                Spacer()
                TextField("不限", value: $viewModel.newTaskForm.minComments, format: .number)
                    .keyboardType(.numberPad)
                    .multilineTextAlignment(.trailing)
                    .frame(width: 80)
            }
            
            Picker("发布时间", selection: $viewModel.newTaskForm.searchTimeRange) {
                Text("24小时内").tag(24)
                Text("3天内").tag(72)
                Text("7天内").tag(168)
                Text("30天内").tag(720)
            }
        }
    }
    
    // MARK: - 组件 - 执行配置
    
    private var executionSection: some View {
        Section("执行配置") {
            HStack {
                Text("最大截客数")
                Spacer()
                Stepper(
                    value: $viewModel.newTaskForm.maxInterceptCount,
                    in: 1...500,
                    step: 10
                ) {
                    Text("\(viewModel.newTaskForm.maxInterceptCount)")
                        .foregroundColor(.secondary)
                }
                .fixedSize()
            }
            
            HStack {
                Text("最大搜索次数")
                Spacer()
                Stepper(
                    value: $viewModel.newTaskForm.maxSearchCount,
                    in: 10...1000,
                    step: 50
                ) {
                    Text("\(viewModel.newTaskForm.maxSearchCount)")
                        .foregroundColor(.secondary)
                }
                .fixedSize()
            }
            
            HStack {
                Text("执行间隔（秒）")
                Spacer()
                Picker("", selection: $viewModel.newTaskForm.executionInterval) {
                    Text("10秒").tag(10)
                    Text("30秒").tag(30)
                    Text("1分钟").tag(60)
                    Text("2分钟").tag(120)
                    Text("5分钟").tag(300)
                }
                .pickerStyle(.menu)
                .fixedSize()
            }
        }
    }
    
    // MARK: - 组件 - 触达配置
    
    private var reachOutSection: some View {
        Section("触达配置") {
            Toggle("自动触达", isOn: $viewModel.newTaskForm.autoReachOut)
            
            if viewModel.newTaskForm.autoReachOut {
                VStack(alignment: .leading, spacing: 12) {
                    Text("选择触达方式")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    ForEach(ReachOutMethod.allCases, id: \.self) { method in
                        Button(action: {
                            toggleReachOutMethod(method)
                        }) {
                            HStack {
                                Image(systemName: method.iconName)
                                    .foregroundColor(.blue)
                                
                                Text(method.name)
                                    .foregroundColor(.primary)
                                
                                Spacer()
                                
                                if viewModel.newTaskForm.reachOutMethods.contains(method) {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(.green)
                                } else {
                                    Image(systemName: "circle")
                                        .foregroundColor(.gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private func toggleReachOutMethod(_ method: ReachOutMethod) {
        if let index = viewModel.newTaskForm.reachOutMethods.firstIndex(of: method) {
            viewModel.newTaskForm.reachOutMethods.remove(at: index)
        } else {
            viewModel.newTaskForm.reachOutMethods.append(method)
        }
    }
}

// MARK: - 关键词芯片

struct KeywordChip: View {
    let text: String
    let onDelete: () -> Void
    
    var body: some View {
        HStack(spacing: 4) {
            Text(text)
                .font(.caption)
                .foregroundColor(.blue)
            
            Button(action: onDelete) {
                Image(systemName: "xmark.circle.fill")
                    .font(.caption)
                    .foregroundColor(.blue.opacity(0.7))
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color.blue.opacity(0.1))
        .cornerRadius(12)
    }
}

// MARK: - 排除关键词芯片

struct ExcludeKeywordChip: View {
    let text: String
    let onDelete: () -> Void
    
    var body: some View {
        HStack(spacing: 4) {
            Text(text)
                .font(.caption)
                .foregroundColor(.orange)
                .strikethrough()
            
            Button(action: onDelete) {
                Image(systemName: "xmark.circle.fill")
                    .font(.caption)
                    .foregroundColor(.orange.opacity(0.7))
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color.orange.opacity(0.1))
        .cornerRadius(12)
    }
}

// MARK: - 加载遮罩

struct LoadingOverlay: View {
    let message: String
    
    var body: some View {
        ZStack {
            Color.black.opacity(0.3)
                .ignoresSafeArea()
            
            VStack(spacing: 16) {
                ProgressView()
                    .scaleEffect(1.2)
                
                Text(message)
                    .font(.subheadline)
                    .foregroundColor(.primary)
            }
            .padding(24)
            .background(Color(.systemBackground))
            .cornerRadius(12)
        }
    }
}

// MARK: - 预览

#if DEBUG
struct CreateInterceptTaskView_Previews: PreviewProvider {
    static var previews: some View {
        CreateInterceptTaskView(viewModel: InterceptTaskViewModel.preview)
    }
}
#endif
