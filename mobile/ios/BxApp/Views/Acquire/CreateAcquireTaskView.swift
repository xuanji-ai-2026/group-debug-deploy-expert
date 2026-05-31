//
//  CreateAcquireTaskView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  创建获客任务视图
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 创建获客任务视图
/// 
/// 提供向导式表单用于创建新的获客任务。
struct CreateAcquireTaskView: View {
    
    // MARK: - 属性
    
    @ObservedObject var viewModel: AcquireTaskViewModel
    
    @Environment(\.dismiss) private var dismiss
    
    /// 新兴趣标签输入
    @State private var newInterestTag = ""
    
    /// 新内容主题输入
    @State private: String = ""
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // 步骤指示器
                stepIndicator
                
                // 表单内容
                formContent
                
                // 底部导航
                bottomNavigation
            }
            .navigationTitle("创建获客任务")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("取消") {
                        dismiss()
                    }
                }
            }
            .overlay {
                if viewModel.isCreating {
                    LoadingOverlay(message: "创建中...")
                }
            }
        }
    }
    
    // MARK: - 组件 - 步骤指示器
    
    private var stepIndicator: some View {
        VStack(spacing: 12) {
            // 步骤点
            HStack(spacing: 8) {
                ForEach(0..<viewModel.totalCreateSteps, id: \.self) { index in
                    Circle()
                        .fill(index <= viewModel.createStep ? Color.blue : Color.gray.opacity(0.3))
                        .frame(width: 10, height: 10)
                }
            }
            
            // 步骤标题
            Text(stepTitle)
                .font(.headline)
                .foregroundColor(.primary)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
    }
    
    private var stepTitle: String {
        switch viewModel.createStep {
        case 0: return "基本信息"
        case 1: return "目标人群"
        case 2: return "内容配置"
        case 3: return "执行设置"
        default: return ""
        }
    }
    
    // MARK: - 组件 - 表单内容
    
    @ViewBuilder
    private var formContent: some View {
        switch viewModel.createStep {
        case 0:
            basicInfoStep
        case 1:
            targetAudienceStep
        case 2:
            contentConfigStep
        case 3:
            executionConfigStep
        default:
            EmptyView()
        }
    }
    
    // MARK: - 步骤1: 基本信息
    
    private var basicInfoStep: some View {
        Form {
            Section("任务信息") {
                TextField("任务名称", text: $viewModel.newTaskForm.name)
                
                TextField("任务描述（可选）", text: $viewModel.newTaskForm.description, axis: .vertical)
                    .lineLimit(2...4)
                
                Picker("获客类型", selection: $viewModel.newTaskForm.acquireType) {
                    ForEach(AcquireType.allCases, id: \.self) { type in
                        HStack {
                            Image(systemName: type.iconName)
                            Text(type.name)
                        }
                        .tag(type)
                    }
                }
                
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
            
            Section("获客渠道") {
                ForEach([SourcePlatform.douyin, .xiaohongshu, .wechatChannels, .kuaishou], id: \.self) { platform in
                    Button(action: {
                        toggleChannel(platform)
                    }) {
                        HStack {
                            platformIcon(for: platform)
                            
                            Text(platform.name)
                                .foregroundColor(.primary)
                            
                            Spacer()
                            
                            if viewModel.newTaskForm.channels.contains(where: { $0.platform == platform }) {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.green)
                            }
                        }
                    }
                }
            }
            
            Section("主平台") {
                Picker("选择主平台", selection: $viewModel.newTaskForm.primaryPlatform) {
                    ForEach(viewModel.newTaskForm.channels.map { $0.platform }, id: \.self) { platform in
                        Text(platform.name)
                            .tag(platform)
                    }
                }
            }
        }
    }
    
    // MARK: - 步骤2: 目标人群
    
    private var targetAudienceStep: some View {
        Form {
            Section("基本属性") {
                Picker("性别", selection: $viewModel.newTaskForm.targetAudience.gender) {
                    ForEach(GenderFilter.allCases, id: \.self) { gender in
                        Text(gender.name)
                            .tag(gender)
                    }
                }
                
                Picker("活跃度", selection: $viewModel.newTaskForm.targetAudience.activityLevel) {
                    ForEach(ActivityLevel.allCases, id: \.self) { level in
                        Text(level.name)
                            .tag(level)
                    }
                }
                
                Picker("认证状态", selection: $viewModel.newTaskForm.targetAudience.verificationStatus) {
                    ForEach(VerificationStatus.allCases, id: \.self) { status in
                        Text(status.name)
                            .tag(status)
                    }
                }
            }
            
            Section("兴趣标签") {
                HStack {
                    TextField("添加兴趣标签", text: $newInterestTag)
                        .onSubmit {
                            addInterestTag()
                        }
                    
                    Button(action: addInterestTag) {
                        Image(systemName: "plus.circle.fill")
                            .foregroundColor(.blue)
                    }
                    .disabled(newInterestTag.isEmpty)
                }
                
                if !viewModel.newTaskForm.targetAudience.interestTags.isEmpty {
                    FlowLayout(spacing: 8) {
                        ForEach(viewModel.newTaskForm.targetAudience.interestTags, id: \.self) { tag in
                            KeywordChip(
                                text: tag,
                                onDelete: {
                                    viewModel.removeInterestTag(tag)
                                }
                            )
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            
            Section("粉丝数范围") {
                HStack {
                    Text("最小粉丝数")
                    Spacer()
                    TextField("不限", value: $viewModel.newTaskForm.targetAudience.minFollowers, format: .number)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 80)
                }
                
                HStack {
                    Text("最大粉丝数")
                    Spacer()
                    TextField("不限", value: $viewModel.newTaskForm.targetAudience.maxFollowers, format: .number)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 80)
                }
            }
        }
    }
    
    // MARK: - 步骤3: 内容配置
    
    private var contentConfigStep: some View {
        Form {
            Section("内容类型") {
                Picker("内容类型", selection: $viewModel.newTaskForm.contentType) {
                    Text("请选择").tag(nil as ContentType?)
                    
                    ForEach(ContentType.allCases, id: \.self) { type in
                        HStack {
                            Image(systemName: type.iconName)
                            Text(type.name)
                        }
                        .tag(type as ContentType?)
                    }
                }
            }
            
            Section("内容主题") {
                HStack {
                    TextField("添加内容主题", text: $newContentTopic)
                        .onSubmit {
                            addContentTopic()
                        }
                    
                    Button(action: addContentTopic) {
                        Image(systemName: "plus.circle.fill")
                            .foregroundColor(.blue)
                    }
                    .disabled(newContentTopic.isEmpty)
                }
                
                if !viewModel.newTaskForm.contentTopics.isEmpty {
                    FlowLayout(spacing: 8) {
                        ForEach(viewModel.newTaskForm.contentTopics, id: \.self) { topic in
                            KeywordChip(
                                text: topic,
                                onDelete: {
                                    viewModel.removeContentTopic(topic)
                                }
                            )
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            
            Section("AI生成") {
                Toggle("使用AI生成内容", isOn: $viewModel.newTaskForm.useAIGeneration)
                
                if viewModel.newTaskForm.useAIGeneration {
                    TextField("AI提示词", text: $viewModel.newTaskForm.aiPrompt, axis: .vertical)
                        .lineLimit(2...4)
                }
            }
        }
    }
    
    // MARK: - 步骤4: 执行设置
    
    private var executionConfigStep: some View {
        Form {
            Section("执行策略") {
                Picker("策略", selection: $viewModel.newTaskForm.executionStrategy) {
                    ForEach(ExecutionStrategy.allCases, id: \.self) { strategy in
                        VStack(alignment: .leading) {
                            Text(strategy.name)
                            Text(strategy.description)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .tag(strategy)
                    }
                }
            }
            
            Section("执行频率") {
                HStack {
                    Text("每日执行次数")
                    Spacer()
                    Stepper(
                        value: $viewModel.newTaskForm.dailyExecutionCount,
                        in: 1...50,
                        step: 1
                    ) {
                        Text("\(viewModel.newTaskForm.dailyExecutionCount) 次")
                            .foregroundColor(.secondary)
                    }
                    .fixedSize()
                }
                
                HStack {
                    Text("每次获客数量")
                    Spacer()
                    Stepper(
                        value: $viewModel.newTaskForm.acquireBatchSize,
                        in: 10...200,
                        step: 10
                    ) {
                        Text("\(viewModel.newTaskForm.acquireBatchSize) 人")
                            .foregroundColor(.secondary)
                    }
                    .fixedSize()
                }
            }
            
            Section("执行日期") {
                HStack(spacing: 8) {
                    ForEach(WeekDay.allCases, id: \.self) { day in
                        Button(action: {
                            viewModel.toggleExecutionDay(day)
                        }) {
                            Text(day.shortName)
                                .font(.caption)
                                .fontWeight(.medium)
                                .foregroundColor(viewModel.newTaskForm.executionDays.contains(day) ? .white : .primary)
                                .frame(width: 32, height: 32)
                                .background(viewModel.newTaskForm.executionDays.contains(day) ? Color.blue : Color(.systemGray5))
                                .cornerRadius(16)
                        }
                    }
                }
                .padding(.vertical, 4)
            }
            
            Section("触达配置") {
                Picker("触达方式", selection: $viewModel.newTaskForm.reachOutMethod) {
                    ForEach(ReachOutMethod.allCases, id: \.self) { method in
                        HStack {
                            Image(systemName: method.iconName)
                            Text(method.name)
                        }
                        .tag(method)
                    }
                }
                
                Toggle("自动跟进", isOn: $viewModel.newTaskForm.autoFollowUp)
            }
        }
    }
    
    // MARK: - 组件 - 底部导航
    
    private var bottomNavigation: some View {
        HStack {
            // 上一步按钮
            if viewModel.createStep > 0 {
                Button("上一步") {
                    viewModel.previousStep()
                }
                .buttonStyle(NavigationButtonStyle(color: .gray))
            } else {
                Spacer()
                    .frame(width: 80)
            }
            
            Spacer()
            
            // 下一步/创建按钮
            if viewModel.createStep < viewModel.totalCreateSteps - 1 {
                Button("下一步") {
                    viewModel.nextStep()
                }
                .buttonStyle(NavigationButtonStyle(color: .blue))
                .disabled(!viewModel.canProceedToNextStep)
            } else {
                Button("创建任务") {
                    Task {
                        let success = await viewModel.createTask()
                        if success {
                            dismiss()
                        }
                    }
                }
                .buttonStyle(NavigationButtonStyle(color: .green))
                .disabled(!viewModel.newTaskForm.isValid || viewModel.isCreating)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .shadow(color: .black.opacity(0.05), radius: 4, y: -2)
    }
    
    // MARK: - 辅助方法
    
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
    
    private func toggleChannel(_ platform: SourcePlatform) {
        if let existingChannel = viewModel.newTaskForm.channels.first(where: { $0.platform == platform }) {
            viewModel.removeChannel(platform)
        } else {
            viewModel.addChannel(AcquireChannel(platform: platform))
        }
    }
    
    private func addInterestTag() {
        viewModel.addInterestTag(newInterestTag)
        newInterestTag = ""
    }
    
    private func addContentTopic() {
        viewModel.addContentTopic(newContentTopic)
        newContentTopic = ""
    }
}

// MARK: - 导航按钮样式

struct NavigationButtonStyle: ButtonStyle {
    let color: Color
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline)
            .fontWeight(.medium)
            .foregroundColor(.white)
            .padding(.horizontal, 20)
            .padding(.vertical, 10)
            .background(color)
            .cornerRadius(8)
            .scaleEffect(configuration.isPressed ? 0.95 : 1)
    }
}

// MARK: - 预览

#if DEBUG
struct CreateAcquireTaskView_Previews: PreviewProvider {
    static var previews: some View {
        CreateAcquireTaskView(viewModel: AcquireTaskViewModel.preview)
    }
}
#endif
