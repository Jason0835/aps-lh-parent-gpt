package com.zlt.aps.factory.scheduling;

import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 续作规格续作模具排产辅助类--用来做值传递
 *
 * @author ZLT
 * @date 20250428
 */
@Getter
public class ContinueMouldProductionHelper {
    /**
     * 当前排产日
     */
    private Integer startProductionDate;
    /**
     * 当前排产计划
     */
    private MonthPlanManufacturingRequirementVo continuePlan;
    /**
     * 当前计划的单条硫化时间(包含间隔增加时间)
     */
    private BigDecimal singleCuringTime;
    /**
     * 当前还需排产量
     */
    private Long needProductionQty;

    public ContinueMouldProductionHelper(Integer startProductionDate, MonthPlanManufacturingRequirementVo continuePlan, BigDecimal singleCuringTime, Long needProductionQty) {
        this.startProductionDate = startProductionDate;
        this.continuePlan = continuePlan;
        this.singleCuringTime = singleCuringTime;
        this.needProductionQty = needProductionQty;
    }
}
