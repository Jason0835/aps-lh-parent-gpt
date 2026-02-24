package com.zlt.aps.mp.engine.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 工厂结构硫化配比对象
 *
 * @author ZLT
 * @date 20251212
 */
@Data
public class MonthPlanStructureLhRatioVo implements Serializable {
    /**
     * 产品结构
     */
    private String structureName;
    /**
     * 成型机型
     */
    private String cxMachineTypeCode;
    /**
     * 硫化机台配比值
     */
    private Integer lhMachineMaxQty;
    /**
     * 最大胎胚数
     */
    private Integer maxEmbryoQty;
    /**
     * 硫化机台最低配比值
     */
    private Integer lhMachineMinQty;

}
