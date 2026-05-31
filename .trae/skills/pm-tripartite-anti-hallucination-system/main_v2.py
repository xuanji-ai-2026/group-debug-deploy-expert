"""
main_v2.py - 调度师引擎V2 完整闭环版入口

使用 DispatcherEngineV2（模块七）的增强功能：
- 自动审计流程
- 自动验收流程
- 状态自动回滚
- 统计数据追踪
"""

import sys
import os
from dispatcher_update import create_dispatcher, DispatcherEngineV2
from config import CIRCUIT_BREAKER_CONFIG


def main():
    """Main entry point for the complete AI collaboration system"""
    print("=" * 80)
    print("  AI Collaboration System - Dispatcher Engine V2.0 (Complete Loop)")
    print("  Project Manager Driven Tripartite Anti-Hallucination System")
    print("=" * 80)
    print()
    
    # Get project directory
    project_dir = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
    
    print(f"  Project Directory: {project_dir}")
    print(f"  Token Limit: {CIRCUIT_BREAKER_CONFIG['token_limit_per_call']}/call")
    print(f"  Max Retries: {CIRCUIT_BREAKER_CONFIG['max_retries']}")
    print()
    
    # Initialize V2 engine with all features
    print("Initializing Dispatcher Engine V2.0...")
    print("  Loading: AuditorTools (Flake8 integration)")
    print("  Loading: AcceptorTools (PyTest sandbox)")
    print("  Loading: Statistics module")
    print("  Loading: Circuit breaker system")
    
    engine = create_dispatcher(project_dir)
    
    print("\nEngine initialization complete!")
    print(f"  Health Status: {engine._calculate_health_status()}")
    print()
    
    try:
        # Start main loop
        cycle_count = 0
        
        while True:
            cycle_count += 1
            print(f"\n{'🔄' * 40}")
            print(f"  Cycle #{cycle_count} - Full Lifecycle Management")
            print(f"{'🔄' * 40}\n")
            
            # Run complete cycle with audit and acceptance
            engine.run_cycle()
            
            # Generate and display report
            if cycle_count % 3 == 0:  # Every 3 cycles
                report = engine.generate_report()
                stats = report['statistics']
                
                print(f"\n{'📊' * 40}")
                print(f"  SYSTEM HEALTH REPORT (Cycle #{cycle_count})")
                print(f"{'📊' * 40}")
                print(f"  Status: {report['health_status'].upper()}")
                print(f"  Files Processed: {stats['files_processed']}")
                print(f"  Audits Completed: {stats['audits_completed']}")
                print(f"  Acceptances: {stats['acceptances_completed']}")
                print(f"  Rejections: {stats['rejections_to_dev']}")
                print(f"  Promotions to Verified: {stats['promotions_to_verified']}")
                print(f"  Circuit Breakers: {stats['circuit_breakers_triggered']}")
                print(f"  Active Error Fingerprints: {report['active_fingerprints']}")
                print(f"  Audit Log Entries: {len(report['recent_logs'])}")
                print(f"{'📊' * 40}\n")
            
            # Wait for user input
            print("Press Enter to continue next cycle (or Ctrl+C to exit)...")
            input()
            
    except KeyboardInterrupt:
        print("\n\n" + "=" * 80)
        print("  Dispatcher Engine V2.0 Stopped Safely")
        print("=" * 80)
        
        # Final report
        final_report = engine.generate_report()
        stats = final_report['statistics']
        
        print(f"\n  Final Statistics:")
        print(f"    Total Cycles Executed: {cycle_count}")
        print(f"    Total Files Processed: {stats['files_processed']}")
        print(f"    Total Audits: {stats['audits_completed']}")
        print(f"    Total Acceptances: {stats['acceptances_completed']}")
        print(f"    Total Rejections: {stats['rejections_to_dev']}")
        print(f"    Successful Deliveries: {stats['promotions_to_verified']}")
        print(f"    Circuit Breakers Triggered: {stats['circuit_breakers_triggered']}")
        print(f"    Audit Log Generated: {len(engine.audit_log)} entries")
        print(f"\n  System Health: {final_report['health_status'].upper()}")
        print("\nThank you for using the AI Collaboration System!")
        sys.exit(0)
        
    except Exception as e:
        print(f"\nFatal Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
