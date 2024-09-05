package com.zlt.aps.lh.engine.constants;

/**
 * 硫化工序引擎工序参数key
 */
public class LhEngineParamCodeConstants {

    /**
     * 硫化工序 停汽规则班次参数
     */
    public static final String OCCLUSION_CLASS_SHIFT="OCCLUSION_CLASS_SHIFT";

    /**
     * 成型工序 单个班次总时长分钟数据参数code
     */
    public static final String CLASS_SHIFT_MAX_TIME="CLASS_SHIFT_MAX_TIME";

    /**
     * 成型工序 计算可硫化时长中固定损耗时间分钟数参数CODE
     */
    public static final String CLASS_LOSSRATE_TIME ="CLASS_LOSSRATE_TIME";

    /**
     * 成型工序 计算可硫化时长中固定2分钟刷囊时间参数设定code
     */
    public static final String BRUSH_BAG_TIME="BRUSH_BAG_TIME";

    /**
     * 硫化参数：表示成型待料时间参数key
     */
    public static final String CX_WAIT_MATERIAL_HOUR="CX_WAIT_MATERIAL_HOUR";
    /**
     * 硫化参数：开汽标记判断时间参数KEY
     */
    public static final String OPEN_STREAM_END_HOUR="OPEN_STREAM_END_HOUR";

    /**
     * 硫化参数：更换模具小时数
     */
    public static final String CHANGE_MOLD_TIME_HOUR="CHANGE_MOLD_TIME_HOUR";

    /**
     * 硫化参数：当班换模后可安排最大计划量小时
     */
    public static final String ALLOW_MAX_PLAN_HOUR="ALLOW_MAX_PLAN_HOUR";

    /**
     * 硫化参数：当班开班最大计划量参数
     */
    public static final String OPEN_SHIFT_MAX_PLAN="OPEN_SHIFT_MAX_PLAN";

    public static final String OPEN_SHIFT_MIN_PLAN="OPEN_SHIFT_MIN_PLAN";

    /**
     * 硫化结束时间与胎胚开始时间时间差小时数，可以进行当班成型计划预计库存
     */
    public static final String END_DIFF_START_TIME_HOUR="END_DIFF_START_TIME_HOUR";

    /**
     * 往前追溯的最大天数
     */
    public static final String TRACE_MAX_DAYS="TRACE_MAX_DAYS";

    /**
     * 硫化参数：开汽班次热模时间
     * add by nick 2023-05-11
     */
    public static final String OPEN_STREAM_PREHEAT_TIME="OPEN_STREAM_PREHEAT_TIME";
}
