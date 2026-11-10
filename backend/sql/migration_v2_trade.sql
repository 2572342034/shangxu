-- =============================================================
--  v2 迁移：发货计划 / 开票 / 退货退款
--  仅新增 3 张表与相关数据，不影响已有业务数据
-- =============================================================
USE `shangxu_crm`;

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

-- 角色权限
UPDATE `sys_role`
SET `permissions` = JSON_ARRAY_APPEND(`permissions`, '$', 'crm:plan:list', '$', 'crm:plan:add', '$', 'crm:plan:edit', '$', 'crm:plan:delete', '$', 'crm:plan:ship', '$', 'crm:invoice:list', '$', 'crm:invoice:add', '$', 'crm:invoice:edit', '$', 'crm:invoice:delete', '$', 'crm:invoice:issue', '$', 'crm:return:list', '$', 'crm:return:add', '$', 'crm:return:edit', '$', 'crm:return:delete', '$', 'crm:return:audit')
WHERE `code` IN ('sales_director', 'sales_manager')
  AND JSON_SEARCH(`permissions`, 'one', 'crm:plan:list') IS NULL;

UPDATE `sys_role`
SET `permissions` = JSON_ARRAY_APPEND(`permissions`, '$', 'crm:plan:list', '$', 'crm:plan:add', '$', 'crm:plan:ship', '$', 'crm:invoice:list', '$', 'crm:invoice:add', '$', 'crm:return:list', '$', 'crm:return:add')
WHERE `code` = 'sales_rep'
  AND JSON_SEARCH(`permissions`, 'one', 'crm:plan:list') IS NULL;
