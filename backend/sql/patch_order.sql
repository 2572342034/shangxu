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
