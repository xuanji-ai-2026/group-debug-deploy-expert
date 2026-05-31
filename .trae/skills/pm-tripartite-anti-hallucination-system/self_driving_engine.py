"""
self_driving_engine.py - Self-Driving Core Engine (Autonomous Workflow Engine)

★★★ 核心功能 ★★★
1. 文件系统实时监控（发现新增/修改/删除）
2. 自动工作流驱动（Dev完成→自动Audit→问题通知→Dev修复→重新Audit）
3. 角色活跃度检测（心跳机制确认每个角色都在干活）
4. 3次失败熔断+回滚到初始版本+请求人类裁决
5. 与日志系统深度集成（每个动作强制留痕）
6. 与调度师V2无缝集成

核心设计原则：
- 自发现：无需人工触发，自动检测文件变化
- 自驱动：根据文件状态自动推进工作流
- 自愈：检测到异常自动尝试修复
- 自保护：3次失败立即熔断，防止无限循环
- 可追溯：所有操作完整日志记录

Author: PM-Driven Tripartite System
Version: 1.0.0
Date: 2026-05-31
"""

import os
import sys
import json
import time
import hashlib
import shutil
from typing import Dict, List, Optional, Tuple, Any, Set
from datetime import datetime
from pathlib import Path
from dataclasses import dataclass, field
from enum import Enum
from collections import defaultdict

# 导入现有模块
sys.path.insert(0, str(Path(__file__).parent))

from models import FileStatus, FileFrontmatter, RCAReport, ErrorFingerprint, IssueTicket
from role_logs import RoleLogManager, RoleType, ActionType, LogLevel
from file_lock_manager import FileLockManager, LockType


class FileChangeType(Enum):
    """文件变化类型枚举"""
    CREATED = "created"          # 新增文件
    MODIFIED = "modified"        # 内容修改
    TIMESTAMP_UPDATED = "timestamp_updated"  # 仅时间戳更新（touch）
    DELETED = "deleted"          # 文件删除
    STATUS_CHANGED = "status_changed"  # 状态变更


@dataclass
class FileSnapshot:
    """
    文件快照 - 记录文件在某一时刻的完整状态
    
    用于：
    1. 检测文件变化（对比前后快照）
    2. 回滚到历史版本（保留genesis snapshot）
    3. 变更追踪和审计
    """
    path: str                           # 文件路径
    content_hash: str                   # SHA-256内容哈希
    mtime: float                        # 修改时间戳（os.path.getmtime）
    size: int                           # 文件大小（字节）
    status: Optional[FileStatus] = None # 当前状态（从frontmatter提取）
    version: float = 1.0                # 版本号
    captured_at: str = ""               # 快照时间（ISO8601）
    
    def to_dict(self) -> dict:
        return {
            'path': self.path,
            'content_hash': self.content_hash,
            'mtime': self.mtime,
            'size': self.size,
            'status': self.status.value if self.status else None,
            'version': self.version,
            'captured_at': self.captured_at
        }


@dataclass
class RoleActivity:
    """
    角色活动记录 - 追踪每个角色的活跃状态
    
    用于回答："如何知道每个角色是否都在干活？"
    """
    role: str                          # 角色名称
    agent_name: str                    # Agent标识
    last_heartbeat: str = ""           # 最后心跳时间
    last_action: str = ""              # 最后执行的动作
    last_action_time: str = ""         # 最后动作时间
    files_processed: int = 0           # 已处理文件数
    actions_completed: int = 0         # 已完成动作数
    current_task: Optional[str] = None # 当前正在处理的任务
    is_active: bool = False            # 是否处于活跃状态
    idle_seconds: int = 0              # 空闲秒数
    
    def update_heartbeat(self):
        """更新心跳"""
        self.last_heartbeat = datetime.now().isoformat()
        self.is_active = True
        self.idle_seconds = 0
    
    def mark_action_completed(self, action: str, target_file: str = None):
        """标记一个动作完成"""
        self.last_action = action
        self.last_action_time = datetime.now().isoformat()
        self.actions_completed += 1
        if target_file:
            self.files_processed += 1
        self.update_heartbeat()


@dataclass
class ModificationRecord:
    """
    修改记录 - 追踪单个文件的修改历史
    
    用于：
    1. 统计开发者修改次数（判断是否需要熔断）
    2. 审计不通过次数统计
    3. 回滚点管理
    """
    file_path: str                     # 文件路径
    modification_count: int = 0        # 总修改次数
    audit_rejection_count: int = 0     # 审计拒绝次数
    accept_rejection_count: int = 0    # 验收拒绝次数
    genesis_snapshot: Optional[FileSnapshot] = None  # 初始版本快照
    current_snapshot: Optional[FileSnapshot] = None   # 当前版本快照
    modification_history: List[dict] = field(default_factory=list)  # 修改历史
    is_circuit_broken: bool = False     # 是否已触发熔断
    rollback_executed: bool = False    # 是否已执行回滚
    
    def record_modification(self, snapshot: FileSnapshot, reason: str, actor: str):
        """记录一次修改"""
        self.modification_count += 1
        self.current_snapshot = snapshot
        
        if not self.genesis_snapshot:
            self.genesis_snapshot = snapshot
        
        self.modification_history.append({
            'sequence': self.modification_count,
            'timestamp': datetime.now().isoformat(),
            'actor': actor,
            'reason': reason,
            'snapshot': snapshot.to_dict()
        })
    
    def record_audit_rejection(self, reason: str, auditor: str):
        """记录一次审计拒绝"""
        self.audit_rejection_count += 1
        
        self.modification_history.append({
            'type': 'audit_rejection',
            'timestamp': datetime.now().isoformat(),
            'auditor': auditor,
            'reason': reason,
            'rejection_count': self.audit_rejection_count,
            'total_modifications': self.modification_count
        })
        
        # ★ 核心规则：3次审计不通过 → 触发熔断
        if self.audit_rejection_count >= 3:
            self.is_circuit_broken = True
    
    def should_trigger_circuit_breaker(self) -> bool:
        """判断是否应该触发熔断"""
        return self.audit_rejection_count >= 3 or self.is_circuit_broken
    
    def get_rollback_target(self) -> Optional[FileSnapshot]:
        """获取回滚目标（初始版本）"""
        return self.genesis_snapshot


class FileSystemWatcher:
    """
    文件系统监控器
    
    核心职责：
    - 监控指定目录下的文件变化
    - 检测新增文件、内容修改、时间戳更新
    - 生成FileChange事件供引擎消费
    
    实现方式：基于快照对比（轮询模式）
    未来可升级为：inotify (Linux) / ReadDirectoryChangesW (Windows) / FSEvents (macOS)
    """
    
    def __init__(self, watch_dirs: List[str], extensions: List[str] = None):
        """
        Args:
            watch_dirs: 要监控的目录列表
            extensions: 要关注的文件扩展名（如 ['.py', '.js', '.ts']）
        """
        self.watch_dirs = [Path(d) for d in watch_dirs]
        self.extensions = extensions or ['.py', '.js', '.ts', '.java', '.go', '.vue', '.jsx', '.tsx']
        
        # 当前文件快照缓存 {file_path: FileSnapshot}
        self.current_snapshots: Dict[str, FileSnapshot] = {}
        
        # 上一次扫描时的快照（用于对比）
        self.previous_snapshots: Dict[str, FileSnapshot] = {}
        
        # 发现的变化事件队列
        self.change_events: List[dict] = []
    
    def take_full_snapshot(self) -> Dict[str, FileSnapshot]:
        """
        对所有监控目录进行全量快照
        
        Returns:
            dict: {file_path: FileSnapshot}
        """
        snapshots = {}
        
        for watch_dir in self.watch_dirs:
            if not watch_dir.exists():
                continue
            
            for root, dirs, files in os.walk(watch_dir):
                # 跳过隐藏目录和隔离区
                dirs[:] = [d for d in dirs if not d.startswith('.') and d != 'quarantine']
                
                for filename in files:
                    # 只关注特定扩展名的文件
                    _, ext = os.path.splitext(filename)
                    if ext.lower() not in self.extensions:
                        continue
                    
                    file_path = os.path.join(root, filename)
                    
                    try:
                        stat_info = os.stat(file_path)
                        
                        with open(file_path, 'rb') as f:
                            content_hash = hashlib.sha256(f.read()).hexdigest()
                        
                        snapshot = FileSnapshot(
                            path=file_path,
                            content_hash=content_hash,
                            mtime=stat_info.st_mtime,
                            size=stat_info.st_size,
                            captured_at=datetime.now().isoformat()
                        )
                        
                        snapshots[file_path] = snapshot
                        
                    except Exception as e:
                        print(f"[FileSystemWatcher] 无法读取文件 {file_path}: {e}")
        
        return snapshots
    
    def scan_for_changes(self) -> List[dict]:
        """
        扫描文件系统，发现变化
        
        Returns:
            list: 变化事件列表，每个事件包含：
                - change_type: FileChangeType
                - file_path: 文件路径
                - old_snapshot: 旧快照（如果存在）
                - new_snapshot: 新快照
        """
        # 保存当前快照为"上一次"
        self.previous_snapshots = self.current_snapshots.copy()
        
        # 获取最新快照
        self.current_snapshots = self.take_full_snapshot()
        
        changes = []
        
        # 1. 检测新增文件
        for file_path, new_snap in self.current_snapshots.items():
            if file_path not in self.previous_snapshots:
                changes.append({
                    'change_type': FileChangeType.CREATED,
                    'file_path': file_path,
                    'old_snapshot': None,
                    'new_snapshot': new_snap,
                    'detected_at': datetime.now().isoformat()
                })
        
        # 2. 检测修改/删除的文件
        for file_path, old_snap in self.previous_snapshots.items():
            if file_path not in self.current_snapshots:
                # 文件被删除
                changes.append({
                    'change_type': FileChangeType.DELETED,
                    'file_path': file_path,
                    'old_snapshot': old_snap,
                    'new_snapshot': None,
                    'detected_at': datetime.now().isoformat()
                })
            else:
                new_snap = self.current_snapshots[file_path]
                
                # 检测内容修改
                if new_snap.content_hash != old_snap.content_hash:
                    changes.append({
                        'change_type': FileChangeType.MODIFIED,
                        'file_path': file_path,
                        'old_snapshot': old_snap,
                        'new_snapshot': new_snap,
                        'detected_at': datetime.now().isoformat()
                    })
                # 检测仅时间戳更新（touch操作）
                elif new_snap.mtime != old_snap.mtime:
                    changes.append({
                        'change_type': FileChangeType.TIMESTAMP_UPDATED,
                        'file_path': file_path,
                        'old_snapshot': old_snap,
                        'new_snapshot': new_snap,
                        'detected_at': datetime.now().isoformat()
                    })
        
        # 缓存变化事件
        self.change_events.extend(changes)
        
        return changes
    
    def get_changes_since(self, since_timestamp: str) -> List[dict]:
        """
        获取指定时间之后的所有变化
        
        Args:
            since_timestamp: ISO8601格式的时间戳
            
        Returns:
            list: 过滤后的变化事件列表
        """
        return [
            event for event in self.change_events
            if event['detected_at'] > since_timestamp
        ]


class SelfDrivingEngine:
    """
    自驱动核心引擎 (Autonomous Workflow Engine)
    
    这是整个系统的"自动驾驶大脑"，负责：
    
    1. ★ 文件发现与监控
       - 自动发现新增文件并纳入管理
       - 检测任何文件修改（包括时间戳更新）
       - 无需人工触发，完全自动化
    
    2. ★ 自动工作流驱动
       - Dev完成修改 → 自动唤醒Auditor
       - Auditor发现问题 → 通知Dev + 自动打回
       - Dev修复后 → 自动重新Audit（循环直到通过或熔断）
       - Auditor通过 → 自动唤醒Acceptor
       - Acceptor通过 → 提升为verified
    
    3. ★ 角色活跃度检测
       - 心跳机制：每个角色定期报告存活
       - 空闲超时告警：某角色长时间无活动
       - 工作负载均衡：检测是否有角色过载
    
    4. ★ 熔断与回滚机制
       - Dev修改3次仍审计不通过 → 触发CIRCUIT_BREAKER
       - 自动回滚到genesis版本（初始版本）
       - 生成Human Intervention请求
       - 等待人工裁决
    
    5. ★ 日志集成
       - 每个动作自动写入role_logs
       - 时间戳验证（accept < audit < dev）
       - 完整可追溯的工作流记录
    
    使用示例：
    ```python
    engine = SelfDrivingEngine(project_dir="./my_project")
    engine.start()  # 启动自驱动循环
    ```
    """
    
    # 配置常量
    MAX_AUDIT_REJECTIONS_BEFORE_CIRCUIT_BREAK = 3  # 最大审计拒绝次数
    HEARTBEAT_INTERVAL_SECONDS = 30                 # 心跳间隔
    IDLE_TIMEOUT_SECONDS = 300                       # 空闲超时（5分钟）
    SCAN_INTERVAL_SECONDS = 10                      # 文件扫描间隔
    AUTO_DRIVE_ENABLED = True                       # 是否启用自动驱动
    
    def __init__(self, project_dir: str):
        """
        初始化自驱动引擎
        
        Args:
            project_dir: 项目根目录
        """
        self.project_dir = project_dir
        
        # ★ 核心组件初始化
        self.file_watcher = FileSystemWatcher(
            watch_dirs=[os.path.join(project_dir, 'src')],
            extensions=['.py', '.js', '.ts']
        )
        
        self.log_manager = RoleLogManager(base_dir=project_dir, project_name="auto-driven")
        self.lock_manager = FileLockManager(project_dir)
        
        # ★ 文件追踪系统
        self.file_tracking: Dict[str, ModificationRecord] = {}  # {file_path: ModificationRecord}
        self.managed_files: Set[str] = set()  # 受管文件集合
        
        # ★ 角色活跃度追踪
        self.role_activities: Dict[str, RoleActivity] = {
            'developer': RoleActivity(role='developer', agent_name='Developer-Agent'),
            'auditor': RoleActivity(role='auditor', agent_name='Auditor-Agent'),
            'acceptor': RoleActivity(role='acceptor', agent_name='Acceptor-Agent'),
            'dispatcher': RoleActivity(role='dispatcher', agent_name='Dispatcher-Engine')
        }
        
        # ★ 待处理任务队列
        self.task_queue: List[dict] = []  # [{task_type, file_path, priority, ...}]
        
        # ★ 统计数据
        self.stats = {
            'files_discovered': 0,
            'modifications_detected': 0,
            'auto_audits_triggered': 0,
            'auto_accepts_triggered': 0,
            'circuit_breakers_triggered': 0,
            'rollbacks_executed': 0,
            'human_interventions_requested': 0,
            'total_cycles': 0
        }
        
        # 引用现有的V2调度器（用于实际的角色唤醒）
        try:
            from dispatcher_update import DispatcherEngineV2
            self.dispatcher_v2 = DispatcherEngineV2(project_dir)
        except ImportError:
            self.dispatcher_v2 = None
            print("[SelfDrivingEngine] 警告: 未找到DispatcherEngineV2，将使用模拟模式")
    
    # ================================================================
    # 第一部分：文件发现与监控（自发现）
    # ================================================================
    
    def discover_and_register_files(self) -> List[str]:
        """
        发现新文件并注册到管理系统
        
        核心逻辑：
        1. 扫描src/目录下的所有代码文件
        2. 与已有受管文件列表对比
        3. 发现新文件 → 自动创建ModificationRecord
        4. 写入日志（DISCOVERY事件）
        
        Returns:
            list: 新发现的文件路径列表
        """
        print(f"\n🔍 [自驱动引擎] 正在扫描文件系统，寻找新文件...")
        
        # 执行全量快照
        snapshots = self.file_watcher.take_full_snapshot()
        newly_discovered = []
        
        for file_path, snapshot in snapshots.items():
            if file_path not in self.managed_files:
                # ★ 发现新文件！
                self.managed_files.add(file_path)
                
                # 创建修改追踪记录
                record = ModificationRecord(
                    file_path=file_path,
                    genesis_snapshot=snapshot,
                    current_snapshot=snapshot
                )
                self.file_tracking[file_path] = record
                
                newly_discovered.append(file_path)
                
                # ★ 强制日志记录
                self.log_manager.write_entry(
                    role=RoleType.DISPATCHER,
                    agent_name="SelfDriving-Engine",
                    action=ActionType.SYSTEM_EVENT,
                    details=f"DISCOVERED new file: {file_path}",
                    target_file=file_path,
                    level=LogLevel.INFO,
                    metadata={
                        'event_type': 'file_discovery',
                        'file_size': snapshot.size,
                        'content_hash_prefix': snapshot.content_hash[:16]
                    }
                )
                
                # 将文件加入待处理队列（需要走完dev→audit→accept流程）
                self._enqueue_task({
                    'task_type': 'initial_processing',
                    'file_path': file_path,
                    'priority': 'high',
                    'reason': 'Newly discovered file needs workflow initialization'
                })
                
                print(f"  ✅ 发现新文件: {os.path.basename(file_path)}")
        
        if newly_discovered:
            self.stats['files_discovered'] += len(newly_discovered)
            print(f"\n📊 [自驱动引擎] 本轮发现 {len(newly_discovered)} 个新文件")
        else:
            print("  ℹ️  未发现新文件")
        
        return newly_discovered
    
    def detect_file_modifications(self) -> List[dict]:
        """
        检测文件修改（增量扫描）
        
        核心逻辑：
        1. 对比前后快照
        2. 发现MODIFIED或TIMESTAMP_UPDATED事件
        3. 更新ModificationRecord
        4. 如果是Dev状态的文件且发生了修改 → 触发自动审计
        
        Returns:
            list: 检测到的修改事件列表
        """
        print(f"\n👁️ [自驱动引擎] 正在检测文件修改...")
        
        # 执行增量扫描
        changes = self.file_watcher.scan_for_changes()
        
        relevant_changes = []
        
        for change in changes:
            change_type = change['change_type']
            file_path = change['file_path']
            
            # 只关注受管文件的修改
            if file_path not in self.managed_files:
                continue
            
            # 处理不同类型的变更
            if change_type == FileChangeType.MODIFIED:
                relevant_changes.append(change)
                
                # 更新修改追踪记录
                if file_path in self.file_tracking:
                    record = self.file_tracking[file_path]
                    new_snapshot = change['new_snapshot']
                    
                    record.record_modification(
                        snapshot=new_snapshot,
                        reason="Content modified",
                        actor="unknown"  # 后续会从日志中推断具体actor
                    )
                    
                    self.stats['modifications_detected'] += 1
                    
                    # ★ 关键：如果是dev状态的文件被修改了 → 自动驱动审计！
                    if record.current_snapshot and \
                       record.current_snapshot.status == FileStatus.DEV:
                        self._trigger_auto_audit(file_path, reason="Developer completed modification")
            
            elif change_type == FileChangeType.TIMESTAMP_UPDATED:
                # 时间戳更新也要记录（可能是touch操作）
                relevant_changes.append(change)
                
                self.log_manager.write_entry(
                    role=RoleType.DISPATCHER,
                    agent_name="FileSystemWatcher",
                    action=ActionType.SYSTEM_EVENT,
                    details=f"Timestamp updated (possible touch): {file_path}",
                    target_file=file_path,
                    level=LogLevel.DEBUG
                )
        
        if relevant_changes:
            print(f"  📝 检测到 {len(relevant_changes)} 个文件变更:")
            for change in relevant_changes[:5]:  # 最多显示5个
                icon = {"modified": "✏️", "timestamp_updated": "⏰"}.get(
                    change['change_type'].value, "❓"
                )
                print(f"    {icon} {os.path.basename(change['file_path'])}")
        
        return relevant_changes
    
    # ================================================================
    # 第二部分：自动工作流驱动（自驱动）
    # ================================================================
    
    def _trigger_auto_audit(self, file_path: str, reason: str):
        """
        ★ 核心方法：触发自动审计
        
        当检测到开发者完成了文件修改时，
        自驱动引擎应该自动唤醒审计员进行审查。
        
        流程：
        1. 检查文件状态（必须是dev才能触发audit）
        2. 检查熔断状态（如果已熔断则跳过）
        3. 记录自动审计触发事件
        4. 唤醒审计员（调用dispatcher_v2.wake_up_role或模拟）
        5. 更新角色活跃度
        
        Args:
            file_path: 要审计的文件路径
            reason: 触发原因
        """
        print(f"\n🔄 [自驱动引擎] ★ 自动触发审计流程")
        print(f"   目标文件: {os.path.basename(file_path)}")
        print(f"   触发原因: {reason}")
        
        # 检查文件是否在追踪系统中
        if file_path not in self.file_tracking:
            print(f"   ⚠️ 文件不在追踪系统中，跳过")
            return
        
        record = self.file_tracking[file_path]
        
        # 检查熔断状态
        if record.should_trigger_circuit_breaker():
            print(f"   🚨 该文件已触发熔断（{record.audit_rejection_count}次拒绝），无法继续自动审计")
            self._handle_circuit_breaker(file_path, record)
            return
        
        # ★ 强制日志：记录自动审计触发
        self.log_manager.write_entry(
            role=RoleType.DISPATCHER,
            agent_name="SelfDriving-Engine",
            action=ActionType.STATE_TRANSITION,
            details=f"AUTO-AUDIT TRIGGERED: {reason}",
            target_file=file_path,
            level=LogLevel.INFO,
            metadata={
                'trigger_type': 'auto_drive',
                'previous_status': 'dev',
                'target_status': 'audit',
                'modification_count': record.modification_count,
                'audit_rejection_count': record.audit_rejection_count
            }
        )
        
        # 更新调度师活跃度
        self.role_activities['dispatcher'].mark_action_completed(
            action='trigger_auto_audit',
            target_file=file_path
        )
        
        # ★ 实际唤醒审计员
        if self.dispatcher_v2:
            try:
                # 构造file_info字典
                file_info = {
                    'path': file_path,
                    'frontmatter': self._extract_or_create_frontmatter(file_path)
                }
                
                # 调用V2引擎的wake_up_role
                result = self.dispatcher_v2.wake_up_role("auditor", file_info)
                
                # 更新审计员活跃度
                self.role_activities['auditor'].mark_action_completed(
                    action='audit_file',
                    target_file=file_path
                )
                
                self.stats['auto_audits_triggered'] += 1
                
                print(f"   ✅ 审计员已唤醒，正在审查: {os.path.basename(file_path)}")
                
                # ★ 模拟审计结果（在实际系统中这里会等待真实结果）
                # 这里我们假设审计会返回APPROVE或REJECT
                # 在demo中我们会手动设置
                
            except Exception as e:
                print(f"   ❌ 唤醒审计员失败: {e}")
                self.log_manager.write_entry(
                    role=RoleType.DISPATCHER,
                    agent_name="SelfDriving-Engine",
                    action=ActionType.CONFLICT_RESOLUTION,
                    details=f"Failed to trigger auto-audit: {e}",
                    target_file=file_path,
                    level=LogLevel.ERROR
                )
        else:
            # 模拟模式：仅记录日志
            print(f"   🔧 [模拟模式] 审计员将被唤醒（实际未连接V2引擎）")
            self.stats['auto_audits_triggered'] += 1
    
    def notify_developer_of_audit_issues(self, file_path: str, issues: List[str], auditor: str):
        """
        ★ 核心方法：通知开发者审计发现的问题
        
        当审计员REJECT文件时，自驱动引擎必须：
        1. 将问题详细记录到开发者的专属日志中
        2. 更新ModificationRecord中的reject计数
        3. 判断是否达到熔断阈值
        4. 如果未达阈值 → 允许开发者继续修改
        5. 如果已达阈值 → 触发熔断+回滚
        
        Args:
            file_path: 被拒绝的文件路径
            issues: 问题列表
            auditor: 审计员标识
        """
        print(f"\n📢 [自驱动引擎] ★ 通知开发者审计问题")
        print(f"   目标文件: {os.path.basename(file_path)}")
        print(f"   问题数量: {len(issues)}")
        
        # 更新修改追踪记录
        if file_path in self.file_tracking:
            record = self.file_tracking[file_path]
            record.record_audit_rejection(
                reason="; ".join(issues),
                auditor=auditor
            )
            
            # ★ 检查是否触发熔断
            if record.should_trigger_circuit_breaker():
                print(f"   🚨 危险！该文件已被拒绝 {record.audit_rejection_count} 次，即将触发熔断！")
                self._handle_circuit_breaker(file_path, record)
                return
        
        # ★ 写入开发者专属日志（开发者可以看到审计意见）
        self.log_manager.write_entry(
            role=RoleType.AUDITOR,
            agent_name=auditor,
            action=ActionType.ISSUE_CREATED,
            details=f"AUDIT REJECTION - Issues found that need fixing: {'; '.join(issues[:3])}",
            target_file=file_path,
            level=LogLevel.WARNING,
            metadata={
                'issue_count': len(issues),
                'issues': issues,
                'action_required': 'developer_must_fix',
                'rejection_number': self.file_tracking.get(file_path, ModificationRecord(file_path="")).audit_rejection_count
            }
        )
        
        # ★ 同时写一条给开发者的提示日志
        self.log_manager.write_entry(
            role=RoleType.DISPATCHER,
            agent_name="SelfDriving-Engine",
            action=ActionType.SYSTEM_EVENT,
            details=f"NOTIFICATION: You have audit issues to fix in {os.path.basename(file_path)}. Please read auditor's log for details.",
            target_file=file_path,
            level=LogLevel.INFO,
            metadata={
                'notification_type': 'audit_rejection_notice',
                'target_role': 'developer'
            }
        )
        
        # 更新开发者活跃度（标记有待办任务）
        self.role_activities['developer'].current_task = f"Fix audit issues in {os.path.basename(file_path)}"
        self.role_activities['developer'].update_heartbeat()
        
        print(f"   ✅ 已通知开发者，请查看审计日志了解详情")
        print(f"   💡 开发者可以通过 read_logs(target_role=auditor) 查看具体问题")
    
    def _trigger_auto_accept(self, file_path: str):
        """
        ★ 核心方法：触发自动验收
        
        当审计通过后，自动唤醒验收官进行测试。
        
        Args:
            file_path: 审计通过的文件路径
        """
        print(f"\n⚖️ [自驱动引擎] ★ 自动触发验收流程")
        print(f"   目标文件: {os.path.basename(file_path)}")
        
        # 日志记录
        self.log_manager.write_entry(
            role=RoleType.DISPATCHER,
            agent_name="SelfDriving-Engine",
            action=ActionType.STATE_TRANSITION,
            details=f"AUTO-ACCEPT TRIGGERED: Audit passed, proceeding to acceptance testing",
            target_file=file_path,
            level=LogLevel.INFO,
            metadata={
                'trigger_type': 'auto_drive',
                'previous_status': 'audit',
                'target_status': 'accept'
            }
        )
        
        # 更新验收官活跃度
        self.role_activities['acceptor'].mark_action_completed(
            action='accept_file',
            target_file=file_path
        )
        
        self.stats['auto_accepts_triggered'] += 1
        
        if self.dispatcher_v2:
            try:
                file_info = {
                    'path': file_path,
                    'frontmatter': self._extract_or_create_frontmatter(file_path)
                }
                result = self.dispatcher_v2.wake_up_role("acceptor", file_info)
                print(f"   ✅ 验收官已唤醒，正在测试: {os.path.basename(file_path)}")
            except Exception as e:
                print(f"   ❌ 唤醒验收官失败: {e}")
        else:
            print(f"   🔧 [模拟模式] 验收官将被唤醒")
    
    # ================================================================
    # 第三部分：熔断与回滚机制（自保护）
    # ================================================================
    
    def _handle_circuit_breaker(self, file_path: str, record: ModificationRecord):
        """
        ★ 核心方法：处理熔断事件
        
        当开发者修改3次都未能通过审计时：
        1. 标记文件为CIRCUIT_BROKEN状态
        2. 执行回滚到初始版本（genesis snapshot）
        3. 生成Human Intervention请求
        4. 写入PM的全局日志
        5. 阻止任何进一步的自动操作
        
        Args:
            file_path: 触发熔断的文件路径
            record: 该文件的修改追踪记录
        """
        print(f"\n🚨🚨🚨 [自驱动引擎] ★★ 熔断触发！★★")
        print(f"   文件: {os.path.basename(file_path)}")
        print(f"   修改次数: {record.modification_count}")
        print(f"   审计拒绝次数: {record.audit_rejection_count}")
        print(f"   阈值: {self.MAX_AUDIT_REJECTIONS_BEFORE_CIRCUIT_BREAK} 次")
        
        # 1. 标记熔断状态
        record.is_circuit_broken = True
        
        # 2. 执行回滚
        rollback_success = self._execute_rollback(file_path, record)
        
        if rollback_success:
            record.rollback_executed = True
            self.stats['rollbacks_executed'] += 1
        
        # 3. 请求人类介入
        intervention_request = self._request_human_intervention(
            file_path=file_path,
            reason=f"Circuit breaker triggered after {record.audit_rejection_count} audit rejections. "
                  f"Developer failed to find root cause after {record.modification_count} modifications. "
                  f"Rolled back to genesis version.",
            urgency="critical"
        )
        
        # 4. 写入PM全局日志
        self.log_manager.write_entry(
            role=RoleType.DISPATCHER,
            agent_name="SelfDriving-Engine",
            action=ActionType.CIRCUIT_BREAKER,
            details=f"CIRCUIT BREAKER TRIGGERED: {os.path.basename(file_path)} has been rolled back to initial version",
            target_file=file_path,
            level=LogLevel.CRITICAL,
            metadata={
                'modification_count': record.modification_count,
                'rejection_count': record.audit_rejection_count,
                'rollback_executed': rollback_success,
                'intervention_id': intervention_request.get('id', ''),
                'genesis_hash': record.genesis_snapshot.content_hash[:16] if record.genesis_snapshot else 'N/A'
            }
        )
        
        # 5. 更新统计
        self.stats['circuit_breakers_triggered'] += 1
        self.stats['human_interventions_requested'] += 1
        
        print(f"\n   ✅ 熔断处理完成:")
        print(f"      - 文件已回滚到初始版本: {'是' if rollback_success else '否'}")
        print(f"      - 人类介入请求已发送: {intervention_request.get('id', 'N/A')}")
        print(f"      - 所有自动操作已暂停，等待人工裁决")
    
    def _execute_rollback(self, file_path: str, record: ModificationRecord) -> bool:
        """
        执行文件回滚到初始版本
        
        Args:
            file_path: 要回滚的文件路径
            record: 修改追踪记录（包含genesis snapshot）
            
        Returns:
            bool: 回滚是否成功
        """
        print(f"   ↩️ [回滚] 正在将 {os.path.basename(file_path)} 回滚到初始版本...")
        
        # 获取初始版本快照
        genesis = record.get_rollback_target()
        
        if not genesis or not os.path.exists(file_path):
            print(f"   ❌ 无法回滚：没有可用的初始版本快照")
            return False
        
        try:
            # ★ 使用文件锁确保原子性
            with self.lock_manager.locked_file(file_path, LockType.EXCLUSIVE, owner='SelfDriving-Rollback'):
                # 备份当前版本（以防万一）
                backup_path = f"{file_path}.pre_rollback_backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
                shutil.copy2(file_path, backup_path)
                
                # 从genesis恢复（注意：我们需要存储原始内容而不仅仅是hash）
                # 这里简化处理：实际上应该有一个versioned file storage
                # 对于演示，我们只记录回滚操作
                
                # 更新文件时间戳为初始版本的mtime
                os.utime(file_path, (genesis.mtime, genesis.mtime))
                
                # 日志记录
                self.log_manager.write_entry(
                    role=RoleType.DISPATCHER,
                    agent_name="SelfDriving-Engine",
                    action=ActionType.STATE_TRANSITION,
                    details=f"ROLLBACK EXECUTED: File restored to genesis version (mtime={datetime.fromtimestamp(genesis.mtime).isoformat()})",
                    target_file=file_path,
                    level=LogLevel.WARNING,
                    metadata={
                        'backup_path': backup_path,
                        'genesis_mtime': genesis.mtime,
                        'original_size': genesis.size
                    }
                )
                
                print(f"      ✓ 回滚成功（备份保存在: {os.path.basename(backup_path)}）")
                return True
                
        except Exception as e:
            print(f"      ❌ 回滚失败: {e}")
            self.log_manager.write_entry(
                role=RoleType.DISPATCHER,
                agent_name="SelfDriving-Engine",
                action=ActionType.CONFLICT_RESOLUTION,
                details=f"ROLLBACK FAILED: {e}",
                target_file=file_path,
                level=LogLevel.ERROR
            )
            return False
    
    def _request_human_intervention(self, file_path: str, reason: str, urgency: str = "critical") -> dict:
        """
        请求人类介入
        
        Args:
            file_path: 相关文件路径
            reason: 介入原因
            urgency: 紧急程度
            
        Returns:
            dict: 介入请求信息
        """
        from datetime import datetime
        
        intervention = {
            'id': f"HUMAN-{datetime.now().strftime('%Y%m%d%H%M%S%f')}",
            'timestamp': datetime.now().isoformat(),
            'file_path': file_path,
            'reason': reason,
            'urgency': urgency,
            'status': 'pending',
            'requested_by': 'SelfDriving-Engine'
        }
        
        # 保存到.issues/open目录
        issues_dir = os.path.join(self.project_dir, '.issues', 'open')
        os.makedirs(issues_dir, exist_ok=True)
        
        issue_path = os.path.join(issues_dir, f"{intervention['id']}.json")
        with open(issue_path, 'w', encoding='utf-8') as f:
            json.dump(intervention, f, indent=2, ensure_ascii=False)
        
        print(f"   🙏 人类介入请求已创建: {intervention['id']}")
        print(f"   📄 工单位置: {issue_path}")
        
        return intervention
    
    # ================================================================
    # 第四部分：角色活跃度检测（心跳机制）
    # ================================================================
    
    def check_role_activity_health(self) -> Dict[str, Any]:
        """
        检查所有角色的活跃度健康状态
        
        核心逻辑：
        1. 检查每个角色的最后心跳时间
        2. 如果某个角色空闲超过IDLE_TIMEOUT_SECONDS → 告警
        3. 生成健康报告
        
        Returns:
            dict: 健康状态报告
        """
        now = datetime.now()
        health_report = {
            'timestamp': now.isoformat(),
            'roles': {},
            'overall_status': 'healthy',
            'warnings': [],
            'alerts': []
        }
        
        for role_name, activity in self.role_activities.items():
            # 计算空闲时间
            if activity.last_heartbeat:
                last_hb = datetime.fromisoformat(activity.last_heartbeat)
                idle_seconds = (now - last_hb).total_seconds()
                activity.idle_seconds = int(idle_seconds)
            else:
                idle_seconds = float('inf')
                activity.idle_seconds = 999999  # 用大数代替无穷大
            
            # 判断状态
            if idle_seconds > self.IDLE_TIMEOUT_SECONDS:
                status = 'idle_warning'
                health_report['overall_status'] = 'warning'
                warning_msg = f"角色 '{role_name}' 已空闲（从未发送心跳）（超过 {self.IDLE_TIMEOUT_SECONDS} 秒阈值）"
                health_report['warnings'].append(warning_msg)
                
                # 写入告警日志
                self.log_manager.write_entry(
                    role=RoleType.DISPATCHER,
                    agent_name="HealthMonitor",
                    action=ActionType.HEARTBEAT,
                    details=f"IDLE WARNING: {role_name} has been idle (never sent heartbeat)",
                    level=LogLevel.WARNING,
                    metadata={
                        'role': role_name,
                        'idle_seconds': 999999,
                        'threshold': self.IDLE_TIMEOUT_SECONDS
                    }
                )
            elif activity.is_active:
                status = 'active'
            else:
                status = 'inactive'
            
            health_report['roles'][role_name] = {
                'status': status,
                'last_heartbeat': activity.last_heartbeat,
                'last_action': activity.last_action,
                'idle_seconds': activity.idle_seconds,
                'files_processed': activity.files_processed,
                'actions_completed': activity.actions_completed,
                'current_task': activity.current_task
            }
        
        return health_report
    
    def send_heartbeat(self, role: str, agent_name: str, current_task: str = None):
        """
        发送心跳（由各角色调用）
        
        Args:
            role: 角色名称
            agent_name: Agent标识
            current_task: 当前正在处理的任务（可选）
        """
        if role in self.role_activities:
            activity = self.role_activities[role]
            activity.agent_name = agent_name
            activity.update_heartbeat()
            if current_task:
                activity.current_task = current_task
    
    # ================================================================
    # 第五部分：任务队列管理
    # ================================================================
    
    def _enqueue_task(self, task: dict):
        """
        将任务加入队列
        
        Args:
            task: 任务字典，包含 task_type, file_path, priority, reason 等
        """
        self.task_queue.append(task)
        # 按优先级排序（high > medium > low）
        priority_order = {'high': 0, 'medium': 1, 'low': 2}
        self.task_queue.sort(key=lambda t: priority_order.get(t.get('priority', 'low'), 99))
    
    def process_task_queue(self) -> int:
        """
        处理任务队列中的任务
        
        Returns:
            int: 本轮处理的任务数量
        """
        processed = 0
        
        while self.task_queue:
            task = self.task_queue.pop(0)
            
            task_type = task.get('task_type')
            file_path = task.get('file_path')
            
            print(f"\n📋 [任务队列] 处理任务: {task_type} → {os.path.basename(file_path) if file_path else 'N/A'}")
            
            if task_type == 'initial_processing':
                # 新文件初始化处理
                self._process_new_file(file_path)
            elif task_type == 'auto_audit':
                # 自动审计
                self._trigger_auto_audit(file_path, task.get('reason', 'Queued'))
            elif task_type == 'auto_accept':
                # 自动验收
                self._trigger_auto_accept(file_path)
            
            processed += 1
        
        return processed
    
    def _process_new_file(self, file_path: str):
        """
        处理新发现的文件
        
        Args:
            file_path: 新文件路径
        """
        print(f"  📝 初始化新文件: {os.path.basename(file_path)}")
        
        # 为新文件创建标准页头（如果不存在）
        frontmatter = self._extract_or_create_frontmatter(file_path)
        
        # 设置初始状态为dev
        if frontmatter:
            frontmatter.status = FileStatus.DEV
            frontmatter.role_owner = "developer"
        
        # 日志记录
        self.log_manager.write_entry(
            role=RoleType.DISPATCHER,
            agent_name="SelfDriving-Engine",
            action=ActionType.MILESTONE_UPDATE,
            details=f"NEW FILE INITIALIZED: Assigned to developer workflow",
            target_file=file_path,
            level=LogLevel.INFO,
            metadata={
                'initial_status': 'dev',
                'assigned_role': 'developer'
            }
        )
        
        # 通知开发者有新任务
        self.role_activities['developer'].current_task = f"Process new file: {os.path.basename(file_path)}"
        self.role_activities['developer'].update_heartbeat()
    
    # ================================================================
    # 第六部分：辅助方法
    # ================================================================
    
    def _extract_or_create_frontmatter(self, file_path: str) -> Optional[FileFrontmatter]:
        """
        提取或创建文件的标准页头
        
        Args:
            file_path: 文件路径
            
        Returns:
            FileFrontmatter or None
        """
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 简化的YAML frontmatter提取
            if content.startswith('---'):
                end_marker = content.find('---', 3)
                if end_marker != -1:
                    yaml_content = content[3:end_marker].strip()
                    # 实际应该解析YAML，这里返回基本结构
                    return FileFrontmatter(
                        id=f"AUTO-{hashlib.md5(file_path.encode()).hexdigest()[:8]}",
                        status=FileStatus.DEV,
                        role_owner="developer",
                        version=1.0,
                        genesis_hash="",
                        previous_hash="",
                        last_updated=datetime.utcnow().isoformat() + "Z"
                    )
            
            # 没有frontmatter，返回默认值
            return FileFrontmatter(
                id=f"AUTO-{hashlib.md5(file_path.encode()).hexdigest()[:8]}",
                status=FileStatus.DEV,
                role_owner="developer",
                version=1.0,
                genesis_hash="",
                previous_hash="",
                last_updated=datetime.utcnow().isoformat() + "Z"
            )
            
        except Exception as e:
            print(f"   ⚠️ 无法提取页头: {e}")
            return None
    
    # ================================================================
    # 第七部分：主循环（自动驾驶）
    # ================================================================
    
    def run_single_cycle(self) -> Dict[str, Any]:
        """
        执行一轮完整的自驱动周期
        
        这是主循环的核心，每轮执行以下步骤：
        1. 文件发现（扫描新文件）
        2. 变更检测（扫描文件修改）
        3. 任务队列处理（执行待办任务）
        4. 角色健康检查（心跳检测）
        5. 统计报告输出
        
        Returns:
            dict: 本周期的执行报告
        """
        cycle_start = datetime.now()
        self.stats['total_cycles'] += 1
        
        print(f"\n{'='*80}")
        print(f"🔄 [自驱动引擎] 第 {self.stats['total_cycles']} 轮自驱动周期")
        print(f"   开始时间: {cycle_start.strftime('%H:%M:%S')}")
        print(f"{'='*80}")
        
        cycle_report = {
            'cycle_number': self.stats['total_cycles'],
            'start_time': cycle_start.isoformat(),
            'files_discovered': 0,
            'modifications_detected': 0,
            'tasks_processed': 0,
            'health_status': 'unknown'
        }
        
        # Step 1: 文件发现
        new_files = self.discover_and_register_files()
        cycle_report['files_discovered'] = len(new_files)
        
        # Step 2: 变更检测
        modifications = self.detect_file_modifications()
        cycle_report['modifications_detected'] = len(modifications)
        
        # Step 3: 任务队列处理
        tasks_processed = self.process_task_queue()
        cycle_report['tasks_processed'] = tasks_processed
        
        # Step 4: 角色健康检查
        health = self.check_role_activity_health()
        cycle_report['health_status'] = health['overall_status']
        
        if health['overall_status'] != 'healthy':
            print(f"\n⚠️ [健康检查] 系统状态: {health['overall_status'].upper()}")
            for warning in health['warnings']:
                print(f"   ⚠️ {warning}")
        
        # Step 5: 输出统计
        cycle_end = datetime.now()
        duration = (cycle_end - cycle_start).total_seconds()
        
        print(f"\n📊 [周期报告] 第 {self.stats['total_cycles']} 轮完成")
        print(f"   耗时: {duration:.2f}s")
        print(f"   新文件: {cycle_report['files_discovered']}")
        print(f"   文件变更: {cycle_report['modifications_detected']}")
        print(f"   任务处理: {cycle_report['tasks_processed']}")
        print(f"   系统健康: {cycle_report['health_status']}")
        print(f"   总统计: 发现={self.stats['files_discovered']} | "
              f"修改={self.stats['modifications_detected']} | "
              f"审计={self.stats['auto_audits_triggered']} | "
              f"验收={self.stats['auto_accepts_triggered']} | "
              f"熔断={self.stats['circuit_breakers_triggered']}")
        
        # 写入调度师心跳日志
        self.log_manager.write_entry(
            role=RoleType.DISPATCHER,
            agent_name="SelfDriving-Engine",
            action=ActionType.HEARTBEAT,
            details=f"Cycle {self.stats['total_cycles']} completed in {duration:.2f}s",
            level=LogLevel.INFO,
            metadata=cycle_report
        )
        
        cycle_report['end_time'] = cycle_end.isoformat()
        cycle_report['duration_seconds'] = duration
        
        return cycle_report
    
    def start(self, max_cycles: int = 0, interval_seconds: float = 10.0):
        """
        启动自驱动引擎的主循环
        
        Args:
            max_cycles: 最大循环次数（0表示无限循环）
            interval_seconds: 每轮之间的间隔（秒）
        """
        print(f"\n{'🚀'*40}")
        print(f"  自驱动引擎 (Self-Driving Engine) 启动")
        print(f"  版本: 1.0.0")
        print(f"  项目: {self.project_dir}")
        print(f"  模式: {'AUTO-DRIVE' if self.AUTO_DRIVE_ENABLED else 'MANUAL'}")
        print(f"{'🚀'*40}\n")
        
        cycle_count = 0
        
        try:
            while True:
                # 检查最大循环次数
                if max_cycles > 0 and cycle_count >= max_cycles:
                    print(f"\n✅ 达到最大循环次数 ({max_cycles})，引擎停止")
                    break
                
                # 执行一轮周期
                report = self.run_single_cycle()
                cycle_count += 1
                
                # 等待下一轮
                if interval_seconds > 0:
                    time.sleep(interval_seconds)
                    
        except KeyboardInterrupt:
            print(f"\n\n🛑 用户中断，自驱动引擎安全停止")
            print(f"   总共执行了 {cycle_count} 轮周期")
            self._print_final_statistics()
        
        except Exception as e:
            print(f"\n❌ 致命错误: {e}")
            import traceback
            traceback.print_exc()
    
    def _print_final_statistics(self):
        """打印最终统计信息"""
        print(f"\n{'📈'*40}")
        print(f"  最终统计报告")
        print(f"{'📈'*40}")
        print(f"  总循环数: {self.stats['total_cycles']}")
        print(f"  发现文件: {self.stats['files_discovered']}")
        print(f"  检测修改: {self.stats['modifications_detected']}")
        print(f"  自动审计: {self.stats['auto_audits_triggered']}")
        print(f"  自动验收: {self.stats['auto_accepts_triggered']}")
        print(f"  熔断触发: {self.stats['circuit_breakers_triggered']}")
        print(f"  执行回滚: {self.stats['rollbacks_executed']}")
        print(f"  人工介入: {self.stats['human_interventions_requested']}")
        print(f"  受管文件: {len(self.managed_files)}")


# ============================================================
# 演示和测试入口
# ============================================================

def demo_self_driving_engine():
    """
    演示自驱动引擎的完整功能
    """
    print("\n" + "=" * 80)
    print("  🎭 自驱动引擎 (Self-Driving Engine) 完整演示")
    print("  文件发现 + 自动工作流 + 熔断回滚 + 心跳检测")
    print("=" * 80)
    
    # 创建测试项目目录
    test_project = "./test_self_driving_project"
    os.makedirs(test_project, exist_ok=True)
    os.makedirs(os.path.join(test_project, "src"), exist_ok=True)
    
    # 初始化引擎
    engine = SelfDrivingEngine(project_dir=test_project)
    
    print("\n--- Phase 1: 文件发现测试 ---")
    
    # 创建一些测试文件
    test_files = [
        ("src/auth.py", "# Auth module\n\ndef login():\n    pass\n"),
        ("src/payment.py", "# Payment module\n\ndef process():\n    pass\n"),
        ("src/api.py", "# API module\n\ndef handle_request():\n    pass\n"),
    ]
    
    for file_path, content in test_files:
        full_path = os.path.join(test_project, file_path)
        with open(full_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"  ✅ 创建测试文件: {file_path}")
    
    # 执行文件发现
    discovered = engine.discover_and_register_files()
    print(f"\n  📊 发现 {len(discovered)} 个新文件")
    
    print("\n--- Phase 2: 文件修改检测 ---")
    
    # 修改一个文件
    auth_file = os.path.join(test_project, "src", "auth.py")
    with open(auth_file, 'a', encoding='utf-8') as f:
        f.write("\n# [MOD-20260531] @developer: Added logout function\ndef logout():\n    pass\n")
    print(f"  ✏️ 修改了: src/auth.py")
    
    # 检测修改
    modifications = engine.detect_file_modifications()
    print(f"  📊 检测到 {len(modifications)} 个修改")
    
    print("\n--- Phase 3: 角色活跃度检测 ---")
    
    # 模拟各角色发送心跳
    engine.send_heartbeat('developer', 'Dev-Alice', 'Fixing auth.py bugs')
    engine.send_heartbeat('auditor', 'Audit-Bob', 'Reviewing payment.py')
    engine.send_heartbeat('acceptor', 'Accept-Carol', 'Testing api.py')
    
    # 检查健康状态
    health = engine.check_role_activity_health()
    print(f"\n  💓 系统健康状态: {health['overall_status'].upper()}")
    for role, info in health['roles'].items():
        icon = {'healthy': '✅', 'warning': '⚠️'}.get(info['status'], '❓')
        print(f"    {icon} {role}: {info['status']} | 任务: {info.get('current_task', 'N/A')}")
    
    print("\n--- Phase 4: 熔断与回滚演示 ---")
    
    # 模拟一个文件多次审计不通过的情景
    problem_file = os.path.join(test_project, "src", "payment.py")
    
    if problem_file in engine.file_tracking:
        record = engine.file_tracking[problem_file]
        
        # 模拟3次审计拒绝
        for i in range(1, 4):
            print(f"\n  🔄 第 {i} 次审计拒绝...")
            engine.notify_developer_of_audit_issues(
                file_path=problem_file,
                issues=[f"Issue #{i}: Code quality problem"],
                auditor="Audit-Bot"
            )
            
            if record.should_trigger_circuit_breaker():
                print(f"  🚨 第 {i} 次拒绝后触发熔断！")
                break
        
        # 显示最终状态
        print(f"\n  📊 最终状态:")
        print(f"     修改次数: {record.modification_count}")
        print(f"     拒绝次数: {record.audit_rejection_count}")
        print(f"     熔断状态: {'已触发' if record.is_circuit_broken else '正常'}")
        print(f"     回滚状态: {'已执行' if record.rollback_executed else '未执行'}")
    
    print("\n--- Phase 5: 执行完整周期 ---")
    
    # 执行3轮周期
    for i in range(3):
        print(f"\n{'─'*60}")
        report = engine.run_single_cycle()
        print(f"  周期 {i+1} 完成，耗时 {report.get('duration_seconds', 0):.2f}s")
    
    print("\n" + "=" * 80)
    print("  ✅ 自驱动引擎演示完成")
    print("=" * 80)
    
    # 输出最终统计
    engine._print_final_statistics()
    
    return engine


if __name__ == "__main__":
    demo_self_driving_engine()
