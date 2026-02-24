package com.zlt.aps.enums;

import com.zlt.aps.utils.ProductSpecificationsUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工装类别 不同寸口，成型法定义条件不一样
 *
 * @author ZLT
 * 20250917
 */
public enum WorkWearTypeEnum {
    /**
     * 0 通用
     */
    GENERAL("0", "通用"),
    /**
     * 1 大鼓--断面宽、扁平比
     */
    BIG_DRUM("1", "大鼓"),
    /**
     * 2 小鼓--断面宽、扁平比
     */
    SMALL_DRUM("2", "小鼓");
    /**
     * 18寸
     */
    public static final BigDecimal PRO_SIZE_18 = new BigDecimal("18.00");
    /**
     * 20寸
     */
    public static final BigDecimal PRO_SIZE_20 = new BigDecimal("20.00");
    /**
     * 18寸二次法，大鼓判断
     */
    private static final Integer[] bigDrumValues = {235, 60};

    private String typeValue;

    private String name;

    WorkWearTypeEnum(String typeValue, String name) {
        this.typeValue = typeValue;
        this.name = name;
    }

    /**
     * 根据寸口，成型法，规格，获取对应的工装类别
     *
     * @param proSize        寸口
     * @param mouldMethod    成型法
     * @param specifications 规格
     * @return
     */
    public static WorkWearTypeEnum getInstance(BigDecimal proSize, String mouldMethod, String specifications) {
        if (FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            return GENERAL;
        }
        if (!PRO_SIZE_18.equals(proSize)) {
            return GENERAL;
        }
        List<Integer> info = ProductSpecificationsUtils.parseSectionWidthAndAspectRatio(specifications);
        if (CollectionUtils.isEmpty(info)) {
            return GENERAL;
        }
        Integer sectionWidth = info.get(BigDecimal.ZERO.intValue());
        Integer aspectRatio = info.get(BigDecimal.ONE.intValue());
        if (sectionWidth >= bigDrumValues[0] && aspectRatio >= bigDrumValues[1]) {
            return BIG_DRUM;
        }
        return GENERAL;
    }

    public String getTypeValue() {
        return typeValue;
    }

    public String getName() {
        return name;
    }
}
