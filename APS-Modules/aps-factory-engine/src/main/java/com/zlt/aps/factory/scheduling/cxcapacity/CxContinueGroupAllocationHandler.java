package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.deduct.DailyScheduleVo;
import com.zlt.aps.factory.deduct.DeductMouldScheduler;
import com.zlt.aps.factory.deduct.DeductMouldVo;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.factory.handler.ContinueSkuCalculator;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
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
        TbrProductionContext productionContext = (TbrProductionContext) context;
        BigDecimal needCount = groupPlanInfo.getNeedCxCapacityMachineCount();
        //续作Sku高优先级排产
        Set<String> productionCxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        BigDecimal productionCount = BigDecimal.valueOf(productionCxMachineCodeSet.size());
        Set<Integer> stopDays = context.getStopDays();
        Integer monthDays = context.getMonthDays();
        ProductionCapacityParamConfiguration paramConfiguration = productionContext.getBaseDataContainer().getParamConfiguration();
        //所需机台数 > 在产机台数，则表示还需增机台，则直接按满产算，如果=，则表示刚好，也直接按满产算
        if (needCount.compareTo(productionCount) >= BigDecimal.ZERO.intValue()) {
            return buildAllProductionCxMachineResult(context, groupPlanInfo, groupContinueInfo);
        }
        Map<Integer, CxLhProductionHelper> cxLhRatioMap = groupPlanInfo.getCxLhRatioMap();
        if (CollectionUtils.isEmpty(cxLhRatioMap)) {
            //todo 记录日志
            return Collections.emptyList();
        }
        Integer maxLhGroupNo = cxLhRatioMap.size();
        Set<Integer> assignedLhGroupNo = new HashSet<>(maxLhGroupNo);
        //todo 模拟在机结构的续作Sku高优先级部分，用以确定在产机台的分配信息
        Map<String, CxContinueSkuInfoHelper> continueSkuInfoMap = groupContinueInfo.getContinueSkuMouldNumberMap();
        if (CollectionUtils.isEmpty(continueSkuInfoMap)) {
            //todo 记录日志
            return Collections.emptyList();
        }
        //修正续作Sku的模具数
        continueSkuInfoMap.forEach((materialDesc, cxContinueSkuInfo) -> {
            List<MonthPlanProductMouldInfoVo> mouldList = productionContext.getBaseDataContainer().getSkuMouldRelationMap().get(materialDesc);
            if (CollectionUtils.isEmpty(mouldList)) {
                //todo 记录日志-sku没有模具
                return;
            }
            Integer mouldNumber = cxContinueSkuInfo.getMouldNumber();
            Integer maxNumber = mouldList.size();
            //超出可用模具
            if (mouldNumber > maxNumber) {
                cxContinueSkuInfo.setMouldNumber(maxNumber);
            }
        });
        //1.1、先使用续作Sku的高优先级部分进行模拟排产
        continueSkuInfoMap.forEach((materialDesc, cxContinueSkuInfo) -> {
            DeductMouldVo deductMould = DeductMouldVo.createDeductMouldBySku(monthDays, stopDays, new HashSet<>(), paramConfiguration, cxContinueSkuInfo);
            List<DailyScheduleVo> resultList = DeductMouldScheduler.scheduleProduction(deductMould);
            //分配结果
            if (CollectionUtils.isEmpty(resultList)) {
                //todo 记录日志
                return;
            }
            resultList.sort(Comparator.comparing(DailyScheduleVo::getScheduleDate));
            List<CxLhProductionHelper> allocationGroupList = ContinueSkuCalculator.continueSkuAllocationLhGroup(productionContext, groupPlanInfo, assignedLhGroupNo, cxContinueSkuInfo);
            if (CollectionUtils.isEmpty(allocationGroupList)) {
                //todo 记录日志
                return;
            }
            allocationGroupList.forEach(cxLhGroup -> {

            });
        });
        //1.2、接着进行同规格同花纹的续作高优先级部分进行模拟排产

        //1.3、接着进行共生胎，同模具的续作高优级部分进行模拟排产

        //1.4、最后用来确定在产机台各自收尾时间点及分配


        //2、在此基础上进行后续新增规格和续作其它优先级量的模具排产，确定各机台比较准确的收尾时间点，根据实单情况是否需要提前收尾


        groupPlanInfo.getTheoryDays();
        List<ProductGroupCxCapacityInfo> cxCapacityInfoList = groupContinueInfo.getCxCapacityInfoList();

        return null;
    }

    /**
     * @param context
     * @param groupPlanInfo
     * @param groupContinueInfo
     * @return
     */
    private static List<CxMachineAllocationPlanHelper> buildProductionCxMachineResult(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        BigDecimal needCount = groupPlanInfo.getNeedCxCapacityMachineCount();
        //最少需要的机台数
        Integer needMinCxMachineCount = needCount.setScale(0, RoundingMode.UP).intValue();
        //续作Sku高优先级排产
        Set<String> productionCxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        List<ProductGroupCxCapacityInfo> cxCapacityInfoList = groupContinueInfo.getCxCapacityInfoList();


        Integer productionCount = productionCxMachineCodeSet.size();


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
