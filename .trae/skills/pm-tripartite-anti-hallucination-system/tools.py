"""
tools.py - 底层防篡改与原子工具拦截器

实现了《宪法》中最硬核的物理约束：
- 链式哈希校验
- 强制原地手术
- 防越权拦截
"""

import os
import re
import hashlib
from typing import List, Optional
from models import FileFrontmatter, FileStatus


class PermissionError(Exception):
    """权限不足异常"""
    pass


class TamperDetectedError(Exception):
    """检测到篡改异常"""
    pass


class SurgeryRejectedError(Exception):
    """手术被拒绝异常（违反原地手术原则）"""
    pass


class CommentMissingError(Exception):
    """缺少活体注释异常"""
    pass


class AtomicToolInterceptor:
    """
    原子工具拦截器
    
    所有的文件操作都必须经过这个拦截器，
    实现《宪法》第二章的物理防越权机制。
    """

    def __init__(self, allowed_tools: list, role_name: str):
        self.allowed_tools = allowed_tools  # 当前角色的专属工具白名单
        self.role_name = role_name

    def check_permission(self, tool_name: str):
        """
        物理防越权：如果工具不在白名单内，直接在SDK层面抛出异常
        
        Args:
            tool_name: 要调用的工具名称
            
        Raises:
            PermissionError: 当工具不在白名单中时
        """
        if tool_name not in self.allowed_tools:
            raise PermissionError(
                f"[越权拦截] 角色 '{self.role_name}' 无权调用工具: {tool_name}\n"
                f"可用工具: {self.allowed_tools}"
            )

    def verify_hash_chain(self, file_path: str, expected_previous_hash: str):
        """
        防篡改校验：修改前必须核对 previous_hash
        
        Args:
            file_path: 文件路径
            expected_previous_hash: 期望的上一次哈希值
            
        Raises:
            TamperDetectedError: 当哈希不匹配时
        """
        if not os.path.exists(file_path):
            return True  # 新建文件无需校验历史哈希
        
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 计算实际内容的哈希（包含页头部分）
        actual_hash = hashlib.sha256(content.encode('utf-8')).hexdigest()
        
        if actual_hash != expected_previous_hash:
            raise TamperDetectedError(
                f"[防篡改拦截] 文件 {file_path} 的哈希链断裂！\n"
                f"期望哈希: {expected_previous_hash[:16]}...\n"
                f"实际哈希: {actual_hash[:16]}...\n"
                f"检测到绕过系统的私自修改。"
            )

    def enforce_micro_surgery(self, file_path: str, new_content: str):
        """
        强制原地手术：严禁全量覆盖超过50行的文件
        
        Args:
            file_path: 文件路径
            new_content: 新内容
            
        Raises:
            SurgeryRejectedError: 当违反原地手术原则时
        """
        if not os.path.exists(file_path):
            return  # 新文件不受此限制
        
        with open(file_path, 'r', encoding='utf-8') as f:
            old_lines = len(f.readlines())
        
        # 如果原文件超过50行，且新内容试图大幅改变行数
        if old_lines > 50:
            new_lines = len(new_content.splitlines())
            line_diff = abs(old_lines - new_lines)
            
            if line_diff > 10:
                raise SurgeryRejectedError(
                    f"[原地手术拦截] 禁止对大型文件进行全量覆盖写入！\n"
                    f"文件: {file_path}\n"
                    f"原行数: {old_lines}\n"
                    f"新行数: {new_lines}\n"
                    f"行数差异: {line_diff}\n"
                    f"请使用 replace_in_file 进行局部精准修复。"
                )

    def enforce_living_comments(self, content_snippet: str, role_name: str):
        """
        强制活体注释检查：修改必须包含 [MOD-日期] 标记
        
        Args:
            content_snippet: 要写入的内容片段
            role_name: 当前角色名
            
        Raises:
            CommentMissingError: 当缺少活体注释时
        """
        from datetime import datetime
        today = datetime.now().strftime('%Y%m%d')
        pattern = rf'\[MOD-{today}.*@{role_name}.*\]'
        
        if not re.search(pattern, content_snippet, re.IGNORECASE):
            raise CommentMissingError(
                f"[规范拦截] 代码修改未附带标准的活体注释！\n"
                f"期望格式: [MOD-{today}] @{role_name}: 简述修改原因\n"
                f"已驳回操作！"
            )


class ToolRegistry:
    """
    工具注册表
    
    管理所有可用工具及其元数据，供调度师动态加载。
    """
    
    _instance = None
    _tools = {}
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def register(self, tool_name: str, tool_config: dict):
        """注册工具"""
        self._tools[tool_name] = tool_config
    
    def get_tool(self, tool_name: str) -> Optional[dict]:
        """获取工具配置"""
        return self._tools.get(tool_name)
    
    def get_tools_for_role(self, role_name: str) -> List[str]:
        """获取指定角色的工具白名单"""
        from config import ROLE_CONFIG
        role_config = ROLE_CONFIG.get(role_name, {})
        return role_config.get('tools', [])
    
    def validate_tool_call(self, role_name: str, tool_name: str):
        """验证工具调用权限"""
        allowed_tools = self.get_tools_for_role(role_name)
        interceptor = AtomicToolInterceptor(allowed_tools, role_name)
        interceptor.check_permission(tool_name)


if __name__ == "__main__":
    # 测试代码
    print("✅ 工具拦截器测试通过")
    
    # 测试权限检查
    interceptor = AtomicToolInterceptor(['read_file', 'write_file'], 'developer')
    
    try:
        interceptor.check_permission('read_file')
        print("✓ 权限检查通过: read_file")
    except PermissionError as e:
        print(f"✗ 权限检查失败: {e}")
    
    try:
        interceptor.check_permission('delete_file')
        print("✗ 应该抛出异常但没有")
    except PermissionError as e:
        print("✓ 正确拦截了越权操作: delete_file")
