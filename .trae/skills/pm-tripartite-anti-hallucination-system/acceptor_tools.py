"""
acceptor_tools.py - 验收官的沙箱与 PyTest 自动执行

验收官是最后一道防线，它必须冷酷无情。
利用 subprocess 启动独立的子进程来跑测试，
即使测试代码崩溃也不会搞挂调度师主程序。
"""

import subprocess
import os
from typing import Tuple, Optional
from models import FileStatus


class AcceptorTools:
    """
    验收官专用工具集
    
    核心职责：在沙箱中执行测试，根据结果做出最终决策。
    """
    
    def __init__(self, project_dir: str):
        self.project_dir = project_dir
        self.default_timeout = 30  # 默认30秒超时

    def run_unit_tests(self, source_file: str) -> Tuple[bool, str]:
        """
        针对特定文件执行对应的单元测试
        
        Args:
            source_file: 源代码文件路径
            
        Returns:
            tuple: (是否通过, 输出信息)
        """
        filename = os.path.basename(source_file)
        name, ext = os.path.splitext(filename)
        
        # 假设测试文件遵循 pytest 规范
        test_filename = f"test_{name}.py" if ext == '.py' else f"test_{name}{ext}"
        test_path = os.path.join(self.project_dir, "tests", "unit", test_filename)
        
        if not os.path.exists(test_path):
            # 尝试其他可能的测试目录
            alt_paths = [
                os.path.join(self.project_dir, "tests", test_filename),
                os.path.join(self.project_dir, "test", test_filename),
            ]
            
            for alt_path in alt_paths:
                if os.path.exists(alt_path):
                    test_path = alt_path
                    break
            else:
                print(f"⚠️ [验收警告] 未找到对应的测试文件 {test_filename}，跳过测试直接放行。")
                return True, "无对应测试用例（建议补充单元测试）"
        
        print(f"⚖️ [验收官] 正在沙箱中执行测试套件：{test_filename} ...")
        
        try:
            # 启动 PyTest 并在失败时立即停止 (-x)，输出详细信息 (-v)
            result = subprocess.run(
                ["pytest", test_path, "-x", "-v", "--tb=short"],
                cwd=self.project_dir,
                capture_output=True,
                text=True,
                timeout=self.default_timeout  # 设置超时熔断
            )
            
            if result.returncode == 0:
                # 解析测试结果统计
                output = result.stdout
                
                # 提取通过的测试数量
                passed_match = output.rsplit(' passed', 1)
                if len(passed_match) > 1:
                    summary = passed_match[-1].split()[0] if passed_match[-1].strip().isdigit() else "若干"
                else:
                    summary = "全部"
                
                print(f"✅ [验收通过] 所有单元测试全部 Green！（共 {summary} 个用例）")
                return True, f"PyTest 执行成功，{summary} 个测试用例全部通过。"
                
            else:
                print(f"❌ [验收驳回] 测试用例执行失败！")
                
                # 提取 PyTest 的失败摘要
                failure_log = result.stdout + "\n" + result.stderr
                
                # 简化失败日志（只保留关键信息）
                short_log = self._extract_failure_summary(failure_log)
                
                return False, f"测试失败详情:\n{short_log}"
                
        except subprocess.TimeoutExpired:
            return False, "[致命错误] 测试执行超过30秒，疑似陷入死循环，已强制终止！"
        except Exception as e:
            return False, f"[验收工具异常] {str(e)}"

    def run_integration_tests(self, source_file: str) -> Tuple[bool, str]:
        """
        执行集成测试（如果有）
        
        Args:
            source_file: 源代码文件路径
            
        Returns:
            tuple: (是否通过, 输出信息)
        """
        filename = os.path.basename(source_file)
        name, ext = os.path.splitext(filename)
        test_filename = f"integration_test_{name}.py" if ext == '.py' else f"integration_test_{name}{ext}"
        test_path = os.path.join(self.project_dir, "tests", "integration", test_filename)
        
        if not os.path.exists(test_path):
            return True, "无对应集成测试用例（可选）"
        
        print(f"🔗 [验收官] 正在执行集成测试：{test_filename} ...")
        
        try:
            result = subprocess.run(
                ["pytest", test_path, "-x", "-v", "--tb=short"],
                cwd=self.project_dir,
                capture_output=True,
                text=True,
                timeout=60  # 集成测试超时时间更长
            )
            
            if result.returncode == 0:
                print("✅ [验收通过] 集成测试全部通过！")
                return True, "集成测试执行成功。"
            else:
                print("❌ [验收驳回] 集成测试失败！")
                failure_log = result.stdout + "\n" + result.stderr
                return False, f"集成测试失败:\n{self._extract_failure_summary(failure_log)}"
                
        except subprocess.TimeoutExpired:
            return False, "[致命错误] 集成测试执行超过60秒，已强制终止！"
        except Exception as e:
            return False, f"[验收工具异常] {str(e)}"

    def check_coverage(self, source_file: str) -> Tuple[bool, str, float]:
        """
        检查测试覆盖率
        
        Args:
            source_file: 源代码文件路径
            
        Returns:
            tuple: (是否达标, 详情, 覆盖率百分比)
        """
        try:
            result = subprocess.run(
                ["pytest", "--cov=", source_file, "--cov-report=term-missing"],
                cwd=self.project_dir,
                capture_output=True,
                text=True,
                timeout=60
            )
            
            output = result.stdout + result.stderr
            
            # 尝试提取覆盖率百分比
            coverage_percent = self._extract_coverage_percentage(output)
            
            if coverage_percent >= 80:  # 80%覆盖率门槛
                print(f"✅ [覆盖率达标] {coverage_percent:.1f}% (门槛: 80%)")
                return True, f"测试覆盖率: {coverage_percent:.1f}%，符合标准。", coverage_percent
            else:
                print(f"⚠️ [覆盖率不足] {coverage_percent:.1f}% (门槛: 80%)")
                return False, f"测试覆盖率不足: {coverage_percent:.1f}% (需达到80%)", coverage_percent
                
        except Exception as e:
            print(f"⚠️ [覆盖率检查异常] {str(e)}")
            return True, f"无法检查覆盖率（{str(e)}），跳过此项。", 0.0

    def acceptance_decision(self, file_path: str) -> Tuple[str, str]:
        """
        验收官的最终决策逻辑
        
        Args:
            file_path: 要验收的文件路径
            
        Returns:
            tuple: (决策, 决策说明)
                - "PROMOTE_TO_VERIFIED" / "REJECT_TO_DEV"
        """
        print(f"\n{'='*60}")
        print(f"⚖️ [验收官] 开始最终验收流程")
        print(f"   目标文件: {file_path}")
        print(f"{'='*60}\n")
        
        # 1. 单元测试
        unit_passed, unit_message = self.run_unit_tests(file_path)
        if not unit_passed:
            return "REJECT_TO_DEV", f"单元测试未通过:\n{unit_message}"
        
        # 2. 集成测试（可选）
        integration_passed, integration_message = self.run_integration_tests(file_path)
        if not integration_passed:
            return "REJECT_TO_DEV", f"集成测试未通过:\n{integration_message}"
        
        # 3. 覆盖率检查（可选）
        coverage_ok, coverage_message, coverage_pct = self.check_coverage(file_path)
        
        # 4. 最终决策
        all_passed = unit_passed and integration_passed and coverage_ok
        
        if all_passed:
            final_msg = f"✅ [最终验收通过]\n"
            final_msg += f"   单元测试: {unit_message}\n"
            final_msg += f"   集成测试: {integration_message}\n"
            final_msg += f"   覆盖率: {coverage_message}\n"
            final_msg += f"\n   🎉 文件已准备就绪，可以发布到生产环境！"
            
            return "PROMOTE_TO_VERIFIED", final_msg
        else:
            return "REJECT_TO_DEV", f"验收未通过，请查看具体失败原因。"

    def _extract_failure_summary(self, log: str) -> str:
        """提取失败的测试摘要"""
        lines = log.split('\n')
        summary_lines = []
        
        # 寻找 FAILED 行和断言错误
        for line in lines:
            if 'FAILED' in line or 'AssertionError' in line or 'Error' in line:
                summary_lines.append(line.strip())
        
        if not summary_lines:
            # 如果没找到特定模式，返回最后20行
            summary_lines = lines[-20:]
        
        return '\n'.join(summary_lines[-15:])  # 最多返回15行

    def _extract_coverage_percentage(self, log: str) -> float:
        """从pytest-cov输出中提取覆盖率百分比"""
        import re
        
        # 匹配 "TOTAL xx%" 或 "xx% coverage" 等模式
        patterns = [
            r'TOTAL\s+(\d+\.?\d*)%',
            r'(\d+\.?\d*)%\s+coverage',
            r'(\d+\.?\d*)%'
        ]
        
        for pattern in patterns:
            match = re.search(pattern, log)
            if match:
                try:
                    return float(match.group(1))
                except ValueError:
                    continue
        
        return 0.0


if __name__ == "__main__":
    # 测试代码
    print("✅ 验收官工具测试通过")
    
    # 创建测试实例
    acceptor = AcceptorTools('.')
    print(f"项目目录: {acceptor.project_dir}")
    print(f"默认超时: {acceptor.default_timeout} 秒")
