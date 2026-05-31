"""
models.py - Enhanced System State & Data Contract Definitions (V2.0)

Upgraded with professional-grade frontmatter fields:
- Module identity & domain classification
- Detailed capability description
- Design specification breakdown
- Technical source attribution
- Tripartite signature verification system
"""

from enum import Enum
from pydantic import BaseModel, Field
from typing import List, Optional, Literal, Dict, Any
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
        transitions = {
            FileStatus.DEV: [FileStatus.AUDIT, FileStatus.BLOCKED],
            FileStatus.AUDIT: [FileStatus.ACCEPT, FileStatus.DEV, FileStatus.BLOCKED],
            FileStatus.ACCEPT: [FileStatus.VERIFIED, FileStatus.DEV, FileStatus.BLOCKED],
            FileStatus.VERIFIED: [],
            FileStatus.BLOCKED: []
        }
        return transitions.get(self, [])


class RoleType(str, Enum):
    """Role type enumeration"""
    DEVELOPER = "developer"
    AUDITOR = "auditor"
    ACCEPTOR = "acceptor"
    DISPATCHER = "dispatcher"


class SignatureBlock(BaseModel):
    """Tripartite signature block for verification"""
    craftsman: str = Field(default="", description="Developer/Builder signature")
    craftsman_date: str = Field(default="", description="Craftsman signature timestamp")
    
    supervisor: str = Field(default="", description="Auditor/Supervisor signature")
    supervisor_date: str = Field(default="", description="Supervisor signature timestamp")
    
    acceptor: str = Field(default="", description="Acceptor/Final verifier signature")
    acceptor_date: str = Field(default="", description="Acceptor signature timestamp")
    
    def is_fully_signed(self) -> bool:
        return bool(self.craftsman and self.supervisor and self.acceptor)
    
    def get_signature_status(self) -> Dict[str, bool]:
        return {
            "craftman_signed": bool(self.craftsman),
            "supervisor_signed": bool(self.supervisor),
            "acceptor_signed": bool(self.acceptor),
            "fully_verified": self.is_fully_signed()
        }


class TechSource(BaseModel):
    """Technical source attribution entry"""
    name: str = Field(..., description="Source/library/tool name")
    version: Optional[str] = Field(None, description="Version if applicable")
    url: Optional[str] = Field(None, description="URL or reference")
    description: str = Field(default="", description="How this source is used")


class DesignSpec(BaseModel):
    """Design specification item for detailed capability breakdown"""
    number: int = Field(..., description="Sequential design point number")
    title: str = Field(..., description="Design component name")
    description: str = Field(default="", description="Detailed description")
    status: Literal["implemented", "partial", "planned", "deprecated"] = "planned"


class FileFrontmatterV2(BaseModel):
    """
    Professional-grade enhanced file frontmatter data contract (V2.0)
    
    Complete identity contract with:
    - Module classification & domain ownership
    - Capability description & design specs
    - Technical source chain of custody
    - Tripartite cryptographic signatures
    - Full audit trail support
    """
    
    # ===== Core Identity Fields =====
    id: str = Field(..., description="Global unique identifier (e.g., MODULE-001)")
    module_name: str = Field(..., description="Module/class/function name (e.g., crawler.engines.anti_detect)")
    domain: str = Field(..., description="Domain section (e.g., Crawler Engine - Anti-Detection Framework)")
    
    # ===== Status & Lifecycle =====
    status: FileStatus = Field(..., description="Lifecycle state (dev/audit/accept/verified/blocked)")
    role_owner: str = Field(..., description="Current responsible role")
    version: float = Field(default=1.0, description="Semantic version (auto-incremented)")
    priority: Literal["critical", "high", "medium", "low"] = "medium"
    
    # ===== Hash Chain Integrity =====
    genesis_hash: str = Field(default="", description="Initial hash at creation time")
    previous_hash: str = Field(default="", description="Previous content hash for tamper detection")
    current_hash: str = Field(default="", description="Current content hash (calculated on save)")
    
    # ===== Timestamps =====
    created_at: str = Field(
        default_factory=lambda: datetime.now().isoformat(),
        description="Creation timestamp (ISO8601)"
    )
    last_updated: str = Field(
        default_factory=lambda: datetime.now().isoformat(),
        description="Last modification timestamp (ISO8601)"
    )
    
    # ===== Capability Description =====
    description: str = Field(
        default="", 
        description="Detailed capability description (what this module does)"
    )
    capabilities: List[str] = Field(
        default_factory=list,
        description="List of specific capabilities/features"
    )
    
    # ===== Design Specifications =====
    design_version: str = Field(default="v1.0", description="Design document version")
    design_specs: List[DesignSpec] = Field(
        default_factory=list,
        description="Detailed breakdown of design points"
    )
    
    # ===== Technical Sources =====
    tech_sources: List[TechSource] = Field(
        default_factory=list,
        description="Technical references and source attributions"
    )
    
    # ===== Audit Trail =====
    changelog: List[str] = Field(
        default_factory=list,
        description="Change log entries with timestamps and reasons"
    )
    tags: List[str] = Field(
        default_factory=list,
        description="Semantic tags for search and categorization"
    )
    
    # ===== Dependencies =====
    dependencies: List[str] = Field(
        default_factory=list,
        description="List of module IDs this file depends on"
    )
    dependents: List[str] = Field(
        default_factory=list,
        description="List of module IDs that depend on this file"
    )
    
    # ===== Security & Compliance =====
    security_level: Literal["public", "internal", "confidential", "restricted"] = "internal"
    compliance_tags: List[str] = Field(
        default_factory=list,
        description="Compliance requirements (GDPR, SOC2, etc.)"
    )
    
    # ===== ★ Tripartite Signatures (Core Feature) =====
    signatures: SignatureBlock = Field(
        default_factory=SignatureBlock,
        description="Three-party signature block for verification"
    )
    
    # ===== Metadata =====
    author: str = Field(default="", description="Original author")
    reviewers: List[str] = Field(
        default_factory=list,
        description="List of reviewers who have examined this file"
    )
    metadata: Dict[str, Any] = Field(
        default_factory=dict,
        description="Extended metadata key-value store"
    )
    
    def calculate_content_hash(self, content: str) -> str:
        """Calculate SHA-256 hash of content"""
        return hashlib.sha256(content.encode('utf-8')).hexdigest()
    
    def update_version(self, increment: float = 0.1) -> float:
        """Auto-increment version number"""
        self.version = round(self.version + increment, 1)
        self.last_updated = datetime.now().isoformat()
        return self.version
    
    def add_changelog(self, entry: str, author: str = "system"):
        """Add changelog entry with author attribution"""
        version_str = f"v{self.version}"
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M')
        formatted_entry = f"[{timestamp}] @{author}: {entry}"
        self.changelog.append(f"{version_str}: {formatted_entry}")
    
    def sign_as_craftsman(self, signer_name: str):
        """Sign as developer/craftsman"""
        self.signatures.craftsman = signer_name
        self.signatures.craftsman_date = f"{signer_name} / {datetime.now().strftime('%Y-%m-%dT%H:%M')}"
    
    def sign_as_supervisor(self, signer_name: str):
        """Sign as auditor/supervisor"""
        self.signatures.supervisor = signer_name
        self.signatures.supervisor_date = f"{signer_name} / {datetime.now().strftime('%Y-%m-%dT%H:%M')} GMT+8"
    
    def sign_as_acceptor(self, signer_name: str):
        """Sign as final acceptor/verifier"""
        self.signatures.acceptor = signer_name
        self.signatures.acceptor_date = f"{signer_name} / {datetime.now().strftime('%Y-%m-%dT%H:%M')}"
    
    def generate_frontmatter_yaml(self) -> str:
        """Generate complete YAML frontmatter string in professional format"""
        lines = []
        lines.append("=" * 80)
        
        lines.append(f"Module: {self.module_name}")
        lines.append(f"Domain: {self.domain}")
        lines.append(f"Description: {self.description}")
        
        lines.append("")
        lines.append(f"Version: {self.design_version} | Status: {self.status.value} | Priority: {self.priority}")
        lines.append(f"ID: {self.id} | Owner: {self.role_owner} | v{self.version}")
        
        if self.capabilities:
            lines.append("")
            lines.append("Capabilities:")
            for cap in self.capabilities:
                lines.append(f"  - {cap}")
        
        if self.design_specs:
            lines.append("")
            lines.append(f"{self.design_version} Design Specifications:")
            for spec in self.design_specs:
                status_icon = {"implemented": "✅", "partial": "🔶", "planned": "⏳", "deprecated": "❌"}.get(spec.status, "❓")
                lines.append(f"  {spec.number}. [{status_icon}] {spec.title}")
                if spec.description:
                    lines.append(f"     {spec.description}")
        
        if self.tech_sources:
            lines.append("")
            lines.append("Technical Sources:")
            for src in self.tech_sources:
                ver_str = f" (v{src.version})" if src.version else ""
                url_str = f" - {src.url}" if src.url else ""
                lines.append(f"  - {src.name}{ver_str}{url_str}")
                if src.description:
                    lines.append(f"    → {src.description}")
        
        lines.append("")
        lines.append("Tripartite Signatures:")
        lines.append(f"  Craftsman (Developer):   {self.signatures.craftsman or 'Pending...'} ({self.signatures.craftsman_date or 'Not signed'})")
        lines.append(f"  Supervisor (Auditor):    {self.signatures.supervisor or 'Pending...'} ({self.signatures.supervisor_date or 'Not signed'})")
        lines.append(f"  Acceptor (Verifier):     {self.signatures.acceptor or 'Pending...'} ({self.signatures.acceptor_date or 'Not signed'})")
        
        if self.changelog:
            lines.append("")
            lines.append("Recent Changelog (last 3):")
            for entry in self.changelog[-3:]:
                lines.append(f"  • {entry}")
        
        lines.append("")
        lines.append(f"Last Updated: {self.last_updated}")
        lines.append(f"Security Level: {self.security_level}")
        lines.append("=" * 80)
        
        return "\n".join(lines)


# ===== Backward Compatibility Alias =====
FileFrontmatter = FileFrontmatterV2


class RCAReport(BaseModel):
    """Root cause analysis report data contract"""
    rca_type: Literal["root_cause_analysis"] = "root_cause_analysis"
    symptom: str = Field(..., description="Error symptom")
    trigger_chain: str = Field(..., description="Trigger chain")
    root_cause: str = Field(..., description="Root cause")
    fix_plan: str = Field(..., description="Fix plan")
    affected_files: List[str] = Field(..., description="Affected files list")
    
    def validate(self) -> bool:
        required_fields = ['symptom', 'trigger_chain', 'root_cause', 'fix_plan', 'affected_files']
        return all(getattr(self, field) for field in required_fields)


class IssueTicket(BaseModel):
    """Issue ticket data model"""
    id: str
    file_path: str
    issue_type: str
    severity: str
    description: str
    created_by: str
    status: str = "open"
    created_at: str = Field(default_factory=lambda: datetime.now().isoformat())


class ErrorFingerprint(BaseModel):
    """Error fingerprint model"""
    fingerprint: str
    error_type: str
    file_path: str
    keywords: str
    count: int = 0
    first_occurred: str = Field(default_factory=lambda: datetime.now().isoformat())
    last_occurred: str = Field(default_factory=lambda: datetime.now().isoformat())


if __name__ == "__main__":
    print("✅ Enhanced Models V2.0 Test")
    
    # Test new enhanced frontmatter
    fm = FileFrontmatterV2(
        id="CRAWLER-001",
        module_name="crawler.engines.anti_detect",
        domain="Self-developed Crawler Engine - Anti-Detection Framework",
        status=FileStatus.DEV,
        role_owner="developer",
        description="Unified anti-crawling / anti-detection / risk control bypass infrastructure"
    )
    
    fm.capabilities = [
        "Browser launch parameter factory",
        "Stealth injection (3-layer)",
        "Fingerprint spoofing",
        "Human behavior simulation",
        "Adaptive rate limiting",
        "Captcha/risk handler",
        "Resource blocker"
    ]
    
    fm.design_specs = [
        DesignSpec(number=1, title="BrowserLaunchConfig", description="Factory for browser startup params", status="implemented"),
        DesignSpec(number=2, title="StealthInjector", description="playwright-stealth + CDP + init_script", status="implemented"),
        DesignSpec(number=3, title="FingerprintSpoofer", description="Random UA/viewport/timezone/language", status="implemented"),
    ]
    
    fm.tech_sources = [
        TechSource(name="playwright-stealth", version="18-dimensions", description="Anti-detection base"),
        TechSource(name="CDP Page.addScriptToEvaluateOnNewDocument", description="Page-level injection"),
        TechSource(name="bot.sannysoft.com", url="https://bot.sannysoft.com", description="Detection analysis reference"),
    ]
    
    # Simulate lifecycle
    fm.sign_as_craftsman("HenryChow")
    fm.add_changelog("Initial implementation complete", "developer")
    
    fm.update_version()  # 1.0 -> 1.1
    
    fm.sign_as_supervisor("QClaw")
    fm.add_changelog("Audit passed, code quality verified", "auditor")
    
    fm.update_version()  # 1.1 -> 1.2
    
    fm.sign_as_acceptor("SystemAuto")
    fm.add_changelog("All tests passed, promoted to production", "acceptor")
    
    # Generate output
    print("\n" + fm.generate_frontmatter_yaml())
    
    print(f"\n✅ Signature Status: {fm.signatures.get_signature_status()}")
    print(f"✅ Fully Signed: {fm.signatures.is_fully_signed()}")
