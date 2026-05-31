#!/bin/bash

# ============================================================
#  北极星AI - 单体应用生产级启动脚本 (v2.0)
#  整合16个微服务为单一JAR包 | 支持配置完全外置
#  
#  核心特性:
#    ✅ 自动加载 .env 环境变量文件（实现配置外置）
#    ✅ 支持外部配置覆盖 (config/application-override.yml)
#    ✅ 多环境切换 (.env.dev / .env.test / .env.prod)
#    ✅ 启动前环境校验 (DB/Redis连通性检查)
#    ✅ 必需环境变量检测 (防止空密码启动)
#    ✅ 优雅停机信号处理 (SIGTERM/SIGINT)
#    ✅ 彩色日志输出 + 时间戳
#
#  使用方式:
#    ./start.sh                    # 默认启动（自动加载 .env）
#    ./start.sh dev                # 开发环境
#    ./start.sh prod               # 生产环境
#    ./start.sh --check-only       # 仅做环境检查，不启动
#
#  最后更新: 2026-05-20 (生产部署版 v2.0)
#  符合21条铁原则: P1(绝对真实性) + P3(最小改动) + P7(真实操作)
# ============================================================

set -e

# ==================== 颜色定义 ====================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ==================== 全局变量 ====================
APP_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_FILE="beijixing-app-1.0.0-SNAPSHOT.jar"
LOG_FILE="app.log"
PID_FILE="app.pid"
ENV_FILE=""
CONFIG_DIR="config"
OVERRIDE_CONFIG="$CONFIG_DIR/application-override.yml"

# JVM参数 (可通过环境变量覆盖)
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx768m}"
SPRING_PROFILES="${SPRING_PROFILES:-}"

# ==================== 函数定义 ====================

log_info() {
    echo -e "${CYAN}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[⚠]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

show_banner() {
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}  北极星AI - 单体应用生产级启动脚本 v2.0${NC}"
    echo -e "${BLUE}  整合16个微服务为单一JAR包 | 配置完全外置${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
}

# ==================== 核心功能: 加载环境变量 ====================
load_env_file() {
    log_info "[ENV-LOAD] 开始加载环境变量文件..."
    
    # 优先级1: 命令行参数指定环境
    if [ -n "$1" ] && [ "$1" != "--check-only" ]; then
        ENV_CANDIDATES=(".env.$1")
    else
        # 优先级2: 按顺序查找 .env 文件
        ENV_CANDIDATES=(".env" ".env.local" ".env.prod" ".env.dev")
    fi
    
    for env_candidate in "${ENV_CANDIDATES[@]}"; do
        if [ -f "$env_candidate" ]; then
            ENV_FILE="$env_candidate"
            break
        fi
    done
    
    if [ -z "$ENV_FILE" ]; then
        log_warn "未找到 .env 文件，将使用 application.yml 中的默认值"
        log_warn "提示: 复制 .env.example 为 .env 并填写实际配置"
        return 0
    fi
    
    log_success "找到环境变量文件: $ENV_FILE"
    
    # 使用 set -a 自动导出所有变量为环境变量
    set -a
    source "$ENV_FILE"
    set +a
    
    log_success "环境变量加载完成 ($ENV_FILE)"
    echo ""
}

# ==================== 核心功能: 校验必需环境变量 ====================
validate_required_vars() {
    log_info "[ENV-CHECK] 校验必需环境变量..."
    
    local errors=0
    
    # 必需变量列表 (生产环境不能使用默认值)
    local required_vars=(
        "DB_PASSWORD:数据库密码"
        "REDIS_PASSWORD:Redis密码"
        "JWT_SECRET:JWT密钥"
    )
    
    for var_info in "${required_vars[@]}"; do
        IFS=':' read -r var_name var_desc <<< "$var_info"
        var_value="${!var_name}"  # 间接引用获取变量值
        
        if [ -z "$var_value" ] || [[ "$var_value" == \$* ]]; then
            log_error "$var_desc ($var_name) 未设置!"
            ((errors++))
        fi
    done
    
    # 可选变量警告 (建议设置但非强制)
    local optional_vars=(
        "SMTP_HOST:邮件服务器"
        "VOLCENGINE_API_KEY:火山引擎API密钥"
        "TENCENT_SECRET_ID:腾讯云SecretId"
    )
    
    for var_info in "${optional_vars[@]}"; do
        IFS=':' read -r var_name var_desc <<< "$var_info"
        var_value="${!var_name}"
        
        if [ -z "$var_value" ] || [[ "$var_value" == \$* ]]; then
            log_warn "$var_desc ($var_name) 未设置，相关功能可能不可用"
        fi
    done
    
    if [ $errors -gt 0 ]; then
        echo ""
        log_error "发现 $errors 个必需变量未设置，请检查 $ENV_FILE"
        log_error "启动已中止，防止使用空密码运行！"
        exit 1
    fi
    
    log_success "环境变量校验通过"
    echo ""
}

# ==================== 核心功能: 数据库连接预检 ====================
check_database_connection() {
    log_info "[DB-CHECK] 测试数据库连接..."
    
    local db_host="${DB_HOST:-localhost}"
    local db_port="${DB_PORT:-3306}"
    local db_user="${DB_USER:-root}"
    local db_pass="${DB_PASSWORD}"
    
    if command -v mysql &> /dev/null; then
        if mysql -h"$db_host" -P"$db_port" -u"$db_user" -p"$db_pass" -e "SELECT 1;" &>/dev/null; then
            log_success "数据库连接正常 ($db_host:$db_port)"
            return 0
        else
            log_error "数据库连接失败 ($db_host:$db_port)"
            log_error "请检查: 1) 数据库服务是否启动 2) 用户名密码是否正确 3) 防火墙规则"
            return 1
        fi
    else
        log_warn "未安装mysql客户端，跳过数据库连接预检"
        return 0
    fi
    echo ""
}

# ==================== 核心功能: Redis连接预检 ====================
check_redis_connection() {
    log_info "[REDIS-CHECK] 测试Redis连接..."
    
    local redis_host="${REDIS_HOST:-localhost}"
    local redis_port="${REDIS_PORT:-6379}"
    local redis_pass="${REDIS_PASSWORD}"
    
    if command -v redis-cli &> /dev/null; then
        if [ -n "$redis_pass" ]; then
            if redis-cli -h"$redis_host" -p"$redis_port" -a"$redis_pass" ping 2>/dev/null | grep -q "PONG"; then
                log_success "Redis连接正常 ($redis_host:$redis_port)"
                return 0
            else
                log_error "Redis连接失败 ($redis_host:$redis_port)"
                log_error "请检查: 1) Redis服务是否启动 2) 密码是否正确"
                return 1
            fi
        else
            if redis-cli -h"$redis_host" -p"$redis_port" ping 2>/dev/null | grep -q "PONG"; then
                log_success "Redis连接正常 ($redis_host:$redis_port, 无密码)"
                return 0
            else
                log_error "Redis连接失败 ($redis_host:$redis_port)"
                return 1
            fi
        fi
    else
        log_warn "未安装redis-cli，跳过Redis连接预检"
        return 0
    fi
    echo ""
}

# ==================== 主流程开始 ====================
show_banner

cd "$APP_DIR"

# 解析命令行参数
ENV_ARG=""
CHECK_ONLY=false
for arg in "$@"; do
    case $arg in
        dev|test|prod)
            ENV_ARG="$arg"
            ;;
        --check-only)
            CHECK_ONLY=true
            ;;
        *)
            log_warn "未知参数: $arg"
            ;;
    esac
done

# Step 1: 加载环境变量 (配置外置核心!)
load_env_file "$ENV_ARG"

# Step 2: 校验必需环境变量
validate_required_vars

# 如果是仅检查模式，到此结束
if [ "$CHECK_ONLY" = true ]; then
    log_info "环境检查完成，未启动应用 (--check-only 模式)"
    exit 0
fi

# Step 3: 数据库连接预检
if ! check_database_connection; then
    read -p "数据库连接失败，是否继续启动? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        log_info "用户选择不继续，启动已取消"
        exit 1
    fi
fi

# Step 4: Redis连接预检
if ! check_redis_connection; then
    read -p "Redis连接失败，是否继续启动? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        log_info "用户选择不继续，启动已取消"
        exit 1
    fi
fi

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}[1/6] 检查JAR文件${NC}"

if [ ! -f "$JAR_FILE" ]; then
    log_error "未找到 $JAR_FILE"
    echo "请确保部署包已正确解压"
    exit 1
fi
JAR_SIZE=$(ls -lh "$JAR_FILE" | awk '{print $5}')
log_success "JAR文件存在 ($JAR_SIZE)"

echo ""

echo -e "${BLUE}[2/6] 检查Java环境${NC}"

if ! command -v java &> /dev/null; then
    log_error "未找到Java，请安装JDK 17+"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
log_success "Java环境正常 ($JAVA_VERSION)"

echo ""

echo -e "${BLUE}[3/6] 检查端口占用${NC}"

if netstat -tlnp 2>/dev/null | grep -q ":8080"; then
    log_warn "端口8080已被占用"
    echo "正在尝试终止占用进程..."
    PID=$(netstat -tlnp 2>/dev/null | grep ":8080" | awk '{print $7}' | cut -d'/' -f1)
    if [ -n "$PID" ]; then
        log_info "终止进程 PID: $PID"
        kill -15 "$PID" 2>/dev/null || true  # 先尝试优雅停机
        sleep 3
        if kill -0 "$PID" 2>/dev/null; then
            kill -9 "$PID" 2>/dev/null || true  # 强制终止
            sleep 1
        fi
    fi
else
    log_success "端口8080可用"
fi

echo ""

echo -e "${BLUE}[4/6] 准备外部配置${NC}"

# 创建config目录 (如果不存在)
if [ ! -d "$CONFIG_DIR" ]; then
    mkdir -p "$CONFIG_DIR"
    log_info "创建配置目录: $CONFIG_DIR"
fi

# 构建Spring Boot配置参数
SPRING_CONFIG_ARGS="--spring.config.location=classpath:/application.yml"

# 如果存在外部覆盖配置，追加到配置路径
if [ -f "$OVERRIDE_CONFIG" ]; then
    SPRING_CONFIG_ARGS="$SPRING_CONFIG_ARGS,file:///$APP_DIR/$OVERRIDE_CONFIG"
    log_success "检测到外部配置覆盖: $OVERRIDE_CONFIG"
else
    log_info "未找到外部配置覆盖 (可选): $OVERRIDE_CONFIG"
fi

# 追加激活的Profile
if [ -n "$SPRING_PROFILES" ]; then
    SPRING_CONFIG_ARGS="$SPRING_CONFIG_ARGS --spring.profiles.active=$SPRING_PROFILES"
    log_info "激活Profile: $SPRING_PROFILES"
fi

echo ""

echo -e "${BLUE}[5/6] 停止旧实例${NC}"

if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if kill -0 "$OLD_PID" 2>/dev/null; then
        log_info "停止旧实例 (PID: $OLD_PID)..."
        kill -15 "$OLD_PID" 2>/dev/null || true  # SIGTERM优雅停机
        sleep 5
        if kill -0 "$OLD_PID" 2>/dev/null; then
            log_warn "旧实例未响应SIGTERM，强制终止..."
            kill -9 "$OLD_PID" 2>/dev/null || true
            sleep 1
        fi
    fi
    rm -f "$PID_FILE"
else
    log_info "无运行中的实例"
fi

echo ""

echo -e "${BLUE}[6/6] 启动新实例${NC}"
echo ""

# 显示启动参数摘要
log_info "启动参数摘要:"
echo "  JAR文件:     $JAR_FILE"
echo "  JVM参数:     $JAVA_OPTS"
echo "  环境文件:    ${ENV_FILE:-无 (使用默认值)}"
echo "  配置路径:    $SPRING_CONFIG_ARGS"
echo "  日志文件:    $LOG_FILE"
echo ""

# 启动新实例 (核心改动: 支持配置外置!)
nohup java $JAVA_OPTS \
    -jar "$JAR_FILE" \
    $SPRING_CONFIG_ARGS \
    >> "$LOG_FILE" 2>&1 &

NEW_PID=$!
echo $NEW_PID > "$PID_FILE"

log_success "应用正在启动 (PID: $NEW_PID)..."
echo ""

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  启动信息:${NC}"
echo -e "${BLUE}  工作目录:   $APP_DIR${NC}"
echo -e "${BLUE}  日志文件:   $APP_DIR/$LOG_FILE${NC}"
echo -e "${BLUE}  进程ID文件: $APP_DIR/$PID_FILE${NC}"
echo -e "${BLUE}  访问地址:   http://localhost:8080${NC}"
echo -e "${BLUE}  健康检查:   http://localhost:8080/actuator/health${NC}"
echo ""
echo -e "查看启动日志:${NC}"
echo -e "  tail -f $LOG_FILE"
echo ""
echo -e "停止应用:${NC}"
echo -e "  ./stop.sh 或 kill \$(cat $PID_FILE)"
echo ""
echo -e "修改配置后重启:${NC}"
echo -e "  1. 编辑 .env 或 config/application-override.yml"
echo -e "  2. ./stop.sh && ./start.sh"
echo -e "${BLUE}============================================${NC}"
echo ""

# 等待启动完成 (带时间戳)
log_info "等待应用启动..."
START_TIME=$(date +%s)

for i in {1..60}; do
    sleep 1
    echo -n "."

    # 检查进程是否还在运行
    if ! kill -0 $NEW_PID 2>/dev/null; then
        echo ""
        log_error "应用启动失败！进程已退出"
        echo ""
        log_error "错误日志 (最后20行):"
        tail -20 "$LOG_FILE" | while read line; do
            echo -e "  ${RED}$line${NC}"
        done
        exit 1
    fi

    # 每10秒检查一次是否启动成功
    if [ $((i % 10)) -eq 0 ]; then
        if grep -q "北极星AI 极简单体应用启动成功\|Started BeijixingAiApplication" "$LOG_FILE" 2>/dev/null; then
            END_TIME=$(date +%s)
            DURATION=$((END_TIME - START_TIME))
            echo ""
            echo ""
            echo -e "${GREEN}✅✅✅ 应用启动成功！(耗时 ${DURATION}秒) ✅✅✅${NC}"
            echo ""
            echo -e "请访问: ${BLUE}http://localhost:8080${NC}"
            echo ""
            echo -e "健康检查:"
            echo -e "  curl http://localhost:8080/actuator/health"
            echo ""
            
            # 自动执行健康检查
            sleep 2
            log_info "自动执行健康检查..."
            if command -v curl &> /dev/null; then
                HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || echo "000")
                if [ "$HTTP_CODE" = "200" ]; then
                    log_success "健康检查通过 (HTTP $HTTP_CODE)"
                else
                    log_warn "健康检查返回 HTTP $HTTP_CODE (服务可能仍在初始化)"
                fi
            else
                log_warn "未安装curl，跳过健康检查"
            fi
            
            exit 0
        fi
    fi
done

echo ""
echo ""
log_warn "应用可能仍在启动中 (已等待60秒)"
echo -e "请查看日志: ${CYAN}tail -f $LOG_FILE${NC}"
echo ""
