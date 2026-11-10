USE `shangxu_crm`;
-- =============================================================
--  v3 演示数据：收款单 / 核销明细 / 预收 / 通知 / 取号序列
-- =============================================================

-- 回款计划回归「计划」属性：实际收款以收款单为准，避免与应收双通道重复计收
UPDATE `crm_payment_plan` SET `actual_amount` = 0, `actual_date` = NULL, `status` = 'UNPAID';

INSERT INTO `crm_payment_record`
(`id`,`record_no`,`customer_id`,`customer_name`,`amount`,`allocated_amount`,`unallocated_amount`,`receive_date`,`method`,`bank_account`,`voucher_no`,`source_type`,`source_id`,`source_no`,`status`,`owner_user_id`,`dept_id`,`remark`,`create_by`,`create_time`) VALUES
 (1,'SR20260001',8,'武汉光谷新材料股份有限公司',1450000.00,1450000.00,0.00,'2026-10-08','BANK','招商银行武汉分行','2026100812345','ORDER',1,'SO20260001','CONFIRMED',7,3,'一期款全额到账','赵敏','2026-10-08 10:20:00'),
 (2,'SR20260002',15,'重庆山城汽车零部件有限公司',1120000.00,1120000.00,0.00,'2026-08-24','BANK','工商银行重庆分行','2026082467890','ORDER',2,'SO20260002','CONFIRMED',6,3,'一期款结清','刘洋','2026-08-24 15:10:00'),
 (3,'SR20260003',2,'深圳市恒泰电子有限公司',160000.00,160000.00,0.00,'2026-10-20','BANK','建设银行深圳分行','2026102034567','ORDER',3,'SO20260003','CONFIRMED',4,2,'预付款 50%','李娜','2026-10-20 09:40:00'),
 (4,'SR20260004',3,'上海联创金融信息服务有限公司',340000.00,340000.00,0.00,'2026-09-18','ACCEPTANCE','交通银行上海分行','AC20260918001','ORDER',5,'SO20260005','CONFIRMED',5,2,'银行承兑汇票，已贴现','王强','2026-09-18 11:30:00'),
 (5,'SR20260005',4,'北京数智时代科技有限公司',180000.00,180000.00,0.00,'2026-09-04','ONLINE','支付宝企业账户','ALI2026090412','ORDER',8,'SO20260008','CONFIRMED',5,2,'在线支付','王强','2026-09-04 16:20:00'),
 (6,'SR20260006',16,'合肥中科生物科技有限公司',50000.00,0.00,50000.00,'2026-11-05','BANK','中国银行合肥分行','2026110511223','MANUAL',NULL,NULL,'CONFIRMED',7,3,'客户预付明年扩容款，暂未核销','赵敏','2026-11-05 14:00:00');

INSERT INTO `crm_payment_allocation` (`record_id`,`receivable_id`,`receivable_no`,`amount`) VALUES
 (1,1,'AR20260001',1450000.00),
 (2,2,'AR20260002',1120000.00),
 (3,3,'AR20260003',160000.00),
 (4,4,'AR20260004',340000.00),
 (5,6,'AR20260006',180000.00);

INSERT INTO `sys_notice` (`user_id`,`title`,`content`,`type`,`biz_type`,`biz_id`,`biz_no`,`read_flag`,`create_time`) VALUES
 (1,'系统已完成 v3 商用增强升级','收款核销中心、附件中心、站内通知、导入导出、并发取号与乐观锁已上线，请查看使用手册。','SYSTEM',NULL,NULL,NULL,0,'2026-11-06 09:00:00'),
 (1,'应收单 AR20260007 已逾期','合肥中科生物科技有限公司应收 320,000.00 元已于 2026-10-25 到期，请尽快催收。','RECEIVABLE','RECEIVABLE',7,'AR20260007',0,'2026-11-06 09:00:00'),
 (4,'客户「广州云启智能科技有限公司」待跟进','下次跟进时间 2026-11-10 10:00，请提前准备拜访材料。','FOLLOWUP','CUSTOMER',1,NULL,0,'2026-11-06 09:00:00');

INSERT INTO `sys_sequence` (`seq_key`,`current_val`) VALUES
 ('customer',(SELECT IFNULL(MAX(CAST(SUBSTRING(customer_no,7) AS UNSIGNED)),0) FROM `crm_customer`)),
 ('lead',(SELECT IFNULL(MAX(CAST(SUBSTRING(lead_no,7) AS UNSIGNED)),0) FROM `crm_lead`)),
 ('opportunity',(SELECT IFNULL(MAX(CAST(SUBSTRING(opp_no,7) AS UNSIGNED)),0) FROM `crm_opportunity`)),
 ('contract',(SELECT IFNULL(MAX(CAST(SUBSTRING(contract_no,7) AS UNSIGNED)),0) FROM `crm_contract`)),
 ('order',(SELECT IFNULL(MAX(CAST(SUBSTRING(order_no,7) AS UNSIGNED)),0) FROM `crm_order`)),
 ('delivery',(SELECT IFNULL(MAX(CAST(SUBSTRING(delivery_no,7) AS UNSIGNED)),0) FROM `crm_delivery`)),
 ('receivable',(SELECT IFNULL(MAX(CAST(SUBSTRING(receivable_no,7) AS UNSIGNED)),0) FROM `crm_receivable`)),
 ('payment',6),
 ('return',(SELECT IFNULL(MAX(CAST(SUBSTRING(return_no,7) AS UNSIGNED)),0) FROM `crm_return`)),
 ('invoice',(SELECT IFNULL(MAX(CAST(SUBSTRING(invoice_no,9) AS UNSIGNED)),0) FROM `crm_invoice` WHERE invoice_no IS NOT NULL AND invoice_no REGEXP '^[0-9]+$')),
 ('plan',(SELECT IFNULL(MAX(CAST(SUBSTRING(plan_no,7) AS UNSIGNED)),0) FROM `crm_delivery_plan`));
