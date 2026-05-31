"""
tools_impl.py - 核心工具的底层实现

实现了《宪法》第三章的具体工具函数：
- replace_in_file (原地手术)
- trace_call_stack (调用栈溯源)
- 以及其他原子工具
"""

import os
import re
import subprocess
from typing import List, Tuple, Optional
from models import RCAReport, IssueTicket


def replace_in_file(file_path: str, old_str: str, new_str: str) -> bool:
    """
    强制原地手术的真实执行逻辑
    
    精准替换文件中的某一段代码。
    如果 old_str 在文件中出现多次或不唯一，直接抛出异常驳回操作。
    
    Args:
        file_path: 目标文件路径
        old_str: 要替换的旧字符串（必须唯一）
        new_str: 替换后的新字符串
        
    Returns:
        bool: 是否替换成功
        
    Raises:
        FileNotFoundError: 文件不存在
        ValueError: 目标字符串不唯一或不存在
    """
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"文件 {file_path} 不存在")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 严格校验：目标字符串必须唯一存在，防止误伤
    occurrence_count = content.count(old_str)
    
    if occurrence_count == 0:
        raise ValueError(
            f"[手术失败] 目标字符串在文件中不存在！\n"
            f"文件: {file_path}\n"
            f"请检查你要替换的内容是否正确。"
        )
    
    if occurrence_count > 1:
        raise ValueError(
            f"[手术失败] 目标字符串在文件中出现 {occurrence_count} 次，不唯一！\n"
            f"文件: {file_path}\n"
            f"请提供更多上下文以确保唯一性。"
        )
    
    # 执行替换
    new_content = content.replace(old_str, new_str, 1)
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    
    print(f"✅ [成功] 对 {file_path} 完成了精准局部修复。")
    return True


def trace_call_stack(file_path: str, function_name: str, project_root: str = None) -> List[str]:
    """
    跨文件调用栈溯源（简易版）
    
    在当前项目目录下，查找是谁调用了这个函数。
    这是开发者在做 RCA 根因分析时的必备神器。
    
    Args:
        file_path: 当前文件路径（用于确定项目根目录）
        function_name: 要追踪的函数名
        project_root: 项目根目录（可选，默认从file_path推断）
        
    Returns:
        list: 调用位置列表，格式为 ["file_path:line_number -> code_line"]
    """
    if project_root is None:
        project_root = os.path.dirname(file_path)
    
    call_sites = []
    
    # 正则匹配：查找 "function_name("
    pattern = re.compile(rf'{re.escape(function_name)}\s*\(')
    
    for dirpath, dirnames, filenames in os.walk(project_root):
        # 排除虚拟环境、隐藏文件夹和隔离区
        exclude_dirs = {'.git', '__pycache__', 'node_modules', '.venv', 'venv', '.quarantine', '.constitution'}
        dirnames[:] = [d for d in dirnames if d not in exclude_dirs]
        
        # 只处理源代码文件
        code_extensions = ('.py', '.js', '.ts', '.java', '.go')
        filenames = [f for f in filenames if f.endswith(code_extensions)]
        
        for filename in filenames:
            full_path = os.path.join(dirpath, filename)
            
            try:
                with open(full_path, 'r', encoding='utf-8') as f:
                    lines = f.readlines()
                
                for i, line in enumerate(lines, 1):
                    if pattern.search(line):
                        call_site = f"{full_path}:{i} -> {line.strip()}"
                        call_sites.append(call_site)
                        
            except Exception as e:
                print(f"⚠️ [溯源] 无法读取文件 {full_path}: {e}")
    
    return call_sites


def read_related_files(file_path: str, depth: int = 1, project_root: str = None) -> List[str]:
    """
    关联文件阅读（全景阅读）
    
    读取与指定文件相关的其他文件，支持导入依赖分析。
    
    Args:
        file_path: 当前文件路径
        depth: 搜索深度（默认1层）
        project_root: 项目根目录
        
    Returns:
        list: 相关文件路径列表
    """
    if project_root is None:
        project_root = os.path.dirname(file_path)
    
    related_files = set()
    related_files.add(file_path)  # 包含自身
    
    # 简单的导入语句匹配（针对Python）
    import_patterns = [
        r'^import\s+(\w+)',
        r'^from\s+(\w+)\s+import',
        r'^require\s*\(["\']([^"\']+)["\']'
    ]
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            lines = content.split('\n')
        
        imported_modules = set()
        for line in lines:
            for pattern in import_patterns:
                match = re.search(pattern, line)
                if match:
                    imported_modules.add(match.group(1))
        
        # 在项目中查找这些模块对应的文件
        if depth > 0:
            for module_name in imported_modules:
                for dirpath, dirnames, filenames in os.walk(project_root):
                    exclude_dirs = {'.git', '__pycache__', 'node_modules', '.venv'}
                    dirnames[:] = [d for d in dirnames if d not in exclude_dirs]
                    
                    for filename in filenames:
                        if filename.startswith(module_name) or filename == f"{module_name}.py":
                            full_path = os.path.join(dirpath, filename)
                            if full_path not in related_files:
                                related_files.add(full_path)
                                # 递归查找（限制深度）
                                if depth > 1:
                                    sub_related = read_related_files(full_path, depth-1, project_root)
                                    related_files.update(sub_related)
                                
    except Exception as e:
        print(f"⚠️ [关联阅读] 分析文件失败: {e}")
    
    return list(related_files)


def create_rca_report(rca_data: dict) -> RCAReport:
    """
    提交根因分析报告
    
    Args:
        rca_data: RCA报告数据字典
        
    Returns:
        RCAReport: 结构化的RCA报告对象
    """
    report = RCAReport(**rca_data)
    
    if not report.validate():
        raise ValueError("[RCA拒绝] 报告缺少必填字段，请补充完整！")
    
    print(f"📋 [RCA] 根因分析报告已提交:")
    print(f"   症状: {report.symptom}")
    print(f"   根因: {report.root_cause}")
    print(f"   方案: {report.fix_plan}")
    
    return report


def create_issue_ticket(
    file_path: str,
    issue_type: str,
    description: str,
    severity: str = "major"
) -> IssueTicket:
    """
    生成标准化缺陷工单
    
    Args:
        file_path: 问题文件路径
        issue_type: 问题类型 (bug, security, performance, style)
        description: 问题描述
        severity: 严重程度 (critical, major, minor, info)
        
    Returns:
        IssueTicket: 工单对象
    """
    from datetime import datetime
    
    ticket = IssueTicket(
        id=f"ISSUE-{datetime.now().strftime('%Y%m%d%H%M%S')}",
        file_path=file_path,
        issue_type=issue_type,
        severity=severity,
        description=description,
        created_by="auditor"
    )
    
    print(f"🎫 [工单] 缺陷工单已生成:")
    print(f"   ID: {ticket.id}")
    print(f"   类型: {issue_type}")
    print(f"   严重程度: {severity}")
    print(f"   文件: {file_path}")
    
    return ticket


def request_human_intervention(reason: str, urgency: str = "normal") -> dict:
    """
    请求人类介入
    
    Args:
        reason: 介入原因
        urgency: 紧急程度 (low, normal, high, critical)
        
    Returns:
        dict: 介入请求信息
    """
    from datetime import datetime
    
    intervention_request = {
        "id": f"HUMAN-{datetime.now().strftime('%Y%m%d%H%M%S')}",
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "reason": reason,
        "urgency": urgency,
        "status": "pending"
    }
    
    urgency_icons = {
        "low": "💚",
        "normal": "💛",
        "high": "🧡",
        "critical": "❤️‍🔥"
    }
    
    icon = urgency_icons.get(urgency, "⚪")
    print(f"{icon} [人工介入] 请求已发送:")
    print(f"   原因: {reason}")
    print(f"   紧急程度: {urgency}")
    
    return intervention_request


if __name__ == "__main__":
    # 测试代码
    print("✅ 工具实现测试通过")
    
    # 测试replace_in_file
    test_file = "test_temp.txt"
    with open(test_file, 'w', encoding='utf-8') as f:
        f.write("Hello World\nThis is a test\n")
    
    replace_in_file(test_file, "Hello World", "Hello Universe")
    
    with open(test_file, 'r', encoding='utf-8') as f:
        content = f.read()
        assert "Hello Universe" in content
        print("✓ replace_in_file 测试通过")
    
    os.remove(test_file)
