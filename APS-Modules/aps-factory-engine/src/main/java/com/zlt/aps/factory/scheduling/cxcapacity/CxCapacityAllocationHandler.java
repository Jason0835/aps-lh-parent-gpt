package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.dto.ProductGroupCxCapacityInfo;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 成型产能分配处理业务类--相当于工具类
 *
 * @author ZLT
 * @date 20251215
 */
@Slf4j
public class CxCapacityAllocationHandler {

    /**
     * 续作分组计划，采用续作成型产能进行分配
     * 先确认续作分组计划延续的续作成型机台
     * 1、如果续作分组计划需要的机台数减少，则成型机台对应硫化机台数多的优先下机，其次按编号大的优先下机
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 分组计划预估分配信息
     * @param cxContinueInfoMap            续作信息
     */
    public static void continueGroupPlanAllocation(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap, Map<String, CxContinueInfoHelper> cxContinueInfoMap) {
        //续作分组 --TBR按结构
        if (CollectionUtils.isEmpty(cxContinueInfoMap)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer monthDays = productionContext.getMonthDays();
        //成型基础信息
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfoMap = productionContext.getCxMachineBaseInfo();
        cxContinueInfoMap.forEach((structureName, cxContinueInfo) -> {
            //预估成型机台的计划分组信息
            ProductionPlanGroupInfo groupPlanInfo = estimateGroupCxAllocationMap.get(structureName);
            //续作结构，没有需求
            if (null == groupPlanInfo) {
                return;
            }
            Map<String, Integer> continueSkuMap = cxContinueInfo.getCxMachineGroup().get(structureName);
            //实际需要的机台数
            BigDecimal machineCount = groupPlanInfo.getNeedCxCapacityMachineCount();
            //整数机台
            BigDecimal integerPart = machineCount.setScale(0, RoundingMode.DOWN);
            //向上取整，看续作机台是否需要空出机台
            Integer wholeMachineCount = machineCount.setScale(0, RoundingMode.UP).intValue();
            List<ProductGroupCxCapacityInfo> cxCapacityInfoList = cxContinueInfo.getCxCapacityInfoList();
            //按对应的硫化机台数少优先，成型机编号小的优先排序
            cxCapacityInfoList.sort(Comparator.comparing(ProductGroupCxCapacityInfo::getRealMaxLhMachineCount, Comparator.reverseOrder()).thenComparing(ProductGroupCxCapacityInfo::getCxMachineCode, Comparator.reverseOrder()));
            //先分整台
            Integer wholeMachine = integerPart.intValue();
            for (int allocationIndex = BigDecimal.ZERO.intValue(); allocationIndex < wholeMachine; allocationIndex++) {
                ProductGroupCxCapacityInfo cxCapacityInfo = cxCapacityInfoList.get(allocationIndex);
                CxMachineBaseInfoVo cxMachineBaseInfo = cxMachineBaseInfoMap.get(cxCapacityInfo.getCxMachineCode());
                Integer allocationDay = cxMachineBaseInfo.getMaxProductionDays();
                cxMachineBaseInfo.setRemainingDays(BigDecimal.ZERO.intValue());
                CxMachineAllocationPlanHelper helper = createAllocationPlanHelper(cxMachineBaseInfo, groupPlanInfo, continueSkuMap, allocationDay, BigDecimal.ONE.intValue(), monthDays);
                cxMachineBaseInfo.addAllocationPlanInfo(helper);
            }
            //不是整台部分
            ProductGroupCxCapacityInfo cxCapacityInfo = cxCapacityInfoList.get(wholeMachineCount - BigDecimal.ONE.intValue());
            CxMachineBaseInfoVo cxMachineBaseInfo = cxMachineBaseInfoMap.get(cxCapacityInfo.getCxMachineCode());
            BigDecimal decimalPart = machineCount.subtract(integerPart);
            Integer allocationDay = decimalPart.multiply(BigDecimal.valueOf(context.getMaxProductionDays())).setScale(0, RoundingMode.UP).intValue();
            CxMachineAllocationPlanHelper helper = createAllocationPlanHelper(cxMachineBaseInfo, groupPlanInfo, continueSkuMap, allocationDay, BigDecimal.ONE.intValue(), monthDays);
            cxMachineBaseInfo.addAllocationPlanInfo(helper);
            cxMachineBaseInfo.setRemainingDays(cxMachineBaseInfo.getRemainingDays() - allocationDay);
        });
    }

    /**
     * 对成型机台创建分配集合对象
     *
     * @param cxMachineBaseInfo 成型机台信息
     * @param groupPlanInfo     分配的分组计划
     * @param continueSkuMap    续作规格信息
     * @param allocationDay     分配天数
     * @param startDay          起始天数
     * @param monthDays         月份最大天数
     * @return
     */
    private static CxMachineAllocationPlanHelper createAllocationPlanHelper(CxMachineBaseInfoVo cxMachineBaseInfo, ProductionPlanGroupInfo groupPlanInfo, Map<String, Integer> continueSkuMap, Integer allocationDay, Integer startDay, Integer monthDays) {
        Integer startAllocationDay = BigDecimal.ZERO.intValue();
        Integer endAllocationDay = BigDecimal.ZERO.intValue();
        Set<Integer> stopDayInfo = cxMachineBaseInfo.getStopDayInfo();
        if (null == stopDayInfo) {
            stopDayInfo = new HashSet<>();
        }
        //分配的天数
        for (int index = BigDecimal.ZERO.intValue(); index <= allocationDay; ) {
            Integer day = startDay + index;
            //停产日
            if (stopDayInfo.contains(day)) {
                continue;
            }
            //超出月份周期
            if (day > monthDays) {
                break;
            }
            index = index + BigDecimal.ONE.intValue();
            if (startAllocationDay < day) {
                startAllocationDay = day;
            }
            if (day > endAllocationDay) {
                endAllocationDay = day;
            }
        }
        if (null == continueSkuMap) {
            continueSkuMap = new HashMap<>();
        }
        return new CxMachineAllocationPlanHelper(groupPlanInfo, continueSkuMap, allocationDay, startAllocationDay, endAllocationDay);
    }

    /**
     * 获取需要月初就需要释放的续作成型机台
     * 如果计划机台数超出或是等于在机机台数，则无需月初释放
     * 如果计划机台数小于在机机台数，则按先最大硫化机台数的先释放
     *
     * @param wholeMachineCount    计划需要完整机台数
     * @param continueMachineCount 在机机台数
     * @param cxCapacityInfoList   在机机台数信息数据
     * @return
     */
    private static Set<String> getNeedReleaseMachineInfo(Integer wholeMachineCount, Integer continueMachineCount, List<ProductGroupCxCapacityInfo> cxCapacityInfoList) {
        if (wholeMachineCount >= continueMachineCount) {
            return Collections.emptySet();
        }
        Set<String> needReleaseMachineSet = new HashSet<>();
        //需要释放机台的数量
        int needCount = continueMachineCount - wholeMachineCount;
        //按对应的硫化机台数少，成型机编号小的排序
        cxCapacityInfoList.sort(Comparator.comparing(ProductGroupCxCapacityInfo::getRealMaxLhMachineCount).thenComparing(ProductGroupCxCapacityInfo::getCxMachineCode));
        for (ProductGroupCxCapacityInfo canReleaseMachine : cxCapacityInfoList) {
            //已经达到释放量
            if (needCount <= BigDecimal.ZERO.intValue()) {
                break;
            }
            needReleaseMachineSet.add(canReleaseMachine.getCxMachineCode());
            needCount = needCount - 1;
        }
        return needReleaseMachineSet;
    }

}
