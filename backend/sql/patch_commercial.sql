-- =============================================================
--  v3 商用增强：收款核销 / 附件 / 通知 / 取号序列 / 乐观锁
-- =============================================================

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
