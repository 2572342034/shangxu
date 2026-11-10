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
