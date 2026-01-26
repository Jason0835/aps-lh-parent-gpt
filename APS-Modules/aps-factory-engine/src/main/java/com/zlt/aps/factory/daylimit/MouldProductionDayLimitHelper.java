package com.zlt.aps.factory.daylimit;

import lombok.Getter;

import java.io.Serializable;
import java.util.Set;

/**
 * 选中模具排产日限制结果信息
 *
 * @author ZLT
 * @date 20260121
 */
@Getter
public class MouldProductionDayLimitHelper implements Serializable {
    /**
     * 可排产集合
     */
    private Set<Integer> productionDaySet;
    /**
     * 限制类型
     */
    private MouldProductionLimitTypeEnum limitType;

    public MouldProductionDayLimitHelper(Set<Integer> productionDaySet, MouldProductionLimitTypeEnum limitType) {
        this.productionDaySet = productionDaySet;
        this.limitType = limitType;
    }
}
