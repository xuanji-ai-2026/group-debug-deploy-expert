//
//  RechargeHistoryView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  充值记录页面
//
//  Created by Zhou Jie (EMP-IOS-002) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 充值记录页面
/// 
/// 展示用户的积分充值历史记录，支持分页加载和筛选。
struct RechargeHistoryView: View {
    
    // MARK: - 属性
    
    /// 视图模型
    @ObservedObject var viewModel: RechargeViewModel
    
    /// 环境变量，用于关闭页面
    @Environment(\.dismiss) private var dismiss
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ZStack {
                List {
                    if viewModel.rechargeHistory.isEmpty && !viewModel.isLoading {
                        // 空状态
                        emptyStateSection
                    } else {
                        // 记录列表
                        recordsSection
                        
                        // 加载更多
                        if viewModel.hasMoreHistory {
                            loadMoreSection
                        }
                    }
                }
                .listStyle(.plain)
                
                // 加载指示器
                if viewModel.isLoading && viewModel.rechargeHistory.isEmpty {
                    LoadingView()
                }
            }
            .navigationTitle("充值记录")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("关闭") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        Task {
                            await viewModel.refreshHistory()
                        }
                    }) {
                        Image(systemName: "arrow.clockwise")
                    }
                    .disabled(viewModel.isLoading)
                }
            }
            .alert("错误", isPresented: $viewModel.showError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "加载失败")
            }
        }
        .task {
            await viewModel.loadRechargeHistory()
        }
    }
    
    // MARK: - 空状态
    
    private var emptyStateSection: some View {
        Section {
            VStack(spacing: 16) {
                Image(systemName: "doc.text.magnifyingglass")
                    .font(.system(size: 60))
                    .foregroundColor(.secondary)
                
                Text("暂无充值记录")
                    .font(.headline)
                    .foregroundColor(.primary)
                
                Text("您还没有进行过积分充值")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 80)
            .listRowBackground(Color.clear)
        }
    }
    
    // MARK: - 记录列表
    
    private var recordsSection: some View {
        Section {
            ForEach(viewModel.rechargeHistory) { order in
                RechargeRecordRow(order: order)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
            }
        } header: {
            HStack {
                Text("共 \(viewModel.rechargeHistory.count) 条记录")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Spacer()
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
            .background(Color(.systemBackground))
        }
    }
    
    // MARK: - 加载更多
    
    private var loadMoreSection: some View {
        Section {
            HStack {
                Spacer()
                
                if viewModel.isLoading {
                    ProgressView()
                        .scaleEffect(0.8)
                } else {
                    Button(action: {
                        Task {
                            await viewModel.loadMoreHistory()
                        }
                    }) {
                        Text("加载更多")
                            .font(.subheadline)
                            .foregroundColor(.blue)
                    }
                }
                
                Spacer()
            }
            .padding()
            .listRowBackground(Color.clear)
        }
    }
}

// MARK: - 充值记录行

struct RechargeRecordRow: View {
    let order: RechargeOrder
    
    var body: some View {
        VStack(spacing: 12) {
            HStack {
                // 左侧：图标和基本信息
                HStack(spacing: 12) {
                    // 状态图标
                    ZStack {
                        Circle()
                            .fill(statusColor.opacity(0.15))
                            .frame(width: 44, height: 44)
                        
                        Image(systemName: statusIcon)
                            .font(.system(size: 20))
                            .foregroundColor(statusColor)
                    }
                    
                    // 信息
                    VStack(alignment: .leading, spacing: 4) {
                        Text(order.packageName)
                            .font(.subheadline)
                            .fontWeight(.medium)
                        
                        HStack(spacing: 4) {
                            Text(order.createdAt.formatted(date: .numeric, time: .omitted))
                                .font(.caption)
                                .foregroundColor(.secondary)
                            
                            if let method = order.paymentMethod {
                                Text("·")
                                    .foregroundColor(.secondary)
                                
                                Text(method.name)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
                
                Spacer()
                
                // 右侧：金额和积分
                VStack(alignment: .trailing, spacing: 4) {
                    HStack(alignment: .firstTextBaseline, spacing: 2) {
                        Text("¥")
                            .font(.caption)
                        Text(order.displayPrice)
                            .font(.subheadline)
                            .fontWeight(.semibold)
                    }
                    
                    Text("+\(order.points + order.bonusPoints) 积分")
                        .font(.caption)
                        .foregroundColor(.green)
                }
            }
            
            // 底部：订单号和状态
            HStack {
                Text("订单号: \(order.orderNo)")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                
                Spacer()
                
                // 状态标签
                Text(order.status.description)
                    .font(.caption2)
                    .fontWeight(.medium)
                    .foregroundColor(statusColor)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(statusColor.opacity(0.15))
                    .cornerRadius(4)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
    
    // 状态颜色
    private var statusColor: Color {
        Color(hex: order.status.color) ?? .gray
    }
    
    // 状态图标
    private var statusIcon: String {
        switch order.status {
        case .pending:
            return "clock"
        case .processing:
            return "arrow.clockwise"
        case .paid, .completed:
            return "checkmark.circle.fill"
        case .cancelled:
            return "xmark.circle.fill"
        case .refunded:
            return "arrow.uturn.backward.circle.fill"
        case .failed:
            return "exclamationmark.circle.fill"
        }
    }
}

// MARK: - 扩展

extension RechargeOrder {
    var displayPrice: String {
        String(format: "%.2f", Double(amount) / 100)
    }
}

// MARK: - 预览

#if DEBUG
struct RechargeHistoryView_Previews: PreviewProvider {
    static var previews: some View {
        RechargeHistoryView(viewModel: RechargeViewModel.preview)
    }
}
#endif
