package com.zlt.aps.dj.engine.constant;

import java.math.BigDecimal;

/**
 * 垫胶自动排程算法常量定义
 */
public class DjEngineConstants {

    // ==================== 排产参数默认值 ====================

    /** 卷曲标准长度（米/卷），当 DjCurlRoll 未配置时使用 */
    public static final BigDecimal DEFAULT_CURL_LENGTH = new BigDecimal("84");

    /** 工装（台车）总数 */
    public static final int DEFAULT_TOOL_TOTAL_NUM = 10;

    /** 整车率，取值范围 0~1 */
    public static final BigDecimal DEFAULT_TROLLEY_FULL_RATE = new BigDecimal("0.8");

    /** 成型机台数与排产深度映射，格式 "机台数1:班数1,机台数2:班数2" */
    public static final String DEFAULT_CX_MACHINE_DEPTH_MAP = "1:4";

    /** 全局默认损耗率 */
    public static final BigDecimal DEFAULT_LOSS_RATE = BigDecimal.ZERO;

    /** 切换口型时长（小时） */
    public static final BigDecimal DEFAULT_MOUTH_PLATE_SWITCH_TIME = new BigDecimal("0.5");

    /** 停产天数阈值（天） */
    public static final int DEFAULT_SHUTDOWN_DAYS_THRESHOLD = 2;

    /** 安全水位线（个班次） */
    public static final int DEFAULT_SAFETY_STOCK_LEVEL = 2;

    /** 切换胶料时长（小时） */
    public static final BigDecimal DEFAULT_GLUE_SWITCH_TIME = BigDecimal.ONE;

    // ==================== 参数编码常量（对应 DjParams.PARAM_CODE） ====================
    // 编码规则：SYS + 工序号(14) + 分组号(01) + 3位流水号
    /** SYS1401001 卷曲标准长度 */
    public static final String PARAM_STANDARD_CRIMP_LENGTH = "SYS1401001";
    /** SYS1401002 工装（台车）总数 */
    public static final String PARAM_TOOL_TOTAL_NUM = "SYS1401002";
    /** SYS1401003 整车率 */
    public static final String PARAM_TROLLEY_FULL_RATE = "SYS1401003";
    /** SYS1401004 成型机台数与排产深度映射 */
    public static final String PARAM_CX_MACHINE_DEPTH_MAP = "SYS1401004";
    /** SYS1401005 安全水位线 */
    public static final String PARAM_SAFETY_STOCK_LEVEL = "SYS1401005";
    /** SYS1401006 停产天数阈值 */
    public static final String PARAM_SHUTDOWN_DAYS_THRESHOLD = "SYS1401006";
    /** SYS1401007 全局默认损耗率 */
    public static final String PARAM_LOSS_RATE = "SYS1401007";
    /** SYS1401008 切换口型时长 */
    public static final String PARAM_MOUTH_PLATE_SWITCH_TIME = "SYS1401008";
    /** SYS1401009 切换胶料时长 */
    public static final String PARAM_GLUE_SWITCH_TIME = "SYS1401009";

    // ==================== 班次常量 ====================

    /** 垫胶排产班次数 */
    public static final int SHIFT_COUNT = 6;

    /** 成型计划班次数 */
    public static final int CX_SHIFT_COUNT = 8;

    /** 每班小时数 */
    public static final BigDecimal SHIFT_HOURS = new BigDecimal("8");

    /** 班次索引与 ClassNumThreePlanEnums.classIndex 映射
     *  shiftIndex=1(中班)→"03", 2(夜班)→"01", 3(早班)→"02",
     *  shiftIndex=4(中班)→"03", 5(夜班)→"01", 6(早班)→"02" */
    public static final String[] SHIFT_CLASS_MAP = {"03", "01", "02", "03", "01", "02"};

    // ==================== 批次号常量 ====================

    /** 批次号前缀 */
    public static final String BATCH_NO_PREFIX = "DJ";

    /** 批次号日期格式 */
    public static final String BATCH_NO_DATE_FORMAT = "yyyyMMdd";

    /** 批次号 3 位序号格式 */
    public static final String BATCH_NO_SEQ_FORMAT = "%03d";

    /** 批次号内订单号 4 位序号格式 */
    public static final String ORDER_NO_SEQ_FORMAT = "%04d";

    // ==================== 业务常量 ====================

    /** 定点机台作业类型：不可作业 */
    public static final String JOB_TYPE_FORBIDDEN = "1";

    /** 定点机台线路类型：生产线 */
    public static final String LINE_TYPE_PRODUCTION = "0";

    /** 定点机台线路类型：备用线 */
    public static final String LINE_TYPE_BACKUP = "1";

    /** 机台状态：启用 */
    public static final String MACHINE_STATUS_ENABLED = "0";

    // ==================== 单位换算常量 ====================

    /** 毫米转米除数 */
    public static final BigDecimal MM_TO_M_DIVISOR = new BigDecimal("1000");

    /** 成型生产状态：已收尾 */
    public static final String CX_PRODUCTION_STATUS_FINISHED = "2";

    /** 收尾标志：是 */
    public static final String TAIL_FLAG_YES = "1";

    /** 收尾标志：否 */
    public static final String TAIL_FLAG_NO = "0";

    /** 发布状态：未发布 */
    public static final String RELEASE_STATUS_UNPUBLISHED = "0";

    /** 数据来源：自动排程 */
    public static final String DATA_SOURCE_AUTO = "auto";

    // ==================== 动态字段名格式模板 ====================

    /** 班次计划量字段名模板，format: classIndex → class1PlanQty */
    public static final String CLASS_PLAN_QTY_FIELD = "class%dPlanQty";

    /** 班次顺序字段名模板，format: shiftIndex → class1Sequence */
    public static final String CLASS_SEQUENCE_FIELD = "class%dSequence";
}
