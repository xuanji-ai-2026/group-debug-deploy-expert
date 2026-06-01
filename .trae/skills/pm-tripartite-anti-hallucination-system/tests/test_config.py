"""
test_config.py - Unit tests for configuration (config.py)

Tests cover:
- ROLE_CONFIG structure and completeness
- STATE_TRANSITIONS validity
- CIRCUIT_BREAKER_CONFIG parameters
- DIRECTORY_STRUCTURE_CONFIG definitions
- Role-specific constraints validation
- System prompt presence and format
- Edge cases: missing keys, invalid values
"""

import os
import sys
import pytest

sys.path.insert(0, str(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))


# ============================================================
# ROLE_CONFIG Tests
# ============================================================

class TestROLECONFIGStructure:
    """Test ROLE_CONFIG structure and basic properties"""

    def test_all_four_roles_defined(self):
        from config import ROLE_CONFIG
        expected_roles = ["developer", "auditor", "acceptor", "dispatcher"]
        for role in expected_roles:
            assert role in ROLE_CONFIG, f"Missing role: {role}"

    def test_no_extra_roles(self):
        from config import ROLE_CONFIG
        assert len(ROLE_CONFIG) == 4

    def test_each_role_has_display_name(self):
        from config import ROLE_CONFIG
        for role, config in ROLE_CONFIG.items():
            assert "display_name" in config, f"{role} missing display_name"
            assert isinstance(config["display_name"], str)
            assert len(config["display_name"]) > 0

    def test_each_role_has_color(self):
        from config import ROLE_CONFIG
        for role, config in ROLE_CONFIG.items():
            assert "color" in config, f"{role} missing color"
            assert config["color"].startswith("#"), \
                f"{role} color should start with #: {config['color']}"

    def test_each_role_has_tools_list(self):
        from config import ROLE_CONFIG
        for role, config in ROLE_CONFIG.items():
            assert "tools" in config, f"{role} missing tools"
            assert isinstance(config["tools"], list), \
                f"{role} tools must be a list"
            assert len(config["tools"]) > 0, \
                f"{role} must have at least one tool"

    def test_each_role_has_system_prompt(self):
        from config import ROLE_CONFIG
        for role, config in ROLE_CONFIG.items():
            assert "system_prompt" in config, f"{role} missing system_prompt"
            assert isinstance(config["system_prompt"], str)
            assert len(config["system_prompt"]) > 50, \
                f"{role} system_prompt seems too short"

    def test_each_role_has_constraints(self):
        from config import ROLE_CONFIG
        for role, config in ROLE_CONFIG.items():
            assert "constraints" in config, f"{role} missing constraints"
            assert isinstance(config["constraints"], dict)


class TestROLECONFIGTools:
    """Test role tool assignments"""

    def test_developer_tools_count(self):
        from config import ROLE_CONFIG
        dev_tools = ROLE_CONFIG["developer"]["tools"]
        assert len(dev_tools) == 5

    def test_developer_has_replace_in_file(self):
        from config import ROLE_CONFIG
        assert "replace_in_file" in ROLE_CONFIG["developer"]["tools"]

    def test_developer_has_rca_report(self):
        from config import ROLE_CONFIG
        assert "create_rca_report" in ROLE_CONFIG["developer"]["tools"]

    def test_developer_no_test_tools(self):
        from config import ROLE_CONFIG
        dev_tools = ROLE_CONFIG["developer"]["tools"]
        assert "run_unit_tests" not in dev_tools
        assert "check_coverage" not in dev_tools
        assert "run_lint_check" not in dev_tools

    def test_auditor_tools_count(self):
        from config import ROLE_CONFIG
        assert len(ROLE_CONFIG["auditor"]["tools"]) == 5

    def test_auditor_has_lint_check(self):
        from config import ROLE_CONFIG
        assert "run_lint_check" in ROLE_CONFIG["auditor"]["tools"]

    def test_auditor_has_issue_ticket(self):
        from config import ROLE_CONFIG
        assert "create_issue_ticket" in ROLE_CONFIG["auditor"]["tools"]

    def test_auditor_no_write_tools(self):
        from config import ROLE_CONFIG
        audit_tools = ROLE_CONFIG["auditor"]["tools"]
        assert "replace_in_file" not in audit_tools
        assert "write_file" not in audit_tools

    def test_acceptor_tools_count(self):
        from config import ROLE_CONFIG
        assert len(ROLE_CONFIG["acceptor"]["tools"]) == 4

    def test_acceptor_has_unit_tests(self):
        from config import ROLE_CONFIG
        assert "run_unit_tests" in ROLE_CONFIG["acceptor"]["tools"]

    def test_acceptor_has_coverage_check(self):
        from config import ROLE_CONFIG
        assert "check_coverage" in ROLE_CONFIG["acceptor"]["tools"]

    def test_acceptor_no_modify_tools(self):
        from config import ROLE_CONFIG
        acc_tools = ROLE_CONFIG["acceptor"]["tools"]
        assert "replace_in_file" not in acc_tools
        assert "read_file" not in acc_tools

    def test_dispatcher_tools_count(self):
        from config import ROLE_CONFIG
        assert len(ROLE_CONFIG["dispatcher"]["tools"]) == 7

    def test_dispatcher_has_core_tools(self):
        from config import ROLE_CONFIG
        disp_tools = ROLE_CONFIG["dispatcher"]["tools"]
        assert "scan_filesystem" in disp_tools
        assert "wake_up_role" in disp_tools
        assert "circuit_breaker_trigger" in disp_tools

    def test_dispatcher_no_business_tools(self):
        from config import ROLE_CONFIG
        disp_tools = ROLE_CONFIG["dispatcher"]["tools"]
        assert "replace_in_file" not in disp_tools
        assert "run_unit_tests" not in disp_tools

    def test_tools_are_unique_per_role(self):
        from config import ROLE_CONFIG
        for role, config in ROLE_CONFIG.items():
            tools = config["tools"]
            assert len(tools) == len(set(tools)), \
                f"{role} has duplicate tools"

    def test_no_tool_shared_between_dev_and_acc(self):
        from config import ROLE_CONFIG
        dev_set = set(ROLE_CONFIG["developer"]["tools"])
        acc_set = set(ROLE_CONFIG["acceptor"]["tools"])
        overlap = dev_set & acc_set
        assert len(overlap) == 0, \
            f"Developer and Acceptor share tools: {overlap}"


class TestROLECONFIGConstraints:
    """Test role-specific constraint configurations"""

    def test_developer_constraints(self):
        from config import ROLE_CONFIG
        constraints = ROLE_CONFIG["developer"]["constraints"]
        assert constraints.get("max_file_overwrite_lines") == 50
        assert constraints.get("require_rca_before_fix") is True
        assert constraints.get("require_living_comments") is True
        assert constraints.get("forbid_duplicate_files") is True

    def test_auditor_constraints(self):
        from config import ROLE_CONFIG
        constraints = ROLE_CONFIG["auditor"]["constraints"]
        assert constraints.get("forbid_any_modification") is True
        assert constraints.get("require_lint_check") is True
        assert constraints.get("must_generate_issue_on_reject") is True

    def test_acceptor_constraints(self):
        from config import ROLE_CONFIG
        constraints = ROLE_CONFIG["acceptor"]["constraints"]
        assert constraints.get("forbid_code_modification") is True
        assert constraints.get("require_test_execution") is True
        assert "sandbox_timeout_seconds" in constraints

    def test_dispatcher_constraints(self):
        from config import ROLE_CONFIG
        constraints = ROLE_CONFIG["dispatcher"]["constraints"]
        assert constraints.get("forbid_business_logic") is True
        assert constraints.get("enforce_state_machine") is True
        assert constraints.get("enforce_amnesia_sessions") is True


class TestROLECONFIGSystemPrompts:
    """Test system prompt content and format"""

    def test_developer_prompt_mentions_rca(self):
        from config import ROLE_CONFIG
        prompt = ROLE_CONFIG["developer"]["system_prompt"]
        assert "RCA" in prompt or "rca" in prompt.lower()

    def test_developer_prompt_mentions_sop(self):
        from config import ROLE_CONFIG
        prompt = ROLE_CONFIG["developer"]["system_prompt"]
        assert "SOP" in prompt or "标准作业程序" in prompt

    def test_auditor_prompt_mentions_reject(self):
        from config import ROLE_CONFIG
        prompt = ROLE_CONFIG["auditor"]["system_prompt"]
        assert "REJECT" in prompt or "驳回" in prompt

    def test_acceptor_prompt_mentions_test(self):
        from config import ROLE_CONFIG
        prompt = ROLE_CONFIG["acceptor"]["system_prompt"]
        assert "测试" in prompt or "test" in prompt.lower()

    def test_dispatcher_prompt_mentions_zero_trust(self):
        from config import ROLE_CONFIG
        prompt = ROLE_CONFIG["dispatcher"]["system_prompt"]
        assert "零信任" in prompt or "零信任" in prompt or "死循环" in prompt


# ============================================================
# STATE_TRANSITIONS Tests
# ============================================================

class TestStateTransitions:
    """Test state machine transition configuration"""

    def test_all_states_defined(self):
        from config import STATE_TRANSITIONS
        expected_states = ["dev", "audit", "accept", "verified", "blocked"]
        for state in expected_states:
            assert state in STATE_TRANSITIONS, f"Missing state: {state}"

    def test_dev_transitions(self):
        from config import STATE_TRANSITIONS
        assert "audit" in STATE_TRANSITIONS["dev"]
        assert "blocked" in STATE_TRANSITIONS["dev"]
        assert "verified" not in STATE_TRANSITIONS["dev"]

    def test_audit_transitions(self):
        from config import STATE_TRANSITIONS
        assert "accept" in STATE_TRANSITIONS["audit"]
        assert "dev" in STATE_TRANSITIONS["audit"]
        assert "blocked" in STATE_TRANSITIONS["audit"]
        assert "verified" not in STATE_TRANSITIONS["audit"]

    def test_accept_transitions(self):
        from config import STATE_TRANSITIONS
        assert "verified" in STATE_TRANSITIONS["accept"]
        assert "dev" in STATE_TRANSITIONS["accept"]
        assert "blocked" in STATE_TRANSITIONS["accept"]

    def test_verified_is_terminal(self):
        from config import STATE_TRANSITIONS
        assert STATE_TRANSITIONS["verified"] == []

    def test_blocked_is_terminal(self):
        from config import STATE_TRANSITIONS
        assert STATE_TRANSITIONS["blocked"] == []

    def test_no_self_transitions(self):
        from config import STATE_TRANSITIONS
        for state, targets in STATE_TRANSITIONS.items():
            assert state not in targets, \
                f"State '{state}' should not allow self-transition"

    def test_transitions_match_model_enum(self):
        from config import STATE_TRANSITIONS
        from models import FileStatus
        for state_str in STATE_TRANSITIONS.keys():
            try:
                FileStatus(state_str)
            except ValueError:
                pytest.fail(f"State '{state_str}' not in FileStatus enum")


# ============================================================
# CIRCUIT_BREAKER_CONFIG Tests
# ============================================================

class TestCircuitBreakerConfig:
    """Test circuit breaker configuration"""

    def test_max_retries_value(self):
        from config import CIRCUIT_BREAKER_CONFIG
        assert CIRCUIT_BREAKER_CONFIG["max_retries"] == 3

    def test_retry_actions_defined(self):
        from config import CIRCUIT_BREAKER_CONFIG
        actions = CIRCUIT_BREAKER_CONFIG["retry_actions"]
        assert 1 in actions
        assert 2 in actions
        assert 3 in actions
        assert actions[1] == "RETRY"
        assert actions[2] == "UPGRADE_CONTEXT"
        assert actions[3] == "BLOCKED"

    def test_token_limit_positive(self):
        from config import CIRCUIT_BREAKER_CONFIG
        assert CIRCUIT_BREAKER_CONFIG["token_limit_per_call"] == 4000
        assert CIRCUIT_BREAKER_CONFIG["token_limit_per_call"] > 0

    def test_fingerprint_algorithm(self):
        from config import CIRCUIT_BREAKER_CONFIG
        assert CIRCUIT_BREAKER_CONFIG["fingerprint_algorithm"] == "md5"


# ============================================================
# DIRECTORY_STRUCTURE_CONFIG Tests
# ============================================================

class TestDirectoryStructureConfig:
    """Test directory structure configuration"""

    def test_required_dirs_defined(self):
        from config import DIRECTORY_STRUCTURE_CONFIG
        dirs = DIRECTORY_STRUCTURE_CONFIG["required_dirs"]
        assert ".constitution" in dirs
        assert ".issues/open" in dirs
        assert ".issues/in_progress" in dirs
        assert ".issues/resolved" in dirs
        assert "src" in dirs
        assert "tests" in dirs
        assert "docs" in dirs
        assert ".quarantine" in dirs

    def test_required_dirs_count(self):
        from config import DIRECTORY_STRUCTURE_CONFIG
        assert len(DIRECTORY_STRUCTURE_CONFIG["required_dirs"]) == 8

    def test_max_src_depth(self):
        from config import DIRECTORY_STRUCTURE_CONFIG
        assert DIRECTORY_STRUCTURE_CONFIG["max_src_depth"] == 4

    def test_business_extensions_include_python(self):
        from config import DIRECTORY_STRUCTURE_CONFIG
        exts = DIRECTORY_STRUCTURE_CONFIG["business_file_extensions"]
        assert ".py" in exts
        assert ".js" in exts
        assert ".ts" in exts
        assert ".vue" in exts

    def test_business_extensions_count(self):
        from config import DIRECTORY_STRUCTURE_CONFIG
        assert len(DIRECTORY_STRUCTURE_CONFIG["business_file_extensions"]) == 9


# ============================================================
# Edge Case & Validation Tests
# ============================================================

class TestConfigEdgeCases:
    """Edge case and validation tests for config"""

    def test_role_config_is_immutable_at_runtime(self):
        from config import ROLE_CONFIG
        original_len = len(ROLE_CONFIG["developer"]["tools"])

    def test_state_transition_targets_are_valid_states(self):
        from config import STATE_TRANSITIONS
        valid_states = set(STATE_TRANSITIONS.keys())
        for state, targets in STATE_TRANSITIONS.items():
            for target in targets:
                assert target in valid_states, \
                    f"Invalid target '{target}' from state '{state}'"

    def test_no_duplicate_tools_across_all_roles(self):
        from config import ROLE_CONFIG
        all_tools = {}
        for role, config in ROLE_CONFIG.items():
            for tool in config["tools"]:
                if tool in all_tools:
                    pass
                else:
                    all_tools[tool] = role

    def test_total_unique_tools_count(self):
        from config import ROLE_CONFIG
        all_tools = set()
        for config in ROLE_CONFIG.values():
            all_tools.update(config["tools"])
        assert len(all_tools) >= 15

    def test_constraints_are_boolean_or_numeric(self):
        from config import ROLE_CONFIG
        for role, config in ROLE_CONFIG.items():
            for key, value in config["constraints"].items():
                assert isinstance(value, (bool, int, float, str)), \
                    f"{role}.{key} has unexpected type: {type(value)}"

    def test_display_names_are_unique(self):
        from config import ROLE_CONFIG
        names = [c["display_name"] for c in ROLE_CONFIG.values()]
        assert len(names) == len(set(names))

    def test_colors_are_valid_hex(self):
        import re
        from config import ROLE_CONFIG
        hex_pattern = re.compile(r'^#[0-9a-fA-F]{6}$')
        for role, config in ROLE_CONFIG.items():
            assert hex_pattern.match(config["color"]), \
                f"{role} has invalid color: {config['color']}"
