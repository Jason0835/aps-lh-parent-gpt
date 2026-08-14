package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Sets;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitVo;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMouldDayProductionHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.enums.DeductionDayProductionTypeEnum;
import com.zlt.aps.mp.engine.logrecorder.DayLimitLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.DeductionDayProductionInfoLogRecorder;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分组计划减分配天数
 * 处理器
 *
 * @author ZLT
 * @date 20260328
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupPlanDeductionDayHandler {

    /**
     * groupPlanInfo在deductionDaySet内对cxMachineInfo进行产能释放
     *
     * @param context              排产上下文
     * @param deductionType        类型
     * @param cxMachineInfo        成型机台
     * @param groupPlanInfo        分组计划
     * @param allocationInfo       分配信息
     * @param dayProductionInfoMap 日排产信息
     * @param deductionDaySet      释放日信息
     */
    public void deductionDayInfo(Context context,
                                 DeductionDayProductionTypeEnum deductionType,
                                 CxMachineBaseInfoVo cxMachineInfo,
                                 ProductionPlanGroupInfo groupPlanInfo,
                                 CxMachineAllocationPlanHelper allocationInfo,
                                 Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionInfoMap,
                                 Set<Integer> deductionDaySet) {
        if (!isEffectiveParam(cxMachineInfo, groupPlanInfo, allocationInfo, dayProductionInfoMap, deductionDaySet)) {
            return;
        }
        String daysInfo = deductionDaySet.stream().map(String::valueOf).collect(Collectors.joining(StringConstant.COMMA));
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        String groupName = groupPlanInfo.getGroupName();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        DeductionDayProductionInfoLogRecorder.addStartGroupDeductionDayCapacityLog(productionContext, groupName, deductionType, cxMachineCode, daysInfo);
        Integer deductionDayCount = deductionDaySet.size();
        //不可以标记结构分配完成，需等下一轮分配：故而调整为更新分配的天数
        groupPlanInfo.deductionAllocationDays(deductionDayCount);
        //释放，成型工装使用量，切换结构使用量、分配日产能、特殊材料分配量
        cxMachineInfo.handlerBeforeConclusion(productionContext, true, allocationInfo, deductionDaySet, groupPlanInfo);
        //逐日释放：模具排产信息 模具产能占用、模壳、胶囊卡盘、换模次数等
        deductionMouldProductionInfo(productionContext, groupPlanInfo, dayProductionInfoMap, deductionDaySet);
    }

    /**
     * 扣除产能占用：成型工装占用量、分配日产能量、特殊材料分配量
     * 业务场景-在机分组(TBR-结构)与指定在排产时间上冲突时导致
     * 在机分配需要在中途进行强制下机
     *
     * @param context         排产上下文
     * @param cxMachineInfo   成型机台
     * @param groupPlanInfo   分组信息对象
     * @param allocationInfo  分配信息段
     * @param deductionDaySet 扣除天数信息
     */
    public void deductionDayInfoByContinueForceOffline(Context context,
                                                       CxMachineBaseInfoVo cxMachineInfo,
                                                       ProductionPlanGroupInfo groupPlanInfo,
                                                       CxMachineAllocationPlanHelper allocationInfo,
                                                       Set<Integer> deductionDaySet) {
        if (null == cxMachineInfo || null == groupPlanInfo || null == allocationInfo) {
            return;
        }
        if (CollectionUtils.isEmpty(deductionDaySet)) {
            return;
        }
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        String groupName = groupPlanInfo.getGroupName();
        if (StringUtils.isBlank(cxMachineCode) || StringUtils.isBlank(groupName)) {
            return;
        }
        if (!(cxMachineCode.equals(allocationInfo.getCxMachineCode()) || !groupName.equals(allocationInfo.getAllocationGroup()))) {
            return;
        }
        String daysInfo = deductionDaySet.stream().map(String::valueOf).collect(Collectors.joining(StringConstant.COMMA));
        TbrProductionContext productionContext = (TbrProductionContext) context;
        DeductionDayProductionInfoLogRecorder.addStartGroupDeductionDayCapacityLog(productionContext, groupName, DeductionDayProductionTypeEnum.APPOINT_FORCED, cxMachineCode, daysInfo);
        Integer deductionDayCount = deductionDaySet.size();
        //不可以标记结构分配完成，需等下一轮分配：故而调整为更新分配的天数
        groupPlanInfo.deductionAllocationDays(deductionDayCount);
        //释放，成型工装使用量、分配日产能、特殊材料分配量
        cxMachineInfo.handlerBeforeConclusion(productionContext, false, allocationInfo, deductionDaySet, groupPlanInfo);
    }

    /**
     * 在dayProductionInfoMap中扣除deductionDaySet模具排产信息
     * 同步释放模具产能占用、模壳、胶囊卡盘、换模次数
     *
     * @param context              排产上下文
     * @param deductionType        扣除类型
     * @param cxMachineInfo        成型机台
     * @param groupPlanInfo        分组计划
     * @param allocationInfo       分配信息
     * @param dayProductionInfoMap 日产限制集合
     * @param deductionDaySet      释放模具产能集合
     */
    public void deductionMouldDayInfo(Context context,
                                      DeductionDayProductionTypeEnum deductionType,
                                      CxMachineBaseInfoVo cxMachineInfo,
                                      ProductionPlanGroupInfo groupPlanInfo,
                                      CxMachineAllocationPlanHelper allocationInfo,
                                      Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionInfoMap,
                                      Set<Integer> deductionDaySet) {
        if (!isEffectiveParam(cxMachineInfo, groupPlanInfo, allocationInfo, dayProductionInfoMap, deductionDaySet)) {
            return;
        }
        String daysInfo = deductionDaySet.stream().map(String::valueOf).collect(Collectors.joining(StringConstant.COMMA));
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        String groupName = groupPlanInfo.getGroupName();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        DeductionDayProductionInfoLogRecorder.addStartGroupDeductionDayCapacityLog(productionContext, groupName, deductionType, cxMachineCode, daysInfo);
        //逐日释放：模具排产信息 模具产能占用、模壳、胶囊卡盘、换模次数等
        deductionMouldProductionInfo(productionContext, groupPlanInfo, dayProductionInfoMap, deductionDaySet);
    }

    /**
     * 对groupPlanInfo清除在singleDeductionDay日的排产信息
     *
     * @param productionContext  排产上下文
     * @param groupPlanInfo      分组计划
     * @param dayCapacityLimit   日产能控制信息对象
     * @param productionDayLimit 分组日排产对象
     * @param allMouldInfoMap    模具排产集合对象
     * @param singleDeductionDay 清除日
     */
    public void clearProductionInfoBySkuMoldDay(TbrProductionContext productionContext,
                                                ProductionPlanGroupInfo groupPlanInfo,
                                                String materialDesc,
                                                DayCapacityLimitVo dayCapacityLimit,
                                                GroupPlanCxLhCapacityLimitHelper productionDayLimit,
                                                Map<String, ProductionMouldInfoVo> allMouldInfoMap,
                                                Integer singleDeductionDay) {
        if (StringUtils.isBlank(materialDesc)) {
            return;
        }
        Map<String, SkuDayProductionInfoHelper> productionSkuQtyInfo = productionDayLimit.getProductionSkuQtyInfo();
        if (CollectionUtils.isEmpty(productionSkuQtyInfo)) {
            return;
        }
        //分组计划在singleDeductionDay排产materialDescSku信息
        SkuDayProductionInfoHelper skuDayProductionInfo = productionSkuQtyInfo.get(materialDesc);
        if (null == skuDayProductionInfo) {
            return;
        }
        clearSkuProductionInfoByDay(productionContext, groupPlanInfo, materialDesc, skuDayProductionInfo, dayCapacityLimit, productionDayLimit, allMouldInfoMap, singleDeductionDay);
    }

    /**
     * 校验参数是否有效
     * allocationInfo分配信息与成型机台、分组计划都得匹配上才有效
     *
     * @param cxMachineInfo        成型机台
     * @param groupPlanInfo        分组计划
     * @param allocationInfo       分配信息
     * @param dayProductionInfoMap 日排产信息
     * @param deductionDaySet      需要扣除的排产日信息
     * @return
     */
    private boolean isEffectiveParam(CxMachineBaseInfoVo cxMachineInfo,
                                     ProductionPlanGroupInfo groupPlanInfo,
                                     CxMachineAllocationPlanHelper allocationInfo,
                                     Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionInfoMap,
                                     Set<Integer> deductionDaySet) {
        if (null == cxMachineInfo || null == groupPlanInfo || null == allocationInfo) {
            return false;
        }
        if (CollectionUtils.isEmpty(dayProductionInfoMap) || CollectionUtils.isEmpty(deductionDaySet)) {
            return false;
        }
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        String groupName = groupPlanInfo.getGroupName();
        if (StringUtils.isBlank(cxMachineCode) || StringUtils.isBlank(groupName)) {
            return false;
        }
        if ((cxMachineCode.equals(allocationInfo.getCxMachineCode()) && groupName.equals(allocationInfo.getAllocationGroup()))) {
            return true;
        }
        return false;
    }

    /**
     * 释放模具排产信息
     *
     * @param productionContext    排产上下文
     * @param groupPlanInfo        分组计划
     * @param dayProductionInfoMap 日排产信息
     * @param deductionDaySet      需要释放的天数集合
     */
    private void deductionMouldProductionInfo(TbrProductionContext productionContext, ProductionPlanGroupInfo groupPlanInfo, Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionInfoMap, Set<Integer> deductionDaySet) {
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        Map<String, ProductionMouldInfoVo> allMouldInfoMap = baseDataContainer.getMouldInfoMap();
        //日产能限制信息
        DayCapacityLimitVo dayCapacityLimit = baseDataContainer.getDayCapacityLimit();
        //逐日释放排产信息：模具产能占用、模壳、胶囊卡盘、换模次数等
        deductionDaySet.forEach(singleDeductionDay -> {
            GroupPlanCxLhCapacityLimitHelper productionDayLimit = dayProductionInfoMap.get(singleDeductionDay);
            if (null == productionDayLimit) {
                return;
            }
            clearProductionInfoByDay(productionContext, groupPlanInfo, dayCapacityLimit, productionDayLimit, allMouldInfoMap, singleDeductionDay);
        });
    }

    /**
     * 对groupPlanInfo清除在singleDeductionDay日的排产信息
     *
     * @param productionContext  排产上下文
     * @param groupPlanInfo      分组计划
     * @param dayCapacityLimit   日产能控制信息对象
     * @param productionDayLimit 分组日排产对象
     * @param allMouldInfoMap    模具排产集合对象
     * @param singleDeductionDay 清除日
     */
    private void clearProductionInfoByDay(TbrProductionContext productionContext,
                                          ProductionPlanGroupInfo groupPlanInfo,
                                          DayCapacityLimitVo dayCapacityLimit,
                                          GroupPlanCxLhCapacityLimitHelper productionDayLimit,
                                          Map<String, ProductionMouldInfoVo> allMouldInfoMap,
                                          Integer singleDeductionDay) {
        Map<String, SkuDayProductionInfoHelper> productionSkuQtyInfo = productionDayLimit.getProductionSkuQtyInfo();
        if (CollectionUtils.isEmpty(productionSkuQtyInfo)) {
            return;
        }
        //分组计划在singleDeductionDay的排产Sku信息
        productionSkuQtyInfo.forEach((materialDesc, skuDayProductionInfo) -> clearSkuProductionInfoByDay(productionContext, groupPlanInfo, materialDesc, skuDayProductionInfo, dayCapacityLimit, productionDayLimit, allMouldInfoMap, singleDeductionDay));
    }

    /**
     * 清除Sku指定日的排产信息
     *
     * @param productionContext    排产上下文
     * @param groupPlanInfo        分组计划信息对象
     * @param materialDesc         清除的Sku
     * @param skuDayProductionInfo sku日排产信息
     * @param dayCapacityLimit     日产限制对象
     * @param productionDayLimit   单结构日排产限制对象
     * @param allMouldInfoMap      模具排产信息对象集合（指定清除的模具）
     * @param singleDeductionDay   清除日
     */
    private void clearSkuProductionInfoByDay(TbrProductionContext productionContext,
                                             ProductionPlanGroupInfo groupPlanInfo,
                                             String materialDesc,
                                             SkuDayProductionInfoHelper skuDayProductionInfo,
                                             DayCapacityLimitVo dayCapacityLimit,
                                             GroupPlanCxLhCapacityLimitHelper productionDayLimit,
                                             Map<String, ProductionMouldInfoVo> allMouldInfoMap,
                                             Integer singleDeductionDay) {
        if (StringUtils.isBlank(materialDesc) || null == skuDayProductionInfo || !materialDesc.equals(skuDayProductionInfo.getMaterialDesc())) {
            return;
        }
        //使用的模具信息
        Set<String> usedMouldSet = skuDayProductionInfo.getUsedMouldSet();
        if (CollectionUtils.isEmpty(usedMouldSet)) {
            return;
        }
        //需要清除的使用模具信息
        Set<String> clearUsedMouldSet = Sets.newHashSet();
        usedMouldSet.forEach(mouldCode -> {
            ProductionMouldInfoVo mouldInfo = allMouldInfoMap.get(mouldCode);
            if (null == mouldInfo) {
                return;
            }
            clearUsedMouldSet.add(mouldCode);
            List<CxMouldDayProductionHelper> dayProductionList = mouldInfo.getDayProductionInfo().get(singleDeductionDay);
            if (CollectionUtils.isEmpty(dayProductionList)) {
                return;
            }
            //20260119 释放，模具分配比例、模壳标准、胶囊卡盘、换模次数
            CxLhMouldProductionCalculator.handlerBeforeConclusion(productionContext, groupPlanInfo, singleDeductionDay, mouldInfo, materialDesc);
            mouldInfo.getFinishDaySet().remove(singleDeductionDay);
            //计划量释放还原
            List<CxMouldDayProductionHelper> releaseSkuList = dayProductionList.stream().filter(singlePlan -> materialDesc.equals(singlePlan.getMaterialDesc())).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(releaseSkuList)) {
                releaseSkuList.forEach(singleProduction -> {
                    DayLimitLogRecorder.addDeductionProductionQtyLog(productionContext, singleDeductionDay, singleProduction);
                    returnMonthPlanProductionQty(productionContext, materialDesc, singleProduction);
                });
            }
            List<CxMouldDayProductionHelper> otherSkuList = dayProductionList.stream().filter(singlePlan -> !materialDesc.equals(singlePlan.getMaterialDesc())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(otherSkuList)) {
                mouldInfo.getDayProductionInfo().remove(singleDeductionDay);
            } else {
                mouldInfo.getDayProductionInfo().put(singleDeductionDay, otherSkuList);
            }
        });
        //20260125 释放，日产能占用量
        if (null != dayCapacityLimit) {
            Integer sumProductionQty = skuDayProductionInfo.getSumProductionQty();
            dayCapacityLimit.deductionSkuDayProductionQty(productionContext, singleDeductionDay, materialDesc, clearUsedMouldSet, sumProductionQty, skuDayProductionInfo.getLossQty(), skuDayProductionInfo.getBrand());
            //20260211 特殊材料的消耗量释放(Sku已排产量对应释放)
            productionContext.updateSpecialMaterialInfoSkuAllocateQty(groupPlanInfo, -sumProductionQty);
        }
        //20260518+ 清除结构日排产使用模具信息
        productionDayLimit.clearAllUsedMouldInfo(clearUsedMouldSet, materialDesc);
    }

    /**
     * 按计划更新计划的排产量
     * 排产日信息清除，需回退已排产
     *
     * @param productionContext 排产上下文
     * @param materialDesc      排产物料
     * @param singleProduction  排产信息
     */
    private void returnMonthPlanProductionQty(TbrProductionContext productionContext, String materialDesc, CxMouldDayProductionHelper singleProduction) {
        if (null == singleProduction || StringUtils.isBlank(materialDesc)) {
            return;
        }
        Long monthPlanId = singleProduction.getMonthPlanId();
        if (null == monthPlanId) {
            return;
        }
        Map<Long, MonthPlanProductionRequirePlanVo> allProductionPlan = productionContext.getAllProductionPlan();
        if (CollectionUtils.isEmpty(allProductionPlan)) {
            return;
        }
        MonthPlanProductionRequirePlanVo requirePlan = allProductionPlan.get(monthPlanId);
        if (null == requirePlan) {
            return;
        }
        //排产量，先低优先级，再高优先级
        Integer productionQty = singleProduction.getProductionQty();
        requirePlan.withdrawProductionQty(productionQty);
    }
}
