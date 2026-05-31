import SwiftUI

struct CrawlManagementView: View {
    @StateObject private var viewModel = CrawlViewModel()
    @State private var selectedTab = 0
    @State private var showCreateTaskSheet = false
    @State private var showSendMessageSheet = false
    @State private var selectedComment: SocialComment?
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                Picker("功能", selection: $selectedTab) {
                    Text("📡 抓取任务").tag(0)
                    Text("💬 高意向评论").tag(1)
                    Text("✉️ 私信模板").tag(2)
                }
                .pickerStyle(SegmentedPickerStyle())
                .padding(.horizontal)
                
                TabView(selection: $selectedTab) {
                    TaskListView(viewModel: viewModel)
                        .tag(0)
                    
                    CommentListView(viewModel: viewModel, 
                                   onSendMessage: { comment in
                        selectedComment = comment
                        showSendMessageSheet = true
                    })
                    .tag(1)
                    
                    TemplateListView(viewModel: viewModel)
                    .tag(2)
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
            }
            .navigationTitle("获客中心")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        showCreateTaskSheet = true
                    }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title2)
                    }
                }
            }
            .sheet(isPresented: $showCreateTaskSheet) {
                CreateTaskView(viewModel: viewModel)
            }
            .sheet(isPresented: $showSendMessageSheet) {
                if let comment = selectedComment {
                    SendMessageView(comment: comment, viewModel: viewModel)
                }
            }
        }
    }
}

struct TaskListView: View {
    @ObservedObject var viewModel: CrawlViewModel
    
    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.tasks.isEmpty {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if viewModel.tasks.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "tray")
                        .font(.system(size: 60))
                        .foregroundColor(.gray)
                    Text("暂无抓取任务")
                        .foregroundColor(.secondary)
                    Button("创建第一个任务") {
                        
                    }
                    .buttonStyle(.borderedProminent)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List {
                    ForEach(viewModel.tasks) { task in
                        TaskRowView(task: task, viewModel: viewModel)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                viewModel.loadComments(taskId: task.id ?? 0)
                                withAnimation {
                                    selectedTab = 1
                                }
                            }
                    }
                }
                .refreshable {
                    viewModel.loadTasks()
                }
            }
        }
        .alert("错误", isPresented: Binding<Bool>(
            get: { viewModel.errorMessage != nil },
            set: { _ in viewModel.errorMessage = nil }
        )) {
            Button("确定") {}
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
    }
}

struct TaskRowView: View {
    let task: CrawlTask
    @ObservedObject var viewModel: CrawlViewModel
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(task.taskName ?? "未命名任务")
                    .font(.headline)
                
                Spacer()
                
                Text(task.platformDisplayName)
                    .font(.caption)
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(platformColor(task.platformCode))
                    .cornerRadius(6)
            }
            
            HStack(spacing: 16) {
                Label("\(task.totalCommentsFound ?? 0)", systemImage: "bubble.left")
                    .font(.caption)
                
                Label("\(task.highIntentCount ?? 0)", systemImage: "star.fill")
                    .font(.caption)
                    .foregroundColor(.orange)
                
                Label("\(task.leadsGenerated ?? 0)", systemImage: "person.badge.plus")
                    .font(.caption)
                    .foregroundColor(.green)
                
                Spacer()
                
                Text(statusText(task.status))
                    .font(.caption)
                    .foregroundColor(statusColor(task.status))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(statusColor(task.status).opacity(0.1))
                    .cornerRadius(6)
            }
            
            ProgressView(value: Double(task.progressPercent ?? 0), total: 100)
                .tint(progressColor(task.status))
        }
        .padding(.vertical, 4)
    }
    
    func platformColor(_ code: String?) -> Color {
        switch code?.uppercased() {
        case "DOUYIN":
            return .black
        case "XIAOHONGSHU":
            return .red
        case "KUAISHOU":
            return .orange
        default:
            return .blue
        }
    }
    
    func statusText(_ status: Int?) -> String {
        switch status {
        case 0:
            return "待执行"
        case 1:
            return "进行中"
        case 2:
            return "已完成"
        case 3:
            return "失败"
        default:
            return "未知"
        }
    }
    
    func statusColor(_ status: Int?) -> Color {
        switch status {
        case 0:
            return .gray
        case 1:
            return .blue
        case 2:
            return .green
        case 3:
            return .red
        default:
            return .gray
        }
    }
    
    func progressColor(_ status: Int?) -> Color {
        switch status {
        case 3:
            return .red
        default:
            return .blue
        }
    }
}

struct CommentListView: View {
    @ObservedObject var viewModel: CrawlViewModel
    var onSendMessage: (SocialComment) -> Void
    
    var body: some View {
        VStack(spacing: 12) {
            statisticsCards
            
            if viewModel.isLoading && viewModel.comments.isEmpty {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if viewModel.comments.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "text.bubble")
                        .font(.system(size: 60))
                        .foregroundColor(.gray)
                    Text("暂无评论数据")
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List {
                    ForEach(viewModel.comments) { comment in
                        CommentRowView(comment: comment, onSendMessage: onSendMessage)
                    }
                }
                .refreshable {
                    
                }
            }
        }
    }
    
    var statisticsCards: some View {
        HStack(spacing: 12) {
            StatCard(title: "总评论", value: "\(viewModel.comments.count)", color: .blue)
            StatCard(title: "高意向", value: "\(viewModel.comments.filter { $0.isHighIntent == true }.count)", color: .orange)
            StatCard(title: "留电话", value: "\(viewModel.comments.filter { $0.hasPhoneContact == true }.count)", color: .red)
            StatCard(title: "留微信", value: "\(viewModel.comments.filter { $0.hasWechatContact == true }.count)", color: .green)
        }
        .padding(.horizontal)
    }
}

struct StatCard: View {
    let title: String
    let value: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title2.bold())
                .foregroundColor(color)
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(color.opacity(0.1))
        .cornerRadius(10)
    }
}

struct CommentRowView: View {
    let comment: SocialComment
    var onSendMessage: (SocialComment) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                AsyncImage(url: URL(string: comment.authorAvatar ?? "")) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable()
                    case .failure:
                        Image(systemName: "person.circle.fill")
                            .resizable()
                    default:
                        ProgressView()
                    }
                }
                .frame(width: 40, height: 40)
                .clipShape(Circle())
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(comment.authorName ?? "匿名用户")
                        .font(.subheadline.bold())
                    
                    HStack(spacing: 4) {
                        if comment.userVerified == true {
                            Image(systemName: "checkmark.seal.fill")
                                .font(.caption2)
                                .foregroundColor(.blue)
                        }
                        
                        if let followers = comment.userFollowerCount {
                            Text(formatNumber(followers) + "粉丝")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 4) {
                    HStack(spacing: 4) {
                        Text("\(comment.aiIntentScore ?? 0)")
                            .font(.subheadline.bold())
                            .foregroundColor(scoreColor(comment.aiIntentScore))
                        
                        Text("分")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    if let level = comment.aiIntentLevel {
                        Text(levelDisplay(level))
                            .font(.caption2)
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(levelColor(level))
                            .cornerRadius(4)
                    }
                }
            }
            
            Text(comment.commentText ?? "")
                .font(.subheadline)
                .lineLimit(3)
            
            if comment.hasPhoneContact == true || comment.hasWechatContact == true {
                HStack(spacing: 8) {
                    if comment.hasPhoneContact == true,
                       let phone = comment.extractedPhone {
                        Label(phone, systemImage: "phone.fill")
                            .font(.caption)
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.red.opacity(0.9))
                            .cornerRadius(6)
                    }
                    
                    if comment.hasWechatContact == true,
                       let wechat = comment.extractedWechat {
                        Label(wechat, systemImage: "message.fill")
                            .font(.caption)
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.green.opacity(0.9))
                            .cornerRadius(6)
                    }
                }
            }
            
            HStack {
                Label("\(comment.likeCount ?? 0)", systemImage: "heart.fill")
                    .font(.caption)
                    .foregroundColor(.red)
                
                Label("\(comment.replyCount ?? 0)", systemImage: "arrowshape.turn.up.left.fill")
                    .font(.caption)
                    .foregroundColor(.blue)
                
                Spacer()
                
                Button("发送私信") {
                    onSendMessage(comment)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.small)
            }
        }
        .padding(.vertical, 4)
    }
    
    func scoreColor(_ score: Int?) -> Color {
        guard let score = score else { return .gray }
        if score >= 85 { return .red }
        if score >= 70 { return .orange }
        if score >= 55 { return .blue }
        return .gray
    }
    
    func levelDisplay(_ level: String) -> String {
        switch level.uppercased() {
        case "A": return "A级"
        case "B": return "B级"
        case "C": return "C级"
        default: return level
        }
    }
    
    func levelColor(_ level: String) -> Color {
        switch level.uppercased() {
        case "A": return .red
        case "B": return .orange
        case "C": return .blue
        default: return .gray
        }
    }
    
    func formatNumber(_ num: Int64) -> String {
        if num >= 10000 {
            return String(format: "%.1fw", Double(num) / 10000.0)
        }
        if num >= 1000 {
            return String(format: "%.1fk", Double(num) / 1000.0)
        }
        return "\(num)"
    }
}

struct TemplateListView: View {
    @ObservedObject var viewModel: CrawlViewModel
    
    var body: some View {
        LazyVGrid(columns: [
            GridItem(.flexible()),
            GridItem(.flexible()),
        ], spacing: 16) {
            ForEach(viewModel.templates) { template in
                TemplateCardView(template: template)
            }
        }
        .padding()
    }
}

struct TemplateCardView: View {
    let template: MessageTemplate
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                if let level = template.intentLevel {
                    Text(level + "级")
                        .font(.caption.bold())
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(levelColor(level))
                        .cornerRadius(6)
                }
                
                if template.aiGenerated == true {
                    Text("AI生成")
                        .font(.caption)
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.orange.opacity(0.9))
                        .cornerRadius(6)
                }
            }
            
            Text(template.templateName ?? "未命名模板")
                .font(.headline)
                .lineLimit(1)
            
            Text(template.templateContent ?? "")
                .font(.caption)
                .foregroundColor(.secondary)
                .lineLimit(3)
            
            HStack {
                Text("使用\(template.useCount ?? 0)次")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                
                Spacer()
                
                Text("成功率\(((template.successRate ?? 0) * 100).toInt())%")
                    .font(.caption2)
                    .foregroundColor(template.successRate ?? 0 > 0.5 ? .green : .orange)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 5, x: 0, y: 2)
    }
    
    func levelColor(_ level: String) -> Color {
        switch level.uppercased() {
        case "A": return .red
        case "B": return .orange
        case "C": return .blue
        default: return .gray
        }
    }
}
