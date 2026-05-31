# 北极星AI - 自动化脚本库架构设计
# 基于2025-2026业界最佳实践整合
# 整合Jenkins/GitHub Actions/Fastlane/Docker/K8s成熟方案

## ============================================================
##  目录结构总览
## ============================================================

deploy/
├── scripts/                          # 🎯 核心自动化脚本库 (NEW)
│   ├── lib/                         #    公共函数库
│   │   ├── common.sh                #       通用函数 (日志/颜色/工具检查)
│   │   ├── env.sh                   #       环境加载 (.env管理)
│   │   ├── docker-lib.sh            #       Docker操作函数
│   │   ├── k8s-lib.sh               #       K8s操作函数
│   │   ├── health-lib.sh            #       健康检查函数
│   │   └── rollback-lib.sh          #       回滚操作函数
│   │
│   ├── backend/                     #    后端服务自动化
│   │   ├── build.sh                 #       Maven多模块构建
│   │   ├── deploy.sh                #       JAR包部署 (支持Docker/裸机)
│   │   ├── restart.sh               #       服务重启 (优雅停启)
│   │   └── scale.sh                 #       水平扩缩容
│   │
│   ├── frontend/                    #    前端自动化
│   │   ├── build.sh                 #       Vue3/Vite构建优化
│   │   ├── deploy.sh                #       Nginx静态资源部署
│   │   └── cdn-sync.sh             #       CDN同步 (可选)
│   │
│   ├── mobile/                      #    移动端自动化
│   │   ├── android/
│   │   │   ├── build-apk.sh         #       Gradle多渠道打包
│   │   │   ├── sign-apk.sh          #       APK签名 (V1+V2)
│   │   │   ├── release.sh           #       完整发布流程
│   │   │   └── fastlane/Fastfile    #       Fastlane配置
│   │   └── ios/
│   │       ├── build-ipa.sh         #       Xcode Archive构建
│   │       ├── sign-export.sh       #       签名+导出IPA
│   │       ├── release.sh           #       App Store发布
│   │       └── fastlane/Fastfile    #       Fastlane配置
│   │
│   ├── docker/                      #    Docker容器化
│   │   ├── compose-deploy.sh        #       Docker Compose一键部署
│   │   ├── image-build.sh           #       镜像构建+推送
│   │   ├── cleanup.sh              #       资源清理
│   │   └── migrate.sh              #       数据库迁移
│   │
│   ├── k8s/                         #    Kubernetes编排
│   │   ├── full-deploy.sh           #       全量部署
│   │   ├── rolling-update.sh        #       滚动更新
│   │   ├── canary-deploy.sh         #       金丝雀发布
│   │   └── namespace-cleanup.sh     #       命名空间清理
│   │
│   ├── ops/                         #    运维工具
│   │   ├── health-check.sh          #       全栈健康检查
│   │   ├── rollback.sh              #       一键回滚 (Docker+K8s)
│   │   ├── backup.sh                #       数据备份 (DB/Redis/Config)
│   │   ├── restore.sh               #       数据恢复
│   │   ├── log-collect.sh           #       日志收集
│   │   └── monitor.sh               #       资源监控告警
│   │
│   └── ci/                          #    CI/CD流水线模板
│       ├── jenkins/
│       │   ├── Jenkinsfile          #       Jenkins流水线
│       │   └── groovy/              #       共享库
│       ├── github-actions/
│       │   ├── build-backend.yml    #       后端CI
│       │   ├── build-frontend.yml   #       前端CI
│       │   ├── build-android.yml    #       Android CI
│       │   ├── build-ios.yml        #       iOS CI
│       │   └── deploy.yml           #       CD部署
│       └── gitlab-ci/
│           └── .gitlab-ci.yml       #       GitLab CI
│
├── config/                           # 🔒 配置管理 (NEW)
│   ├── .env.example                 #    环境变量模板
│   ├── .env.dev                     #    开发环境 (git ignore)
│   ├── .env.prod                    #    生产环境 (git ignore)
│   ├── services.yaml                #    服务定义 (端口/依赖/JVM参数)
│   └── versions.yaml                #    版本锁定文件
│
├── docker/                          # [EXISTING] Docker配置 (保留)
├── k8s/                             # [EXISTING] K8s配置 (保留)
└── archive/                         # 📦 备份归档 (NEW)
    ├── backups/                     #    数据库备份
    ├── configs/                     #    配置快照
    └── releases/                    #    发布版本归档

## ============================================================
##  设计原则 (基于21 Iron Principles)
## ============================================================

P1 绝对真实: 所有脚本必须执行真实命令，输出真实结果
P2 效率优先: 复用现有脚本，不重复造轮子
P3 最小变更: 增量改进，不推翻重写
P7 真实操作: 必须可在真实环境执行
P12 预备份+即时回滚: 所有破坏性操作前必须备份
P18 全生命周期闭环: 构建→测试→部署→验证→监控→回滚

## ============================================================
##  技术选型 (基于全网搜索最佳实践)
## ============================================================

后端构建: Maven + Jenkins Pipeline (Spring Boot官方推荐)
前端构建: Vite + npm ci + GitHub Actions (Vite官方CI最佳实践)
Android打包: Gradle + Fastlane (Android官方CI/CD最佳实践)
iOS打包: xcodebuild + Fastlane (iOS官方CI/CD最佳实践)
Docker: Docker Compose + BuildKit + 镜像扫描
K8s: Helm Charts + kubectl rolling-update (K8s官方滚动更新)
CI/CD: GitHub Actions (免费/开源项目首选) 或 Jenkins (企业级)

## ============================================================
##  关键能力矩阵
## ============================================================

| 能力 | 当前状态 | 目标状态 | 实现方式 |
|------|---------|---------|---------|
| 一键构建 | ❌ 手动 | ✅ 自动化 | scripts/backend/build.sh |
| 一键部署 | ⚠️ 半自动 | ✅ 全自动 | scripts/docker/compose-deploy.sh |
| 健康检查 | ❌ 无 | ✅ 自动化 | scripts/ops/health-check.sh |
| 一键回滚 | ❌ 无 | ✅ <10分钟 | scripts/ops/rollback.sh |
| Android打包 | ❌ 手动 | ✅ 自动化 | scripts/mobile/android/release.sh |
| iOS打包 | ❌ 无 | ✅ 自动化 | scripts/mobile/ios/release.sh |
| 前端部署 | ❌ 手动 | ✅ 自动化 | scripts/frontend/deploy.sh |
| CI/CD | ❌ 无 | ✅ 自动触发 | .github/workflows/*.yml |
| 环境隔离 | ⚠️ 混乱 | ✅ 严格分离 | config/.env.* |
| 版本管理 | ❌ 手动 | ✅ 自动递增 | config/versions.yaml |

## ============================================================
##  执行顺序 (依赖关系)
## ============================================================

Phase 1: 基础设施 (lib + config)
  ↓
Phase 2: 后端自动化 (build → deploy → health)
  ↓  
Phase 3: 前端自动化 (build → deploy)
  ↓
Phase 4: 移动端自动化 (android + ios)
  ↓
Phase 5: 容器化 (docker + k8s)
  ↓
Phase 6: 运维工具 (backup → rollback → monitor)
  ↓
Phase 7: CI/CD集成 (github-actions/jenkins)
