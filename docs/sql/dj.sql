/*
 Navicat Premium Data Transfer

 Source Server         : ZLT
 Source Server Type    : MySQL
 Source Server Version : 80100
 Source Host           : 192.168.2.124:3306
 Source Schema         : jy_aps

 Target Server Type    : MySQL
 Target Server Version : 80100
 File Encoding         : 65001

 Date: 16/06/2026 11:37:08
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_dj_curl_roll
-- ----------------------------
CREATE TABLE `t_dj_curl_roll`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应自增序列为：SEQ_PUBLIC',
  `factory_code` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '工厂编码',
  `padding_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '垫胶代码',
  `curl_length` double NULL DEFAULT NULL COMMENT '卷曲长度。此内衬一卷的最大长度，单位：米。',
  `is_delete` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  `create_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `IDX_NC_CURL_ROLL`(`padding_code` ASC, `is_delete` ASC) USING BTREE,
  INDEX `IDX_NC_CURL_ROLL_CODE`(`padding_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '垫胶卷曲信息维护表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_dj_loss_setting
-- ----------------------------
CREATE TABLE `t_dj_loss_setting`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应自增序列为：SEQ_LOSS_SETTING',
  `factory_code` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '工厂编码',
  `padding_code` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '垫胶代码',
  `machine_code` bigint NULL DEFAULT NULL COMMENT '机台编码',
  `loss_rate` double NULL DEFAULT NULL COMMENT '损耗率(百分比)',
  `create_by` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` int NULL DEFAULT 0 COMMENT '删除标识（0未删除；1已删除）',
  `remark` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `IDX_NC_LOSS_SETTING`(`padding_code` ASC, `machine_code` ASC, `is_delete` ASC) USING BTREE,
  INDEX `IDX_NC_LOSS_SETTING_CODE`(`padding_code` ASC) USING BTREE,
  INDEX `IDX_NC_LOSS_SETTING_MACHINE_ID`(`machine_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '垫胶损耗率设定表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_dj_machine_info
-- ----------------------------
CREATE TABLE `t_dj_machine_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '工厂编码',
  `machine_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台编号',
  `machine_name` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台名称，比如：1线、2线',
  `width_min` double NULL DEFAULT NULL COMMENT '前生产机台所生产的胶料最小宽度（米）',
  `width_max` double NULL DEFAULT NULL COMMENT '前生产机台所生产的胶料最大宽度（米）',
  `thick_min` double NULL DEFAULT NULL COMMENT '前生产机台所生产的胶料最小厚度（米）',
  `thick_max` double NULL DEFAULT NULL COMMENT '前生产机台所生产的胶料最大厚度（米）',
  `quata` double NULL DEFAULT NULL COMMENT '生产定额，是指单班一次能生产的量，单位：吨/班',
  `class_shift` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '班制，如：三班制，两班制；对应数据字典CLASS_SHIFT',
  `open_machine_class` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '开机班次，如：中班、夜班；对应数据字典CLASS_NUM',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '0' COMMENT '机台状态，0--启用，1--禁用。对应数据字典STATUS',
  `remark` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  `is_delete` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除.对应数据字典DEL_FLAG',
  `create_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `IDX_NC_MACHINE_INFO`(`machine_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '垫胶机台信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_dj_params
-- ----------------------------
CREATE TABLE `t_dj_params`  (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `FACTORY_CODE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工厂编号',
  `PRODUCT_TYPE_CODE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品品类 数据字典：biz_product_type TBR 全钢 PCR 半钢',
  `BUSINESS_GROUP` char(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务分组--相关联业务参数同组值',
  `PARAM_CODE` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '参数编码',
  `PARAM_NAME` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '参数名称',
  `IS_SHOW` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '是否展现到界面 1 展现 0 不展现',
  `DATA_TYPE` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '数据类型:\r\n            0-字符型\r\n            1-整型\r\n            2-数值型\r\n            3-日期型\r\n            4-时间型\r\n            5-日期时间型\r\n            6-布尔型',
  `DEFAULE_VALUE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '默认值',
  `PARAM_VALUE` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '参数值',
  `REMARK` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `IS_DELETE` decimal(1, 0) NULL DEFAULT 0 COMMENT '是否删除（0：默认未删除 1：已删除）',
  `CREATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `CREATE_BY` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `UPDATE_BY` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE INDEX `IDX_MP_FACTORY_PARAM_PARAM_CODE`(`PARAM_CODE` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '垫胶排产参数' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_dj_specify_machine
-- ----------------------------
CREATE TABLE `t_dj_specify_machine`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '工厂编码',
  `padding_code` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '垫胶代码',
  `machine_code` bigint NULL DEFAULT NULL COMMENT '机台编码',
  `line_type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '线路，数据维护在数据字典：0-生产线、1-备用线',
  `job_type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '作业类型，数据维护在数据字典：0-限制作业；1-不可作业',
  `create_by` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` int NULL DEFAULT 0 COMMENT '删除标识（0未删除；1已删除）',
  `remark` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注说明字段',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `IDX_NC_SPECIFY_MACHINE`(`padding_code` ASC, `machine_code` ASC, `is_delete` ASC) USING BTREE,
  INDEX `IDX_NC_SPECIFY_MACHINE_CODE`(`padding_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '垫胶定点机台表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_dj_stock
-- ----------------------------
CREATE TABLE `t_dj_stock`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应自增序列为：SEQ_NC_STOCK',
  `factory_code` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '工厂编码',
  `stock_date` datetime NULL DEFAULT NULL COMMENT '库存日期',
  `material_code` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '库存物料编号',
  `stock_num` double NULL DEFAULT NULL COMMENT '库存量',
  `modify_num` double NULL DEFAULT NULL COMMENT '修正数量',
  `bad_num` double NULL DEFAULT NULL COMMENT '不良数量',
  `remark` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  `is_delete` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  `create_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `DATA_SOURCE` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '数据来源：MES-MES同步，MANUAL-手动录入',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `IDX_NC_STOCK`(`stock_date` ASC, `material_code` ASC, `is_delete` ASC) USING BTREE,
  INDEX `IDX_NC_STOCK_DATE_CODE`(`stock_date` ASC, `material_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '内衬库存信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_dj_schedule_result
-- ----------------------------
CREATE TABLE `t_dj_schedule_result`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '工厂编号',
  `batch_no` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '批次号',
  `order_no` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '工单号',
  `schedule_date` datetime NULL DEFAULT NULL COMMENT '排程日期',
  `machine_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台编码',
  `padding_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '垫胶编码',
  `padding_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '垫胶物料名',
  `glue_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胶料代码',
  `mouth_plate_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '口型板编码',
  `class1_sequence` int NULL DEFAULT NULL COMMENT '1班顺序',
  `class1_plan_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '1班计划量',
  `class1_analysis` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '1班原因分析',
  `class2_sequence` int NULL DEFAULT NULL COMMENT '2班顺序',
  `class2_plan_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '2班计划量',
  `class2_analysis` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '2班原因分析',
  `class3_sequence` int NULL DEFAULT NULL COMMENT '3班顺序',
  `class3_plan_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '3班计划量',
  `class3_analysis` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '3班原因分析',
  `class4_sequence` int NULL DEFAULT NULL COMMENT '4班顺序',
  `class4_plan_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '4班计划量',
  `class4_analysis` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '4班原因分析',
  `class5_sequence` int NULL DEFAULT NULL COMMENT '5班顺序',
  `class5_plan_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '5班计划量',
  `class5_analysis` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '5班原因分析',
  `class6_sequence` int NULL DEFAULT NULL COMMENT '6班顺序',
  `class6_plan_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '6班计划量',
  `class6_analysis` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '6班原因分析',
  `release_status` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '发布状态',
  `schedule_shift_class` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '排程首班班次（ClassNumThreePlanEnums.classIndex），如 03=中班、01=夜班、02=早班',
  `data_source` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '数据来源',
  `tail_flag` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '是否收尾任务',
  `publish_success_count` int NULL DEFAULT 0 COMMENT '发布成功计数器',
  `stock_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '有效库存量',
  `remark` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  `is_delete` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  `create_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `IDX_DJ_SCHEDULE_DATE`(`schedule_date` ASC) USING BTREE,
  INDEX `IDX_DJ_SCHEDULE_MACHINE`(`machine_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '垫胶排程结果表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_dj_machine_maintenance
-- ----------------------------
CREATE TABLE `t_dj_machine_maintenance`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '工厂编号',
  `machine_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台编码',
  `stop_start_time` datetime NULL DEFAULT NULL COMMENT '停机开始时间',
  `stop_end_time` datetime NULL DEFAULT NULL COMMENT '停机结束时间',
  `stop_shift` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '停机班次，字典：CLASS_NUM',
  `remark` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  `is_delete` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  `create_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `IDX_DJ_MAINTENANCE_MACHINE`(`machine_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '垫胶机台维修计划表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_dj_glue_group_order
-- ----------------------------
CREATE TABLE `t_dj_glue_group_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应自增序列为：SEQ_PUBLIC',
  `glue_group_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胶料组别编码',
  `glue_group_name` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胶料组别名称',
  `order_num` int NULL DEFAULT NULL COMMENT '生产顺序',
  `is_delete` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  `create_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '垫胶胶料组别顺序维护' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_dj_glue_order
-- ----------------------------
CREATE TABLE `t_dj_glue_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应自增序列为：SEQ_PUBLIC',
  `factory_code` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '工厂编码',
  `glue_group_id` bigint NULL DEFAULT NULL COMMENT '胶料组别id，对应t_dj_glue_group_order表主键id',
  `glue_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胶料编号',
  `order_num` int NULL DEFAULT NULL COMMENT '生产顺序',
  `is_delete` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  `create_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `IDX_DJ_GLUE_ORDER_GROUP`(`glue_group_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '垫胶胶料顺序维护' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_dj_schedule_result_log
-- ----------------------------
CREATE TABLE `t_dj_schedule_result_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '工厂编号',
  `batch_no` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '批次号',
  `order_no` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '工单号',
  `schedule_date` datetime NULL DEFAULT NULL COMMENT '排程日期',
  `machine_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台编码',
  `padding_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '垫胶编码',
  `padding_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '垫胶物料名',
  `glue_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '胶料代码',
  `mouth_plate_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '口型板编码',
  `class1_sequence` int NULL DEFAULT NULL COMMENT '1班顺序',
  `class1_plan_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '1班计划量',
  `class1_analysis` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '1班原因分析',
  `class2_sequence` int NULL DEFAULT NULL COMMENT '2班顺序',
  `class2_plan_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '2班计划量',
  `class2_analysis` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '2班原因分析',
  `class3_sequence` int NULL DEFAULT NULL COMMENT '3班顺序',
  `class3_plan_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '3班计划量',
  `class3_analysis` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '3班原因分析',
  `class4_sequence` int NULL DEFAULT NULL COMMENT '4班顺序',
  `class4_plan_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '4班计划量',
  `class4_analysis` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '4班原因分析',
  `class5_sequence` int NULL DEFAULT NULL COMMENT '5班顺序',
  `class5_plan_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '5班计划量',
  `class5_analysis` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '5班原因分析',
  `class6_sequence` int NULL DEFAULT NULL COMMENT '6班顺序',
  `class6_plan_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '6班计划量',
  `class6_analysis` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '6班原因分析',
  `release_status` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '发布状态',
  `schedule_shift_class` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '排程首班班次（ClassNumThreePlanEnums.classIndex），如 03=中班、01=夜班、02=早班',
  `data_source` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '数据来源',
  `tail_flag` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '是否收尾任务',
  `publish_success_count` int NULL DEFAULT 0 COMMENT '发布成功计数器',
  `stock_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '有效库存量',
  `remark` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  `is_delete` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  `create_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `IDX_LOG_SCHEDULE_DATE`(`schedule_date` ASC) USING BTREE,
  INDEX `IDX_LOG_SCHEDULE_MACHINE`(`machine_code` ASC) USING BTREE,
  INDEX `IDX_LOG_BATCH_NO`(`batch_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '垫胶排程结果日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_dj_depth_config
-- 垫胶备库班数与供成型机数配置表
-- 根据成型工序生产某规格所使用的机台数量范围，确定对应的垫胶备库班数
-- ----------------------------
CREATE TABLE `t_dj_depth_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `factory_code` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '工厂编码',
  `machine_qty` int NULL DEFAULT NULL COMMENT '供成型机台数（成型工序生产某垫胶规格所使用的机台数量）',
  `machine_range` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '机台范围（数据字典machine_range，选项：LT-小于、LE-小于等于、EQ-等于、GE-大于等于、GT-大于）',
  `depth_class_qty` decimal(10,2) NULL DEFAULT NULL COMMENT '垫胶备库班数（该机台数范围对应的排产深度/供应窗口班次数）',
  `remark` varchar(900) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  `is_delete` int NULL DEFAULT 0 COMMENT '删除标识：0--正常，1-删除',
  `create_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `IDX_DJ_DEPTH_CONFIG_FACTORY`(`factory_code` ASC) USING BTREE,
  INDEX `IDX_DJ_DEPTH_CONFIG_MACHINE_QTY`(`machine_qty` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '垫胶备库班数与供成型机数配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_dj_day_finish_qty
-- ----------------------------
DROP TABLE IF EXISTS `t_dj_day_finish_qty`;
CREATE TABLE `t_dj_day_finish_qty` (
  `ID`              BIGINT       NOT NULL COMMENT '主键ID',
  `FACTORY_CODE`    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '工厂编码',
  `SCHEDULE_DATE`   DATE         NOT NULL COMMENT '排程日期',
  `PADDING_CODE`    VARCHAR(100) NOT NULL DEFAULT '' COMMENT '垫胶代码',
  `NIGHT_FINISH_QTY` DECIMAL(18,2) DEFAULT 0.00 COMMENT '夜班完成量',
  `DAY_FINISH_QTY`  DECIMAL(18,2) DEFAULT 0.00 COMMENT '早班完成量',
  `MID_FINISH_QTY`  DECIMAL(18,2) DEFAULT 0.00 COMMENT '中班完成量',
  `ORDER_NO`        VARCHAR(100) NOT NULL DEFAULT '' COMMENT '工单号',
  `CREATE_BY`       VARCHAR(64)  DEFAULT '' COMMENT '创建者',
  `CREATE_TIME`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_BY`       VARCHAR(64)  DEFAULT '' COMMENT '更新者',
  `UPDATE_TIME`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `IS_DELETE`       CHAR(1)      DEFAULT '0' COMMENT '删除标记（0=正常，1=删除）',
  `REMARK`          VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='垫胶排程计划每日各班完成量';

SET FOREIGN_KEY_CHECKS = 1;
