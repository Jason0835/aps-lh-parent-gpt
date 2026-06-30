SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- APS 胎面排程重构建表脚本
-- 数据库: MySQL 8.x
-- 字符集: utf8mb4
-- 存储引擎: InnoDB
-- =========================================================

-- =========================================================
-- 1. 排程结果（六班一行横向模型，参考胎圈 T_TQ_SCHEDULE_RESULT）
-- =========================================================
DROP TABLE IF EXISTS `T_TM_SCHEDULE_RESULT`;
CREATE TABLE `T_TM_SCHEDULE_RESULT` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `batch_no` varchar(64) NOT NULL COMMENT '批次号',
  `order_no` varchar(64) DEFAULT NULL COMMENT '工单号',
  `schedule_date` date NOT NULL COMMENT '排程日期',
  `machine_code` varchar(50) DEFAULT NULL COMMENT '机台编码，关联T_TM_MACHINE_INFO.machine_code',
  `tread_code` varchar(50) NOT NULL COMMENT '胎面编码',
  `glue_code` varchar(50) DEFAULT NULL COMMENT '主胶料编码',
  `whole_glue_code` varchar(100) DEFAULT NULL COMMENT '整条胶料组合编码',
  `glue_seq` varchar(30) DEFAULT NULL COMMENT '胶料顺序',
  `mouth_plate_code` varchar(50) DEFAULT NULL COMMENT '口型板编码',
  `class1_sequence` int DEFAULT NULL COMMENT '1班顺序',
  `class1_start_time` datetime DEFAULT NULL COMMENT '1班预计开始时间',
  `class1_end_time` datetime DEFAULT NULL COMMENT '1班预计结束时间',
  `class1_plan_qty` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '1班计划量',
  `class1_finish_qty` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '1班完成量',
  `class1_analysis` varchar(500) DEFAULT NULL COMMENT '1班原因分析',
  `class2_sequence` int DEFAULT NULL COMMENT '2班顺序',
  `class2_start_time` datetime DEFAULT NULL COMMENT '2班预计开始时间',
  `class2_end_time` datetime DEFAULT NULL COMMENT '2班预计结束时间',
  `class2_plan_qty` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '2班计划量',
  `class2_finish_qty` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '2班完成量',
  `class2_analysis` varchar(500) DEFAULT NULL COMMENT '2班原因分析',
  `class3_sequence` int DEFAULT NULL COMMENT '3班顺序',
  `class3_start_time` datetime DEFAULT NULL COMMENT '3班预计开始时间',
  `class3_end_time` datetime DEFAULT NULL COMMENT '3班预计结束时间',
  `class3_plan_qty` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '3班计划量',
  `class3_finish_qty` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '3班完成量',
  `class3_analysis` varchar(500) DEFAULT NULL COMMENT '3班原因分析',
  `class4_sequence` int DEFAULT NULL COMMENT '4班顺序',
  `class4_start_time` datetime DEFAULT NULL COMMENT '4班预计开始时间',
  `class4_end_time` datetime DEFAULT NULL COMMENT '4班预计结束时间',
  `class4_plan_qty` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '4班计划量',
  `class4_finish_qty` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '4班完成量',
  `class4_analysis` varchar(500) DEFAULT NULL COMMENT '4班原因分析',
  `class5_sequence` int DEFAULT NULL COMMENT '5班顺序',
  `class5_start_time` datetime DEFAULT NULL COMMENT '5班预计开始时间',
  `class5_end_time` datetime DEFAULT NULL COMMENT '5班预计结束时间',
  `class5_plan_qty` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '5班计划量',
  `class5_finish_qty` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '5班完成量',
  `class5_analysis` varchar(500) DEFAULT NULL COMMENT '5班原因分析',
  `class6_sequence` int DEFAULT NULL COMMENT '6班顺序',
  `class6_start_time` datetime DEFAULT NULL COMMENT '6班预计开始时间',
  `class6_end_time` datetime DEFAULT NULL COMMENT '6班预计结束时间',
  `class6_plan_qty` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '6班计划量',
  `class6_finish_qty` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '6班完成量',
  `class6_analysis` varchar(500) DEFAULT NULL COMMENT '6班原因分析',
  `release_status` varchar(30) NOT NULL COMMENT '发布状态，字典：IS_RELEASE，0未发布，1已发布，2发布失败，3发布中，4超时失败，5待发布',
  `data_source` varchar(30) NOT NULL COMMENT '数据来源',
  `tail_flag` char(1) NOT NULL DEFAULT '0' COMMENT '是否收尾任务，字典：biz_yes_no，0否，1是',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  PRIMARY KEY (`id`),
  KEY `idx_tm_schedule_result_batch` (`batch_no`, `schedule_date`, `is_delete`),
  KEY `idx_tm_schedule_result_order_no` (`order_no`),
  KEY `idx_tm_schedule_result_tread` (`tread_code`),
  KEY `idx_tm_schedule_result_glue` (`glue_code`),
  KEY `idx_tm_schedule_result_machine` (`machine_code`),
  KEY `idx_tm_schedule_result_release` (`release_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='胎面排程结果表（六班一行横向模型）';

-- =========================================================
-- 2. 结果解释信息
-- =========================================================
DROP TABLE IF EXISTS `T_TM_SCHEDULE_RESULT_EXPLAIN`;
CREATE TABLE `T_TM_SCHEDULE_RESULT_EXPLAIN` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `result_id` bigint DEFAULT NULL COMMENT '结果ID，已排任务关联T_TM_SCHEDULE_RESULT.id，未排任务为空',
  `batch_no` varchar(64) NOT NULL COMMENT '批次号',
  `base_demand_qty` decimal(18,6) DEFAULT NULL COMMENT '基础需求量，表示直接来源于成型拉动的原始需求',
  `loss_add_qty` decimal(18,6) DEFAULT NULL COMMENT '损耗补偿量，表示按损耗规则补加的数量',
  `stock_deduct_qty` decimal(18,6) DEFAULT NULL COMMENT '库存抵扣量，表示用现有库存冲减的数量',
  `last_shift_supply_qty` decimal(18,6) DEFAULT NULL COMMENT '上班覆盖量，表示上一个班次已提供的供应量',
  `month_surplus_deduct_qty` decimal(18,6) DEFAULT NULL COMMENT '月剩余抵扣量，表示按月剩余量冲减的数量',
  `tool_limit_adjust_qty` decimal(18,6) DEFAULT NULL COMMENT '工装约束调整量，表示因工装限制产生的补正数量',
  `min_start_adjust_qty` decimal(18,6) DEFAULT NULL COMMENT '最小起排补正量，表示因最小起排规则补加的数量',
  `tail_round_adjust_qty` decimal(18,6) DEFAULT NULL COMMENT '收尾取整补正量，表示因收尾规则取整增加的数量',
  `capacity_adjust_qty` decimal(18,6) DEFAULT NULL COMMENT '产能均衡补正量，表示均衡阶段对计划量的调整值',
  `final_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '最终计划量，表示本任务最终确定的计划数量',
  `calc_formula_desc` varchar(1000) DEFAULT NULL COMMENT '计划量计算公式说明',
  `stock_qty` decimal(18,6) DEFAULT NULL COMMENT '当前库存量',
  `plan_stock_qty` decimal(18,6) DEFAULT NULL COMMENT '预计库存量',
  `supply_hours` decimal(18,6) DEFAULT NULL COMMENT '库存供应小时数',
  `coverage_shift_count` decimal(18,6) DEFAULT NULL COMMENT '库存覆盖班次数',
  `last_shift_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '上班计划量',
  `month_surplus_qty` decimal(18,6) DEFAULT NULL COMMENT '月剩余量',
  `required_qty` decimal(18,6) DEFAULT NULL COMMENT '库存抵扣前当前班成型胎面需求量',
  `rule_hit_json` longtext COMMENT '命中规则明细文本，记录最终生效的规则集合',
  `rule_summary_desc` varchar(1000) DEFAULT NULL COMMENT '规则摘要说明',
  `candidate_machine_json` longtext COMMENT '候选机台明细文本，记录候选机台、排序、过滤原因和评分',
  `selected_machine_score` decimal(18,6) DEFAULT NULL COMMENT '最终机台评分值',
  `machine_select_reason` varchar(1000) DEFAULT NULL COMMENT '选机说明',
  `assign_status` varchar(30) NOT NULL COMMENT '分配状态',
  `unplanned_reason_code` varchar(50) DEFAULT NULL COMMENT '未排原因编码',
  `unplanned_reason_desc` varchar(1000) DEFAULT NULL COMMENT '未排原因说明',
  `unplanned_evidence_json` longtext COMMENT '未排证据文本',
  `task_status` varchar(30) DEFAULT NULL COMMENT '任务状态，字典：tm_task_status，PLANNED已计划/LOCKED已锁定/RUNNING生产中/PART_FINISHED部分完成/FINISHED已完成/CANCELLED已取消/SPLIT已拆分',
  `tread_code` varchar(50) DEFAULT NULL COMMENT '胎面编码',
  `glue_code` varchar(50) DEFAULT NULL COMMENT '主胶料编码',
  `base_glue_code` varchar(50) DEFAULT NULL COMMENT '基部胶编码',
  `mouth_plate_code` varchar(50) DEFAULT NULL COMMENT '口型板编码',
  `manual_locked_flag` char(1) NOT NULL DEFAULT '0' COMMENT '是否人工锁定，字典：biz_yes_no，0否，1是',
  `sequence_lock_flag` char(1) NOT NULL DEFAULT '0' COMMENT '是否顺序锁定，字典：biz_yes_no，0否，1是',
  `force_change_flag` char(1) NOT NULL DEFAULT '0' COMMENT '是否强制转机台，字典：biz_yes_no，0否，1是',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '追踪标识',
  `task_business_key` varchar(200) NOT NULL DEFAULT '' COMMENT '任务业务键，由胎面|胶料|口型|班次组成',
  `task_order_no` varchar(100) DEFAULT NULL COMMENT '任务工单号',
  `source_order_nos` varchar(500) DEFAULT NULL COMMENT '聚合前来源成型工单号集合',
  `shift_order` int DEFAULT NULL COMMENT '班次顺序',
  `generate_mode` varchar(30) DEFAULT NULL COMMENT '生成方式',
  `result_status` varchar(30) DEFAULT NULL COMMENT '执行结果',
  `current_step_code` varchar(50) DEFAULT NULL COMMENT '当前步骤编码',
  `sys_analysis` longtext COMMENT '系统分析说明',
  `warning_msg` varchar(1000) DEFAULT NULL COMMENT '告警信息',
  `error_msg` varchar(2000) DEFAULT NULL COMMENT '错误信息',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tm_schedule_result_explain_task` (`batch_no`, `task_business_key`, `is_delete`),
  KEY `idx_tm_schedule_result_explain_batch` (`batch_no`, `is_delete`),
  KEY `idx_tm_schedule_result_explain_assign` (`assign_status`, `unplanned_reason_code`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='胎面排程结果解释信息表';

-- =========================================================
-- 2.1 未排列表
-- =========================================================
DROP TABLE IF EXISTS `T_TM_SCHEDULE_UNPLANNED`;
CREATE TABLE `T_TM_SCHEDULE_UNPLANNED` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `batch_no` varchar(64) NOT NULL COMMENT '批次号',
  `schedule_date` date NOT NULL COMMENT '排程日期',
  `tread_code` varchar(50) NOT NULL COMMENT '胎面编码',
  `glue_code` varchar(50) DEFAULT NULL COMMENT '主胶料编码',
  `mouth_plate_code` varchar(50) DEFAULT NULL COMMENT '口型板编码',
  `unplanned_reason_code` varchar(50) DEFAULT NULL COMMENT '未排原因编码',
  `unplanned_reason_desc` varchar(1000) DEFAULT NULL COMMENT '未排原因说明',
  `unplanned_evidence_json` longtext COMMENT '未排证据文本',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  PRIMARY KEY (`id`),
  KEY `idx_tm_schedule_unplanned_batch` (`batch_no`, `schedule_date`, `is_delete`),
  KEY `idx_tm_schedule_unplanned_tread` (`tread_code`),
  KEY `idx_tm_schedule_unplanned_reason` (`unplanned_reason_code`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='胎面排程未排列表';

-- =========================================================
-- 3. 机台基础
-- =========================================================
DROP TABLE IF EXISTS `T_TM_MACHINE_INFO`;
CREATE TABLE `T_TM_MACHINE_INFO` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `machine_code` varchar(30) DEFAULT NULL COMMENT '机台编码',
  `machine_name` varchar(50) DEFAULT NULL COMMENT '机台名称',
  `max_capacity` decimal(10,2) DEFAULT NULL COMMENT '最大班产',
  `open_shift_code` char(10) DEFAULT NULL COMMENT '开放班次编码',
  `machine_status` varchar(50) DEFAULT NULL COMMENT '机台状态',
  `shift_code` varchar(10) DEFAULT NULL COMMENT '班次编码',
  `remark` varchar(900) DEFAULT NULL COMMENT '备注',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_t_tm_machine_info_code` (`machine_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='S5-0402-胎面机台基础表';

-- =========================================================
-- 4. 机台维修计划
-- =========================================================
DROP TABLE IF EXISTS `T_TM_MACHINE_MAINTENANCE`;
CREATE TABLE `T_TM_MACHINE_MAINTENANCE` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `machine_code` varchar(50) DEFAULT NULL COMMENT '机台编码，关联T_TM_MACHINE_INFO.machine_code',
  `stop_start_time` datetime DEFAULT NULL COMMENT '停机开始时间',
  `stop_end_time` datetime DEFAULT NULL COMMENT '停机结束时间',
  `remark` varchar(900) DEFAULT NULL COMMENT '备注',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_t_tm_machine_maintenance_date` (`stop_start_time`, `machine_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='S5-0404-胎面机台维修计划表';

-- =========================================================
-- 5. 机台生产速度
-- =========================================================
DROP TABLE IF EXISTS `T_TM_MACHINE_SPEED`;
CREATE TABLE `T_TM_MACHINE_SPEED` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `machine_code` varchar(50) DEFAULT NULL COMMENT '机台编码，关联T_TM_MACHINE_INFO.machine_code',
  `tread_code` varchar(30) DEFAULT NULL COMMENT '胎面编码',
  `product_speed` decimal(10,2) DEFAULT NULL COMMENT '生产速度，单位米/秒',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(900) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_t_tm_machine_speed_machine_tread` (`machine_code`, `tread_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='S5-0408-胎面机台生产速度管理表';

-- =========================================================
-- 6. 口型板基础
-- =========================================================
DROP TABLE IF EXISTS `T_TM_MOUTH_PLATE`;
CREATE TABLE `T_TM_MOUTH_PLATE` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `mouth_plate_code` varchar(30) DEFAULT NULL COMMENT '口型板编码',
  `machine_code` varchar(50) DEFAULT NULL COMMENT '机台编码，关联T_TM_MACHINE_INFO.machine_code',
  `plate_status` char(1) DEFAULT NULL COMMENT '口型板状态',
  `remark` varchar(900) DEFAULT NULL COMMENT '备注',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_t_tm_mouth_plate_code` (`mouth_plate_code`),
  KEY `idx_t_tm_mouth_plate_machine` (`machine_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='S5-0802-胎面口型板基础表';

-- =========================================================
-- 7. 定点与禁排机台
-- =========================================================
DROP TABLE IF EXISTS `T_TM_SPECIFY_MACHINE`;
CREATE TABLE `T_TM_SPECIFY_MACHINE` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `tread_code` varchar(20) DEFAULT NULL COMMENT '胎面编码',
  `machine_code` varchar(50) DEFAULT NULL COMMENT '机台编码，关联T_TM_MACHINE_INFO.machine_code',
  `job_type` varchar(10) DEFAULT NULL COMMENT '作业类型，表示指定机台或禁排机台',
  `priority` int DEFAULT NULL COMMENT '优先级，用于同胎面多个规则时排序',
  `enable_status` char(1) NOT NULL DEFAULT '1' COMMENT '是否启用，字典：biz_yes_no，0否，1是',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `remark` varchar(900) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_t_tm_specify_machine_tread` (`tread_code`),
  KEY `idx_t_tm_specify_machine_machine` (`machine_code`),
  KEY `idx_t_tm_specify_machine_enable` (`enable_status`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='S5-0406-胎面定点与禁排机台规则表';

-- =========================================================
-- 8. 胶料机台关系
-- =========================================================
DROP TABLE IF EXISTS `T_TM_GLUE_MACHINE_REAL`;
CREATE TABLE `T_TM_GLUE_MACHINE_REAL` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `glue_code` varchar(20) DEFAULT NULL COMMENT '胶料号',
  `base_glue_code` varchar(60) DEFAULT NULL COMMENT '基部胶编码',
  `machine_code` varchar(50) DEFAULT NULL COMMENT '机台编码，关联T_TM_MACHINE_INFO.machine_code',
  `shift_code` varchar(10) DEFAULT NULL COMMENT '机台班次编码',
  `priority` int DEFAULT NULL COMMENT '优先级，用于胶料可投多机台时排序',
  `allow_flag` char(1) NOT NULL DEFAULT '1' COMMENT '是否允许，字典：biz_yes_no，0否，1是',
  `enable_status` char(1) NOT NULL DEFAULT '1' COMMENT '是否启用，字典：biz_yes_no，0否，1是',
  `remark` varchar(900) DEFAULT NULL COMMENT '备注',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_t_tm_glue_machine_real` (`glue_code`, `machine_code`),
  KEY `idx_t_tm_glue_machine_real_enable` (`enable_status`, `allow_flag`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='S5-0602-胎面胶料与机台关系表';

-- =========================================================
-- 9. 胶料组顺序
-- =========================================================
DROP TABLE IF EXISTS `T_TM_GLUE_GROUP_ORDER`;
CREATE TABLE `T_TM_GLUE_GROUP_ORDER` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `glue_group_code` varchar(30) DEFAULT NULL COMMENT '胶料组编码',
  `glue_group_name` varchar(50) DEFAULT NULL COMMENT '胶料组名称',
  `order_num` int DEFAULT NULL COMMENT '排序号',
  `remark` varchar(900) DEFAULT NULL COMMENT '备注',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_t_tm_glue_group_order_code` (`glue_group_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='S5-0604-胎面胶料组顺序表';

-- =========================================================
-- 10. 胶料顺序
-- =========================================================
DROP TABLE IF EXISTS `T_TM_GLUE_ORDER`;
CREATE TABLE `T_TM_GLUE_ORDER` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `glue_group_code` varchar(30) DEFAULT NULL COMMENT '胶料组编码，关联T_TM_GLUE_GROUP_ORDER.glue_group_code',
  `glue_code` varchar(30) DEFAULT NULL COMMENT '胶料号',
  `machine_code` varchar(50) DEFAULT NULL COMMENT '机台编码，关联T_TM_MACHINE_INFO.machine_code',
  `order_num` int DEFAULT NULL COMMENT '排序号',
  `remark` varchar(900) DEFAULT NULL COMMENT '备注',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_t_tm_glue_order_code` (`glue_code`),
  KEY `idx_t_tm_glue_order_group` (`glue_group_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='胎面胶料顺序表';

-- =========================================================
-- 11. 损耗设置
-- =========================================================
DROP TABLE IF EXISTS `T_TM_LOSS_SETTING`;
CREATE TABLE `T_TM_LOSS_SETTING` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `tread_code` varchar(20) DEFAULT NULL COMMENT '胎面编码',
  `machine_code` varchar(50) DEFAULT NULL COMMENT '机台编码，关联T_TM_MACHINE_INFO.machine_code',
  `loss_rate` decimal(10,2) DEFAULT NULL COMMENT '损耗率',
  `setting_level` varchar(20) DEFAULT NULL COMMENT '配置层级，用于区分规格级、机台级、默认级',
  `priority` int DEFAULT NULL COMMENT '优先级，用于同层级多条配置时排序',
  `enable_status` char(1) NOT NULL DEFAULT '1' COMMENT '是否启用，字典：biz_yes_no，0否，1是',
  `remark` varchar(900) DEFAULT NULL COMMENT '备注',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_t_tm_loss_setting_code` (`tread_code`),
  KEY `idx_t_tm_loss_setting_machine` (`machine_code`),
  KEY `idx_t_tm_loss_setting_enable` (`enable_status`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='S5-1002-胎面损耗设置表';

-- =========================================================
-- 12. 卷曲长度
-- =========================================================
DROP TABLE IF EXISTS `T_TM_CURL_ROLL`;
CREATE TABLE `T_TM_CURL_ROLL` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `tread_code` varchar(20) DEFAULT NULL COMMENT '胎面编码',
  `curl_length` decimal(10,2) DEFAULT NULL COMMENT '卷曲长度',
  `remark` varchar(900) DEFAULT NULL COMMENT '备注',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_t_tm_curl_roll_code` (`tread_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='S5-1004-胎面卷曲长度表';

-- =========================================================
-- 13. 参数配置
-- =========================================================
DROP TABLE IF EXISTS `T_TM_PARAMS`;
CREATE TABLE `T_TM_PARAMS` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `param_code` varchar(50) DEFAULT NULL COMMENT '参数编码',
  `param_name` varchar(50) DEFAULT NULL COMMENT '参数名称',
  `param_value` varchar(200) DEFAULT NULL COMMENT '参数值',
  `default_value` varchar(200) DEFAULT NULL COMMENT '默认值',
  `regular_expression` varchar(200) DEFAULT NULL COMMENT '校验正则',
  `error_tips` varchar(200) DEFAULT NULL COMMENT '错误提示',
  `param_group` varchar(50) DEFAULT NULL COMMENT '参数分组',
  `value_type` varchar(50) DEFAULT NULL COMMENT '参数值类型',
  `enable_status` char(1) NOT NULL DEFAULT '1' COMMENT '是否启用，字典：biz_yes_no，0否，1是',
  `remark` varchar(900) DEFAULT NULL COMMENT '备注',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_t_tm_params_code` (`param_code`),
  KEY `idx_t_tm_params_enable` (`enable_status`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='S5-1202-胎面排程参数配置表';

INSERT INTO `T_TM_PARAMS`
(`factory_code`, `param_code`, `param_name`, `param_value`, `default_value`, `regular_expression`, `error_tips`, `param_group`, `value_type`, `enable_status`, `remark`, `create_by`)
VALUES
('116', 'TM_ALGORITHM_SWITCH', '胎面需求算法开关', '1', '1', '^[12]$', '胎面需求算法开关只能维护1或2', 'TM_SCHEDULE', 'NUMBER', '1', '1表示成型三班最大计划量算法，2表示下个班成型计划算法', 'system'),
('116', 'TM_MIN_STOCK_CLASS', '胎面库存最低保证班数', '1', '1', '^[0-9]+(\\.[0-9]+)?$', '胎面库存最低保证班数必须为非负数字', 'TM_SCHEDULE', 'NUMBER', '1', '计算需排产量时使用的库存最低保证班数', 'system'),
('116', 'TM_MIN_START_QTY', '胎面最小起排量', '300', '300', '^[0-9]+(\\.[0-9]+)?$', '胎面最小起排量必须为非负数字', 'TM_SCHEDULE', 'NUMBER', '1', '需排产量未达到该值时本班不排产', 'system'),
('116', 'TM_VEHICLE_RATE', '胎面工装整车率', '1', '1', '^[0-9]+(\\.[0-9]+)?$', '胎面工装整车率必须为非负数字', 'TM_SCHEDULE', 'NUMBER', '1', '工装可用数量计算使用的整车率', 'system'),
('116', 'TM_OPEN_SHIFT_THRESHOLD', '胎面开班排产阈值', '1', '1', '^[0-9]+(\\.[0-9]+)?$', '胎面开班排产阈值必须为非负数字', 'TM_SCHEDULE', 'NUMBER', '1', '开产班次按库存供应成型时长切换规格时使用', 'system'),
('116', 'TM_MAX_CLASS_QTY', '胎面最大班产限制', '5500', '5500', '^[0-9]+(\\.[0-9]+)?$', '胎面最大班产限制必须为非负数字', 'TM_SCHEDULE', 'NUMBER', '1', '单班最多可排产米数', 'system'),
('116', 'TM_TOOL_TOTAL_QTY', '胎面工装总数', NULL, NULL, '^[0-9]+(\\.[0-9]+)?$', '胎面工装总数必须为非负数字', 'TM_SCHEDULE', 'NUMBER', '1', '工装限制排产量时使用，按现场实际数量维护', 'system'),
('116', 'TM_SPEC_CHANGE_MINUTES', '胎面规格切换时长', '0', '0', '^[0-9]+(\\.[0-9]+)?$', '胎面规格切换时长必须为非负数字', 'TM_SCHEDULE', 'NUMBER', '1', '计算机台剩余产能和预计开始时间时使用，单位分钟', 'system'),
('116', 'TM_GLUE_CHANGE_MINUTES', '胎面胶料切换时长', '0', '0', '^[0-9]+(\\.[0-9]+)?$', '胎面胶料切换时长必须为非负数字', 'TM_SCHEDULE', 'NUMBER', '1', '计算机台剩余产能时使用，单位分钟', 'system'),
('116', 'TM_PROCESS_STANDING_HOURS', '胎面工艺停放时长', '0', '0', '^[0-9]+(\\.[0-9]+)?$', '胎面工艺停放时长必须为非负数字', 'TM_SCHEDULE', 'NUMBER', '1', '计算最晚开始生产时间时使用，单位小时', 'system');

-- =========================================================
-- 14. 库存
-- =========================================================
DROP TABLE IF EXISTS `T_TM_STOCK`;
CREATE TABLE `T_TM_STOCK` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `stock_date` date NOT NULL COMMENT '库存日期',
  `tread_code` varchar(50) NOT NULL COMMENT '胎面编码',
  `stock_qty` decimal(18,6) DEFAULT NULL COMMENT '库存数量',
  `bad_qty` decimal(18,6) DEFAULT NULL COMMENT '不良数量',
  `adjust_qty` decimal(18,6) DEFAULT NULL COMMENT '调整数量',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_t_tm_stock_date_tread` (`stock_date`, `tread_code`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='胎面库存表';

-- =========================================================
-- 15. 班制配置
-- =========================================================
DROP TABLE IF EXISTS `T_TM_SHIFT_CONFIG`;
CREATE TABLE `T_TM_SHIFT_CONFIG` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `shift_code` varchar(30) NOT NULL COMMENT '班次编码',
  `shift_name` varchar(50) NOT NULL COMMENT '班次名称',
  `shift_order` int NOT NULL COMMENT '班次顺序',
  `plan_start_time` varchar(8) NOT NULL COMMENT '计划开始时间(HH:mm:ss)',
  `plan_end_time` varchar(8) NOT NULL COMMENT '计划结束时间(HH:mm:ss)',
  `cross_day_flag` char(1) NOT NULL DEFAULT '0' COMMENT '是否跨天，字典：biz_yes_no，0否，1是',
  `open_flag` char(1) NOT NULL DEFAULT '1' COMMENT '是否开班，字典：biz_yes_no，0否，1是',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tm_shift_config_factory_shift` (`FACTORY_CODE`, `SHIFT_CODE`, `SHIFT_ORDER`, `IS_DELETE`),
  KEY `idx_tm_shift_config_factory_shift` (`FACTORY_CODE`, `SHIFT_CODE`, `SHIFT_ORDER`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='胎面班制配置表';

DROP TABLE IF EXISTS `t_tm_day_finish_qty`;
CREATE TABLE `t_tm_day_finish_qty`  (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `schedule_date` datetime NULL DEFAULT NULL COMMENT '排程时间',
    `tread_code` varchar(20)  NULL DEFAULT NULL COMMENT '胎面代码',
    `class1_finish_qty` double NULL DEFAULT NULL COMMENT '1班完成量',
    `class2_finish_qty` double NULL DEFAULT NULL COMMENT '2班完成量',
    `class3_finish_qty` double NULL DEFAULT NULL COMMENT '3班完成量',
    `class4_finish_qty` double NULL DEFAULT NULL COMMENT '4班完成量',
    `class5_finish_qty` double NULL DEFAULT NULL COMMENT '5班完成量',
    `class6_finish_qty` double NULL DEFAULT NULL COMMENT '6班完成量',
    `remark` varchar(900)  NULL DEFAULT NULL COMMENT '备注',
    `is_delete` int NOT NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
    `create_by` varchar(25)  NULL DEFAULT NULL COMMENT '创建者',
    `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(25)  NULL DEFAULT NULL COMMENT '更新者',
    `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
    `order_no` varchar(20)  NULL DEFAULT NULL COMMENT '工单号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `IDX_TM_DAY_FINISH_DATE_CODE`(`schedule_date`, `tread_code`) USING BTREE,
    INDEX `IDX_TM_DAY_FINISH_ORDER_NO`(`order_no`) USING BTREE
) ENGINE = InnoDB COMMENT = '胎面排程计划每日各班完成量';


DROP TABLE IF EXISTS `t_tm_day_finish_total`;
CREATE TABLE `t_tm_day_finish_total`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `schedule_date` datetime NULL DEFAULT NULL COMMENT '排程时间',
  `tread_code` varchar(20)  NULL DEFAULT NULL COMMENT '胎面代码',
  `finish_qty` double NULL DEFAULT NULL COMMENT '日完成量',
  `remark` varchar(900)  NULL DEFAULT NULL COMMENT '备注',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  `create_by` varchar(25)  NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(25)  NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `IDX_TM_DAY_FINISH_DATE_CODE2`(`schedule_date`, `tread_code`) USING BTREE
) ENGINE = InnoDB COMMENT = '胎面日完成量汇总';

-- =========================================================
-- 16. 调度操作日志
-- =========================================================
DROP TABLE IF EXISTS `T_TM_DISPATCHER_LOG`;
CREATE TABLE `T_TM_DISPATCHER_LOG` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编号',
  `batch_no` varchar(64) NOT NULL COMMENT '批次号',
  `schedule_id` bigint DEFAULT NULL COMMENT '排程结果ID，关联T_TM_SCHEDULE_RESULT.id',
  `oper_type` varchar(2) DEFAULT NULL COMMENT '操作类型：0-转机台、1-调量',
  `schedule_date` date NOT NULL COMMENT '排程日期',
  `tread_code` varchar(50) DEFAULT NULL COMMENT '胎面编码',
  `before_machine_code` varchar(500) DEFAULT NULL COMMENT '操作前机台编码，多个逗号分割',
  `after_machine_code` varchar(500) DEFAULT NULL COMMENT '操作后机台编码，多个逗号分割',
  `before_class1_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '操作前1班计划量',
  `before_class2_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '操作前2班计划量',
  `before_class3_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '操作前3班计划量',
  `before_class4_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '操作前4班计划量',
  `before_class5_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '操作前5班计划量',
  `before_class6_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '操作前6班计划量',
  `after_class1_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '操作后1班计划量',
  `after_class2_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '操作后2班计划量',
  `after_class3_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '操作后3班计划量',
  `after_class4_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '操作后4班计划量',
  `after_class5_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '操作后5班计划量',
  `after_class6_plan_qty` decimal(18,6) DEFAULT NULL COMMENT '操作后6班计划量',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  PRIMARY KEY (`id`),
  KEY `idx_tm_dispatcher_log_schedule_id` (`schedule_id`),
  KEY `idx_tm_dispatcher_log_batch` (`batch_no`, `schedule_date`, `is_delete`),
  KEY `idx_tm_dispatcher_log_tread` (`tread_code`),
  KEY `idx_tm_dispatcher_log_oper` (`oper_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='胎面调度员排程操作日志表';

-- =========================================================
-- 16. 备库班数配置
-- =========================================================
DROP TABLE IF EXISTS `T_TM_DEPTH_CONFIG`;
CREATE TABLE `T_TM_DEPTH_CONFIG` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(50) NOT NULL DEFAULT '' COMMENT '工厂编码',
  `machine_qty` int NOT NULL COMMENT '硫化机数量',
  `machine_range` varchar(30) NOT NULL COMMENT '机台范围，字典machine_range',
  `depth_class_qty` decimal(18,6) NOT NULL DEFAULT '0.000000' COMMENT '保证班数',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除，0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tm_depth_config_factory_range` (`factory_code`, `machine_range`, `machine_qty`, `is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备库班数配置表';

SET FOREIGN_KEY_CHECKS = 1;
