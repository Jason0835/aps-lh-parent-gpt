package com.zlt.aps.factory.domain.vo;

import lombok.Getter;

import java.io.Serializable;

/**
 * 模具信息
 *
 * @author ZLT
 * @date 20250219
 */
@Getter
public class PlanAssemblingMouldChangeInfoVo implements Serializable {
    /**
     * 拼模前的排产量
     */
    private Long beforeProductionQty;
    /**
     * 拼模前的原因
     */
    private String beforeNoProductionReason;
    /**
     * 拼模前的不排产数量
     */
    private Long beforeNoProductionQty;
    /**
     * 拼模前的备注
     */
    private String beforeRemark;

    /**
     * 构建拼模前的可能变更数据信息
     *
     * @param beforeProductionQty      拼模前的排产量
     * @param beforeNoProductionReason 拼模前的不排产原因
     * @param beforeNoProductionQty    拼模前的不可排数量
     * @param beforeRemark             拼模前的备注信息
     */
    public PlanAssemblingMouldChangeInfoVo(Long beforeProductionQty, String beforeNoProductionReason, Long beforeNoProductionQty, String beforeRemark) {
        this.beforeProductionQty = beforeProductionQty;
        this.beforeNoProductionReason = beforeNoProductionReason;
        this.beforeNoProductionQty = beforeNoProductionQty;
        this.beforeRemark = beforeRemark;
    }

}
