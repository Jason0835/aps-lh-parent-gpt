package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * 清除排产信息数据
 *
 * @author ZLT
 * @date 20260127
 */
@Slf4j
public class ClearProductionInfoHandler {

    /**
     * 清除排产数据-重置排产数据
     * 场景：
     * 1、模拟排产前，清除在机结构在产机台对续作的排产测试在产机台的收尾点
     * 2、正式排产前，清除模拟排产信息
     *
     * @param context 排产上下文
     */
    public void clearProductionData(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //物料已排产量及损耗量清空
        productionContext.resetSkuProductionAndWastageQty();
        //处理计划的待排产量及排产标记重置
        Map<Long, MonthPlanProductionRequirePlanVo> allSinglePlanMap = productionContext.getAllProductionPlan();
        if (!CollectionUtils.isEmpty(allSinglePlanMap)) {
            allSinglePlanMap.forEach((monthPlanId, singlePlan) -> singlePlan.resetProductionDataInfo());
        }
        //重新构建模具排产信息，全部清空
        Map<String, ProductionMouldInfoVo> allMouldInfoMap = productionContext.getBaseDataContainer().getMouldInfoMap();
        if (!CollectionUtils.isEmpty(allMouldInfoMap)) {
            allMouldInfoMap.forEach((mouldCode, singleMouldInfo) -> {
                singleMouldInfo.setFinishDaySet(new HashSet<>());
                singleMouldInfo.setDayProductionInfo(new HashMap<>());
            });
        }
        //清除模壳使用量
        productionContext.clearAllMouldShellUsed();
        //清除模具分配使用量
        productionContext.clearAllMouldAllocationUsed();
        //清除胶囊卡盘使用量
        productionContext.clearAllCapsuleChuckUsed();
        //清除日排产限制使用量
        productionContext.clearAllDayLimitUsed();
    }
}
