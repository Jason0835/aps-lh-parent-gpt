package com.zlt.aps.mp.engine.enums;

import lombok.Getter;

/**
 * 分组计划与成型机台选中类型枚举定义类
 * 01 胎胚种类数限制
 * 02 最大硫化机台数限制
 * 03 实单最低硫化机台数限制
 *
 * @author ZLT
 * @date 20260131
 */
@Getter
public enum GroupCxMachineSelectedTypeEnum {
    /**
     * 01 续作优先
     */
    CONTINUE_PRIORITY("01", "续作优先"),
    /**
     * 02 固定优先
     */
    FIXED_PRIORITY("02", "固定优先"),
    /**
     * 03 同规格优先
     */
    SAME_SPECIFICATIONS_PRIORITY("03", "同规格优先"),
    /**
     * 04 同英寸优先
     */
    SAME_PRO_SIZE_PRIORITY("04", "同英寸优先"),
    /**
     * 05 断面宽优先
     */
    SECTION_WIDTH_PRIORITY("05", "断面宽优先"),
    /**
     * 06 历史生产质量优先
     */
    HISTORY_QUALITY_PRIORITY("06", "历史生产质量优先"),

    /**
     * 07 含有结构优先
     */
    SAME_STRUCTURE_PRIORITY("07", "含有结构优先"),

    /**
     * 08 结构需求与成型产能接近的优先
     */
    NEAR_CAPACITY_PRIORITY("08", "结构需求与成型产能接近的优先");

    private String selectedType;

    private String selectedDesc;

    GroupCxMachineSelectedTypeEnum(String selectedType, String selectedDesc) {
        this.selectedType = selectedType;
        this.selectedDesc = selectedDesc;
    }
}
