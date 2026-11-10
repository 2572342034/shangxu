-- =============================================================
--  商序云 · 销售运营一体化平台  |  数据库初始化脚本
--  MySQL 8.0+ / 9.x    charset: utf8mb4
--  默认账号: admin / 123456
-- =============================================================

CREATE DATABASE IF NOT EXISTS `shangxu_crm` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `shangxu_crm`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------- 部门 ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级部门',
  `name`        VARCHAR(64)  NOT NULL COMMENT '部门名称',
  `leader`      VARCHAR(32)           DEFAULT NULL COMMENT '负责人',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1正常 0停用',
  `create_by`   VARCHAR(64)           DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';

-- ---------------------------- 角色 ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(64)  NOT NULL COMMENT '角色名称',
  `code`        VARCHAR(64)  NOT NULL COMMENT '角色编码',
  `data_scope`  VARCHAR(16)  NOT NULL DEFAULT 'SELF' COMMENT '数据权限 ALL/DEPT/SELF',
  `permissions` TEXT         COMMENT '权限标识 JSON 数组',
  `sort`        INT          NOT NULL DEFAULT 0,
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `remark`      VARCHAR(255)          DEFAULT NULL,
  `create_by`   VARCHAR(64)           DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- ---------------------------- 用户 ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `username`        VARCHAR(64)  NOT NULL COMMENT '登录账号',
  `password`        VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码',
  `nickname`        VARCHAR(64)  NOT NULL COMMENT '姓名',
  `avatar`          VARCHAR(255)          DEFAULT NULL,
  `mobile`          VARCHAR(32)           DEFAULT NULL,
  `email`           VARCHAR(128)          DEFAULT NULL,
  `dept_id`         BIGINT                DEFAULT NULL,
  `post`            VARCHAR(64)           DEFAULT NULL COMMENT '岗位',
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0停用',
  `last_login_time` DATETIME              DEFAULT NULL,
  `create_by`       VARCHAR(64)           DEFAULT NULL,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- ---------------------------- 数据字典 ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `dict_type`   VARCHAR(64)  NOT NULL COMMENT '字典类型',
  `dict_label`  VARCHAR(64)  NOT NULL COMMENT '显示名',
  `dict_value`  VARCHAR(64)  NOT NULL COMMENT '字典值',
  `sort`        INT          NOT NULL DEFAULT 0,
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `remark`      VARCHAR(255)          DEFAULT NULL,
  `create_by`   VARCHAR(64)           DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典';

-- ---------------------------- 操作日志 ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `title`          VARCHAR(128)          DEFAULT NULL COMMENT '模块标题',
  `business_type`  VARCHAR(32)           DEFAULT NULL COMMENT '操作类型',
  `method`         VARCHAR(255)          DEFAULT NULL,
  `request_method` VARCHAR(16)           DEFAULT NULL,
  `oper_name`      VARCHAR(64)           DEFAULT NULL,
  `oper_url`       VARCHAR(255)          DEFAULT NULL,
  `oper_ip`        VARCHAR(64)           DEFAULT NULL,
  `oper_param`     TEXT,
  `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
  `error_msg`      TEXT,
  `cost_time`      BIGINT                DEFAULT 0,
  `oper_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_time` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ---------------------------- 客户 ----------------------------
DROP TABLE IF EXISTS `crm_customer`;
CREATE TABLE `crm_customer` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `customer_no`      VARCHAR(32)  NOT NULL COMMENT '客户编号',
  `name`             VARCHAR(128) NOT NULL COMMENT '客户名称',
  `short_name`       VARCHAR(64)           DEFAULT NULL,
  `industry`         VARCHAR(32)           DEFAULT NULL COMMENT '所属行业',
  `level`            VARCHAR(8)   NOT NULL DEFAULT 'C' COMMENT '客户级别 A/B/C',
  `source`           VARCHAR(32)           DEFAULT NULL COMMENT '客户来源',
  `scale`            VARCHAR(32)           DEFAULT NULL COMMENT '人员规模',
  `phone`            VARCHAR(32)           DEFAULT NULL,
  `website`          VARCHAR(128)          DEFAULT NULL,
  `province`         VARCHAR(32)           DEFAULT NULL,
  `city`             VARCHAR(32)           DEFAULT NULL,
  `address`          VARCHAR(255)          DEFAULT NULL,
  `owner_user_id`    BIGINT                DEFAULT NULL COMMENT '负责人',
  `dept_id`          BIGINT                DEFAULT NULL COMMENT '所属部门',
  `pool_type`        VARCHAR(16)  NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE私海 PUBLIC公海',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'FOLLOWING' COMMENT 'FOLLOWING跟进中 DEAL已成交 INVALID无效',
  `next_follow_time` DATETIME              DEFAULT NULL,
  `last_follow_time` DATETIME              DEFAULT NULL,
  `remark`           VARCHAR(500)          DEFAULT NULL,
  `create_by`        VARCHAR(64)           DEFAULT NULL,
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`customer_no`),
  KEY `idx_owner` (`owner_user_id`),
  KEY `idx_name` (`name`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户';

-- ---------------------------- 联系人 ----------------------------
DROP TABLE IF EXISTS `crm_contact`;
CREATE TABLE `crm_contact` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `customer_id` BIGINT       NOT NULL COMMENT '所属客户',
  `name`        VARCHAR(64)  NOT NULL COMMENT '姓名',
  `post`        VARCHAR(64)           DEFAULT NULL COMMENT '职务',
  `mobile`      VARCHAR(32)           DEFAULT NULL,
  `phone`       VARCHAR(32)           DEFAULT NULL,
  `email`       VARCHAR(128)          DEFAULT NULL,
  `wechat`      VARCHAR(64)           DEFAULT NULL,
  `gender`      VARCHAR(8)            DEFAULT NULL COMMENT 'MALE/FEMALE',
  `is_primary`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否关键联系人',
  `birthday`    DATE                  DEFAULT NULL,
  `remark`      VARCHAR(500)          DEFAULT NULL,
  `create_by`   VARCHAR(64)           DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联系人';

-- ---------------------------- 线索 ----------------------------
DROP TABLE IF EXISTS `crm_lead`;
CREATE TABLE `crm_lead` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
  `lead_no`               VARCHAR(32)  NOT NULL,
  `name`                  VARCHAR(128) NOT NULL COMMENT '线索名称',
  `company`               VARCHAR(128)          DEFAULT NULL COMMENT '公司',
  `contact_name`          VARCHAR(64)           DEFAULT NULL,
  `mobile`                VARCHAR(32)           DEFAULT NULL,
  `email`                 VARCHAR(128)          DEFAULT NULL,
  `source`                VARCHAR(32)           DEFAULT NULL,
  `industry`              VARCHAR(32)           DEFAULT NULL,
  `status`                VARCHAR(16)  NOT NULL DEFAULT 'NEW' COMMENT 'NEW/FOLLOWING/QUALIFIED/INVALID/CONVERTED',
  `owner_user_id`         BIGINT                DEFAULT NULL,
  `dept_id`               BIGINT                DEFAULT NULL,
  `converted_customer_id` BIGINT                DEFAULT NULL,
  `converted_time`        DATETIME              DEFAULT NULL,
  `remark`                VARCHAR(500)          DEFAULT NULL,
  `create_by`             VARCHAR(64)           DEFAULT NULL,
  `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`               TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`lead_no`),
  KEY `idx_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线索';

-- ---------------------------- 商机 ----------------------------
DROP TABLE IF EXISTS `crm_opportunity`;
CREATE TABLE `crm_opportunity` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `opp_no`            VARCHAR(32)  NOT NULL,
  `name`              VARCHAR(128) NOT NULL,
  `customer_id`       BIGINT       NOT NULL,
  `customer_name`     VARCHAR(128)          DEFAULT NULL,
  `amount`            DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '预计金额',
  `stage`             VARCHAR(32)  NOT NULL DEFAULT 'CONTACT' COMMENT 'CONTACT/DEMAND/QUOTE/NEGOTIATION/WON/LOST',
  `win_rate`          INT          NOT NULL DEFAULT 10 COMMENT '赢单率%',
  `expect_deal_date`  DATE                  DEFAULT NULL,
  `actual_deal_date`  DATE                  DEFAULT NULL,
  `status`            VARCHAR(16)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/WON/LOST',
  `source`            VARCHAR(32)           DEFAULT NULL,
  `owner_user_id`     BIGINT                DEFAULT NULL,
  `dept_id`           BIGINT                DEFAULT NULL,
  `remark`            VARCHAR(500)          DEFAULT NULL,
  `create_by`         VARCHAR(64)           DEFAULT NULL,
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`           TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`opp_no`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_owner` (`owner_user_id`),
  KEY `idx_stage` (`stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机';

-- ---------------------------- 产品 ----------------------------
DROP TABLE IF EXISTS `crm_product`;
CREATE TABLE `crm_product` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(32)  NOT NULL,
  `name`        VARCHAR(128) NOT NULL,
  `category`    VARCHAR(32)           DEFAULT NULL,
  `unit`        VARCHAR(16)           DEFAULT NULL,
  `price`       DECIMAL(18,2) NOT NULL DEFAULT 0,
  `cost`        DECIMAL(18,2) NOT NULL DEFAULT 0,
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `remark`      VARCHAR(500)          DEFAULT NULL,
  `create_by`   VARCHAR(64)           DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品';

-- ---------------------------- 合同 ----------------------------
DROP TABLE IF EXISTS `crm_contract`;
CREATE TABLE `crm_contract` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `contract_no`    VARCHAR(32)  NOT NULL,
  `name`           VARCHAR(128) NOT NULL,
  `customer_id`    BIGINT       NOT NULL,
  `customer_name`  VARCHAR(128)          DEFAULT NULL,
  `opportunity_id` BIGINT                DEFAULT NULL,
  `amount`         DECIMAL(18,2) NOT NULL DEFAULT 0,
  `sign_date`      DATE                  DEFAULT NULL,
  `start_date`     DATE                  DEFAULT NULL,
  `end_date`       DATE                  DEFAULT NULL,
  `status`         VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/APPROVED/REJECTED/SIGNED/VOID',
  `payment_status` VARCHAR(16)  NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PART/PAID',
  `owner_user_id`  BIGINT                DEFAULT NULL,
  `dept_id`        BIGINT                DEFAULT NULL,
  `remark`         VARCHAR(500)          DEFAULT NULL,
  `create_by`      VARCHAR(64)           DEFAULT NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`contract_no`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同';

-- ---------------------------- 回款计划 ----------------------------
DROP TABLE IF EXISTS `crm_payment_plan`;
CREATE TABLE `crm_payment_plan` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `contract_id`   BIGINT       NOT NULL,
  `period`        INT          NOT NULL DEFAULT 1 COMMENT '期数',
  `plan_amount`   DECIMAL(18,2) NOT NULL DEFAULT 0,
  `plan_date`     DATE                  DEFAULT NULL,
  `actual_amount` DECIMAL(18,2) NOT NULL DEFAULT 0,
  `actual_date`   DATE                  DEFAULT NULL,
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PAID',
  `remark`        VARCHAR(255)          DEFAULT NULL,
  `create_by`     VARCHAR(64)           DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_contract` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回款计划';

-- ---------------------------- 跟进记录 ----------------------------
DROP TABLE IF EXISTS `crm_activity`;
CREATE TABLE `crm_activity` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `biz_type`         VARCHAR(16)  NOT NULL COMMENT 'CUSTOMER/LEAD/OPPORTUNITY/CONTRACT',
  `biz_id`           BIGINT       NOT NULL,
  `biz_name`         VARCHAR(128)          DEFAULT NULL,
  `type`             VARCHAR(16)  NOT NULL DEFAULT 'CALL' COMMENT 'CALL/VISIT/WECHAT/EMAIL/MEETING/OTHER',
  `content`          VARCHAR(1000) NOT NULL,
  `next_follow_time` DATETIME              DEFAULT NULL,
  `owner_user_id`    BIGINT                DEFAULT NULL,
  `create_by`        VARCHAR(64)           DEFAULT NULL,
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跟进记录';

-- ---------------------------- 销售订单 ----------------------------
DROP TABLE IF EXISTS `crm_order`;
CREATE TABLE `crm_order` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `order_no`          VARCHAR(32)  NOT NULL COMMENT '订单编号',
  `title`             VARCHAR(128) NOT NULL COMMENT '订单标题',
  `customer_id`       BIGINT       NOT NULL,
  `customer_name`     VARCHAR(128)          DEFAULT NULL,
  `contract_id`       BIGINT                DEFAULT NULL,
  `contract_no`       VARCHAR(32)           DEFAULT NULL,
  `amount`            DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '订单金额',
  `status`            VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/APPROVED/REJECTED/CANCELLED',
  `order_date`        DATE                  DEFAULT NULL,
  `delivery_status`   VARCHAR(16)  NOT NULL DEFAULT 'UNDELIVERED' COMMENT 'UNDELIVERED/PART/DELIVERED',
  `receivable_status` VARCHAR(16)  NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PART/PAID',
  `owner_user_id`     BIGINT                DEFAULT NULL,
  `dept_id`           BIGINT                DEFAULT NULL,
  `remark`            VARCHAR(500)          DEFAULT NULL,
  `create_by`         VARCHAR(64)           DEFAULT NULL,
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`           TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单';

DROP TABLE IF EXISTS `crm_order_item`;
CREATE TABLE `crm_order_item` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `order_id`     BIGINT       NOT NULL,
  `product_id`   BIGINT                DEFAULT NULL,
  `product_name` VARCHAR(128) NOT NULL,
  `unit`         VARCHAR(16)           DEFAULT NULL,
  `quantity`     DECIMAL(18,2) NOT NULL DEFAULT 1,
  `price`        DECIMAL(18,2) NOT NULL DEFAULT 0,
  `amount`       DECIMAL(18,2) NOT NULL DEFAULT 0,
  `remark`       VARCHAR(255)          DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

-- ---------------------------- 出库单 ----------------------------
DROP TABLE IF EXISTS `crm_delivery`;
CREATE TABLE `crm_delivery` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `delivery_no`       VARCHAR(32)  NOT NULL COMMENT '出库单号',
  `order_id`          BIGINT       NOT NULL,
  `order_no`          VARCHAR(32)           DEFAULT NULL,
  `customer_id`       BIGINT                DEFAULT NULL,
  `customer_name`     VARCHAR(128)          DEFAULT NULL,
  `delivery_date`     DATE                  DEFAULT NULL,
  `warehouse`         VARCHAR(32)           DEFAULT NULL COMMENT '发货仓库',
  `total_qty`         DECIMAL(18,2) NOT NULL DEFAULT 0,
  `amount`            DECIMAL(18,2) NOT NULL DEFAULT 0,
  `status`            VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SHIPPED/SIGNED/CANCELLED',
  `logistics_company` VARCHAR(64)           DEFAULT NULL,
  `logistics_no`      VARCHAR(64)           DEFAULT NULL,
  `receiver`          VARCHAR(64)           DEFAULT NULL,
  `receiver_phone`    VARCHAR(32)           DEFAULT NULL,
  `address`           VARCHAR(255)          DEFAULT NULL,
  `owner_user_id`     BIGINT                DEFAULT NULL,
  `dept_id`           BIGINT                DEFAULT NULL,
  `remark`            VARCHAR(500)          DEFAULT NULL,
  `create_by`         VARCHAR(64)           DEFAULT NULL,
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`           TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_delivery_no` (`delivery_no`),
  KEY `idx_order` (`order_id`),
  KEY `idx_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单';

DROP TABLE IF EXISTS `crm_delivery_item`;
CREATE TABLE `crm_delivery_item` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `delivery_id`  BIGINT       NOT NULL,
  `product_name` VARCHAR(128) NOT NULL,
  `unit`         VARCHAR(16)           DEFAULT NULL,
  `quantity`     DECIMAL(18,2) NOT NULL DEFAULT 0,
  `remark`       VARCHAR(255)          DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_delivery` (`delivery_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库明细';

-- ---------------------------- 应收单 ----------------------------
DROP TABLE IF EXISTS `crm_receivable`;
CREATE TABLE `crm_receivable` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `receivable_no`   VARCHAR(32)  NOT NULL COMMENT '应收单号',
  `customer_id`     BIGINT       NOT NULL,
  `customer_name`   VARCHAR(128)          DEFAULT NULL,
  `source_type`     VARCHAR(16)  NOT NULL DEFAULT 'ORDER' COMMENT 'ORDER/CONTRACT',
  `source_id`       BIGINT                DEFAULT NULL,
  `source_no`       VARCHAR(32)           DEFAULT NULL,
  `amount`          DECIMAL(18,2) NOT NULL DEFAULT 0,
  `received_amount` DECIMAL(18,2) NOT NULL DEFAULT 0,
  `receive_time`    DATETIME              DEFAULT NULL COMMENT '最近收款时间',
  `due_date`        DATE                  DEFAULT NULL COMMENT '应收日期',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PART/PAID',
  `owner_user_id`   BIGINT                DEFAULT NULL,
  `dept_id`         BIGINT                DEFAULT NULL,
  `remark`          VARCHAR(500)          DEFAULT NULL,
  `create_by`       VARCHAR(64)           DEFAULT NULL,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receivable_no` (`receivable_no`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收单';

-- ---------------------------- 发货计划 ----------------------------
DROP TABLE IF EXISTS `crm_delivery_plan`;
CREATE TABLE `crm_delivery_plan` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `plan_no`       VARCHAR(32)  NOT NULL COMMENT '计划编号',
  `order_id`      BIGINT       NOT NULL,
  `order_no`      VARCHAR(32)           DEFAULT NULL,
  `customer_id`   BIGINT                DEFAULT NULL,
  `customer_name` VARCHAR(128)          DEFAULT NULL,
  `plan_date`     DATE                  DEFAULT NULL COMMENT '计划发货日期',
  `plan_qty`      DECIMAL(18,2) NOT NULL DEFAULT 0,
  `amount`        DECIMAL(18,2) NOT NULL DEFAULT 0,
  `warehouse`     VARCHAR(32)           DEFAULT NULL,
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SHIPPED/CANCELLED',
  `delivery_id`   BIGINT                DEFAULT NULL COMMENT '生成出库单后回写',
  `owner_user_id` BIGINT                DEFAULT NULL,
  `dept_id`       BIGINT                DEFAULT NULL,
  `remark`        VARCHAR(500)          DEFAULT NULL,
  `create_by`     VARCHAR(64)           DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_no` (`plan_no`),
  KEY `idx_order` (`order_id`),
  KEY `idx_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发货计划';

-- ---------------------------- 发票 ----------------------------
DROP TABLE IF EXISTS `crm_invoice`;
CREATE TABLE `crm_invoice` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `invoice_no`    VARCHAR(64)           DEFAULT NULL COMMENT '发票号码',
  `title`         VARCHAR(128)          DEFAULT NULL COMMENT '发票抬头',
  `tax_no`        VARCHAR(64)           DEFAULT NULL COMMENT '纳税人识别号',
  `customer_id`   BIGINT                DEFAULT NULL,
  `customer_name` VARCHAR(128)          DEFAULT NULL,
  `source_type`   VARCHAR(16)  NOT NULL DEFAULT 'ORDER' COMMENT 'ORDER/CONTRACT',
  `source_id`     BIGINT                DEFAULT NULL,
  `source_no`     VARCHAR(32)           DEFAULT NULL,
  `amount`        DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '价税合计',
  `tax_rate`      DECIMAL(5,2)  NOT NULL DEFAULT 6.00,
  `tax_amount`    DECIMAL(18,2) NOT NULL DEFAULT 0,
  `type`          VARCHAR(16)  NOT NULL DEFAULT 'SPECIAL' COMMENT 'SPECIAL/NORMAL/E_INVOICE',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ISSUED/VOID',
  `invoice_date`  DATE                  DEFAULT NULL,
  `owner_user_id` BIGINT                DEFAULT NULL,
  `dept_id`       BIGINT                DEFAULT NULL,
  `remark`        VARCHAR(500)          DEFAULT NULL,
  `create_by`     VARCHAR(64)           DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票';

-- ---------------------------- 退货退款 ----------------------------
DROP TABLE IF EXISTS `crm_return`;
CREATE TABLE `crm_return` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `return_no`     VARCHAR(32)  NOT NULL COMMENT '退货单号',
  `order_id`      BIGINT                DEFAULT NULL,
  `order_no`      VARCHAR(32)           DEFAULT NULL,
  `customer_id`   BIGINT                DEFAULT NULL,
  `customer_name` VARCHAR(128)          DEFAULT NULL,
  `return_date`   DATE                  DEFAULT NULL,
  `return_qty`    DECIMAL(18,2) NOT NULL DEFAULT 0,
  `return_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '退货金额',
  `refund_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '退款金额',
  `reason`        VARCHAR(255)          DEFAULT NULL,
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/REFUNDED',
  `refund_time`   DATETIME              DEFAULT NULL,
  `owner_user_id` BIGINT                DEFAULT NULL,
  `dept_id`       BIGINT                DEFAULT NULL,
  `remark`        VARCHAR(500)          DEFAULT NULL,
  `create_by`     VARCHAR(64)           DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_return_no` (`return_no`),
  KEY `idx_order` (`order_id`),
  KEY `idx_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退货退款单';

-- 1. 收款单（唯一记账入口）
CREATE TABLE `crm_payment_record` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
  `record_no`          VARCHAR(32)  NOT NULL COMMENT '收款单号 SR+年+4位',
  `customer_id`        BIGINT       NOT NULL,
  `customer_name`      VARCHAR(128) NOT NULL,
  `amount`             DECIMAL(18,2) NOT NULL COMMENT '收款金额',
  `allocated_amount`   DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已核销金额',
  `unallocated_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '未核销（预收）金额',
  `receive_date`       DATE         NOT NULL COMMENT '收款日期',
  `method`             VARCHAR(16)  NOT NULL DEFAULT 'BANK' COMMENT 'BANK/CASH/ACCEPTANCE/ONLINE/OTHER',
  `bank_account`       VARCHAR(64)           DEFAULT NULL COMMENT '收款账户',
  `voucher_no`         VARCHAR(64)           DEFAULT NULL COMMENT '银行流水号/凭证号',
  `source_type`        VARCHAR(16)           DEFAULT 'MANUAL' COMMENT 'MANUAL/ORDER/CONTRACT',
  `source_id`          BIGINT                DEFAULT NULL,
  `source_no`          VARCHAR(32)           DEFAULT NULL,
  `status`             VARCHAR(16)  NOT NULL DEFAULT 'CONFIRMED' COMMENT 'CONFIRMED/CANCELLED',
  `owner_user_id`      BIGINT                DEFAULT NULL,
  `dept_id`            BIGINT                DEFAULT NULL,
  `remark`             VARCHAR(500)          DEFAULT NULL,
  `version`            INT          NOT NULL DEFAULT 0,
  `create_by`          VARCHAR(64)           DEFAULT NULL,
  `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`            TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_record_no` (`record_no`),
  KEY `idx_payment_customer` (`customer_id`),
  KEY `idx_payment_date` (`receive_date`),
  KEY `idx_payment_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款单';

-- 2. 核销明细（收款单 × 应收单）
CREATE TABLE `crm_payment_allocation` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `record_id`      BIGINT        NOT NULL COMMENT '收款单ID',
  `receivable_id`  BIGINT        NOT NULL COMMENT '应收单ID',
  `receivable_no`  VARCHAR(32)            DEFAULT NULL,
  `amount`         DECIMAL(18,2) NOT NULL COMMENT '本次核销金额',
  `version`        INT           NOT NULL DEFAULT 0,
  `create_by`      VARCHAR(64)            DEFAULT NULL,
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_alloc_record` (`record_id`),
  KEY `idx_alloc_receivable` (`receivable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款核销明细';

-- 3. 附件中心
CREATE TABLE `sys_attachment` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `biz_type`       VARCHAR(32)  NOT NULL COMMENT 'CUSTOMER/CONTRACT/ORDER/INVOICE/RETURN/OPPORTUNITY',
  `biz_id`         BIGINT       NOT NULL,
  `original_name`  VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_name`      VARCHAR(255) NOT NULL COMMENT '存储文件名（UUID）',
  `file_size`      BIGINT       NOT NULL DEFAULT 0,
  `content_type`   VARCHAR(128)          DEFAULT NULL,
  `storage_path`   VARCHAR(500) NOT NULL COMMENT '相对存储路径',
  `uploader_id`    BIGINT                DEFAULT NULL,
  `uploader_name`  VARCHAR(64)           DEFAULT NULL,
  `version`        INT          NOT NULL DEFAULT 0,
  `create_by`      VARCHAR(64)           DEFAULT NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_attach_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件';

-- 4. 站内通知
CREATE TABLE `sys_notice` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       NOT NULL COMMENT '接收人',
  `title`        VARCHAR(200) NOT NULL,
  `content`      VARCHAR(1000)         DEFAULT NULL,
  `type`         VARCHAR(32)  NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM/RECEIVABLE/FOLLOWUP/APPROVAL/POOL',
  `biz_type`     VARCHAR(32)           DEFAULT NULL,
  `biz_id`       BIGINT                DEFAULT NULL,
  `biz_no`       VARCHAR(64)           DEFAULT NULL,
  `read_flag`    TINYINT      NOT NULL DEFAULT 0,
  `read_time`    DATETIME              DEFAULT NULL,
  `version`      INT          NOT NULL DEFAULT 0,
  `create_by`    VARCHAR(64)           DEFAULT NULL,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_notice_user` (`user_id`, `read_flag`),
  KEY `idx_notice_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';

-- 5. 单号序列（替代 MAX(no)+1，并发安全）
CREATE TABLE `sys_sequence` (
  `seq_key`      VARCHAR(64) NOT NULL COMMENT '业务键，如 customer/order',
  `current_val`  BIGINT      NOT NULL DEFAULT 0,
  `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`seq_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单号序列';

-- 6. 全表乐观锁字段
ALTER TABLE `sys_dept`              ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `sys_role`              ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `sys_user`              ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `sys_dict`              ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `sys_log`               ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_customer`          ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_contact`           ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_lead`              ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_opportunity`       ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_product`           ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_contract`          ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_payment_plan`      ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_activity`          ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_order`             ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_order_item`        ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_delivery`          ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_delivery_item`     ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_receivable`        ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_delivery_plan`     ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_invoice`           ADD COLUMN `version` INT NOT NULL DEFAULT 0;
ALTER TABLE `crm_return`            ADD COLUMN `version` INT NOT NULL DEFAULT 0;

-- 7. 应收单补充「已核销来源」便于对账（可选冗余）
ALTER TABLE `crm_receivable` ADD COLUMN `allocated_count` INT NOT NULL DEFAULT 0 COMMENT '核销笔数';

-- ---------------------------- 产品目录（树形分类） ----------------------------
CREATE TABLE `crm_product_category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父级ID，0 为根节点',
  `name`        VARCHAR(64)  NOT NULL COMMENT '分类名称',
  `code`        VARCHAR(64)           DEFAULT NULL COMMENT '分类编码',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `remark`      VARCHAR(255)          DEFAULT NULL,
  `version`     INT          NOT NULL DEFAULT 0,
  `create_by`   VARCHAR(64)           DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_pc_parent` (`parent_id`),
  KEY `idx_pc_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品目录';

-- ---------------------------- 产品扩展字段 ----------------------------
ALTER TABLE `crm_product` ADD COLUMN `category_id`  BIGINT        DEFAULT NULL COMMENT '所属目录ID' AFTER `category`;
ALTER TABLE `crm_product` ADD COLUMN `spec`         VARCHAR(128)  DEFAULT NULL COMMENT '规格型号' AFTER `unit`;
ALTER TABLE `crm_product` ADD COLUMN `description`  TEXT                    DEFAULT NULL COMMENT '产品描述' AFTER `remark`;
ALTER TABLE `crm_product` ADD COLUMN `tax_rate`     DECIMAL(5,2)  DEFAULT 6.00 COMMENT '默认税率(%)' AFTER `cost`;
ALTER TABLE `crm_product` ADD COLUMN `min_price`    DECIMAL(18,2) DEFAULT NULL COMMENT '最低成交价' AFTER `tax_rate`;
ALTER TABLE `crm_product` ADD COLUMN `sort`         INT           NOT NULL DEFAULT 0 COMMENT '排序' AFTER `min_price`;
ALTER TABLE `crm_product` ADD COLUMN `image_url`    VARCHAR(255) DEFAULT NULL COMMENT '主图地址' AFTER `sort`;

ALTER TABLE `crm_product` ADD INDEX `idx_product_category_id` (`category_id`);
ALTER TABLE `crm_product` ADD INDEX `idx_product_status` (`status`);

-- ---------------------------- 目录种子数据 ----------------------------
INSERT INTO `crm_product_category` (`id`,`parent_id`,`name`,`code`,`sort`,`status`,`remark`) VALUES
 (1,0,'软件产品','SOFTWARE',1,1,'订阅类软件许可'),
 (2,0,'实施服务','SERVICE',2,1,'交付与集成服务'),
 (3,0,'运维与部署','OPS',3,1,'运维、部署与托管'),
 (4,0,'硬件设备','HARDWARE',4,1,'服务器及配套终端'),
 (101,1,'订阅许可','SUBSCRIPTION',1,1,'按年订阅的版本许可'),
 (102,1,'平台与扩展','PLATFORM',2,1,'PaaS 平台与扩展模块'),
 (201,2,'实施交付','IMPLEMENT',1,1,'顾问驻场实施'),
 (202,2,'数据集成','INTEGRATION',2,1,'ERP / OA / 数据对接'),
 (301,3,'运维支持','SUPPORT',1,1,'年度运维与技术支持'),
 (302,3,'私有化部署','DEPLOY',2,1,'私有化环境部署'),
 (401,4,'服务器与终端','DEVICE',1,1,'硬件及终端设备');

-- =============================================================
--  v6 通用业务变更轨迹
--  1) 所有核心模块的字段级修改记录
--  2) 客户负责人移交轨迹（原负责人仍可查看脱敏摘要）
--  可重复执行：CREATE TABLE IF NOT EXISTS
-- =============================================================

CREATE TABLE IF NOT EXISTS `sys_change_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `biz_type`      VARCHAR(32)  NOT NULL COMMENT '业务类型 CUSTOMER/ORDER/CONTRACT/...',
  `biz_id`        BIGINT       NOT NULL COMMENT '业务主键',
  `biz_no`        VARCHAR(64)  DEFAULT NULL COMMENT '业务单号',
  `action`        VARCHAR(24)  NOT NULL DEFAULT 'UPDATE' COMMENT 'UPDATE/CREATE/DELETE/CLAIM/TRANSFER/TO_PUBLIC/RECYCLE',
  `field_key`     VARCHAR(64)  DEFAULT NULL COMMENT '变更字段（数据库列名）',
  `field_label`   VARCHAR(64)  DEFAULT NULL COMMENT '字段中文名',
  `old_value`     VARCHAR(500) DEFAULT NULL,
  `new_value`     VARCHAR(500) DEFAULT NULL,
  `old_owner_id`  BIGINT       DEFAULT NULL COMMENT '负责人变更前值，用于历史客户查询',
  `operator_id`   BIGINT       DEFAULT NULL,
  `operator_name` VARCHAR(64)  DEFAULT NULL,
  `remark`        VARCHAR(255) DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`,`biz_id`,`id`),
  KEY `idx_owner_change` (`biz_type`,`field_key`,`old_owner_id`),
  KEY `idx_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用业务变更轨迹';

-- =============================================================
--  v11 合同明细
--  合同标的行（产品/服务、数量、单价、金额），合同金额由明细汇总
-- =============================================================

CREATE TABLE IF NOT EXISTS `crm_contract_item` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT,
  `contract_id`  BIGINT        NOT NULL COMMENT '所属合同',
  `product_id`   BIGINT                 DEFAULT NULL,
  `product_name` VARCHAR(128)  NOT NULL COMMENT '产品/服务名称',
  `unit`         VARCHAR(16)            DEFAULT NULL,
  `quantity`     DECIMAL(18,2) NOT NULL DEFAULT 1,
  `price`        DECIMAL(18,2) NOT NULL DEFAULT 0,
  `amount`       DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '数量 × 单价',
  `remark`       VARCHAR(255)           DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_contract` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同明细';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
--  初始化数据
-- =============================================================
SET @PWD = '$2a$10$GtR9YPJASu5sIAkqKu93/.dKdqkmWZrOVL3knHTvXRC21.z15Ds1a'; -- 123456

INSERT INTO `sys_dept` (`id`,`parent_id`,`name`,`leader`,`sort`,`status`) VALUES
 (1,0,'九巢科技（总部）','张伟',1,1),
 (2,1,'销售一部','陈静',1,1),
 (3,1,'销售二部','刘洋',2,1),
 (4,1,'售前支持部','孙浩',3,1);

INSERT INTO `sys_role` (`id`,`name`,`code`,`data_scope`,`permissions`,`sort`,`status`,`remark`) VALUES
 (1,'超级管理员','admin','ALL','["*:*:*"]',1,1,'拥有系统全部权限'),
 (2,'销售总监','sales_director','ALL','["dashboard:view", "crm:customer:list", "crm:customer:add", "crm:customer:edit", "crm:customer:delete", "crm:customer:transfer", "crm:contact:list", "crm:contact:add", "crm:contact:edit", "crm:contact:delete", "crm:lead:list", "crm:lead:add", "crm:lead:edit", "crm:lead:delete", "crm:lead:convert", "crm:opportunity:list", "crm:opportunity:add", "crm:opportunity:edit", "crm:opportunity:delete", "crm:opportunity:stage", "crm:product:list", "crm:product:add", "crm:product:edit", "crm:product:delete", "crm:contract:list", "crm:contract:add", "crm:contract:edit", "crm:contract:delete", "crm:contract:audit", "crm:payment:list", "crm:payment:add", "crm:activity:list", "crm:activity:add", "crm:activity:delete", "system:user:list", "system:role:list", "system:dept:list", "system:dict:list", "system:log:list", "crm:order:list", "crm:order:add", "crm:order:edit", "crm:order:delete", "crm:order:audit", "crm:order:deliver", "crm:delivery:list", "crm:delivery:add", "crm:delivery:edit", "crm:delivery:delete", "crm:delivery:ship", "crm:receivable:list", "crm:receivable:add", "crm:receivable:receive", "crm:trade:view", "crm:plan:list", "crm:plan:add", "crm:plan:edit", "crm:plan:delete", "crm:plan:ship", "crm:invoice:list", "crm:invoice:add", "crm:invoice:edit", "crm:invoice:delete", "crm:invoice:issue", "crm:return:list", "crm:return:add", "crm:return:edit", "crm:return:delete", "crm:return:audit", "crm:payment:cancel"]',2,1,'查看全公司数据'),
 (3,'销售经理','sales_manager','DEPT','["dashboard:view", "crm:customer:list", "crm:customer:add", "crm:customer:edit", "crm:customer:transfer", "crm:contact:list", "crm:contact:add", "crm:contact:edit", "crm:contact:delete", "crm:lead:list", "crm:lead:add", "crm:lead:edit", "crm:lead:delete", "crm:lead:convert", "crm:opportunity:list", "crm:opportunity:add", "crm:opportunity:edit", "crm:opportunity:delete", "crm:opportunity:stage", "crm:product:list", "crm:contract:list", "crm:contract:add", "crm:contract:edit", "crm:contract:audit", "crm:payment:list", "crm:payment:add", "crm:activity:list", "crm:activity:add", "crm:activity:delete", "system:log:list", "crm:order:list", "crm:order:add", "crm:order:edit", "crm:order:delete", "crm:order:audit", "crm:order:deliver", "crm:delivery:list", "crm:delivery:add", "crm:delivery:edit", "crm:delivery:delete", "crm:delivery:ship", "crm:receivable:list", "crm:receivable:add", "crm:receivable:receive", "crm:trade:view", "crm:plan:list", "crm:plan:add", "crm:plan:edit", "crm:plan:delete", "crm:plan:ship", "crm:invoice:list", "crm:invoice:add", "crm:invoice:edit", "crm:invoice:delete", "crm:invoice:issue", "crm:return:list", "crm:return:add", "crm:return:edit", "crm:return:delete", "crm:return:audit", "crm:payment:cancel"]',3,1,'查看本部门数据'),
 (4,'销售专员','sales_rep','SELF','["dashboard:view", "crm:customer:list", "crm:customer:add", "crm:customer:edit", "crm:contact:list", "crm:contact:add", "crm:contact:edit", "crm:contact:delete", "crm:lead:list", "crm:lead:add", "crm:lead:edit", "crm:lead:delete", "crm:lead:convert", "crm:opportunity:list", "crm:opportunity:add", "crm:opportunity:edit", "crm:opportunity:stage", "crm:product:list", "crm:contract:list", "crm:contract:add", "crm:contract:edit", "crm:payment:list", "crm:payment:add", "crm:activity:list", "crm:activity:add", "crm:activity:delete", "crm:order:list", "crm:order:add", "crm:order:edit", "crm:order:deliver", "crm:delivery:list", "crm:delivery:add", "crm:receivable:list", "crm:receivable:receive", "crm:trade:view", "crm:plan:list", "crm:plan:add", "crm:plan:ship", "crm:invoice:list", "crm:invoice:add", "crm:return:list", "crm:return:add", "crm:payment:cancel"]',4,1,'仅查看本人数据');

INSERT INTO `sys_user` (`id`,`username`,`password`,`nickname`,`mobile`,`email`,`dept_id`,`post`,`status`) VALUES
 (1,'admin',@PWD,'系统管理员','13800000000','admin@shangxu.com',1,'系统管理员',1),
 (2,'manager01',@PWD,'张伟','13800000001','zhangwei@shangxu.com',1,'销售总监',1),
 (3,'manager02',@PWD,'陈静','13800000002','chenjing@shangxu.com',2,'销售一部经理',1),
 (4,'sales01',@PWD,'李娜','13800000003','lina@shangxu.com',2,'高级销售顾问',1),
 (5,'sales02',@PWD,'王强','13800000004','wangqiang@shangxu.com',2,'销售顾问',1),
 (6,'sales03',@PWD,'刘洋','13800000005','liuyang@shangxu.com',3,'销售二部经理',1),
 (7,'sales04',@PWD,'赵敏','13800000006','zhaomin@shangxu.com',3,'销售顾问',1),
 (8,'service01',@PWD,'孙浩','13800000007','sunhao@shangxu.com',4,'售前顾问',1);

INSERT INTO `sys_user_role` (`user_id`,`role_id`) VALUES
 (1,1),(2,2),(3,3),(4,4),(5,4),(6,3),(7,4),(8,4);

INSERT INTO `sys_dict` (`dict_type`,`dict_label`,`dict_value`,`sort`) VALUES
 ('industry','互联网/IT','IT',1),('industry','制造业','MANUFACTURE',2),('industry','金融','FINANCE',3),
 ('industry','医疗健康','MEDICAL',4),('industry','教育','EDUCATION',5),('industry','物流','LOGISTICS',6),
 ('industry','建筑','CONSTRUCTION',7),('industry','快消','RETAIL',8),('industry','能源','ENERGY',9),
 ('industry','汽车','AUTO',10),('industry','其他','OTHER',99),
 ('customer_source','官网留资','WEBSITE',1),('customer_source','展会活动','EXHIBITION',2),
 ('customer_source','客户转介绍','REFERRAL',3),('customer_source','电话营销','TELEMARKETING',4),
 ('customer_source','广告投放','ADVERTISING',5),('customer_source','渠道合作','PARTNER',6),
 ('customer_level','A类（重点）','A',1),('customer_level','B类（普通）','B',2),('customer_level','C类（潜在）','C',3),
 ('customer_scale','1-50人','1-50',1),('customer_scale','51-200人','51-200',2),
 ('customer_scale','201-500人','201-500',3),('customer_scale','500人以上','500+',4),
 ('opp_stage','初步接洽','CONTACT',1),('opp_stage','需求确认','DEMAND',2),
 ('opp_stage','方案报价','QUOTE',3),('opp_stage','商务谈判','NEGOTIATION',4),
 ('opp_stage','赢单','WON',5),('opp_stage','输单','LOST',6),
 ('activity_type','电话沟通','CALL',1),('activity_type','上门拜访','VISIT',2),
 ('activity_type','微信沟通','WECHAT',3),('activity_type','邮件往来','EMAIL',4),
 ('activity_type','会议沟通','MEETING',5),('activity_type','其他','OTHER',6),
 ('customer_status','跟进中','FOLLOWING',1),('customer_status','已成交','DEAL',2),('customer_status','无效','INVALID',3),
 ('lead_status','新线索','NEW',1),('lead_status','跟进中','FOLLOWING',2),('lead_status','已确认','QUALIFIED',3),
 ('lead_status','已转化','CONVERTED',4),('lead_status','无效','INVALID',5),
 ('contract_status','草稿','DRAFT',1),('contract_status','审批中','PENDING',2),('contract_status','已审批','APPROVED',3),
 ('contract_status','已驳回','REJECTED',4),('contract_status','已签订','SIGNED',5),('contract_status','已作废','VOID',6),
 ('payment_status','未回款','UNPAID',1),('payment_status','部分回款','PART',2),('payment_status','已回款','PAID',3),
 ('pool_type','私海','PRIVATE',1),('pool_type','公海','PUBLIC',2);

-- 客户
INSERT INTO `crm_customer`
(`id`,`customer_no`,`name`,`short_name`,`industry`,`level`,`source`,`scale`,`phone`,`website`,`province`,`city`,`address`,`owner_user_id`,`dept_id`,`pool_type`,`status`,`next_follow_time`,`last_follow_time`,`remark`,`create_by`,`create_time`) VALUES
 (1,'KH20260001','广州云启智能科技有限公司','云启智能','IT','A','WEBSITE','201-500','020-88991234','www.yunqi-ai.com','广东省','广州市','天河区珠江新城华夏路10号',4,2,'PRIVATE','FOLLOWING','2026-11-10 10:00:00','2026-11-02 15:20:00','关注 AI 中台建设，预算充足','李娜','2026-01-12 09:30:00'),
 (2,'KH20260002','深圳市恒泰电子有限公司','恒泰电子','MANUFACTURE','A','EXHIBITION','500+','0755-83221100','www.hengtai-ele.com','广东省','深圳市','宝安区西乡街道固戍二路',4,2,'PRIVATE','FOLLOWING','2026-11-08 14:00:00','2026-11-03 11:00:00','老客户，二期项目在推进','李娜','2026-01-20 10:10:00'),
 (3,'KH20260003','上海联创金融信息服务有限公司','联创金融','FINANCE','A','REFERRAL','201-500','021-68889900','www.lianchuang-fin.com','上海市','上海市','浦东新区陆家嘴环路166号',5,2,'PRIVATE','FOLLOWING','2026-11-12 09:00:00','2026-11-01 16:40:00','对数据安全合规要求高','王强','2026-02-05 14:00:00'),
 (4,'KH20260004','北京数智时代科技有限公司','数智时代','IT','B','WEBSITE','51-200','010-62345678','www.shuzhi-era.com','北京市','北京市','海淀区中关村大街27号',5,2,'PRIVATE','FOLLOWING','2026-11-15 10:30:00','2026-10-28 09:50:00','技术型客户，重视产品能力','王强','2026-02-18 11:20:00'),
 (5,'KH20260005','杭州锦鸿医疗器械有限公司','锦鸿医疗','MEDICAL','B','TELEMARKETING','51-200','0571-87654321','www.jinhong-med.com','浙江省','杭州市','滨江区江陵路88号',6,3,'PRIVATE','FOLLOWING','2026-11-09 15:00:00','2026-11-04 10:10:00','需要私有化部署方案','刘洋','2026-03-02 09:00:00'),
 (6,'KH20260006','成都天府软件服务有限公司','天府软件','IT','C','ADVERTISING','51-200','028-85556677','www.tianfu-soft.com','四川省','成都市','高新区天府大道中段666号',6,3,'PRIVATE','FOLLOWING','2026-11-20 10:00:00','2026-10-20 14:30:00','预算有限，长期培育','刘洋','2026-03-15 16:20:00'),
 (7,'KH20260007','苏州精密机械制造有限公司','苏州精密','MANUFACTURE','B','EXHIBITION','500+','0512-67889900','www.szjm-mach.com','江苏省','苏州市','工业园区星湖街328号',7,3,'PRIVATE','FOLLOWING','2026-11-11 09:30:00','2026-11-05 15:00:00','关注生产与销售协同','赵敏','2026-04-08 10:40:00'),
 (8,'KH20260008','武汉光谷新材料股份有限公司','光谷新材','ENERGY','A','REFERRAL','500+','027-87651234','www.ggxincai.com','湖北省','武汉市','东湖高新区光谷大道77号',7,3,'PRIVATE','DEAL','2026-12-01 10:00:00','2026-10-25 11:30:00','已签约，等待二期扩容','赵敏','2026-04-22 14:10:00'),
 (9,'KH20260009','南京智慧物流有限公司','智慧物流','LOGISTICS','B','WEBSITE','201-500','025-83334444','www.njzhwl.com','江苏省','南京市','建邺区江东中路359号',4,2,'PRIVATE','FOLLOWING','2026-11-18 14:00:00','2026-10-30 09:20:00','关注移动端外勤能力','李娜','2026-05-06 09:50:00'),
 (10,'KH20260010','青岛海润食品集团有限公司','海润食品','RETAIL','C','TELEMARKETING','500+','0532-86667788','www.hairun-food.com','山东省','青岛市','市南区香港中路76号',5,2,'PRIVATE','FOLLOWING','2026-12-05 10:00:00','2026-09-18 15:40:00','决策链较长，暂缓推进','王强','2026-05-19 11:10:00'),
 (11,'KH20260011','厦门鹭岛跨境电商有限公司','鹭岛跨境','RETAIL','B','ADVERTISING','51-200','0592-5999888','www.ludao-cb.com','福建省','厦门市','思明区软件园二期观日路12号',6,3,'PRIVATE','FOLLOWING','2026-11-14 11:00:00','2026-11-02 10:00:00','多平台订单管理需求','刘洋','2026-06-03 15:30:00'),
 (12,'KH20260012','长沙湘江建筑工程有限公司','湘江建筑','CONSTRUCTION','C','EXHIBITION','201-500','0731-84446666','www.xjjiang.com','湖南省','长沙市','岳麓区潇湘中路283号',7,3,'PRIVATE','FOLLOWING','2026-11-25 09:00:00','2026-10-12 16:00:00','项目制销售，回款周期长','赵敏','2026-06-21 10:20:00'),
 (13,'KH20260013','天津滨海能源科技有限公司','滨海能源','ENERGY','A','REFERRAL','500+','022-25668899','www.bh-energy.com','天津市','天津市','滨海新区第五大街80号',4,2,'PRIVATE','FOLLOWING','2026-11-07 09:00:00','2026-11-05 14:20:00','集团级项目，需总部审批','李娜','2026-11-04 14:10:00'),
 (14,'KH20260014','西安丝路教育科技有限公司','丝路教育','EDUCATION','B','WEBSITE','51-200','029-88223344','www.silu-edu.com','陕西省','西安市','雁塔区高新路25号',5,2,'PRIVATE','FOLLOWING','2026-11-16 15:30:00','2026-10-29 11:10:00','关注招生线索管理','王强','2026-11-05 10:20:00'),
 (15,'KH20260015','重庆山城汽车零部件有限公司','山城汽配','AUTO','A','EXHIBITION','500+','023-68887777','www.scqp.com','重庆市','重庆市','九龙坡区科园一路200号',6,3,'PRIVATE','DEAL','2026-11-28 10:00:00','2026-10-22 09:30:00','一期已交付，客户满意度高','刘洋','2026-08-11 10:00:00'),
 (16,'KH20260016','合肥中科生物科技有限公司','中科生物','MEDICAL','B','REFERRAL','51-200','0551-65558888','www.zkbio.com','安徽省','合肥市','高新区望江西路666号',7,3,'PRIVATE','FOLLOWING','2026-11-13 14:00:00','2026-11-03 09:00:00','研发型团队，重视易用性','赵敏','2026-11-02 09:30:00'),
 (17,'KH20260017','郑州中原商贸有限公司','中原商贸','RETAIL','C','TELEMARKETING','51-200','0371-66223344','www.zymall.com','河南省','郑州市','金水区花园路39号',NULL,NULL,'PUBLIC','FOLLOWING',NULL,'2026-06-15 10:00:00','公海客户，超过30天未跟进','admin','2026-06-02 11:00:00'),
 (18,'KH20260018','佛山顺德家居制造有限公司','顺德家居','MANUFACTURE','B','ADVERTISING','201-500','0757-28889900','www.sdhome.com','广东省','佛山市','顺德区北滘镇工业大道8号',NULL,NULL,'PUBLIC','FOLLOWING',NULL,'2026-05-28 14:00:00','公海客户，待重新分配','admin','2026-05-11 09:30:00');

-- 联系人
INSERT INTO `crm_contact` (`customer_id`,`name`,`post`,`mobile`,`phone`,`email`,`gender`,`is_primary`,`remark`) VALUES
 (1,'周明','CTO','13900000001','020-88991234','zhouming@yunqi-ai.com','MALE',1,'技术决策人'),
 (1,'林小雨','信息化经理','13900000002',NULL,'linxy@yunqi-ai.com','FEMALE',0,'对接日常事务'),
 (2,'黄志强','采购总监','13900000003','0755-83221100','huangzq@hengtai-ele.com','MALE',1,'二期项目负责人'),
 (2,'吴倩','IT 经理','13900000004',NULL,'wuqian@hengtai-ele.com','FEMALE',0,'技术评估'),
 (3,'徐建国','副总裁','13900000005','021-68889900','xujianguo@lianchuang-fin.com','MALE',1,'最终决策人'),
 (3,'陈曦','风控总监','13900000006',NULL,'chenxi@lianchuang-fin.com','FEMALE',0,'合规审查'),
 (4,'马涛','研发总监','13900000007','010-62345678','matao@shuzhi-era.com','MALE',1,'关注 API 能力'),
 (5,'许静','院长助理','13900000008','0571-87654321','xujing@jinhong-med.com','FEMALE',1,'项目牵头人'),
 (6,'何军','总经理','13900000009','028-85556677','hejun@tianfu-soft.com','MALE',1,'价格敏感'),
 (7,'沈立','销售副总','13900000010','0512-67889900','shenli@szjm-mach.com','MALE',1,'业务负责人'),
 (7,'顾晓','信息中心主任','13900000011',NULL,'guxiao@szjm-mach.com','FEMALE',0,'系统对接'),
 (8,'邓超','董事长','13900000012','027-87651234','dengchao@ggxincai.com','MALE',1,'已签约客户'),
 (8,'罗敏','财务总监','13900000013',NULL,'luomin@ggxincai.com','FEMALE',0,'回款对接'),
 (9,'蒋伟','运营总监','13900000014','025-83334444','jiangwei@njzhwl.com','MALE',1,'关注外勤轨迹'),
 (10,'范丽','信息部经理','13900000015','0532-86667788','fanli@hairun-food.com','FEMALE',1,'暂缓项目'),
 (11,'高翔','电商负责人','13900000016','0592-5999888','gaoxiang@ludao-cb.com','MALE',1,'多店铺管理'),
 (12,'唐勇','项目经理','13900000017','0731-84446666','tangyong@xjjiang.com','MALE',1,'项目制采购'),
 (13,'曹宇','信息中心主任','13900000018','022-25668899','caoyu@bh-energy.com','MALE',1,'集团信息化负责人'),
 (13,'袁媛','采购经理','13900000019',NULL,'yuanyuan@bh-energy.com','FEMALE',0,'商务流程'),
 (14,'薛涛','招生总监','13900000020','029-88223344','xuetao@silu-edu.com','MALE',1,'线索量需求大'),
 (15,'阎峰','供应链总监','13900000021','023-68887777','yanfeng@scqp.com','MALE',1,'一期客户'),
 (16,'白露','研发副总','13900000022','0551-65558888','bailu@zkbio.com','FEMALE',1,'重视产品体验'),
 (17,'宋涛','总经理','13900000023','0371-66223344','songtao@zymall.com','MALE',1,'公海客户'),
 (18,'钟华','销售总监','13900000024','0757-28889900','zhonghua@sdhome.com','MALE',1,'公海客户');

-- 线索
INSERT INTO `crm_lead` (`id`,`lead_no`,`name`,`company`,`contact_name`,`mobile`,`email`,`source`,`industry`,`status`,`owner_user_id`,`dept_id`,`converted_customer_id`,`converted_time`,`remark`,`create_by`,`create_time`) VALUES
 (1,'XS20260001','云启智能 AI 中台项目','广州云启智能科技有限公司','周明','13900000001','zhouming@yunqi-ai.com','WEBSITE','IT','CONVERTED',4,2,1,'2026-01-12 10:00:00','已转化为客户','李娜','2026-01-08 09:20:00'),
 (2,'XS20260002','恒泰电子销售数字化升级','深圳市恒泰电子有限公司','黄志强','13900000003','huangzq@hengtai-ele.com','EXHIBITION','MANUFACTURE','CONVERTED',4,2,2,'2026-01-20 11:30:00','展会获取，已转化','李娜','2026-01-16 14:00:00'),
 (3,'XS20260003','联创金融客户管理系统采购','上海联创金融信息服务有限公司','徐建国','13900000005','xujianguo@lianchuang-fin.com','REFERRAL','FINANCE','CONVERTED',5,2,3,'2026-02-05 15:00:00','转介绍线索','王强','2026-02-01 10:10:00'),
 (4,'XS20260004','数智时代 CRM 选型','北京数智时代科技有限公司','马涛','13900000007','matao@shuzhi-era.com','WEBSITE','IT','FOLLOWING',5,2,NULL,NULL,'正在做 POC 测试','王强','2026-10-15 09:40:00'),
 (5,'XS20260005','锦鸿医疗私有化部署咨询','杭州锦鸿医疗器械有限公司','许静','13900000008','xujing@jinhong-med.com','TELEMARKETING','MEDICAL','QUALIFIED',6,3,NULL,NULL,'已确认需求，待报价','刘洋','2026-10-20 14:30:00'),
 (6,'XS20260006','天府软件中小企业版试用','成都天府软件服务有限公司','何军','13900000009','hejun@tianfu-soft.com','ADVERTISING','IT','FOLLOWING',6,3,NULL,NULL,'已开通试用账号','刘洋','2026-10-22 10:00:00'),
 (7,'XS20260007','苏州精密产销协同平台','苏州精密机械制造有限公司','沈立','13900000010','shenli@szjm-mach.com','EXHIBITION','MANUFACTURE','QUALIFIED',7,3,NULL,NULL,'需求明确，进入方案阶段','赵敏','2026-10-25 15:20:00'),
 (8,'XS20260008','智慧物流外勤管理需求','南京智慧物流有限公司','蒋伟','13900000014','jiangwei@njzhwl.com','WEBSITE','LOGISTICS','NEW',4,2,NULL,NULL,'官网表单留资','李娜','2026-11-01 09:00:00'),
 (9,'XS20260009','鹭岛跨境多店铺订单管理','厦门鹭岛跨境电商有限公司','高翔','13900000016','gaoxiang@ludao-cb.com','ADVERTISING','RETAIL','FOLLOWING',6,3,NULL,NULL,'关注电商插件能力','刘洋','2026-11-02 11:20:00'),
 (10,'XS20260010','滨海能源集团级 CRM 项目','天津滨海能源科技有限公司','曹宇','13900000018','caoyu@bh-energy.com','REFERRAL','ENERGY','QUALIFIED',4,2,NULL,NULL,'集团采购流程，周期较长','李娜','2026-11-03 10:30:00'),
 (11,'XS20260011','丝路教育招生线索管理','西安丝路教育科技有限公司','薛涛','13900000020','xuetao@silu-edu.com','WEBSITE','EDUCATION','NEW',5,2,NULL,NULL,'待首次电话触达','王强','2026-11-04 16:00:00'),
 (12,'XS20260012','中原商贸门店会员管理','郑州中原商贸有限公司','宋涛','13900000023','songtao@zymall.com','TELEMARKETING','RETAIL','INVALID',NULL,NULL,NULL,NULL,'号码为空号，标记无效','admin','2026-10-08 13:00:00');

-- 商机
INSERT INTO `crm_opportunity`
(`id`,`opp_no`,`name`,`customer_id`,`customer_name`,`amount`,`stage`,`win_rate`,`expect_deal_date`,`actual_deal_date`,`status`,`source`,`owner_user_id`,`dept_id`,`remark`,`create_by`,`create_time`) VALUES
 (1,'SJ20260001','云启智能 AI 中台一期','1','广州云启智能科技有限公司',860000.00,'NEGOTIATION',70,'2026-12-15',NULL,'OPEN','WEBSITE',4,2,'商务条款谈判中','李娜','2026-01-15 10:00:00'),
 (2,'SJ20260002','恒泰电子销售管理系统二期','2','深圳市恒泰电子有限公司',520000.00,'QUOTE',50,'2026-12-05',NULL,'OPEN','EXHIBITION',4,2,'报价已发出，等待反馈','李娜','2026-02-10 14:20:00'),
 (3,'SJ20260003','联创金融客户数据平台','3','上海联创金融信息服务有限公司',1280000.00,'DEMAND',30,'2027-01-20',NULL,'OPEN','REFERRAL',5,2,'安全合规评审中','王强','2026-02-20 09:30:00'),
 (4,'SJ20260004','数智时代 CRM 标准版','4','北京数智时代科技有限公司',180000.00,'CONTACT',10,'2027-02-10',NULL,'OPEN','WEBSITE',5,2,'POC 阶段','王强','2026-10-18 10:00:00'),
 (5,'SJ20260005','锦鸿医疗私有化 CRM 项目','5','杭州锦鸿医疗器械有限公司',760000.00,'QUOTE',50,'2026-12-20',NULL,'OPEN','TELEMARKETING',6,3,'方案已提交','刘洋','2026-10-22 15:00:00'),
 (6,'SJ20260006','天府软件中小企业版年费','6','成都天府软件服务有限公司',68000.00,'CONTACT',10,'2026-12-28',NULL,'OPEN','ADVERTISING',6,3,'试用中','刘洋','2026-10-25 11:00:00'),
 (7,'SJ20260007','苏州精密产销协同平台','7','苏州精密机械制造有限公司',960000.00,'DEMAND',30,'2027-01-15',NULL,'OPEN','EXHIBITION',7,3,'业务调研完成','赵敏','2026-10-28 09:20:00'),
 (8,'SJ20260008','光谷新材销售云一期','8','武汉光谷新材料股份有限公司',1450000.00,'WON',100,'2026-09-30','2026-09-28','WON','REFERRAL',7,3,'已签约','赵敏','2026-05-10 10:00:00'),
 (9,'SJ20260009','山城汽配供应链协同','15','重庆山城汽车零部件有限公司',1120000.00,'WON',100,'2026-08-20','2026-08-18','WON','EXHIBITION',6,3,'已签约并交付','刘洋','2026-06-15 14:00:00'),
 (10,'SJ20260010','滨海能源集团 CRM 平台','13','天津滨海能源科技有限公司',2380000.00,'NEGOTIATION',70,'2027-01-10',NULL,'OPEN','REFERRAL',4,2,'集团董事会审批中','李娜','2026-07-20 10:30:00'),
 (11,'SJ20260011','鹭岛跨境店铺管理扩展','11','厦门鹭岛跨境电商有限公司',240000.00,'DEMAND',30,'2027-02-28',NULL,'OPEN','ADVERTISING',6,3,'需求梳理中','刘洋','2026-11-02 14:00:00'),
 (12,'SJ20260012','中科生物研发协同管理','16','合肥中科生物科技有限公司',320000.00,'CONTACT',10,'2027-03-15',NULL,'OPEN','REFERRAL',7,3,'初次接触','赵敏','2026-11-03 10:00:00'),
 (13,'SJ20260013','湘江建筑项目型销售管理','12','长沙湘江建筑工程有限公司',450000.00,'LOST',0,'2026-10-30',NULL,'LOST','EXHIBITION',7,3,'预算不足，本期放弃','赵敏','2026-07-02 09:00:00'),
 (14,'SJ20260014','海润食品经销商管理','10','青岛海润食品集团有限公司',680000.00,'LOST',0,'2026-09-30',NULL,'LOST','TELEMARKETING',5,2,'选择竞品','王强','2026-05-25 15:30:00');

-- 产品
INSERT INTO `crm_product` (`code`,`name`,`category`,`unit`,`price`,`cost`,`status`,`remark`) VALUES
 ('PRD001','商序云 标准版（年）','软件订阅','套/年',39800.00,8000.00,1,'含 20 个账号'),
 ('PRD002','商序云 专业版（年）','软件订阅','套/年',98000.00,18000.00,1,'含 50 个账号，含审批流'),
 ('PRD003','商序云 企业版（年）','软件订阅','套/年',198000.00,32000.00,1,'不限账号，含 PaaS 平台'),
 ('PRD004','私有化部署服务','实施服务','次',150000.00,60000.00,1,'含部署与迁移'),
 ('PRD005','CRM 实施服务（标准）','实施服务','人天',2500.00,1200.00,1,'顾问驻场'),
 ('PRD006','CRM 实施服务（高级）','实施服务','人天',4000.00,1800.00,1,'资深顾问'),
 ('PRD007','数据集成服务','实施服务','次',60000.00,20000.00,1,'ERP/OA 对接'),
 ('PRD008','年度运维支持','运维服务','年',48000.00,15000.00,1,'7x12 小时响应');

-- 合同
INSERT INTO `crm_contract`
(`id`,`contract_no`,`name`,`customer_id`,`customer_name`,`opportunity_id`,`amount`,`sign_date`,`start_date`,`end_date`,`status`,`payment_status`,`owner_user_id`,`dept_id`,`remark`,`create_by`,`create_time`) VALUES
 (1,'HT20260001','光谷新材销售云一期合同','8','武汉光谷新材料股份有限公司',8,1450000.00,'2026-09-28','2026-10-01','2027-09-30','SIGNED','PART',7,3,'分三期回款','赵敏','2026-09-28 16:00:00'),
 (2,'HT20260002','山城汽配供应链协同合同','15','重庆山城汽车零部件有限公司',9,1120000.00,'2026-08-18','2026-08-20','2027-08-19','SIGNED','PAID',6,3,'已全额回款','刘洋','2026-08-18 15:30:00'),
 (3,'HT20260003','恒泰电子二期实施合同','2','深圳市恒泰电子有限公司',2,320000.00,'2026-10-15','2026-10-20','2027-04-19','APPROVED','UNPAID',4,2,'待客户盖章','李娜','2026-10-15 10:00:00'),
 (4,'HT20260004','云启智能 AI 中台实施合同','1','广州云启智能科技有限公司',1,480000.00,'2026-10-28','2026-11-01','2027-10-31','PENDING','UNPAID',4,2,'审批中','李娜','2026-10-28 14:20:00'),
 (5,'HT20260005','联创金融数据平台服务合同','3','上海联创金融信息服务有限公司',3,680000.00,'2026-09-10','2026-09-15','2027-09-14','SIGNED','PART',5,2,'首款已到账','王强','2026-09-10 09:30:00'),
 (6,'HT20260006','滨海能源集团 CRM 框架合同','13','天津滨海能源科技有限公司',10,2380000.00,'2026-10-30','2026-11-15','2027-11-14','DRAFT','UNPAID',4,2,'草稿，待法务审核','李娜','2026-10-30 11:00:00'),
 (7,'HT20260007','锦鸿医疗私有化部署合同','5','杭州锦鸿医疗器械有限公司',5,760000.00,'2026-09-25','2026-10-01','2027-09-30','SIGNED','UNPAID',6,3,'回款计划已生成','刘洋','2026-09-25 15:00:00'),
 (8,'HT20260008','数智时代标准版订阅合同','4','北京数智时代科技有限公司',4,180000.00,'2026-08-30','2026-09-01','2027-08-31','REJECTED','UNPAID',5,2,'金额条款被驳回','王强','2026-08-30 10:20:00');

-- 回款计划
INSERT INTO `crm_payment_plan` (`contract_id`,`period`,`plan_amount`,`plan_date`,`actual_amount`,`actual_date`,`status`,`remark`) VALUES
 (1,1,580000.00,'2026-10-10',580000.00,'2026-10-08','PAID','首付款 40%'),
 (1,2,435000.00,'2026-12-20',0.00,NULL,'UNPAID','上线款 30%'),
 (1,3,435000.00,'2027-03-31',0.00,NULL,'UNPAID','验收款 30%'),
 (2,1,560000.00,'2026-08-25',560000.00,'2026-08-23','PAID','首付款 50%'),
 (2,2,560000.00,'2026-10-15',560000.00,'2026-10-12','PAID','验收款 50%'),
 (3,1,160000.00,'2026-11-15',0.00,NULL,'UNPAID','预付款 50%'),
 (3,2,160000.00,'2027-02-28',0.00,NULL,'UNPAID','尾款 50%'),
 (4,1,240000.00,'2026-11-20',0.00,NULL,'UNPAID','首付款 50%'),
 (4,2,240000.00,'2027-03-31',0.00,NULL,'UNPAID','验收款 50%'),
 (5,1,340000.00,'2026-09-20',340000.00,'2026-09-18','PAID','首付款 50%'),
 (5,2,340000.00,'2026-12-31',0.00,NULL,'UNPAID','尾款 50%'),
 (7,1,380000.00,'2026-10-15',0.00,NULL,'UNPAID','预付款 50%'),
 (7,2,380000.00,'2027-01-31',0.00,NULL,'UNPAID','验收款 50%');

-- 跟进记录
INSERT INTO `crm_activity` (`biz_type`,`biz_id`,`biz_name`,`type`,`content`,`next_follow_time`,`owner_user_id`,`create_by`,`create_time`) VALUES
 ('CUSTOMER',1,'广州云启智能科技有限公司','VISIT','上门拜访周明 CTO，确认 AI 中台一期范围为销售+服务模块，客户希望 12 月中旬完成上线。','2026-11-10 10:00:00',4,'李娜','2026-11-02 15:20:00'),
 ('CUSTOMER',2,'深圳市恒泰电子有限公司','CALL','电话沟通二期实施排期，客户要求 11 月底前完成进场。','2026-11-08 14:00:00',4,'李娜','2026-11-03 11:00:00'),
 ('CUSTOMER',3,'上海联创金融信息服务有限公司','MEETING','与风控总监陈曦开合规评审会，需补充等保三级材料。','2026-11-12 09:00:00',5,'王强','2026-11-01 16:40:00'),
 ('CUSTOMER',4,'北京数智时代科技有限公司','WECHAT','微信发送 POC 测试环境账号，客户技术团队本周开始验证。','2026-11-15 10:30:00',5,'王强','2026-10-28 09:50:00'),
 ('CUSTOMER',5,'杭州锦鸿医疗器械有限公司','EMAIL','邮件提交私有化部署方案与报价单，等待客户内部评审。','2026-11-09 15:00:00',6,'刘洋','2026-11-04 10:10:00'),
 ('CUSTOMER',7,'苏州精密机械制造有限公司','VISIT','拜访销售副总沈立，梳理产销协同流程，确认 3 个核心场景。','2026-11-11 09:30:00',7,'赵敏','2026-11-05 15:00:00'),
 ('CUSTOMER',8,'武汉光谷新材料股份有限公司','CALL','回访一期使用情况，客户满意度高，二期扩容需求明确。','2026-12-01 10:00:00',7,'赵敏','2026-10-25 11:30:00'),
 ('CUSTOMER',9,'南京智慧物流有限公司','CALL','首次电话触达，客户关注外勤轨迹与拜访签到功能。','2026-11-18 14:00:00',4,'李娜','2026-10-30 09:20:00'),
 ('CUSTOMER',11,'厦门鹭岛跨境电商有限公司','MEETING','线上会议演示多店铺订单同步，客户提出 3 点定制需求。','2026-11-14 11:00:00',6,'刘洋','2026-11-02 10:00:00'),
 ('CUSTOMER',13,'天津滨海能源科技有限公司','VISIT','赴天津汇报集团级方案，信息中心主任曹宇认可整体架构。','2026-11-07 09:00:00',4,'李娜','2026-11-05 14:20:00'),
 ('CUSTOMER',15,'重庆山城汽车零部件有限公司','CALL','一期验收后回访，客户愿意作为标杆案例。','2026-11-28 10:00:00',6,'刘洋','2026-10-22 09:30:00'),
 ('CUSTOMER',16,'合肥中科生物科技有限公司','WECHAT','发送产品介绍与同行业案例，客户安排下周演示。','2026-11-13 14:00:00',7,'赵敏','2026-11-03 09:00:00'),
 ('LEAD',4,'数智时代 CRM 选型','CALL','电话确认 POC 范围，客户重点验证权限与自定义字段能力。',NULL,5,'王强','2026-10-20 10:00:00'),
 ('LEAD',5,'锦鸿医疗私有化部署咨询','MEETING','需求调研会，明确 80 人使用规模与等保要求。',NULL,6,'刘洋','2026-10-25 14:30:00'),
 ('LEAD',7,'苏州精密产销协同平台','VISIT','现场调研生产与销售协同流程。',NULL,7,'赵敏','2026-10-29 09:00:00'),
 ('LEAD',8,'智慧物流外勤管理需求','CALL','首次触达，客户希望了解移动端能力。',NULL,4,'李娜','2026-11-01 09:30:00'),
 ('LEAD',10,'滨海能源集团级 CRM 项目','EMAIL','发送集团级解决方案白皮书。',NULL,4,'李娜','2026-11-03 11:00:00'),
 ('OPPORTUNITY',1,'云启智能 AI 中台一期','MEETING','商务谈判会，确认折扣与付款节奏。','2026-11-10 10:00:00',4,'李娜','2026-10-30 16:00:00'),
 ('OPPORTUNITY',3,'联创金融客户数据平台','EMAIL','提交等保三级合规材料清单。',NULL,5,'王强','2026-11-01 17:00:00'),
 ('OPPORTUNITY',5,'锦鸿医疗私有化 CRM 项目','EMAIL','报价单已发送，含实施与三年运维。',NULL,6,'刘洋','2026-11-04 10:30:00'),
 ('OPPORTUNITY',10,'滨海能源集团 CRM 平台','VISIT','集团董事会前的最后一次方案汇报。','2026-11-07 09:00:00',4,'李娜','2026-11-05 15:00:00'),
 ('CONTRACT',1,'光谷新材销售云一期合同','CALL','提醒第二期回款时间，客户确认按期支付。',NULL,7,'赵敏','2026-10-20 10:00:00'),
 ('CONTRACT',4,'云启智能 AI 中台实施合同','OTHER','合同已提交审批流，等待销售总监审批。',NULL,4,'李娜','2026-10-28 14:30:00'),
 ('CONTRACT',7,'锦鸿医疗私有化部署合同','CALL','与财务确认首付款开票信息。',NULL,6,'刘洋','2026-10-08 09:00:00');

INSERT INTO `sys_log` (`title`,`business_type`,`request_method`,`oper_name`,`oper_url`,`oper_ip`,`status`,`cost_time`,`oper_time`) VALUES
 ('用户登录','LOGIN','POST','admin','/api/auth/login','127.0.0.1',1,86,'2026-11-06 09:12:00'),
 ('新增客户','INSERT','POST','李娜','/api/customers','127.0.0.1',1,132,'2026-11-06 09:40:00'),
 ('更新商机','UPDATE','PUT','王强','/api/opportunities/3','127.0.0.1',1,98,'2026-11-06 10:05:00');


-- 销售订单
INSERT INTO `crm_order`
(`id`,`order_no`,`title`,`customer_id`,`customer_name`,`contract_id`,`contract_no`,`amount`,`status`,`order_date`,`delivery_status`,`receivable_status`,`owner_user_id`,`dept_id`,`remark`,`create_by`,`create_time`) VALUES
 (1,'SO20260001','光谷新材销售云一期订单',8,'武汉光谷新材料股份有限公司',1,'HT20260001',1450000.00,'APPROVED','2026-09-30','DELIVERED','PAID',7,3,'按合同分三期执行','赵敏','2026-09-30 10:20:00'),
 (2,'SO20260002','山城汽配供应链协同订单',15,'重庆山城汽车零部件有限公司',2,'HT20260002',1120000.00,'APPROVED','2026-08-20','DELIVERED','PAID',6,3,'一期已交付验收','刘洋','2026-08-20 14:10:00'),
 (3,'SO20260003','恒泰电子二期实施订单',2,'深圳市恒泰电子有限公司',3,'HT20260003',320000.00,'APPROVED','2026-10-18','PART','PART',4,2,'实施服务分两批出库','李娜','2026-10-18 09:40:00'),
 (4,'SO20260004','云启智能 AI 中台实施订单',1,'广州云启智能科技有限公司',4,'HT20260004',480000.00,'PENDING','2026-10-30','UNDELIVERED','UNPAID',4,2,'等待销售总监审批','李娜','2026-10-30 15:00:00'),
 (5,'SO20260005','联创金融数据平台订单',3,'上海联创金融信息服务有限公司',5,'HT20260005',680000.00,'APPROVED','2026-09-15','DELIVERED','PART',5,2,'含数据迁移服务','王强','2026-09-15 11:20:00'),
 (6,'SO20260006','锦鸿医疗私有化部署订单',5,'杭州锦鸿医疗器械有限公司',7,'HT20260007',760000.00,'APPROVED','2026-10-08','UNDELIVERED','UNPAID',6,3,'待客户机房就绪后发货','刘洋','2026-10-08 10:00:00'),
 (7,'SO20260007','滨海能源集团 CRM 平台订单',13,'天津滨海能源科技有限公司',6,'HT20260006',2380000.00,'DRAFT','2026-11-02','UNDELIVERED','UNPAID',4,2,'框架合同，待分批下单','李娜','2026-11-02 16:30:00'),
 (8,'SO20260008','数智时代标准版年度订单',4,'北京数智时代科技有限公司',NULL,NULL,180000.00,'APPROVED','2026-09-02','DELIVERED','PAID',5,2,'20 账号标准版','王强','2026-09-02 10:10:00'),
 (9,'SO20260009','鹭岛跨境店铺管理扩展订单',11,'厦门鹭岛跨境电商有限公司',NULL,NULL,240000.00,'PENDING','2026-11-03','UNDELIVERED','UNPAID',6,3,'电商插件扩展包','刘洋','2026-11-03 14:20:00'),
 (10,'SO20260010','中科生物研发协同订单',16,'合肥中科生物科技有限公司',NULL,NULL,320000.00,'APPROVED','2026-10-28','UNDELIVERED','UNPAID',7,3,'含实施与培训','赵敏','2026-10-28 09:30:00');

INSERT INTO `crm_order_item` (`order_id`,`product_id`,`product_name`,`unit`,`quantity`,`price`,`amount`,`remark`) VALUES
 (1,3,'商序云 企业版（年）','套/年',5,198000.00,990000.00,NULL),
 (1,5,'CRM 实施服务（标准）','人天',184,2500.00,460000.00,NULL),
 (2,2,'商序云 专业版（年）','套/年',8,98000.00,784000.00,NULL),
 (2,6,'CRM 实施服务（高级）','人天',84,4000.00,336000.00,NULL),
 (3,5,'CRM 实施服务（标准）','人天',80,2500.00,200000.00,NULL),
 (3,7,'数据集成服务','次',2,60000.00,120000.00,NULL),
 (4,3,'商序云 企业版（年）','套/年',1,198000.00,198000.00,NULL),
 (4,4,'私有化部署服务','次',1,150000.00,150000.00,NULL),
 (4,8,'年度运维支持','年',1,48000.00,48000.00,NULL),
 (5,3,'商序云 企业版（年）','套/年',2,198000.00,396000.00,NULL),
 (5,7,'数据集成服务','次',4,60000.00,240000.00,NULL),
 (6,4,'私有化部署服务','次',1,150000.00,150000.00,NULL),
 (6,3,'商序云 企业版（年）','套/年',2,198000.00,396000.00,NULL),
 (6,8,'年度运维支持','年',2,48000.00,96000.00,NULL),
 (7,3,'商序云 企业版（年）','套/年',10,198000.00,1980000.00,NULL),
 (7,6,'CRM 实施服务（高级）','人天',100,4000.00,400000.00,NULL),
 (8,1,'商序云 标准版（年）','套/年',4,39800.00,159200.00,NULL),
 (8,8,'年度运维支持','年',1,20800.00,20800.00,NULL),
 (9,3,'商序云 企业版（年）','套/年',1,198000.00,198000.00,NULL),
 (9,7,'数据集成服务','次',1,42000.00,42000.00,NULL),
 (10,2,'商序云 专业版（年）','套/年',2,98000.00,196000.00,NULL),
 (10,5,'CRM 实施服务（标准）','人天',49,2500.00,122500.00,NULL);

-- 出库单
INSERT INTO `crm_delivery`
(`id`,`delivery_no`,`order_id`,`order_no`,`customer_id`,`customer_name`,`delivery_date`,`warehouse`,`total_qty`,`amount`,`status`,`logistics_company`,`logistics_no`,`receiver`,`receiver_phone`,`address`,`owner_user_id`,`dept_id`,`remark`,`create_by`,`create_time`) VALUES
 (1,'DN20260001',1,'SO20260001',8,'武汉光谷新材料股份有限公司','2026-10-08','主仓',5.00,1450000.00,'SIGNED','顺丰速运','SF1029384756','罗敏','13900000013','武汉市东湖高新区光谷大道77号',7,3,'含实施入场物料','赵敏','2026-10-08 09:30:00'),
 (2,'DN20260002',2,'SO20260002',15,'重庆山城汽车零部件有限公司','2026-08-25','主仓',8.00,1120000.00,'SIGNED','德邦物流','DB8827364510','阎峰','13900000021','重庆市九龙坡区科园一路200号',6,3,'一期交付','刘洋','2026-08-25 10:00:00'),
 (3,'DN20260003',3,'SO20260003',2,'深圳市恒泰电子有限公司','2026-10-22','主仓',80.00,200000.00,'SHIPPED','京东物流','JD5566778899','黄志强','13900000003','深圳市宝安区西乡街道固戍二路',4,2,'第一批实施服务物料','李娜','2026-10-22 14:00:00'),
 (4,'DN20260004',5,'SO20260005',3,'上海联创金融信息服务有限公司','2026-09-20','主仓',2.00,680000.00,'SIGNED','顺丰速运','SF9988776655','陈曦','13900000006','上海市浦东新区陆家嘴环路166号',5,2,'数据平台部署包','王强','2026-09-20 09:00:00'),
 (5,'DN20260005',8,'SO20260008',4,'北京数智时代科技有限公司','2026-09-05','主仓',4.00,180000.00,'SIGNED','中通快递','ZT3344556677','马涛','13900000007','北京市海淀区中关村大街27号',5,2,'标准版账号开通单','王强','2026-09-05 11:30:00'),
 (6,'DN20260006',6,'SO20260006',5,'杭州锦鸿医疗器械有限公司','2026-10-15','主仓',2.00,760000.00,'DRAFT',NULL,NULL,'许静','13900000008','杭州市滨江区江陵路88号',6,3,'待机房就绪后发出','刘洋','2026-10-15 15:20:00');

INSERT INTO `crm_delivery_item` (`delivery_id`,`product_name`,`unit`,`quantity`,`remark`) VALUES
 (1,'商序云 企业版（年）','套/年',5.00,NULL),
 (2,'商序云 专业版（年）','套/年',8.00,NULL),
 (3,'CRM 实施服务（标准）','人天',80.00,'第一批'),
 (4,'商序云 企业版（年）','套/年',2.00,NULL),
 (5,'商序云 标准版（年）','套/年',4.00,NULL),
 (6,'商序云 企业版（年）','套/年',2.00,NULL);

-- 应收单
INSERT INTO `crm_receivable`
(`id`,`receivable_no`,`customer_id`,`customer_name`,`source_type`,`source_id`,`source_no`,`amount`,`received_amount`,`receive_time`,`due_date`,`status`,`owner_user_id`,`dept_id`,`remark`,`create_by`,`create_time`) VALUES
 (1,'AR20260001',8,'武汉光谷新材料股份有限公司','ORDER',1,'SO20260001',1450000.00,1450000.00,'2026-10-08 10:20:00','2026-10-31','PAID',7,3,'已全额收款','赵敏','2026-10-01 10:00:00'),
 (2,'AR20260002',15,'重庆山城汽车零部件有限公司','ORDER',2,'SO20260002',1120000.00,1120000.00,'2026-08-24 15:10:00','2026-09-30','PAID',6,3,'一期款已结清','刘洋','2026-08-21 09:30:00'),
 (3,'AR20260003',2,'深圳市恒泰电子有限公司','ORDER',3,'SO20260003',320000.00,160000.00,'2026-10-20 09:40:00','2026-11-15','PART',4,2,'预付款已到账，尾款待验收','李娜','2026-10-19 10:20:00'),
 (4,'AR20260004',3,'上海联创金融信息服务有限公司','ORDER',5,'SO20260005',680000.00,340000.00,'2026-09-18 11:30:00','2026-11-20','PART',5,2,'首款 50% 已收','王强','2026-09-16 14:00:00'),
 (5,'AR20260005',5,'杭州锦鸿医疗器械有限公司','ORDER',6,'SO20260006',760000.00,0.00,NULL,'2026-11-25','UNPAID',6,3,'待发货后开票','刘洋','2026-10-09 09:10:00'),
 (6,'AR20260006',4,'北京数智时代科技有限公司','ORDER',8,'SO20260008',180000.00,180000.00,'2026-09-04 16:20:00','2026-10-02','PAID',5,2,'年费已收','王强','2026-09-03 10:00:00'),
 (7,'AR20260007',16,'合肥中科生物科技有限公司','ORDER',10,'SO20260010',320000.00,0.00,NULL,'2026-10-25','UNPAID',7,3,'已逾期，需重点催收','赵敏','2026-09-25 11:00:00'),
 (8,'AR20260008',1,'广州云启智能科技有限公司','CONTRACT',4,'HT20260004',480000.00,0.00,NULL,'2026-12-10','UNPAID',4,2,'合同首期应收','李娜','2026-11-01 09:00:00');

-- 订单/出库/应收 相关字典
INSERT INTO `sys_dict` (`dict_type`,`dict_label`,`dict_value`,`sort`) VALUES
 ('order_status','草稿','DRAFT',1),('order_status','审批中','PENDING',2),
 ('order_status','已审核','APPROVED',3),('order_status','已驳回','REJECTED',4),
 ('order_status','已取消','CANCELLED',5),
 ('delivery_status','待出库','DRAFT',1),('delivery_status','已出库','SHIPPED',2),
 ('delivery_status','已签收','SIGNED',3),('delivery_status','已作废','CANCELLED',4),
 ('delivery_progress','未出库','UNDELIVERED',1),('delivery_progress','部分出库','PART',2),
 ('delivery_progress','已出库','DELIVERED',3),
 ('receivable_status','未收款','UNPAID',1),('receivable_status','部分收款','PART',2),
 ('receivable_status','已收款','PAID',3),
 ('receivable_source','订单','ORDER',1),('receivable_source','合同','CONTRACT',2);


-- 发货计划
INSERT INTO `crm_delivery_plan`
(`id`,`plan_no`,`order_id`,`order_no`,`customer_id`,`customer_name`,`plan_date`,`plan_qty`,`amount`,`warehouse`,`status`,`delivery_id`,`owner_user_id`,`dept_id`,`remark`,`create_by`,`create_time`) VALUES
 (1,'DP20260001',1,'SO20260001',8,'武汉光谷新材料股份有限公司','2026-10-08',5.00,1450000.00,'主仓','SHIPPED',1,7,3,'已生成出库单 DN20260001','赵敏','2026-10-05 10:00:00'),
 (2,'DP20260002',2,'SO20260002',15,'重庆山城汽车零部件有限公司','2026-08-25',8.00,1120000.00,'主仓','SHIPPED',2,6,3,'一期一次性发货','刘洋','2026-08-22 09:30:00'),
 (3,'DP20260003',3,'SO20260003',2,'深圳市恒泰电子有限公司','2026-10-22',80.00,200000.00,'主仓','SHIPPED',3,4,2,'第一批实施物料','李娜','2026-10-20 14:00:00'),
 (4,'DP20260004',5,'SO20260005',3,'上海联创金融信息服务有限公司','2026-09-20',2.00,680000.00,'主仓','SHIPPED',4,5,2,'数据平台部署包','王强','2026-09-18 11:00:00'),
 (5,'DP20260005',6,'SO20260006',5,'杭州锦鸿医疗器械有限公司','2026-11-10',2.00,760000.00,'主仓','PENDING',NULL,6,3,'待客户机房就绪','刘洋','2026-10-15 15:20:00'),
 (6,'DP20260006',10,'SO20260010',16,'合肥中科生物科技有限公司','2026-11-08',2.00,320000.00,'主仓','PENDING',NULL,7,3,'含实施与培训物料','赵敏','2026-10-29 09:30:00'),
 (7,'DP20260007',7,'SO20260007',13,'天津滨海能源科技有限公司','2026-11-20',10.00,2380000.00,'主仓','CANCELLED',NULL,4,2,'订单未审核，计划取消','李娜','2026-11-03 10:00:00');

-- 发票
INSERT INTO `crm_invoice`
(`id`,`invoice_no`,`title`,`tax_no`,`customer_id`,`customer_name`,`source_type`,`source_id`,`source_no`,`amount`,`tax_rate`,`tax_amount`,`type`,`status`,`invoice_date`,`owner_user_id`,`dept_id`,`remark`,`create_by`,`create_time`) VALUES
 (1,'044120260001','武汉光谷新材料股份有限公司','91420100MA4K1234XA',8,'武汉光谷新材料股份有限公司','ORDER',1,'SO20260001',1450000.00,6.00,82075.47,'SPECIAL','ISSUED','2026-10-09',7,3,'已开具增值税专用发票','赵敏','2026-10-09 10:20:00'),
 (2,'044120260002','重庆山城汽车零部件有限公司','91500107MA5U7890XC',15,'重庆山城汽车零部件有限公司','ORDER',2,'SO20260002',1120000.00,6.00,63396.23,'SPECIAL','ISSUED','2026-08-26',6,3,'随一期验收开票','刘洋','2026-08-26 09:40:00'),
 (3,'044120260003','上海联创金融信息服务有限公司','91310115MA1K4567XD',3,'上海联创金融信息服务有限公司','ORDER',5,'SO20260005',680000.00,6.00,38490.57,'SPECIAL','ISSUED','2026-09-21',5,2,'首款部分开票','王强','2026-09-21 14:10:00'),
 (4,'044120260004','北京数智时代科技有限公司','91110108MA01AB12XE',4,'北京数智时代科技有限公司','ORDER',8,'SO20260008',180000.00,6.00,10188.68,'NORMAL','ISSUED','2026-09-05',5,2,'标准版年费普票','王强','2026-09-05 11:30:00'),
 (5,NULL,'深圳市恒泰电子有限公司','91440300MA5F5678XB',2,'深圳市恒泰电子有限公司','ORDER',3,'SO20260003',320000.00,6.00,18113.21,'SPECIAL','PENDING',NULL,4,2,'待客户确认开票抬头','李娜','2026-10-23 10:00:00'),
 (6,NULL,'杭州锦鸿医疗器械有限公司','91330108MA2C9012XF',5,'杭州锦鸿医疗器械有限公司','ORDER',6,'SO20260006',760000.00,6.00,43018.87,'SPECIAL','PENDING',NULL,6,3,'发货后开具','刘洋','2026-10-16 09:00:00');

-- 退货退款
INSERT INTO `crm_return`
(`id`,`return_no`,`order_id`,`order_no`,`customer_id`,`customer_name`,`return_date`,`return_qty`,`return_amount`,`refund_amount`,`reason`,`status`,`refund_time`,`owner_user_id`,`dept_id`,`remark`,`create_by`,`create_time`) VALUES
 (1,'RT20260001',2,'SO20260002',15,'重庆山城汽车零部件有限公司','2026-09-08',0.50,80000.00,80000.00,'部分许可未使用，协商退货','REFUNDED','2026-09-12 10:00:00',6,3,'已原路退款','刘洋','2026-09-08 15:00:00'),
 (2,'RT20260002',5,'SO20260005',3,'上海联创金融信息服务有限公司','2026-11-05',0.00,30000.00,0.00,'实施人天核减，申请退款','PENDING',NULL,5,2,'待财务审核','王强','2026-11-05 09:30:00'),
 (3,'RT20260003',8,'SO20260008',4,'北京数智时代科技有限公司','2026-09-20',0.00,18000.00,0.00,'客户要求退订，已超出售后期','REJECTED',NULL,5,2,'驳回：超出退订窗口','王强','2026-09-20 10:00:00');

-- 发货计划 / 开票 / 退货 字典
INSERT INTO `sys_dict` (`dict_type`,`dict_label`,`dict_value`,`sort`) VALUES
 ('plan_status','待发货','PENDING',1),('plan_status','已发货','SHIPPED',2),('plan_status','已取消','CANCELLED',3),
 ('invoice_status','待开票','PENDING',1),('invoice_status','已开票','ISSUED',2),('invoice_status','已作废','VOID',3),
 ('invoice_type','增值税专用发票','SPECIAL',1),('invoice_type','增值税普通发票','NORMAL',2),('invoice_type','电子发票','E_INVOICE',3),
 ('return_status','待审核','PENDING',1),('return_status','已通过','APPROVED',2),
 ('return_status','已驳回','REJECTED',3),('return_status','已退款','REFUNDED',4);

-- =============================================================
--  v3 演示数据：收款单 / 核销明细 / 预收 / 通知 / 取号序列
-- =============================================================

-- 回款计划回归「计划」属性：实际收款以收款单为准，避免与应收双通道重复计收
UPDATE `crm_payment_plan` SET `actual_amount` = 0, `actual_date` = NULL, `status` = 'UNPAID';

INSERT INTO `crm_payment_record`
(`id`,`record_no`,`customer_id`,`customer_name`,`amount`,`allocated_amount`,`unallocated_amount`,`receive_date`,`method`,`bank_account`,`voucher_no`,`source_type`,`source_id`,`source_no`,`status`,`owner_user_id`,`dept_id`,`remark`,`create_by`,`create_time`) VALUES
 (1,'SR20260001',8,'武汉光谷新材料股份有限公司',1450000.00,1450000.00,0.00,'2026-10-08','BANK','招商银行武汉分行','2026100812345','ORDER',1,'SO20260001','CONFIRMED',7,3,'一期款全额到账','赵敏','2026-10-08 10:20:00'),
 (2,'SR20260002',15,'重庆山城汽车零部件有限公司',1120000.00,1120000.00,0.00,'2026-08-24','BANK','工商银行重庆分行','2026082467890','ORDER',2,'SO20260002','CONFIRMED',6,3,'一期款结清','刘洋','2026-08-24 15:10:00'),
 (3,'SR20260003',2,'深圳市恒泰电子有限公司',160000.00,160000.00,0.00,'2026-10-20','BANK','建设银行深圳分行','2026102034567','ORDER',3,'SO20260003','CONFIRMED',4,2,'预付款 50%','李娜','2026-10-20 09:40:00'),
 (4,'SR20260004',3,'上海联创金融信息服务有限公司',340000.00,340000.00,0.00,'2026-09-18','ACCEPTANCE','交通银行上海分行','AC20260918001','ORDER',5,'SO20260005','CONFIRMED',5,2,'银行承兑汇票，已贴现','王强','2026-09-18 11:30:00'),
 (5,'SR20260005',4,'北京数智时代科技有限公司',180000.00,180000.00,0.00,'2026-09-04','ONLINE','支付宝企业账户','ALI2026090412','ORDER',8,'SO20260008','CONFIRMED',5,2,'在线支付','王强','2026-09-04 16:20:00'),
 (6,'SR20260006',16,'合肥中科生物科技有限公司',50000.00,0.00,50000.00,'2026-11-05','BANK','中国银行合肥分行','2026110511223','MANUAL',NULL,NULL,'CONFIRMED',7,3,'客户预付明年扩容款，暂未核销','赵敏','2026-11-05 14:00:00');

INSERT INTO `crm_payment_allocation` (`record_id`,`receivable_id`,`receivable_no`,`amount`) VALUES
 (1,1,'AR20260001',1450000.00),
 (2,2,'AR20260002',1120000.00),
 (3,3,'AR20260003',160000.00),
 (4,4,'AR20260004',340000.00),
 (5,6,'AR20260006',180000.00);

INSERT INTO `sys_notice` (`user_id`,`title`,`content`,`type`,`biz_type`,`biz_id`,`biz_no`,`read_flag`,`create_time`) VALUES
 (1,'系统已完成 v3 商用增强升级','收款核销中心、附件中心、站内通知、导入导出、并发取号与乐观锁已上线，请查看使用手册。','SYSTEM',NULL,NULL,NULL,0,'2026-11-06 09:00:00'),
 (1,'应收单 AR20260007 已逾期','合肥中科生物科技有限公司应收 320,000.00 元已于 2026-10-25 到期，请尽快催收。','RECEIVABLE','RECEIVABLE',7,'AR20260007',0,'2026-11-06 09:00:00'),
 (4,'客户「广州云启智能科技有限公司」待跟进','下次跟进时间 2026-11-10 10:00，请提前准备拜访材料。','FOLLOWUP','CUSTOMER',1,NULL,0,'2026-11-06 09:00:00');

INSERT INTO `sys_sequence` (`seq_key`,`current_val`) VALUES
 ('customer',(SELECT IFNULL(MAX(CAST(SUBSTRING(customer_no,7) AS UNSIGNED)),0) FROM `crm_customer`)),
 ('lead',(SELECT IFNULL(MAX(CAST(SUBSTRING(lead_no,7) AS UNSIGNED)),0) FROM `crm_lead`)),
 ('opportunity',(SELECT IFNULL(MAX(CAST(SUBSTRING(opp_no,7) AS UNSIGNED)),0) FROM `crm_opportunity`)),
 ('contract',(SELECT IFNULL(MAX(CAST(SUBSTRING(contract_no,7) AS UNSIGNED)),0) FROM `crm_contract`)),
 ('order',(SELECT IFNULL(MAX(CAST(SUBSTRING(order_no,7) AS UNSIGNED)),0) FROM `crm_order`)),
 ('delivery',(SELECT IFNULL(MAX(CAST(SUBSTRING(delivery_no,7) AS UNSIGNED)),0) FROM `crm_delivery`)),
 ('receivable',(SELECT IFNULL(MAX(CAST(SUBSTRING(receivable_no,7) AS UNSIGNED)),0) FROM `crm_receivable`)),
 ('payment',6),
 ('return',(SELECT IFNULL(MAX(CAST(SUBSTRING(return_no,7) AS UNSIGNED)),0) FROM `crm_return`)),
 ('invoice',(SELECT IFNULL(MAX(CAST(SUBSTRING(invoice_no,9) AS UNSIGNED)),0) FROM `crm_invoice` WHERE invoice_no IS NOT NULL AND invoice_no REGEXP '^[0-9]+$')),
 ('plan',(SELECT IFNULL(MAX(CAST(SUBSTRING(plan_no,7) AS UNSIGNED)),0) FROM `crm_delivery_plan`));

-- =============================================================
--  v4 产品目录归属与扩展字段演示数据
-- =============================================================
UPDATE `crm_product` SET `category_id`=101, `spec`='20 账号 / 年', `tax_rate`=6.00,
  `min_price`=35800.00, `sort`=1,
  `description`='面向小型销售团队的标准版，包含客户、联系人、线索、商机、合同与基础报表，支持 20 个账号，按年订阅，含标准在线支持。'
  WHERE `code`='PRD001';
UPDATE `crm_product` SET `category_id`=101, `spec`='50 账号 / 年', `tax_rate`=6.00,
  `min_price`=88000.00, `sort`=2,
  `description`='面向成长型团队的专业版，在标准版基础上增加审批流、商机看板、回款计划与数据权限，支持 50 个账号。'
  WHERE `code`='PRD002';
UPDATE `crm_product` SET `category_id`=102, `spec`='不限账号 / 年', `tax_rate`=6.00,
  `min_price`=178000.00, `sort`=3,
  `description`='面向集团客户的企业版，不限账号，包含 PaaS 平台能力、自定义字段、开放 API 与专属客户成功服务。'
  WHERE `code`='PRD003';
UPDATE `crm_product` SET `category_id`=302, `spec`='单次部署', `tax_rate`=6.00,
  `min_price`=135000.00, `sort`=4,
  `description`='私有化环境部署服务，含环境规划、安装部署、数据迁移、安全加固与验收交付。'
  WHERE `code`='PRD004';
UPDATE `crm_product` SET `category_id`=201, `spec`='人天', `tax_rate`=6.00,
  `min_price`=2200.00, `sort`=5,
  `description`='标准顾问驻场实施，含需求调研、系统配置、用户培训与上线支持。'
  WHERE `code`='PRD005';
UPDATE `crm_product` SET `category_id`=201, `spec`='人天', `tax_rate`=6.00,
  `min_price`=3600.00, `sort`=6,
  `description`='资深顾问实施，适用于复杂业务流程梳理、跨系统集成与集团型组织落地。'
  WHERE `code`='PRD006';
UPDATE `crm_product` SET `category_id`=202, `spec`='次', `tax_rate`=6.00,
  `min_price`=54000.00, `sort`=7,
  `description`='ERP / OA / 财务系统对接服务，含接口开发、数据映射、联调测试与上线保障。'
  WHERE `code`='PRD007';
UPDATE `crm_product` SET `category_id`=301, `spec`='年', `tax_rate`=6.00,
  `min_price`=43000.00, `sort`=8,
  `description`='年度运维支持，7x12 小时响应，含版本升级、故障排查、健康检查与季度巡检报告。'
  WHERE `code`='PRD008';

-- 目录名称与产品表冗余字段保持一致（列表直接读 category）
UPDATE `crm_product` SET `category`='订阅许可'   WHERE `code` IN ('PRD001','PRD002');
UPDATE `crm_product` SET `category`='平台与扩展' WHERE `code`='PRD003';
UPDATE `crm_product` SET `category`='私有化部署' WHERE `code`='PRD004';
UPDATE `crm_product` SET `category`='实施交付'   WHERE `code` IN ('PRD005','PRD006');
UPDATE `crm_product` SET `category`='数据集成'   WHERE `code`='PRD007';
UPDATE `crm_product` SET `category`='运维支持'   WHERE `code`='PRD008';

-- =============================================================
--  v5 产品编码序列 / CSV 导入导出
-- =============================================================
-- 产品编码序列初始化：取现有编码中的最大序号，避免与 PRD001~PRD008 冲突
INSERT IGNORE INTO `sys_sequence` (`seq_key`, `current_val`)
SELECT 'product', IFNULL(MAX(CAST(SUBSTRING(`code`, 4) AS UNSIGNED)), 0) FROM `crm_product` WHERE `code` REGEXP '^PRD[0-9]+$';

UPDATE `sys_sequence`
SET `current_val` = GREATEST(`current_val`,
    (SELECT IFNULL(MAX(CAST(SUBSTRING(`code`, 4) AS UNSIGNED)), 0) FROM `crm_product` WHERE `code` REGEXP '^PRD[0-9]+$'))
WHERE `seq_key` = 'product';
