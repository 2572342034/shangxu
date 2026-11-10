-- =============================================================
--  v4 产品目录与产品详情 增量迁移（已有库执行，不清数据）
-- =============================================================
USE `shangxu_crm`;

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
