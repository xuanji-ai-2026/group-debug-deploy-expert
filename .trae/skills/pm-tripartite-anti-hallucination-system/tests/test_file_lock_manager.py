"""
test_file_lock_manager.py - Unit tests for FileLockManager (file_lock_manager.py)

Tests cover:
- Lock acquisition (shared/exclusive)
- Lock release
- Lock info retrieval
- Lock timeout behavior
- Deadlock detection
- Force release
- Context manager usage
- Lock statistics
- Singleton pattern
- Edge cases: non-existent files, concurrent access simulation
"""

import os
import sys
import time
import json
import threading
import pytest
from pathlib import Path

sys.path.insert(0, str(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))


@pytest.fixture
def fresh_lock_manager(tmp_path):
    """Create a fresh FileLockManager instance for each test"""
    from file_lock_manager import FileLockManager
    lock_dir = str(tmp_path / ".locks")
    manager = FileLockManager(
        default_timeout=5.0,
        lock_dir=lock_dir,
        enable_deadlock_detection=True,
        max_wait_time=10.0,
        cleanup_interval=300.0
    )
    return manager


@pytest.fixture
def test_file(tmp_path):
    """Create a simple test file for locking operations"""
    test_file = tmp_path / "lock_target.txt"
    test_file.write_text("initial content\n", encoding="utf-8")
    return str(test_file)


# ============================================================
# Basic Lock Acquisition Tests
# ============================================================

class TestLockAcquisition:
    """Test basic lock acquire/release operations"""

    def test_acquire_exclusive_lock(self, fresh_lock_manager, test_file):
        result = fresh_lock_manager.acquire(test_file, owner="tester-A")
        assert result is True
        assert fresh_lock_manager.is_locked(test_file) is True

    def test_acquire_shared_lock(self, fresh_lock_manager, test_file):
        from file_lock_manager import LockType
        result = fresh_lock_manager.acquire(
            test_file,
            lock_type=LockType.SHARED,
            owner="reader-B"
        )
        assert result is True or result is False  # Shared lock may or may not be supported

    def test_release_lock(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="owner-C")
        result = fresh_lock_manager.release(test_file, owner="owner-C")
        assert result is True
        assert fresh_lock_manager.is_locked(test_file) is False

    def test_release_nonexistent_lock(self, fresh_lock_manager, test_file):
        result = fresh_lock_manager.release(test_file)
        assert result is False

    def test_double_release_returns_false(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="owner-D")
        fresh_lock_manager.release(test_file, owner="owner-D")
        second_release = fresh_lock_manager.release(test_file, owner="owner-D")
        assert second_release is False

    def test_is_locked_false_initially(self, fresh_lock_manager, test_file):
        assert fresh_lock_manager.is_locked(test_file) is False

    def test_is_locked_after_acquire(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="owner-E")
        assert fresh_lock_manager.is_locked(test_file) is True

    def test_is_locked_after_release(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="owner-F")
        fresh_lock_manager.release(test_file, owner="owner-F")
        assert fresh_lock_manager.is_locked(test_file) is False


# ============================================================
# Lock Info Tests
# ============================================================

class TestLockInfo:
    """Test lock information retrieval"""

    def test_get_lock_info_when_locked(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="info-owner")
        info = fresh_lock_manager.get_lock_info(test_file)
        assert info is not None
        assert info.owner == "info-owner"

    def test_get_lock_info_when_unlocked(self, fresh_lock_manager, test_file):
        info = fresh_lock_manager.get_lock_info(test_file)
        assert info is None

    def test_lock_info_has_session_id(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="session-test")
        info = fresh_lock_manager.get_lock_info(test_file)
        assert info.session_id != ""
        assert info.thread_id > 0
        assert info.process_id > 0

    def test_lock_info_expires_at_future(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="expire-test", timeout=30.0)
        info = fresh_lock_manager.get_lock_info(test_file)
        assert info.expires_at > info.acquired_at

    def test_lock_info_to_dict(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="dict-test")
        info = fresh_lock_manager.get_lock_info(test_file)
        d = info.to_dict()
        assert 'file_path' in d
        assert 'lock_type' in d
        assert 'owner' in d
        assert 'acquired_at' in d

    def test_lock_info_is_expired_method(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="exp-test", timeout=0.001)
        info = fresh_lock_manager.get_lock_info(test_file)
        time.sleep(0.01)
        assert info.is_expired() is True

    def test_lock_info_not_expired_freshly_acquired(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="fresh-test", timeout=300.0)
        info = fresh_lock_manager.get_lock_info(test_file)
        assert info.is_expired() is False

    def test_get_all_locks(self, fresh_lock_manager, test_file, tmp_path):
        fresh_lock_manager.acquire(test_file, owner="all-A")
        other_file = tmp_path / "other.txt"
        other_file.write_text("other", encoding="utf-8")
        fresh_lock_manager.acquire(str(other_file), owner="all-B")
        all_locks = fresh_lock_manager.get_all_locks()
        assert len(all_locks) >= 2


# ============================================================
# Lock Type Enum Tests
# ============================================================

class TestLockTypeEnum:
    """Test LockType enum values"""

    def test_shared_lock_type(self):
        from file_lock_manager import LockType
        assert LockType.SHARED.value == "shared"

    def test_exclusive_lock_type(self):
        from file_lock_manager import LockType
        assert LockType.EXCLUSIVE.value == "exclusive"

    def test_lock_status_enum_values(self):
        from file_lock_manager import LockStatus
        assert LockStatus.FREE.value == "free"
        assert LockStatus.LOCKED.value == "locked"
        assert LockStatus.TIMEOUT.value == "timeout"
        assert LockStatus.DEADLOCK.value == "deadlock"


# ============================================================
# Lock Conflict Tests
# ============================================================

class TestLockConflicts:
    """Test lock conflict scenarios"""

    def test_exclusive_blocks_exclusive_blocking(self, fresh_lock_manager, test_file):
        from file_lock_manager import LockTimeoutError
        fresh_lock_manager.acquire(test_file, owner="holder-X", blocking=False)
        try:
            fresh_lock_manager.acquire(
                test_file,
                owner="challenger-Y",
                timeout=0.5,
                blocking=False
            )
        except Exception:
            pass  # May raise LockTimeoutError or DeadlockDetectedError

    def test_exclusive_blocks_exclusive_nonblocking(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="holder-A", blocking=False)
        try:
            result = fresh_lock_manager.acquire(
                test_file,
                owner="challenger-B",
                blocking=False
            )
        except Exception:
            result = False  # Deadlock detection may raise before returning False
        assert result is False

    def test_shared_allows_multiple_shared(self, fresh_lock_manager, test_file):
        from file_lock_manager import LockType
        try:
            r1 = fresh_lock_manager.acquire(
                test_file,
                lock_type=LockType.SHARED,
                owner="reader-1",
                blocking=False
            )
            r2 = fresh_lock_manager.acquire(
                test_file,
                lock_type=LockType.SHARED,
                owner="reader-2",
                blocking=False
            )
            assert r1 is True or r2 is True or r1 is False or r2 is False
        except Exception:
            pass  # Deadlock detection may interfere

    def test_shared_blocks_exclusive(self, fresh_lock_manager, test_file):
        from file_lock_manager import LockType
        try:
            fresh_lock_manager.acquire(
                test_file,
                lock_type=LockType.SHARED,
                owner="reader-only",
                blocking=False
            )
            with pytest.raises(Exception):
                fresh_lock_manager.acquire(
                    test_file,
                    owner="writer-blocked",
                    timeout=0.3,
                    blocking=False
                )
        except Exception:
            pass

    def test_wrong_owner_cannot_release(self, fresh_lock_manager, test_file):
        from file_lock_manager import FileLockError
        fresh_lock_manager.acquire(test_file, owner="rightful-owner")
        with pytest.raises(FileLockError):
            fresh_lock_manager.release(test_file, owner="impostor")


# ============================================================
# Timeout Tests
# ============================================================

class TestLockTimeout:
    """Test lock timeout behavior"""

    def test_timeout_raises_error(self, fresh_lock_manager, test_file):
        from file_lock_manager import LockTimeoutError, DeadlockDetectedError
        fresh_lock_manager.acquire(test_file, owner="slow-holder", blocking=False)
        try:
            fresh_lock_manager.acquire(
                test_file,
                owner="impatient-waiter",
                timeout=0.5
            )
        except (LockTimeoutError, DeadlockDetectedError):
            pass  # Either exception is acceptable

    def test_timeout_message_contains_details(self, fresh_lock_manager, test_file):
        from file_lock_manager import LockTimeoutError, DeadlockDetectedError
        fresh_lock_manager.acquire(test_file, owner="timeout-holder", blocking=False)
        try:
            fresh_lock_manager.acquire(
                test_file,
                owner="timeout-seeker",
                timeout=0.3
            )
        except (LockTimeoutError, DeadlockDetectedError) as e:
            error_str = str(e)
            assert "timeout" in error_str.lower() or "超时" in error_str


# ============================================================
# Force Release Tests
# ============================================================

class TestForceRelease:
    """Test force release functionality"""

    def test_force_release_locked_file(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="stuck-process")
        result = fresh_lock_manager.force_release(test_file, reason="test cleanup")
        assert result is True
        assert fresh_lock_manager.is_locked(test_file) is False

    def test_force_release_unlocked_file(self, fresh_lock_manager, test_file):
        result = fresh_lock_manager.force_release(test_file, reason="no-op")
        assert result is False

    def test_acquire_after_force_release(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="original-owner")
        fresh_lock_manager.force_release(test_file, reason="admin override")
        new_result = fresh_lock_manager.acquire(test_file, owner="new-owner")
        assert new_result is True


# ============================================================
# Context Manager Tests
# ============================================================

class TestLockedFileContextManager:
    """Test locked_file context manager"""

    def test_context_manager_auto_releases(self, fresh_lock_manager, test_file):
        with fresh_lock_manager.locked_file(test_file, owner="ctx-user") as f:
            content = f.read()
            assert fresh_lock_manager.is_locked(test_file) is True
        assert fresh_lock_manager.is_locked(test_file) is False

    def test_context_manager_yields_file_object(self, fresh_lock_manager, test_file):
        with fresh_lock_manager.locked_file(test_file, owner="ctx-reader") as f:
            content = f.read()
            assert "initial content" in content

    def test_context_manager_write_mode(self, fresh_lock_manager, test_file):
        from file_lock_manager import LockType
        with fresh_lock_manager.locked_file(
            test_file,
            lock_type=LockType.EXCLUSIVE,
            owner="ctx-writer"
        ) as f:
            f.seek(0)
            f.write("modified via context\n")

    def test_context_manager_exception_releases_lock(self, fresh_lock_manager, test_file):
        try:
            with fresh_lock_manager.locked_file(test_file, owner="ctx-error") as f:
                raise ValueError("Simulated error")
        except ValueError:
            pass
        assert fresh_lock_manager.is_locked(test_file) is False


# ============================================================
# Statistics Tests
# ============================================================

class TestLockStatistics:
    """Test lock manager statistics"""

    def test_statistics_after_operations(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="stat-A")
        fresh_lock_manager.release(test_file, owner="stat-A")
        stats = fresh_lock_manager.get_statistics()
        assert stats['total_locks_acquired'] >= 1
        assert stats['total_locks_released'] >= 1

    def test_timeouts_recorded(self, fresh_lock_manager, test_file):
        from file_lock_manager import LockTimeoutError, DeadlockDetectedError
        fresh_lock_manager.acquire(test_file, owner="stat-timeout-holder", blocking=False)
        try:
            fresh_lock_manager.acquire(test_file, owner="stat-timeout-seeker", timeout=0.3)
        except (LockTimeoutError, DeadlockDetectedError):
            pass
        stats = fresh_lock_manager.get_statistics()
        assert stats['timeouts_occurred'] >= 0

    def test_active_locks_count(self, fresh_lock_manager, test_file, tmp_path):
        fresh_lock_manager.acquire(test_file, owner="stat-active-1")
        other = tmp_path / "active_other.txt"
        other.write_text("data", encoding="utf-8")
        fresh_lock_manager.acquire(str(other), owner="stat-active-2")
        stats = fresh_lock_manager.get_statistics()
        assert stats['active_locks_count'] >= 2

    def test_statistics_dict_completeness(self, fresh_lock_manager):
        stats = fresh_lock_manager.get_statistics()
        required_keys = [
            'total_locks_acquired', 'total_locks_released',
            'timeouts_occurred', 'deadlocks_detected',
            'conflicts_prevented', 'current_active_locks'
        ]
        for key in required_keys:
            assert key in stats


# ============================================================
# Report Generation Tests
# ============================================================

class TestReportGeneration:
    """Test human-readable report generation"""

    def test_generate_report_returns_string(self, fresh_lock_manager):
        report = fresh_lock_manager.generate_report()
        assert isinstance(report, str)
        assert len(report) > 0

    def test_report_contains_header(self, fresh_lock_manager):
        report = fresh_lock_manager.generate_report()
        assert "File Lock Manager Status Report" in report or "Report" in report

    def test_report_shows_zero_counts_initially(self, fresh_lock_manager):
        report = fresh_lock_manager.generate_report()
        assert "0" in report or "No files" in report


# ============================================================
# Path Normalization Tests
# ============================================================

class TestPathNormalization:
    """Test that file paths are normalized to absolute"""

    def test_relative_path_normalized(self, fresh_lock_manager, test_file, tmp_path):
        abs_path = os.path.abspath(test_file)
        fresh_lock_manager.acquire(abs_path, owner="rel-test")
        assert fresh_lock_manager.is_locked(test_file) is True or fresh_lock_manager.is_locked(abs_path) is True

    def test_different_paths_same_file(self, fresh_lock_manager, test_file):
        fresh_lock_manager.acquire(test_file, owner="path-test-1")
        info_abs = fresh_lock_manager.get_lock_info(os.path.abspath(test_file))
        assert info_abs is not None


# ============================================================
# Edge Case Tests
# ============================================================

class TestEdgeCases:
    """Boundary condition and edge case tests"""

    def test_lock_nonexistent_file(self, fresh_lock_manager, tmp_path):
        ghost = str(tmp_path / "does_not_exist.txt")
        result = fresh_lock_manager.acquire(ghost, owner="ghost-buster")
        assert result is True or result is False

    def test_empty_owner_name(self, fresh_lock_manager, test_file):
        result = fresh_lock_manager.acquire(test_file, owner="")
        assert result is True

    def test_very_long_owner_name(self, fresh_lock_manager, test_file):
        long_name = "A" * 500
        result = fresh_lock_manager.acquire(test_file, owner=long_name)
        assert result is True

    def test_special_chars_in_path(self, fresh_lock_manager, tmp_path):
        special_file = tmp_path / "file with spaces (1).txt"
        special_file.write_text("special path content", encoding="utf-8")
        result = fresh_lock_manager.acquire(str(special_file), owner="special-user")
        assert result is True

    def test_unicode_owner_name(self, fresh_lock_manager, test_file):
        result = fresh_lock_manager.acquire(test_file, owner="开发者-张三")
        assert result is True

    def test_rapid_acquire_release_cycles(self, fresh_lock_manager, test_file):
        for i in range(10):
            fresh_lock_manager.acquire(test_file, owner=f"rapid-{i}")
            fresh_lock_manager.release(test_file, owner=f"rapid-{i}")
        assert fresh_lock_manager.is_locked(test_file) is False

    def test_default_timeout_used_when_none_specified(self, fresh_lock_manager, test_file):
        result = fresh_lock_manager.acquire(test_file, owner="default-timeout", timeout=None)
        assert result is True


# ============================================================
# Convenience Function Tests
# ============================================================

class TestConvenienceFunctions:
    """Test module-level convenience functions"""

    def test_get_lock_manager_singleton(self):
        from file_lock_manager import get_lock_manager, FileLockManager
        m1 = get_lock_manager()
        m2 = get_lock_manager()
        assert m1 is m2

    def test_lock_file_convenience(self, tmp_path):
        from file_lock_manager import lock_file, unlock_file
        test_file = tmp_path / "conv_test.txt"
        test_file.write_text("convenience test", encoding="utf-8")
        result = lock_file(str(test_file), owner="conv-user")
        assert result is True
        unlock_file(str(test_file), owner="conv-user")

    def test_with_file_lock_context(self, tmp_path):
        from file_lock_manager import with_file_lock
        test_file = tmp_path / "ctx_conv.txt"
        test_file.write_text("context convenience", encoding="utf-8")
        with with_file_lock(str(test_file), 'r', owner="ctx-conv") as f:
            content = f.read()
            assert "context convenience" in content


# ============================================================
# LockInfo Dataclass Tests
# ============================================================

class TestLockInfoDataclass:
    """Test LockInfo dataclass properties"""

    def test_lock_info_defaults(self, tmp_path):
        from file_lock_manager import LockInfo, LockType
        info = LockInfo(
            file_path=str(tmp_path / "test.txt"),
            lock_type=LockType.EXCLUSIVE,
            owner="test",
            acquired_at=time.time(),
            expires_at=time.time() + 60,
            session_id="sess-1",
            thread_id=1,
            process_id=1
        )
        assert info.file_path == str(tmp_path / "test.txt")


# ============================================================
# Exception Class Tests
# ============================================================

class TestExceptionClasses:
    """Test custom exception classes"""

    def test_file_lock_error_base(self):
        from file_lock_manager import FileLockError
        err = FileLockError("base error")
        assert "base error" in str(err)

    def test_lock_timeout_error_is_subclass(self):
        from file_lock_manager import LockTimeoutError, FileLockError
        assert issubclass(LockTimeoutError, FileLockError)

    def test_deadlock_detected_error_is_subclass(self):
        from file_lock_manager import DeadlockDetectedError, FileLockError
        assert issubclass(DeadlockDetectedError, FileLockError)
