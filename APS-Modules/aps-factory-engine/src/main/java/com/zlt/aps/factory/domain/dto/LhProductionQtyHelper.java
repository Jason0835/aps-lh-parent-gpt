package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import lombok.Data;

import java.io.Serializable;

/**
 * 硫化排产量辅助对象
 * 用以值传递，没有其它特殊含义
 *
 * @author ZLT
 * @date 20251219
 */
@Data
public class LhProductionQtyHelper implements Serializable {
    /**
     * 成型机台
     */
    private CxMachineBaseInfoVo cxMachineInfo;
    /**
     * 成型机台的硫化组
     */
    private CxLhProductionHelper cxLhGroup;
    /**
     * 需要排产的总量
     */
    private Long sumProductionQty;
    /**
     * 实际排产总量
     */
    private Long realSumProductionQty;
    /**
     * 双模日硫化量
     */
    private Long dayMaxProductionQty;

    /**
     * 构建对象实例
     *
     * @param cxMachineInfo        成型机台
     * @param cxLhGroup            成型硫化组
     * @param sumProductionQty     需要排产的总量
     * @param realSumProductionQty 实际排产总量
     * @param dayMaxProductionQty  日双模最大硫化量
     */
    public LhProductionQtyHelper(CxMachineBaseInfoVo cxMachineInfo, CxLhProductionHelper cxLhGroup, Long sumProductionQty, Long realSumProductionQty, Long dayMaxProductionQty) {
        this.cxMachineInfo = cxMachineInfo;
        this.cxLhGroup = cxLhGroup;
        this.sumProductionQty = sumProductionQty;
        this.realSumProductionQty = realSumProductionQty;
        this.dayMaxProductionQty = dayMaxProductionQty;
    }
}
