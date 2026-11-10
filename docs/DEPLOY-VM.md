# 在 VMware 虚拟机上用 Docker 部署「商序云」

> 适用环境：**CentOS 7**（本机 `E:\code\VMware\CentOS 7 64 位.vmx`，4G 内存 / NAT 网络）
> Ubuntu 22.04 的差异见文末「附录 C」。

整套系统由 4 个容器组成：

| 容器 | 镜像 | 端口（宿主） | 说明 |
| --- | --- | --- | --- |
| shangxu-frontend | nginx:1.27-alpine | 80 | 前端静态页 + 反向代理 `/api` |
| shangxu-backend | eclipse-temurin:21-jre-alpine | 8080 | Spring Boot 接口 |
| shangxu-mysql | mysql:8.0 | 3307 | 数据库，首次启动自动执行 `init.sql` |
| shangxu-redis | redis:7-alpine | 6380 | 登录态缓存 |

---

## 0. 开搞前的 3 条检查命令

```bash
cat /etc/redhat-release
free -h
df -h /
nproc
```

判断标准：

- 内存 **≥ 4G**（你这台正好 4G，构建时建议加 2G swap，见附录 B）
- 根分区剩余 **≥ 12G**（镜像 + 构建缓存约 6-8G）
- CPU **≥ 2 核**

磁盘不够先看「附录 A」，否则构建到一半会 `No space left on device`。

---

## 1. 修复 CentOS 7 软件源（必做，否则 yum 全废）

CentOS 7 已于 2024-06-30 停止维护，官方镜像站已下线，必须切到阿里云的 vault 归档源。

```bash
mkdir -p /etc/yum.repos.d/bak
mv /etc/yum.repos.d/CentOS-*.repo /etc/yum.repos.d/bak/ 2>/dev/null

cat > /etc/yum.repos.d/CentOS-Base.repo <<'EOF'
[base]
name=CentOS-7.9 - Base
baseurl=https://mirrors.aliyun.com/centos-vault/7.9.2009/os/$basearch/
gpgcheck=0
enabled=1

[updates]
name=CentOS-7.9 - Updates
baseurl=https://mirrors.aliyun.com/centos-vault/7.9.2009/updates/$basearch/
gpgcheck=0
enabled=1

[extras]
name=CentOS-7.9 - Extras
baseurl=https://mirrors.aliyun.com/centos-vault/7.9.2009/extras/$basearch/
gpgcheck=0
enabled=1
EOF

yum clean all
yum makecache
yum install -y yum-utils device-mapper-persistent-data lvm2
```

验证：`yum repolist` 能看到 base/updates/extras 三个仓库且有包数量。

---

## 2. 安装 Docker Engine

```bash
yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo

# CentOS 7 内核 3.10，Docker 24.0.9 是最后一个稳定支持版本，不要装 latest
yum install -y docker-ce-24.0.9 docker-ce-cli-24.0.9 containerd.io

systemctl enable --now docker
docker version
```

如果提示找不到 24.0.9，先看可用版本再挑：

```bash
yum list docker-ce --showduplicates | sort -r | head
```

安装完成后 `docker version` 应能同时显示 Client 和 Server 两段信息。若 Server 段报错：

```bash
systemctl status docker -l
journalctl -u docker -n 50 --no-pager
```

> 想用普通用户执行 docker 命令：`usermod -aG docker $USER` 后重新登录。本指南统一用 root。

---

## 3. 配置镜像加速（国内必做）

不配加速，`docker pull` 大概率超时。

1. 打开阿里云控制台 → 搜索「容器镜像服务」→ 左侧「镜像工具 → 镜像加速器」
2. 复制形如 `https://xxxxxxxx.mirror.aliyuncs.com` 的专属地址
3. 写入配置：

```bash
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": ["https://xxxxxxxx.mirror.aliyuncs.com"],
  "log-driver": "json-file",
  "log-opts": { "max-size": "100m", "max-file": "3" }
}
EOF

systemctl daemon-reload
systemctl restart docker
docker info | grep -A3 "Registry Mirrors"
```

验证拉取（能下载就说明加速生效）：

```bash
docker pull hello-world && docker run --rm hello-world
```

---

## 4. 安装 Docker Compose v2

**推荐：直接装官方静态二进制（版本新、不依赖系统库）**

```bash
mkdir -p /usr/local/lib/docker/cli-plugins
curl -L https://github.com/docker/compose/releases/download/v2.24.7/docker-compose-linux-x86_64 \
     -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
docker compose version      # 应输出 Docker Compose version v2.24.7
```

> GitHub 下载慢的话，可以在 Windows 上下好这个文件，用 scp/WinSCP 传到
> `/usr/local/lib/docker/cli-plugins/` 并 `chmod +x`。

**备选：用 yum 装插件包**

```bash
yum install -y docker-compose-plugin
docker compose version
```

> CentOS 7 的 docker-ce 源里插件最高只有 **v2.6.0**（Docker 官方已停止为 el7 构建新版本），
> 功能可用但版本较老，所以优先用上面的二进制方式。

> 注意命令是 `docker compose`（空格，v2 插件），不是 `docker-compose`。

---

## 5. 把项目放进虚拟机

### 方式 A：VMware 共享文件夹（推荐，Windows 改代码后可直接同步）

1. 关闭虚拟机 → 虚拟机设置 → **选项** → 共享文件夹 → 启用 → 添加：
   主机路径 `C:\Users\liangfujian\Desktop\CRM系统`，名称 `crm`，勾选「启用此共享」
2. 开机后在虚拟机里执行：

```bash
yum install -y open-vm-tools rsync
mkdir -p /mnt/hgfs
vmhgfs-fuse .host:/ /mnt/hgfs -o allow_other
ls /mnt/hgfs                 # 应看到 crm

mkdir -p /opt/shangxu
rsync -av --exclude node_modules --exclude target --exclude dist \
      /mnt/hgfs/crm/ /opt/shangxu/
```

> 一定要排除 `node_modules`、`target`：Windows 版依赖不能给 Linux 用，而且拷贝极慢。
> 以后 Windows 上改了代码，再执行一次 rsync 即可，然后 `docker compose up -d --build`。

### 方式 B：打包 + scp（不需要共享文件夹）

Windows 的 Git Bash：

```bash
cd "/c/Users/liangfujian/Desktop/CRM系统"
tar --exclude=node_modules --exclude=target --exclude=dist -czf /c/temp/shangxu.tar.gz .
scp /c/temp/shangxu.tar.gz root@192.168.137.128:/opt/
```

虚拟机里：

```bash
mkdir -p /opt/shangxu && tar -xzf /opt/shangxu.tar.gz -C /opt/shangxu
```

（用 WinSCP / Xftp 拖拽压缩包同样可以）

### 方式 C：Windows 临时起 HTTP 服务

```bash
# Windows 端（在项目目录）
python -m http.server 8000
# 虚拟机端（IP 换成 Windows 的 IP，ipconfig 查看）
curl -O http://192.168.1.100:8000/shangxu.tar.gz
```

---

## 6. 配置并启动

```bash
cd /opt/shangxu
cp .env.example .env
vi .env
```

`.env` 需要关注的只有端口和密码：

```ini
MYSQL_ROOT_PASSWORD=root     # 数据库密码，建议改
WEB_PORT=80                  # 前端端口，被占用就改 8081
BACKEND_PORT=8080            # 后端端口
MYSQL_PORT=3307              # 宿主机映射端口（容器内仍是 3306）
REDIS_PORT=6380
```

启动（推荐用脚本，会自动等待健康检查）：

```bash
bash deploy.sh
```

或者手动：

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend     # Ctrl+C 退出，不影响容器
```

首次构建要拉 Maven/Node 镜像并编译，**5-15 分钟**属正常，看到下面这行说明构建成功：

```
[商序云] 后端已就绪
```

---

## 7. 验证部署

```bash
# 1) 四个容器都在跑，mysql / redis 为 healthy
docker compose ps

# 2) 后端健康检查
curl http://localhost/api/health
# {"code":200,"msg":"操作成功","data":{"status":"UP",...}}

# 3) 登录接口
curl -s -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

浏览器打开 `http://<虚拟机IP>`，用 **admin / 123456** 登录。
接口文档：`http://<虚拟机IP>:8080/swagger-ui.html`

---

## 8. 从 Windows 访问虚拟机

你的虚拟机是 **NAT** 模式，Windows 可以直接访问虚拟机 IP：

```bash
# 虚拟机内查看 IP
ip addr | grep "inet 192"
```

Windows 浏览器访问 `http://192.168.x.x` 即可（80 端口）。

**如果访问不了：**

1. 确认容器在跑：`docker compose ps`
2. 虚拟机内自测：`curl -I http://localhost`
3. 临时关防火墙排查：`systemctl stop firewalld`（排查完再开）
4. NAT 端口转发：VMware → 编辑 → 虚拟网络编辑器 → 更改设置 → 选中 VMnet8 → NAT 设置 → 添加映射 `主机 80 → 虚拟机 80`
5. 想让手机/同事也能访问：虚拟机设置 → 网络适配器 → 改为 **桥接模式**，重启网络后 IP 与 Windows 同网段

---

## 9. 日常运维

| 操作 | 命令 |
| --- | --- |
| 查看状态 | `bash deploy.sh --status` |
| 实时日志 | `bash deploy.sh --logs` |
| 只看后端 | `docker compose logs -f backend` |
| 重启全部 | `docker compose restart` |
| 改代码后重新部署 | `docker compose up -d --build` |
| 停止（保留数据） | `bash deploy.sh --down` |
| 停止并清空数据 | `docker compose down -v` |
| 进 MySQL | `docker exec -it shangxu-mysql mysql -uroot -proot shangxu_crm` |
| 备份数据库 | `docker exec shangxu-mysql mysqldump -uroot -proot shangxu_crm > backup_$(date +%F).sql` |
| 恢复数据库 | `docker exec -i shangxu-mysql mysql -uroot -proot shangxu_crm < backup.sql` |

> `down -v` 会删除数据卷，下次启动重新执行 `init.sql`，演示数据回到初始状态。

---

## 10. 常见问题

**① `yum` 报 404 / Cannot find a valid baseurl**
第 1 步没做或 vault 路径写错。`curl -I https://mirrors.aliyun.com/centos-vault/7.9.2009/os/x86_64/` 应返回 200。

**② `docker pull` 一直超时**
镜像加速没配或地址填错（必须是自己的阿里云地址）。`docker info` 里能看到 Registry Mirrors 才算生效。

**③ 构建时进程被 Killed（OOM）**
4G 内存不够用，加 2G swap（附录 B），再执行 `docker compose up -d --build`。

**④ 端口被占用**
`ss -lntp | grep -E ':80|:8080|:3307|:6380'` 查占用，然后改 `.env` 里的 `WEB_PORT` 等，`docker compose up -d` 重建。

**⑤ 打开页面 502 Bad Gateway**
后端还没起或挂了：`docker compose logs --tail=100 backend`。常见原因是 MySQL 没就绪，等 1-2 分钟或看 backend 日志里的 `Communications link failure`。

**⑥ 登录报「系统繁忙」**
后端日志出现 `RedisConnectionFailureException` = Redis 没起来，`docker compose ps` 确认 `shangxu-redis` 是 healthy。

**⑦ 数据库表不存在 / 没有演示数据**
`init.sql` 只在数据卷为空时执行一次。执行 `docker compose down -v && docker compose up -d` 重新初始化。

**⑧ 磁盘写满**
`docker system df` 查看占用 → 附录 A 清理。

**⑨ 时间不对（差 8 小时）**
容器已统一 `TZ=Asia/Shanghai`；若仍不对，检查虚拟机时间：`timedatectl set-timezone Asia/Shanghai && ntpdate ntp.aliyun.com`。

---

## 附录 A：磁盘清理与扩容

```bash
# 1. 看谁占空间
docker system df
du -sh /var/lib/docker/* | sort -h | tail

# 2. 清理构建缓存（构建完执行，能省 1-2G）
docker builder prune -f
docker image prune -f

# 3. 系统层清理
yum clean all
journalctl --vacuum-size=100M
```

扩容（VMware 里先关机扩容硬盘，再开机执行）：

```bash
lsblk                          # 确认根分区是 /dev/sda2 还是 /dev/sda3
yum install -y cloud-utils-growpart
growpart /dev/sda 2            # 把 2 换成实际分区号
xfs_growfs /                   # ext4 文件系统用 resize2fs /dev/sda2
df -h /
```

---

## 附录 B：增加 2G Swap（4G 内存构建时建议）

```bash
dd if=/dev/zero of=/swapfile bs=1M count=2048
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile swap swap defaults 0 0' >> /etc/fstab
free -h
```

---

## 附录 C：Ubuntu 22.04 差异

Ubuntu 不需要修源（用阿里云镜像即可），Docker 安装更简单：

```bash
# 1. 换源（可选）
sed -i 's@//.*archive.ubuntu.com@//mirrors.aliyun.com@g' /etc/apt/sources.list
apt update

# 2. 装 Docker + Compose 插件
apt install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://mirrors.aliyun.com/docker-ce/linux/ubuntu $(lsb_release -cs) stable" \
  > /etc/apt/sources.list.d/docker.list
apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

systemctl enable --now docker
docker compose version
```

第 3 步（镜像加速）、第 5 步（拷代码）、第 6 步（启动）与 CentOS 完全一致。

---

## 附：最小可用命令清单（熟手版）

```bash
# CentOS 7 源
mkdir -p /etc/yum.repos.d/bak && mv /etc/yum.repos.d/CentOS-*.repo /etc/yum.repos.d/bak/
printf '[base]\nname=b\nbaseurl=https://mirrors.aliyun.com/centos-vault/7.9.2009/os/$basearch/\ngpgcheck=0\nenabled=1\n[updates]\nname=u\nbaseurl=https://mirrors.aliyun.com/centos-vault/7.9.2009/updates/$basearch/\ngpgcheck=0\nenabled=1\n[extras]\nname=e\nbaseurl=https://mirrors.aliyun.com/centos-vault/7.9.2009/extras/$basearch/\ngpgcheck=0\nenabled=1\n' > /etc/yum.repos.d/CentOS-Base.repo
yum clean all && yum makecache

# Docker
yum install -y yum-utils
yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
yum install -y docker-ce-24.0.9 docker-ce-cli-24.0.9 containerd.io
systemctl enable --now docker

# Compose v2
mkdir -p /usr/local/lib/docker/cli-plugins
curl -L https://github.com/docker/compose/releases/download/v2.24.7/docker-compose-linux-x86_64 \
     -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 加速器（替换地址）
mkdir -p /etc/docker && echo '{"registry-mirrors":["https://你的ID.mirror.aliyuncs.com"]}' > /etc/docker/daemon.json
systemctl restart docker

# 启动
cd /opt/shangxu && cp .env.example .env && bash deploy.sh
```
