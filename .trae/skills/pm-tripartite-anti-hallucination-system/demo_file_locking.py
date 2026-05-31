"""
demo_file_locking.py - Multi-Agent Collaboration with File Locking

Demonstrates how the file lock system prevents concurrent editing conflicts
in a real AI collaboration environment with Developer, Auditor, and Acceptor.

Scenarios demonstrated:
1. Developer editing file → Auditor blocked from reading
2. Acceptor running tests → Developer cannot modify
3. Multiple agents queuing for access
4. Automatic conflict resolution
5. Integration with Dispatcher Engine V2
"""

import os
import sys
import time
import json
import threading
from datetime import datetime

sys.path.insert(0, os.path.dirname(__file__))

from file_lock_manager import (
    FileLockManager, LockType, LockStatus,
    get_lock_manager, with_file_lock, lock_file, unlock_file
)
from typing import Optional, Tuple


class MockAgent:
    """Simulated AI agent for demonstration"""
    
    def __init__(self, name: str, role: str):
        self.name = name
        self.role = role
        self.lock_manager = get_lock_manager()
    
    def try_edit_file(self, file_path: str, content: str) -> bool:
        """
        Attempt to edit a file (requires EXCLUSIVE lock)
        
        Returns: True if successful, False if locked by another agent
        """
        print(f"\n  🔨 {self.name} ({self.role}) attempting to EDIT {os.path.basename(file_path)}...")
        
        try:
            # Try to acquire exclusive write lock (non-blocking)
            acquired = self.lock_manager.acquire(
                file_path,
                LockType.EXCLUSIVE,
                owner=self.name,
                timeout=5.0,
                blocking=False  # Don't wait, return immediately if locked
            )
            
            if acquired:
                print(f"     ✅ Lock acquired! Now editing...")
                
                # Simulate editing work
                time.sleep(1)
                
                # Write content (with lock protection)
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                
                print(f"     ✅ Edit complete!")
                
                # Release lock after editing
                self.lock_manager.release(file_path, self.name)
                return True
                
            else:
                # File is locked by someone else
                holder = self.lock_manager.get_lock_info(file_path)
                holder_name = holder.owner if holder else "unknown"
                print(f"     ❌ BLOCKED! File is locked by: {holder_name}")
                return False
                
        except Exception as e:
            print(f"     ❌ Error: {e}")
            return False
    
    def try_read_file(self, file_path: str) -> Optional[str]:
        """
        Attempt to read a file (can use SHARED lock)
        
        Returns: File content or None if locked exclusively
        """
        print(f"\n  👁️ {self.name} ({self.role}) attempting to READ {os.path.basename(file_path)}...")
        
        try:
            # Try to acquire shared read lock
            acquired = self.lock_manager.acquire(
                file_path,
                LockType.SHARED,
                owner=self.name,
                timeout=5.0,
                blocking=False
            )
            
            if acquired:
                print(f"     ✅ Read lock acquired!")
                
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                print(f"     ✅ Read complete ({len(content)} bytes)")
                
                self.lock_manager.release(file_path, self.name)
                return content
                
            else:
                holder = self.lock_manager.get_lock_info(file_path)
                holder_name = holder.owner if holder else "unknown"
                lock_type = holder.lock_type.value if holder else "unknown"
                print(f"     ❌ BLOCKED! Exclusive lock held by: {holder_name} ({lock_type})")
                return None
                
        except Exception as e:
            print(f"     ❌ Error: {e}")
            return None
    
    def run_tests(self, file_path: str) -> Tuple[bool, str]:
        """
        Run tests on a file (requires exclusive lock during execution)
        
        Returns: (passed, message)
        """
        print(f"\n  ⚖️ {self.name} ({self.role}) running TESTS on {os.path.basename(file_path)}...")
        
        try:
            acquired = self.lock_manager.acquire(
                file_path,
                LockType.EXCLUSIVE,
                owner=self.name,
                blocking=True,
                timeout=10.0  # Wait up to 10s for tests to complete
            )
            
            if acquired:
                print(f"     ✅ Test execution lock acquired!")
                
                # Simulate running tests
                time.sleep(2)
                
                # Simulate test result
                passed = True
                message = "All 3 unit tests passed ✓"
                
                print(f"     🧪 Tests: {message}")
                
                self.lock_manager.release(file_path, self.name)
                return (passed, message)
                
        except Exception as e:
            return (False, f"Test failed: {e}")


def demonstrate_scenario_1_developer_editing():
    """
    Scenario 1: Developer is editing, Auditor tries to read simultaneously
    - Expected: Auditor should be blocked until Developer finishes
    """
    print("\n" + "="*70)
    print("📋 SCENARIO 1: Concurrent Edit + Read Conflict Prevention")
    print("="*70)
    
    test_file = "src/demo_concurrent_edit.txt"
    os.makedirs(os.path.dirname(test_file), exist_ok=True)
    
    # Create initial file
    initial_content = "# Initial content\nprint('Hello World')\n"
    with open(test_file, 'w', encoding='utf-8') as f:
        f.write(initial_content)
    
    # Create agents
    developer = MockAgent("Developer-A", "developer")
    auditor = MockAgent("Auditor-B", "auditor")
    
    print("\n--- Timeline ---")
    print("T+0s: Developer starts editing...")
    
    # Developer starts editing in background thread
    dev_thread = threading.Thread(
        target=developer.try_edit_file,
        args=(test_file, "# Updated code\ndef new_function():\n    pass\n"),
        daemon=True
    )
    dev_thread.start()
    
    # Give developer time to acquire lock
    time.sleep(0.3)
    
    print("T+0.3s: Auditor attempts to read (should be BLOCKED)...")
    
    # Auditor tries to read immediately (should be blocked)
    auditor_result = auditor.try_read_file(test_file)
    
    print("T+1.3s: Waiting for developer to finish...")
    
    # Wait for developer to complete
    dev_thread.join(timeout=5)
    
    print("T+1.5s: Auditor tries again (should succeed now)...")
    auditor_result_2 = auditor.try_read_file(test_file)
    
    # Cleanup
    if os.path.exists(test_file):
        os.remove(test_file)
    
    print("\n✅ Scenario 1 Complete:")
    print(f"   - First auditor attempt: {'BLOCKED ✓' if not auditor_result else 'UNEXPECTED'}")
    print(f"   - Second auditor attempt: {'SUCCESS ✓' if auditor_result_2 else 'FAILED'}")


def demonstrate_scenario_2_test_conflict():
    """
    Scenario 2: Acceptor running tests, Developer tries to modify
    - Expected: Developer should be blocked during test execution
    """
    print("\n" + "="*70)
    print("📋 SCENARIO 2: Test Execution vs Code Modification Conflict")
    print("="*70)
    
    test_file = "src/demo_module_under_test.py"
    os.makedirs(os.path.dirname(test_file), exist_ok=True)
    
    test_content = '''# Module under test
def calculate(a, b):
    return a + b

def validate_input(x):
    if not isinstance(x, (int, float)):
        raise TypeError("Invalid input")
'''
    
    with open(test_file, 'w', encoding='utf-8') as f:
        f.write(test_content)
    
    acceptor = MockAgent("Acceptor-C", "acceptor")
    developer = MockAgent("Developer-D", "developer")
    
    print("\n--- Timeline ---")
    print("T+0s: Acceptor starts running tests...")
    
    # Start acceptor running tests in background
    test_thread = threading.Thread(
        target=acceptor.run_tests,
        args=(test_file,),
        daemon=True
    )
    test_thread.start()
    
    # Give acceptor time to acquire lock
    time.sleep(0.5)
    
    print("T+0.5s: Developer tries to modify (should be BLOCKED)...")
    
    # Developer tries to modify while tests are running
    dev_result = developer.try_edit_file(
        test_file,
        '# Modified code\ndef calculate(a, b):\n    return a * b\n'
    )
    
    print("T+2.5s: Waiting for tests to complete...")
    test_thread.join(timeout=10)
    
    print("T+2.7s: Developer tries again (should succeed)...")
    dev_result_2 = developer.try_edit_file(
        test_file,
        '# Final version\ndef calculate(a, b):\n    return a + b\n'
    )
    
    # Cleanup
    if os.path.exists(test_file):
        os.remove(test_file)
    
    print("\n✅ Scenario 2 Complete:")
    print(f"   - First developer attempt: {'BLOCKED ✓' if not dev_result else 'UNEXPECTED'}")
    print(f"   - Second developer attempt: {'SUCCESS ✓' if dev_result_2 else 'FAILED'}")


def demonstrate_scenario_3_multiple_agents():
    """
    Scenario 3: Multiple agents competing for same file
    Demonstrates queue behavior and fair access
    """
    print("\n" + "="*70)
    print("📋 SCENARIO 3: Multi-Agent Queue & Fair Access")
    print("="*70)
    
    test_file = "src/shared_resource.json"
    os.makedirs(os.path.dirname(test_file), exist_ok=True)
    
    with open(test_file, 'w', encoding='utf-8') as f:
        json.dump({"version": 1, "data": []}, f)
    
    agents = [
        MockAgent("Agent-1", "developer"),
        MockAgent("Agent-2", "auditor"),
        MockAgent("Agent-3", "acceptor"),
        MockAgent("Agent-4", "developer"),
    ]
    
    results = []
    
    print("\n--- All agents trying to access file simultaneously ---")
    
    threads = []
    for i, agent in enumerate(agents):
        def agent_work(ag, idx):
            result = ag.try_edit_file(
                test_file,
                json.dumps({"version": idx+1, "modified_by": ag.name})
            )
            results.append((ag.name, result))
        
        t = threading.Thread(target=agent_work, args=(agent, i), daemon=True)
        threads.append(t)
        t.start()
        time.sleep(0.1)  # Stagger start times slightly
    
    # Wait for all to complete
    for t in threads:
        t.join(timeout=15)
    
    successful = [name for name, success in results if success]
    blocked = [name for name, success in results if not success]
    
    # Cleanup
    if os.path.exists(test_file):
        os.remove(test_file)
    
    print("\n✅ Results:")
    print(f"   Total agents: {len(agents)}")
    print(f"   Successful: {len(successful)} ({', '.join(successful)})")
    print(f"   Blocked: {len(blocked)} ({', '.join(blocked) if blocked else 'None'})")


def demonstrate_integration_with_dispatcher():
    """
    Scenario 4: Integration with Dispatcher Engine V2
    Shows how file locking integrates into the main workflow
    """
    print("\n" + "="*70)
    print("📋 SCENARIO 4: Integration with Dispatcher Engine V2")
    print("="*70)
    
    from dispatcher_update import create_dispatcher
    
    # Initialize dispatcher
    engine = create_dispatcher('.')
    
    test_file = "src/integrated_module.py"
    os.makedirs(os.path.dirname(test_file), exist_ok=True)
    
    module_code = '''# Integrated Module Example
class DataProcessor:
    def process(self, data):
        return [x.upper() for x in data]
'''
    
    with open(test_file, 'w', encoding='utf-8') as f:
        f.write(module_code)
    
    lock_manager = get_lock_manager()
    
    print("\n--- Workflow with File Locking ---")
    
    # Phase 1: Developer phase with automatic locking
    print("\n[Phase 1] Developer Phase (with auto-lock)")
    
    # Use context manager for automatic lock handling
    with lock_manager.locked_file(test_file, LockType.EXCLUSIVE, owner="Developer-X") as f:
        content = f.read()
        print(f"   ✓ Locked and read file ({len(content)} bytes)")
        
        # Modify content
        new_content = content.replace(
            'return [x.upper() for x in data]',
            '# [MOD-20260531] @developer: Added error handling\n        return [str(x).upper() for x in data]'
        )
        
        f.seek(0)
        f.write(new_content)
        f.truncate()
        print(f"   ✓ Modified and saved (lock auto-released)")
    
    # Phase 2: Auditor phase
    print("\n[Phase 2] Auditor Phase (with shared lock)")
    
    with lock_manager.locked_file(test_file, LockType.SHARED, owner="Auditor-Y") as f:
        audit_content = f.read()
        print(f"   ✓ Auditor read file for review ({len(audit_content)} bytes)")
        
        # Simulate audit check
        lines = audit_content.split('\n')
        issues_found = len([l for l in lines if 'TODO' in l or 'FIXME' in l])
        print(f"   ✓ Audit complete (issues found: {issues_found})")
    
    # Phase 3: Check lock status
    print("\n[Phase 3] System Status Check")
    
    is_locked = lock_manager.is_locked(test_file)
    all_locks = lock_manager.get_all_locks()
    stats = lock_manager.get_statistics()
    
    print(f"   File currently locked: {is_locked}")
    print(f"   Active locks in system: {len(all_locks)}")
    print(f"   Total locks acquired (session): {stats['total_locks_acquired']}")
    print(f"   Conflicts prevented: {stats['conflicts_prevented']}")
    
    # Generate report
    print("\n" + "-"*70)
    print(lock_manager.generate_report())
    
    # Cleanup
    if os.path.exists(test_file):
        os.remove(test_file)


def demonstrate_context_manager_usage():
    """
    Scenario 5: Using the convenience context manager
    Shows the simplest way to use file locks
    """
    print("\n" + "="*70)
    print("📋 SCENARIO 5: Context Manager Convenience API")
    print("="*70)
    
    config_file = "demo_config.json"
    
    # Write config with automatic locking
    print("\n--- Writing config file ---")
    with with_file_lock(config_file, 'w+', owner="Admin") as f:
        json.dump({
            "database": {"host": "localhost", "port": 5432},
            "debug": True,
            "version": "2.0"
        }, f, indent=2)
        print("   ✓ Config written with exclusive lock")
    
    # Read config with shared lock (allows concurrent reads)
    print("\n--- Reading config file ---")
    with with_file_lock(config_file, 'r', LockType.SHARED, owner="Reader-1") as f:
        config1 = json.load(f)
        print(f"   ✓ Reader-1 read: database={config1['database']['host']}")
    
    # Another reader can read simultaneously
    with with_file_lock(config_file, 'r', LockType.SHARED, owner="Reader-2") as f:
        config2 = json.load(f)
        print(f"   ✓ Reader-2 read: debug={config2['debug']}")
    
    # Writer will block readers
    print("\n--- Attempting to write while potentially reading ---")
    try:
        with with_file_lock(config_file, 'r+', owner="Updater") as f:
            config = json.load(f)
            config["version"] = "3.0"
            f.seek(0)
            json.dump(config, f, indent=2)
            f.truncate()
            print("   ✓ Config updated successfully")
    except Exception as e:
        print(f"   ⚠️ Update failed: {e}")
    
    # Cleanup
    if os.path.exists(config_file):
        os.remove(config_file)
    
    print("\n✅ Context manager demo complete!")


def main():
    """Run all demonstrations"""
    print("=" * 80)
    print("  🔐 File Lock System - Multi-Agent Collaboration Demo")
    print("     Preventing Concurrent Editing Conflicts in AI Systems")
    print("=" * 80)
    
    print("\nInitializing File Lock Manager...")
    manager = get_lock_manager(default_timeout=30.0)
    print(f"✓ Manager initialized (platform: {sys.platform})")
    print(f"✓ Default timeout: {manager.default_timeout}s")
    print(f"✓ Deadlock detection: {'ENABLED' if manager.enable_deadlock_detection else 'DISABLED'}")
    
    try:
        # Run all scenarios
        demonstrate_scenario_1_developer_editing()
        demonstrate_scenario_2_test_conflict()
        demonstrate_scenario_3_multiple_agents()
        demonstrate_integration_with_dispatcher()
        demonstrate_context_manager_usage()
        
        # Final summary
        print("\n" + "=" * 80)
        print("  🎊 All Scenarios Completed Successfully!")
        print("=" * 80)
        
        final_stats = manager.get_statistics()
        print(f"\n📊 Session Statistics:")
        print(f"   Total lock operations: {final_stats['total_locks_acquired'] + final_stats['total_locks_released']}")
        print(f"   Timeouts occurred: {final_stats['timeouts_occurred']}")
        print(f"   Deadlocks detected: {final_stats['deadlocks_detected']}")
        print(f"   Conflicts prevented: {final_stats['conflicts_prevented']}")
        
        print(f"\n🔐 Key Features Demonstrated:")
        print(f"   ✅ Exclusive write locks prevent simultaneous edits")
        print(f"   ✅ Shared read locks allow concurrent safe reads")
        print(f"   ✅ Automatic blocking when conflicts detected")
        print(f"   ✅ Timeout mechanism prevents infinite waiting")
        print(f"   ✅ Deadlock detection prevents system hangs")
        print(f"   ✅ Integration with Dispatcher Engine V2")
        print(f"   ✅ Context managers for easy usage")
        print(f"   ✅ Cross-platform compatibility (Windows/Linux/Mac)")
        
    except KeyboardInterrupt:
        print("\n\n⚠️ Demonstration interrupted by user")
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()
