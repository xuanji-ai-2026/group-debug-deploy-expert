"""
main.py - 调度师的入口与闭环测试

将所有模块串联起来，形成可以实际运行的主程序入口。
"""

import sys
import os
from dispatcher import DispatcherEngine
from tools_impl import (
    replace_in_file,
    trace_call_stack,
    create_rca_report,
    create_issue_ticket,
    request_human_intervention
)
from auditor_tools import AuditorTools
from acceptor_tools import AcceptorTools
from config import ROLE_CONFIG, CIRCUIT_BREAKER_CONFIG


def main():
    """主程序入口"""
    print("=" * 70)
    print("🚀 AI 协作系统 - 调度师引擎 v1.0")
    print("   《项目经理驱动的三权分立防幻觉系统》")
    print("=" * 70)
    print()
    
    # 获取项目目录（默认当前目录）
    project_dir = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
    
    print(f"📂 项目目录: {project_dir}")
    print(f"📋 Token限制: {CIRCUIT_BREAKER_CONFIG['token_limit_per_call']}/调用")
    print(f"🔁 最大重试: {CIRCUIT_BREAKER_CONFIG['max_retries']} 次")
    print()
    
    # 初始化调度师引擎
    engine = DispatcherEngine(project_dir)
    
    # 注册工具（实际生产中会对接 LangChain 或 OpenAI Function Calling）
    tool_registry = engine.tool_registry
    
    # 注册开发者工具
    tool_registry.register("replace_in_file", {
        "function": replace_in_file,
        "roles": ["developer"],
        "description": "局部替换文件内容（原地手术）"
    })
    
    tool_registry.register("trace_call_stack", {
        "function": trace_call_stack,
        "roles": ["developer"],
        "description": "追踪函数调用栈"
    })
    
    tool_registry.register("create_rca_report", {
        "function": create_rca_report,
        "roles": ["developer"],
        "description": "提交根因分析报告"
    })
    
    tool_registry.register("request_human_intervention", {
        "function": request_human_intervention,
        "roles": ["developer"],
        "description": "请求人类介入"
    })
    
    # 注册审计员工具
    tool_registry.register("run_lint_check", {
        "function": lambda fp: AuditorTools(project_dir).run_lint_check(fp),
        "roles": ["auditor"],
        "description": "运行静态代码分析"
    })
    
    tool_registry.register("create_issue_ticket", {
        "function": create_issue_ticket,
        "roles": ["auditor"],
        "description": "生成缺陷工单"
    })
    
    # 注册验收官工具
    tool_registry.register("run_unit_tests", {
        "function": lambda fp: AcceptorTools(project_dir).run_unit_tests(fp),
        "roles": ["acceptor"],
        "description": "执行单元测试"
    })
    
    tool_registry.register("check_coverage", {
        "function": lambda fp: AcceptorTools(project_dir).check_coverage(fp),
        "roles": ["acceptor"],
        "description": "检查测试覆盖率"
    })
    
    print("✅ 工具注册完成")
    print()
    
    try:
        # 开启死循环监听
        cycle_count = 0
        
        while True:
            cycle_count += 1
            print(f"\n{'🔄' * 35}")
            print(f"  第 {cycle_count} 轮调度周期")
            print(f"{'🔄' * 35}\n")
            
            # 执行一轮完整的调度循环
            engine.run_cycle()
            
            # 显示统计信息
            print("\n📊 本轮统计:")
            print(f"   受管文件: {len(engine.scan_managed_files())}")
            print(f"   审计日志: {len(engine.audit_log)} 条")
            print(f"   错误指纹: {len(engine.error_fingerprint_counter)} 个")
            
            # 实际应用中这里可以加个 sleep，或者通过文件系统监听事件来触发
            print("\n⏸️  按回车键继续下一轮调度（或 Ctrl+C 退出）...")
            input()
            
    except KeyboardInterrupt:
        print("\n\n👋 调度师引擎已安全停止。")
        print(f"   总共执行了 {cycle_count} 轮调度周期")
        print(f"   生成了 {len(engine.audit_log)} 条审计日志")
        sys.exit(0)
        
    except Exception as e:
        print(f"\n❌ 致命错误: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
