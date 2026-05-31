#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
北极星AI商机获客系统 - Python工具库
"""

import os
import sys
import json
import requests
from datetime import datetime
from typing import Dict, List, Any, Optional

# 配置常量
BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")
API_VERSION = "v1"
TIMEOUT = 30


class APIClient:
    """API客户端"""
    
    def __init__(self, base_url: str = BASE_URL, token: Optional[str] = None):
        self.base_url = base_url.rstrip("/")
        self.token = token
        self.session = requests.Session()
        
    def request(self, method: str, endpoint: str, **kwargs) -> Dict[str, Any]:
        """发送请求"""
        url = f"{self.base_url}/api/{API_VERSION}{endpoint}"
        headers = kwargs.pop("headers", {})
        
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
            
        try:
            response = self.session.request(
                method,
                url,
                headers=headers,
                timeout=TIMEOUT,
                **kwargs
            )
            response.raise_for_status()
            return response.json()
        except requests.RequestException as e:
            print(f"请求失败: {e}")
            return {"code": 500, "message": str(e)}


def format_date(date: datetime, fmt: str = "%Y-%m-%d %H:%M:%S") -> str:
    """格式化日期"""
    return date.strftime(fmt)


def parse_date(date_str: str, fmt: str = "%Y-%m-%d %H:%M:%S") -> datetime:
    """解析日期"""
    return datetime.strptime(date_str, fmt)


def load_config(config_path: str) -> Dict[str, Any]:
    """加载配置文件"""
    with open(config_path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_config(config: Dict[str, Any], config_path: str) -> None:
    """保存配置文件"""
    with open(config_path, "w", encoding="utf-8") as f:
        json.dump(config, f, indent=2, ensure_ascii=False)


def create_backup(file_path: str) -> str:
    """创建备份"""
    backup_path = f"{file_path}.backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    if os.path.exists(file_path):
        import shutil
        shutil.copy2(file_path, backup_path)
    return backup_path


def restore_backup(backup_path: str, target_path: str) -> bool:
    """恢复备份"""
    if not os.path.exists(backup_path):
        print(f"备份文件不存在: {backup_path}")
        return False
        
    import shutil
    shutil.copy2(backup_path, target_path)
    return True


def main():
    """主函数"""
    print("北极星AI商机获客系统 - Python工具库")
    print(f"版本: 1.0.0")
    print(f"API地址: {BASE_URL}")
    

if __name__ == "__main__":
    main()
