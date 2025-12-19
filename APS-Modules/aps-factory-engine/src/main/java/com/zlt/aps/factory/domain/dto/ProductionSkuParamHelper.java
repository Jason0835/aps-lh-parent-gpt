package com.zlt.aps.factory.domain.dto;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * 排产规格参数辅助类，用以传参
 * 本身无业务含义
 *
 * @author ZLT
 * @date 20251219
 */
@Slf4j
@Getter
public class ProductionSkuParamHelper implements Serializable {
    /**
     * 排产开始日
     */
    private Integer startDay;
    /**
     * 排产结束日
     */
    private Integer endDay;
    /**
     * 成型机台
     */
    private String cxMachineCode;
    /**
     * 硫化分组编号
     */
    private Integer cxLhGroupNo;
    /**
     * 排产sku的物料描述
     */
    private String materialDesc;

    /**
     * 构造函数
     *
     * @param startDay      排产开始日
     * @param endDay        排产结束日
     * @param cxMachineCode 成型机台
     * @param cxLhGroupNo   硫化分组编号
     * @param materialDesc  排产sku的物料描述
     */
    public ProductionSkuParamHelper(Integer startDay, Integer endDay, String cxMachineCode, Integer cxLhGroupNo, String materialDesc) {
        this.startDay = startDay;
        this.endDay = endDay;
        this.cxMachineCode = cxMachineCode;
        this.cxLhGroupNo = cxLhGroupNo;
        this.materialDesc = materialDesc;
    }
}
