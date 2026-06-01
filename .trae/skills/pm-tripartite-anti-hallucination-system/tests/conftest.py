"""
conftest.py - Pytest Fixtures for PM Tripartite Anti-Hallucination System Test Suite

Provides shared fixtures across all test modules:
- Temporary project directories
- Sample files with frontmatter
- Model instances (FileFrontmatter, RCAReport, IssueTicket, ErrorFingerprint)
- Engine instances (DispatcherEngine, DispatcherEngineV2)
- Tool interceptor instances
- Lock manager instances
- Log manager instances
"""

import os
import sys
import json
import hashlib
from datetime import datetime

import pytest

sys.path.insert(0, str(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))


@pytest.fixture(scope="session")
def skill_root():
    """Return the root path of the skill directory"""
    return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


@pytest.fixture
def tmp_project(tmp_path):
    """
    Create a temporary project directory with standard structure.
    
    Returns the path to the temporary project directory.
    """
    required_dirs = [
        "src",
        ".constitution",
        ".issues/open",
        ".issues/in_progress",
        ".issues/resolved",
        "tests/unit",
        "tests/integration",
        "docs",
        ".quarantine",
    ]
    for d in required_dirs:
        (tmp_path / d).mkdir(parents=True, exist_ok=True)
    
    yield tmp_path


@pytest.fixture
def sample_frontmatter_data():
    """Return valid frontmatter data dictionary"""
    return {
        "id": "TEST-001",
        "module_name": "test.module.auth",
        "domain": "Test Domain - Authentication Module",
        "status": "dev",
        "role_owner": "developer",
        "version": 1.0,
        "genesis_hash": hashlib.sha256(b"initial").hexdigest(),
        "previous_hash": hashlib.sha256(b"previous").hexdigest(),
        "description": "Test authentication module",
        "capabilities": ["login", "logout", "register"],
        "tags": ["security", "core"],
    }


@pytest.fixture
def sample_frontmatter(sample_frontmatter_data):
    """Create a FileFrontmatter instance with valid data"""
    from models import FileFrontmatter, FileStatus
    
    return FileFrontmatter(
        id=sample_frontmatter_data["id"],
        module_name=sample_frontmatter_data["module_name"],
        domain=sample_frontmatter_data["domain"],
        status=FileStatus.DEV,
        role_owner=sample_frontmatter_data["role_owner"],
        version=sample_frontmatter_data["version"],
        genesis_hash=sample_frontmatter_data["genesis_hash"],
        previous_hash=sample_frontmatter_data["previous_hash"],
        description=sample_frontmatter_data["description"],
        capabilities=sample_frontmatter_data["capabilities"],
        tags=sample_frontmatter_data["tags"],
    )


@pytest.fixture
def sample_rca_report_data():
    """Return valid RCA report data"""
    return {
        "rca_type": "root_cause_analysis",
        "symptom": "第45行抛出空指针异常",
        "trigger_chain": "用户登录 -> 校验Token -> 获取User对象 -> User对象为空",
        "root_cause": "数据库查询未处理软删除用户，导致返回空结果",
        "fix_plan": "在查询层增加 is_deleted = false 过滤条件",
        "affected_files": ["src/backend/auth.py"]
    }


@pytest.fixture
def sample_rca_report(sample_rca_report_data):
    """Create an RCAReport instance"""
    from models import RCAReport
    return RCAReport(**sample_rca_report_data)


@pytest.fixture
def sample_issue_ticket_data():
    """Return valid issue ticket data"""
    return {
        "id": "ISSUE-20260601001",
        "file_path": "src/backend/auth.py",
        "issue_type": "bug",
        "severity": "major",
        "description": "空指针异常：未检查用户对象是否为None",
        "created_by": "auditor"
    }


@pytest.fixture
def sample_issue_ticket(sample_issue_ticket_data):
    """Create an IssueTicket instance"""
    from models import IssueTicket
    return IssueTicket(**sample_issue_ticket_data)


@pytest.fixture
def sample_error_fingerprint_data():
    """Return valid error fingerprint data"""
    return {
        "fingerprint": hashlib.md5("TypeError: NoneType".encode()).hexdigest(),
        "error_type": "TypeError",
        "file_path": "src/backend/auth.py",
        "keywords": "null pointer"
    }


@pytest.fixture
def sample_error_fingerprint(sample_error_fingerprint_data):
    """Create an ErrorFingerprint instance"""
    from models import ErrorFingerprint
    return ErrorFingerprint(**sample_error_fingerprint_data)


@pytest.fixture
def developer_tools_list():
    """Return developer's allowed tools list"""
    return [
        "read_file",
        "trace_call_stack",
        "replace_in_file",
        "create_rca_report",
        "request_human_intervention"
    ]


@pytest.fixture
def auditor_tools_list():
    """Return auditor's allowed tools list"""
    return [
        "read_file",
        "read_related_files",
        "create_issue_ticket",
        "verify_rca_logic",
        "run_lint_check"
    ]


@pytest.fixture
def acceptor_tools_list():
    """Return acceptor's allowed tools list"""
    return [
        "run_unit_tests",
        "run_integration_tests",
        "check_coverage",
        "update_status_to_verified"
    ]


@pytest.fixture
def dispatcher_tools_list():
    """Return dispatcher's allowed tools list"""
    return [
        "scan_filesystem",
        "enforce_constitution",
        "calculate_hash",
        "move_to_quarantine",
        "circuit_breaker_trigger",
        "log_audit_trail",
        "wake_up_role"
    ]


@pytest.fixture
def developer_interceptor(developer_tools_list):
    """Create AtomicToolInterceptor for developer role"""
    from tools import AtomicToolInterceptor
    return AtomicToolInterceptor(developer_tools_list, "developer")


@pytest.fixture
def auditor_interceptor(auditor_tools_list):
    """Create AtomicToolInterceptor for auditor role"""
    from tools import AtomicToolInterceptor
    return AtomicToolInterceptor(auditor_tools_list, "auditor")


@pytest.fixture
def acceptor_interceptor(acceptor_tools_list):
    """Create AtomicToolInterceptor for acceptor role"""
    from tools import AtomicToolInterceptor
    return AtomicToolInterceptor(acceptor_tools_list, "acceptor")


@pytest.fixture
def dispatcher_engine(tmp_project):
    """Create DispatcherEngine instance pointing to temp project dir"""
    from dispatcher import DispatcherEngine
    return DispatcherEngine(str(tmp_project))


@pytest.fixture
def dispatcher_engine_v2(tmp_project):
    """Create DispatcherEngineV2 instance pointing to temp project dir"""
    from dispatcher_update import DispatcherEngineV2
    return DispatcherEngineV2(str(tmp_project))


@pytest.fixture
def file_with_frontmatter(tmp_project, sample_frontmatter_data):
    """
    Create a file in tmp_project with YAML frontmatter.
    Returns the full path to the created file.
    """
    src_dir = tmp_project / "src"
    src_dir.mkdir(exist_ok=True)
    
    file_path = src_dir / "auth.py"
    content = f"""---
id: {sample_frontmatter_data['id']}
status: {sample_frontmatter_data['status']}
role_owner: {sample_frontmatter_data['role_owner']}
version: {sample_frontmatter_data['version']}
genesis_hash: {sample_frontmatter_data['genesis_hash']}
previous_hash: {sample_frontmatter_data['previous_hash']}
last_updated: {datetime.utcnow().isoformat()}Z
changelog:
  - "v1.0: Initial creation"
tags: ["security", "core"]
---
# Authentication Module

def login(username, password):
    pass

def logout(user_id):
    pass
"""
    file_path.write_text(content, encoding="utf-8")
    return str(file_path)


@pytest.fixture
def large_file(tmp_project):
    """Create a file with more than 50 lines for surgery testing"""
    src_dir = tmp_project / "src"
    src_dir.mkdir(exist_ok=True)
    
    file_path = src_dir / "large_module.py"
    lines = ["# Large module\n"] + [f"def function_{i}():\n    pass\n" for i in range(60)]
    file_path.write_text("".join(lines), encoding="utf-8")
    return str(file_path)


@pytest.fixture
def small_file(tmp_project):
    """Create a small file (<50 lines) for surgery testing"""
    src_dir = tmp_project / "src"
    src_dir.mkdir(exist_ok=True)
    
    file_path = src_dir / "small_module.py"
    file_path.write_text("# Small module\n\ndef hello():\n    pass\n", encoding="utf-8")
    return str(file_path)


@pytest.fixture
def log_manager(tmp_project):
    """Create RoleLogManager instance using temp project dir"""
    from role_logs import RoleLogManager
    return RoleLogManager(base_dir=str(tmp_project), project_name="test-project")


@pytest.fixture
def lock_manager(tmp_project):
    """Create FileLockManager instance using temp project dir"""
    from file_lock_manager import FileLockManager
    lock_mgr = FileLockManager(
        default_timeout=5.0,
        lock_dir=str(tmp_project / ".locks"),
        enable_deadlock_detection=True,
        max_wait_time=10.0,
        cleanup_interval=300.0
    )
    return lock_mgr


@pytest.fixture
def self_driving_engine(tmp_project):
    """Create SelfDrivingEngine instance using temp project dir"""
    from self_driving_engine import SelfDrivingEngine
    engine = SelfDrivingEngine(project_dir=str(tmp_project))
    return engine


@pytest.fixture
def managed_file_info(file_with_frontmatter):
    """Create a managed file info dict as returned by scan_managed_files"""
    from models import FileStatus, FileFrontmatter
    return {
        'path': file_with_frontmatter,
        'frontmatter': FileFrontmatter(
            id="TEST-001",
            module_name="test.auth",
            domain="Test Auth",
            status=FileStatus.DEV,
            role_owner="developer",
            genesis_hash="",
            previous_hash="",
            last_updated=datetime.utcnow().isoformat() + "Z"
        )
    }
