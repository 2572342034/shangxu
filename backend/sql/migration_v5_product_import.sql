USE `shangxu_crm`;

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
