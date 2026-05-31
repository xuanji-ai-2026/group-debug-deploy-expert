"""
file_lock_manager.py - Enterprise-Grade File Locking System

Prevents concurrent editing conflicts in multi-Agent collaboration environment.
Supports cross-platform locking (Windows/Linux/Mac) with:
- Exclusive write locks (prevents concurrent modifications)
- Shared read locks (allows concurrent reads)
- Lock timeout & deadlock detection
- Automatic lock release on exception
- Lock state monitoring & statistics

Usage Scenarios:
- Developer editing file → Auditor cannot read/write simultaneously
- Acceptor running tests → Developer cannot modify during test execution
- Multiple agents accessing same file → Queue-based access control
"""

import os
import sys
import time
import hashlib
import json
import threading
from typing import Optional, Dict, List, Tuple, Any
from datetime import datetime
from dataclasses import dataclass, field
from enum import Enum
from contextlib import contextmanager

# Cross-platform imports for file locking
if sys.platform == 'win32':
    import msvcrt
else:
    try:
        import fcntl
    except ImportError:
        fcntl = None  # Fallback to metadata-only locking on unsupported platforms


class LockType(Enum):
    """Lock type enumeration"""
    SHARED = "shared"      # Read lock - multiple readers allowed
    EXCLUSIVE = "exclusive"  # Write lock - single writer only


class LockStatus(Enum):
    """Lock status enumeration"""
    FREE = "free"
    LOCKED = "locked"
    TIMEOUT = "timeout"
    DEADLOCK = "deadlock"
    ERROR = "error"


@dataclass
class LockInfo:
    """Lock information data structure"""
    file_path: str
    lock_type: LockType
    owner: str           # Role or agent name holding the lock
    acquired_at: float   # Timestamp when lock was acquired
    expires_at: float    # When the lock will auto-expire
    session_id: str      # Unique session identifier
    thread_id: int       # Thread ID of lock holder
    process_id: int      # Process ID
    
    def is_expired(self) -> bool:
        return time.time() > self.expires_at
    
    def to_dict(self) -> dict:
        return {
            'file_path': self.file_path,
            'lock_type': self.lock_type.value,
            'owner': self.owner,
            'acquired_at': datetime.fromtimestamp(self.acquired_at).isoformat(),
            'expires_at': datetime.fromtimestamp(self.expires_at).isoformat(),
            'session_id': self.session_id,
            'thread_id': self.thread_id,
            'process_id': self.process_id,
            'is_expired': self.is_expired()
        }


class FileLockError(Exception):
    """Base exception for file locking errors"""
    pass


class LockTimeoutError(FileLockError):
    """Raised when lock acquisition times out"""
    pass


class DeadlockDetectedError(FileLockError):
    """Raised when deadlock is detected"""
    pass


class FileLockManager:
    """
    Enterprise-grade file lock manager with cross-platform support.
    
    Features:
    - Cross-platform file locking (fcntl/msvcrt)
    - Automatic deadlock detection via wait-for graph
    - Configurable timeout per lock operation
    - Graceful degradation on unsupported platforms
    - Comprehensive lock state monitoring
    - Thread-safe operations
    """
    
    # Class-level singleton instance
    _instance = None
    _lock = threading.Lock()
    
    def __new__(cls, *args, **kwargs):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(
        self,
        default_timeout: float = 30.0,
        lock_dir: str = ".locks",
        enable_deadlock_detection: bool = True,
        max_wait_time: float = 120.0,
        cleanup_interval: float = 60.0
    ):
        """
        Initialize file lock manager
        
        Args:
            default_timeout: Default lock timeout in seconds (default: 30s)
            lock_dir: Directory to store lock metadata files
            enable_deadlock_detection: Whether to enable automatic deadlock detection
            max_wait_time: Maximum time to wait before declaring deadlock
            cleanup_interval: Interval for cleaning up expired locks
        """
        if hasattr(self, '_initialized') and self._initialized:
            return
            
        self.default_timeout = default_timeout
        self.lock_dir = lock_dir
        self.enable_deadlock_detection = enable_deadlock_detection
        self.max_wait_time = max_wait_time
        self.cleanup_interval = cleanup_interval
        
        # Active locks registry: {file_path: LockInfo}
        self._active_locks: Dict[str, LockInfo] = {}
        
        # Wait-for graph for deadlock detection: {file_path: [waiting_sessions]}
        self._wait_for_graph: Dict[str, List[str]] = {}
        
        # Statistics
        self.stats = {
            'total_locks_acquired': 0,
            'total_locks_released': 0,
            'timeouts_occurred': 0,
            'deadlocks_detected': 0,
            'conflicts_prevented': 0,
            'current_active_locks': 0
        }
        
        # Platform detection
        self._platform = sys.platform
        self._is_windows = self._platform == 'win32'
        
        # Ensure lock directory exists
        os.makedirs(self.lock_dir, exist_ok=True)
        
        # Start background cleanup thread
        self._cleanup_thread = threading.Thread(
            target=self._background_cleanup,
            daemon=True
        )
        self._cleanup_thread.start()
        
        self._initialized = True
    
    def acquire(
        self,
        file_path: str,
        lock_type: LockType = LockType.EXCLUSIVE,
        owner: str = "unknown",
        timeout: Optional[float] = None,
        blocking: bool = True
    ) -> bool:
        """
        Acquire a lock on the specified file.
        
        Args:
            file_path: Path to the file to lock
            lock_type: Type of lock (SHARED or EXCLUSIVE)
            owner: Name of the role/agent acquiring the lock
            timeout: Maximum time to wait for lock (None = use default)
            blocking: Whether to block until lock is available
            
        Returns:
            bool: True if lock acquired successfully
            
        Raises:
            LockTimeoutError: If timeout expires before lock acquired
            DeadlockDetectedError: If deadlock detected
            FileLockError: If other error occurs
        """
        timeout = timeout or self.default_timeout
        abs_path = os.path.abspath(file_path)
        session_id = self._generate_session_id()
        
        start_time = time.time()
        
        while True:
            elapsed = time.time() - start_time
            
            # Check for timeout
            if elapsed > timeout:
                self.stats['timeouts_occurred'] += 1
                raise LockTimeoutError(
                    f"Failed to acquire {lock_type.value} lock on {abs_path} "
                    f"after {timeout:.1f}s (timeout). "
                    f"Current holder: {self._get_current_holder(abs_path)}"
                )
            
            # Check for deadlock
            if self.enable_deadlock_detection and self._check_deadlock(abs_path, session_id):
                self.stats['deadlocks_detected'] += 1
                raise DeadlockDetectedError(
                    f"Deadlock detected while waiting for lock on {abs_path}"
                )
            
            # Try to acquire the lock
            try:
                success = self._try_acquire_lock(
                    abs_path, lock_type, owner, 
                    session_id, timeout
                )
                
                if success:
                    self.stats['total_locks_acquired'] += 1
                    self.stats['current_active_locks'] = len(self._active_locks)
                    return True
                    
            except Exception as e:
                raise FileLockError(f"Lock acquisition failed: {e}")
            
            # If non-blocking mode, return immediately
            if not blocking:
                return False
            
            # Wait a bit before retrying (exponential backoff)
            wait_time = min(0.1 * (2 ** int(elapsed / 2)), 1.0)
            time.sleep(wait_time)
    
    def release(self, file_path: str, owner: Optional[str] = None) -> bool:
        """
        Release a lock on the specified file.
        
        Args:
            file_path: Path to the file to unlock
            owner: Owner verification (optional, for security)
            
        Returns:
            bool: True if released successfully
        """
        abs_path = os.path.abspath(file_path)
        
        with self._lock:
            if abs_path not in self._active_locks:
                return False
            
            lock_info = self._active_locks[abs_path]
            
            # Verify owner if specified
            if owner and lock_info.owner != owner:
                raise FileLockError(
                    f"Permission denied: {owner} cannot release lock held by {lock_info.owner}"
                )
            
            # Remove from active locks
            del self._active_locks[abs_path]
            
            # Clean up metadata file
            self._cleanup_lock_metadata(abs_path)
            
            # Update wait-for graph
            if abs_path in self._wait_for_graph:
                del self._wait_for_graph[abs_path]
            
            self.stats['total_locks_released'] += 1
            self.stats['current_active_locks'] = len(self._active_locks)
            
            return True
    
    @contextmanager
    def locked_file(
        self,
        file_path: str,
        lock_type: LockType = LockType.EXCLUSIVE,
        owner: str = "unknown",
        timeout: Optional[float] = None
    ):
        """
        Context manager for automatic lock acquisition/release.
        
        Usage:
            with lock_manager.locked_file("file.txt", owner="developer") as f:
                content = f.read()
                f.write(new_content)
            # Lock automatically released here
        
        Args:
            file_path: Path to the file
            lock_type: Type of lock
            owner: Name of lock owner
            timeout: Lock acquisition timeout
            
        Yields:
            File object (opened in appropriate mode based on lock type)
        """
        abs_path = os.path.abspath(file_path)
        
        # Acquire lock
        self.acquire(abs_path, lock_type, owner, timeout)
        
        try:
            # Open file in appropriate mode
            mode = 'r' if lock_type == LockType.SHARED else 'r+'
            encoding = 'utf-8'
            
            with open(abs_path, mode, encoding=encoding) as f:
                yield f
                
        finally:
            # Always release lock
            self.release(abs_path, owner)
    
    def is_locked(self, file_path: str) -> bool:
        """Check if a file is currently locked"""
        abs_path = os.path.abspath(file_path)
        return abs_path in self._active_locks
    
    def get_lock_info(self, file_path: str) -> Optional[LockInfo]:
        """Get information about current lock on file"""
        abs_path = os.path.abspath(file_path)
        return self._active_locks.get(abs_path)
    
    def get_all_locks(self) -> Dict[str, LockInfo]:
        """Get all currently active locks"""
        return dict(self._active_locks)
    
    def force_release(self, file_path: str, reason: str = "administrative") -> bool:
        """
        Forcefully release a lock (use with caution!).
        
        Only for emergency/administrative use when normal release fails.
        """
        abs_path = os.path.abspath(file_path)
        
        if abs_path in self._active_locks:
            old_owner = self._active_locks[abs_path].owner
            del self._active_locks[abs_path]
            self._cleanup_lock_metadata(abs_path)
            
            print(f"⚠️ Force-released lock on {abs_path} (was held by {old_owner}, reason: {reason})")
            return True
        
        return False
    
    def get_statistics(self) -> dict:
        """Get lock manager statistics"""
        return {
            **self.stats,
            'active_locks_count': len(self._active_locks),
            'wait_graph_size': len(self._wait_for_graph),
            'uptime_seconds': time.time() - getattr(self, '_start_time', time.time())
        }
    
    def generate_report(self) -> str:
        """Generate human-readable lock status report"""
        lines = [
            "=" * 70,
            "File Lock Manager Status Report",
            "=" * 70,
            f"Generated: {datetime.now().isoformat()}",
            "",
            f"Active Locks: {len(self._active_locks)}",
            f"Total Acquired: {self.stats['total_locks_acquired']}",
            f"Total Released: {self.stats['total_locks_released']}",
            f"Timeouts: {self.stats['timeouts_occurred']}",
            f"Deadlocks: {self.stats['deadlocks_detected']}",
            f"Conflicts Prevented: {self.stats['conflicts_prevented']}",
            ""
        ]
        
        if self._active_locks:
            lines.append("Currently Locked Files:")
            lines.append("-" * 70)
            for path, info in self._active_locks.items():
                age = time.time() - info.acquired_at
                remaining = max(0, info.expires_at - time.time())
                lines.append(f"  📄 {os.path.basename(path)}")
                lines.append(f"     Type: {info.lock_type.value.upper()}")
                lines.append(f"     Owner: {info.owner}")
                lines.append(f"     Age: {age:.1f}s | Expires in: {remaining:.1f}s")
                lines.append(f"     Session: {info.session_id[:12]}...")
                lines.append("")
        else:
            lines.append("No files currently locked.")
        
        lines.extend([
            "-" * 70,
            "Platform: " + self._platform,
            "Default Timeout: " + str(self.default_timeout) + "s",
            "Deadlock Detection: " + ("ENABLED" if self.enable_deadlock_detection else "DISABLED"),
            "=" * 70
        ])
        
        return "\n".join(lines)
    
    # ===== Private Implementation Methods =====
    
    def _try_acquire_lock(
        self,
        abs_path: str,
        lock_type: LockType,
        owner: str,
        session_id: str,
        timeout: float
    ) -> bool:
        """Internal method to attempt lock acquisition"""
        
        with self._lock:
            # Check if already locked
            if abs_path in self._active_locks:
                existing_lock = self._active_locks[abs_path]
                
                # Check if expired
                if existing_lock.is_expired():
                    # Auto-release expired lock
                    del self._active_locks[abs_path]
                    self._cleanup_lock_metadata(abs_path)
                    self.stats['conflicts_prevented'] += 1
                elif lock_type == LockType.SHARED and existing_lock.lock_type == LockType.SHARED:
                    # Allow multiple shared locks
                    pass
                else:
                    # Conflict - cannot acquire
                    return False
            
            # Create new lock info
            now = time.time()
            lock_info = LockInfo(
                file_path=abs_path,
                lock_type=lock_type,
                owner=owner,
                acquired_at=now,
                expires_at=now + timeout,
                session_id=session_id,
                thread_id=threading.get_ident(),
                process_id=os.getpid()
            )
            
            # Try platform-specific file lock
            if self._acquire_platform_lock(abs_path, lock_type):
                # Register in active locks
                self._active_locks[abs_path] = lock_info
                
                # Save metadata
                self._save_lock_metadata(lock_info)
                
                return True
            else:
                return False
    
    def _acquire_platform_lock(self, abs_path: str, lock_type: LockType) -> bool:
        """Acquire platform-specific file system lock"""
        try:
            # Create/open lock file
            lock_file_path = self._get_lock_file_path(abs_path)
            
            # Use different modes for shared vs exclusive
            if self._is_windows:
                return self._windows_acquire(lock_file_path, lock_type)
            else:
                return self._posix_acquire(lock_file_path, lock_type)
                
        except Exception as e:
            # Fallback to metadata-based locking if OS-level lock fails
            return self._metadata_only_lock(abs_path, lock_type)
    
    def _windows_acquire(self, lock_file_path: str, lock_type: LockType) -> bool:
        """Windows-specific lock using msvcrt"""
        try:
            mode = 'r+' if os.path.exists(lock_file_path) else 'w+'
            fd = open(lock_file_path, mode)
            
            if lock_type == LockType.EXCLUSIVE:
                msvcrt.locking(fd.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                msvcrt.locking(fd.fileno(), msvcrt.LK_RBLCK, 1)
            
            fd.close()
            return True
        except (IOError, OSError):
            return False
    
    def _posix_acquire(self, lock_file_path: str, lock_type: LockType) -> bool:
        """POSIX lock using fcntl"""
        try:
            mode = 'r+' if os.path.exists(lock_file_path) else 'w+'
            fd = open(lock_file_path, mode)
            
            if lock_type == LockType.EXCLUSIVE:
                fcntl.flock(fd.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
            else:
                fcntl.flock(fd.fileno(), fcntl.LOCK_SH | fcntl.LOCK_NB)
            
            fd.close()
            return True
        except (IOError, OSError, BlockingIOError):
            return False
    
    def _metadata_only_lock(self, abs_path: str, lock_type: LockType) -> bool:
        """Fallback: metadata-only locking without OS-level enforcement"""
        # This is less secure but works everywhere
        meta_path = abs_path + ".lockmeta"
        
        try:
            if os.path.exists(meta_path):
                with open(meta_path, 'r') as f:
                    data = json.load(f)
                    
                # Check if still valid
                if data.get('expires_at', 0) > time.time():
                    return False  # Still locked by someone else
            
            # Write our lock metadata
            with open(meta_path, 'w') as f:
                json.dump({
                    'locked': True,
                    'type': lock_type.value,
                    'timestamp': time.time(),
                    'expires': time.time() + self.default_timeout
                }, f)
            
            return True
        except Exception:
            return False
    
    def _get_lock_file_path(self, abs_path: str) -> str:
        """Generate path for lock metadata file"""
        file_hash = hashlib.md5(abs_path.encode()).hexdigest()[:16]
        return os.path.join(self.lock_dir, f".lock_{file_hash}")
    
    def _save_lock_metadata(self, lock_info: LockInfo):
        """Save lock information to metadata file"""
        meta_path = self._get_lock_file_path(lock_info.file_path) + ".json"
        
        with open(meta_path, 'w', encoding='utf-8') as f:
            json.dump(lock_info.to_dict(), f, indent=2, ensure_ascii=False)
    
    def _cleanup_lock_metadata(self, abs_path: str):
        """Remove lock metadata files"""
        base_path = self._get_lock_file_path(abs_path)
        
        for ext in ['', '.json']:
            path = base_path + ext
            if os.path.exists(path):
                try:
                    os.remove(path)
                except Exception:
                    pass
    
    def _generate_session_id(self) -> str:
        """Generate unique session ID"""
        return f"LOCK-{int(time.time()*1000)}-{os.getpid()}-{threading.get_ident()}"
    
    def _get_current_holder(self, abs_path: str) -> Optional[str]:
        """Get current lock holder name"""
        if abs_path in self._active_locks:
            return self._active_locks[abs_path].owner
        return None
    
    def _check_deadlock(self, abs_path: str, session_id: str) -> bool:
        """Detect potential deadlock using wait-for graph"""
        if not self.enable_deadlock_detection:
            return False
        
        # Add to wait-for graph
        if abs_path not in self._wait_for_graph:
            self._wait_for_graph[abs_path] = []
        self._wait_for_graph[abs_path].append(session_id)
        
        # Simple cycle detection (can be enhanced with full graph algorithm)
        visited = set()
        stack = [(abs_path, session_id)]
        
        while stack:
            current_path, current_session = stack.pop(0)
            
            if (current_path, current_session) in visited:
                return True  # Cycle detected!
            
            visited.add((current_path, current_session))
            
            # Find what this session is waiting for
            if current_path in self._active_locks:
                holder_info = self._active_locks[current_path]
                # Check if holder is waiting for something we hold
                for waited_path, sessions in self._wait_for_graph.items():
                    if holder_info.session_id in sessions:
                        stack.append((waited_path, holder_info.session_id))
        
        return False
    
    def _background_cleanup(self):
        """Background thread to clean up expired locks"""
        while True:
            try:
                time.sleep(self.cleanup_interval)
                
                with self._lock:
                    now = time.time()
                    expired_files = []
                    
                    for path, info in list(self._active_locks.items()):
                        if info.is_expired():
                            expired_files.append(path)
                    
                    for path in expired_files:
                        old_owner = self._active_locks[path].owner
                        del self._active_locks[path]
                        self._cleanup_lock_metadata(path)
                        
                        if path in self._wait_for_graph:
                            del self._wait_for_graph[path]
                        
                        print(f"🧹 Auto-released expired lock on {path} (held by {old_owner})")
                        
            except Exception as e:
                pass  # Silently handle cleanup errors


# ===== Global Singleton Instance =====
_global_lock_manager: Optional[FileLockManager] = None


def get_lock_manager(**kwargs) -> FileLockManager:
    """Get global file lock manager instance"""
    global _global_lock_manager
    
    if _global_lock_manager is None:
        _global_lock_manager = FileLockManager(**kwargs)
    
    return _global_lock_manager


# ===== Convenience Functions =====

def lock_file(
    file_path: str,
    lock_type: LockType = LockType.EXCLUSIVE,
    owner: str = "system",
    timeout: float = 30.0
) -> bool:
    """Convenience function to acquire a file lock"""
    return get_lock_manager().acquire(file_path, lock_type, owner, timeout)


def unlock_file(file_path: str, owner: Optional[str] = None) -> bool:
    """Convenience function to release a file lock"""
    return get_lock_manager().release(file_path, owner)


@contextmanager
def with_file_lock(
    file_path: str,
    mode: str = 'r',
    lock_type: LockType = LockType.EXCLUSIVE,
    owner: str = "system",
    timeout: float = 30.0
):
    """
    Context manager for safe file access with automatic locking.
    
    Usage:
        with with_file_lock("config.json", 'r+', owner="developer") as f:
            data = json.load(f)
            data['updated'] = True
            json.dump(data, f)
        # Lock automatically released
    """
    abs_path = os.path.abspath(file_path)
    manager = get_lock_manager()
    
    manager.acquire(abs_path, lock_type, owner, timeout)
    
    try:
        with open(abs_path, mode, encoding='utf-8') as f:
            yield f
    finally:
        manager.release(abs_path, owner)


if __name__ == "__main__":
    # Test the file lock manager
    print("=" * 70)
    print("  🔐 File Lock Manager Test Suite")
    print("=" * 70)
    
    # Initialize
    manager = get_lock_manager(default_timeout=10.0)
    
    test_file = "test_locked_file.txt"
    
    # Create test file
    with open(test_file, 'w', encoding='utf-8') as f:
        f.write("Initial content\n")
    
    print("\n✅ Test 1: Basic Lock Acquisition")
    try:
        result = manager.acquire(test_file, LockType.EXCLUSIVE, owner="Developer-A", timeout=5.0)
        print(f"   Lock acquired: {result}")
        print(f"   Is locked: {manager.is_locked(test_file)}")
        
        lock_info = manager.get_lock_info(test_file)
        if lock_info:
            print(f"   Lock owner: {lock_info.owner}")
            print(f"   Session ID: {lock_info.session_id[:20]}...")
        
        # Try to acquire again (should fail or block)
        print("\n⚠️ Test 2: Concurrent Access Attempt")
        try:
            manager.acquire(test_file, LockType.EXCLUSIVE, owner="Auditor-B", timeout=2.0, blocking=False)
            print("   Unexpected: Second lock acquired!")
        except LockTimeoutError as e:
            print(f"   ✓ Expected timeout: {str(e)[:60]}...")
        
        # Release
        print("\n✅ Test 3: Lock Release")
        released = manager.release(test_file, owner="Developer-A")
        print(f"   Released: {released}")
        print(f"   Is locked: {manager.is_locked(test_file)}")
        
    except Exception as e:
        print(f"   Error: {e}")
    finally:
        # Cleanup
        if manager.is_locked(test_file):
            manager.force_release(test_file, "test cleanup")
        if os.path.exists(test_file):
            os.remove(test_file)
    
    # Generate report
    print("\n" + manager.generate_report())
    
    stats = manager.get_statistics()
    print(f"\n📊 Final Statistics:")
    print(f"   Total locks acquired: {stats['total_locks_acquired']}")
    print(f"   Total locks released: {stats['total_locks_released']}")
    print(f"   Timeouts: {stats['timeouts_occurred']}")
    print(f"   Deadlocks detected: {stats['deadlocks_detected']}")
    
    print("\n" + "=" * 70)
    print("  ✅ All tests completed!")
    print("=" * 70)
