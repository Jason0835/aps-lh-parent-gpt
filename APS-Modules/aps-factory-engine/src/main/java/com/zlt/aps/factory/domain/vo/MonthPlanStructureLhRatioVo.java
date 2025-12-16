package com.zlt.aps.factory.domain.vo;

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
     * 硫化机台配比值
     */
    private Integer lhMachineMaxQty;
}
