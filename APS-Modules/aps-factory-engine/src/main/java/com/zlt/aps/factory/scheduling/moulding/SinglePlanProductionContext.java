package com.zlt.aps.factory.scheduling.moulding;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.MouldInfoVO;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 单计划模具排产上下文
 *
 * @author ZLT
 * @date 20250311
 */
@Data
public class SinglePlanProductionContext extends Context {
    /**
     * 分组排产上下文
     */
    private GroupPlanProductionContext groupContext;
    /**
     * 排产计划
     */
    private MonthPlanManufacturingRequirementVo productionPlan;
    /**
     * 物料可用模具列表
     */
    private List<MouldInfoVO> enableMouldList;
    /**
     * 物料配置的模具及规格代号
     */
    private Set<String> enableMouldSet;
    /**
     * 延后排产的计划--同规格计划
     */
    private List<MonthPlanManufacturingRequirementVo> delayProductionPlanList;
}
