#!/bin/bash
# 北极星AI商机获客系统 - K8s部署脚本
# 作者: 周杰 (EMP-K8S-001)

set -e

NAMESPACE="beijixing"
DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  北极星AI商机获客系统 - K8s部署脚本  ${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 检查kubectl
echo -e "${YELLOW}[1/5] 检查kubectl...${NC}"
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}错误: kubectl未安装${NC}"
    exit 1
fi
echo -e "${GREEN}✓ kubectl已安装${NC}"

# 检查集群连接
echo -e "${YELLOW}[2/5] 检查K8s集群连接...${NC}"
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}错误: 无法连接到K8s集群${NC}"
    exit 1
fi
echo -e "${GREEN}✓ K8s集群连接正常${NC}"

# 创建命名空间
echo -e "${YELLOW}[3/5] 创建命名空间...${NC}"
kubectl apply -f "${DEPLOY_DIR}/namespace.yaml"
echo -e "${GREEN}✓ 命名空间创建完成${NC}"

# 创建配置和密钥
echo -e "${YELLOW}[4/5] 创建ConfigMap和Secret...${NC}"
kubectl apply -f "${DEPLOY_DIR}/configmap.yaml"
kubectl apply -f "${DEPLOY_DIR}/secret.yaml"
echo -e "${GREEN}✓ 配置和密钥创建完成${NC}"

# 部署服务
echo -e "${YELLOW}[5/5] 部署服务...${NC}"
for service in gateway user tenant content lead risk billing ai message storage web; do
    echo -e "  部署 ${service}..."
    kubectl apply -f "${DEPLOY_DIR}/services/${service}-deployment.yaml"
done
echo -e "${GREEN}✓ 所有服务部署完成${NC}"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}       部署完成！                      ${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "查看部署状态:"
echo "  kubectl get pods -n ${NAMESPACE}"
echo "  kubectl get svc -n ${NAMESPACE}"
echo ""
echo "可选操作:"
echo "  1. 部署Ingress: kubectl apply -f ${DEPLOY_DIR}/ingress/ingress.yaml"
echo "  2. 部署监控:   kubectl apply -f ${DEPLOY_DIR}/monitoring/"
echo ""
