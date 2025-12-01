package com.zlt.aps.lh.api.enums;

import lombok.Getter;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2025/2/25
 */
@Getter
public enum LhParamCodeEnums implements PublicEnum{

    //班制
    CLASS_SYSTEM("班制", "CLASS_SYSTEM"),
    //班制往前推的天数（-1）
    CLASS_SYSTEM_START_DAYS("班制往前推的天数", "CLASS_SYSTEM_START_DAYS"),
    //班制开始小时_(0-24)小时
    CLASS_SYSTEM_START_HOURS("班制开始小时", "CLASS_SYSTEM_START_HOURS"),
    //班制开始分钟_(0-59)分钟
    CLASS_SYSTEM_START_MINUTES("班制开始分钟", "CLASS_SYSTEM_START_MINUTES"),
    //是否启用计划延误自动增补
    IS_START_PLAN_DELAY_AUTO_SUPPLE("是否启用计划延误自动增补","IS_START_PLAN_DELAY_AUTO_SUPPLE"),
    //前日排程往前追溯天数
    LAST_SCHEDULE_TRACE_DAYS("前日排程往前追溯天数","LAST_SCHEDULE_TRACE_DAYS"),
    //是否允许提前排产规格
    IS_ALLOW_ADVANCE_SCHEDULE_SPEC("是否允许提前排产规格","IS_ALLOW_ADVANCE_SCHEDULE_SPEC"),
    //试产试制规格前缀
    TRIAL_PRODUCTION_PRE_FIX("试产试制规格前缀","TRIAL_PRODUCTION_PRE_FIX"),
    //首排规格判断时间（天数）
    FIRST_SKU_CHECK_TIME("首排规格判断时间（天数）","FIRST_SKU_CHECK_TIME"),
    //首排规格排产计划量（条）
    FIRST_SKU_SCHEDULE_NUM("首排规格排产计划量（条）","FIRST_SKU_SCHEDULE_NUM"),
    //辅助时间，包括检查轮胎、喷脱模剂、胶囊定型等
    BRUSH_BAG_TIME("辅助时间","BRUSH_BAG_TIME"),
    //机械式设备操作时长
    MECHANICAL_MACHINE_OPER_TIME("机械式设备操作时长","MECHANICAL_MACHINE_OPER_TIME"),
    //液压式设备操作时长
    HYDRAULIC_MACHINE_OPER_TIME("液压式设备操作时长","HYDRAULIC_MACHINE_OPER_TIME"),
    //换模时长
    CHANGE_MOULD_TIME("换模时长","CHANGE_MOULD_TIME"),
    //日排程总计划量限制
    TOTAL_PLAN_NUM_LIMIT("日排程总计划量限制","TOTAL_PLAN_NUM_LIMIT"),
    //启用夏季硫化日期
    START_CURING_SUMMER_DAY("启用夏季硫化日期","START_CURING_SUMMER_DAY"),
    //启用冬季硫化日期
    START_CURING_WINTER_DAY("启用冬季硫化日期","START_CURING_WINTER_DAY"),

    BRAND_ORDER("品牌优先生产排序","BRAND_ORDER"),
    // 换模次数限制
    CHANGE_MOULD_LIMIT("换模次数限制","CHANGE_MOULD_LIMIT"),

    ;
    private String code;

    private String name;

    LhParamCodeEnums(String name, String code) {
        this.code = code;
        this.name = name;
    }
}
