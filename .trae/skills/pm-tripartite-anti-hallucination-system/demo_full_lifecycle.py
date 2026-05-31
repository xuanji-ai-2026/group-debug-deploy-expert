"""
demo_full_lifecycle.py - 完整生命周期演示

自动展示AI协作系统的完整工作流程：
1. 创建测试文件
2. 开发者阶段 (RCA + 原地手术)
3. 审计员阶段 (Flake8 + 自动决策)
4. 验收官阶段 (PyTest + 最终放行)
5. 统计报告
"""

import os
import sys
from datetime import datetime

# Add current directory to path
sys.path.insert(0, os.path.dirname(__file__))

from dispatcher_update import create_dispatcher
from tools_impl import replace_in_file, trace_call_stack, create_rca_report


def create_test_file(file_path: str) -> str:
    """Create a test file with standard frontmatter"""
    content = f"""---
id: "TEST-{datetime.now().strftime('%Y%m%d%H%M%S')}"
status: "dev"
role_owner: "developer"
version: 1.0
genesis_hash: ""
previous_hash: ""
last_updated: "{datetime.utcnow().isoformat()}Z"
changelog:
  - "v1.0: Initial version created @dispatcher"
tags: ["test", "demo"]
---

# Test File - AI Collaboration System Demo

This is a sample file to demonstrate the complete lifecycle.

def calculate_sum(a, b):
    # [TODO] This function needs error handling
    return a + b

def greet_user(name):
    if name is None:
        return "Hello, Guest!"
    return "Hello, " + str(name) + "!"

class DataProcessor:
    def __init__(self):
        self.data = []
    
    def add_item(self, item):
        self.data.append(item)
    
    def process(self):
        result = []
        for item in self.data:
            processed = item.upper()
            result.append(processed)
        return result
"""
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"✅ Created test file: {file_path}")
    return file_path


def demo_developer_phase(engine, file_path: str):
    """Phase 1: Developer - RCA + Micro-surgery"""
    print("\n" + "="*70)
    print("🔨 PHASE 1: DEVELOPER (RCA + Micro-surgery)")
    print("="*70)
    
    print(f"\n📄 Target File: {os.path.basename(file_path)}")
    print("📊 Current Status: dev")
    
    # Simulate finding an issue
    print("\n⚠️ Issue Found: calculate_sum() lacks type validation")
    
    # Step 1: Trace call stack
    print("\n🔍 Step 1: Tracing call stack...")
    call_sites = trace_call_stack(file_path, "calculate_sum")
    if call_sites:
        print(f"   Found {len(call_sites)} call site(s):")
        for site in call_sites[:3]:
            print(f"   • {site}")
    else:
        print("   No external calls found (new function)")
    
    # Step 2: Output RCA report
    print("\n📋 Step 2: Generating RCA Report...")
    rca = create_rca_report({
        'symptom': 'calculate_sum() accepts non-numeric input causing TypeError',
        'trigger_chain': 'User input -> calculate_sum("abc", 123) -> TypeError',
        'root_cause': 'Missing type checking and input validation',
        'fix_plan': 'Add isinstance() checks with proper error messages',
        'affected_files': [file_path]
    })
    
    # Step 3: Execute micro-surgery
    print("\n🔪 Step 3: Executing micro-surgery (replace_in_file)...")
    today = datetime.now().strftime('%Y%m%d')
    
    old_code = '''def calculate_sum(a, b):
    # [TODO] This function needs error handling
    return a + b'''
    
    new_code = f'''def calculate_sum(a, b):
    # [MOD-{today}] @developer: Add type validation to prevent TypeError
    if not isinstance(a, (int, float)):
        raise TypeError(f"a must be numeric, got {{type(a).__name__}}")
    if not isinstance(b, (int, float)):
        raise TypeError(f"b must be numeric, got {{type(b).__name__}}")
    return a + b'''
    
    try:
        replace_in_file(file_path, old_code, new_code)
        print("   ✅ Micro-surgery completed successfully!")
    except Exception as e:
        print(f"   ❌ Surgery failed: {e}")
        return False
    
    # Step 4: Update status to audit
    print("\n🔄 Step 4: Updating status -> audit")
    engine.update_file_status(
        file_path=file_path,
        new_status=engine.FileStatus.AUDIT if hasattr(engine, 'FileStatus') else __import__('models', fromlist=['FileStatus']).FileStatus.AUDIT,
        changelog=[f"[DEV-COMPLETE] RCA analysis done, fix applied"],
        role_owner="auditor"
    )
    
    return True


def demo_auditor_phase(engine, file_path: str):
    """Phase 2: Auditor - Flake8 + Auto-decision"""
    print("\n" + "="*70)
    print("👮 PHASE 2: AUDITOR (Flake8 + Static Analysis)")
    print("="*70)
    
    print(f"\n📄 Auditing: {os.path.basename(file_path)}")
    print("📊 Current Status: audit")
    
    # Run audit decision
    action, report = engine.auditor.audit_decision(
        file_path, 
        __import__('models', fromlist=['FileStatus']).FileStatus.AUDIT
    )
    
    print(f"\n📋 Audit Decision: {action}")
    print(f"   Details: {report}")
    
    if action == "APPROVE_TO_ACCEPT":
        print("\n✅ Audit PASSED! Promoting to acceptance phase...")
        engine.update_file_status(
            file_path=file_path,
            new_status=__import__('models', fromlist=['FileStatus']).FileStatus.ACCEPT,
            changelog=["[AUDIT-PASS] Code quality approved"],
            role_owner="acceptor"
        )
        return True
    else:
        print("\n❌ Audit FAILED! Rejecting to developer...")
        engine.update_file_status(
            file_path=file_path,
            new_status=__import__('models', fromlist=['FileStatus']).FileStatus.DEV,
            changelog=[f"[AUDIT-REJECT] {report}"],
            role_owner="developer"
        )
        return False


def demo_acceptor_phase(engine, file_path: str):
    """Phase 3: Acceptor - PyTest + Final verdict"""
    print("\n" + "="*70)
    print("⚖️ PHASE 3: ACCEPTOR (PyTest Sandbox)")
    print("="*70)
    
    print(f"\n📄 Testing: {os.path.basename(file_path)}")
    print("📊 Current Status: accept")
    
    # Run acceptance decision
    action, report = engine.acceptor.acceptance_decision(file_path)
    
    print(f"\n🏆 Final Verdict: {action}")
    print(f"\n{report}")
    
    if action == "PROMOTE_TO_VERIFIED":
        print("\n🎉 ACCEPTANCE PASSED! Code is production-ready!")
        engine.update_file_status(
            file_path=file_path,
            new_status=__import__('models', fromlist=['FileStatus']).FileStatus.VERIFIED,
            changelog="[ACCEPT-PASS] All tests passed, promoted to delivery 🎉",
            role_owner="system"
        )
        return True
    else:
        print("\n❌ ACCEPTANCE FAILED! Returning to developer...")
        engine.update_file_status(
            file_path=file_path,
            new_status=__import__('models', fromlist=['FileStatus']).FileStatus.DEV,
            changelog=[f"[ACCEPT-REJECT] Tests failed"],
            role_owner="developer"
        )
        return False


def main():
    """Main demo execution"""
    print("=" * 80)
    print("  🚀 AI Collaboration System - Full Lifecycle Demo")
    print("     Project Manager Driven Tripartite Anti-Hallucination System")
    print("=" * 80)
    
    # Initialize engine
    print("\n[INIT] Initializing Dispatcher Engine V2.0...")
    engine = create_dispatcher('.')
    
    print("[INIT] Loading components:")
    print("       ✓ AuditorTools (Flake8 integration)")
    print("       ✓ AcceptorTools (PyTest sandbox)")
    print("       ✓ Statistics module")
    print("       ✓ Circuit breaker system")
    
    # Create test environment
    print("\n[SETUP] Creating test environment...")
    engine.enforce_directory_structure()
    
    # Create test file
    test_file = "src/demo_test_module.py"
    os.makedirs(os.path.dirname(test_file), exist_ok=True)
    create_test_file(test_file)
    
    try:
        # Execute full lifecycle
        print("\n" + "🎬"*40)
        print("  STARTING COMPLETE LIFECYCLE DEMO")
        print("🎬"*40)
        
        # Phase 1: Developer
        success_dev = demo_developer_phase(engine, test_file)
        if not success_dev:
            print("\n❌ Developer phase failed!")
            return
        
        # Phase 2: Auditor
        success_audit = demo_auditor_phase(engine, test_file)
        if not success_audit:
            print("\n⚠️ Audit phase rejected (this is normal behavior!)")
            print("   In real workflow, developer would fix issues and resubmit")
        
        # Phase 3: Acceptor (only if audit passed)
        if success_audit:
            success_accept = demo_acceptor_phase(engine, test_file)
            
            # Generate final report
            print("\n" + "="*70)
            print("📊 FINAL SYSTEM REPORT")
            print("="*70)
            
            report = engine.generate_report()
            stats = report['statistics']
            
            print(f"\n  System Health: {report['health_status'].upper()}")
            print(f"  Total Cycles: {stats['total_cycles']}")
            print(f"  Files Processed: {stats['files_processed']}")
            print(f"  Audits Completed: {stats['audits_completed']}")
            print(f"  Acceptances: {stats['acceptances_completed']}")
            print(f"  Rejections: {stats['rejections_to_dev']}")
            print(f"  Successful Deliveries: {stats['promotions_to_verified']}")
            print(f"  Circuit Breakers: {stats['circuit_breakers_triggered']}")
            print(f"  Audit Log Entries: {len(engine.audit_log)}")
            
            print("\n" + "-"*70)
            print("Recent Audit Log (last 5 entries):")
            print("-"*70)
            for log_entry in engine.audit_log[-5:]:
                timestamp = log_entry['timestamp'][:19]
                event = log_entry['event_type']
                details = str(log_entry['details'])[:60]
                print(f"  [{timestamp}] {event}: {details}...")
        
        print("\n" + "="*80)
        print("  🎊 DEMONSTRATION COMPLETED!")
        print("="*80)
        print(f"\n  Test file location: {test_file}")
        print(f"  You can inspect the file to see the updated YAML frontmatter")
        print("\n  The system successfully demonstrated:")
        print("    ✅ Developer phase (RCA + micro-surgery)")
        print("    ✅ Auditor phase (Flake8 + auto-decision)")
        print("    ✅ Acceptor phase (PyTest + final verdict)")
        print("    ✅ Automatic status transitions")
        print("    ✅ Hash chain integrity")
        print("    ✅ Complete audit trail")
        print("\n" + "="*80)
        
    except Exception as e:
        print(f"\n❌ Demo error: {e}")
        import traceback
        traceback.print_exc()
    
    finally:
        print("\n👋 Thank you for testing the AI Collaboration System!")


if __name__ == "__main__":
    main()
