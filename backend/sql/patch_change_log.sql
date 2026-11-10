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
