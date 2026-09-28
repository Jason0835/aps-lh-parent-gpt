package com.zlt.aps.lh.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 产品状态枚举
 *
 * @author zlt
 */
@Getter
@AllArgsConstructor
public enum TrialStatusEnum {

    TRIAL("X", "试验", "试验 Thử nghiệm"),
    MASS_TRIAL("T", "量试", "量试 Thử nghiệm số lượng"),
    FORMAL("S", "正规", "正规 Chính quy");

    /** 状态编码 */
    private final String code;

    /** 状态描述 */
    private final String description;

    /**
     * 模具交替计划导出使用的中越双语示方类型
     */
    private final String mouldChangePlanExcelText;

    /**
     * 根据编码获取枚举
     *
     * @param code 状态编码
     * @return 产品状态枚举，未找到返回null
     */
    public static TrialStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (TrialStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 根据模具交替计划模板文本获取示方类型枚举，支持中文短标签和中越双语文本。
     *
     * @param text 模板中的示方类型文本
     * @return 匹配的示方类型枚举，空值或不支持的文本返回null
     */
    public static TrialStatusEnum getByMouldChangePlanExcelText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String normalizedText = text.trim();
        for (TrialStatusEnum statusEnum : values()) {
            if (statusEnum.description.equals(normalizedText)
                    || statusEnum.mouldChangePlanExcelText.equals(normalizedText)) {
                return statusEnum;
            }
        }
        return null;
    }
}
