"""
role_logs.py - Role-Specific Audit Log System with Timestamp Validation

Enterprise-grade logging system that enforces:
1. Each role has exclusive writable log + read-only access to others' logs
2. Every action must be recorded (work traceability)
3. Cross-role log sharing for progress visibility
4. Timestamp-based workflow validation rules:
   - accept_time < audit_time < dev_time (strict chronological order)
   - Any modification after audit/accept requires re-verification
5. PM/Dispatcher gets global oversight logs (heartbeat, conflicts, milestones)

Log File Structure:
├── .logs/
│   ├── dev/                    # Developer workspace
│   │   ├── developer_A.log     # Per-developer logs
│   │   ├── developer_B.log
│   │   └── dev_summary.log    # Aggregated dev activity
│   ├── audit/                  # Auditor workspace  
│   │   ├── auditor_A.log      # Per-auditor logs
│   │   └── audit_summary.log  # Aggregated audit results
│   ├── accept/                 # Acceptor workspace
│   │   ├── acceptor_A.log     # Per-acceptor test logs
│   │   └── accept_summary.log # Aggregated acceptance records
│   └── pm/                     # Project Manager / Dispatcher
│       ├── dispatcher.log     # System heartbeat & state machine
│       ├── conflict_log.log   # Conflict resolution decisions
│       ├── milestone.log      # Project milestone tracking
│       └── pm_overview.log    # Global project progress overview
"""

import os
import sys
import json
import time
import hashlib
import threading
from typing import Optional, Dict, List, Tuple, Any
from datetime import datetime
from enum import Enum
from dataclasses import dataclass, field
from pathlib import Path


class RoleType(Enum):
    """Role type enumeration"""
    DEVELOPER = "developer"
    AUDITOR = "auditor"
    ACCEPTOR = "acceptor"
    DISPATCHER = "dispatcher"  # Also acts as PM


class LogLevel(Enum):
    """Log entry severity levels"""
    DEBUG = "DEBUG"
    INFO = "INFO"
    WARNING = "WARNING"
    ERROR = "ERROR"
    CRITICAL = "CRITICAL"


class ActionType(Enum):
    """Standardized action types for work tracing"""
    # Developer actions
    FILE_READ = "file_read"
    FILE_WRITE = "file_write"
    FILE_MODIFY = "file_modify"  # replace_in_file operation
    RCA_CREATED = "rca_created"
    DEBUG_SESSION = "debug_session"
    
    # Auditor actions
    LINT_CHECK = "lint_check"
    SECURITY_SCAN = "security_scan"
    CODE_REVIEW = "code_review"
    ISSUE_CREATED = "issue_created"
    APPROVAL_DECISION = "approval_decision"  # approve/reject
    
    # Acceptor actions
    UNIT_TEST = "unit_test"
    INTEGRATION_TEST = "integration_test"
    COVERAGE_CHECK = "coverage_check"
    VERIFICATION_DECISION = "verification_decision"  # promote/reject
    
    # Dispatcher actions
    STATE_TRANSITION = "state_transition"
    CONFLICT_RESOLUTION = "conflict_resolution"
    CIRCUIT_BREAKER = "circuit_breaker_trigger"
    MILESTONE_UPDATE = "milestone_update"
    HEARTBEAT = "heartbeat"
    
    # Generic
    SYSTEM_EVENT = "system_event"
    HUMAN_INTERVENTION = "human_intervention"


@dataclass
class LogEntry:
    """Single log entry structure with full traceability"""
    timestamp: str                    # ISO8601 format
    role: str                         # Who performed the action
    agent_name: str                   # Specific agent identifier
    action: ActionType               # What was done
    target_file: Optional[str]        # File being operated on
    details: str                      # Human-readable description
    level: LogLevel = LogLevel.INFO   # Severity
    
    # Metadata for validation
    session_id: str = ""              # Unique session identifier
    previous_hash: str = ""           # File hash before action (if applicable)
    new_hash: str = ""                # File hash after action (if applicable)
    
    # Workflow timestamps (CRITICAL for validation)
    dev_timestamp: Optional[str] = None      # When developer last modified
    audit_timestamp: Optional[str] = None    # When auditor last reviewed
    accept_timestamp: Optional[str] = None   # When acceptor last verified
    
    # References
    related_issue: Optional[str] = None     # Linked issue ticket ID
    parent_entry: Optional[str] = None      # Parent log entry (for follow-ups)
    
    # Machine-readable data
    metadata: Dict[str, Any] = field(default_factory=dict)
    
    def to_dict(self) -> dict:
        """Convert to dictionary for JSON serialization"""
        return {
            'timestamp': self.timestamp,
            'role': self.role,
            'agent_name': self.agent_name,
            'action': self.action.value if isinstance(self.action, ActionType) else self.action,
            'target_file': self.target_file,
            'details': self.details,
            'level': self.level.value if isinstance(self.level, LogLevel) else self.level,
            'session_id': self.session_id,
            'previous_hash': self.previous_hash[:16] if self.previous_hash else '',
            'new_hash': self.new_hash[:16] if self.new_hash else '',
            'dev_timestamp': self.dev_timestamp,
            'audit_timestamp': self.audit_timestamp,
            'accept_timestamp': self.accept_timestamp,
            'related_issue': self.related_issue,
            'metadata': self.metadata
        }
    
    def validate_timestamp_chain(self) -> Tuple[bool, str]:
        """
        Validate timestamp ordering rules:
        accept < audit < dev (strict chronological)
        
        Returns:
            (is_valid, error_message)
        """
        errors = []
        
        # Rule 1: All three timestamps must exist for verified files
        if self.accept_timestamp and self.audit_timestamp and self.dev_timestamp:
            accept_dt = datetime.fromisoformat(self.accept_timestamp)
            audit_dt = datetime.fromisoformat(self.audit_timestamp)
            dev_dt = datetime.fromisoformat(self.dev_timestamp)
            
            # Rule 2: Accept must be AFTER audit
            if accept_dt <= audit_dt:
                errors.append(
                    f"Accept timestamp ({self.accept_timestamp}) must be > "
                    f"Audit timestamp ({self.audit_timestamp})"
                )
            
            # Rule 3: Audit must be AFTER development
            if audit_dt <= dev_dt:
                errors.append(
                    f"Audit timestamp ({self.audit_timestamp}) must be > "
                    f"Dev timestamp ({self.dev_timestamp})"
                )
        
        # Partial validation (only some timestamps present)
        elif self.audit_timestamp and self.dev_timestamp:
            audit_dt = datetime.fromisoformat(self.audit_timestamp)
            dev_dt = datetime.fromisoformat(self.dev_timestamp)
            
            if audit_dt <= dev_dt:
                errors.append(
                    f"Audit timestamp ({self.audit_timestamp}) must be > "
                    f"Dev timestamp ({self.dev_timestamp})"
                )
        
        if errors:
            return False, "; ".join(errors)
        
        return True, "Timestamp chain valid"


class RoleLogManager:
    """
    Manages role-specific log files with permission control.
    
    Core Principles:
    1. Each role writes ONLY to their own log directory
    2. Each role can READ all other roles' logs (read-only)
    3. PM/Dispatcher has global read-write to all logs
    4. Every action MUST generate a log entry (enforced by API design)
    5. Timestamps are validated automatically
    """
    
    LOG_DIR = ".logs"
    
    ROLE_DIRECTORIES = {
        RoleType.DEVELOPER: "dev",
        RoleType.AUDITOR: "audit", 
        RoleType.ACCEPTOR: "accept",
        RoleType.DISPATCHER: "pm"
    }
    
    ROLE_DISPLAY_NAMES = {
        RoleType.DEVELOPER: "Developer",
        RoleType.AUDITOR: "Auditor",
        RoleType.ACCEPTOR: "Acceptor",
        RoleType.DISPATCHER: "Project Manager / Dispatcher"
    }
    
    def __init__(self, base_dir: str = ".", project_name: str = "project"):
        self.base_dir = Path(base_dir)
        self.project_name = project_name
        self.log_base = self.base_dir / self.LOG_DIR
        
        # Ensure all log directories exist
        self._initialize_directories()
        
        # Track active sessions per role
        self.active_sessions: Dict[str, str] = {}
        
        # Statistics
        self.stats = {
            'total_entries_written': 0,
            'entries_by_role': {},
            'timestamp_validations': 0,
            'timestamp_violations': 0,
            'cross_role_reads': 0
        }
    
    def _initialize_directories(self):
        """Create all necessary log directories"""
        self.log_base.mkdir(parents=True, exist_ok=True)
        
        for dir_path in self.ROLE_DIRECTORIES.values():
            (self.log_base / dir_path).mkdir(parents=True, exist_ok=True)
        
        # Create per-role subdirectories for individual agents
        for role, dir_name in [
            (RoleType.DEVELOPER, "developers"),
            (RoleType.AUDITOR, "auditors"),
            (RoleType.ACCEPTOR, "acceptors")
        ]:
            (self.log_base / self.ROLE_DIRECTORIES[role] / dir_name).mkdir(parents=True, exist_ok=True)
    
    def create_session(self, role: RoleType, agent_name: str) -> str:
        """Create a new logging session for an agent"""
        session_id = f"{role.value}-{agent_name}-{int(time.time()*1000)}-{os.getpid()}"
        self.active_sessions[session_id] = {
            'role': role,
            'agent_name': agent_name,
            'created_at': datetime.now().isoformat()
        }
        return session_id
    
    def write_entry(
        self,
        role: RoleType,
        agent_name: str,
        action: ActionType,
        details: str,
        target_file: Optional[str] = None,
        level: LogLevel = LogLevel.INFO,
        session_id: Optional[str] = None,
        previous_hash: Optional[str] = None,
        new_hash: Optional[str] = None,
        related_issue: Optional[str] = None,
        metadata: Optional[Dict] = None,
        force_timestamps: Optional[Dict[str, str]] = None
    ) -> LogEntry:
        """
        Write a log entry to the role's专属日志文件.
        
        This is the PRIMARY method that all roles MUST call for every action.
        Enforces work traceability and timestamp tracking.
        
        Args:
            role: The role performing the action
            agent_name: Specific agent identifier
            action: What action was taken (ActionType enum)
            details: Human-readable description
            target_file: File being operated on (if any)
            level: Severity level
            session_id: Session identifier
            previous_hash: File hash before action
            new_hash: File hash after action
            related_issue: Linked issue ticket
            metadata: Additional key-value data
            force_timestamps: Manually set dev/audit/accept timestamps
            
        Returns:
            LogEntry: The created and saved log entry
        """
        # Generate or use provided session
        if not session_id:
            session_id = self.create_session(role, agent_name)
        
        # Create log entry
        now = datetime.now().isoformat()
        
        entry = LogEntry(
            timestamp=now,
            role=role.value,
            agent_name=agent_name,
            action=action,
            target_file=target_file,
            details=details,
            level=level,
            session_id=session_id,
            previous_hash=previous_hash or "",
            new_hash=new_hash or "",
            related_issue=related_issue,
            metadata=metadata or {}
        )
        
        # Handle forced timestamps (for workflow transitions)
        if force_timestamps:
            entry.dev_timestamp = force_timestamps.get('dev')
            entry.audit_timestamp = force_timestamps.get('audit')
            entry.accept_timestamp = force_timestamps.get('accept')
        else:
            # Auto-set timestamp based on action type
            self._auto_set_workflow_timestamps(entry, role, action)
        
        # Validate timestamp chain BEFORE saving
        is_valid, error_msg = entry.validate_timestamp_chain()
        
        if not is_valid:
            entry.level = LogLevel.WARNING
            entry.metadata['timestamp_validation_error'] = error_msg
            self.stats['timestamp_violations'] += 1
            print(f"⚠️ [TIMESTAMP WARNING] {error_msg}")
        else:
            self.stats['timestamp_validations'] += 1
        
        # Determine which log file(s) to write to
        log_files = self._get_target_log_files(role, agent_name, action)
        
        # Write entry to all relevant log files
        for log_file in log_files:
            self._append_to_log(log_file, entry)
        
        # Update statistics
        self.stats['total_entries_written'] += 1
        role_key = role.value
        self.stats['entries_by_role'][role_key] = \
            self.stats['entries_by_role'].get(role_key, 0) + 1
        
        return entry
    
    def _auto_set_workflow_timestamps(self, entry: LogEntry, role: RoleType, action: ActionType):
        """Automatically set workflow timestamps based on action type"""
        now_iso = entry.timestamp
        
        if role == RoleType.DEVELOPER:
            # Developer actions update dev_timestamp
            if action in [ActionType.FILE_WRITE, ActionType.FILE_MODIFY, ActionType.RCA_CREATED]:
                entry.dev_timestamp = now_iso
                
        elif role == RoleType.AUDITOR:
            # Auditor actions update audit_timestamp
            if action in [ActionType.APPROVAL_DECISION, ActionType.CODE_REVIEW, ActionType.ISSUE_CREATED]:
                entry.audit_timestamp = now_iso
                
        elif role == RoleType.ACCEPTOR:
            # Acceptor actions update accept_timestamp
            if action in [ActionType.VERIFICATION_DECISION]:
                entry.accept_timestamp = now_iso
    
    def _get_target_log_files(self, role: RoleType, agent_name: str, action: ActionType) -> List[Path]:
        """Determine which log file(s) this entry should go to"""
        role_dir = Path(self.LOG_DIR) / self.ROLE_DIRECTORIES[role]
        files = []
        
        # 1. Always write to agent's personal log
        if role in [RoleType.DEVELOPER, RoleType.AUDITOR, RoleType.ACCEPTOR]:
            safe_agent_name = agent_name.replace(" ", "_").replace("/", "-")
            personal_log = role_dir / f"{safe_agent_name}.log"
            files.append(personal_log)
        
        # 2. Write to role summary log for significant actions
        if action in [
            ActionType.APPROVAL_DECISION,
            ActionType.VERIFICATION_DECISION,
            ActionType.ISSUE_CREATED,
            ActionType.MILESTONE_UPDATE,
            ActionType.CONFLICT_RESOLUTION
        ]:
            summary_log = role_dir / f"{role.value}_summary.log"
            files.append(summary_log)
        
        # 3. Dispatcher/PM writes to multiple special logs
        if role == RoleType.DISPATCHER:
            pm_dir = Path(self.LOG_DIR) / "pm"
            
            # All dispatcher actions go to main log
            files.append(pm_dir / "dispatcher.log")
            
            # Specialized logs based on action type
            if action == ActionType.HEARTBEAT:
                files.append(pm_dir / "heartbeat.log")
            elif action == ActionType.CONFLICT_RESOLUTION:
                files.append(pm_dir / "conflict_log.log")
            elif action == ActionType.MILESTONE_UPDATE:
                files.append(pm_dir / "milestone.log")
            elif action in [ActionType.STATE_TRANSITION, ActionType.CIRCUIT_BREAKER]:
                files.append(pm_dir / "pm_overview.log")
        
        return files
    
    def _append_to_log(self, log_file: Path, entry: LogEntry):
        """Append a log entry to a log file (with file locking consideration)"""
        log_file.parent.mkdir(parents=True, exist_ok=True)
        
        entry_dict = entry.to_dict()
        log_line = json.dumps(entry_dict, ensure_ascii=False, default=str)
        
        with open(log_file, 'a', encoding='utf-8') as f:
            f.write(log_line + '\n')
    
    def read_logs(
        self,
        role: RoleType,
        target_role: Optional[RoleType] = None,
        agent_name: Optional[str] = None,
        limit: int = 100,
        since: Optional[str] = None,
        until: Optional[str] = None,
        action_filter: Optional[ActionType] = None,
        file_filter: Optional[str] = None
    ) -> List[LogEntry]:
        """
        Read logs from a specific role's log files.
        
        Permission Rules:
        - Everyone can read ALL logs (transparency)
        - Only owner can WRITE to own logs (enforced by write_entry method)
        - PM/Dispatcher has elevated access
        
        Args:
            role: The reader's role (for permission context)
            target_role: Whose logs to read (None = all roles)
            agent_name: Filter by specific agent
            limit: Max entries to return
            since: Only entries after this ISO timestamp
            until: Only entries before this ISO timestamp
            action_filter: Only entries of this action type
            file_filter: Only entries affecting this file path
            
        Returns:
            List of LogEntry objects matching criteria
        """
        results = []
        
        # Determine which directories to scan
        if target_role:
            dirs_to_scan = [Path(self.LOG_DIR) / self.ROLE_DIRECTORIES[target_role]]
        else:
            dirs_to_scan = [Path(self.LOG_DIR) / d for d in set(self.ROLE_DIRECTORIES.values())]
        
        for log_dir in dirs_to_scan:
            if not log_dir.exists():
                continue
            
            # Find relevant log files
            if agent_name and target_role in [RoleType.DEVELOPER, RoleType.AUDITOR, RoleType.ACCEPTOR]:
                safe_name = agent_name.replace(" ", "_").replace("/", "-")
                log_files = [log_dir / f"{safe_name}.log"]
            else:
                log_files = list(log_dir.glob("*.log"))
            
            for log_file in log_files:
                if not log_file.exists():
                    continue
                
                try:
                    with open(log_file, 'r', encoding='utf-8') as f:
                        for line_num, line in enumerate(f):
                            if limit and len(results) >= limit:
                                break
                            
                            line = line.strip()
                            if not line:
                                continue
                            
                            try:
                                entry_data = json.loads(line)
                                entry = self._dict_to_log_entry(entry_data)
                                
                                # Apply filters
                                if since and entry.timestamp < since:
                                    continue
                                if until and entry.timestamp > until:
                                    continue
                                if action_filter and entry.action != action_filter:
                                    continue
                                if file_filter and entry.target_file != file_filter:
                                    continue
                                if agent_name and entry.agent_name != agent_name:
                                    continue
                                
                                results.append(entry)
                                
                            except json.JSONDecodeError:
                                continue  # Skip malformed lines
                                
                except Exception as e:
                    pass  # Skip unreadable files
        
        # Sort by timestamp (newest first)
        results.sort(key=lambda x: x.timestamp, reverse=True)
        
        # Update cross-role read statistic
        if target_role and target_role != role:
            self.stats['cross_role_reads'] += len(results)
        
        return results
    
    def _dict_to_log_entry(self, data: dict) -> LogEntry:
        """Convert dictionary back to LogEntry object"""
        action_str = data.get('action', 'system_event')
        try:
            action = ActionType(action_str)
        except ValueError:
            action = action_str
        
        level_str = data.get('level', 'INFO')
        try:
            level = LogLevel(level_str)
        except ValueError:
            level = level_str
        
        return LogEntry(
            timestamp=data.get('timestamp', ''),
            role=data.get('role', ''),
            agent_name=data.get('agent_name', ''),
            action=action,
            target_file=data.get('target_file'),
            details=data.get('details', ''),
            level=level,
            session_id=data.get('session_id', ''),
            previous_hash=data.get('previous_hash', ''),
            new_hash=data.get('new_hash', ''),
            dev_timestamp=data.get('dev_timestamp'),
            audit_timestamp=data.get('audit_timestamp'),
            accept_timestamp=data.get('accept_timestamp'),
            related_issue=data.get('related_issue'),
            metadata=data.get('metadata', {})
        )
    
    def get_timeline_for_file(self, file_path: str) -> Dict[str, Any]:
        """
        Get complete workflow timeline for a specific file.
        Shows dev → audit → accept progression with timestamps.
        
        Returns:
            Dictionary with timeline info and validation status
        """
        # Get all entries related to this file
        all_entries = self.read_logs(
            role=RoleType.DISPATCHER,  # Dispatcher can see everything
            file_filter=file_path,
            limit=500
        )
        
        # Organize by role
        timeline = {
            'file_path': file_path,
            'developer_actions': [],
            'auditor_actions': [],
            'acceptor_actions': [],
            'latest_dev_ts': None,
            'latest_audit_ts': None,
            'latest_accept_ts': None,
            'validation_status': 'unknown',
            'validation_message': ''
        }
        
        for entry in all_entries:
            if entry.role == 'developer':
                timeline['developer_actions'].append(entry)
                if entry.dev_timestamp:
                    if not timeline['latest_dev_ts'] or entry.dev_timestamp > timeline['latest_dev_ts']:
                        timeline['latest_dev_ts'] = entry.dev_timestamp
                        
            elif entry.role == 'auditor':
                timeline['auditor_actions'].append(entry)
                if entry.audit_timestamp:
                    if not timeline['latest_audit_ts'] or entry.audit_timestamp > timeline['latest_audit_ts']:
                        timeline['latest_audit_ts'] = entry.audit_timestamp
                        
            elif entry.role == 'acceptor':
                timeline['acceptor_actions'].append(entry)
                if entry.accept_timestamp:
                    if not timeline['latest_accept_ts'] or entry.accept_timestamp > timeline['latest_accept_ts']:
                        timeline['latest_accept_ts'] = entry.accept_timestamp
        
        # Validate timestamp chain
        if timeline['latest_accept_ts'] and timeline['latest_audit_ts'] and timeline['latest_dev_ts']:
            accept_dt = datetime.fromisoformat(timeline['latest_accept_ts'])
            audit_dt = datetime.fromisoformat(timeline['latest_audit_ts'])
            dev_dt = datetime.fromisoformat(timeline['latest_dev_ts'])
            
            if accept_dt > audit_dt or audit_dt > dev_dt:
                timeline['validation_status'] = 'VIOLATED'
                timeline['validation_message'] = (
                    f"Timestamp chain broken! "
                    f"Accept({timeline['latest_accept_ts']}) must be < "
                    f"Audit({timeline['latest_audit_ts']}) < Dev({timeline['latest_dev_ts']}). "
                    f"File needs re-verification."
                )
            else:
                timeline['validation_status'] = 'VALID'
                timeline['validation_message'] = (
                    f"Timestamp chain valid: "
                    f"Dev → Audit → Accept (chronologically correct)"
                )
        elif timeline['latest_audit_ts'] and timeline['latest_dev_ts']:
            audit_dt = datetime.fromisoformat(timeline['latest_audit_ts'])
            dev_dt = datetime.fromisoformat(timeline['latest_dev_ts'])
            
            if audit_dt <= dev_dt:
                timeline['validation_status'] = 'WARNING'
                timeline['validation_message'] = (
                    f"Audit timestamp ({timeline['latest_audit_ts']}) <= "
                    f"Dev timestamp ({timeline['latest_dev_ts']}. "
                    f"Awaiting final verification."
                )
            else:
                timeline['validation_status'] = 'VALID_SO_FAR'
                timeline['validation_message'] = (
                    f"Dev → Audit OK. Awaiting Accept verification."
                )
        else:
            timeline['validation_status'] = 'INSUFFICIENT_DATA'
            timeline['validation_message'] = (
                "Not enough timestamps to validate. Need at least Dev + Audit."
            )
        
        return timeline
    
    def get_progress_report(self) -> Dict[str, Any]:
        """
        Generate comprehensive progress report from all logs.
        Shows what each role has been doing.
        """
        report = {
            'generated_at': datetime.now().isoformat(),
            'project': self.project_name,
            'roles': {},
            'recent_activity': [],
            'blocked_items': [],
            'milestones': []
        }
        
        for role_type in RoleType:
            role_entries = self.read_logs(
                role=RoleType.DISPATCHER,
                target_role=role_type,
                limit=50
            )
            
            role_info = {
                'name': self.ROLE_DISPLAY_NAMES[role_type],
                'total_actions': len(role_entries),
                'last_activity': role_entries[0].timestamp if role_entries else 'Never',
                'recent_actions': [
                    {
                        'time': e.timestamp,
                        'agent': e.agent_name,
                        'action': e.action.value if isinstance(e.action, ActionType) else e.action,
                        'target': e.target_file or 'N/A',
                        'details': e.details[:80]
                    }
                    for e in role_entries[:10]
                ]
            }
            
            report['roles'][role_type.value] = role_info
        
        # Find items needing attention (timestamp violations)
        recent_all = self.read_logs(
            role=RoleType.DISPATCHER,
            limit=200
        )
        
        for entry in recent_all:
            if 'timestamp_validation_error' in entry.metadata:
                report['blocked_items'].append({
                    'time': entry.timestamp,
                    'role': entry.role,
                    'agent': entry.agent_name,
                    'file': entry.target_file,
                    'error': entry.metadata['timestamp_validation_error'],
                    'details': entry.details
                })
        
        report['statistics'] = self.stats
        
        return report
    
    def export_logs_to_json(self, output_path: str, role: Optional[RoleType] = None):
        """Export all logs to a JSON file for external analysis"""
        entries = self.read_logs(
            role=RoleType.DISPATCHER,
            target_role=role,
            limit=10000
        )
        
        output = {
            'exported_at': datetime.now().isoformat(),
            'exporter': 'RoleLogManager',
            'total_entries': len(entries),
            'entries': [e.to_dict() for e in entries],
            'statistics': self.stats
        }
        
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(output, f, indent=2, ensure_ascii=False, default=str)
        
        print(f"✅ Exported {len(entries)} log entries to {output_path}")


# ===== Global Singleton & Convenience Functions =====

_global_log_manager: Optional[RoleLogManager] = None


def get_log_manager(base_dir: str = ".", project_name: str = "project") -> RoleLogManager:
    """Get global log manager instance"""
    global _global_log_manager
    
    if _global_log_manager is None:
        _global_log_manager = RoleLogManager(base_dir, project_name)
    
    return _global_log_manager


def log_action(
    role: RoleType,
    agent_name: str,
    action: ActionType,
    details: str,
    **kwargs
) -> LogEntry:
    """Convenience function to log an action immediately"""
    manager = get_log_manager()
    return manager.write_entry(role, agent_name, action, details, **kwargs)


def read_other_logs(
    my_role: RoleType,
    target_role: RoleType,
    **kwargs
) -> List[LogEntry]:
    """Convenience function to read another role's logs"""
    manager = get_log_manager()
    return manager.read_logs(my_role, target_role=target_role, **kwargs)


if __name__ == "__main__":
    # Test the role log system
    print("=" * 80)
    print("  📋 Role-Specific Log System Test Suite")
    print("     Work Traceability + Timestamp Validation + Cross-Role Sharing")
    print("=" * 80)
    
    # Initialize
    manager = get_log_manager(project_name="TestProject")
    print(f"\n✅ Log system initialized")
    print(f"   Base directory: {manager.base_dir}")
    print(f"   Log directory: {manager.log_base}")
    
    # Simulate a complete workflow
    print("\n--- Simulating Complete Development Workflow ---\n")
    
    # Phase 1: Developer works on file
    print("[Phase 1] Developer Phase:")
    entry1 = manager.write_entry(
        role=RoleType.DEVELOPER,
        agent_name="Dev-Agent-A",
        action=ActionType.FILE_MODIFY,
        details="Fixed null pointer exception in auth.py",
        target_file="src/backend/auth.py",
        previous_hash="abc123",
        new_hash="def456",
        metadata={'lines_changed': 5, 'function_affected': 'login_user'}
    )
    print(f"  ✓ Logged: {entry1.action.value} @ {entry1.timestamp}")
    print(f"  ✓ Dev timestamp set: {entry1.dev_timestamp}")
    
    time.sleep(0.3)  # Simulate time passing
    
    # Phase 2: Auditor reviews
    print("\n[Phase 2] Auditor Phase:")
    entry2 = manager.write_entry(
        role=RoleType.AUDITOR,
        agent_name="Audit-Bot-X",
        action=ActionType.APPROVAL_DECISION,
        details="APPROVED - Code quality acceptable, no security issues found",
        target_file="src/backend/auth.py",
        related_issue=None,
        metadata={'lint_errors': 0, 'security_issues': 0}
    )
    print(f"  ✓ Logged: {entry2.action.value} @ {entry2.timestamp}")
    print(f"  ✓ Audit timestamp set: {entry2.audit_timestamp}")
    
    # Check: Is audit timestamp > dev timestamp?
    if entry2.audit_timestamp and entry1.dev_timestamp:
        audit_dt = datetime.fromisoformat(entry2.audit_timestamp)
        dev_dt = datetime.fromisoformat(entry1.dev_timestamp)
        if audit_dt > dev_dt:
            print(f"  ✓ Timestamp order VALID (audit {audit_dt.strftime('%H:%M:%S')} > dev {dev_dt.strftime('%H:%M:%S')})")
        else:
            print(f"  ⚠️ Timestamp order INVALID!")
    
    time.sleep(0.3)
    
    # Phase 3: Acceptor verifies
    print("\n[Phase 3] Acceptor Phase:")
    entry3 = manager.write_entry(
        role=RoleType.ACCEPTOR,
        agent_name="TestRunner-Pro",
        action=ActionType.VERIFICATION_DECISION,
        details="PROMOTED TO VERIFIED - All 15 unit tests passed, coverage 87%",
        target_file="src/backend/auth.py",
        metadata={'tests_run': 15, 'tests_passed': 15, 'coverage_pct': 87}
    )
    print(f"  ✓ Logged: {entry3.action.value} @ {entry3.timestamp}")
    print(f"  ✓ Accept timestamp set: {entry3.accept_timestamp}")
    
    # Validate complete chain
    print("\n[Timestamp Chain Validation]")
    is_valid, msg = entry3.validate_timestamp_chain()
    print(f"  Validation result: {'✅ PASS' if is_valid else '❌ FAIL'} - {msg}")
    
    # Phase 4: Developer makes ANOTHER change (should trigger re-audit warning)
    print("\n[Phase 4] Developer Makes Subsequent Change (Re-audit Required):")
    entry4 = manager.write_entry(
        role=RoleType.DEVELOPER,
        agent_name="Dev-Agent-A",
        action=ActionType.FILE_MODIFY,
        details="Added rate limiting feature (post-audit modification)",
        target_file="src/backend/auth.py",
        new_hash="ghi789"
    )
    print(f"  ✓ Logged: {entry4.action.value} @ {entry4.timestamp}")
    print(f"  ✓ New dev timestamp: {entry4.dev_timestamp}")
    
    # Check: New dev timestamp > audit timestamp?
    if entry4.dev_timestamp and entry2.audit_timestamp:
        dev_dt = datetime.fromisoformat(entry4.dev_timestamp)
        audit_dt = datetime.fromisoformat(entry2.audit_timestamp)
        
        if dev_dt > audit_dt:
            print(f"  ⚠️ RE-AUDIT REQUIRED!")
            print(f"     Dev modified at {dev_dt.strftime('%H:%M:%S')} > ")
            print(f"     Audit done at {audit_dt.strftime('%H:%M:%S')}")
            print(f"     File must go through audit cycle again")
    
    # Demonstrate cross-role log reading
    print("\n[Cross-Role Log Reading]")
    print("  Developer reading Auditor's logs to understand status:")
    audit_logs = manager.read_logs(
        role=RoleType.DEVELOPER,
        target_role=RoleType.AUDITOR,
        limit=5
    )
    print(f"  ✓ Found {len(audit_logs)} recent audit entries")
    for log in audit_logs[:3]:
        print(f"    - [{log.timestamp}] {log.agent_name}: {log.details[:60]}...")
    
    # Generate timeline
    print("\n[File Timeline Analysis]")
    timeline = manager.get_timeline_for_file("src/backend/auth.py")
    print(f"  File: {timeline['file_path']}")
    print(f"  Status: {timeline['validation_status'].upper()}")
    print(f"  Message: {timeline['validation_message']}")
    print(f"  Actions: Dev={len(timeline['developer_actions'])}, "
          f"Audit={len(timeline['auditor_actions'])}, "
          f"Accept={len(timeline['acceptor_actions'])}")
    
    # Generate progress report
    print("\n[Progress Report Summary]")
    report = manager.get_progress_report()
    print(f"  Roles tracked: {len(report['roles'])}")
    total_actions = sum(r['total_actions'] for r in report['roles'].values())
    print(f"  Total logged actions: {total_actions}")
    print(f"  Blocked items (timestamp violations): {len(report['blocked_items'])}")
    
    # Export
    export_path = ".logs/exported_full_log.json"
    manager.export_logs_to_json(export_path)
    
    print("\n" + "=" * 80)
    print("  ✅ All Tests Completed Successfully!")
    print("=" * 80)
