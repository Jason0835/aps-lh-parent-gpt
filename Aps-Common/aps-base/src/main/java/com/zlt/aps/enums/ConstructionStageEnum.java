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
    NO_CONSTRUCTION("00", "无施工", "00", 99),
    /**
     * 1 试制
     */
    MEASUREMENT("01", "试制", ConstructionStageEnum.MEASUREMENT_FLAG, 3),
    /**
     * 2 量试
     */
    TRIAL_PRODUCTION("02", "量试", ConstructionStageEnum.TRIAL_FLAG, 2),
    /**
     * 3 正式
     */
    FORMAL_PRODUCTION("03", "正式", ConstructionStageEnum.FORMAL_FLAG, 1);
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
    /**
     * 标记
     */
    private String markFlag;

    private String desc;
    /**
     * 顺序
     */
    private Integer sort;

    ConstructionStageEnum(String stage, String desc, String markFlag, Integer sort) {
        this.stage = stage;
        this.desc = desc;
        this.markFlag = markFlag;
        this.sort = sort;
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
     * 根据标记获取
     *
     * @param markFlag 标记值
     * @return
     */
    public static ConstructionStageEnum matchByMarkFlag(String markFlag) {
        if (StringUtils.isBlank(markFlag)) {
            return ConstructionStageEnum.NO_CONSTRUCTION;
        }
        if (ConstructionStageEnum.FORMAL_FLAG.equalsIgnoreCase(markFlag)) {
            return ConstructionStageEnum.FORMAL_PRODUCTION;
        }
        if (ConstructionStageEnum.TRIAL_FLAG.equalsIgnoreCase(markFlag)) {
            return ConstructionStageEnum.TRIAL_PRODUCTION;
        }
        if (ConstructionStageEnum.MEASUREMENT_FLAG.equalsIgnoreCase(markFlag)) {
            return ConstructionStageEnum.MEASUREMENT;
        }
        return ConstructionStageEnum.NO_CONSTRUCTION;
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

    public String getMarkFlag() {
        return markFlag;
    }

    public Integer getSort() {
        return sort;
    }
}
