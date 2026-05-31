import SwiftUI

struct SendMessageView: View {
    let comment: SocialComment
    @ObservedObject var viewModel: CrawlViewModel
    
    @State private var selectedTemplateId: Int64?
    @State private var messageContent = ""
    @State private var isSending = false
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("接收用户")) {
                    HStack(spacing: 12) {
                        AsyncImage(url: URL(string: comment.authorAvatar ?? "")) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable()
                            case .failure:
                                Image(systemName: "person.circle.fill")
                                    .resizable()
                                    .foregroundColor(.gray)
                            default:
                                ProgressView()
                            }
                        }
                        .frame(width: 50, height: 50)
                        .clipShape(Circle())
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text(comment.authorName ?? "匿名用户")
                                .font(.headline)
                            
                            if let score = comment.aiIntentScore,
                               let level = comment.aiIntentLevel {
                                HStack(spacing: 8) {
                                    Text("\(score)分")
                                        .font(.subheadline.bold())
                                        .foregroundColor(scoreColor(score))
                                    
                                    Text(levelDisplay(level))
                                        .font(.caption)
                                        .foregroundColor(.white)
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(levelColor(level))
                                        .cornerRadius(4)
                                }
                            }
                        }
                    }
                }
                
                if comment.hasPhoneContact == true || comment.hasWechatContact == true {
                    Section(header: Text("联系方式（已识别）")) {
                        if comment.hasPhoneContact == true,
                           let phone = comment.extractedPhone {
                            Label(phone, systemImage: "phone.fill")
                                .foregroundColor(.red)
                        }
                        
                        if comment.hasWechatContact == true,
                           let wechat = comment.extractedWechat {
                            Label(wechat, systemImage: "message.fill")
                                .foregroundColor(.green)
                        }
                    }
                }
                
                Section(header: Text("选择模板")) {
                    Picker("私信模板", selection: $selectedTemplateId) {
                        Text("自定义消息").tag(nil as Int64?)
                        
                        ForEach(filteredTemplates) { template in
                            Text("\(template.templateName ?? "") (\(((template.successRate ?? 0) * 100).toInt())%)")
                                .tag(template.id as Int64?)
                        }
                    }
                    .onChange(of: selectedTemplateId) { newValue in
                        if let id = newValue,
                           let template = viewModel.templates.first(where: { $0.id == id }) {
                            applyTemplate(template)
                        }
                    }
                }
                
                Section(header: Text("消息内容")) {
                    TextEditor(text: $messageContent)
                        .frame(minHeight: 150)
                    
                    HStack {
                        Spacer()
                        Text("\(messageContent.count)/500")
                            .font(.caption)
                            .foregroundColor(messageContent.count > 500 ? .red : .secondary)
                    }
                }
                
                Section {
                    Button(action: sendMessage) {
                        HStack {
                            if isSending {
                                ProgressView()
                                    .tint(.white)
                            } else {
                                Image(systemName: "paperplane.fill")
                            }
                            Text("发送私信")
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .listRowInsets(EdgeInsets())
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .disabled(isSending || messageContent.isEmpty || messageContent.count > 500)
                }
            }
            .navigationTitle("发送私信")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("取消") {
                        dismiss()
                    }
                }
            }
        }
    }
    
    var filteredTemplates: [MessageTemplate] {
        guard let level = comment.aiIntentLevel else {
            return viewModel.templates.filter { $0.isDefault == true }
        }
        
        return viewModel.templates.filter { $0.intentLevel == level || $0.isDefault == true }
    }
    
    func applyTemplate(_ template: MessageTemplate) {
        var content = template.templateContent ?? ""
        
        content = content.replacingOccurrences(of: "{昵称}", with: comment.authorName ?? "亲")
        content = content.replacingOccurrences(of: "{产品名}", with: "我们的产品")
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        content = content.replacingOccurrences(of: "{时间}", with: formatter.string(from: Date()))
        
        content = content.replacingOccurrences(of: "{联系方式}", with: "13800138000")
        
        messageContent = content
    }
    
    func sendMessage() {
        guard !messageContent.isEmpty, messageContent.count <= 500 else { return }
        
        isSending = true
        
        let request = SendMessageRequest(
            commentId: comment.id ?? 0,
            templateId: selectedTemplateId,
            content: messageContent
        )
        
        viewModel.sendMessage(request: request)
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
        case "A": return "A级-超高意向"
        case "B": return "B级-高意向"
        case "C": return "C级-中意向"
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
}

struct CreateTaskView: View {
    @ObservedObject var viewModel: CrawlViewModel
    @Environment(\.dismiss) private var dismiss
    
    @State private var platformCode = ""
    @State private var targetType = ""
    @State private var targetId = ""
    @State private var keywords = ""
    @State private var maxCrawlCount = 500
    @State private var crawlIntervalSeconds = 3
    @State private var isCreating = false
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("目标平台")) {
                    Picker("平台", selection: $platformCode) {
                        Text("请选择").tag("")
                        Text("抖音").tag("DOUYIN")
                        Text("小红书").tag("XIAOHONGSHU")
                        Text("快手").tag("KUAISHOU")
                        Text("微博").tag("WEIBO")
                        Text("B站").tag("BILIBILI")
                    }
                }
                
                Section(header: Text("目标类型")) {
                    Picker("类型", selection: $targetType) {
                        Text("请选择").tag("")
                        Text("视频/笔记ID").tag("VIDEO_NOTE")
                        Text("账号主页").tag("USER_PROFILE")
                        Text("话题/标签").tag("TOPIC")
                        Text("搜索关键词").tag("SEARCH_KEYWORD")
                    }
                }
                
                Section(header: Text("目标ID/URL")) {
                    TextField("输入视频ID、笔记链接或搜索词", text: $targetId)
                }
                
                Section(header: Text("关键词过滤（可选）")) {
                    TextField("多个用逗号分隔，例如：购买,咨询,报价", text: $keywords, axis: .vertical)
                        .lineLimit(3...6)
                }
                
                Section(header: Text("抓取设置")) {
                    Stepper("最大抓取数：\(maxCrawlCount)", value: $maxCrawlCount, in: 10...10000, step: 100)
                    
                    Stepper("抓取间隔（秒）：\(crawlIntervalSeconds)", value: $crawlIntervalSeconds, in: 1...60)
                }
                
                Section {
                    Button(action: createTask) {
                        HStack {
                            if isCreating {
                                ProgressView()
                                    .tint(.white)
                            } else {
                                Image(systemName: "play.fill")
                            }
                            Text("开始抓取")
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .listRowInsets(EdgeInsets())
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .disabled(isCreating || platformCode.isEmpty || targetType.isEmpty || targetId.isEmpty)
                }
            }
            .navigationTitle("新建抓取任务")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("取消") {
                        dismiss()
                    }
                }
            }
        }
    }
    
    func createTask() {
        guard !platformCode.isEmpty, !targetType.isEmpty, !targetId.isEmpty else { return }
        
        isCreating = true
        
        let request = CreateTaskRequest(
            platformCode: platformCode,
            targetType: targetType,
            targetId: targetId,
            keywords: keywords.isEmpty ? nil : keywords,
            maxCrawlCount: maxCrawlCount,
            crawlIntervalSeconds: crawlIntervalSeconds
        )
        
        viewModel.createTask(request)
    }
}
