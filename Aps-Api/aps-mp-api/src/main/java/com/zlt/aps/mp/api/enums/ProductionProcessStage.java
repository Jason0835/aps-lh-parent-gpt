package com.zlt.aps.mp.api.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * 工厂排产阶段定义
 * 01 一键排产
 * 02 分阶段-初始化
 * 03 分阶段-排结构
 * 04 分阶段-排模具
 *
 * @author ZLT
 * 20260102
 */
public enum ProductionProcessStage {
    /**
     * 01 一键排产
     */
    ONE_CLICK_SCHEDULING("01", "一键排产"),
    /**
     * 02 分阶段-初始化
     */
    STAGE_INIT("02", "分阶段-初始化"),
    /**
     * 03 分阶段-排结构
     */
    STAGE_GROUP("03", "分阶段-排结构"),
    /**
     * 04 分阶段-排模具
     */
    STAGE_MOULDING("04", "分阶段-排模具"),
    /**
     * 00 unknown
     */
    UNKNOWN_LOG("00", "unknown");

    private String stageCode;

    private String desc;

    ProductionProcessStage(String stageCode, String desc) {
        this.stageCode = stageCode;
        this.desc = desc;
    }

    /**
     * 得到排产阶段枚举类
     *
     * @param stageCode
     * @return
     */
    public static ProductionProcessStage getInstance(String stageCode) {
        if (StringUtils.isBlank(stageCode)) {
            return UNKNOWN_LOG;
        }
        return Arrays.stream(values()).filter(processStage -> processStage.getStageCode().equals(stageCode)).findFirst().orElse(UNKNOWN_LOG);
    }

    public String getStageCode() {
        return stageCode;
    }

    public String getDesc() {
        return desc;
    }
}
