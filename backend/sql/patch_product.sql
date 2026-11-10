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
