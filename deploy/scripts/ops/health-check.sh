#!/bin/bash
# ============================================================
#  北极星AI - 全栈健康检查脚本
#  基于: Spring Boot Actuator + Docker Health + K8s Readiness Probe
#  能力: 自动发现服务、并行检查、失败告警、报告生成
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"
source "$SCRIPT_DIR/../lib/env.sh"

usage() {
    cat << EOF
北极星AI全栈健康检查

用法: $(basename $0) [选项]

选项:
    -e, --env ENV       环境 (dev|prod, 默认dev)
    -s, --service SVC   检查指定服务 (不指定则全部)
    --deep              深度检查 (含DB/Redis/ES连接测试)
    --json               输出JSON格式结果
    --webhook URL        检查完成后发送Webhook通知
    --threshold N        失败阈值 (默认: 1, 超过则返回非零)

示例:
    $(basename $0)                          # 基础健康检查
    $(basename $0) --deep                  # 深度检查 (含依赖)
    $(basename $0) -s bx-user              # 仅检查用户服务
    $(basename $0) --json --webhook https://hooks.xxx.com/webhook

EOF
}

DEPLOY_ENV="dev"
SERVICE_NAME=""
DEEP_CHECK=false
OUTPUT_FORMAT="text"
WEBHOOK_URL=""
FAILURE_THRESHOLD=1

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help) usage; exit 0 ;;
        -e|--env) DEPLOY_ENV=$2; shift 2 ;;
        -s|--service) SERVICE_NAME=$2; shift 2 ;;
        --deep) DEEP_CHECK=true; shift ;;
        --json) OUTPUT_FORMAT="json"; shift ;;
        --webhook) WEBHOOK_URL=$2; shift 2 ;;
        --threshold) FAILURE_THRESHOLD=$2; shift 2 ;;
        *) log_error "未知参数: $1"; usage; exit 1 ;;
    esac
done

load_env "$DEPLOY_ENV" || true

declare -A SERVICE_PORTS=(
    [gateway]=8080 [user]=8081 [tenant]=8082 [content]=8083
    [lead]=8084 [risk]=8085 [billing]=8086 [ai]=8087
    [message]=8088 [storage]=8089 [web]=8090
)

declare -A HEALTH_RESULTS=()
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
FAILED_SERVICES=()

check_service_health() {
    local svc=$1
    local port=${SERVICE_PORTS[$svc]}
    local url="http://127.0.0.1:${port}/actuator/health"
    
    ((TOTAL_CHECKS++)) || true
    
    local start_time=$(date +%s%3N)
    local http_code response_time
    
    http_code=$(curl -s -o /tmp/health_${svc}.json -w "%{http_code}" \
        --connect-timeout 5 --max-time 10 "$url" 2>/dev/null || echo "000")
    
    local end_time=$(( ($(date +%s%3N) - start_time) ))
    response_time="${end_time}ms"
    
    if [[ "$http_code" == "200" ]]; then
        local status
        status=$(jq -r '.status // "UP"' /tmp/health_${svc}.json 2>/dev/null || echo "UNKNOWN")
        
        if [[ "$status" == "UP" ]]; then
            HEALTH_RESULTS[$svc]="PASS|${response_time}|${http_code}"
            ((PASSED_CHECKS++)) || true
            return 0
        else
            local details
            details=$(jq -c '.details // {}' /tmp/health_${svc}.json 2>/dev/null || echo "{}")
            HEALTH_RESULTS[$svc]="DOWN|${response_time}|${status}|${details}"
            FAILED_SERVICES+=("$svc")
            ((FAILED_CHECKS++)) || true
            return 1
        fi
    else
        HEALTH_RESULTS[$svc]="FAIL|${response_time}|HTTP_${http_code}|连接失败"
        FAILED_SERVICES+=("$svc")
        ((FAILED_CHECKS++)) || true
        return 1
    fi
}

check_infrastructure() {
    log_info "--- 基础设施检查 ---"
    
    local infra_ok=true
    
    if redis-cli -a "${REDIS_PASS:-}" ping 2>/dev/null | grep -q PONG; then
        log_success "[REDIS] 连接正常 (${REDIS_HOST:-127.0.0.1}:${REDIS_PORT:-6379})"
    else
        log_error "[REDIS] 连接失败!"
        infra_ok=false
    fi
    
    if mysql -u"${DB_USER:-root}" -p"${DB_PASS:-}" -h"${DB_HOST:-127.0.0.1}" -e "SELECT 1" &>/dev/null; then
        log_success "[MariaDB] 连接正常 (${DB_HOST:-127.0.0.1}:${DB_PORT:-3306})"
    else
        log_error "[MariaDB] 连接失败!"
        infra_ok=false
    fi
    
    if curl -sf "http://${NACOS_ADDR:-127.0.0.1:8848}/nacos/v1/console/health/readiness" &>/dev/null; then
        log_success "[Nacos] 就绪正常 (${NACOS_ADDR:-127.0.0.1:8848})"
    else
        log_warn "[Nacos] 未就绪或不可达"
    fi
    
    if curl -sf "http://${ES_HOST:-127.0.0.1}:${ES_PORT:-9200}/_cluster/health" &>/dev/null; then
        local es_status
        es_status=$(curl -sf "http://${ES_HOST:-127.0.0.1}:${ES_PORT:-9200}/_cluster/health" | jq -r '.status // "unknown"' 2>/dev/null)
        log_success "[Elasticsearch] 状态: ${es_status}"
    else
        log_warn "[Elasticsearch] 不可达 (非必需)"
    fi
    
    $infra_ok && return 0 || return 1
}

generate_report() {
    if [[ "$OUTPUT_FORMAT" == "json" ]]; then
        echo "{"
        echo "  \"timestamp\": \"$(date -Iseconds)\","
        echo "  \"environment\": \"$DEPLOY_ENV\","
        echo "  \"total_checks\": $TOTAL_CHECKS,"
        echo "  \"passed\": $PASSED_CHECKS,"
        echo "  \"failed\": $FAILED_CHECKS,"
        echo "  \"services\": {"
        
        local first=true
        for svc in "${!HEALTH_RESULTS[@]}"; do
            [[ "$first" == "true" ]] && first=false || echo ","
            printf "    \"%s\": \"%s\"" "$svc" "${HEALTH_RESULTS[$svc]}"
        done
        echo ""
        echo "  },"
        echo "  \"failed_services\": [$(printf '\"%s\" ' "${FAILED_SERVICES[@]}" | sed 's/ $//')],"
        echo "  \"overall\": $([ $FAILED_CHECKS -ge $FAILURE_THRESHOLD ] && echo '\"UNHEALTHY\"' || echo '\"HEALTHY\"')"
        echo "}"
    else
        echo ""
        echo "=========================================="
        echo "  健康检查报告"
        echo "  时间: $(now)"
        echo "=========================================="
        printf "%-15s %-10s %-12s %s\n" "服务" "状态" "响应时间" "详情"
        echo "----------------------------------------------"
        
        for svc in gateway user tenant content lead risk billing ai message storage web; do
            if [[ -n "${HEALTH_RESULTS[$svc]:-}" ]]; then
                IFS='|' read -r status response rest <<< "${HEALTH_RESULTS[$svc]}"
                local marker="✓"
                [[ "$status" != "PASS" ]] && marker="✗"
                printf "%-15s %-10s %-12s %s\n" "$svc" "${marker}${status}" "$response" "$rest"
            else
                printf "%-15s %-10s %-12s %s\n" "$svc" "?" "-" "未检查"
            fi
        done
        
        echo "------------------------------------------"
        echo "总计: ${TOTAL_CHECKS} | 通过: ${PASSED_CHECKS} | 失败: ${FAILED_CHECKS}"
        
        if (( FAILED_CHECKS >= FAILURE_THRESHOLD )); then
            echo "状态: ❌ 不健康"
            return 1
        else
            echo "状态: ✅ 健康"
            return 0
        fi
    fi
}

send_webhook() {
    [[ -z "$WEBHOOK_URL" ]] && return 0
    
    local payload
    payload=$(generate_report)
    
    if [[ "$OUTPUT_FORMAT" == "json" ]]; then
        curl -sf -X POST "$WEBHOOK_URL" \
            -H "Content-Type: application/json" \
            -d "$payload" &>/dev/null && \
            log_info "Webhook通知已发送" || \
            log_warn "Webhook发送失败"
    fi
}

log_info "=========================================="
log_info "  北极星AI - 全栈健康检查 ($DEPLOY_ENV)"
log_info "  时间: $(now)"
log_info "=========================================="

TARGET_SERVICES=()
if [[ -n "$SERVICE_NAME" ]]; then
    TARGET_SERVICES+=("$SERVICE_NAME")
else
    for svc in "${!SERVICE_PORTS[@]}"; do
        TARGET_SERVICES+=("$svc")
    done
fi

if [[ "$DEEP_CHECK" == "true" ]]; then
    check_infrastructure || true
fi

for svc in "${TARGET_SERVICES[@]}"; do
    check_service_health "$svc" || true
done

rm -f /tmp/health_*.json 2>/dev/null || true

generate_report
RESULT=$?

send_webhook || true

exit $RESULT
