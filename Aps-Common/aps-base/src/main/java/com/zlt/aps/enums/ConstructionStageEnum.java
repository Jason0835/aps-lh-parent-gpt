package com.zlt.aps.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * 施工阶段
 *
 * @author ZLT
 * 20250307
 */
public enum ConstructionStageEnum {
    /**
     * 0 无施工
     */
    NO_CONSTRUCTION("00", "无施工"),
    /**
     * 1 试制
     */
    MEASUREMENT("01", "试制"),
    /**
     * 2 量试
     */
    TRIAL_PRODUCTION("02", "量试"),
    /**
     * 3 正式
     */
    FORMAL_PRODUCTION("03", "正式");
    /**
     * T 量试标记
     */
    public static final String TRIAL_FLAG = "T";
    /**
     * X 试制标记
     */
    public static final String MEASUREMENT_FLAG = "X";
    /**
     * S 正式标记
     */
    public static final String FORMAL_FLAG = "S";

    private String stage;

    private String desc;

    ConstructionStageEnum(String stage, String desc) {
        this.stage = stage;
        this.desc = desc;
    }

    /**
     * 根据施工号，匹配施工阶段
     * 空白：代表还没有工艺规格
     * 试制，施工号第一码为X，不排产
     * 量试，施工号第一码为T，不排产
     * 正式，施工号第一码为S，正式会排产
     *
     * @param constructionCode
     * @return
     */
    public static ConstructionStageEnum matchByConstructionCode(String constructionCode) {
        if (StringUtils.isBlank(constructionCode)) {
            return ConstructionStageEnum.NO_CONSTRUCTION;
        }
        if (constructionCode.startsWith(ConstructionStageEnum.TRIAL_FLAG)) {
            return ConstructionStageEnum.TRIAL_PRODUCTION;
        }
        if (constructionCode.startsWith(ConstructionStageEnum.MEASUREMENT_FLAG)) {
            return ConstructionStageEnum.MEASUREMENT;
        }
        if (constructionCode.startsWith(ConstructionStageEnum.FORMAL_FLAG)) {
            return ConstructionStageEnum.FORMAL_PRODUCTION;
        }
        return null;
    }

    /**
     * 根据施工阶段值，获取施工枚举实例
     *
     * @param stage
     * @return
     */
    public static ConstructionStageEnum getInstance(String stage) {
        if (null == stage) {
            return ConstructionStageEnum.NO_CONSTRUCTION;
        }
        for (ConstructionStageEnum constructionStage : values()) {
            if (constructionStage.getStage().equals(stage)) {
                return constructionStage;
            }
        }
        return ConstructionStageEnum.NO_CONSTRUCTION;
    }

    public String getStage() {
        return stage;
    }

    public String getDesc() {
        return desc;
    }
}
