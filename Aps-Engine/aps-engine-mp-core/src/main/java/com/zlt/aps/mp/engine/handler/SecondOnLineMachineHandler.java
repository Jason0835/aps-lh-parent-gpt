package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.mp.engine.check.SkuSecondChecker;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 规格二次上机业务处理器
 *
 * @author ZLT
 * @date 20260328
 */
@Slf4j
public class SecondOnLineMachineHandler {

    /**
     * 检查二次上机
     * true 表示在startDay可以二次上机
     * false 表示不可在startDay二次上机
     *
     * @param productionPlanInfo 排产计划信息
     * @param productionContext  排产上下文
     * @param productionPlan     排产计划信息
     * @param startDay           上机日
     * @return true-允许二次上机，false-不允许二次上机
     */
    public static boolean checkSecondOnLine(ProductionPlanGroupInfo productionPlanInfo, TbrProductionContext productionContext,
                                            MonthPlanProductionRequirePlanVo productionPlan, Integer startDay) {
        List<Integer> dayList = productionPlanInfo.getProductionDaySetBySku(productionPlan.getMaterialDesc());
        if (CollectionUtils.isEmpty(dayList)) {
            return true;
        }
        Set<Integer> productionDaySet = dayList.stream().collect(Collectors.toSet());
        if (productionDaySet.contains(startDay)) {
            return true;
        }
        //降序,第一个元素最大
        dayList.sort(Comparator.reverseOrder());
        Integer lastCloseDay = dayList.get(0);
        int skuSecondProductionDays = productionContext.getBaseDataContainer().getParamConfiguration().getSkuSecondProduction();
        SkuSecondChecker skuSecondChecker = new SkuSecondChecker(startDay, lastCloseDay, skuSecondProductionDays);
        return skuSecondChecker.doCheck();
    }
}
