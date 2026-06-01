"""
test_models.py - Unit tests for data models (models.py / models_v2.py)

Tests cover:
- FileStatus enum and state transitions
- RoleType enum
- FileFrontmatterV2 data model (from models_v2.py)
- SignatureBlock model
- TechSource, DesignSpec models
- RCAReport data model
- IssueTicket data model
- ErrorFingerprint data model
- Edge cases: empty inputs, invalid values, boundary conditions
"""

import pytest
import hashlib
from datetime import datetime

import sys
import os
sys.path.insert(0, str(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from models import (
    FileStatus, RoleType,
    RCAReport, IssueTicket, ErrorFingerprint
)
from models_v2 import (
    FileFrontmatterV2, SignatureBlock, TechSource, DesignSpec
)


# ============================================================
# FileStatus Enum Tests
# ============================================================

class TestFileStatusEnum:
    """Test FileStatus enum definition and transitions"""

    def test_all_status_values_exist(self):
        assert FileStatus.DEV.value == "dev"
        assert FileStatus.AUDIT.value == "audit"
        assert FileStatus.ACCEPT.value == "accept"
        assert FileStatus.VERIFIED.value == "verified"
        assert FileStatus.BLOCKED.value == "blocked"

    def test_dev_transitions(self):
        transitions = FileStatus.DEV.get_allowed_transitions()
        assert FileStatus.AUDIT in transitions
        assert FileStatus.BLOCKED in transitions
        assert FileStatus.VERIFIED not in transitions
        assert FileStatus.ACCEPT not in transitions

    def test_audit_transitions(self):
        transitions = FileStatus.AUDIT.get_allowed_transitions()
        assert FileStatus.ACCEPT in transitions
        assert FileStatus.DEV in transitions
        assert FileStatus.BLOCKED in transitions
        assert FileStatus.VERIFIED not in transitions

    def test_accept_transitions(self):
        transitions = FileStatus.ACCEPT.get_allowed_transitions()
        assert FileStatus.VERIFIED in transitions
        assert FileStatus.DEV in transitions
        assert FileStatus.BLOCKED in transitions

    def test_verified_is_terminal(self):
        assert FileStatus.VERIFIED.get_allowed_transitions() == []

    def test_blocked_is_terminal(self):
        assert FileStatus.BLOCKED.get_allowed_transitions() == []

    def test_no_cross_level_jumps(self):
        dev_transitions = FileStatus.DEV.get_allowed_transitions()
        assert FileStatus.VERIFIED not in dev_transitions
        assert FileStatus.ACCEPT not in dev_transitions


# ============================================================
# RoleType Enum Tests
# ============================================================

class TestRoleTypeEnum:
    """Test RoleType enum"""

    def test_all_role_values_exist(self):
        assert RoleType.DEVELOPER.value == "developer"
        assert RoleType.AUDITOR.value == "auditor"
        assert RoleType.ACCEPTOR.value == "acceptor"
        assert RoleType.DISPATCHER.value == "dispatcher"

    def test_role_count(self):
        assert len(list(RoleType)) == 4


# ============================================================
# SignatureBlock Model Tests
# ============================================================

class TestSignatureBlock:
    """Test SignatureBlock model"""

    def test_default_empty_signature(self):
        block = SignatureBlock()
        assert block.craftsman == ""
        assert block.supervisor == ""
        assert block.acceptor == ""

    def test_not_fully_signed_by_default(self):
        block = SignatureBlock()
        assert not block.is_fully_signed()

    def test_partial_signing_not_fully_signed(self):
        block = SignatureBlock(craftsman="dev1", supervisor="audit1")
        assert not block.is_fully_signed()

    def test_fully_signed_when_all_present(self):
        block = SignatureBlock(craftsman="dev1", supervisor="audit1", acceptor="acc1")
        assert block.is_fully_signed()

    def test_get_signature_status_all_unsigned(self):
        block = SignatureBlock()
        status = block.get_signature_status()
        assert status["craftman_signed"] is False
        assert status["supervisor_signed"] is False
        assert status["acceptor_signed"] is False
        assert status["fully_verified"] is False

    def test_get_signature_status_mixed(self):
        block = SignatureBlock(craftsman="dev1")
        status = block.get_signature_status()
        assert status["craftman_signed"] is True
        assert status["supervisor_signed"] is False
        assert status["fully_verified"] is False


# ============================================================
# TechSource & DesignSpec Model Tests
# ============================================================

class TestTechSource:
    """Test TechSource model"""

    def test_minimal_creation(self):
        src = TechSource(name="pytest", description="Testing framework")
        assert src.name == "pytest"
        assert src.version is None
        assert src.url is None

    def test_full_creation(self):
        src = TechSource(
            name="pydantic",
            version="2.0",
            url="https://docs.pydantic.dev/",
            description="Data validation library"
        )
        assert src.version == "2.0"
        assert src.url == "https://docs.pydantic.dev/"


class TestDesignSpec:
    """Test DesignSpec model"""

    def test_default_status_is_planned(self):
        spec = DesignSpec(number=1, title="Auth System")
        assert spec.status == "planned"

    def test_all_status_values(self):
        for status in ["implemented", "partial", "planned", "deprecated"]:
            spec = DesignSpec(number=1, title="Test", status=status)
            assert spec.status == status

    def test_invalid_status_raises_error(self):
        with pytest.raises(Exception):
            DesignSpec(number=1, title="Test", status="invalid_status")


# ============================================================
# FileFrontmatterV2 Tests
# ============================================================

class TestFileFrontmatterV2:
    """Test the enhanced V2 frontmatter model"""

    def test_minimal_required_fields(self):
        fm = FileFrontmatterV2(
            id="MOD-001",
            module_name="test.module",
            domain="Test Domain",
            status=FileStatus.DEV,
            role_owner="developer"
        )
        assert fm.id == "MOD-001"
        assert fm.status == FileStatus.DEV
        assert fm.version == 1.0
        assert fm.capabilities == []
        assert fm.tags == []
        assert fm.changelog == []

    def test_default_timestamps_are_generated(self):
        fm = FileFrontmatterV2(
            id="MOD-002",
            module_name="test.mod",
            domain="Domain",
            status=FileStatus.DEV,
            role_owner="developer"
        )
        assert fm.created_at != ""
        assert fm.last_updated != ""

    def test_calculate_content_hash(self):
        fm = FileFrontmatterV2(
            id="MOD-003",
            module_name="test.mod",
            domain="Domain",
            status=FileStatus.DEV,
            role_owner="developer"
        )
        content = "hello world"
        expected_hash = hashlib.sha256(content.encode('utf-8')).hexdigest()
        assert fm.calculate_content_hash(content) == expected_hash

    def test_calculate_content_hash_deterministic(self):
        fm = FileFrontmatterV2(
            id="MOD-004",
            module_name="test.mod",
            domain="Domain",
            status=FileStatus.DEV,
            role_owner="developer"
        )
        content = "same content twice"
        h1 = fm.calculate_content_hash(content)
        h2 = fm.calculate_content_hash(content)
        assert h1 == h2

    def test_update_version_default_increment(self):
        fm = FileFrontmatterV2(
            id="MOD-005",
            module_name="test.mod",
            domain="Domain",
            status=FileStatus.DEV,
            role_owner="developer",
            version=1.0
        )
        new_ver = fm.update_version()
        assert new_ver == 1.1
        assert fm.version == 1.1

    def test_update_version_custom_increment(self):
        fm = FileFrontmatterV2(
            id="MOD-006",
            module_name="test.mod",
            domain="Domain",
            status=FileStatus.DEV,
            role_owner="developer",
            version=2.0
        )
        new_ver = fm.update_version(increment=0.5)
        assert new_ver == 2.5

    def test_add_changelog(self):
        fm = FileFrontmatterV2(
            id="MOD-007",
            module_name="test.mod",
            domain="Domain",
            status=FileStatus.DEV,
            role_owner="developer",
            version=1.0
        )
        fm.add_changelog("Fixed bug in auth")
        assert len(fm.changelog) == 1
        assert "v1.0" in fm.changelog[0]
        assert "Fixed bug" in fm.changelog[0]

    def test_add_changelog_with_author(self):
        fm = FileFrontmatterV2(
            id="MOD-008",
            module_name="test.mod",
            domain="Domain",
            status=FileStatus.DEV,
            role_owner="developer",
            version=1.5
        )
        fm.add_changelog("Security fix applied", author="auditor")
        assert "@auditor" in fm.changelog[0]

    def test_sign_as_craftsman(self):
        fm = FileFrontmatterV2(
            id="MOD-009",
            module_name="test.mod",
            domain="Domain",
            status=FileStatus.DEV,
            role_owner="developer"
        )
        fm.sign_as_craftsman("Developer-Alice")
        assert fm.signatures.craftsman == "Developer-Alice"
        assert "Developer-Alice" in fm.signatures.craftsman_date

    def test_sign_as_supervisor(self):
        fm = FileFrontmatterV2(
            id="MOD-010",
            module_name="test.mod",
            domain="Domain",
            status=FileStatus.AUDIT,
            role_owner="auditor"
        )
        fm.sign_as_supervisor("Auditor-Bob")
        assert fm.signatures.supervisor == "Auditor-Bob"

    def test_sign_as_acceptor(self):
        fm = FileFrontmatterV2(
            id="MOD-011",
            module_name="test.mod",
            domain="Domain",
            status=FileStatus.ACCEPT,
            role_owner="acceptor"
        )
        fm.sign_as_acceptor("Acceptor-Carol")
        assert fm.signatures.acceptor == "Acceptor-Carol"

    def test_generate_frontmatter_yaml_basic(self):
        fm = FileFrontmatterV2(
            id="AUTH-001",
            module_name="auth.core.login",
            domain="Authentication - Core Login Module",
            status=FileStatus.DEV,
            role_owner="developer",
            description="Handles user login and session management"
        )
        output = fm.generate_frontmatter_yaml()
        assert "AUTH-001" in output
        assert "auth.core.login" in output
        assert "dev" in output

    def test_generate_frontmatter_yaml_with_capabilities(self):
        fm = FileFrontmatterV2(
            id="API-001",
            module_name="api.rest.v1",
            domain="REST API v1",
            status=FileStatus.DEV,
            role_owner="developer",
            capabilities=["GET /users", "POST /login", "DELETE /session"]
        )
        output = fm.generate_frontmatter_yaml()
        assert "Capabilities:" in output
        assert "GET /users" in output

    def test_generate_frontmatter_yaml_with_design_specs(self):
        fm = FileFrontmatterV2(
            id="CRAWLER-001",
            module_name="crawler.engine",
            domain="Crawler Engine",
            status=FileStatus.DEV,
            role_owner="developer",
            design_specs=[
                DesignSpec(number=1, title="URL Parser", status="implemented"),
                DesignSpec(number=2, title="Rate Limiter", status="planned"),
            ]
        )
        output = fm.generate_frontmatter_yaml()
        assert "Design Specifications" in output
        assert "URL Parser" in output

    def test_generate_frontmatter_yaml_with_tech_sources(self):
        fm = FileFrontmatterV2(
            id="TOOL-001",
            module_name="tool.scraping",
            domain="Scraping Tools",
            status=FileStatus.DEV,
            role_owner="developer",
            tech_sources=[
                TechSource(name="requests", version="2.31"),
                TechSource(name="beautifulsoup4", url="https://pypi.org/"),
            ]
        )
        output = fm.generate_frontmatter_yaml()
        assert "Technical Sources" in output
        assert "requests" in output

    def test_generate_frontmatter_yaml_with_signatures(self):
        fm = FileFrontmatterV2(
            id="SIG-001",
            module_name="sig.test",
            domain="Signature Test",
            status=FileStatus.VERIFIED,
            role_owner="system"
        )
        fm.sign_as_craftsman("Dev-A")
        fm.sign_as_supervisor("Audit-B")
        fm.sign_as_acceptor("Accept-C")
        output = fm.generate_frontmatter_yaml()
        assert "Tripartite Signatures" in output
        assert "Dev-A" in output
        assert "Audit-B" in output
        assert "Accept-C" in output

    def test_generate_frontmatter_yaml_shows_recent_changelog(self):
        fm = FileFrontmatterV2(
            id="CL-001",
            module_name="cl.test",
            domain="Changelog Test",
            status=FileStatus.DEV,
            role_owner="developer"
        )
        fm.add_changelog("First change")
        fm.add_changelog("Second change")
        fm.add_changelog("Third change")
        fm.add_changelog("Fourth change")
        output = fm.generate_frontmatter_yaml()
        assert "Recent Changelog" in output or "Changelog" in output

    def test_security_level_defaults(self):
        fm = FileFrontmatterV2(
            id="SEC-001",
            module_name="sec.test",
            domain="Security Test",
            status=FileStatus.DEV,
            role_owner="developer"
        )
        assert fm.security_level == "internal"

    def test_metadata_dict(self):
        fm = FileFrontmatterV2(
            id="META-001",
            module_name="meta.test",
            domain="Meta Test",
            status=FileStatus.DEV,
            role_owner="developer",
            metadata={"custom_key": "custom_value"}
        )
        assert fm.metadata["custom_key"] == "custom_value"

    def test_dependencies_and_dependents_lists(self):
        fm = FileFrontmatterV2(
            id="DEP-001",
            module_name="dep.test",
            domain="Dependency Test",
            status=FileStatus.DEV,
            role_owner="developer",
            dependencies=["MOD-001", "MOD-002"],
            dependents=["MOD-100"]
        )
        assert len(fm.dependencies) == 2
        assert len(fm.dependents) == 1


# ============================================================
# RCAReport Model Tests
# ============================================================

class TestRCAReport:
    """Test Root Cause Analysis Report model"""

    def test_valid_rca_report_creation(self, sample_rca_report_data):
        report = RCAReport(**sample_rca_report_data)
        assert report.rca_type == "root_cause_analysis"
        assert report.symptom == sample_rca_report_data["symptom"]
        assert report.root_cause == sample_rca_report_data["root_cause"]

    def test_rca_type_is_const(self):
        report = RCAReport(
            symptom="error",
            trigger_chain="chain",
            root_cause="cause",
            fix_plan="plan",
            affected_files=["file.py"]
        )
        assert report.rca_type == "root_cause_analysis"

    def test_validate_returns_true_for_complete_report(self, sample_rca_report):
        assert sample_rca_report.validate() is True

    def test_affected_files_list(self, sample_rca_report):
        assert isinstance(sample_rca_report.affected_files, list)
        assert len(sample_rca_report.affected_files) >= 1

    def test_rca_with_multiple_affected_files(self):
        report = RCAReport(
            symptom="Import error",
            trigger_chain="Module load -> Import -> Fail",
            root_cause="Missing __init__.py in package",
            fix_plan="Add __init__.py file",
            affected_files=["src/pkg/__init__.py", "src/main.py", "tests/test_pkg.py"]
        )
        assert len(report.affected_files) == 3
        assert report.validate() is True

    def test_rca_symptom_can_be_long(self):
        long_symptom = "A" * 500
        report = RCAReport(
            symptom=long_symptom,
            trigger_chain="chain",
            root_cause="cause",
            fix_plan="fix",
            affected_files=["f.py"]
        )
        assert len(report.symptom) == 500

    def test_rca_trigger_chain_format(self):
        report = RCAReport(
            symptom="timeout",
            trigger_chain="Request -> API -> DB -> Timeout",
            root_cause="DB connection pool exhausted",
            fix_plan="Increase pool size",
            affected_files=["db.py"]
        )
        assert "->" in report.trigger_chain


class TestRCAReportEdgeCases:
    """Edge case tests for RCAReport"""

    def test_empty_strings_in_fields(self):
        report = RCAReport(
            symptom="",
            trigger_chain="",
            root_cause="",
            fix_plan="",
            affected_files=[]
        )
        assert report.validate() is False

    def test_unicode_content(self):
        report = RCAReport(
            symptom="用户登录失败: 中文错误信息",
            trigger_chain="用户点击 -> 发送请求 -> 返回500错误",
            root_cause="数据库编码不匹配",
            fix_plan="设置UTF-8编码",
            affected_files=["中文路径/文件.py"]
        )

    def test_special_characters_in_fix_plan(self):
        report = RCAReport(
            symptom="SQL injection",
            trigger_chain="Input -> Query -> Execute",
            root_cause="Unsanitized user input",
            fix_plan='Use parameterized queries: db.execute("SELECT * FROM users WHERE id=?", [user_id])',
            affected_files=["query.py"]
        )
        assert "?" in report.fix_plan


# ============================================================
# IssueTicket Model Tests
# ============================================================

class TestIssueTicket:
    """Test Issue Ticket data model"""

    def test_ticket_creation(self, sample_issue_ticket_data):
        ticket = IssueTicket(**sample_issue_ticket_data)
        assert ticket.id == sample_issue_ticket_data["id"]
        assert ticket.issue_type == "bug"
        assert ticket.severity == "major"
        assert ticket.status == "open"

    def test_default_status_is_open(self):
        ticket = IssueTicket(
            id="ISSUE-001",
            file_path="/path/to/file.py",
            issue_type="security",
            severity="critical",
            description="SQL injection vulnerability",
            created_by="auditor"
        )
        assert ticket.status == "open"

    def test_created_at_auto_generated(self):
        ticket = IssueTicket(
            id="ISSUE-TIME-001",
            file_path="f.py",
            issue_type="style",
            severity="minor",
            description="Line too long",
            created_by="auditor"
        )
        assert ticket.created_at != ""
        assert "T" in ticket.created_at or "-" in ticket.created_at

    def test_all_severity_levels(self):
        for sev in ["critical", "major", "minor", "info"]:
            ticket = IssueTicket(
                id=f"SEV-{sev}",
                file_path="f.py",
                issue_type="bug",
                severity=sev,
                description=f"{sev} issue",
                created_by="auditor"
            )
            assert ticket.severity == sev

    def test_all_issue_types(self):
        for itype in ["bug", "security", "performance", "style"]:
            ticket = IssueTicket(
                id=f"TYPE-{itype}",
                file_path="f.py",
                issue_type=itype,
                severity="major",
                description=f"{itype} issue found",
                created_by="auditor"
            )
            assert ticket.issue_type == itype


class TestIssueTicketEdgeCases:
    """Edge case tests for IssueTicket"""

    def test_very_long_description(self):
        desc = "X" * 10000
        ticket = IssueTicket(
            id="LONG-DESC",
            file_path="f.py",
            issue_type="bug",
            severity="major",
            description=desc,
            created_by="auditor"
        )
        assert len(ticket.description) == 10000

    def test_file_path_with_special_chars(self):
        ticket = IssueTicket(
            id="PATH-001",
            file_path="C:/Users/开发者/project/src/模块/文件.py",
            issue_type="bug",
            severity="major",
            description="Path with unicode",
            created_by="auditor"
        )
        assert "开发者" in ticket.file_path


# ============================================================
# ErrorFingerprint Model Tests
# ============================================================

class TestErrorFingerprint:
    """Test Error Fingerprint data model"""

    def test_fingerprint_creation(self, sample_error_fingerprint_data):
        fp = ErrorFingerprint(**sample_error_fingerprint_data)
        assert fp.fingerprint == sample_error_fingerprint_data["fingerprint"]
        assert fp.error_type == "TypeError"
        assert fp.count == 0

    def test_count_starts_at_zero(self):
        fp = ErrorFingerprint(
            fingerprint="abc123",
            error_type="ValueError",
            file_path="file.py",
            keywords="type mismatch"
        )
        assert fp.count == 0

    def test_auto_timestamps_generated(self):
        fp = ErrorFingerprint(
            fingerprint="fp1",
            error_type="RuntimeError",
            file_path="script.js",
            keywords="runtime error"
        )
        assert fp.first_occurred != ""
        assert fp.last_occurred != ""


class TestErrorFingerprintEdgeCases:
    """Edge case tests for ErrorFingerprint"""

    def test_count_increment_simulation(self):
        fp = ErrorFingerprint(
            fingerprint="inc-test",
            error_type="Error",
            file_path="f.py",
            keywords="test"
        )
        fp.count += 1
        assert fp.count == 1
        fp.count += 1
        assert fp.count == 2

    def test_fingerprint_hash_length(self):
        fp = ErrorFingerprint(
            fingerprint=hashlib.md5(b"test").hexdigest(),
            error_type="Error",
            file_path="f.py",
            keywords="kw"
        )
        assert len(fp.fingerprint) == 32

    def test_various_error_types(self):
        error_types = [
            "TypeError", "ValueError", "KeyError", "AttributeError",
            "ImportError", "RuntimeError", "ConnectionError", "TimeoutError"
        ]
        for etype in error_types:
            fp = ErrorFingerprint(
                fingerprint="fp",
                error_type=etype,
                file_path="f.py",
                keywords="kw"
            )
            assert fp.error_type == etype


# ============================================================
# Integration / Cross-model Tests
# ============================================================

class TestModelIntegration:
    """Integration tests across multiple models"""

    def test_frontmatter_to_rca_workflow(self):
        fm = FileFrontmatterV2(
            id="WORKFLOW-001",
            module_name="workflow.test",
            domain="Workflow Test",
            status=FileStatus.DEV,
            role_owner="developer"
        )
        
        rca = RCAReport(
            symptom="Null pointer in auth",
            trigger_chain="Login -> getUser -> Null",
            root_cause="Missing null check",
            fix_plan="Add if guard",
            affected_files=[fm.module_name]
        )
        assert rca.validate() is True
        assert fm.module_name in rca.affected_files[0]

    def test_rca_to_issue_ticket_workflow(self, sample_rca_report):
        ticket = IssueTicket(
            id="WORKFLOW-ISSUE",
            file_path=sample_rca_report.affected_files[0],
            issue_type="bug",
            severity="major",
            description=f"RCA: {sample_rca_report.symptom} | Fix: {sample_rca_report.fix_plan}",
            created_by="auditor"
        )
        assert sample_rca_report.symptom in ticket.description
        assert sample_rca_report.affected_files[0] == ticket.file_path

    def test_complete_lifecycle_models(self):
        fm = FileFrontmatterV2(
            id="LIFECYCLE-001",
            module_name="lifecycle.auth",
            domain="Full Lifecycle Demo",
            status=FileStatus.DEV,
            role_owner="developer"
        )
        fm.add_changelog("Initial creation by developer", "developer")
        fm.update_version()
        fm.sign_as_craftsman("Dev-Alice")
        
        assert fm.status == FileStatus.DEV
        assert len(fm.changelog) >= 1
        
        rca = RCAReport(
            symptom="Auth token expired early",
            trigger_chain="Login -> Generate Token -> Expire",
            root_cause="TTL set to 0 in config",
            fix_plan="Set TTL to 3600",
            affected_files=["lifecycle.auth"]
        )
        assert rca.validate()
        
        ticket = IssueTicket(
            id="LIFECYCLE-ISSUE-001",
            file_path="src/lifecycle/auth.py",
            issue_type="security",
            severity="critical",
            description=rca.symptom,
            created_by="auditor"
        )
        assert ticket.severity == "critical"


# ============================================================
# Boundary & Invalid Input Tests
# ============================================================

class TestModelBoundaryConditions:
    """Boundary condition tests for all models"""

    def test_rca_missing_required_field(self):
        with pytest.raises(Exception):
            RCAReport(
                symptom="error",
                trigger_chain="chain",
                root_cause="cause",
                fix_plan="plan"
            )

    def test_issue_ticket_missing_required_field(self):
        with pytest.raises(Exception):
            IssueTicket(
                id="TICKET-001",
                file_path="file.py",
                issue_type="bug",
                severity="major"
            )

    def test_error_fingerprint_missing_required_field(self):
        with pytest.raises(Exception):
            ErrorFingerprint(
                fingerprint="fp",
                error_type="err",
                file_path="path"
            )

    def test_version_negative(self):
        fm = FileFrontmatterV2(
            id="NEG-VER",
            module_name="neg.ver",
            domain="Domain",
            status=FileStatus.DEV,
            role_owner="developer",
            version=-1.0
        )
        new_ver = fm.update_version()
        assert new_ver == -0.9

    def test_large_changelog_accumulation(self):
        fm = FileFrontmatterV2(
            id="LARGE-CL",
            module_name="large.cl",
            domain="Domain",
            status=FileStatus.DEV,
            role_owner="developer"
        )
        for i in range(100):
            fm.add_changelog(f"Change number {i}")
        assert len(fm.changelog) == 100

    def test_design_spec_high_number(self):
        spec = DesignSpec(number=99999, title="High number spec")
        assert spec.number == 99999

    def test_tech_source_empty_description(self):
        src = TechSource(name="lib")
        assert src.description == ""
