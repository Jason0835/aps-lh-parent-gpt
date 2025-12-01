package com.zlt.aps.factory.scheduling.moulding;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.ProductionPlanGroupVo;
import com.zlt.aps.factory.scheduling.ProductionContext;
import lombok.Data;

/**
 * 分组排产上下文-模具排产
 *
 * @author ZLT
 * @date 20250311
 */
@Data
public class GroupPlanProductionContext extends Context {
    /**
     * 排产上下文对象
     */
    private ProductionContext productionContext;
    /**
     * 分组排产计划信息
     */
    private ProductionPlanGroupVo productionPlanGroup;
}
