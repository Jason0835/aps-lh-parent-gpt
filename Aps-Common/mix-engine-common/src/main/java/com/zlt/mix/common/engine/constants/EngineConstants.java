package com.zlt.mix.common.engine.constants;

/**
  * 引擎常量类
**/
public class EngineConstants {

    /**
     * 工序类型：终炼母炼排程
     */
    public static String PROCEDURE_CODE_GLUE = "0";

    /**
     * 工序类型：硫磺辅料排程
     */
    public static String PROCEDURE_CODE_MATERIAL = "1";

    /**
     * 分厂胶料需求计划批次号前缀
     */
    public static String DEMAND_PREFIX = "DEMAND";

    /**
     * 汇总胶料需求计划批次号前缀
     */
    public static String COLLECT_PREFIX = "COLLECT";

    /**
     * 分解胶料需求量表批次号前缀
     */
    public static String DECOMPOSE_PREFIX = "DECOMPOSE";

    /**
     * 终炼/母炼日计划排程表批次号前缀
     */
    public static String GLUE_SCHEDULE_PREFIX = "GLUE";

    /**
     * 硫化辅料日计划排程表批次号前缀
     */
    public static String MATERIAL_SCHEDULE_PREFIX = "MATERIAL";

    /**
     * 收尾计划标识： 是
     */
    public static String IS_FINISHING_YES = "1";

    /**
     * 密炼区：默认
     */
    public static String MIX_AREA_DEFAULT = "0";

    /**
     * 配方类型：ZZ
     */
    public static String RECIPE_TYPE_ZZ = "1";

    /**
     * 常用规格天数
     */
    public static String COMMONLY_USED_DAY = "COMMONLY_USED_DAY";

    /**
     * 常用规格安全库存率
     */
    public static String SAFE_STOCK_RATE = "SAFE_STOCK_RATE";

    /**
     * 不同规格直接的间隔时间
     */
    public static String MATERIAL_INTERVAL_TIME = "MATERIAL_INTERVAL_TIME";

    /**
     * 前置准备时间（单位：分）
     */
    public static String PRE_PREPARE_TIME = "PRE_PREPARE_TIME";

    /**
     * 机台默认产能
     */
    public static String MACHINE_DEFAULT_CAPACITY = "MACHINE_DEFAULT_CAPACITY";

    /**
     * 用餐时间（分钟）
     */
    public static final String DINNER_TIME = "DINNER_TIME";

    /**
     * 分解胶料方式
     */
    public static final String DECOMPOSE_GLUE_TYPE = "DECOMPOSE_GLUE_TYPE";

    /**
     * 班制：长白班
     */
    public static Integer CLASS_SHIFT_ONE = 1;

    /**
     * 班制：两班制
     */
    public static Integer CLASS_SHIFT_TWO = 2;

    /**
     * 班制：三班制
     */
    public static Integer CLASS_SHIFT_THREE = 3;

    /**
     * 中班
     */
    public static String CLASS_MID = "MID";

    /**
     * 夜班
     */
    public static String CLASS_NIGHT = "NIGHT";

    /**
     * 白班
     */
    public static String CLASS_DAY = "DAY";

    /**
     * 硫磺辅料是否是常用规格：是
     */
    public static Integer COMMONLY_USED_YES = 1;

    /**
     * 硫磺辅料是否是常用规格：否
     */
    public static Integer COMMONLY_USED_NO = 0;

    /**
     *  顺序之间的倍数
     */
    public static int ORDER_MULTIPLE = 10;

    /**
     * 需求计划时间和计划量直接的分隔符
     */
    public static String DEMAND_PLANNING_DIVISION = "--";

    /**
     * 分解胶料方式：拿“终炼母炼分解表”来进行分解
     */
    public static String DECOMPOSE_GLUE_TYPE_0 = "0";
}
