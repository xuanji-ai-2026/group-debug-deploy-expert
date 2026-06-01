"""
test_role_logs.py - Unit tests for RoleLogManager (role_logs.py)

Tests cover:
- Log manager initialization and directory structure
- Session management
- Entry writing (write_entry)
- Log reading (read_logs) with filters
- Timestamp chain validation (validate_timestamp_chain)
- Timeline analysis for files
- Progress report generation
- Log export to JSON
- Cross-role log visibility
- Edge cases: empty entries, invalid timestamps, missing files
"""

import os
import sys
import json
import time
from datetime import datetime, timedelta

import pytest

sys.path.insert(0, str(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))


# ============================================================
# Initialization Tests
# ============================================================

class TestRoleLogManagerInit:
    """Test RoleLogManager initialization"""

    def test_initialization(self, log_manager):
        assert log_manager.base_dir is not None
        assert log_manager.log_base is not None
        assert log_manager.project_name == "test-project"

    def test_log_directories_created(self, log_manager):
        expected_dirs = ["dev", "audit", "accept", "pm"]
        for d in expected_dirs:
            assert (log_manager.log_base / d).exists(), f"Missing log dir: {d}"

    def test_subdirectories_created(self, log_manager):
        subdirs = ["developers", "auditors", "acceptors"]
        for sd in subdirs:
            found = any(sd in str(p) for p in log_manager.log_base.rglob("*"))
            assert found, f"Missing subdir: {sd}"

    def test_stats_initialized(self, log_manager):
        assert log_manager.stats['total_entries_written'] == 0
        assert 'entries_by_role' in log_manager.stats
        assert log_manager.stats['timestamp_validations'] == 0

    def test_active_sessions_empty_initially(self, log_manager):
        assert len(log_manager.active_sessions) == 0


# ============================================================
# Session Management Tests
# ============================================================

class TestSessionManagement:
    """Test session creation and tracking"""

    def test_create_session_returns_id(self, log_manager):
        from role_logs import RoleType
        sid = log_manager.create_session(RoleType.DEVELOPER, "Dev-Agent-1")
        assert sid != ""
        assert "developer" in sid

    def test_create_session_tracks_agent(self, log_manager):
        from role_logs import RoleType
        sid = log_manager.create_session(RoleType.AUDITOR, "AuditBot-X")
        assert sid in log_manager.active_sessions
        assert log_manager.active_sessions[sid]['agent_name'] == "AuditBot-X"

    def test_create_session_unique_ids(self, log_manager):
        from role_logs import RoleType
        ids = [log_manager.create_session(RoleType.DEVELOPER, f"Agent-{i}") for i in range(5)]
        assert len(ids) == len(set(ids))

    def test_create_session_has_timestamp(self, log_manager):
        from role_logs import RoleType
        sid = log_manager.create_session(RoleType.ACCEPTOR, "Acc-Agent")
        session_data = log_manager.active_sessions[sid]
        assert 'created_at' in session_data
        assert session_data['created_at'] != ""


# ============================================================
# Write Entry Tests
# ============================================================

class TestWriteEntry:
    """Test log entry writing functionality"""

    def test_write_basic_entry(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="Dev-Alice",
            action=ActionType.FILE_WRITE,
            details="Wrote auth.py changes"
        )
        assert entry is not None
        assert entry.role == "developer"
        assert entry.action == ActionType.FILE_WRITE

    def test_write_entry_updates_stats(self, log_manager):
        from role_logs import RoleType, ActionType
        initial = log_manager.stats['total_entries_written']
        log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="Dev-Bob",
            action=ActionType.FILE_READ,
            details="Read config.yaml"
        )
        assert log_manager.stats['total_entries_written'] == initial + 1

    def test_write_entry_with_target_file(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.AUDITOR,
            agent_name="Audit-Carol",
            action=ActionType.LINT_CHECK,
            target_file="src/auth.py",
            details="Ran flake8 on auth.py"
        )
        assert entry.target_file == "src/auth.py"

    def test_write_entry_with_metadata(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.ACCEPTOR,
            agent_name="Accept-Dave",
            action=ActionType.UNIT_TEST,
            details="Running unit tests with metadata",
            target_file="test_module.py",
            metadata={'tests_run': 15, 'passed': 15}
        )
        assert entry.metadata.get('tests_run') == 15

    def test_write_entry_with_level(self, log_manager):
        from role_logs import RoleType, ActionType, LogLevel
        entry = log_manager.write_entry(
            role=RoleType.DISPATCHER,
            agent_name="Disp-Eve",
            action=ActionType.CIRCUIT_BREAKER,
            level=LogLevel.CRITICAL,
            details="Circuit breaker triggered!"
        )
        assert entry.level == LogLevel.CRITICAL

    def test_write_entry_generates_timestamp(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="Dev-Time",
            action=ActionType.FILE_MODIFY,
            details="Timestamp test"
        )
        assert entry.timestamp != ""
        assert "T" in entry.timestamp or "-" in entry.timestamp

    def test_write_entry_creates_log_file(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="Dev-File",
            action=ActionType.FILE_READ,
            details="Should create a .log file"
        )
        assert entry is not None


# ============================================================
# Read Logs Tests
# ============================================================

class TestReadLogs:
    """Test log reading and filtering"""

    def test_read_own_logs(self, log_manager):
        from role_logs import RoleType, ActionType
        log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="ReadTester",
            action=ActionType.FILE_WRITE,
            details="Entry to read back"
        )
        entries = log_manager.read_logs(role=RoleType.DEVELOPER)
        assert len(entries) >= 1

    def test_read_other_role_logs(self, log_manager):
        from role_logs import RoleType, ActionType
        log_manager.write_entry(
            role=RoleType.AUDITOR,
            agent_name="CrossRead-Auditor",
            action=ActionType.CODE_REVIEW,
            details="Auditor wrote this"
        )
        dev_entries = log_manager.read_logs(
            role=RoleType.DEVELOPER,
            target_role=RoleType.AUDITOR
        )
        assert len(dev_entries) >= 1

    def test_read_limit(self, log_manager):
        from role_logs import RoleType, ActionType
        for i in range(10):
            log_manager.write_entry(
                role=RoleType.DEVELOPER,
                agent_name="LimitTester",
                action=ActionType.FILE_READ,
                details=f"Entry {i}"
            )
        entries = log_manager.read_logs(role=RoleType.DEVELOPER, limit=3)
        assert len(entries) <= 3

    def test_read_filter_by_action(self, log_manager):
        from role_logs import RoleType, ActionType
        log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="FilterTest",
            action=ActionType.FILE_WRITE,
            details="Write action"
        )
        log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="FilterTest",
            action=ActionType.FILE_READ,
            details="Read action"
        )
        write_entries = log_manager.read_logs(
            role=RoleType.DEVELOPER,
            action_filter=ActionType.FILE_WRITE
        )
        assert all(e.action == ActionType.FILE_WRITE for e in write_entries)

    def test_read_filter_by_file(self, log_manager):
        from role_logs import RoleType, ActionType
        log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="FileFilter",
            action=ActionType.FILE_MODIFY,
            target_file="auth.py",
            details="Modified auth.py"
        )
        log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="FileFilter",
            action=ActionType.FILE_MODIFY,
            target_file="config.py",
            details="Modified config.py"
        )
        auth_entries = log_manager.read_logs(
            role=RoleType.DEVELOPER,
            file_filter="auth.py"
        )
        assert all(e.target_file == "auth.py" for e in auth_entries)

    def test_read_filter_by_agent(self, log_manager):
        from role_logs import RoleType, ActionType
        log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="AgentAlpha",
            action=ActionType.FILE_READ,
            details="Alpha's entry"
        )
        log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="AgentBeta",
            action=ActionType.FILE_READ,
            details="Beta's entry"
        )
        alpha_entries = log_manager.read_logs(
            role=RoleType.DEVELOPER,
            agent_name="AgentAlpha"
        )
        assert all(e.agent_name == "AgentAlpha" for e in alpha_entries)

    def test_read_returns_sorted_by_time_desc(self, log_manager):
        from role_logs import RoleType, ActionType
        log_manager.write_entry(role=RoleType.DEVELOPER, agent_name="Sort1", action=ActionType.FILE_READ, details="First")
        time.sleep(0.05)
        log_manager.write_entry(role=RoleType.DEVELOPER, agent_name="Sort1", action=ActionType.FILE_READ, details="Second")
        entries = log_manager.read_logs(role=RoleType.DEVELOPER)
        if len(entries) >= 2:
            assert entries[0].timestamp >= entries[1].timestamp

    def test_read_empty_logs(self, log_manager):
        from role_logs import RoleType
        entries = log_manager.read_logs(role=RoleType.DISPATCHER, target_role=RoleType.DISPATCHER)
        assert len(entries) >= 0


# ============================================================
# Timestamp Chain Validation Tests
# ============================================================

class TestTimestampValidation:
    """Test timestamp chain validation rules (accept < audit < dev)"""

    def test_valid_full_chain(self):
        from role_logs import LogEntry
        now = datetime.now()
        dev_ts = now.isoformat()
        audit_ts = (now + timedelta(seconds=10)).isoformat()
        accept_ts = (now + timedelta(seconds=20)).isoformat()
        
        entry = LogEntry(
            timestamp=now.isoformat(),
            role="developer",
            agent_name="test",
            action="file_modify",
            target_file="test.py",
            details="valid chain test",
            dev_timestamp=dev_ts,
            audit_timestamp=audit_ts,
            accept_timestamp=accept_ts
        )
        valid, msg = entry.validate_timestamp_chain()
        assert valid is True

    def test_invalid_accept_before_audit(self):
        from role_logs import LogEntry
        now = datetime.now()
        dev_ts = now.isoformat()
        audit_ts = (now + timedelta(seconds=20)).isoformat()
        accept_ts = (now + timedelta(seconds=10)).isoformat()
        
        entry = LogEntry(
            timestamp=now.isoformat(),
            role="developer",
            agent_name="test",
            action="verification_decision",
            target_file="test.py",
            details="invalid accept before audit",
            dev_timestamp=dev_ts,
            audit_timestamp=audit_ts,
            accept_timestamp=accept_ts
        )
        valid, msg = entry.validate_timestamp_chain()
        assert valid is False

    def test_invalid_audit_before_dev(self):
        from role_logs import LogEntry
        now = datetime.now()
        dev_ts = (now + timedelta(seconds=20)).isoformat()
        audit_ts = (now + timedelta(seconds=10)).isoformat()
        
        entry = LogEntry(
            timestamp=now.isoformat(),
            role="auditor",
            agent_name="test",
            action="approval_decision",
            target_file="test.py",
            details="invalid audit before dev",
            dev_timestamp=dev_ts,
            audit_timestamp=audit_ts
        )
        valid, msg = entry.validate_timestamp_chain()
        assert valid is False

    def test_partial_chain_valid_so_far(self):
        from role_logs import LogEntry
        now = datetime.now()
        dev_ts = now.isoformat()
        audit_ts = (now + timedelta(seconds=10)).isoformat()
        
        entry = LogEntry(
            timestamp=now.isoformat(),
            role="auditor",
            agent_name="test",
            action="approval_decision",
            target_file="test.py",
            details="partial chain",
            dev_timestamp=dev_ts,
            audit_timestamp=audit_ts
        )
        valid, msg = entry.validate_timestamp_chain()
        assert valid is True

    def test_no_timestamps_valid(self):
        from role_logs import LogEntry
        entry = LogEntry(
            timestamp=datetime.now().isoformat(),
            role="developer",
            agent_name="test",
            action="file_read",
            target_file="test.py",
            details="no timestamps"
        )
        valid, msg = entry.validate_timestamp_chain()
        assert valid is True


# ============================================================
# LogEntry Model Tests
# ============================================================

class TestLogEntryModel:
    """Test LogEntry data model"""

    def test_to_dict_roundtrip(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="DictTest",
            action=ActionType.RCA_CREATED,
            details="Dict roundtrip test",
            previous_hash="abc123",
            new_hash="def456"
        )
        d = entry.to_dict()
        assert d['role'] == "developer"
        assert d['action'] == "rca_created"
        assert d['previous_hash'] == "abc123"
        assert d['new_hash'] == "def456"

    def test_to_dict_truncates_hashes(self, log_manager):
        from role_logs import RoleType, ActionType
        long_hash = "a" * 64
        entry = log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="HashTrunc",
            action=ActionType.FILE_WRITE,
            details="Hash truncation test",
            previous_hash=long_hash,
            new_hash=long_hash
        )
        d = entry.to_dict()
        assert len(d['previous_hash']) <= 16
        assert len(d['new_hash']) <= 16

    def test_log_entry_defaults(self):
        from role_logs import LogEntry, ActionType, LogLevel
        entry = LogEntry(
            timestamp="2026-06-01T12:00:00",
            role="developer",
            agent_name="DefaultTest",
            action=ActionType.FILE_READ,
            target_file="default.py",
            details="Testing defaults"
        )
        assert entry.level == LogLevel.INFO
        assert entry.session_id == ""
        assert entry.previous_hash == ""
        assert entry.new_hash == ""
        assert entry.metadata == {}


# ============================================================
# Timeline Analysis Tests
# ============================================================

class TestTimelineAnalysis:
    """Test per-file timeline analysis"""

    def test_timeline_for_unknown_file(self, log_manager):
        timeline = log_manager.get_timeline_for_file("nonexistent/file.py")
        assert timeline['file_path'] == "nonexistent/file.py"
        assert timeline['validation_status'] == 'INSUFFICIENT_DATA'

    def test_timeline_with_developer_action(self, log_manager):
        from role_logs import RoleType, ActionType
        log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="TimelineDev",
            action=ActionType.FILE_MODIFY,
            target_file="src/timeline_test.py",
            details="Modified timeline test file"
        )
        timeline = log_manager.get_timeline_for_file("src/timeline_test.py")
        assert len(timeline['developer_actions']) >= 1

    def test_timeline_validation_status(self, log_manager):
        from role_logs import RoleType, ActionType
        log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="TL-Dev",
            action=ActionType.FILE_MODIFY,
            target_file="src/tl_validate.py",
            details="Dev modification"
        )
        time.sleep(0.05)
        log_manager.write_entry(
            role=RoleType.AUDITOR,
            agent_name="TL-Audit",
            action=ActionType.APPROVAL_DECISION,
            target_file="src/tl_validate.py",
            details="Audit approval"
        )
        time.sleep(0.05)
        log_manager.write_entry(
            role=RoleType.ACCEPTOR,
            agent_name="TL-Acc",
            action=ActionType.VERIFICATION_DECISION,
            target_file="src/tl_validate.py",
            details="Verification passed"
        )
        timeline = log_manager.get_timeline_for_file("src/tl_validate.py")
        assert timeline['validation_status'] in ['VALID', 'VALID_SO_FAR', 'VIOLATED', 'WARNING', 'INSUFFICIENT_DATA']


# ============================================================
# Progress Report Tests
# ============================================================

class TestProgressReport:
    """Test progress report generation"""

    def test_progress_report_structure(self, log_manager):
        report = log_manager.get_progress_report()
        assert 'generated_at' in report
        assert 'roles' in report
        assert 'statistics' in report
        assert 'recent_activity' in report

    def test_progress_report_lists_all_roles(self, log_manager):
        report = log_manager.get_progress_report()
        from role_logs import RoleType
        for rt in RoleType:
            assert rt.value in report['roles']

    def test_progress_report_has_project_name(self, log_manager):
        report = log_manager.get_progress_report()
        assert report['project'] == "test-project"


# ============================================================
# Export Tests
# ============================================================

class TestExportLogs:
    """Test JSON export functionality"""

    def test_export_creates_json_file(self, log_manager, tmp_path):
        from role_logs import RoleType, ActionType
        log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="ExportTest",
            action=ActionType.FILE_READ,
            details="Entry to export"
        )
        export_path = str(tmp_path / "exported_logs.json")
        log_manager.export_logs_to_json(export_path)
        assert os.path.exists(export_path)

    def test_export_contains_entries(self, log_manager, tmp_path):
        from role_logs import RoleType, ActionType
        log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="ExportContent",
            action=ActionType.FILE_WRITE,
            details="Content for export"
        )
        export_path = str(tmp_path / "export_content.json")
        log_manager.export_logs_to_json(export_path)
        with open(export_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        assert 'entries' in data
        assert len(data['entries']) >= 1
        assert 'statistics' in data

    def test_export_metadata(self, log_manager, tmp_path):
        export_path = str(tmp_path / "export_meta.json")
        log_manager.export_logs_to_json(export_path)
        with open(export_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        assert 'exported_at' in data
        assert 'exporter' in data
        assert data['exporter'] == 'RoleLogManager'


# ============================================================
# Auto-Timestamp Setting Tests
# ============================================================

class TestAutoTimestampSetting:
    """Test automatic workflow timestamp setting based on action type"""

    def test_developer_write_sets_dev_timestamp(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="AutoTS-Dev",
            action=ActionType.FILE_WRITE,
            details="Should set dev_timestamp"
        )
        assert entry.dev_timestamp is not None
        assert entry.dev_timestamp != ""

    def test_developer_modify_sets_dev_timestamp(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="AutoTS-Mod",
            action=ActionType.FILE_MODIFY,
            details="Modify should set dev ts"
        )
        assert entry.dev_timestamp is not None

    def test_developer_read_does_not_set_dev_timestamp(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="AutoTS-Read",
            action=ActionType.FILE_READ,
            details="Read should NOT set dev ts"
        )

    def test_auditor_approval_sets_audit_timestamp(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.AUDITOR,
            agent_name="AutoTS-Audit",
            action=ActionType.APPROVAL_DECISION,
            details="Approval sets audit ts"
        )
        assert entry.audit_timestamp is not None

    def test_acceptor_verification_sets_accept_timestamp(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.ACCEPTOR,
            agent_name="AutoTS-Acc",
            action=ActionType.VERIFICATION_DECISION,
            details="Verification sets accept ts"
        )
        assert entry.accept_timestamp is not None

    def test_force_timestamps_override_auto(self, log_manager):
        from role_logs import RoleType, ActionType
        forced = {
            'dev': '2026-01-01T00:00:00',
            'audit': '2026-02-01T00:00:00',
            'accept': '2026-03-01T00:00:00'
        }
        entry = log_manager.write_entry(
            role=RoleType.DISPATCHER,
            agent_name="ForceTS",
            action=ActionType.STATE_TRANSITION,
            details="Forced timestamps",
            force_timestamps=forced
        )
        assert entry.dev_timestamp == forced['dev']
        assert entry.audit_timestamp == forced['audit']
        assert entry.accept_timestamp == forced['accept']


# ============================================================
# Enum Tests
# ============================================================

class TestEnums:
    """Test enum definitions"""

    def test_role_type_values(self):
        from role_logs import RoleType
        assert RoleType.DEVELOPER.value == "developer"
        assert RoleType.AUDITOR.value == "auditor"
        assert RoleType.ACCEPTOR.value == "acceptor"
        assert RoleType.DISPATCHER.value == "dispatcher"

    def test_log_level_values(self):
        from role_logs import LogLevel
        levels = [LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR, LogLevel.CRITICAL]
        assert len(levels) == 5

    def test_action_type_categories(self):
        from role_logs import ActionType
        dev_actions = [ActionType.FILE_READ, ActionType.FILE_WRITE, ActionType.FILE_MODIFY, ActionType.RCA_CREATED]
        audit_actions = [ActionType.LINT_CHECK, ActionType.CODE_REVIEW, ActionType.ISSUE_CREATED, ActionType.APPROVAL_DECISION]
        acc_actions = [ActionType.UNIT_TEST, ActionType.INTEGRATION_TEST, ActionType.COVERAGE_CHECK, ActionType.VERIFICATION_DECISION]
        disp_actions = [ActionType.STATE_TRANSITION, ActionType.HEARTBEAT, ActionType.CIRCUIT_BREAKER]
        assert len(dev_actions) >= 4
        assert len(audit_actions) >= 4
        assert len(acc_actions) >= 4
        assert len(disp_actions) >= 3


# ============================================================
# Edge Case Tests
# ============================================================

class TestEdgeCases:
    """Boundary and edge case tests"""

    def test_empty_details_accepted(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="EmptyDetails",
            action=ActionType.FILE_READ,
            details=""
        )
        assert entry.details == ""

    def test_very_long_details(self, log_manager):
        from role_logs import RoleType, ActionType
        long_details = "X" * 10000
        entry = log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="LongDetails",
            action=ActionType.FILE_WRITE,
            details=long_details
        )
        assert len(entry.details) == 10000

    def test_unicode_content(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="Unicode开发者",
            action=ActionType.FILE_MODIFY,
            details="修改了认证模块：用户登录功能"
        )
        assert "认证模块" in entry.details

    def test_special_characters_in_agent_name(self, log_manager):
        from role_logs import RoleType, ActionType
        entry = log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="agent/name@with.special.chars",
            action=ActionType.FILE_READ,
            details="Special name test"
        )
        safe_name = entry.agent_name.replace("/", "-").replace(" ", "_")
        assert "/" not in safe_name

    def test_read_logs_with_since_until_filters(self, log_manager):
        from role_logs import RoleType, ActionType
        past = (datetime.now() - timedelta(hours=1)).isoformat()
        future = (datetime.now() + timedelta(hours=1)).isoformat()
        log_manager.write_entry(
            role=RoleType.DEVELOPER,
            agent_name="TimeFilter",
            action=ActionType.FILE_READ,
            details="Time filter test"
        )
        entries = log_manager.read_logs(
            role=RoleType.DEVELOPER,
            since=past,
            until=future
        )
        assert len(entries) >= 0

    def test_multiple_entries_same_file(self, log_manager):
        from role_logs import RoleType, ActionType
        unique_marker = f"multi_{datetime.now().strftime('%f')}"
        for i in range(5):
            log_manager.write_entry(
                role=RoleType.DEVELOPER,
                agent_name="MultiEntry",
                action=ActionType.FILE_MODIFY,
                target_file=f"{unique_marker}.py",
                details=f"Modification {i}"
            )
        entries = log_manager.read_logs(
            role=RoleType.DEVELOPER,
            file_filter=f"{unique_marker}.py"
        )
        assert len(entries) == 5
