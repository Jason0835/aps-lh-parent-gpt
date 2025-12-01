package com.zlt.aps.monthplan.demand.controller;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ZLT
 * @date
 */
@Data
public class TempProductionDto implements Serializable {

    /**
     * 物料编码
     */
    private String productCode;

    /**
     * 已产数量
     */
    private Long productionQty;
}
