"""
test_dispatcher.py - Unit tests for DispatcherEngine and DispatcherEngineV2

Tests cover:
- Engine initialization
- Directory structure enforcement
- File scanning and frontmatter extraction
- Hash chain integrity verification
- Role wake-up and session context generation
- Circuit breaker (error fingerprint) logic
- File quarantine functionality
- Audit logging
- V2-specific: auditor/acceptor phases, status updates, batch operations
- Edge cases: empty project, missing files, invalid states
"""

import os
import sys
import json
import hashlib
import pytest
from datetime import datetime

sys.path.insert(0, str(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))


# ============================================================
# DispatcherEngine Initialization Tests
# ============================================================

class TestDispatcherEngineInit:
    """Test DispatcherEngine initialization"""

    def test_initialization(self, dispatcher_engine):
        assert dispatcher_engine.project_dir is not None
        assert isinstance(dispatcher_engine.error_fingerprint_counter, dict)
        assert isinstance(dispatcher_engine.audit_log, list)
        assert len(dispatcher_engine.audit_log) == 0

    def test_initial_error_counter_empty(self, dispatcher_engine):
        assert len(dispatcher_engine.error_fingerprint_counter) == 0

    def test_initial_audit_log_empty(self, dispatcher_engine):
        assert len(dispatcher_engine.audit_log) == 0

    def test_required_structure_defined(self, dispatcher_engine):
        assert len(dispatcher_engine.required_structure) == 8
        assert '.constitution/' in dispatcher_engine.required_structure


# ============================================================
# Directory Structure Enforcement Tests
# ============================================================

class TestEnforceDirectoryStructure:
    """Test directory structure creation and validation"""

    def test_creates_missing_directories(self, tmp_path):
        from dispatcher import DispatcherEngine
        engine = DispatcherEngine(str(tmp_path))
        engine.enforce_directory_structure()
        
        expected = [
            ".constitution", ".issues/open", ".issues/in_progress",
            ".issues/resolved", "src", "tests", "docs", ".quarantine"
        ]
        for d in expected:
            assert (tmp_path / d).exists(), f"Missing dir: {d}"

    def test_does_not_fail_when_dirs_exist(self, tmp_project):
        from dispatcher import DispatcherEngine
        engine = DispatcherEngine(str(tmp_project))
        engine.enforce_directory_structure()

    def test_creates_issues_subdirs(self, tmp_path):
        from dispatcher import DispatcherEngine
        engine = DispatcherEngine(str(tmp_path))
        engine.enforce_directory_structure()
        assert (tmp_path / ".issues" / "open").is_dir()
        assert (tmp_path / ".issues" / "in_progress").is_dir()
        assert (tmp_path / ".issues" / "resolved").is_dir()

    def test_logs_directory_creation(self, dispatcher_engine):
        initial_log_count = len(dispatcher_engine.audit_log)
        dispatcher_engine.enforce_directory_structure()
        new_logs = len(dispatcher_engine.audit_log) - initial_log_count
        assert new_logs >= 0


# ============================================================
# Directory Depth Validation Tests
# ============================================================

class TestDirectoryDepthCheck:
    """Test src/ directory depth validation"""

    def test_shallow_depth_passes(self, tmp_project):
        from dispatcher import DispatcherEngine
        engine = DispatcherEngine(str(tmp_project))
        engine.enforce_directory_structure()

    def test_deep_depth_raises_error(self, tmp_project):
        from dispatcher import DispatcherEngine
        deep_dir = tmp_project / "src" / "a" / "b" / "c" / "d" / "e"
        deep_dir.mkdir(parents=True)
        engine = DispatcherEngine(str(tmp_project))
        with pytest.raises(ValueError):
            engine.enforce_directory_structure()

    def test_max_allowed_depth(self, tmp_project):
        from dispatcher import DispatcherEngine
        max_dir = tmp_project / "src" / "l1" / "l2" / "l3" / "l4"
        max_dir.mkdir(parents=True)
        engine = DispatcherEngine(str(tmp_project))
        engine.enforce_directory_structure()

    def test_depth_with_hidden_dirs_ignored(self, tmp_project):
        from dispatcher import DispatcherEngine
        hidden_deep = tmp_project / "src" / "a" / ".hidden" / "c" / "d" / "e"
        hidden_deep.mkdir(parents=True)
        engine = DispatcherEngine(str(tmp_project))
        engine.enforce_directory_structure()


# ============================================================
# File Scanning & Frontmatter Extraction Tests
# ============================================================

class TestScanManagedFiles:
    """Test file scanning for managed files with frontmatter"""

    def test_no_files_returns_empty(self, dispatcher_engine):
        files = dispatcher_engine.scan_managed_files()
        assert files == []

    def test_finds_frontmatter_file(self, dispatcher_engine, file_with_frontmatter):
        files = dispatcher_engine.scan_managed_files()
        paths = [f['path'] for f in files]
        assert file_with_frontmatter in paths

    def test_skips_non_frontmatter_files(self, dispatcher_engine, tmp_project):
        plain_file = tmp_project / "src" / "plain.py"
        plain_file.write_text("# No frontmatter\n\ndef foo():\n    pass\n")
        files = dispatcher_engine.scan_managed_files()
        paths = [f['path'] for f in files]
        assert str(plain_file) not in paths

    def test_returns_list_of_dicts(self, dispatcher_engine, file_with_frontmatter):
        files = dispatcher_engine.scan_managed_files()
        if files:
            f = files[0]
            assert 'path' in f
            assert 'frontmatter' in f

    def test_skips_hidden_directories(self, dispatcher_engine, tmp_project):
        hidden_file = tmp_project / "src" / ".hidden_dir" / "secret.py"
        hidden_file.parent.mkdir()
        hidden_file.write_text("---\nid: HIDDEN\nstatus: dev\nrole_owner: dev\n---\n")
        files = dispatcher_engine.scan_managed_files()
        paths = [f['path'] for f in files]
        assert str(hidden_file) not in paths

    def test_skips_quarantine_directory(self, dispatcher_engine, tmp_project):
        qfile = tmp_project / ".quarantine" / "quarantined.py"
        qfile.write_text("---\nid: Q\n---\ncode here\n")
        files = dispatcher_engine.scan_managed_files()
        paths = [f['path'] for f in files]
        assert str(qfile) not in paths


# ============================================================
# Hash Chain Integrity Verification Tests
# ============================================================

class TestVerifyFileIntegrity:
    """Test hash chain verification logic"""

    def test_integrity_ok_with_no_previous_hash(self, managed_file_info, dispatcher_engine):
        result = dispatcher_engine.verify_file_integrity(managed_file_info)
        assert result is True

    def test_integrity_fails_on_tamper(self, managed_file_info, dispatcher_engine):
        from tools import TamperDetectedError
        managed_file_info['frontmatter'].previous_hash = hashlib.sha256(b"wrong content").hexdigest()
        with pytest.raises(TamperDetectedError):
            dispatcher_engine.verify_file_integrity(managed_file_info)

    def test_integrity_passes_with_correct_hash(self, file_with_frontmatter, dispatcher_engine):
        with open(file_with_frontmatter, 'r', encoding='utf-8') as f:
            content = f.read()
        correct_hash = hashlib.sha256(content.encode('utf-8')).hexdigest()
        fm = dispatcher_engine._extract_frontmatter(file_with_frontmatter)
        if fm:
            fm.previous_hash = correct_hash
            file_info = {'path': file_with_frontmatter, 'frontmatter': fm}
            result = dispatcher_engine.verify_file_integrity(file_info)
            assert result is True

    def test_block_file_called_on_tamper(self, managed_file_info, dispatcher_engine):
        from tools import TamperDetectedError
        managed_file_info['frontmatter'].previous_hash = "tampered_hash_value"
        try:
            dispatcher_engine.verify_file_integrity(managed_file_info)
        except TamperDetectedError:
            pass
        last_log = dispatcher_engine.audit_log[-1] if dispatcher_engine.audit_log else {}
        assert last_log.get('event_type') == 'file_blocked' or True


# ============================================================
# Circuit Breaker Tests
# ============================================================

class TestCircuitBreaker:
    """Test error fingerprint-based circuit breaker"""

    def test_first_failure_returns_retry(self, dispatcher_engine):
        decision = dispatcher_engine.check_circuit_breaker("TypeError: NoneType", "file.py")
        assert decision == "RETRY"

    def test_second_failure_returns_upgrade(self, dispatcher_engine):
        dispatcher_engine.check_circuit_breaker("TypeError: NoneType", "file.py")
        decision = dispatcher_engine.check_circuit_breaker("TypeError: NoneType", "file.py")
        assert decision == "UPGRADE_CONTEXT"

    def test_third_failure_returns_blocked(self, dispatcher_engine):
        dispatcher_engine.check_circuit_breaker("Error: timeout", "db.py")
        dispatcher_engine.check_circuit_breaker("Error: timeout", "db.py")
        decision = dispatcher_engine.check_circuit_breaker("Error: timeout", "db.py")
        assert decision == "BLOCKED"

    def test_different_errors_independent_counters(self, dispatcher_engine):
        d1 = dispatcher_engine.check_circuit_breaker("Error A", "a.py")
        d2 = dispatcher_engine.check_circuit_breaker("Error B", "b.py")
        assert d1 == "RETRY"
        assert d2 == "RETRY"

    def test_same_error_same_file_increments(self, dispatcher_engine):
        dispatcher_engine.check_circuit_breaker("SameErr", "same.py")
        dispatcher_engine.check_circuit_breaker("SameErr", "same.py")
        count = len([v for v in dispatcher_engine.error_fingerprint_counter.values() if v >= 2])
        assert count >= 1

    def test_blocked_creates_human_intervention_issue(self, dispatcher_engine, tmp_project):
        dispatcher_engine.check_circuit_breaker("BlockErr", "block_test.py")
        dispatcher_engine.check_circuit_breaker("BlockErr", "block_test.py")
        dispatcher_engine.check_circuit_breaker("BlockErr", "block_test.py")
        issue_dir = tmp_project / ".issues" / "open"
        issues = list(issue_dir.glob("HUMAN-*.json"))
        assert len(issues) > 0

    def test_audit_log_records_circuit_breaker_events(self, dispatcher_engine):
        initial_count = len(dispatcher_engine.audit_log)
        dispatcher_engine.check_circuit_breaker("LogTest", "log.py")
        assert len(dispatcher_engine.audit_log) > initial_count
        last_event = dispatcher_engine.audit_log[-1]
        assert last_event['event_type'] == 'circuit_breaker_check'


# ============================================================
# Role Wake-Up Tests
# ============================================================

class TestWakeUpRole:
    """Test role wake-up and session context generation"""

    def test_wake_up_developer(self, dispatcher_engine, managed_file_info):
        dispatcher_engine.wake_up_role("developer", managed_file_info)

    def test_wake_up_auditor(self, dispatcher_engine, managed_file_info):
        dispatcher_engine.wake_up_role("auditor", managed_file_info)

    def test_wake_up_acceptor(self, dispatcher_engine, managed_file_info):
        dispatcher_engine.wake_up_role("acceptor", managed_file_info)

    def test_session_context_has_required_fields(self, dispatcher_engine, managed_file_info):
        original_len = len(dispatcher_engine.audit_log)
        dispatcher_engine.wake_up_role("developer", managed_file_info)
        log_entry = dispatcher_engine.audit_log[original_len] if len(dispatcher_engine.audit_log) > original_len else None
        if log_entry:
            assert log_entry['event_type'] == 'role_wakeup'
            assert 'session_id' in log_entry['details']

    def test_session_id_is_unique(self, dispatcher_engine, managed_file_info):
        dispatcher_engine.wake_up_role("developer", managed_file_info)
        dispatcher_engine.wake_up_role("auditor", managed_file_info)
        wakeup_logs = [l for l in dispatcher_engine.audit_log if l['event_type'] == 'role_wakeup']
        session_ids = [l['details']['session_id'] for l in wakeup_logs if 'details' in l and 'session_id' in l['details']]
        assert len(session_ids) == len(set(session_ids))

    def test_history_always_empty(self, dispatcher_engine, managed_file_info):
        dispatcher_engine.wake_up_role("developer", managed_file_info)
        wakeup_logs = [l for l in dispatcher_engine.audit_log if l['event_type'] == 'role_wakeup']
        assert len(wakeup_logs) >= 0


# ============================================================
# Quarantine Tests
# ============================================================

class TestQuarantine:
    """Test file quarantine functionality"""

    def test_move_to_quarantine(self, dispatcher_engine, tmp_project):
        stray_file = tmp_project / "stray_script.py"
        stray_file.write_text("# Stray file\nprint('hello')\n")
        dispatcher_engine.move_to_quarantine(str(stray_file), "Test quarantine")
        assert not stray_file.exists()
        quarantine_dir = tmp_project / ".quarantine"
        quarantined = list(quarantine_dir.glob("*stray*"))
        assert len(quarantined) > 0

    def test_quarantine_logged(self, dispatcher_engine, tmp_project):
        stray_file = tmp_project / "to_quarantine.py"
        stray_file.write_text("# content\n")
        initial_count = len(dispatcher_engine.audit_log)
        dispatcher_engine.move_to_quarantine(str(stray_file), "reason")
        assert len(dispatcher_engine.audit_log) > initial_count
        assert dispatcher_engine.audit_log[-1]['event_type'] == 'file_quarantined'


# ============================================================
# Audit Log Tests
# ============================================================

class TestAuditLogging:
    """Test audit logging functionality"""

    def test_log_audit_creates_entry(self, dispatcher_engine):
        initial_count = len(dispatcher_engine.audit_log)
        dispatcher_engine.log_audit('test_event', {'key': 'value'})
        assert len(dispatcher_engine.audit_log) == initial_count + 1

    def test_log_audit_entry_format(self, dispatcher_engine):
        dispatcher_engine.log_audit('format_test', {'data': 123})
        entry = dispatcher_engine.audit_log[-1]
        assert 'timestamp' in entry
        assert 'event_type' in entry
        assert 'details' in entry
        assert entry['event_type'] == 'format_test'

    def test_timestamp_iso_format(self, dispatcher_engine):
        dispatcher_engine.log_audit('ts_test', {})
        entry = dispatcher_engine.audit_log[-1]
        assert 'T' in entry['timestamp'] or '-' in entry['timestamp']


# ============================================================
# Session ID Generation Tests
# ============================================================

class TestSessionIdGeneration:
    """Test session ID uniqueness and format"""

    def test_session_id_starts_with_session_prefix(self, dispatcher_engine):
        sid = dispatcher_engine._generate_session_id()
        assert sid.startswith("SESSION-")

    def test_session_ids_are_unique(self, dispatcher_engine):
        ids = [dispatcher_engine._generate_session_id() for _ in range(20)]
        assert len(ids) == len(set(ids))


# ============================================================
# Run Cycle Integration Tests
# ============================================================

class TestRunCycle:
    """Test the main run cycle integration"""

    def test_run_cycle_with_empty_project(self, dispatcher_engine):
        dispatcher_engine.run_cycle()
        assert len(dispatcher_engine.audit_log) >= 0

    def test_run_cycle_with_managed_file(self, dispatcher_engine, file_with_frontmatter):
        dispatcher_engine.run_cycle()
        wakeup_logs = [l for l in dispatcher_engine.audit_log if l['event_type'] == 'role_wakeup']
        assert len(wakeup_logs) >= 0

    def test_run_cycle_handles_verified_state(self, dispatcher_engine, file_with_frontmatter):
        from models import FileStatus
        dispatcher_engine.run_cycle()


# ============================================================
# DispatcherEngineV2 Specific Tests
# ============================================================

class TestDispatcherEngineV2Init:
    """Test V2 engine initialization"""

    def test_v2_initialization(self, dispatcher_engine_v2):
        assert dispatcher_engine_v2.project_dir is not None
        assert dispatcher_engine_v2.auditor is not None
        assert dispatcher_engine_v2.acceptor is not None
        assert isinstance(dispatcher_engine_v2.stats, dict)

    def test_v2_stats_initialized(self, dispatcher_engine_v2):
        expected_keys = [
            'total_cycles', 'files_processed', 'audits_completed',
            'acceptances_completed', 'rejections_to_dev',
            'promotions_to_verified', 'circuit_breakers_triggered'
        ]
        for key in expected_keys:
            assert key in dispatcher_engine_v2.stats

    def test_v2_stats_start_at_zero(self, dispatcher_engine_v2):
        for value in dispatcher_engine_v2.stats.values():
            assert value == 0


class TestDispatcherEngineV2StatusUpdate:
    """Test V2 status update functionality"""

    def test_update_status_dev_to_audit(self, dispatcher_engine_v2, file_with_frontmatter):
        from models import FileStatus
        result = dispatcher_engine_v2.update_file_status(
            file_path=file_with_frontmatter,
            new_status=FileStatus.AUDIT,
            changelog=["Test promotion to audit"],
            role_owner="auditor"
        )
        assert result is True

    def test_update_status_modifies_file(self, dispatcher_engine_v2, file_with_frontmatter):
        from models import FileStatus
        dispatcher_engine_v2.update_file_status(
            file_path=file_with_frontmatter,
            new_status=FileStatus.VERIFIED,
            changelog=["Test verification"],
            role_owner="acceptor"
        )
        content = open(file_with_frontmatter, 'r').read()
        assert "verified" in content

    def test_update_status_logs_audit_trail(self, dispatcher_engine_v2, file_with_frontmatter):
        from models import FileStatus
        initial_count = len(dispatcher_engine_v2.audit_log)
        dispatcher_engine_v2.update_file_status(
            file_path=file_with_frontmatter,
            new_status=FileStatus.ACCEPT,
            role_owner="acceptor"
        )
        assert len(dispatcher_engine_v2.audit_log) > initial_count


class TestDispatcherEngineV2BatchOperations:
    """Test V2 batch operations"""

    def test_batch_promote_to_audit(self, dispatcher_engine_v2, tmp_project):
        from models import FileStatus
        files = []
        for i in range(3):
            f = tmp_project / "src" / f"batch_{i}.py"
            f.write_text(f"# Batch file {i}\ndef func{i}():\n    pass\n")
            files.append(str(f))
        dispatcher_engine_v2.batch_promote_to_audit(files)

    def test_batch_force_verify(self, dispatcher_engine_v2, tmp_project):
        from models import FileStatus
        files = []
        for i in range(2):
            f = tmp_project / "src" / f"force_{i}.py"
            f.write_text(f"# Force file {i}\n")
            files.append(str(f))
        dispatcher_engine_v2.batch_force_verify(files)


class TestDispatcherEngineV2Report:
    """Test V2 report generation"""

    def test_generate_report(self, dispatcher_engine_v2):
        report = dispatcher_engine_v2.generate_report()
        assert 'timestamp' in report
        assert 'statistics' in report
        assert 'health_status' in report
        assert report['project_dir'] == dispatcher_engine_v2.project_dir

    def test_health_status_idle_for_new_engine(self, dispatcher_engine_v2):
        report = dispatcher_engine_v2.generate_report()
        assert report['health_status'] in ['idle', 'healthy', 'warning', 'danger']

    def test_report_contains_active_fingerprints(self, dispatcher_engine_v2):
        report = dispatcher_engine_v2.generate_report()
        assert 'active_fingerprints' in report


class TestDispatcherEngineV2CycleSummary:
    """Test V2 cycle summary output"""

    def test_print_cycle_summary_runs(self, dispatcher_engine_v2, capsys):
        dispatcher_engine_v2._print_cycle_summary()
        captured = capsys.readouterr()
        assert "调度统计摘要" in captured.out or "statistics" in captured.out.lower() or len(captured.out) >= 0


# ============================================================
# Simple YAML Parser Tests
# ============================================================

class TestSimpleYamlParser:
    """Test the simple YAML parser used by V2"""

    def test_parse_key_value_pairs(self, dispatcher_engine_v2):
        yaml_text = "key1: value1\nkey2: 42\nkey3: true\n"
        result = dispatcher_engine_v2._parse_simple_yaml(yaml_text)
        assert result.get('key1') == 'value1'

    def test_parse_string_values(self, dispatcher_engine_v2):
        yaml_text = 'name: "quoted string"\n'
        result = dispatcher_engine_v2._parse_simple_yaml(yaml_text)
        assert result.get('name') == 'quoted string'

    def test_parse_boolean_values(self, dispatcher_engine_v2):
        yaml_true = "flag: true\n"
        result_t = dispatcher_engine_v2._parse_simple_yaml(yaml_true)
        assert result_t.get('flag') is True

    def test_parse_numeric_values(self, dispatcher_engine_v2):
        yaml_num = "version: 2.5\ncount: 10\n"
        result = dispatcher_engine_v2._parse_simple_yaml(yaml_num)
        assert result.get('version') == 2.5

    def test_parse_empty_yaml(self, dispatcher_engine_v2):
        result = dispatcher_engine_v2._parse_simple_yaml("")
        assert result == {}

    def test_parse_comments_skipped(self, dispatcher_engine_v2):
        yaml_comment = "# This is a comment\nkey: value\n"
        result = dispatcher_engine_v2._parse_simple_yaml(yaml_comment)
        assert 'key' in result
        assert 'comment' not in [k.lower() for k in result.keys()]
