-- =============================================================
--  v7 数据订正：把 v6 早期落库的英文码值/字段名改成中文
--  可重复执行（都是幂等的条件更新）
-- =============================================================
USE `shangxu_crm`;

-- 字段名中文
UPDATE `sys_change_log` SET `field_label` = '性别'       WHERE `field_key` = 'gender'     AND `field_label` <> '性别';
UPDATE `sys_change_log` SET `field_label` = '关键联系人' WHERE `field_key` = 'is_primary' AND `field_label` <> '关键联系人';
UPDATE `sys_change_log` SET `field_label` = '微信'       WHERE `field_key` = 'wechat'     AND `field_label` <> '微信';
UPDATE `sys_change_log` SET `field_label` = '职务'       WHERE `field_key` = 'post'       AND `field_label` <> '职务';
UPDATE `sys_change_log` SET `field_label` = '生日'       WHERE `field_key` = 'birthday'   AND `field_label` <> '生日';

-- 性别（兼容大小写）
UPDATE `sys_change_log` SET `old_value` = '男' WHERE `field_key` = 'gender' AND UPPER(`old_value`) = 'MALE';
UPDATE `sys_change_log` SET `new_value` = '男' WHERE `field_key` = 'gender' AND UPPER(`new_value`) = 'MALE';
UPDATE `sys_change_log` SET `old_value` = '女' WHERE `field_key` = 'gender' AND UPPER(`old_value`) = 'FEMALE';
UPDATE `sys_change_log` SET `new_value` = '女' WHERE `field_key` = 'gender' AND UPPER(`new_value`) = 'FEMALE';

-- 布尔标志
UPDATE `sys_change_log` SET `old_value` = '否' WHERE `field_key` IN ('is_primary','status') AND `old_value` = '0';
UPDATE `sys_change_log` SET `new_value` = '否' WHERE `field_key` IN ('is_primary','status') AND `new_value` = '0';
UPDATE `sys_change_log` SET `old_value` = '是' WHERE `field_key` = 'is_primary' AND `old_value` = '1';
UPDATE `sys_change_log` SET `new_value` = '是' WHERE `field_key` = 'is_primary' AND `new_value` = '1';

-- 客户池
UPDATE `sys_change_log` SET `old_value` = '私海' WHERE `field_key` = 'pool_type' AND `old_value` = 'PRIVATE';
UPDATE `sys_change_log` SET `new_value` = '私海' WHERE `field_key` = 'pool_type' AND `new_value` = 'PRIVATE';
UPDATE `sys_change_log` SET `old_value` = '公海' WHERE `field_key` = 'pool_type' AND `old_value` = 'PUBLIC';
UPDATE `sys_change_log` SET `new_value` = '公海' WHERE `field_key` = 'pool_type' AND `new_value` = 'PUBLIC';

-- 负责人 / 部门 ID 转姓名
UPDATE `sys_change_log` l JOIN `sys_user` u ON u.`id` = CAST(l.`old_value` AS UNSIGNED)
SET l.`old_value` = u.`nickname`
WHERE l.`field_key` = 'owner_user_id' AND l.`old_value` REGEXP '^[0-9]+$';
UPDATE `sys_change_log` l JOIN `sys_user` u ON u.`id` = CAST(l.`new_value` AS UNSIGNED)
SET l.`new_value` = u.`nickname`
WHERE l.`field_key` = 'owner_user_id' AND l.`new_value` REGEXP '^[0-9]+$';
UPDATE `sys_change_log` l JOIN `sys_dept` d ON d.`id` = CAST(l.`old_value` AS UNSIGNED)
SET l.`old_value` = d.`name`
WHERE l.`field_key` = 'dept_id' AND l.`old_value` REGEXP '^[0-9]+$';
UPDATE `sys_change_log` l JOIN `sys_dept` d ON d.`id` = CAST(l.`new_value` AS UNSIGNED)
SET l.`new_value` = d.`name`
WHERE l.`field_key` = 'dept_id' AND l.`new_value` REGEXP '^[0-9]+$';

-- 时间里的 ISO 分隔符 T 改成空格
UPDATE `sys_change_log` SET `old_value` = REPLACE(`old_value`, 'T', ' ')
WHERE `field_key` LIKE '%time' AND `old_value` LIKE '%T%';
UPDATE `sys_change_log` SET `new_value` = REPLACE(`new_value`, 'T', ' ')
WHERE `field_key` LIKE '%time' AND `new_value` LIKE '%T%';
