#!/usr/bin/env bash
# ============================================================
#  商序云 一键部署脚本（虚拟机 / 云服务器通用）
#  用法：
#    bash deploy.sh            构建并启动
#    bash deploy.sh --no-build 仅启动（镜像已存在）
#    bash deploy.sh --down     停止并移除容器
#    bash deploy.sh --logs     查看实时日志
#    bash deploy.sh --status   查看状态
# ============================================================
set -euo pipefail

cd "$(dirname "$0")"

GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${GREEN}[商序云]${NC} $1"; }
warn()  { echo -e "${YELLOW}[商序云]${NC} $1"; }
error() { echo -e "${RED}[商序云]${NC} $1"; }

# ---------- 环境检查 ----------
if ! command -v docker >/dev/null 2>&1; then
  error "未检测到 docker，请先安装 Docker Engine（见 docs/DEPLOY-VM.md 第二、三步）"
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  DC="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  DC="docker-compose"
else
  error "未检测到 Docker Compose，请安装 docker-compose-plugin（见 docs/DEPLOY-VM.md 第四步）"
  exit 1
fi

info "Docker: $(docker --version | cut -d',' -f1)"
info "Compose: $($DC version --short 2>/dev/null || echo 'v1/v2')"

# ---------- 参数 ----------
ACTION="up"
BUILD="--build"
case "${1:-}" in
  --down)     ACTION="down" ;;
  --logs)     ACTION="logs" ;;
  --status)   ACTION="status" ;;
  --no-build) BUILD="" ;;
  "")         ;;
  *)          error "未知参数：$1"; exit 1 ;;
esac

# ---------- 配置 ----------
if [ ! -f .env ]; then
  cp .env.example .env
  warn "已根据 .env.example 生成 .env，如需修改端口/密码请先编辑再重启"
fi

# shellcheck disable=SC1091
read_env() {
  grep -E "^$1=" .env | tail -n 1 | cut -d= -f2- | sed -e 's/^"//' -e 's/"$//'
}
WEB_PORT="$(read_env WEB_PORT)"; WEB_PORT="${WEB_PORT:-80}"

case "$ACTION" in
  down)
    info "停止并移除容器..."
    $DC down
    info "已停止（数据卷保留，如需清空数据请执行：$DC down -v）"
    exit 0
    ;;
  logs)
    $DC logs -f --tail=200
    exit 0
    ;;
  status)
    $DC ps
    exit 0
    ;;
esac

# ---------- 启动 ----------
info "开始构建并启动（首次构建约 5-15 分钟，取决于网络与机器性能）..."
$DC up -d $BUILD

info "等待后端实例健康检查..."
for svc in backend1 backend2; do
  for i in $(seq 1 60); do
    HEALTH=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "shangxu-$svc" 2>/dev/null || echo "starting")
    if [ "$HEALTH" = "healthy" ]; then
      info "$svc 已就绪"
      break
    fi
    if [ "$HEALTH" = "unhealthy" ]; then
      error "$svc 健康检查失败，请查看日志：$DC logs $svc"
      exit 1
    fi
    sleep 5
    [ "$i" = "60" ] && warn "等待 $svc 超时，请手动查看：$DC logs -f $svc"
  done
done

echo
$DC ps
echo
IP=$(hostname -I 2>/dev/null | awk '{print $1}')
info "部署完成，访问地址："
echo "    前端      http://${IP:-<虚拟机IP>}${WEB_PORT:+:$WEB_PORT}"
echo "    接口文档  http://${IP:-<虚拟机IP>}${WEB_PORT:+:$WEB_PORT}/swagger-ui.html"
echo "    演示账号  admin / 123456"
echo
info "常用命令：bash deploy.sh --logs | --status | --down"
