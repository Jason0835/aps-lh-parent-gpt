package com.zlt.aps.factory.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 分组计划成型产能信息对象
 *
 * @author ZLT
 * @date 20251215
 */
@Data
public class ProductGroupCxCapacityInfo implements Serializable {

    /**
     * 分组名 TBR为结构
     */
    private String groupName;

    /**
     * 成型产能机台
     */
    private String cxMachineCode;

    /**
     * 最大胎胚种类数
     */
    private Integer maxEmbryoCodeCount;

    /**
     * 最后一天实际硫化配比
     */
    private Integer realMaxLhMachineCount;
    /**
     * 最大硫化机台数
     */
    private Integer maxLhMachineCount;

    /**
     * 最低硫化机台数
     */
    private Integer minLhMachineCount;
}
