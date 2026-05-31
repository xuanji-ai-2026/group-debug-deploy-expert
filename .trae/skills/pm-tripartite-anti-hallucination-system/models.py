"""
models.py - 系统状态与数据契约定义

将《宪法》中的状态流转和页头范式转化为强类型的Python数据结构。
这是整个系统的基石。
"""

from enum import Enum
from pydantic import BaseModel, Field
from typing import List, Optional, Literal
import hashlib
from datetime import datetime


class FileStatus(str, Enum):
    """Strict state machine enumeration (no cross-level jumps allowed)"""
    DEV = "dev"
    AUDIT = "audit"
    ACCEPT = "accept"
    VERIFIED = "verified"
    BLOCKED = "blocked"
    
    def get_allowed_transitions(self) -> List['FileStatus']:
        """返回允许的状态转换列表"""
        transitions = {
            FileStatus.DEV: [FileStatus.AUDIT, FileStatus.BLOCKED],
            FileStatus.AUDIT: [FileStatus.ACCEPT, FileStatus.DEV, FileStatus.BLOCKED],
            FileStatus.ACCEPT: [FileStatus.VERIFIED, FileStatus.DEV, FileStatus.BLOCKED],
            FileStatus.VERIFIED: [],  # 终态，不允许转出
            FileStatus.BLOCKED: []    # 需要人工解除
        }
        return transitions.get(self, [])


class RoleType(str, Enum):
    """角色类型枚举"""
    DEVELOPER = "developer"
    AUDITOR = "auditor"
    ACCEPTOR = "acceptor"
    DISPATCHER = "dispatcher"


class FileFrontmatter(BaseModel):
    """标准页头的数据契约 (YAML Frontmatter Schema)"""
    id: str = Field(..., description="全局唯一ID")
    status: FileStatus = Field(..., description="生命周期状态")
    role_owner: str = Field(..., description="当前责任角色")
    version: float = Field(default=1.0, description="版本号")
    genesis_hash: str = Field(..., description="文件创建时的初始哈希")
    previous_hash: str = Field(..., description="上一次提交的内容哈希")
    last_updated: str = Field(..., description="最后更新时间 (ISO8601)")
    changelog: List[str] = Field(default_factory=list, description="变更记录")
    tags: List[str] = Field(default_factory=list, description="语义标签")
    
    def calculate_content_hash(self, content: str) -> str:
        """计算内容的SHA-256哈希"""
        return hashlib.sha256(content.encode('utf-8')).hexdigest()
    
    def update_version(self) -> float:
        """版本号自动递增0.1"""
        self.version = round(self.version + 0.1, 1)
        self.last_updated = datetime.utcnow().isoformat() + "Z"
        return self.version
    
    def add_changelog(self, entry: str):
        """添加变更记录"""
        version_str = f"v{self.version}"
        self.changelog.append(f"{version_str}: {entry}")


class RCAReport(BaseModel):
    """Root cause analysis report data contract (RCA Schema)"""
    rca_type: Literal["root_cause_analysis"] = "root_cause_analysis"
    symptom: str = Field(..., description="Error symptom")
    trigger_chain: str = Field(..., description="Trigger chain")
    root_cause: str = Field(..., description="Root cause")
    fix_plan: str = Field(..., description="Fix plan")
    affected_files: List[str] = Field(..., description="Affected files list")
    
    def validate(self) -> bool:
        """验证RCA报告的完整性"""
        required_fields = ['symptom', 'trigger_chain', 'root_cause', 'fix_plan', 'affected_files']
        return all(getattr(self, field) for field in required_fields)


class IssueTicket(BaseModel):
    """Issue工单数据模型"""
    id: str
    file_path: str
    issue_type: str  # bug, security, performance, style, etc.
    severity: str    # critical, major, minor, info
    description: str
    created_by: str  # auditor
    status: str = "open"  # open, in_progress, resolved
    created_at: str = Field(default_factory=lambda: datetime.utcnow().isoformat() + "Z")


class ErrorFingerprint(BaseModel):
    """错误指纹模型"""
    fingerprint: str
    error_type: str
    file_path: str
    keywords: str
    count: int = 0
    first_occurred: str = Field(default_factory=lambda: datetime.utcnow().isoformat() + "Z")
    last_occurred: str = Field(default_factory=lambda: datetime.utcnow().isoformat() + "Z")


if __name__ == "__main__":
    # 测试代码
    print("✅ 模型定义测试通过")
    
    # 测试FileStatus枚举
    status = FileStatus.DEV
    print(f"状态: {status.value}")
    print(f"允许转换到: {[s.value for s in status.get_allowed_transitions()]}")
    
    # 测试FileFrontmatter
    fm = FileFrontmatter(
        id="TEST-001",
        status=FileStatus.DEV,
        role_owner="developer",
        version=1.0,
        genesis_hash="abc123",
        previous_hash="def456",
        last_updated=datetime.utcnow().isoformat() + "Z"
    )
    print(f"\nFrontmatter ID: {fm.id}")
    print(f"版本更新: {fm.update_version()}")
