package com.zlt.aps.cx.engine.constants;

/**
 * 成型工序参数代码
 */
public class CxParamCodeConstants {
    /**
     * 默认单模可硫化班次参数code
     */
    public static final String DEFAULT_LH_CLASS_SHIFTS="DEFAULT_LH_CLASS_SHIFTS";

    /**
     * 默认双模可硫化班次参数code
     */
    public static final String DEFAULT_DOUBLE_LH_CLASS_SHIFTS="DEFAULT_DOUBLE_LH_CLASS_SHIFTS";

    /**
     * 最大可硫化班次参数code
     */
    public static final String MAX_LH_CLASS_SHIFTS="MAX_LH_CLASS_SHIFTS";

    /**
     * 可投产新规格限定平均硫化班次参数code
     */
    public static final String ADD_SPEC_AVG_LH_CLASS_SHIFTS="ADD_SPEC_AVG_LH_CLASS_SHIFTS";

    /**
     * 大换工装时长参数key
     */
    public static final String CX_MAX_CHANGE_SPEC_TIME="CX_MAX_CHANGE_SPEC_TIME";


    /**
     * 小换工装时长参数key
     */
    public static final String CX_MIN_CHANGE_SPEC_TIME="CX_MIN_CHANGE_SPEC_TIME";

    /**
     * 规格可连续生产班次工序参数code
     */
    public static final String  SPEC_CONTINUE_PRODUCT_SHIFTS="SPEC_CONTINUE_PRODUCT_SHIFTS";

    /**
     * 设定一次性安排投产月度剩余量参数code
     */
    public static final String ONCE_CLOSE_OUT_QTY="ONCE_CLOSE_OUT_QTY";

    /**
     * 限定投产量最小班次工序参数code
     */
    public static final String PRODUCT_SPEC_LIMIT_SHIFT="PRODUCT_SPEC_LIMIT_SHIFT";

    /**
     *  最小班次小于限定班次可投产数量工序参数code
     */
    public static final String PRODUCT_SPEC_LIMIT_QTY="PRODUCT_SPEC_LIMIT_QTY";

    /**
     * 收尾提示工序参数code
     */
    public static final String CLOSE_OUT_TIP_QTY="CLOSE_OUT_TIP_QTY";

    /**
     * 原因分析标注剩余X收尾参数code
     */
    public static final String ANALYSIS_MARK_QTY="ANALYSIS_MARK_QTY";

    /**
     * 成型一次法胎胚前缀
     */
    public static final String ONCE_EMBRYOCODE_PREFIX="ONCE_EMBRYOCODE_PREFIX";

    /**
     * 成型二次法胎胚前缀
     */
    public static final String TWICE_EMBRYOCODE_PREFIX="TWICE_EMBRYOCODE_PREFIX";

    /**
     * 单个班次总时长分钟数据参数code
     */
    public static final String CLASS_SHIFT_MAX_TIME="CLASS_SHIFT_MAX_TIME";

    /**
     * 计算可硫化时长中固定损耗时间分钟数参数CODE
     */
    public static final String CLASS_LOSSRATE_TIME ="CLASS_LOSSRATE_TIME";

    /**
     * 计算可硫化时长中固定2分钟刷囊时间参数设定code
     */
    public static final String BRUSH_BAG_TIME="BRUSH_BAG_TIME";

    /**
     * 新规格投产最小班次可硫化班数
     */
    public static final String ADD_SPEC_LIMIT_SHIFT="ADD_SPEC_LIMIT_SHIFT";

    /**
     * 剩余量不排产比例设定（%）
     */
    public static final String MONTH_REMAIN_QTY_MIN_PERCENT="MONTH_REMAIN_QTY_MIN_PERCENT";

    /**
     * 月度计划量不排产设定值
     * 设置月度计划量不排产设定值，若该月度计划>=设定值（500），则月度计划的2%，可以不排产
     */
    public static final String MONTH_PLAN_NO_SCHEDULE_VALUE="MONTH_PLAN_NO_SCHEDULE_VALUE";

    /**
     * 中夜班完成量和计划量差异补产限定条件参数key
     */
    public static final String FINISH_PLAN_DIFF_CONDITION="FINISH_PLAN_DIFF_CONDITION";

    /**
     * 成型胎胚验证是否完整开关 Y:表示进行验证；N表示不进行验证
     */
    public static final String VALIDATE_CONSTRUCTION_SWITCH="VALIDATE_CONSTRUCTION_SWITCH";

    /**
     * 成型添加新规格时，单班硫化总量与定额差额允许范围最大值
     */
    public static final String SHIFT_QUOTA_DIFF_MAX="SHIFT_QUOTA_DIFF_MAX";

    /**
     * 成型添加新规格时，单班硫化总量与定额差额允许范围最小值
     */
    public static final String SHIFT_QUOTA_DIFF_MIN="SHIFT_QUOTA_DIFF_MIN";

    /**
     * 允许投产列表在排产日期之后的天数
     */
    public static final String MAX_PRODUCT_END_DATE_STEP="MAX_PRODUCT_END_DATE_STEP";
}
