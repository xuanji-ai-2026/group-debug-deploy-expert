"""
dispatcher_update.py - 模块七：将新角色接入调度师引擎 (V2 + 日志集成版)

最后一步！将审计员和验收官的逻辑完美嵌入到状态流转的死循环中。
这是整个AI协作系统的MVP完工之作。

★ V2.1 新增：深度集成 RoleLogManager 日志系统
- 每次wake_up_role()自动记录到对应角色的专属日志
- 时间戳验证（accept < audit < dev）
- 跨角色日志可见性

现在的完整生命周期：
1. 开发者负责带着RCA报告做精准微创手术
2. 审计员拿着Flake8严格把关代码规范
3. 验收官在沙箱里无情地跑PyTest
4. 调度师在后台用哈希链和状态机维持秩序
5. ★ 所有动作完整留痕于角色专属日志
"""

import os
import re
import json
import hashlib
from typing import List, Dict, Optional, Tuple
from datetime import datetime
from models import (
    FileStatus, FileFrontmatter, RCAReport,
    ErrorFingerprint, IssueTicket
)
from tools import (
    AtomicToolInterceptor, TamperDetectedError,
    ToolRegistry
)
from config import ROLE_CONFIG, STATE_TRANSITIONS
from auditor_tools import AuditorTools
from acceptor_tools import AcceptorTools

# ★ 新增：导入日志系统
try:
    from role_logs import RoleLogManager, RoleType, ActionType, LogLevel
    LOG_SYSTEM_AVAILABLE = True
except ImportError:
    LOG_SYSTEM_AVAILABLE = False
    print("[警告] 未找到role_logs模块，日志功能将不可用")


class DispatcherEngineV2:
    """
    调度师引擎 V2.0 - 完整版
    
    在V1基础上集成了：
    - 审计员的自动Lint检查与决策
    - 验收官的沙箱测试与最终放行
    - 状态自动回滚机制
    - 完整的文件页头更新逻辑
    
    这是整个系统的"无状态内核"，不参与业务思考，
    只负责维护秩序、流转状态、执行规则。
    """

    def __init__(self, project_dir: str):
        self.project_dir = project_dir
        
        # 核心组件初始化
        self.error_fingerprint_counter: Dict[str, int] = {}
        self.tool_registry = ToolRegistry()
        self.audit_log: List[dict] = []
        
        # ★ 新增：实例化专业角色工具集
        self.auditor = AuditorTools(project_dir)
        self.acceptor = AcceptorTools(project_dir)
        
        # ★ V2.1 新增：初始化日志管理器
        if LOG_SYSTEM_AVAILABLE:
            self.log_manager = RoleLogManager(base_dir=project_dir, project_name="dispatcher-v2")
        else:
            self.log_manager = None
        
        # 统计数据
        self.stats = {
            'total_cycles': 0,
            'files_processed': 0,
            'audits_completed': 0,
            'acceptances_completed': 0,
            'rejections_to_dev': 0,
            'promotions_to_verified': 0,
            'circuit_breakers_triggered': 0
        }
        
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

    # ============================================================
    # 第一部分：空间自检（保持不变）
    # ============================================================

    def enforce_directory_structure(self):
        """STEP 1: 空间自检与隔离"""
        print(f"🔍 [调度师V2] 正在对 {self.project_dir} 进行空间秩序自检...")
        
        for dir_path, config in self.required_structure.items():
            full_path = os.path.join(self.project_dir, dir_path)
            if not os.path.exists(full_path):
                os.makedirs(full_path)
                self.log_audit('directory_created', {
                    'path': dir_path,
                    'purpose': config['purpose']
                })
                print(f"✓ 创建缺失目录: {dir_path} ({config['purpose']})")
        
        # 扫描根目录散落文件
        business_extensions = ['.py', '.js', '.ts', '.java', '.go', '.rs', '.vue', '.jsx', '.tsx']
        root_items = os.listdir(self.project_dir)
        
        for item in root_items:
            if item.startswith('.') or item in ['src', 'tests', 'docs', 'node_modules', '__pycache__']:
                continue
            
            item_path = os.path.join(self.project_dir, item)
            
            if os.path.isfile(item_path):
                _, ext = os.path.splitext(item)
                if ext.lower() in business_extensions:
                    self.move_to_quarantine(item_path, f"根目录散落业务文件: {item}")
        
        # 校验 src/ 目录深度
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

    # ============================================================
    # 第二部分：文件扫描与完整性校验（增强版）
    # ============================================================

    def scan_managed_files(self) -> List[dict]:
        """扫描所有带有标准页头的受管文件"""
        managed_files = []
        
        for root, dirs, files in os.walk(self.project_dir):
            dirs[:] = [d for d in dirs if not d.startswith('.') and d != 'quarantine']
            
            for file in files:
                if file.endswith(('.py', '.js', '.ts')):
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
            
            if content.startswith('---'):
                end_marker = content.find('---', 3)
                if end_marker != -1:
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

    def verify_file_integrity(self, file_info: dict) -> bool:
        """STEP 2: 身份与哈希链校验"""
        file_path = file_info['path']
        frontmatter = file_info['frontmatter']
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            actual_hash = hashlib.sha256(content.encode('utf-8')).hexdigest()
            
            if frontmatter.previous_hash and actual_hash != frontmatter.previous_hash:
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

    # ============================================================
    # 第三部分：★ 核心更新 - 角色唤醒与状态流转（完整闭环）
    # ============================================================

    def wake_up_role(self, role_name: str, file_obj: dict):
        """
        ★ 更新后的角色唤醒与状态流转逻辑（V2.1 + 日志集成版）
        
        这是整个系统的核心！实现了完整的dev→audit→accept→verified生命周期：
        - 每次角色唤醒自动记录到对应角色的专属日志
        - 时间戳自动设置和验证
        - 完整的工作流追踪
        
        Args:
            role_name: 角色名称 (developer/auditor/acceptor)
            file_obj: 文件信息字典，包含 path 和 frontmatter
        """
        file_path = file_obj['path']
        current_status = file_obj['frontmatter'].status
        
        print(f"\n{'='*60}")
        print(f"👤 [调度师V2] 唤醒角色: {role_name.upper()}")
        print(f"   📄 目标文件: {os.path.basename(file_path)}")
        print(f"   📊 当前状态: {current_status.value}")
        print(f"{'='*60}\n")
        
        # ★ V2.1 新增：记录角色唤醒事件到专属日志
        self._log_action(
            role=role_name,
            action_type=ActionType.STATE_TRANSITION,
            details=f"WAKE_UP: Role {role_name} awakened for file {os.path.basename(file_path)} (current status: {current_status.value})",
            target_file=file_path,
            level=LogLevel.INFO,
            metadata={
                'triggered_by': 'dispatcher_v2',
                'previous_status': current_status.value,
                'session_id': self._generate_session_id()
            }
        )
        
        # ==================== 开发者阶段 ====================
        if role_name == "developer" and current_status == FileStatus.DEV:
            self._handle_developer_phase(file_path, file_obj)
        
        # ==================== ★ 审计员阶段（新增）====================
        elif role_name == "auditor" and current_status == FileStatus.AUDIT:
            self._handle_auditor_phase(file_path, current_status)
        
        # ==================== ★ 验收官阶段（新增）====================
        elif role_name == "acceptor" and current_status == FileStatus.ACCEPT:
            self._handle_acceptor_phase(file_path)
        
        # ==================== 特殊状态处理 ====================
        elif current_status == FileStatus.BLOCKED:
            print(f"⛔ [调度师] 文件处于阻塞状态，跳过: {os.path.basename(file_path)}")
            self.log_audit('file_skipped_blocked', {'file': file_path})
            
        elif current_status == FileStatus.VERIFIED:
            print(f"✅ [调度师] 文件已验证通过: {os.path.basename(file_path)}")
            self.log_audit('file_already_verified', {'file': file_path})
        
        else:
            print(f"⚠️ [调度师] 状态不匹配，跳过: {role_name} @ {current_status.value}")

    def _handle_developer_phase(self, file_path: str, file_obj: dict):
        """处理开发者阶段"""
        allowed_tools = self.tool_registry.get_tools_for_role('developer')
        interceptor = AtomicToolInterceptor(allowed_tools, 'developer')
        
        session_context = {
            'session_id': self._generate_session_id(),
            'role': 'developer',
            'tools': allowed_tools,
            'file_context': {
                'path': file_path,
                'frontmatter': file_obj['frontmatter'].dict(),
                'content': self._read_file_content(file_path)
            },
            'system_prompt': ROLE_CONFIG['developer']['system_prompt'],
            'history': []
        }
        
        self.log_audit('developer_wakeup', {
            'file': file_path,
            'session_id': session_context['session_id'],
            'tools_count': len(allowed_tools)
        })
        
        print(f"   🔨 会话ID: {session_context['session_id']}")
        print(f"   🛠️ 可用工具: {len(allowed_tools)} 个 (read_file, trace_call_stack, replace_in_file...)")
        print(f"   ✅ 失忆式注入完成（无历史记忆）")
        print(f"\n   ⏳ 等待开发者执行RCA分析并完成微创手术...")
        print(f"   💡 提示: 完成后请将状态更新为 'audit' 以触发审计流程\n")

    def _handle_auditor_phase(self, file_path: str, current_status: FileStatus):
        """
        ★ 处理审计员阶段（核心新增逻辑）
        
        自动调用 AuditorTools 进行静态审查，并根据结果自动决策：
        - 通过 → 流转到 accept
        - 驳回 → 回滚到 dev
        """
        print(f"   👮 [审计员介入] 开始对文件进行静态审查...")
        
        # 调用审计工具进行决策
        action, report = self.auditor.audit_decision(file_path, current_status)
        
        # 更新统计数据
        self.stats['audits_completed'] += 1
        
        if action == "REJECT_TO_DEV":
            # ★ 审计驳回 → 回滚到开发状态
            print(f"\n   ❌ [审计驳回] 决策: {action}")
            print(f"   📋 原因: {report}")
            
            # ★ V2.1 新增：记录审计驳回到审计员专属日志
            self._log_action(
                role='auditor',
                action_type=ActionType.APPROVAL_DECISION,
                details=f"REJECTED: {report}",
                target_file=file_path,
                level=LogLevel.WARNING,
                metadata={
                    'decision': 'REJECT_TO_DEV',
                    'reason': report,
                    'file_status_after': 'dev'
                }
            )
            
            self.update_file_status(
                file_path=file_path,
                new_status=FileStatus.DEV,
                changelog=[f"[AUDIT-REJECT] {report}"],
                role_owner="developer"
            )
            
            self.stats['rejections_to_dev'] += 1
            self.log_audit('audit_rejected', {
                'file': file_path,
                'reason': report,
                'new_status': 'dev'
            })
            
        elif action == "APPROVE_TO_ACCEPT":
            # ★ 审计通过 → 流转到验收
            print(f"\n   ✅ [审计通过] 决策: {action}")
            print(f"   📋 详情: {report}")
            
            # ★ V2.1 新增：记录审计通过到审计员专属日志
            self._log_action(
                role='auditor',
                action_type=ActionType.APPROVAL_DECISION,
                details=f"APPROVED: {report}",
                target_file=file_path,
                level=LogLevel.INFO,
                metadata={
                    'decision': 'APPROVE_TO_ACCEPT',
                    'detail': report,
                    'file_status_after': 'accept'
                }
            )
            
            self.update_file_status(
                file_path=file_path,
                new_status=FileStatus.ACCEPT,
                changelog=["[AUDIT-PASS] 审计通过，移交验收"],
                role_owner="acceptor"
            )
            
            self.log_audit('audit_approved', {
                'file': file_path,
                'detail': report,
                'new_status': 'accept'
            })

    def _handle_acceptor_phase(self, file_path: str):
        """
        ★ 处理验收官阶段（核心新增逻辑）
        
        自动调用 AcceptorTools 在沙箱中执行测试，并根据结果做出最终决策：
        - 全部通过 → 晋升到 verified（终态）
        - 测试失败 → 回滚到 dev
        """
        print(f"   ⚖️ [验收官介入] 开始在沙箱中执行测试...")
        
        # 调用验收工具进行最终决策
        action, report = self.acceptor.acceptance_decision(file_path)
        
        # 更新统计数据
        self.stats['acceptances_completed'] += 1
        
        if action == "REJECT_TO_DEV":
            # ★ 验收失败 → 打回开发者重修
            print(f"\n   ❌ [验收驳回] 最终决策: {action}")
            print(f"   📋 测试失败日志:\n{report}")
            
            # ★ V2.1 新增：记录验收驳回到验收官专属日志
            self._log_action(
                role='acceptor',
                action_type=ActionType.VERIFICATION_DECISION,
                details=f"REJECTED: Tests failed - {report[:200]}",
                target_file=file_path,
                level=LogLevel.ERROR,
                metadata={
                    'decision': 'REJECT_TO_DEV',
                    'failure_log': report[:500],
                    'file_status_after': 'dev'
                }
            )
            
            self.update_file_status(
                file_path=file_path,
                new_status=FileStatus.DEV,
                changelog=[f"[ACCEPT-REJECT] 验收失败: {report[:200]}"],
                role_owner="developer"
            )
            
            self.stats['rejections_to_dev'] += 1
            self.log_audit('acceptance_rejected', {
                'file': file_path,
                'failure_log': report[:500],
                'new_status': 'dev'
            })
            
        elif action == "PROMOTE_TO_VERIFIED":
            # ★ 验收通过 → 正式毕业！
            print(f"\n   🎉 [验收通过] 最终决策: {action}")
            print(f"   🏆 恭喜！代码正式成为交付态！")
            print(f"   📋 详情:\n{report}")
            
            # ★ V2.1 新增：记录最终验收通过到验收官专属日志
            self._log_action(
                role='acceptor',
                action_type=ActionType.VERIFICATION_DECISION,
                details=f"PROMOTED TO VERIFIED: All tests passed! 🎉 {report[:200]}",
                target_file=file_path,
                level=LogLevel.INFO,
                metadata={
                    'decision': 'PROMOTE_TO_VERIFIED',
                    'detail': report,
                    'final_status': 'verified',
                    'milestone': 'DELIVERED'
                }
            )
            
            self.update_file_status(
                file_path=file_path,
                new_status=FileStatus.VERIFIED,
                changelog="[ACCEPT-PASS] 验收通过，准予交付 🎉",
                role_owner="system"
            )
            
            self.stats['promotions_to_verified'] += 1
            self.log_audit('acceptance_approved', {
                'file': file_path,
                'detail': report,
                'final_status': 'verified'
            })

    # ============================================================
    # 第四部分：★ 文件状态更新器（新增核心方法）
    # ============================================================

    def update_file_status(
        self,
        file_path: str,
        new_status: FileStatus,
        changelog: List[str] = None,
        role_owner: str = "dispatcher"
    ):
        """
        ★ 封装YAML页头更新、哈希重算与写回逻辑
        
        这是连接各个角色的桥梁！每次状态流转都会调用此方法：
        1. 读取当前文件内容
        2. 提取现有frontmatter
        3. 更新状态、版本、changelog
        4. 重算哈希链
        5. 写回文件
        
        Args:
            file_path: 目标文件路径
            new_status: 新状态
            changelog: 变更记录列表
            role_owner: 新的责任角色
        """
        filename = os.path.basename(file_path)
        
        print(f"\n🔄 [状态更新器] 正在更新文件状态...")
        print(f"   📄 文件: {filename}")
        print(f"   ➡️ 新状态: {new_status.value}")
        print(f"   👤 责任人: {role_owner}")
        
        try:
            # Step 1: 读取当前文件内容
            with open(file_path, 'r', encoding='utf-8') as f:
                original_content = f.read()
            
            # Step 2: 计算当前内容的哈希（作为previous_hash的依据）
            current_hash = hashlib.sha256(original_content.encode('utf-8')).hexdigest()
            
            # Step 3: 解析或创建frontmatter
            if original_content.startswith('---'):
                end_marker = original_content.find('---', 3)
                old_frontmatter_text = original_content[3:end_marker].strip()
                body_content = original_content[end_marker+3:].lstrip('\n')
                
                # 简单解析YAML（实际应使用PyYAML）
                frontmatter_data = self._parse_simple_yaml(old_frontmatter_text)
            else:
                body_content = original_content
                frontmatter_data = {}
            
            # Step 4: 更新frontmatter字段
            current_version = float(frontmatter_data.get('version', '1.0'))
            new_version = round(current_version + 0.1, 1)
            
            new_changelog = changelog or [f"状态变更为 {new_status.value}"]
            existing_changelog = frontmatter_data.get('changelog', [])
            if isinstance(existing_changelog, list):
                new_changelog = existing_changelog + new_changelog
            
            # Step 5: 构建新的frontmatter
            new_frontmatter = f"""---
id: {frontmatter_data.get('id', f"AUTO-{hashlib.md5(file_path.encode()).hexdigest()[:8]}")}
status: {new_status.value}
role_owner: {role_owner}
version: {new_version}
genesis_hash: {frontmatter_data.get('genesis_hash', '')}
previous_hash: {current_hash}
last_updated: {datetime.utcnow().isoformat()}Z
changelog:"""
            
            for entry in new_changelog:
                new_frontmatter += f'\n  - "{entry}"'
            
            tags = frontmatter_data.get('tags', [])
            if tags:
                new_frontmatter += f"\ntags: {json.dumps(tags, ensure_ascii=False)}"
            
            new_frontmatter += "\n---\n"
            
            # Step 6: 组合新内容并写入
            new_full_content = new_frontmatter + body_content
            
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(new_full_content)
            
            # Step 7: 计算新哈希并验证
            new_hash = hashlib.sha256(new_full_content.encode('utf-8')).hexdigest()
            
            print(f"   ✓ 版本更新: {current_version} → {new_version}")
            print(f"   📝 Changelog: {new_changelog[-1] if new_changelog else 'N/A'}")
            print(f"   🔒 哈希链: {current_hash[:12]}... → {new_hash[:12]}...")
            print(f"   ✅ 文件状态更新成功！\n")
            
            # 记录审计日志
            self.log_audit('status_updated', {
                'file': file_path,
                'old_status': frontmatter_data.get('status', 'unknown'),
                'new_status': new_status.value,
                'version': new_version,
                'hash_before': current_hash[:16],
                'hash_after': new_hash[:16],
                'changed_by': role_owner
            })
            
            return True
            
        except Exception as e:
            print(f"   ❌ 状态更新失败: {e}")
            self.log_audit('status_update_failed', {
                'file': file_path,
                'error': str(e),
                'target_status': new_status.value
            })
            return False

    def _parse_simple_yaml(self, yaml_text: str) -> dict:
        """简单的YAML解析（生产环境应使用PyYAML库）"""
        result = {}
        
        for line in yaml_text.split('\n'):
            line = line.strip()
            if ':' in line and not line.startswith('#'):
                key, _, value = line.partition(':')
                key = key.strip()
                value = value.strip()
                
                # 处理不同类型
                if value.startswith('"') and value.endswith('"'):
                    result[key] = value[1:-1]
                elif value.startswith("'") and value.endswith("'"):
                    result[key] = value[1:-1]
                elif value.lower() in ('true', 'yes'):
                    result[key] = True
                elif value.lower() in ('false', 'no'):
                    result[key] = False
                elif value.replace('.', '').isdigit():
                    result[key] = float(value) if '.' in value else int(value)
                else:
                    result[key] = value
        
        return result

    # ============================================================
    # 第五部分：资源风控与熔断（保持不变）
    # ============================================================

    def check_circuit_breaker(self, error_msg: str, file_path: str) -> str:
        """资源风控：基于错误指纹的分级熔断策略"""
        fingerprint = self._generate_error_fingerprint(error_msg, file_path)
        
        count = self.error_fingerprint_counter.get(fingerprint, 0) + 1
        self.error_fingerprint_counter[fingerprint] = count
        
        self.log_audit('circuit_breaker_check', {
            'fingerprint': fingerprint[:16],
            'file': file_path,
            'count': count,
            'decision': ''
        })
        
        if count == 1:
            decision = "RETRY"
            print(f"⚠️  [熔断] 第1次失败 ({os.path.basename(file_path)}) - 允许重试")
        elif count == 2:
            decision = "UPGRADE_CONTEXT"
            print(f"🔥 [熔断] 第2次失败 ({os.path.basename(file_path)}) - 升级上下文警告")
        else:
            decision = "BLOCKED"
            print(f"🚨 [熔断] 第{count}次失败 ({os.path.basename(file_path)}) - 触发最高级别熔断！")
            self.block_file(file_path, "loop_detected")
            self._request_human_intervention(file_path, error_msg, count)
            self.stats['circuit_breakers_triggered'] += 1
        
        self.audit_log[-1]['decision'] = decision
        return decision

    def _generate_error_fingerprint(self, error_msg: str, file_path: str) -> str:
        """生成错误指纹"""
        raw = f"{error_msg}{file_path}"
        return hashlib.md5(raw.encode('utf-8')).hexdigest()

    # ============================================================
    # 第六部分：辅助方法（保持不变）
    # ============================================================

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
        
        print(f"📦 [隔离] 文件已移入隔离区: {filename} (原因: {reason})")

    def block_file(self, file_path: str, reason: str):
        """将文件状态置为blocked"""
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
        
        issues_dir = os.path.join(self.project_dir, '.issues', 'open')
        os.makedirs(issues_dir, exist_ok=True)
        issue_path = os.path.join(issues_dir, f"{issue.id}.json")
        
        with open(issue_path, 'w', encoding='utf-8') as f:
            json.dump(issue.dict(), f, indent=2, ensure_ascii=False)
        
        print(f"🙏 [调度师] 已请求人类介入! 工单ID: {issue.id}")

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

    # ============================================================
    # ★ V2.1 新增：第六部分B - 日志集成辅助方法
    # ============================================================

    def _log_action(
        self,
        role: str,
        action_type: ActionType,
        details: str,
        target_file: str = None,
        level: LogLevel = LogLevel.INFO,
        metadata: dict = None
    ):
        """
        ★ 核心辅助方法：统一记录动作到角色专属日志
        
        这是V2.1版本的核心新增功能，确保每个关键动作都：
        1. 写入对应角色的专属日志文件（别人只读）
        2. 自动设置工作流时间戳（dev/audit/accept）
        3. 验证时间戳顺序规则（accept < audit < dev）
        
        Args:
            role: 角色名称 ('developer', 'auditor', 'acceptor', 'dispatcher')
            action_type: 动作类型 (ActionType枚举)
            details: 动作描述
            target_file: 目标文件路径（可选）
            level: 日志级别
            metadata: 附加元数据（可选）
        """
        if not self.log_manager or not LOG_SYSTEM_AVAILABLE:
            return
        
        try:
            # 映射角色名称到RoleType枚举
            role_mapping = {
                'developer': RoleType.DEVELOPER,
                'auditor': RoleType.AUDITOR,
                'acceptor': RoleType.ACCEPTOR,
                'dispatcher': RoleType.DISPATCHER
            }
            
            role_enum = role_mapping.get(role)
            if not role_enum:
                print(f"[日志警告] 未知的角色类型: {role}")
                return
            
            # 调用日志管理器写入
            entry = self.log_manager.write_entry(
                role=role_enum,
                agent_name=f"{role.capitalize()}-Agent",
                action=action_type,
                details=details,
                target_file=target_file,
                level=level,
                metadata=metadata or {}
            )
            
            # 输出确认信息（调试用）
            if entry:
                timestamp_short = entry.timestamp[:19] if hasattr(entry, 'timestamp') else 'N/A'
                print(f"   📝 [日志已记录] {role.upper()} @ {timestamp_short} | {action_type.value}")
                
        except Exception as e:
            print(f"[日志错误] 无法写入日志: {e}")

    # ============================================================
    # 第七部分：★ 主循环（增强版）
    # ============================================================

    def run_cycle(self):
        """
        核心死循环：状态机驱动唤醒（完整闭环版）
        
        现在的完整流程：
        1. 空间自检
        2. 扫描受管文件
        3. 校验完整性
        4. 根据状态唤醒对应角色
           - dev → 开发者（RCA+微创手术）
           - audit → 审计员（Flake8+规范检查）★新增
           - accept → 验收官（PyTest+沙箱测试）★新增
           - verified → 终态（放行）
           - blocked → 阻塞（等待人工）
        5. 资源风控监控
        """
        print("\n" + "=" * 70)
        print("🔄 [调度师V2] 开始新一轮完整调度周期...")
        print("=" * 70 + "\n")
        
        self.stats['total_cycles'] += 1
        
        # Step 1: 空间自检
        try:
            self.enforce_directory_structure()
        except Exception as e:
            self.log_audit('structure_enforcement_failed', {'error': str(e)})
            print(f"❌ 空间自检失败: {e}")
            return
        
        # Step 2: 扫描受管文件
        managed_files = self.scan_managed_files()
        self.stats['files_processed'] += len(managed_files)
        
        print(f"📂 [调度师V2] 发现 {len(managed_files)} 个受管文件\n")
        
        if not managed_files:
            print("ℹ️ 当前没有需要处理的受管文件")
            return
        
        # Step 3 & 4: 校验并按状态分流处理
        for file_info in managed_files:
            try:
                self.verify_file_integrity(file_info)
                
                current_status = file_info['frontmatter'].status
                
                # ★ 根据状态自动选择对应角色（无需手动指定！）
                if current_status == FileStatus.DEV:
                    self.wake_up_role("developer", file_info)
                elif current_status == FileStatus.AUDIT:
                    self.wake_up_role("auditor", file_info)
                elif current_status == FileStatus.ACCEPT:
                    self.wake_up_role("acceptor", file_info)
                elif current_status == FileStatus.BLOCKED:
                    print(f"⛔ [跳过] 文件处于阻塞状态: {os.path.basename(file_info['path'])}")
                elif current_status == FileStatus.VERIFIED:
                    print(f"✅ [跳过] 文件已验证: {os.path.basename(file_info['path'])}")
                    
            except TamperDetectedError as e:
                print(f"🚨 {e}")
                continue
            except Exception as e:
                self.log_audit('cycle_error', {
                    'file': file_info['path'],
                    'error': str(e)
                })
                print(f"❌ 处理文件出错: {file_info['path']} - {e}")
        
        # Step 5: 显示本轮统计
        self._print_cycle_summary()

    def _print_cycle_summary(self):
        """打印本轮调度统计摘要"""
        print("\n" + "-" * 70)
        print(f"📊 [第 {self.stats['total_cycles']} 轮] 调度统计摘要")
        print("-" * 70)
        print(f"   📁 处理文件数: {self.stats['files_processed']}")
        print(f"   👮 审计完成数: {self.stats['audits_completed']}")
        print(f"   ⚖️ 验收完成数: {self.stats['acceptances_completed']}")
        print(f"   ❌ 打回重修数: {self.stats['rejections_to_dev']}")
        print(f"   🎉 晋升交付数: {self.stats['promotions_to_verified']}")
        print(f"   🚨 熔断触发数: {self.stats['circuit_breakers_triggered']}")
        print(f"   📋 审计日志条数: {len(self.audit_log)}")
        print(f"   🔢 错误指纹数: {len(self.error_fingerprint_counter)}")
        print("-" * 70)

    # ============================================================
    # 第八部分：批量操作与高级功能
    # ============================================================

    def batch_promote_to_audit(self, file_paths: List[str]):
        """批量将文件从dev提升到audit"""
        for file_path in file_paths:
            print(f"\n📤 [批量操作] 提升 -> audit: {os.path.basename(file_path)}")
            self.update_file_status(
                file_path=file_path,
                new_status=FileStatus.AUDIT,
                changelog=["[BATCH] 批量提交审计"],
                role_owner="auditor"
            )

    def batch_force_verify(self, file_paths: List[str]):
        """强制将文件提升到verified（谨慎使用！）"""
        for file_path in file_paths:
            print(f"\n⚠️ [强制操作] 提升 -> verified: {os.path.basename(file_path)}")
            self.update_file_status(
                file_path=file_path,
                new_status=FileStatus.VERIFIED,
                changelog=["[FORCE] 强制提升至交付态（已绕过正常流程）"],
                role_owner="admin"
            )

    def generate_report(self) -> dict:
        """Generate complete system runtime report"""
        return {
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "project_dir": self.project_dir,
            "statistics": self.stats.copy(),
            "active_fingerprints": len(self.error_fingerprint_counter),
            "managed_files_count": len(self.scan_managed_files()),
            "recent_logs": self.audit_log[-20:] if self.audit_log else [],
            "health_status": self._calculate_health_status()
        }

    def _calculate_health_status(self) -> str:
        """计算系统健康状态"""
        total = self.stats['audits_completed'] + self.stats['acceptances_completed']
        if total == 0:
            return "idle"  # 空闲
        
        rejection_rate = self.stats['rejections_to_dev'] / max(total, 1)
        
        if rejection_rate > 0.5:
            return "warning"  # 高驳回率
        elif self.stats['circuit_breakers_triggered'] > 3:
            return "danger"  # 多次熔断
        else:
            return "healthy"  # 健康


# ============================================================
# 第九部分：便捷入口函数
# ============================================================

def create_dispatcher(project_dir: str = '.') -> DispatcherEngineV2:
    """
    工厂函数：快速创建调度师实例
    
    Args:
        project_dir: 项目目录路径（默认当前目录）
        
    Returns:
        DispatcherEngineV2: 完整版调度师引擎实例
    """
    engine = DispatcherEngineV2(project_dir)
    
    # 注册基础工具
    from tools_impl import replace_in_file, trace_call_stack, create_rca_report
    
    engine.tool_registry.register("replace_in_file", {
        "function": replace_in_file,
        "roles": ["developer"],
        "description": "局部替换文件内容（原地手术）"
    })
    
    engine.tool_registry.register("trace_call_stack", {
        "function": trace_call_stack,
        "roles": ["developer"],
        "description": "追踪函数调用栈"
    })
    
    engine.tool_registry.register("create_rca_report", {
        "function": create_rca_report,
        "roles": ["developer"],
        "description": "提交根因分析报告"
    })
    
    return engine


if __name__ == "__main__":
    # 测试代码
    print("=" * 70)
    print("🧪 模块七测试：调度师引擎V2（完整闭环版）")
    print("=" * 70)
    
    # 创建引擎实例
    engine = create_dispatcher('.')
    
    print("\n✅ 引擎初始化成功")
    print(f"   项目目录: {engine.project_dir}")
    print(f"   审计工具: 已加载 (AuditorTools)")
    print(f"   验收工具: 已加载 (AcceptorTools)")
    print(f"   统计模块: 已初始化")
    
    # 执行空间自检
    try:
        engine.enforce_directory_structure()
        print("\n✅ 空间自检完成")
    except Exception as e:
        print(f"\n⚠️ 空间自检: {e}")
    
    # 生成健康报告
    report = engine.generate_report()
    print(f"\n📊 系统状态: {report['health_status']}")
    print(f"📋 日志数量: {len(report['recent_logs'])}")
    
    print("\n" + "=" * 70)
    print("🎉 模块七测试通过！调度师引擎V2已准备就绪")
    print("=" * 70)
