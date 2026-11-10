# 商序云 · 销售运营一体化平台

面向 B2B 销售团队的一体化销售运营平台，覆盖「线索 → 客户 → 商机 → 合同 → 订单 → 出库 → 应收/回款」全链路。

不只管客户，还管**履约与钱**：订单明细、出库发运、应收账款账龄都能在一条链路上追到底。

前端 **Vue3 + Element Plus + ECharts**，后端 **Spring Boot 3 + Spring Security + MyBatis-Plus**，存储 **MySQL + Redis**，交付 **Docker Compose** 一键部署。

## 界面预览

| 登录 | 工作台 |
| --- | --- |
| ![登录](docs/screenshots/login.png) | ![工作台](docs/screenshots/dashboard.png) |

| 客户列表 | 客户 360 |
| --- | --- |
| ![客户列表](docs/screenshots/customers.png) | ![客户360](docs/screenshots/customer-360.png) |

| 商机看板 | 订单管理 |
| --- | --- |
| ![商机看板](docs/screenshots/kanban.png) | ![订单管理](docs/screenshots/orders.png) |

| 出库管理 | 应收管理 |
| --- | --- |
| ![出库管理](docs/screenshots/deliveries.png) | ![应收管理](docs/screenshots/receivables.png) |

| 交易总览 |
| --- |
| ![交易总览](docs/screenshots/trade.png) |

| 发货计划 | 开票管理 |
| --- | --- |
| ![发货计划](docs/screenshots/delivery-plans.png) | ![开票管理](docs/screenshots/invoices.png) |

| 退货退款 | 收款核销 |
| --- | --- |
| ![退货退款](docs/screenshots/returns.png) | ![收款核销](docs/screenshots/payments.png) |

| 登记收款（核销应收） |
| --- |
| ![登记收款](docs/screenshots/payment-form.png) |

| 产品管理 | 产品目录（独立页） |
| --- | --- |
| ![产品管理](docs/screenshots/products.png) | ![产品目录](docs/screenshots/product-categories.png) |

| 产品详情 | 批量导入 |
| --- | --- |
| ![产品详情](docs/screenshots/product-detail.png) | ![批量导入](docs/screenshots/product-import.png) |

| 历史客户（原负责人只读） | 修改记录中心 |
| --- | --- |
| ![历史客户](docs/screenshots/customer-history.png) | ![修改记录](docs/screenshots/change-logs.png) |

| 单据修改记录弹窗 |
| --- |
| ![修改记录弹窗](docs/screenshots/change-log-dialog.png) |

| 历史客户（原负责人只读） | 修改记录中心 |
| --- | --- |
| ![历史客户](docs/screenshots/customer-history.png) | ![修改记录](docs/screenshots/change-logs.png) |

| 单据修改记录弹窗 |
| --- |
| ![修改记录弹窗](docs/screenshots/change-log-dialog.png) |

---

## 一、功能总览

| 模块 | 能力 |
| --- | --- |
| 工作台 | 客户/商机/签约/应收 KPI、销售阶段分布、业绩趋势、客户来源、业绩排行、待办（跟进/出库/收款/审批）、最近跟进 |
| 客户管理 | 客户 CRUD、私海/公海、领取、转移负责人、放入公海、客户 360（联系人/商机/合同/跟进）、下次跟进提醒 |
| 联系人 | 联系人 CRUD、关键联系人标记、按客户/姓名/手机号检索 |
| 线索管理 | 线索 CRUD、状态流转、**线索一键转化**（自动生成客户 + 联系人 + 可选商机） |
| 商机管理 | 商机 CRUD、6 阶段推进与赢单率、列表视图 + **拖拽看板视图**、赢单/输单 |
| 合同管理 | 合同 CRUD、审批流转（草稿→审批中→通过/驳回→签订）、自动生成回款计划 |
| 交易中心 | 交易总览（合同额/订单额/出库额/应收/回款/开票）、交易趋势、六类事件合并的**交易流水**、客户交易排行、链路待办 |
| 订单管理 | 订单 CRUD + **明细行**（产品/数量/单价自动算金额）、提交审批→审核→取消、订单状态与出库/收款状态联动 |
| 发货计划 | 按订单排发货计划（计划日期/数量/仓库）、到期一键转出库单、取消计划 |
| 出库管理 | 由订单**一键生成出库单**（自动带出明细与收货信息）、确认出库、客户签收、作废、物流单号维护 |
| 开票管理 | 开票申请（订单/合同带出金额）、发票号/抬头/税号、6% 税额自动倒算、确认开票与作废 |
| 应收管理 | 应收单（订单/合同生成）、账龄与逾期天数、逾期金额统计 |
| 收款核销 | **唯一记账入口**：收款单 + 核销明细，一单可冲多张应收、支持预收与后续核销、作废逐笔反向冲销、乐观锁防并发覆盖 |
| 附件中心 | 客户/合同等业务对象挂附件（PDF/图片/Office），UUID 存储、鉴权下载、类型与大小校验 |
| 消息通知 | 站内信 + 未读角标，逾期应收、跟进到期、公海回收自动推送 |
| 修改记录 | **全模块字段级变更轨迹**：AOP 自动对比更新前后行快照，字段与码值均显示中文；15 个列表页可按单据查看，系统管理下有全量检索中心 |
| 客户历史视图 | 客户移交/回收后，**原负责人仍可在「历史客户」看到脱敏摘要**（隐藏联系方式、地址、备注与关联金额），所有写操作后端拦截 |
| 导入导出 | 客户 / **产品** CSV 导入（重名跳过、逐行报错、产品提供模板）、客户/线索/应收/**产品**导出（UTF-8 BOM） |
| 单据审批链 | 出库单新增「待审核→已审核」，审核通过**自动生成应收**；应收新增「待确认→已确认」，未确认不可核销；开票校验累计金额防超开 |
| 合同明细 | 合同标的行（产品/服务、数量、单价、金额），金额自动汇总；无明细则保留手填金额（框架合同） |
| 合同额度 | 合同详情展示「合同金额 / 已下单 / 剩余可下单」；订单关联合同下拉只列可下单合同，超额前端提示 + 后端拦截 |
| 批量导出中心 | **独立页面**：客户/线索/应收/产品四类数据，按关键词、状态、级别、行业、来源、客户池、创建时间范围组合查询并分页预览，可勾选行与导出列，支持「导出查询结果」「导出选中项」；沿用统一字典中文与数据权限 |
| 退货退款 | 退货单（关联订单）、退货/退款金额、审核通过/驳回、确认退款，退款金额回写交易流水 |
| 回款管理 | 合同回款计划、回款登记、合同回款状态自动核算（未回款/部分/已回款） |
| 产品目录 | **独立菜单页**：两级树形目录（4 大类 / 11 节点）、增删改停用、节点产品数、右侧查看目录内产品并直接新增 |
| 产品管理 | 产品 CRUD + **详情抽屉**（基本信息 / 描述 / 销售记录 / 附件）、**编码留空自动生成**、目录下拉筛选、上下架、编码唯一与价格校验 |
| 跟进记录 | 按客户/线索/商机/合同记录跟进、下次跟进时间、时间轴展示 |
| 系统管理 | 用户、角色（功能权限 + 数据权限）、部门树、数据字典、操作日志 |
| 认证安全 | JWT 无状态认证、BCrypt 密码、Redis 登录态、退出即失效、接口级权限注解、数据权限隔离 |

### 数据权限模型

| 范围 | 说明 |
| --- | --- |
| `ALL` | 全部数据（管理员、销售总监） |
| `DEPT` | 本部门及下属成员数据（销售经理） |
| `SELF` | 仅本人负责的数据（销售专员） |

公海客户对所有销售可见，不受数据权限限制；领取后进入个人私海。

### 销售履约链路

```
线索 ─转化─▶ 客户 ─▶ 商机 ─赢单─▶ 合同 ─▶ 订单 ─┬─▶ 发货计划 ─▶ 出库单 ─┐
                                              ├─▶ 开票（发票）        ├─▶ 应收单 ─▶ 收款
                                              └─▶ 退货退款 ◀──────────┘
```

- 订单必须**审核通过**才能生成出库单 / 应收单，避免未审先发。
- 出库后自动回写订单「出库状态」（未出库 / 部分出库 / 已出库）。
- 收款后自动回写订单「收款状态」与合同「回款状态」（未收款 / 部分收款 / 已收款）。

### 收款与核销模型

```
收款单（SR）── 1:N ──▶ 核销明细 ── N:1 ──▶ 应收单（AR）
     │                                        │
     └── 未核销部分 = 预收款                    └── 已收金额 / 状态由核销结果推导
```

- **收款单是唯一资金入口**：合同「回款计划」只表达计划，不参与记账；合同实收 = 该合同下所有应收单的核销汇总。
- 一张收款单可拆多条核销明细冲多张应收；未核销部分留作预收，后续可继续核销。
- 作废收款单会逐笔反向冲销，应收余额、订单收款状态、合同回款状态同步回退。
- 核销全程乐观锁（`version`），两人同时操作同一应收单时后者提示刷新重试。
- 单号由 `sys_sequence` 行锁原子取号，替代原来的 `MAX(no)+1`，并发不重号。

---

## 二、技术栈

**后端**

- Spring Boot 3.4.5 / Java 21
- Spring Security 6 + JWT（jjwt 0.12）
- MyBatis-Plus 3.5.7（分页、逻辑删除、自动填充、防全表更新）
- MySQL 8.x、Redis 7（Lettuce 连接池）
- Spring AOP 操作日志、Bean Validation、SpringDoc OpenAPI 3

**前端**

- Vue 3.5（`<script setup>`）+ Vite 5
- Element Plus 2.9 + 全部图标
- Pinia 状态管理、Vue Router 4（hash 模式）
- ECharts 5（漏斗图、趋势图、环形图）
- Axios 拦截器（统一响应、401 自动跳登录）

**部署**

- Docker 多阶段构建（后端 Maven → JRE，前端 Node → Nginx）
- Docker Compose 编排 MySQL / Redis / **双后端实例** / Nginx，含健康检查
- **Nginx 负载均衡**：`least_conn` 分发 + 快速失败超时 + 单 IP 限流 + 节点故障转移，后端不对外暴露端口
- 后端无状态（登录态存 Redis），跨实例 token 可复用；定时任务用 `@ConditionalOnProperty` 只在一个实例注册
- 详见 **[docs/部署-负载均衡.md](docs/部署-负载均衡.md)**

---

## 三、目录结构

```
CRM系统/
├── backend/                        # Spring Boot 后端
│   ├── sql/init.sql                # 建表 + 演示数据（18 客户 / 14 商机 / 10 订单 / 8 应收 …）
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/shangxu/crm/
│       ├── auth/                   # 登录、登出、改密、个人信息
│       ├── common/                 # 统一响应 R、分页、异常处理、健康检查
│       ├── config/                 # MyBatis-Plus、Redis、Jackson、OpenAPI、JWT 配置
│       ├── security/               # JWT 过滤器、登录用户、权限校验、数据权限
│       ├── log/                    # @OperLog 注解 + AOP 切面
│       ├── system/                 # 用户 / 角色 / 部门 / 字典 / 日志
│       ├── customer/               # 客户 + 联系人
│       ├── lead/                   # 线索 + 转化
│       ├── opportunity/            # 商机 + 阶段 / 看板
│       ├── contract/               # 合同 + 回款计划
│       ├── trade/                  # 交易中心（总览 / 流水 / 排行）
│       ├── plan/                   # 发货计划
│       ├── order/                  # 销售订单 + 明细
│       ├── delivery/               # 出库单 + 出库明细
│       ├── invoice/                # 开票管理
│       ├── receivable/             # 应收单
│       ├── payment/                # 收款单 + 核销明细
│       ├── attachment/             # 附件中心
│       ├── notice/                 # 站内通知
│       ├── scheduler/              # 定时任务（逾期/跟进/公海回收）
│       ├── transfer/               # 导入导出 + 批量导出中心
│       ├── returns/                # 退货退款
│       ├── product/                # 产品 + 目录树 + 详情
│       ├── changelog/              # 通用字段级变更轨迹（AOP + 查询接口）
│       ├── activity/               # 跟进记录
│       └── dashboard/              # 工作台统计
├── frontend/                       # Vue3 前端
│   ├── Dockerfile / nginx.conf
│   ├── vite.config.js              # /api 代理到 8080
│   └── src/
│       ├── api/                    # 按模块拆分的接口封装
│       ├── layout/                 # 侧边栏 + 顶栏 + 内容区
│       ├── router/                 # 路由 + 权限守卫
│       ├── store/                  # 用户 / 字典 / 布局
│       ├── components/             # ChartBox、DictTag
│       ├── utils/                  # token、格式化、v-perm 指令
│       └── views/                  # 各业务页面
├── docker-compose.yml
├── deploy.sh                       # 一键构建/启动/日志/停止
├── docs/商序云使用手册.docx         # 使用手册（Word，含封面/目录/页码）
├── docs/使用手册.md                # 使用手册源文件
├── docs/DEPLOY-VM.md               # 虚拟机 Docker 部署详细指南
├── docs/screenshots/               # 界面截图
├── .env.example
└── README.md
```

---

## 四、快速开始

### 方式一：Docker Compose（推荐）

> 在 **VMware / 云服务器（CentOS 7、Ubuntu 22.04）** 上从零安装 Docker 并部署，请直接看
> 👉 **[docs/DEPLOY-VM.md](docs/DEPLOY-VM.md)**（含换源、镜像加速、Compose 安装、共享文件夹传代码、排错）。

已有 Docker 环境时，三条命令即可：

```bash
# 1. 准备环境变量
cp .env.example .env

# 2. 构建并启动（首次约 5-15 分钟，含镜像与依赖下载）
bash deploy.sh
# 等价于：docker compose up -d --build

# 3. 查看状态
docker compose ps
```

> 当前默认编排是 **Nginx + 双后端实例**，入口统一为 `http://localhost`（80）。
> 需要单实例调试时，可在 `docker-compose.yml` 里注释掉 `backend2` 与 upstream 中对应的一行。

启动完成后：

| 服务 | 地址 |
| --- | --- |
| 前端 | http://localhost |
| 后端 API | http://localhost:8080/api |
| 接口文档 | http://localhost:8080/swagger-ui.html |
| MySQL | localhost:3307（root/root） |
| Redis | localhost:6380 |

> 端口冲突时修改 `.env` 中的 `WEB_PORT` / `BACKEND_PORT` / `MYSQL_PORT` / `REDIS_PORT`。

停止与清理：

```bash
docker compose down          # 停止
docker compose down -v       # 停止并清空数据卷（会重置演示数据）
```

### 方式二：本地开发

**前置条件**：JDK 21+、Maven 3.9+、Node 18+、MySQL 8+、Redis 7+

```bash
# 1. 初始化数据库（会创建 shangxu_crm 库并写入演示数据）
mysql -uroot -p < backend/sql/init.sql

# 2. 启动 Redis（默认 localhost:6379）

# 3. 启动后端
cd backend
mvn spring-boot:run
# 如需自定义端口：mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"

# 4. 启动前端
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

数据库连接、Redis 地址、JWT 密钥均可用环境变量覆盖：`MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DB`、`MYSQL_USER`、`MYSQL_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`CRM_JWT_SECRET`、`CRM_SERVER_PORT`。

---

## 五、使用文档

系统提供两本手册，均为带封面、目录与页码的 Word 文档：

| 文档 | 定位 | 适合谁 |
| --- | --- | --- |
| **[商序云使用手册.docx](docs/商序云使用手册.docx)** | 按菜单逐项讲功能与操作步骤、字段含义、权限配置、部署运维 | 需要查某个按钮怎么用 |
| **[商序云业务操作手册.docx](docs/商序云业务操作手册.docx)** | 按业务场景讲「谁、在什么时候、做什么、产出什么单据」，含五个端到端场景、各岗位日常 SOP、业务红线与口径问答 | 新员工上手、岗位交接、搞清单据关系 |

对应的 Markdown 源文件为 `docs/使用手册.md` 与 `docs/业务操作手册.md`；改完执行 `python docs/build_manual_html.py`（可带 `--source/--output/--title` 等参数）再转 Word 即可。

## 六、演示账号

密码统一为 **123456**。

| 账号 | 姓名 | 角色 | 数据权限 |
| --- | --- | --- | --- |
| `admin` | 系统管理员 | 超级管理员 | 全部数据 + 系统配置 |
| `manager01` | 张伟 | 销售总监 | 全部数据 |
| `manager02` | 陈静 | 销售经理 | 本部门数据 |
| `sales01` | 李娜 | 销售专员 | 仅本人数据 |
| `sales02` | 王强 | 销售专员 | 仅本人数据 |
| `sales03` | 刘洋 | 销售经理（销售二部） | 本部门数据 |
| `sales04` | 赵敏 | 销售专员 | 仅本人数据 |
| `service01` | 孙浩 | 售前顾问 | 仅本人数据 |

> 用不同账号登录可直观看到数据权限差异：`sales01` 只能看到自己负责的客户与商机。

---

## 七、接口概览

所有接口以 `/api` 开头，除登录与健康检查外均需请求头 `Authorization: Bearer <token>`。

| 分组 | 主要接口 |
| --- | --- |
| 认证 | `POST /auth/login`、`GET /auth/info`、`POST /auth/logout`、`PUT /auth/password` |
| 工作台 | `GET /dashboard/overview`、`/funnel`、`/trend`、`/rank`、`/source`、`/todo` |
| 客户 | `GET/POST /customers`、`GET /customers/{id}/detail`、`PUT /customers/{id}/transfer`、`/to-public`、`/claim` |
| 联系人 | `GET/POST /contacts`、`PUT/DELETE /contacts/{id}` |
| 线索 | `GET/POST /leads`、`POST /leads/{id}/convert` |
| 商机 | `GET/POST /opportunities`、`GET /opportunities/kanban`、`PUT /opportunities/{id}/stage` |
| 合同 | `GET/POST /contracts`、`PUT /contracts/{id}/status`、`POST /contracts/{id}/payments`、`PUT /contracts/payments/{id}/receive` |
| 产品目录 | `GET /product-categories/tree`、`POST/PUT/DELETE /product-categories`、`PUT /{id}/status` |
| 产品 | `GET /products?categoryId=`、`GET /products/{id}`（含销售统计）、`POST/PUT /products`、`PUT /{id}/status` |
| 交易中心 | `GET /trade/overview`、`/trade/trend`、`/trade/flow`、`/trade/rank` |
| 收款核销 | `GET/POST /payments`、`GET /payments/summary`、`/payments/open-receivables`、`PUT /payments/{id}/allocate`、`/cancel` |
| 附件 | `GET/POST /attachments`、`GET /attachments/{id}/download`、`DELETE /attachments/{id}` |
| 通知 | `GET /notices`、`/notices/unread-count`、`PUT /notices/{id}/read`、`/read-all` |
| 导入导出 | `GET /data/export/{customers,leads,receivables,products,product-template}`、`POST /data/import/{customers,products}` |
| 修改记录 | `GET /change-logs?bizType=&bizId=`、`GET /change-logs/page?bizType=&keyword=` |
| 批量导出 | `GET /data/exports/meta`、`/preview`、`/download`（支持 `fields` 选列、`ids` 选行） |
| 历史客户 | `GET /customers?poolType=HISTORY`（脱敏摘要，只读） |
| 订单 | `GET/POST /orders`、`GET /orders/{id}`、`PUT /orders/{id}/status` |
| 发货计划 | `GET/POST /delivery-plans`、`POST /delivery-plans/from-order/{orderId}`、`PUT /{id}/generate-delivery`、`/cancel` |
| 出库 | `GET /deliveries`、`POST /deliveries/from-order/{orderId}`、`PUT /deliveries/{id}/ship`、`/sign`、`/cancel` |
| 开票 | `GET/POST /invoices`、`POST /invoices/from-order/{orderId}`、`PUT /{id}/issue`、`/void` |
| 退货退款 | `GET/POST /returns`、`PUT /returns/{id}/audit`、`PUT /returns/{id}/refund` |
| 应收 | `GET /receivables`、`GET /receivables/summary`、`POST /receivables/from-order/{orderId}`、`PUT /receivables/{id}/receive` |
| 产品 | `GET/POST /products`、`PUT/DELETE /products/{id}` |
| 跟进 | `GET/POST /activities`、`GET /activities/recent` |
| 系统 | `/system/users`、`/system/roles`、`/system/depts`、`/system/dicts`、`/system/logs` |

完整接口与参数说明见 Swagger：`http://localhost:8080/swagger-ui.html`

统一响应结构：

```json
{ "code": 200, "msg": "操作成功", "data": {} }
```

`401` 未登录/登录失效，`403` 无权限，`500` 业务异常（`msg` 为具体提示）。

---

## 八、关键设计说明

**认证链路**
登录 → `AuthenticationManager` 校验 BCrypt 密码 → 生成 JWT（HS384）→ `LoginUser`（含权限、数据权限）写入 Redis（TTL 8h）→ 后续请求由 `JwtAuthenticationFilter` 解析并回填 `SecurityContext`。退出登录删除 Redis Key，Token 立即失效。

**权限校验**
接口使用 `@PreAuthorize("@perm.has('crm:customer:add')")`，`PermissionChecker` 支持 `*:*:*` 超级权限；前端通过 `v-perm` 指令控制按钮显隐，路由守卫控制页面访问。

**数据权限**
`DataScopeHelper` 根据当前用户角色拼接 `owner_user_id` 条件（`SELF` 本人 / `DEPT` 部门成员 / `ALL` 全部），在客户、线索、商机、合同、跟进查询中统一生效；工作台统计通过 `ownerId/deptId` 参数注入 SQL。

**编号生成**
客户 `KH`、线索 `XS`、商机 `SJ`、合同 `HT`、订单 `SO`、出库单 `DN`、应收单 `AR`，统一「前缀 + 年份 + 4 位序号」，取当前最大序号 +1，避免删除后重号。

**单据联动**
订单审核 → 生成出库单（复制明细、带出收货人/地址）→ 确认出库后回写订单出库状态；订单审核 → 生成应收单 → 收款后回写订单收款状态与合同回款状态。所有联动在同一事务内完成，避免单据状态不一致。

**并发与一致性**
- 单号：`sys_sequence` 行锁原子自增（`REQUIRES_NEW` 独立提交），并发不重号。
- 乐观锁：全表 `version` 字段 + `OptimisticLockerInnerInterceptor`，关键单据（应收、收款、合同、订单）状态流转均校验版本。
- 金额一致性：收款、核销、状态回写在同一事务内完成，任一环节失败整体回滚。

**通用变更轨迹**
- `sys_change_log` 一张表同时承载「全模块字段级修改记录」与「客户负责人移交轨迹」。
- `ChangeLogAspect` 拦截所有带 `@OperLog` 的 INSERT/UPDATE/DELETE 请求，用 `JdbcTemplate` 取更新前后行快照做 diff，业务 Service 零侵入；记录失败只告警不影响主流程。
- `DictDisplayService` 统一把码值转中文：运行时加载 `sys_dict`（带缓存）+ 内置字典（性别、布尔、收款方式、模块名等），按「业务类型 + 字段」精确匹配字典类型，避免 `PART` 这类同码不同义被译错；字段名也统一中文化。
- **CSV 导出同样输出中文**，导入时反向解析回码值，中文或码值都兼容；敏感字段的历史值对只读用户过滤。
- 历史英文记录可用 `backend/sql/patch_dict_display_fix.sql` 一次性订正（幂等，两套库已执行）。
- 客户 `owner_user_id` 变更会额外记 `old_owner_id`，据此实现「历史客户」只读视图；定时任务回收不经过 Controller，由 `recordOwnerChange` 显式补记。

**客户移交后的可见性**
- 原负责人 A 在「我的客户」中不再看到该客户，但「历史客户」仍可见脱敏摘要（名称、编号、级别、行业、当前负责人、状态），隐藏电话/地址/备注/跟进时间。
- 客户 360 详情对 A 返回 `readOnly=true` 且不下发联系人、商机、合同、跟进记录与金额；编辑、删除、转移、放入公海、领取、联系人维护在 `requireWritable` 统一拦截。
- 详情接口同步补上数据范围校验（此前仅列表过滤，按 ID 直查可越权）；领取改为 `WHERE pool_type='PUBLIC'` 条件更新，防止并发抢单。

**定时任务**
`@Scheduled` 三类任务：逾期应收提醒（09:00）、跟进提醒（09:05）、公海自动回收（09:10），均可通过环境变量改 cron 或关闭；多实例部署只开一个实例。

**操作日志**
`@OperLog(title, type)` 注解 + AOP 异步落库，记录操作人、URL、IP、参数、耗时、异常信息。

---

## 九、二次开发提示

- **新增业务模块**：按 `entity / mapper / service / controller` 分层放入 `com.shangxu.crm.<module>`，Mapper 继承 `BaseMapper`，实体继承 `BaseEntity`（自动填充创建人/时间、逻辑删除）。
- **新增权限点**：在 `sys_role.permissions`（JSON 数组）中加入权限标识，并在 `frontend/src/views/system/role.vue` 的 `permissionTree` 中登记，即可在角色配置页勾选。
- **新增字典**：在「系统管理 → 数据字典」新增类型与选项，前端通过 `dictStore.options('your_type')` 直接使用。
- **修改主题色**：`frontend/src/styles/index.css` 中的 `--el-color-primary` 等变量。

---

## 十、常见问题

**1. 后端启动报 `Unknown column 'create_by'`**
数据库为旧版本初始化，重新执行 `backend/sql/init.sql`（会重建表）。

**2. 登录提示「账号或密码错误」，日志显示 Redis 连接失败**
先启动 Redis。演示环境未设置密码，如需密码请配置 `REDIS_PASSWORD`。

**3. 端口被占用**
本机若已有 MySQL/Redis，Docker 版本默认映射到 `3307/6380` 以规避冲突；后端端口通过 `BACKEND_PORT` 或 `--server.port` 修改。

**4. 前端 401 循环跳登录**
确认 `frontend/vite.config.js` 的代理目标与后端端口一致；Docker 部署时确认 Nginx `proxy_pass http://backend:8080/api/`。

---

## 十、说明

本项目为学习与演示用途，业务模型参考主流 SaaS CRM（销售易）的通用能力，未使用任何第三方私有代码。生产使用前请补充：接口限流、密码复杂度策略、审计合规、数据备份、HTTPS 与密钥托管。
