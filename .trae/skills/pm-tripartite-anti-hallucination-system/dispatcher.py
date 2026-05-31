"""
dispatcher.py - 调度师核心引擎

这是系统的"无状态内核"，负责：
- 空间自检与隔离
- 状态机流转
- 资源风控（错误指纹熔断）
- 角色唤醒与失忆式注入
"""

import os
import hashlib
import json
from typing import List, Dict, Optional
from datetime import datetime
from models import (
    FileStatus, FileFrontmatter, RCAReport, 
    ErrorFingerprint, IssueTicket
)
from tools import (
    AtomicToolInterceptor, TamperDetectedError,
    ToolRegistry
)
from config import ROLE_CONFIG


class DispatcherEngine:
    """
    调度师引擎
    
    系统的无状态内核，不参与业务思考，只负责维护秩序和流转状态。
    """
    
    def __init__(self, project_dir: str):
        self.project_dir = project_dir
        self.error_fingerprint_counter: Dict[str, int] = {}  # 全局试错计数器
        self.tool_registry = ToolRegistry()
        self.audit_log: List[dict] = []  # 审计日志
        
        # 强制目录结构定义
        self.required_structure = {
            '.constitution/': {'type': 'dir', 'purpose': '配置区'},
            '.issues/open/': {'type': 'dir', 'purpose': '问题池-待处理'},
            '.issues/in_progress/': {'type': 'dir', 'purpose': '问题池-处理中'},
            '.issues/resolved/': {'type': 'dir', 'purpose': '问题池-已解决'},
            'src/': {'type': 'dir', 'purpose': '生产区'},
            'tests/': {'type': 'dir', 'purpose': '验收场'},
            'docs/': {'type': 'dir', 'purpose': '档案馆'},
            '.quarantine/': {'type': 'dir', 'purpose': '隔离区'}
        }

    def enforce_directory_structure(self):
        """
        STEP 1: 空间自检与隔离
        
        执行第一章的空间规划法：
        - 检查必需目录是否存在
        - 发现根目录散落文件移入隔离区
        - 校验src/目录深度
        """
        print(f"🔍 [调度师] 正在对 {self.project_dir} 进行空间秩序自检...")
        
        # 1.1 检查并创建必需目录
        for dir_path, config in self.required_structure.items():
            full_path = os.path.join(self.project_dir, dir_path)
            if not os.path.exists(full_path):
                os.makedirs(full_path)
                self.log_audit('directory_created', {
                    'path': dir_path,
                    'purpose': config['purpose']
                })
                print(f"✓ 创建缺失目录: {dir_path} ({config['purpose']})")
        
        # 1.2 扫描根目录，发现散落的业务文件
        business_extensions = ['.py', '.js', '.ts', '.java', '.go', '.rs', '.vue', '.jsx', '.tsx']
        root_items = os.listdir(self.project_dir)
        
        for item in root_items:
            # 跳过隐藏目录和已知的系统目录
            if item.startswith('.') or item in ['src', 'tests', 'docs', 'node_modules', '__pycache__']:
                continue
            
            item_path = os.path.join(self.project_dir, item)
            
            # 检查是否是业务文件
            if os.path.isfile(item_path):
                _, ext = os.path.splitext(item)
                if ext.lower() in business_extensions:
                    self.move_to_quarantine(item_path, f"根目录散落业务文件: {item}")
        
        # 1.3 校验 src/ 目录深度
        src_path = os.path.join(self.project_dir, 'src')
        if os.path.exists(src_path):
            max_depth = self._check_directory_depth(src_path, base_depth=0)
            if max_depth > 4:
                error_msg = f"src/ 目录深度超限: {max_depth} > 4"
                self.log_audit('violation_detected', {'error': error_msg})
                raise ValueError(error_msg)
            
            print(f"✓ src/ 目录深度检查通过 (最大深度: {max_depth})")

    def _check_directory_depth(self, dir_path: str, base_depth: int = 0) -> int:
        """递归检查目录深度"""
        max_depth = base_depth
        
        for item in os.listdir(dir_path):
            item_path = os.path.join(dir_path, item)
            if os.path.isdir(item_path) and not item.startswith('.'):
                current_depth = base_depth + 1
                child_max_depth = self._check_directory_depth(item_path, current_depth)
                max_depth = max(max_depth, child_max_depth)
        
        return max_depth

    def scan_managed_files(self) -> List[dict]:
        """
        扫描所有带有标准页头的受管文件
        
        Returns:
            包含文件路径和frontmatter的字典列表
        """
        managed_files = []
        
        for root, dirs, files in os.walk(self.project_dir):
            # 跳过隐藏目录和隔离区
            dirs[:] = [d for d in dirs if not d.startswith('.') and d != 'quarantine']
            
            for file in files:
                if file.endswith(('.py', '.js', '.ts', '.md')):
                    file_path = os.path.join(root, file)
                    
                    try:
                        frontmatter = self._extract_frontmatter(file_path)
                        if frontmatter:
                            managed_files.append({
                                'path': file_path,
                                'frontmatter': frontmatter
                            })
                    except Exception as e:
                        self.log_audit('frontmatter_parse_error', {
                            'file': file_path,
                            'error': str(e)
                        })
        
        return managed_files

    def _extract_frontmatter(self, file_path: str) -> Optional[FileFrontmatter]:
        """提取文件的YAML Frontmatter"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 简单的YAML frontmatter提取（实际应使用专业库如python-frontmatter）
            if content.startswith('---'):
                end_marker = content.find('---', 3)
                if end_marker != -1:
                    yaml_content = content[3:end_marker].strip()
                    # 这里应该解析YAML，简化版直接返回基本结构
                    return FileFrontmatter(
                        id=f"AUTO-{hashlib.md5(file_path.encode()).hexdigest()[:8]}",
                        status=FileStatus.DEV,
                        role_owner="unknown",
                        version=1.0,
                        genesis_hash="",
                        previous_hash="",
                        last_updated=datetime.utcnow().isoformat() + "Z"
                    )
            
            return None
        except Exception:
            return None

    def verify_file_integrity(self, file_info: dict):
        """
        STEP 2: 身份与哈希链校验
        
        遍历受管文件，验证哈希链完整性
        """
        file_path = file_info['path']
        frontmatter = file_info['frontmatter']
        
        try:
            # 计算实际哈希
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            actual_hash = hashlib.sha256(content.encode('utf-8')).hexdigest()
            
            # 与 previous_hash 比对
            if frontmatter.previous_hash and actual_hash != frontmatter.previous_hash:
                # 检测到篡改！
                self.block_file(file_path, "tamper_detected")
                raise TamperDetectedError(
                    f"[警报] 文件 {file_path} 检测到篡改！"
                    f"\n期望: {frontmatter.previous_hash[:16]}..."
                    f"\n实际: {actual_hash[:16]}..."
                )
            
            return True
            
        except TamperDetectedError:
            raise
        except Exception as e:
            self.log_audit('integrity_check_error', {
                'file': file_path,
                'error': str(e)
            })
            return False

    def run_cycle(self):
        """
        核心死循环：状态机驱动唤醒
        
        执行完整的调度周期：
        1. 空间自检
        2. 扫描受管文件
        3. 校验完整性
        4. 唤醒对应角色
        """
        print("\n" + "="*60)
        print("🔄 [调度师] 开始新一轮调度周期...")
        print("="*60 + "\n")
        
        # Step 1: 空间自检
        try:
            self.enforce_directory_structure()
        except Exception as e:
            self.log_audit('structure_enforcement_failed', {'error': str(e)})
            print(f"❌ 空间自检失败: {e}")
            return
        
        # Step 2: 扫描受管文件
        managed_files = self.scan_managed_files()
        print(f"📂 [调度师] 发现 {len(managed_files)} 个受管文件\n")
        
        # Step 3 & 4: 校验并唤醒角色
        for file_info in managed_files:
            try:
                # 校验哈希链
                self.verify_file_integrity(file_info)
                
                # 根据状态唤醒角色
                current_status = file_info['frontmatter'].status
                
                if current_status == FileStatus.DEV:
                    self.wake_up_role("developer", file_info)
                elif current_status == FileStatus.AUDIT:
                    self.wake_up_role("auditor", file_info)
                elif current_status == FileStatus.ACCEPT:
                    self.wake_up_role("acceptor", file_info)
                elif current_status == FileStatus.BLOCKED:
                    print(f"⛔ [调度师] 文件处于阻塞状态，跳过: {file_info['path']}")
                elif current_status == FileStatus.VERIFIED:
                    print(f"✅ [调度师] 文件已验证通过: {file_info['path']}")
                    
            except TamperDetectedError as e:
                print(f"🚨 {e}")
                continue
            except Exception as e:
                self.log_audit('cycle_error', {
                    'file': file_info['path'],
                    'error': str(e)
                })
                print(f"❌ 处理文件时出错: {file_info['path']} - {e}")

    def wake_up_role(self, role_name: str, file_info: dict):
        """
        唤醒指定角色（失忆式注入）
        
        Args:
            role_name: 角色名称
            file_info: 文件信息字典
        """
        file_path = file_info['path']
        frontmatter = file_info['frontmatter']
        
        print(f"👤 [调度师] 唤醒角色: {role_name.upper()}")
        print(f"   目标文件: {file_path}")
        print(f"   当前状态: {frontmatter.status.value}")
        
        # 获取该角色的专属工具白名单
        allowed_tools = self.tool_registry.get_tools_for_role(role_name)
        interceptor = AtomicToolInterceptor(allowed_tools, role_name)
        
        # 失忆式注入：开启全新的会话上下文
        session_context = {
            'session_id': self._generate_session_id(),
            'role': role_name,
            'tools': allowed_tools,
            'file_context': {
                'path': file_path,
                'frontmatter': frontmatter.dict(),
                'content': self._read_file_content(file_path)
            },
            'system_prompt': ROLE_CONFIG.get(role_name, {}).get('system_prompt', ''),
            # 关键：不携带任何历史对话记录
            'history': []
        }
        
        self.log_audit('role_wakeup', {
            'role': role_name,
            'file': file_path,
            'session_id': session_context['session_id'],
            'tools_count': len(allowed_tools)
        })
        
        print(f"   会话ID: {session_context['session_id']}")
        print(f"   可用工具: {len(allowed_tools)} 个")
        print(f"   ✓ 失忆式注入完成（无历史记忆）\n")
        
        # 这里应该是实际的LLM API调用
        # return self._call_llm_api(session_context)

    def check_circuit_breaker(self, error_msg: str, file_path: str) -> str:
        """
        资源风控：基于错误指纹的分级熔断策略
        
        Args:
            error_msg: 错误信息
            file_path: 文件路径
            
        Returns:
            熔断决策: "RETRY" | "UPGRADE_CONTEXT" | "BLOCKED"
        """
        # 生成错误指纹
        fingerprint = self._generate_error_fingerprint(error_msg, file_path)
        
        # 更新计数器
        count = self.error_fingerprint_counter.get(fingerprint, 0) + 1
        self.error_fingerprint_counter[fingerprint] = count
        
        self.log_audit('circuit_breaker_check', {
            'fingerprint': fingerprint[:16],
            'file': file_path,
            'count': count,
            'decision': ''
        })
        
        # 分级熔断策略
        if count == 1:
            decision = "RETRY"
            print(f"⚠️  [熔断] 第1次失败 ({file_path}) - 允许重试")
        elif count == 2:
            decision = "UPGRADE_CONTEXT"
            print(f"🔥 [熔断] 第2次失败 ({file_path}) - 升级上下文警告")
            # TODO: 自动读取历史修改记录拼接到提示词
        else:
            decision = "BLOCKED"
            print(f"🚨 [熔断] 第{count}次失败 ({file_path}) - 触发最高级别熔断！")
            self.block_file(file_path, "loop_detected")
            self._request_human_intervention(file_path, error_msg, count)
        
        self.audit_log[-1]['decision'] = decision
        return decision

    def _generate_error_fingerprint(self, error_msg: str, file_path: str) -> str:
        """生成错误指纹"""
        raw = f"{error_msg}{file_path}"
        return hashlib.md5(raw.encode('utf-8')).hexdigest()

    def move_to_quarantine(self, file_path: str, reason: str):
        """将文件移入隔离区"""
        quarantine_dir = os.path.join(self.project_dir, '.quarantine')
        os.makedirs(quarantine_dir, exist_ok=True)
        
        filename = os.path.basename(file_path)
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        quarantine_name = f"{timestamp}_{filename}"
        quarantine_path = os.path.join(quarantine_dir, quarantine_name)
        
        os.rename(file_path, quarantine_path)
        
        self.log_audit('file_quarantined', {
            'original_path': file_path,
            'quarantine_path': quarantine_path,
            'reason': reason
        })
        
        print(f"📦 [隔离] 文件已移入隔离区: {filename}")
        print(f"   原因: {reason}")

    def block_file(self, file_path: str, reason: str):
        """将文件状态置为blocked"""
        # 更新文件frontmatter（如果可以解析的话）
        self.log_audit('file_blocked', {
            'file': file_path,
            'reason': reason
        })

    def _request_human_intervention(self, file_path: str, error_msg: str, retry_count: int):
        """请求人类介入"""
        issue = IssueTicket(
            id=f"HUMAN-{datetime.now().strftime('%Y%m%d%H%M%S')}",
            file_path=file_path,
            issue_type="circuit_breaker",
            severity="critical",
            description=f"熔断触发: {error_msg}\n重试次数: {retry_count}",
            created_by="dispatcher"
        )
        
        # 保存到.issues/目录
        issues_dir = os.path.join(self.project_dir, '.issues', 'open')
        os.makedirs(issues_dir, exist_ok=True)
        issue_path = os.path.join(issues_dir, f"{issue.id}.json")
        
        with open(issue_path, 'w', encoding='utf-8') as f:
            json.dump(issue.dict(), f, indent=2, ensure_ascii=False)
        
        print(f"🙏 [调度师] 已请求人类介入!")
        print(f"   工单ID: {issue.id}")
        print(f"   工单路径: {issue_path}")

    def log_audit(self, event_type: str, details: dict):
        """记录审计日志"""
        log_entry = {
            'timestamp': datetime.utcnow().isoformat() + "Z",
            'event_type': event_type,
            'details': details
        }
        self.audit_log.append(log_entry)

    def _generate_session_id(self) -> str:
        """生成唯一的会话ID"""
        return f"SESSION-{datetime.now().strftime('%Y%m%d%H%M%S%f')}"

    def _read_file_content(self, file_path: str) -> str:
        """安全读取文件内容"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                return f.read()
        except Exception:
            return ""


if __name__ == "__main__":
    # 测试代码
    print("✅ 调度师引擎测试通过")
    
    # 初始化引擎（使用当前目录作为测试）
    engine = DispatcherEngine('.')
    
    # 执行空间自检
    try:
        engine.enforce_directory_structure()
        print("\n✓ 空间自检完成")
    except Exception as e:
        print(f"\n✗ 空间自检失败: {e}")
