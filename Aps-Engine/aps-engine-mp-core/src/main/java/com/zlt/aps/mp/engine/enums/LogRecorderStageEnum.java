package com.zlt.aps.mp.engine.enums;

import lombok.Getter;

/**
 * 排产流程日志记录器阶段枚举定义类
 * 01-初始化
 * 02-在产机台测算阶段
 * 03-模拟排产阶段
 * 04-正式排产阶段
 * 05-搭配排产阶段
 *
 * @author ZLT
 * @date 20251218
 */
@Getter
public enum LogRecorderStageEnum {
    /**
     * 01 初始化阶段
     */
    INIT("01", "初始化阶段"),
    /**
     * 02 在产机台测算阶段
     */
    FIRST_ON_LINE("02", "在产机台测算阶段"),
    /**
     * 03 模拟排产阶段--即成型产能分配
     */
    SIMULATE_PRODUCTION("03", "模拟排产阶段"),
    /**
     * 030101 模拟交付优先-预排排产阶段
     */
    SIMULATE_DELIVERY_PRIORITY_PRODUCTION("030101", "模拟交付优先-预排排产阶段"),
    /**
     * 030102 模拟交付优先-重排在产分组阶段
     */
    SIMULATE_RESET_CONTINUE_PRODUCTION("030102", "模拟交付优先-重排在产分组阶段"),
    /**
     * 030103 模拟交付优先-指定、多段优先阶段
     */
    SIMULATE_FIXED_PRODUCTION("030103", "模拟交付优先-指定、多段优先阶段"),
    /**
     * 030104 模拟交付优先-后续阶段排产阶段
     */
    SIMULATE_LAST_PRODUCTION("030104", "模拟交付优先-最后分配阶段"),
    /**
     * 030201 模拟效率优先-在产机台收尾排产阶段
     */
    SIMULATE_EFFICIENCY_PRIORITY_PRODUCTION("030201", "模拟效率优先-在产机台收尾排产阶段"),
    /**
     * 030202 模拟效率优先-新增分组排产阶段
     */
    SIMULATE_EFFICIENCY_ADD_PRODUCTION("030202", "模拟效率优先-新增分组排产阶段"),
    /**
     * 0303 模拟排产-月末补充分配阶段
     */
    SIMULATE_SUPPLEMENT_PRODUCTION("0303", "模拟排产-月末补充分配阶段"),
    /**
     * 04 正式排产阶段-续作
     */
    FORMAL_PRODUCTION("04", "正式排产阶段"),
    /**
     * 0401 正式排产阶段-续作
     */
    FORMAL_PRODUCTION_CONTINUE("0401", "正式排产阶段-续作"),
    /**
     * 0402 正式排产阶段-最低实单
     */
    FORMAL_PRODUCTION_MIN_LH_MACHINE("0402", "正式排产阶段-最低实单"),
    /**
     * 0403 正式排产阶段-前半段机台
     */
    FORMAL_PRODUCTION_BEFORE_LH_MACHINE("0403", "正式排产阶段-前半段机台"),
    /**
     * 0404 正式排产阶段-后半段机台
     */
    FORMAL_PRODUCTION_FINAL_LH_MACHINE("0404", "正式排产阶段-后半段机台"),
    /**
     * 05 搭配排产阶段
     */
    matchingProductionHandler("05", "搭配排产阶段");

    private String stageCode;

    private String stageDesc;

    LogRecorderStageEnum(String stageCode, String stageDesc) {
        this.stageCode = stageCode;
        this.stageDesc = stageDesc;
    }
}
