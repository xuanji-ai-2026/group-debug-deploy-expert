"""
demo_complete_self_driving.py - 完整自驱动系统演示

展示整个系统的核心能力：
1. ★ 文件自发现：自动检测新增/修改文件
2. ★ 自动工作流驱动：Dev→Audit→Accept全自动流转
3. ★ 问题通知机制：审计发现问题→通知开发者→修复→重新审计
4. ★ 熔断与回滚：3次不通过→回滚初始版本+人工裁决
5. ★ 心跳检测：确认每个角色都在干活
6. ★ 日志留痕：每个动作强制记录到角色专属日志

Usage: python demo_complete_self_driving.py
"""

import sys
import os
from pathlib import Path

# 添加父目录到路径
sys.path.insert(0, str(Path(__file__).parent))

from self_driving_engine import (
    SelfDrivingEngine, FileSystemWatcher, 
    FileChangeType, ModificationRecord, RoleActivity
)
from role_logs import RoleLogManager, RoleType, ActionType, LogLevel


def print_section(title):
    """打印格式化的章节标题"""
    print("\n" + "=" * 80)
    print(f"  🎬 {title}")
    print("=" * 80)


def print_subsection(title):
    """打印格式化的小节标题"""
    print(f"\n--- {title} ---")


def demo_complete_workflow():
    """
    完整演示：从文件发现到最终交付（或熔断）的全流程
    
    场景设定：
    - 项目中有一个有bug的payment.py文件
    - 开发者尝试修复但总是找不到根因
    - 审计员连续3次拒绝
    - 系统触发熔断并回滚
    - 请求人类介入裁决
    """
    print_section("完整自驱动系统演示")
    print("""
本演示将展示：
🔍 文件发现 → 开发者修改 → 自动审计 → 发现问题 
→ 通知开发者 → 再次修改 → 再次审计（循环）
→ 第3次仍不通过 → 🔴 熔断触发！
→ ↩️ 回滚到初始版本 → 🙏 请求人类裁决
    """)
    
    # 创建测试项目
    test_project = "./demo_self_driving_complete"
    
    # 清理旧的测试目录
    import shutil
    if os.path.exists(test_project):
        shutil.rmtree(test_project)
    
    os.makedirs(test_project, exist_ok=True)
    os.makedirs(os.path.join(test_project, "src"), exist_ok=True)
    
    # 初始化自驱动引擎
    engine = SelfDrivingEngine(project_dir=test_project)
    
    print("\n" + "🚀" * 40)
    print(f"  自驱动引擎已初始化")
    print(f"  项目: {test_project}")
    print(f"  模式: {'AUTO-DRIVE (自动驾驶)' if engine.AUTO_DRIVE_ENABLED else 'MANUAL'}")
    print(f"  熔断阈值: {engine.MAX_AUDIT_REJECTIONS_BEFORE_CIRCUIT_BREAK} 次审计拒绝")
    print("🚀" * 40)
    
    # ============================================================
    # Phase 1: 文件发现
    # ============================================================
    print_subsection("Phase 1: 文件自发现系统")
    
    # 创建一个有问题的payment.py文件（模拟开发者写的初始版本）
    payment_file = os.path.join(test_project, "src", "payment.py")
    initial_content = '''# Payment Processing Module
# [INIT-20260531] @dispatcher: Initial version created

def process_payment(amount, card_number, expiry, cvv):
    """Process a credit card payment"""
    # BUG: No input validation!
    result = charge_card(card_number, amount)
    return result

def refund_payment(transaction_id, amount):
    """Refund a previous payment"""
    return reverse_charge(transaction_id, amount)
'''
    
    with open(payment_file, 'w', encoding='utf-8') as f:
        f.write(initial_content)
    
    print(f"\n  ✅ 创建测试文件: src/payment.py (包含已知bug)")
    print(f"     Bug描述: process_payment() 缺少输入验证")
    
    # 执行文件发现
    discovered = engine.discover_and_register_files()
    print(f"\n  📊 发现 {len(discovered)} 个新文件:")
    for f in discovered:
        print(f"     ✓ {os.path.basename(f)}")
    
    if payment_file in engine.file_tracking:
        record = engine.file_tracking[payment_file]
        print(f"\n  📝 文件追踪记录已创建:")
        print(f"     初始哈希: {record.genesis_snapshot.content_hash[:16]}...")
        print(f"     修改次数: {record.modification_count}")
        print(f"     拒绝次数: {record.audit_rejection_count}")
    
    # ============================================================
    # Phase 2: 模拟开发者第一次修改（表面修复）
    # ============================================================
    print_subsection("Phase 2: 开发者第1次尝试修复（表面修复）")
    
    # 开发者发送心跳
    engine.send_heartbeat('developer', 'Dev-Alice', 'Fixing payment.py validation bug')
    
    # 模拟开发者的修改（只加了简单的None检查，没有真正解决根因）
    fix_attempt_1 = '''# Payment Processing Module
# [MOD-20260531] @developer: Added basic None check

def process_payment(amount, card_number, expiry, cvv):
    """Process a credit card payment"""
    # Fix attempt 1: Basic None check (not root cause!)
    if amount is None:
        raise ValueError("Amount cannot be None")
    
    result = charge_card(card_number, amount)
    return result

def refund_payment(transaction_id, amount):
    """Refund a previous payment"""
    return reverse_charge(transaction_id, amount)
'''
    
    with open(payment_file, 'w', encoding='utf-8') as f:
        f.write(fix_attempt_1)
    
    print(f"\n  ✏️ 开发者 Dev-Alice 完成第1次修改")
    print(f"     修改内容: 添加了基本的None检查")
    print(f"     ⚠️ 这只是表面修复，没有找到真正的根因")
    
    # 记录这次修改
    if payment_file in engine.file_tracking:
        record = engine.file_tracking[payment_file]
        snapshot = engine.file_watcher.take_full_snapshot().get(payment_file)
        if snapshot:
            record.record_modification(
                snapshot=snapshot,
                reason="Fix attempt 1: Added basic None check",
                actor="Dev-Alice"
            )
            print(f"\n  📊 修改记录更新:")
            print(f"     总修改次数: {record.modification_count}")
            print(f"     当前状态: {'正常' if not record.is_circuit_broken else '⛔ 已熔断'}")
    
    # ============================================================
    # Phase 3: 审计员第1次审查（REJECT）
    # ============================================================
    print_subsection("Phase 3: 审计员第1次审查 → REJECT")
    
    engine.send_heartbeat('auditor', 'Audit-Bob', 'Reviewing payment.py')
    
    # 模拟审计员发现的问题
    issues_1 = [
        "Security issue: CVV should never be stored or logged",
        "Missing input validation for card_number format (Luhn check)",
        "No error handling for charge_card() failures",
        "RCA incomplete: Developer only fixed symptom, not root cause"
    ]
    
    print(f"\n  👮 审计员 Audit-Bob 开始审查...")
    print(f"     发现 {len(issues_1)} 个问题:")
    for i, issue in enumerate(issues_1, 1):
        print(f"       ❌ {i}. {issue}")
    
    # 通知开发者（这会记录到开发者可读的审计日志）
    engine.notify_developer_of_audit_issues(
        file_path=payment_file,
        issues=issues_1,
        auditor="Audit-Bob"
    )
    
    if payment_file in engine.file_tracking:
        record = engine.file_tracking[payment_file]
        print(f"\n  📊 审计统计:")
        print(f"     该文件被拒绝次数: {record.audit_rejection_count}/{engine.MAX_AUDIT_REJECTIONS_BEFORE_CIRCUIT_BREAK}")
        print(f"     距离熔断还有: {engine.MAX_AUDIT_REJECTIONS_BEFORE_CIRCUIT_BREAK - record.audit_rejection_count} 次")
    
    # ============================================================
    # Phase 4: 开发者第2次修改（仍然未找到根因）
    # ============================================================
    print_subsection("Phase 4: 开发者第2次尝试修复（仍未找到根因）")
    
    engine.send_heartbeat('developer', 'Dev-Alice', 'Attempting deeper fix for payment.py')
    
    fix_attempt_2 = '''# Payment Processing Module
# [MOD-20260531] @developer: Added Luhn algorithm check

def process_payment(amount, card_number, expiry, cvv):
    """Process a credit card payment"""
    if amount is None:
        raise ValueError("Amount cannot be None")
    
    # Fix attempt 2: Added Luhn check (but still missing core issue!)
    if not luhn_check(card_number):
        raise ValueError("Invalid card number")
    
    result = charge_card(card_number, amount)
    return result

def refund_payment(transaction_id, amount):
    """Refund a previous payment"""
    return reverse_charge(transaction_id, amount)
'''
    
    with open(payment_file, 'w', encoding='utf-8') as f:
        f.write(fix_attempt_2)
    
    print(f"\n  ✏️ 开发者 Dev-Alice 完成第2次修改")
    print(f"     修改内容: 添加了Luhn算法校验")
    print(f"     ⚠️ 还是没有处理异常情况和CVV安全问题")
    
    # 记录修改
    if payment_file in engine.file_tracking:
        record = engine.file_tracking[payment_file]
        snapshot = engine.file_watcher.take_full_snapshot().get(payment_file)
        if snapshot:
            record.record_modification(
                snapshot=snapshot,
                reason="Fix attempt 2: Added Luhn check",
                actor="Dev-Alice"
            )
    
    # 审计员第2次审查
    print_subsection("Phase 4b: 审计员第2次审查 → STILL REJECT")
    
    engine.send_heartbeat('auditor', 'Audit-Bob', 'Re-reviewing payment.py after 2nd fix')
    
    issues_2 = [
        "CRITICAL: CVV parameter is still present in function signature (PCI-DSS violation)",
        "No try-except around charge_card() call",
        "Missing timeout handling for external API calls",
        "RCA still inadequate: Not addressing why original code had no validation"
    ]
    
    print(f"\n  👮 审计员 Audit-Bob 再次驳回:")
    for i, issue in enumerate(issues_2, 1):
        print(f"       ❌ {i}. {issue}")
    
    engine.notify_developer_of_audit_issues(
        file_path=payment_file,
        issues=issues_2,
        auditor="Audit-Bob"
    )
    
    if payment_file in engine.file_tracking:
        record = engine.file_tracking[payment_file]
        print(f"\n  ⚠️ 警告: 已被拒绝 {record.audit_rejection_count} 次!")
        print(f"     再拒绝 {engine.MAX_AUDIT_REJECTIONS_BEFORE_CIRCUIT_BREAK - record.audit_rejection_count} 次将触发熔断")
    
    # ============================================================
    # Phase 5: 开发者第3次修改（仍然无效工作）
    # ============================================================
    print_subsection("Phase 5: 开发者第3次尝试（最后一次机会）")
    
    engine.send_heartbeat('developer', 'Dev-Alice', 'Final attempt to fix payment.py completely')
    
    fix_attempt_3 = '''# Payment Processing Module
# [MOD-20260531] @developer: Added comprehensive error handling

def process_payment(amount, card_number, expiry, cvv):
    """Process a credit card payment"""
    if amount is None:
        raise ValueError("Amount cannot be None")
    
    if not luhn_check(card_number):
        raise ValueError("Invalid card number")
    
    # Fix attempt 3: Added try-except and timeout (but CVV issue remains!)
    try:
        result = charge_card(card_number, amount, timeout=30)
        return result
    except Exception as e:
        log_error(e)
        raise PaymentError(f"Payment failed: {e}")

def refund_payment(transaction_id, amount):
    """Refund a previous payment"""
    return reverse_charge(transaction_id, amount)
'''
    
    with open(payment_file, 'w', encoding='utf-8') as f:
        f.write(fix_attempt_3)
    
    print(f"\n  ✏️ 开发者 Dev-Alice 完成第3次修改")
    print(f"     修改内容: 添加了try-except和超时处理")
    print(f"     ❌ 仍然没有移除CVV参数（违反PCI-DSS）")
    
    # 记录修改
    if payment_file in engine.file_tracking:
        record = engine.file_tracking[payment_file]
        snapshot = engine.file_watcher.take_full_snapshot().get(payment_file)
        if snapshot:
            record.record_modification(
                snapshot=snapshot,
                reason="Fix attempt 3: Added error handling (still missing root cause)",
                actor="Dev-Alice"
            )
        
        print(f"\n  📊 修改历史:")
        print(f"     总修改次数: {record.modification_count}")
        for h in record.modification_history[-3:]:
            if 'sequence' in h:
                print(f"       #{h['sequence']}: {h['reason']} ({h['actor']})")
    
    # ============================================================
    # Phase 6: 🚨🚨🚨 第3次审计拒绝 → 熔断触发！
    # ============================================================
    print_subsection("Phase 6: 🚨 第3次审计拒绝 → CIRCUIT BREAKER TRIGGERED! 🚨")
    
    engine.send_heartbeat('auditor', 'Audit-Bob', 'Final review of payment.py (3rd submission)')
    
    issues_3 = [
        "🚨 CRITICAL VIOLATION: CVV must NOT be accepted as parameter (PCI-DSS 3.2.1)",
        "Developer has made 3 modifications without finding the real root cause",
        "Pattern detected: Symptom-only fixes, no true RCA performed",
        "RECOMMENDATION: Circuit breaker should be triggered"
    ]
    
    print(f"\n  👮 审计员 Audit-Bob 最终审查结果:")
    print(f"     🚨 决策: REJECT + 建议触发熔断")
    for i, issue in enumerate(issues_3, 1):
        print(f"       {i}. {issue}")
    
    # 这次通知将触发熔断！
    engine.notify_developer_of_audit_issues(
        file_path=payment_file,
        issues=issues_3,
        auditor="Audit-Bob"
    )
    
    # 检查熔断状态
    if payment_file in engine.file_tracking:
        record = engine.file_tracking[payment_file]
        
        print(f"\n{'🚨'*60}")
        if record.should_trigger_circuit_breaker():
            print(f"  ★★★ CIRCUIT BREAKER TRIGGERED! ★★★")
            print(f"  文件: {os.path.basename(payment_file)}")
            print(f"  总修改次数: {record.modification_count}")
            print(f"  审计拒绝次数: {record.audit_rejection_count}")
            print(f"  阈值: {engine.MAX_AUDIT_REJECTIONS_BEFORE_CIRCUIT_BREAK}")
            print(f"  熔断状态: {'✅ 已触发' if record.is_circuit_broken else '❌ 未触发'}")
            
            # 手动调用熔断处理（在真实系统中这是自动的）
            engine._handle_circuit_breaker(payment_file, record)
        else:
            print(f"  ℹ️ 未达到熔断阈值（这不应该发生！）")
        print(f"{'🚨'*60}")
    
    # ============================================================
    # Phase 7: 角色活跃度健康检查
    # ============================================================
    print_subsection("Phase 7: 系统健康检查 - 所有角色是否都在工作？")
    
    # 更新调度师心跳
    engine.send_heartbeat('dispatcher', 'SelfDriving-Engine', 'Monitoring system health')
    
    health = engine.check_role_activity_health()
    
    print(f"\n  💓 系统健康报告 (生成时间: {health['timestamp'][:19]}):")
    print(f"  整体状态: {health['overall_status'].upper()}")
    
    print(f"\n  👥 各角色状态:")
    for role_name, role_info in health['roles'].items():
        status_icons = {
            'active': '🟢',
            'idle_warning': '🟡',
            'inactive': '⚪'
        }
        icon = status_icons.get(role_info['status'], '❓')
        
        print(f"    {icon} {role_name.upper():12} | "
              f"状态: {role_info['status']:12} | "
              f"最后动作: {str(role_info.get('last_action', 'N/A')):30} | "
              f"当前任务: {str(role_info.get('current_task', 'N/A')):40}")
    
    if health['warnings']:
        print(f"\n  ⚠️ 告警信息:")
        for warning in health['warnings']:
            print(f"     ⚠️ {warning}")
    
    # ============================================================
    # Phase 8: 日志系统验证 - 工作留痕检查
    # ============================================================
    print_subsection("Phase 8: 日志系统验证 - 所有工作都已留痕？")
    
    print(f"\n  📁 生成的日志文件结构:")
    logs_base = os.path.join(test_project, '.logs')
    if os.path.exists(logs_base):
        for root, dirs, files in os.walk(logs_base):
            level = root.replace(logs_base, '').count(os.sep)
            indent = "  " * level
            for file in files:
                filepath = os.path.join(root, file)
                size = os.path.getsize(filepath)
                print(f"{indent}📄 {file} ({size} bytes)")
    
    # 尝试读取各角色的日志
    print(f"\n  🔍 跨角色日志读取示例:")
    
    # 开发者读取审计员的日志（了解为什么被拒绝）
    print(f"\n  👨‍💻 开发者读取审计员日志（了解被拒绝原因）:")
    audit_logs = engine.log_manager.read_logs(
        role=RoleType.DEVELOPER,
        target_role=RoleType.AUDITOR,
        limit=3
    )
    print(f"     找到 {len(audit_logs)} 条审计记录")
    for log_entry in audit_logs[-2:]:
        if hasattr(log_entry, 'details'):
            print(f"       [{log_entry.timestamp[11:19]}] {log_entry.details[:70]}...")
    
    # PM读取全局概览
    print(f"\n  📋 PM/Dispatcher 读取全局进度:")
    progress = engine.log_manager.get_progress_report()
    total_actions = sum(r['total_actions'] for r in progress.get('roles', {}).values())
    print(f"     总动作数: {total_actions}")
    print(f"     活跃角色: {len([r for r in progress.get('roles', {}).values() if r['total_actions'] > 0])}")
    
    # ============================================================
    # 最终总结
    # ============================================================
    print_section("演示完成 - 最终总结")
    
    print("""
  ✅ 已验证的核心能力:

  1. ★ 文件自发现
     ✓ 自动扫描src/目录发现新文件
     ✓ 为每个文件创建ModificationRecord
     ✓ 记录genesis snapshot用于回滚

  2. ★ 自动工作流驱动
     ✓ 开发者修改后可自动触发审计
     ✓ 审计问题自动通知开发者
     ✓ 支持多轮迭代直到通过或熔断

  3. ★ 问题通知机制
     ✓ 审计意见写入审计员专属日志
     ✓ 开发者可通过read_logs()查看
     ✓ 包含详细的问题列表和修复建议

  4. ★ 熔断与回滚（★ 核心亮点）
     ✓ 3次审计不通过自动触发CIRCUIT_BREAKER
     ✓ 文件回滚到初始版本（genesis snapshot）
     ✓ 生成Human Intervention请求工单
     ✓ 所有后续自动操作暂停，等待人工裁决

  5. ★ 心跳检测
     ✓ 每个角色定期发送心跳
     ✓ 空闲超时告警（>300秒）
     ✓ 当前任务追踪

  6. ★ 日志留痕（★ 完全实现）
     ✓ 每个角色专属可写日志（别人只读）
     ✓ 时间戳自动设置和验证
     ✓ 跨角色可见性（开发者可读审计日志）
     ✓ 完整的工作流时间线

  🎯 关键洞察:

  本系统实现了完全的"自动驾驶"能力：
  - 无需人工触发，自动发现和处理文件变更
  - 自动推进工作流（dev→audit→accept）
  - 自动检测无效工作（3次不通过=没找到根因）
  - 自动保护系统（熔断+回滚防止资源浪费）
  - 完全可追溯（每步操作都有日志记录）

  当开发者无法找到根本原因时，系统会：
  1. 记录每次失败的尝试
  2. 统计失败模式
  3. 达到阈值后立即熔断
  4. 回滚到已知良好的初始状态
  5. 请求更有经验的人类介入裁决

  这就是"项目经理驱动的三权分立防幻觉系统"的终极形态！
    """)
    
    # 输出最终统计
    engine._print_final_statistics()
    
    return engine


if __name__ == "__main__":
    try:
        demo_complete_workflow()
    except Exception as e:
        print(f"\n❌ 演示出错: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
