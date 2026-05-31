//
//  RechargeView.swift
//  BxApp
//
//  北极星AI商机获客系统 - iOS客户端
//  积分充值页面
//
//  Created by Zhou Jie (EMP-IOS-002) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import SwiftUI

/// 积分充值页面
/// 
/// 提供积分充值套餐选择、支付方式选择、订单创建等功能。
struct RechargeView: View {
    
    // MARK: - 状态
    
    /// 视图模型
    @StateObject private var viewModel = RechargeViewModel()
    
    /// 是否显示历史记录
    @State private var showHistoryView = false
    
    /// 是否显示支付确认
    @State private var showPaymentConfirm = false
    
    // MARK: - 界面
    
    var body: some View {
        NavigationView {
            ZStack {
                ScrollView {
                    VStack(spacing: 20) {
                        // 积分余额卡片
                        pointsBalanceCard
                        
                        // 充值套餐
                        packagesSection
                        
                        // 支付方式
                        paymentMethodsSection
                        
                        // 充值说明
                        rechargeInfoSection
                    }
                    .padding()
                }
                
                // 加载指示器
                if viewModel.isLoading {
                    LoadingView()
                }
            }
            .navigationTitle("积分充值")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        showHistoryView = true
                    }) {
                        Image(systemName: "clock.arrow.circlepath")
                            .font(.title3)
                    }
                }
            }
            .sheet(isPresented: $showHistoryView) {
                RechargeHistoryView(viewModel: viewModel)
            }
            .alert("确认支付", isPresented: $showPaymentConfirm) {
                Button("取消", role: .cancel) {}
                Button("确认支付") {
                    Task {
                        await viewModel.startPayment()
                    }
                }
            } message: {
                if let package = viewModel.selectedPackage {
                    Text("确认支付 ¥\(package.displayPrice) 购买 \(package.totalPoints) 积分？")
                }
            }
            .alert("支付成功", isPresented: $viewModel.showPaySuccess) {
                Button("确定", role: .cancel) {}
            } message: {
                if let order = viewModel.currentOrder {
                    Text("成功充值 \(order.points + order.bonusPoints) 积分！")
                }
            }
            .alert("错误", isPresented: $viewModel.showError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "操作失败")
            }
        }
        .task {
            await viewModel.loadPackages()
        }
    }
    
    // MARK: - 积分余额卡片
    
    private var pointsBalanceCard: some View {
        VStack(spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("当前积分余额")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    Text("\(viewModel.currentPoints)")
                        .font(.system(size: 42, weight: .bold))
                        .foregroundColor(.primary)
                }
                
                Spacer()
                
                Image(systemName: "dollarsign.circle.fill")
                    .font(.system(size: 50))
                    .foregroundColor(.orange)
            }
            
            Divider()
            
            HStack {
                Label("积分可用于AI获客任务", systemImage: "info.circle")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Spacer()
            }
        }
        .padding()
        .background(
            LinearGradient(
                colors: [Color.orange.opacity(0.15), Color.yellow.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.orange.opacity(0.3), lineWidth: 1)
        )
    }
    
    // MARK: - 充值套餐
    
    private var packagesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("选择套餐")
                    .font(.headline)
                
                Spacer()
                
                if let package = viewModel.recommendedPackage {
                    Label("推荐", systemImage: "star.fill")
                        .font(.caption)
                        .foregroundColor(.orange)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.orange.opacity(0.15))
                        .cornerRadius(4)
                }
            }
            
            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 12) {
                ForEach(viewModel.sortedPackages) { package in
                    PackageCard(
                        package: package,
                        isSelected: viewModel.selectedPackage?.id == package.id
                    )
                    .onTapGesture {
                        viewModel.selectPackage(package)
                    }
                }
            }
        }
    }
    
    // MARK: - 支付方式
    
    private var paymentMethodsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("支付方式")
                .font(.headline)
            
            VStack(spacing: 8) {
                ForEach(viewModel.availablePaymentMethods, id: \.self) { method in
                    PaymentMethodRow(
                        method: method,
                        isSelected: viewModel.selectedPaymentMethod == method
                    )
                    .onTapGesture {
                        viewModel.selectPaymentMethod(method)
                    }
                }
            }
        }
    }
    
    // MARK: - 充值说明
    
    private var rechargeInfoSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("充值说明")
                .font(.headline)
            
            VStack(alignment: .leading, spacing: 8) {
                InfoRow2(icon: "checkmark.circle", text: "充值后积分立即到账")
                InfoRow2(icon: "checkmark.circle", text: "支持微信支付、支付宝、Apple Pay")
                InfoRow2(icon: "checkmark.circle", text: "充值金额不可退款，请确认后支付")
                InfoRow2(icon: "checkmark.circle", text: "如有问题请联系客服")
            }
            
            // 充值按钮
            Button(action: {
                showPaymentConfirm = true
            }) {
                HStack {
                    if viewModel.isLoading {
                        ProgressView()
                            .scaleEffect(0.8)
                            .tint(.white)
                    }
                    
                    Text(viewModel.isLoading ? "处理中..." : "立即充值")
                        .font(.headline)
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(viewModel.canCreateOrder ? Color.blue : Color.gray)
                .cornerRadius(12)
            }
            .disabled(!viewModel.canCreateOrder || viewModel.isLoading)
            .padding(.top, 8)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

// MARK: - 套餐卡片

struct PackageCard: View {
    let package: RechargePackage
    let isSelected: Bool
    
    var body: some View {
        VStack(spacing: 8) {
            // 标签
            HStack {
                Spacer()
                
                if package.isHot {
                    Text("HOT")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.red)
                        .cornerRadius(4)
                }
            }
            
            // 积分数量
            Text("\(package.totalPoints)")
                .font(.title)
                .fontWeight(.bold)
            
            Text("积分")
                .font(.caption)
                .foregroundColor(.secondary)
            
            // 赠送标签
            if package.bonusPoints > 0 {
                Text("送\(package.bonusPoints)")
                    .font(.caption2)
                    .fontWeight(.medium)
                    .foregroundColor(.orange)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.orange.opacity(0.15))
                    .cornerRadius(4)
            } else {
                Spacer()
                    .frame(height: 24)
            }
            
            Divider()
            
            // 价格
            HStack(alignment: .lastTextBaseline, spacing: 4) {
                Text("¥")
                    .font(.caption)
                Text(package.displayPrice)
                    .font(.title3)
                    .fontWeight(.bold)
            }
            .foregroundColor(isSelected ? .white : .primary)
            
            // 原价
            if package.originalPrice > package.price {
                Text("¥\(package.displayOriginalPrice)")
                    .font(.caption)
                    .strikethrough()
                    .foregroundColor(isSelected ? .white.opacity(0.7) : .secondary)
            }
        }
        .padding()
        .frame(height: 180)
        .background(isSelected ? Color.blue : Color(.secondarySystemBackground))
        .foregroundColor(isSelected ? .white : .primary)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(isSelected ? Color.blue : Color.clear, lineWidth: 2)
        )
    }
}

// MARK: - 支付方式行

struct PaymentMethodRow: View {
    let method: PaymentMethod
    let isSelected: Bool
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: method.iconName)
                .font(.title3)
                .foregroundColor(paymentColor)
                .frame(width: 40)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(method.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(method.description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            if isSelected {
                Image(systemName: "checkmark.circle.fill")
                    .font(.title3)
                    .foregroundColor(.blue)
            } else {
                Image(systemName: "circle")
                    .font(.title3)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(isSelected ? Color.blue : Color.clear, lineWidth: 1)
        )
    }
    
    private var paymentColor: Color {
        switch method {
        case .wechatPay:
            return .green
        case .alipay:
            return .blue
        case .applePay:
            return .primary
        case .bankCard:
            return .orange
        }
    }
}

// MARK: - 信息行

struct InfoRow2: View {
    let icon: String
    let text: String
    
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundColor(.green)
            
            Text(text)
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - 预览

#if DEBUG
struct RechargeView_Previews: PreviewProvider {
    static var previews: some View {
        RechargeView()
    }
}
#endif
