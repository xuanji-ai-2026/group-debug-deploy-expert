---
id: TEST-20260531213458
status: dev
role_owner: developer
version: 1.2
genesis_hash: 
previous_hash: eb6502eb2e19988f2e7e35cedd6f7fcfa2e0febed8ff20a0d6d0330df2f7987a
last_updated: 2026-05-31T13:34:59.014739Z
changelog:
  - "[AUDIT-REJECT] 代码存在 1 处规范问题，请修复 Flake8 报错。工单ID: ISSUE-20260531213459"
tags: "[\\\"test\\\", \\\"demo\\\"]"
---
# Test File - AI Collaboration System Demo

This is a sample file to demonstrate the complete lifecycle.

def calculate_sum(a, b):
    # [MOD-20260531] @developer: Add type validation to prevent TypeError
    if not isinstance(a, (int, float)):
        raise TypeError(f"a must be numeric, got {type(a).__name__}")
    if not isinstance(b, (int, float)):
        raise TypeError(f"b must be numeric, got {type(b).__name__}")
    return a + b

def greet_user(name):
    if name is None:
        return "Hello, Guest!"
    return "Hello, " + str(name) + "!"

class DataProcessor:
    def __init__(self):
        self.data = []
    
    def add_item(self, item):
        self.data.append(item)
    
    def process(self):
        result = []
        for item in self.data:
            processed = item.upper()
            result.append(processed)
        return result
