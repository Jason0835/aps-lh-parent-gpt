package com.zlt.aps.factory.enums;

import lombok.Getter;

/**
 * 排产阶段
 * 01 测算阶段
 * 02 模拟排产阶段
 * 03 正式排产阶段
 *
 * @author ZLT
 * @date 20260127
 */
@Getter
public enum ProductionStageEnum {
    /**
     * 01 测算阶段
     */
    CALCULATION_STAGE("01", "测算阶段"),
    /**
     * 02 模拟排产
     */
    SIMULATE_STAGE("02", "模拟排产阶段"),
    /**
     * 03 正式排产
     */
    FORMAL_STAGE("03", "正式排产阶段");

    private String stageCode;

    private String stageDesc;

    ProductionStageEnum(String stageCode, String stageDesc) {
        this.stageCode = stageCode;
        this.stageDesc = stageDesc;
    }
}
