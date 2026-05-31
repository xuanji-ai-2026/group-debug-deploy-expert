"""
pm-tripartite-anti-hallucination-system - 安装脚本

企业级AI协作治理框架
"""

from setuptools import setup, find_packages

# 读取README作为long_description
with open("README.md", "r", encoding="utf-8") as f:
    long_description = f.read()

setup(
    name="pm-tripartite-anti-hallucination-system",
    version="1.0.0",
    author="Project Manager Driven System",
    author_email="pm-system@example.com",
    description="项目经理驱动的三权分立防幻觉系统 - 企业级AI协作治理框架",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/your-org/pm-tripartite-anti-hallucination-system",
    project_urls={
        "Bug Tracker": "https://github.com/your-org/pm-tripartite-anti-hallucination-system/issues",
        "Documentation": "https://github.com/your-org/pm-tripartite-anti-hallucination-system#readme",
        "Source Code": "https://github.com/your-org/pm-tripartite-anti-hallucination-system",
    },
    license="MIT",
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
        "Topic :: Software Development :: Libraries :: Python Modules",
        "Topic :: Software Development :: Quality Assurance",
        "Topic :: Security",
    ],
    keywords=[
        "ai-agent anti-hallucination tripartite-separation code-quality "
        "zero-trust circuit-breaker developer-workflow audit-system "
        "acceptance-testing role-based-access-control file-integrity "
        "automated-testing root-cause-analysis micro-surgery "
        "self-driving-engine log-management file-locking dispatcher-pattern "
        "openclaw hermes claude-code cursor"
    ],
    python_requires=">=3.8",
    packages=find_packages(exclude=["tests", "*.tests", "*.tests.*", "tests.*"]),
    install_requires=[
        "pydantic>=2.0.0",
        "pyyaml>=6.0",
    ],
    extras_require={
        "dev": [
            "pytest>=7.0.0",
            "pytest-cov>=4.0.0",
            "black>=23.0.0",
            "isort>=5.12.0",
            "mypy>=1.0.0",
            "flake8>=6.0.0",
            "bandit>=1.7.0",
        ],
        "audit": [
            "flake8>=6.0.0",
            "bandit>=1.7.0",
        ],
        "test": [
            "pytest>=7.0.0",
            "pytest-cov>=4.0.0",
        ],
        "web": [
            "flask>=2.3.0",
            "redis>=4.5.0",
        ],
    },
    include_package_data=True,
    zip_safe=False,
)
