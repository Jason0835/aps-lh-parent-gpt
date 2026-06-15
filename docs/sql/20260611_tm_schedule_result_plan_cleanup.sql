-- 胎面排程结果表收口调整
-- 1. 结果表补充工单号字段
-- 2. 发布状态统一使用数据字典 IS_RELEASE
-- 3. 库存表不良数量字段统一使用 BAD_QTY

ALTER TABLE `T_TM_SCHEDULE_RESULT`
    ADD COLUMN `ORDER_NO` varchar(20) DEFAULT NULL COMMENT '工单号' AFTER `BATCH_NO`;

ALTER TABLE `T_TM_SCHEDULE_RESULT`
    MODIFY COLUMN `RELEASE_STATUS` varchar(30) NOT NULL COMMENT '发布状态，字典：IS_RELEASE，0未发布，1已发布，2发布失败，3发布中，4超时失败，5待发布';

CREATE INDEX `idx_tm_schedule_result_order_no` ON `T_TM_SCHEDULE_RESULT` (`ORDER_NO`);

ALTER TABLE `T_TM_STOCK`
    MODIFY COLUMN `BAD_QTY` decimal(18,6) DEFAULT NULL COMMENT '不良数量';
