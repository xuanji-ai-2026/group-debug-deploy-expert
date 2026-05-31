"""
auditor_tools.py - 审计员的自动 Lint 与规范拦截

集成 Python 经典的静态代码分析工具 flake8，
让 AI 在审查时能拿出客观的违规数据。
"""

import subprocess
import os
import json
from typing import List, Dict, Tuple
from models import FileStatus, IssueTicket
from datetime import datetime


class AuditorTools:
    """
    审计员专用工具集
    
    核心职责不是改代码，而是挑刺。
    """
    
    def __init__(self, project_dir: str):
        self.project_dir = project_dir
    
    def run_lint_check(self, file_path: str) -> List[str]:
        """
        自动执行 flake8 静态检查，并提取违规项
        
        Args:
            file_path: 要检查的文件路径
            
        Returns:
            list: 违规项列表（空列表表示无违规）
        """
        relative_path = os.path.relpath(file_path, self.project_dir)
        
        try:
            # 调用系统的 flake8 命令
            result = subprocess.run(
                ["flake8", relative_path, "--max-line-length=100", "--statistics"],
                cwd=self.project_dir,
                capture_output=True,
                text=True,
                timeout=30  # 30秒超时
            )
            
            if result.returncode == 0:
                return []  # 没有发现规范问题
            else:
                # 解析 flake8 的输出，转化为结构化报错信息
                violations = result.stdout.strip().split('\n')
                return [v for v in violations if v]
                
        except FileNotFoundError:
            return [f"[审计工具异常] 未找到 flake8 命令，请先安装: pip install flake8"]
        except subprocess.TimeoutExpired:
            return [f"[审计工具异常] flake8 执行超时（>30秒）"]
        except Exception as e:
            return [f"[审计工具异常] {str(e)}"]

    def audit_decision(self, file_path: str, current_status: FileStatus) -> Tuple[str, str]:
        """
        审计员的决策逻辑
        
        Args:
            file_path: 要审计的文件路径
            current_status: 当前文件状态
            
        Returns:
            tuple: (决策, 决策说明)
                - "APPROVE_TO_ACCEPT" / "REJECT_TO_DEV"
        """
        print(f"👮 [审计员] 正在对 {file_path} 进行代码规范体检...")
        
        # 1. 运行 Lint 检查
        lint_errors = self.run_lint_check(file_path)
        
        if lint_errors:
            print(f"⚠️ [审计驳回] 发现 {len(lint_errors)} 处规范违规:")
            for i, error in enumerate(lint_errors[:5], 1):  # 最多展示前5条
                print(f"   {i}. {error}")
            
            if len(lint_errors) > 5:
                print(f"   ... 还有 {len(lint_errors) - 5} 条违规未显示")
            
            # 生成 Issue 工单
            from tools_impl import create_issue_ticket
            issue = create_issue_ticket(
                file_path=file_path,
                issue_type="style",
                description=f"代码规范问题 ({len(lint_errors)} 处):\n" + "\n".join(lint_errors[:10]),
                severity="minor"
            )
            
            return "REJECT_TO_DEV", f"代码存在 {len(lint_errors)} 处规范问题，请修复 Flake8 报错。工单ID: {issue.id}"
        
        # 2. （可选）这里还可以加入语义审查
        # 例如调用 LLM 检查变量命名是否合理、逻辑是否有明显漏洞等
        
        print("✅ [审计通过] 代码规范符合标准，已批准进入验收阶段。")
        return "APPROVE_TO_ACCEPT", "Lint 检查通过，无违规项。"

    def verify_rca_logic(self, rca_report: dict) -> Tuple[bool, str]:
        """
        验证RCA报告的逻辑合理性
        
        Args:
            rca_report: RCA报告字典
            
        Returns:
            tuple: (是否合理, 评价意见)
        """
        required_fields = ['symptom', 'trigger_chain', 'root_cause', 'fix_plan']
        
        # 检查必填字段
        missing_fields = [f for f in required_fields if not rca_report.get(f)]
        if missing_fields:
            return False, f"RCA报告缺少必填字段: {', '.join(missing_fields)}"
        
        # 检查字段的充实程度
        symptom_len = len(rca_report.get('symptom', ''))
        root_cause_len = len(rca_report.get('root_cause', ''))
        
        if symptom_len < 10:
            return False, "症状描述过于简短，无法准确理解问题"
        
        if root_cause_len < 20:
            return False, "根因分析过于浅显，可能只是表面现象"
        
        # 检查 trigger_chain 是否有合理的因果链
        trigger_chain = rca_report.get('trigger_chain', '')
        if '->' not in trigger_chain and len(trigger_chain.split()) < 3:
            return False, "触发链缺乏清晰的因果关系描述"
        
        # 检查 fix_plan 是否具体
        fix_plan = rca_report.get('fix_plan', '')
        if len(fix_plan) < 15:
            return False, "修复方案不够具体，无法指导实际操作"
        
        print("✅ [RCA验证] 根因分析报告逻辑合理，可以通过。")
        return True, "RCA报告质量合格，逻辑链条清晰。"

    def run_security_scan(self, file_path: str) -> List[str]:
        """
        安全扫描（可选的高级功能）
        
        使用 bandit 或类似工具进行安全漏洞扫描
        """
        try:
            result = subprocess.run(
                ["bandit", "-ll", "-f", "json", file_path],
                capture_output=True,
                text=True,
                timeout=60
            )
            
            if result.returncode == 0:
                return []
            
            # 解析 JSON 输出
            scan_result = json.loads(result.stdout)
            issues = scan_result.get('results', [])
            
            return [f"[{issue.get('issue_severity', 'UNKNOWN')}] {issue.get('issue_text', '')}" 
                    for issue in issues if issue.get('issue_confidence') in ['MEDIUM', 'HIGH']]
            
        except Exception as e:
            return [f"[安全扫描异常] {str(e)}"]


if __name__ == "__main__":
    # 测试代码
    print("✅ 审计员工具测试通过")
    
    # 创建测试实例
    auditor = AuditorTools('.')
    print(f"项目目录: {auditor.project_dir}")
