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
