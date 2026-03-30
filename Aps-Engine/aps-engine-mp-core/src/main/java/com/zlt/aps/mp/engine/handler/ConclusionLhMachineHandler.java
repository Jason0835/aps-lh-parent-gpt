package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Lists;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.engine.capacity.MpMonthPlanDailyCapacityLimit;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.BeforeSkuProductionInfo;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.EarliestConclusionLhGroupHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import com.zlt.aps.mp.engine.domain.vo.ConclusionSkuInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 收尾硫化组处理器
 *
 * @author ZLT
 * @date 20260327
 */
@Slf4j
public class ConclusionLhMachineHandler {

    /**
     * 根据需要放入的Sku，获取与其可衔接的前Sku信息
     *
     * @param context       排产上下文
     * @param addSkuInfo    即将放入的Sku
     * @param groupInfo     分组计划
     * @param productionDay 排产日
     * @return
     */
    public static BeforeSkuProductionInfo findBeforeSkuProductionInfoByAddSku(Context context, MonthPlanProductionRequirePlanVo addSkuInfo, ProductionPlanGroupInfo groupInfo, Integer productionDay) {
        if (null == addSkuInfo || null == productionDay) {
            return BeforeSkuProductionInfo.buildEmpty(productionDay);
        }
        String mainPattern = addSkuInfo.getMainPattern();
        String materialDesc = addSkuInfo.getMaterialDesc();
        if (StringUtils.isBlank(materialDesc) || StringUtils.isBlank(mainPattern)) {
            return BeforeSkuProductionInfo.buildEmpty(productionDay);
        }
        List<SkuDayProductionInfoHelper> conclusionSkuInfo = getConclusionSkuInfo(context, groupInfo, productionDay);
        if (CollectionUtils.isEmpty(conclusionSkuInfo)) {
            return BeforeSkuProductionInfo.buildEmpty(productionDay);
        }
        Integer firstQty = getProductionDayQty(context, groupInfo, productionDay, mainPattern);
        TbrProductionContext productionContext = (TbrProductionContext) context;
        ProductionCapacityParamConfiguration paramConfiguration = productionContext.getBaseDataContainer().getParamConfiguration();
        Integer changMouldFirstQty = paramConfiguration.getChangeMouldFirstQty();
        boolean isChangeMould = changMouldFirstQty.equals(firstQty);
        //换活字块
        if (!isChangeMould) {
            return findBeforeSkuProductionInfoByChangeTypeBlock(context, conclusionSkuInfo, productionDay, materialDesc);
        }
        //换模
        return findBeforeSkuProductionInfoByChangeMould(context, conclusionSkuInfo, productionDay, materialDesc);
    }

    /**
     * 根据需要放入的Sku，获取与其换活字块的前Sku信息
     *
     * @param context       排产上下文
     * @param addSkuInfo    即将放入的Sku
     * @param groupInfo     分组计划
     * @param productionDay 排产日
     * @return
     */
    public static BeforeSkuProductionInfo findChangeTypeBlockBeforeSkuByAddSku(Context context, MonthPlanProductionRequirePlanVo addSkuInfo, ProductionPlanGroupInfo groupInfo, Integer productionDay) {
        if (null == addSkuInfo || null == productionDay) {
            return null;
        }
        String mainPattern = addSkuInfo.getMainPattern();
        String materialDesc = addSkuInfo.getMaterialDesc();
        if (StringUtils.isBlank(materialDesc) || StringUtils.isBlank(mainPattern)) {
            return null;
        }
        Integer firstQty = getProductionDayQty(context, groupInfo, productionDay, mainPattern);
        TbrProductionContext productionContext = (TbrProductionContext) context;
        ProductionCapacityParamConfiguration paramConfiguration = productionContext.getBaseDataContainer().getParamConfiguration();
        Integer changMouldFirstQty = paramConfiguration.getChangeMouldFirstQty();
        boolean isChangeMould = changMouldFirstQty.equals(firstQty);
        if (isChangeMould) {
            return null;
        }
        //换活字块
        List<SkuDayProductionInfoHelper> conclusionSkuInfo = getConclusionSkuInfo(context, groupInfo, productionDay);
        if (CollectionUtils.isEmpty(conclusionSkuInfo)) {
            return null;
        }
        return findBeforeSkuProductionInfoByChangeTypeBlock(context, conclusionSkuInfo, productionDay, materialDesc);
    }

    /**
     * 计划日硫化机台数、胎胚种类数、换模次数
     *
     * @param context
     */
    private static Integer getProductionDayQty(Context context, ProductionPlanGroupInfo groupInfo, Integer iDay, String mainPattern) {
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = groupInfo.getDailyCapacityLimitVoMap();
        if (dailyCapacityLimitVoMap.get(iDay) == null) {
            return null;
        }
        MpMonthPlanDailyCapacityLimit dailyCapacityLimitObj = new MpMonthPlanDailyCapacityLimit();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer endDay = productionContext.getMonthDays();
        //1. 转换模具排产结果
        List<FactoryMonthPlanMouldDayResult> mouldDayResultList = groupInfo.convertMouldDayResult(endDay);
        //2. 组装参数Map
        Map<String, Object> paramMap = groupInfo.composeDailyCapacityParamMap(productionContext);
        //3. 计算日产能
        Integer firstQty = dailyCapacityLimitObj.getFirstDayQty(mouldDayResultList, iDay, dailyCapacityLimitVoMap.get(iDay), paramMap, mainPattern);
        return firstQty;
    }

    /**
     * 查找在productionDay可进行换模的余量Sku信息
     *
     * @param context           排产上下文
     * @param conclusionSkuInfo 收尾余量Sku信息
     * @param productionDay     排产日
     * @param materialDesc      新增sku
     * @return
     */
    private static BeforeSkuProductionInfo findBeforeSkuProductionInfoByChangeMould(Context context, List<SkuDayProductionInfoHelper> conclusionSkuInfo, Integer productionDay, String materialDesc) {
        if (CollectionUtils.isEmpty(conclusionSkuInfo) || StringUtils.isBlank(materialDesc)) {
            return BeforeSkuProductionInfo.buildEmpty(productionDay);
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        List<SkuDayProductionInfoHelper> rejectShareMouldSkuList = new ArrayList<>();
        conclusionSkuInfo.forEach(single -> {
            boolean isShareMould = baseDataContainer.isShareMouldSameGroup(single.getMaterialDesc(), materialDesc);
            if (isShareMould) {
                return;
            }
            if (!single.isChangeMould()) {
                return;
            }
            rejectShareMouldSkuList.add(single);
        });
        if (CollectionUtils.isEmpty(rejectShareMouldSkuList)) {
            return BeforeSkuProductionInfo.buildEmpty(productionDay);
        }
        SkuDayProductionInfoHelper beforeSku = rejectShareMouldSkuList.get(BigDecimal.ZERO.intValue());
        return BeforeSkuProductionInfo.createBySku(beforeSku.getMaterialDesc(), beforeSku.getMaterialCode(), productionDay, beforeSku.getSumProductionQty(), beforeSku.getDayLhMachineQty(), beforeSku.getUsedMouldSet());
    }


    /**
     * 查找在productionDay可进行换活字块的余量Sku信息
     *
     * @param context           排产上下文
     * @param conclusionSkuInfo 收尾余量Sku信息
     * @param productionDay     排产日
     * @param materialDesc      新增sku
     * @return
     */
    private static BeforeSkuProductionInfo findBeforeSkuProductionInfoByChangeTypeBlock(Context context, List<SkuDayProductionInfoHelper> conclusionSkuInfo, Integer productionDay, String materialDesc) {
        if (CollectionUtils.isEmpty(conclusionSkuInfo) || StringUtils.isBlank(materialDesc)) {
            return BeforeSkuProductionInfo.buildEmpty(productionDay);
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        List<SkuDayProductionInfoHelper> rejectShareMouldSkuList = new ArrayList<>();
        conclusionSkuInfo.forEach(single -> {
            boolean isShareMould = baseDataContainer.isShareMouldSameGroup(single.getMaterialDesc(), materialDesc);
            if (!isShareMould) {
                return;
            }
            if (!single.isChangeTypeBlock(context)) {
                return;
            }
            rejectShareMouldSkuList.add(single);
        });
        if (CollectionUtils.isEmpty(rejectShareMouldSkuList)) {
            return BeforeSkuProductionInfo.buildEmpty(productionDay);
        }
        SkuDayProductionInfoHelper beforeSku = rejectShareMouldSkuList.get(BigDecimal.ZERO.intValue());
        return BeforeSkuProductionInfo.createBySku(beforeSku.getMaterialDesc(), beforeSku.getMaterialCode(), productionDay, beforeSku.getSumProductionQty(), beforeSku.getDayLhMachineQty(), beforeSku.getUsedMouldSet());
    }

    /**
     * 获取收尾余量Sku信息
     *
     * @param context       排产上下文
     * @param groupInfo     分组计划信息
     * @param productionDay 排产日
     * @return
     */
    private static List<SkuDayProductionInfoHelper> getConclusionSkuInfo(Context context, ProductionPlanGroupInfo groupInfo, Integer productionDay) {
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = groupInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return Collections.emptyList();
        }
        GroupPlanCxLhCapacityLimitHelper currentLimit = dayProductionLimitInfo.get(productionDay);
        if (null == currentLimit) {
            return Collections.emptyList();
        }
        Integer previousDay = groupInfo.getPreviousDay(productionDay);
        GroupPlanCxLhCapacityLimitHelper nextDayLimit = groupInfo.getNextDayInfo(currentLimit);
        GroupPlanCxLhCapacityLimitHelper previousDayLimit = dayProductionLimitInfo.get(previousDay);
        return currentLimit.getConclusionSkuInfo(context, productionDay, previousDayLimit, nextDayLimit);
    }

    /**
     * 按结构排产：续作Sku排产阶段
     * 同规格同花纹 共用模具
     * 获取结构下，最早续作收尾硫化组
     *
     * @param context            排产上下文
     * @param continueType       排产类型
     * @param productionPlanInfo 排产的分组
     * @param continueSkuMap     分组下，续作Sku信息
     * @return
     */
    public static EarliestConclusionLhGroupHelper getEarliestConclusionLhInfoByContinueSku(Context context, ContinueTypeEnum continueType, ProductionPlanGroupInfo productionPlanInfo, Map<String, CxContinueSkuInfoHelper> continueSkuMap) {
        if (null == productionPlanInfo || CollectionUtils.isEmpty(continueSkuMap)) {
            return null;
        }
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = productionPlanInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return null;
        }
        //得到续作最大硫化组可使用的模具数
        Integer sumMouldNumber = continueSkuMap.values().stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum();
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        List<GroupPlanCxLhCapacityLimitHelper> hasAddContinueSkuList = dayLimitList.stream().filter(dayLimit -> dayLimit.getProductionMouldSet().size() < sumMouldNumber).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasAddContinueSkuList)) {
            return null;
        }
        //按日期由小到大排序，找出最早的
        hasAddContinueSkuList.sort(Comparator.comparing(GroupPlanCxLhCapacityLimitHelper::getDay));
        GroupPlanCxLhCapacityLimitHelper selectedDayLimit = hasAddContinueSkuList.get(BigDecimal.ZERO.intValue());
        GroupPlanCxLhCapacityLimitHelper endDayLimit = hasAddContinueSkuList.get(hasAddContinueSkuList.size() - BigDecimal.ONE.intValue());
        Integer conclusionDay = selectedDayLimit.getDay();
        Integer endDay = endDayLimit.getDay();
        if (isGroupStartDayByFormalProduction(dayProductionLimitInfo, conclusionDay)) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        Integer previousDay = productionPlanInfo.getPreviousDay(conclusionDay);
        if (null == previousDay) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        GroupPlanCxLhCapacityLimitHelper previousLimit = dayProductionLimitInfo.get(previousDay);
        Integer canAddCount = previousLimit.getReleaseLhMachineCount(context, selectedDayLimit);
        if (canAddCount <= BigDecimal.ZERO.intValue()) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        SkuDayProductionInfoHelper previousSku = selectedDayLimit.getEarliestConclusionSkuInfo(context, previousLimit, canAddCount);
        if (null == previousSku) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        return EarliestConclusionLhGroupHelper.createEarliestConclusionLhGroup(conclusionDay, endDay, previousSku, true);
    }


    /**
     * 判断上机日是否为分组计划正式排产的最早排产日
     *
     * @param dayProductionLimitInfo 排产日信息
     * @param machineDay             当前排产日
     * @return
     */
    private static boolean isGroupStartDayByFormalProduction(Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo, Integer machineDay) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return true;
        }
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        dayLimitList.sort(Comparator.comparing(GroupPlanCxLhCapacityLimitHelper::getDay));
        return machineDay.equals(dayLimitList.get(BigDecimal.ZERO.intValue()).getDay());
    }

    /**
     * 构建首日收尾的续作Sku排产信息
     *
     * @param productionContext 排产上下文
     * @param firstLimit        首日排产信息对象
     * @param continueSkuMap    续作Sku信息
     * @param conclusionDay     最早收尾日
     * @param endDay            结束日-正常为分组结束日
     * @return
     */
    private static EarliestConclusionLhGroupHelper createFirstConclusionSkuInfo(TbrProductionContext productionContext, ProductionPlanGroupInfo productionPlanInfo, GroupPlanCxLhCapacityLimitHelper firstLimit, Map<String, CxContinueSkuInfoHelper> continueSkuMap, Integer conclusionDay, Integer endDay) {
        if (null == firstLimit || CollectionUtils.isEmpty(continueSkuMap)) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        List<ConclusionSkuInfo> conclusionSkuInfoList = new ArrayList<>();
        Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo = firstLimit.getSkuProductionDetailInfo();

        Set<String> productionSkuInfoSet = CollectionUtils.isEmpty(skuProductionDetailInfo) ? Collections.emptySet() : skuProductionDetailInfo.keySet();
        continueSkuMap.forEach((materialDesc, continueInfo) -> {
            Integer maxLhMachineCount = continueInfo.getMouldNumber() / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            List<SkuDayProductionInfoHelper> oneSelfList = skuProductionDetailInfo.get(materialDesc);
            if (CollectionUtils.isEmpty(oneSelfList)) {
                for (int index = BigDecimal.ONE.intValue(); index <= maxLhMachineCount; index++) {
                    conclusionSkuInfoList.add(ConclusionSkuInfo.createEmptyConclusionByContinueSku(continueInfo, conclusionDay));
                }
            }
            oneSelfList.forEach(single -> {
                if (single.isFullProduction(productionContext)) {
                    return;
                }
                conclusionSkuInfoList.add(ConclusionSkuInfo.createConclusionBySkuDayProductionInfo(single));
            });
        });
        Set<String> continueSkuInfoSet = continueSkuMap.keySet();

        firstLimit.getSkuProductionDetailInfo().keySet();
        return null;
    }

    /**
     * 获取与continueSku排产的硫化机台数
     *
     * @param productionContext       排产上下文
     * @param continueSku             续作Sku信息
     * @param skuProductionDetailInfo 排产信息
     * @param productionPlanInfo      分组计划
     * @return
     */
    private static Integer getUsedLhMachineNumber(TbrProductionContext productionContext, CxContinueSkuInfoHelper continueSku, Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo, ProductionPlanGroupInfo productionPlanInfo) {
        if (CollectionUtils.isEmpty(skuProductionDetailInfo)) {
            return BigDecimal.ZERO.intValue();
        }
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        List<Integer> usedLhMachineList = Lists.newArrayList();
        skuProductionDetailInfo.forEach((materialDesc, productionDetail) -> {
            if (materialDesc.equals(continueSku.getMaterialDesc())) {
                return;
            }
            MonthPlanProductionRequirePlanVo productionSkuInfo = productionPlanInfo.getGroupPlanData().stream()
                    .filter(single -> materialDesc.equals(single.getMaterialDesc())).findFirst().orElse(null);
            if (null == productionSkuInfo) {
                return;
            }
            //同规格同花纹
            if (productionSkuInfo.isSameSpecificationsAndPattern(continueSku)) {
                usedLhMachineList.add(productionDetail.size());
                return;
            }
            //共用模具
            if (baseDataContainer.isShareMouldSameGroup(materialDesc, continueSku.getMaterialDesc())) {
                usedLhMachineList.add(productionDetail.size());
            }
        });
        if (CollectionUtils.isEmpty(usedLhMachineList)) {
            return BigDecimal.ZERO.intValue();
        }
        return usedLhMachineList.stream().mapToInt(Integer::intValue).sum();
    }


}
