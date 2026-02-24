package com.zlt.aps.mp.engine.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * 模具关系类型枚举类
 * 01-sku与模具关系
 * 02-新模具到货计划
 *
 * @author ZLT
 * @date 20251218
 */
@Getter
public enum MouldRelationTypeEnum {
    /**
     * 01 sku与模具关系
     */
    SKU_RELATION_CONFIGURATION("01", "sku与模具关系"),
    /**
     * 02 新模具到货计划
     */
    MOULD_DELIVERY_PLAN("02", "新模具到货计划");

    private String relationType;

    private String desc;

    MouldRelationTypeEnum(String relationType, String desc) {
        this.relationType = relationType;
        this.desc = desc;
    }

    /**
     * 默认为sku与模具关系
     *
     * @param relationType 关系类型
     * @return
     */
    public static MouldRelationTypeEnum getInstance(String relationType) {
        if (StringUtils.isBlank(relationType)) {
            return SKU_RELATION_CONFIGURATION;
        }
        return Arrays.stream(values()).filter(type -> type.getRelationType().equalsIgnoreCase(relationType)).findFirst().orElse(SKU_RELATION_CONFIGURATION);
    }
}
