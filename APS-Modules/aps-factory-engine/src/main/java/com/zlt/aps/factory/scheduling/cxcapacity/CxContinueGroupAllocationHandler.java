package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.dto.ProductGroupCxCapacityInfo;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 成型在机分组分配
 * TBR 在机结构成型机台产能分配
 * PCR 在机寸口成型机台产品分配
 *
 * @author ZLT
 * @date 20251227
 */
@Slf4j
public class CxContinueGroupAllocationHandler {

    /**
     * 对在机结构分配成型产能分配
     * 在机结构从现有的在产机台中，根据粗算所需机台，分配各在产机台的产能
     *
     * @param groupPlanInfo
     * @param groupContinueInfo
     */
    public static List<CxMachineAllocationPlanHelper> allocationContinueSkuMouldNumber(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        BigDecimal needCount = groupPlanInfo.getNeedCxCapacityMachineCount();
        //续作Sku高优先级排产
        Set<String> productionCxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        BigDecimal productionCount = BigDecimal.valueOf(productionCxMachineCodeSet.size());
        //所需机台数 > 在产机台数，则表示还需增机台，则直接按满产算，如果=，则表示刚好，也直接按满产算
        if (needCount.compareTo(productionCount) >= BigDecimal.ZERO.intValue()) {
            return buildAllProductionCxMachineResult(context, groupPlanInfo, groupContinueInfo);
        }
        //todo 模拟在机结构的续作Sku高优先级部分，用以确定在产机台的分配信息

        groupPlanInfo.getTheoryDays();
        List<ProductGroupCxCapacityInfo> cxCapacityInfoList = groupContinueInfo.getCxCapacityInfoList();

        return null;
    }

    /**
     *
     *
     * @param context
     * @param groupPlanInfo
     * @param groupContinueInfo
     * @return
     */
    private static List<CxMachineAllocationPlanHelper> buildProductionCxMachineResult(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo){
        BigDecimal needCount = groupPlanInfo.getNeedCxCapacityMachineCount();
        //续作Sku高优先级排产
        Set<String> productionCxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        BigDecimal productionCount = BigDecimal.valueOf(productionCxMachineCodeSet.size());


       return null;
    }
    /**
     * 构建所有在产机台分配给分组计划
     *
     * @param groupPlanInfo     分组计划信息
     * @param groupContinueInfo 分组计划对应的在产信息
     * @return
     */
    private static List<CxMachineAllocationPlanHelper> buildAllProductionCxMachineResult(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Set<String> productionCxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        Integer monthDays = context.getMonthDays();
        Map<String, CxMachineBaseInfoVo> allCxMachineMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        List<ProductGroupCxCapacityInfo> cxCapacityInfoList = groupContinueInfo.getCxCapacityInfoList();
        Map<String, ProductGroupCxCapacityInfo> groupCxCapacityInfoMap = cxCapacityInfoList.stream().collect(Collectors.toMap(ProductGroupCxCapacityInfo::getCxMachineCode, Function.identity()));
        List<CxMachineAllocationPlanHelper> allocationList = new ArrayList<>();
        productionCxMachineCodeSet.forEach(cxMachineCode -> {
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineMap.get(cxMachineCode);
            Integer allocationDays = cxMachineInfo.getMaxProductionDays();
            cxMachineInfo.setRemainingDays(BigDecimal.ZERO.intValue());
            ProductGroupCxCapacityInfo capacityInfo = groupCxCapacityInfoMap.get(cxMachineCode);
            CxMachineAllocationPlanHelper helper = new CxMachineAllocationPlanHelper(groupPlanInfo, capacityInfo.getMaxLhMachineCount(), groupContinueInfo.getContinueSkuMouldNumberMap(), allocationDays, BigDecimal.ONE.intValue(), monthDays);
            cxMachineInfo.addAllocationPlanInfo(helper);
            allocationList.add(helper);
        });
        return allocationList;
    }

}
