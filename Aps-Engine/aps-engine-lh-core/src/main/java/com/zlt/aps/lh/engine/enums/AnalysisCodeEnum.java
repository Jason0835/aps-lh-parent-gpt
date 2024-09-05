package com.zlt.aps.lh.engine.enums;

import lombok.Getter;

/**
 * 原因分析类型码枚举
 */
@Getter
public enum AnalysisCodeEnum {

    CHANGE_MOLD("1","换模"),STREAM_OFF("2","停汽"),STREAM_ON("3","开汽"),OPEN_SHIFT("4","开班"),RESOURCE_LACK("5","成型待料");
    private String analysisCode;
    private String analysisName;

    private AnalysisCodeEnum(String analysisCode, String analysisName){
        this.analysisCode=analysisCode;
        this.analysisName=analysisName;
    }

    /**
     * 根据下标获取
     * @param analysisCode
     * @return
     */
    public static AnalysisCodeEnum getAnalysisCodeEnums(String analysisCode) {
        for (AnalysisCodeEnum analysisCodeEnum : AnalysisCodeEnum.values()) {
            if (analysisCodeEnum.getAnalysisCode().equals(analysisCode)) {
                return analysisCodeEnum;
            }
        }
        return null;
    }
}
