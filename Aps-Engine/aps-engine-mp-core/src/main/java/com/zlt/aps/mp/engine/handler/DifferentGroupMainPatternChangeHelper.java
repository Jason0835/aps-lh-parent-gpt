package com.zlt.aps.mp.engine.handler;

import lombok.Getter;

import java.io.Serializable;

/**
 * 同主花纹的不同分组，分配模具数针对续作使用模具的变化对象
 * changeNumber 为正表示增模，为负表示要减模
 *
 * @author ZLT
 * @date 20260420
 */
@Getter
public class DifferentGroupMainPatternChangeHelper implements Serializable {
    /**
     * 主花纹
     */
    private String mainPattern;
    /**
     * 分组名 TBR 结构
     */
    private String groupName;
    /**
     * 模具分配比例数
     */
    private Integer allocationNumber;
    /**
     * 续作模具使用数
     */
    private Integer continueUsedNumber;

    /**
     * 构造函数
     *
     * @param mainPattern        主花纹
     * @param groupName          分组名 TBR 结构
     * @param allocationNumber   模具分配比例数
     * @param continueUsedNumber 续作Sku模具使用数
     */
    public DifferentGroupMainPatternChangeHelper(String mainPattern, String groupName, Integer allocationNumber, Integer continueUsedNumber) {
        this.mainPattern = mainPattern;
        this.groupName = groupName;
        this.allocationNumber = allocationNumber;
        this.continueUsedNumber = continueUsedNumber;
    }

    /**
     * 变化数 = 分配数 - 续作使用数
     */
    public Integer getChangeNumber() {
        return allocationNumber - continueUsedNumber;
    }
}
