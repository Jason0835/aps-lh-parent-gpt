package com.zlt.aps.factory.scheduling.cxcapacity;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

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
    @Deprecated
    public static Map<String, CxMachineAllocationPlanHelper> continueGroupPlanAllocation(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap, Map<String, CxContinueInfoHelper> cxContinueInfoMap) {
        //续作分组 --TBR按结构
        if (CollectionUtils.isEmpty(cxContinueInfoMap)) {
            return Collections.emptyMap();
        }
        Map<String, CxMachineAllocationPlanHelper> continueAllocationMap = new HashMap<>();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer monthDays = productionContext.getMonthDays();
        //成型基础信息
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfoMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        cxContinueInfoMap.forEach((structureName, cxContinueInfo) -> {
            //预估成型机台的计划分组信息
            ProductionPlanGroupInfo groupPlanInfo = estimateGroupCxAllocationMap.get(structureName);
            //续作结构，没有需求
            if (null == groupPlanInfo) {
                return;
            }
            Integer minLhRatio = groupPlanInfo.getMinLhMachineCount();
            //实际需要的机台数
            BigDecimal machineCount = groupPlanInfo.getNeedCxCapacityMachineCount();
            //整数机台
            BigDecimal integerPart = machineCount.setScale(0, RoundingMode.DOWN);
            //向上取整，看续作机台是否需要空出机台
            Integer wholeMachineCount = machineCount.setScale(0, RoundingMode.UP).intValue();
            //todo 计算在机结构续作SKU的使用硫化机台数--在机结构机台数减量时，需要空出对应配比的硫化模具数
            CxContinueSkuAllocationMouldHandler.allocationContinueSkuMouldNumber(groupPlanInfo, cxContinueInfo, wholeMachineCount);
            List<ProductGroupCxCapacityInfo> cxCapacityInfoList = cxContinueInfo.getCxCapacityInfoList();
            //按对应的硫化机台数少优先，成型机编号小的优先排序
            cxCapacityInfoList.sort(Comparator.comparing(ProductGroupCxCapacityInfo::getMaxLhMachineCount, Comparator.reverseOrder()).thenComparing(ProductGroupCxCapacityInfo::getCxMachineCode, Comparator.reverseOrder()));
            //先分整台
            Integer wholeMachine = integerPart.intValue();
            for (int allocationIndex = BigDecimal.ZERO.intValue(); allocationIndex < wholeMachine; allocationIndex++) {
                ProductGroupCxCapacityInfo cxCapacityInfo = cxCapacityInfoList.get(allocationIndex);
                CxMachineBaseInfoVo cxMachineBaseInfo = cxMachineBaseInfoMap.get(cxCapacityInfo.getCxMachineCode());
                //续作Sku信息
                Map<String, CxContinueSkuInfoHelper> continueSkuMap = cxContinueInfo.getContinueSkuMouldNumberMap();
//                        cxContinueInfo.getCxMachineGroup().get(cxMachineBaseInfo.getCxMachineCode());
                Integer allocationDay = cxMachineBaseInfo.getMaxProductionDays();
                cxMachineBaseInfo.setRemainingDays(BigDecimal.ZERO.intValue());
                CxMachineAllocationPlanHelper helper = createAllocationPlanHelper(cxMachineBaseInfo, minLhRatio, groupPlanInfo, continueSkuMap, allocationDay, BigDecimal.ONE.intValue(), monthDays);
                cxMachineBaseInfo.addAllocationPlanInfo(helper);
                continueAllocationMap.put(cxCapacityInfo.getCxMachineCode(), helper);
            }
            //不是整台部分
            ProductGroupCxCapacityInfo cxCapacityInfo = cxCapacityInfoList.get(wholeMachineCount - BigDecimal.ONE.intValue());
            CxMachineBaseInfoVo cxMachineBaseInfo = cxMachineBaseInfoMap.get(cxCapacityInfo.getCxMachineCode());
            //续作Sku信息
            Map<String, CxContinueSkuInfoHelper> continueSkuMap = cxContinueInfo.getContinueSkuMouldNumberMap();
//            cxContinueInfo.getCxMachineGroup().get(cxMachineBaseInfo.getCxMachineCode());
            //小数部分的天数
            BigDecimal decimalPart = machineCount.subtract(integerPart);
            Integer allocationDay = decimalPart.multiply(BigDecimal.valueOf(context.getMaxProductionDays())).setScale(0, RoundingMode.UP).intValue();
            CxMachineAllocationPlanHelper helper = createAllocationPlanHelper(cxMachineBaseInfo, minLhRatio, groupPlanInfo, continueSkuMap, allocationDay, BigDecimal.ONE.intValue(), monthDays);
            cxMachineBaseInfo.addAllocationPlanInfo(helper);
            continueAllocationMap.put(cxMachineBaseInfo.getCxMachineCode(), helper);
            Integer newRemainingDays = cxMachineBaseInfo.getRemainingDays() - allocationDay;
            cxMachineBaseInfo.setRemainingDays(newRemainingDays);
            //加入收尾匹配
            if (newRemainingDays > BigDecimal.ZERO.intValue()) {
                productionContext.addReverseMachine(cxMachineBaseInfo.getCxMachineCode());
            }
        });
        return continueAllocationMap;
    }

    /**
     * 对成型机台创建分配集合对象-按最小硫化配比分配
     *
     * @param cxMachineBaseInfo 成型机台信息
     * @param maxLhRatio        最高硫化配比
     * @param groupPlanInfo     分配的分组计划
     * @param continueSkuMap    续作规格信息
     * @param allocationDay     分配天数
     * @param startDay          起始天数
     * @param monthDays         月份最大天数
     * @return
     */
    public static CxMachineAllocationPlanHelper createAllocationPlanHelper(CxMachineBaseInfoVo cxMachineBaseInfo, Integer maxLhRatio, ProductionPlanGroupInfo groupPlanInfo, Map<String, CxContinueSkuInfoHelper> continueSkuMap, Integer allocationDay, Integer startDay, Integer monthDays) {
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
        return new CxMachineAllocationPlanHelper(cxMachineBaseInfo.getCxMachineCode(), groupPlanInfo, maxLhRatio, continueSkuMap, allocationDay, startAllocationDay, endAllocationDay);
    }

    /**
     * 对结构收尾的成型机台反向挑选合适的结构上机
     * 收尾成型机台的剩余产能能覆盖挑选的结构剩余排产净需求
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 分组结构需求
     */
    public static void reverseMachineAllocation(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap) {
        if (CollectionUtils.isEmpty(estimateGroupCxAllocationMap)) {
            //todo 记录日志
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //获取收尾机台信息
        Set<String> reverseFindSet = productionContext.getReverseFindSet();
        if (CollectionUtils.isEmpty(reverseFindSet)) {
            //todo 记录日志
            return;
        }
        List<CxMachineBaseInfoVo> reverseCxMachineList = new ArrayList<>();
        reverseFindSet.forEach(cxMachineCode -> reverseCxMachineList.add(productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode)));
        //最先收尾的先-剩余天数多的
        if (CollectionUtils.isEmpty(reverseCxMachineList)) {
            //todo 记录日志
            return;
        }
        //收尾机台
        List<CxMachineBaseInfoVo> endingCxMachineList = reverseCxMachineList.stream().filter(cxMachineInfo -> !CollectionUtils.isEmpty(cxMachineInfo.getAllocationList())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(endingCxMachineList)) {
            //todo 记录日志
            return;
        }
        endingCxMachineList.sort(Comparator.comparing(CxMachineBaseInfoVo::getRemainingDays, Comparator.reverseOrder()).thenComparing(CxMachineBaseInfoVo::getCxMachineCode));
        //一台一台反向挑选合适的结构分组计划
        endingCxMachineList.forEach(reverseCxMachineInfo -> selectedGroupPlanByCxMachine(productionContext, estimateGroupCxAllocationMap, reverseCxMachineInfo));
    }

    /**
     * 成型产能机台反向挑选合适的结构
     * 剩余产能要能覆盖计划排产净需求
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 分组计划
     * @param cxMachineInfo                成型产能信息
     */
    public static void selectedGroupPlanByCxMachine(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap, CxMachineBaseInfoVo cxMachineInfo) {
        if (null == cxMachineInfo || CollectionUtils.isEmpty(estimateGroupCxAllocationMap)) {
            //todo 记录日志
            return;
        }
        List<CxMachineAllocationPlanHelper> allocationList = cxMachineInfo.getAllocationList();
        if (CollectionUtils.isEmpty(allocationList)) {
            //todo 记录日志 空机台不是收尾
            return;
        }
        Integer remainingDays = cxMachineInfo.getRemainingDays();
        if (remainingDays <= BigDecimal.ZERO.intValue()) {
            //todo 记录日志
            return;
        }
        //成型剩余产能能覆盖结构剩余排产净需求量
        Map<String, ProductionPlanGroupInfo> capacityCoverageMap = getProductionCapacityCoverage(estimateGroupCxAllocationMap, cxMachineInfo);
        if (CollectionUtils.isEmpty(capacityCoverageMap)) {
            //todo 记录日志
            return;
        }
        //剔除不可匹配的结构信息（不可作业的结构或是SKU需要剔除,零度供料架）
        Map<String, ProductionPlanGroupInfo> enableGroupPlanMap = excludeDisable(capacityCoverageMap, cxMachineInfo);
        if (CollectionUtils.isEmpty(enableGroupPlanMap)) {
            //todo 记录日志
            return;
        }
        //获取合适优先级的一个结构
        ProductionPlanGroupInfo allocationGroupPlan = selectedOne(enableGroupPlanMap, cxMachineInfo);
        if (null == allocationGroupPlan) {
            //todo 记录日志
            return;
        }
        Integer lhRatio = cxMachineInfo.getRatio();
        //判断成型鼓是否符合条件
        Integer needAllocationDays = allocationGroupPlan.calculateNeedDays(lhRatio);
        //更新剩余时间
        Integer leftOver = remainingDays - needAllocationDays;
        cxMachineInfo.setRemainingDays(leftOver);
        CxMachineAllocationPlanHelper lastHelper = allocationList.get(allocationList.size() - BigDecimal.ONE.intValue());
        Integer startDay = lastHelper.getEndDay() + BigDecimal.ONE.intValue();
        CxMachineAllocationPlanHelper addHelper = createAllocationPlanHelper(cxMachineInfo, lhRatio, allocationGroupPlan, null, needAllocationDays, startDay, context.getMonthDays());
        cxMachineInfo.addAllocationPlanInfo(addHelper);
        //todo 对成型机台进行模拟模具排产
        CxMouldProductionHandler.noContinueGroupPlanMouldProduction(context, cxMachineInfo.getCxMachineCode(), addHelper);
        //还有剩余产能，继续挑选下一个分组结构
        if (leftOver >= 5) {
            selectedGroupPlanByCxMachine(context, estimateGroupCxAllocationMap, cxMachineInfo);
        }
    }

    /**
     * 获取新增分组计划上机 --新增结构
     * 1、高优先级SKU个数多的优先
     * 2、2副模具共用受限，则结构总净需求小的优先
     * 3、特殊种类SKU个数多的优先
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 分组计划集合
     * @return
     */
    public static ProductionPlanGroupInfo getInsertNewGroupPlan(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap) {
        if (CollectionUtils.isEmpty(estimateGroupCxAllocationMap)) {
            return null;
        }
        List<ProductionPlanGroupInfo> allGroupPlanList = new ArrayList<>(estimateGroupCxAllocationMap.values());
        if (CollectionUtils.isEmpty(allGroupPlanList)) {
            return null;
        }
        List<ProductionPlanGroupInfo> needProductionGroupList = allGroupPlanList.stream().filter(groupPlan -> groupPlan.getRemainingProductionQty() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(needProductionGroupList)) {
            return null;
        }
        //高优先级需求SKU个数多的优先
        Integer maxHeightPriority = needProductionGroupList.stream().mapToInt(ProductionPlanGroupInfo::getHeightPriorityCount).max().getAsInt();
        List<ProductionPlanGroupInfo> heightList = needProductionGroupList.stream().filter(groupPlan -> maxHeightPriority.equals(groupPlan.getHeightPriorityCount())).collect(Collectors.toList());
        if (heightList.size() == BigDecimal.ONE.intValue()) {
            return heightList.get(BigDecimal.ZERO.intValue());
        }
        //todo 共用模具受限

        Integer maxSpecialMaterial = heightList.stream().mapToInt(ProductionPlanGroupInfo::getSpecialMaterialsCount).max().getAsInt();
        List<ProductionPlanGroupInfo> specialMaterialList = heightList.stream().filter(groupPlan -> maxSpecialMaterial.equals(groupPlan.getSpecialMaterialsCount())).collect(Collectors.toList());
        if (specialMaterialList.size() == BigDecimal.ONE.intValue()) {
            return specialMaterialList.get(BigDecimal.ZERO.intValue());
        }
        specialMaterialList.sort(Comparator.comparing(ProductionPlanGroupInfo::getRemainingProductionQty));
        return specialMaterialList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 对分组(结构)计划，挑选合适的成型机台
     *
     * @param context         排产上下文
     * @param addNewGroupPlan 排产分组计划
     * @return
     */
    public static CxMachineBaseInfoVo selectedCxMachine(Context context, ProductionPlanGroupInfo addNewGroupPlan) {
        if (null == addNewGroupPlan) {
            return null;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxMachineBaseInfoVo> allCxMachineMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineMap)) {
            return null;
        }
        List<CxMachineBaseInfoVo> cxMachineList = new ArrayList<>(allCxMachineMap.values());
        List<CxMachineBaseInfoVo> leftOverCxMachineList = cxMachineList.stream().filter(cxMachine -> cxMachine.getRemainingDays() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(leftOverCxMachineList)) {
            return null;
        }
        String isZeroRack = addNewGroupPlan.getIsZero();
        String structureName = addNewGroupPlan.getGroupName();
        //零度匹配，不可作业剔除
        List<CxMachineBaseInfoVo> enableCxMachineList = new ArrayList<>(leftOverCxMachineList.size());
        leftOverCxMachineList.forEach(cxMachineInfo -> {
            if (!isZeroRack.equals(cxMachineInfo.getIsZeroRack())) {
                //todo 记录日志
                return;
            }
            if (cxMachineInfo.isNoProductionStructure(structureName)) {
                //todo 记录日志
                return;
            }
            List<MonthPlanProductionRequirePlanVo> groupPlanData = addNewGroupPlan.getGroupPlanData();
            if (CollectionUtils.isEmpty(groupPlanData)) {
                //todo 记录日志
                return;
            }
            Set<String> materialCodeSet = groupPlanData.stream().map(MonthPlanProductionRequirePlanVo::getMaterialCode).collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(materialCodeSet)) {
                //todo 记录日志
                return;
            }
            boolean isProduction = true;
            for (String materialCode : materialCodeSet) {
                if (cxMachineInfo.isNoProductionMaterial(materialCode)) {
                    isProduction = false;
                    break;
                }
            }
            if (!isProduction) {
                //todo 记录日志
                return;
            }
            String brandCode = cxMachineInfo.getCxMachineBrandCode();
            MonthPlanStructureLhRatioVo lhRatioInfo = addNewGroupPlan.getCxMachineLhRationMap().get(brandCode);
            if (null == lhRatioInfo) {
                //todo 记录日志
                return;
            }
            cxMachineInfo.setRatio(lhRatioInfo.getLhMachineMaxQty());
            enableCxMachineList.add(cxMachineInfo);
        });
        if (CollectionUtils.isEmpty(enableCxMachineList)) {
            return null;
        }
        //设置机台固定
        enableCxMachineList.stream().forEach(cxMachineInfo -> {
            cxMachineInfo.setFixedPriority(cxMachineInfo.getFixedPriorityValue(addNewGroupPlan));
        });
        //固定优先
        Integer minFixedPriority = enableCxMachineList.stream().mapToInt(CxMachineBaseInfoVo::getFixedPriority).min().getAsInt();
        List<CxMachineBaseInfoVo> fixedPriorityList = enableCxMachineList.stream().filter(cxMachineInfo -> minFixedPriority.equals(cxMachineInfo.getFixedPriority())).collect(Collectors.toList());
        if (fixedPriorityList.size() == BigDecimal.ONE.intValue()) {
            return fixedPriorityList.get(BigDecimal.ZERO.intValue());
        }
        //设置是否同规格，同英寸,断面宽
        setSameInfo(context, fixedPriorityList, addNewGroupPlan);
        //同规格优先 -> 同英寸优先 -> 断面宽优先 -> 剩余天数多 -> 机台编号
        Comparator sortComparator = Comparator.comparing(CxMachineBaseInfoVo::getSameSpecifications, Comparator.reverseOrder())
                .thenComparing(CxMachineBaseInfoVo::getSameProSize, Comparator.reverseOrder())
                .thenComparing(CxMachineBaseInfoVo::getSectionWidthCondition, Comparator.reverseOrder())
                .thenComparing(CxMachineBaseInfoVo::getRemainingDays, Comparator.reverseOrder())
                .thenComparing(CxMachineBaseInfoVo::getCxMachineCode, Comparator.reverseOrder());
        fixedPriorityList.sort(sortComparator);
        return fixedPriorityList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 得到成型机台剩余产能能覆盖剩余排产净需求的分组结构计划
     *
     * @param estimateGroupCxAllocationMap 分组结构计划集合
     * @param cxMachineInfo                成型机信息
     * @return
     */
    private static Map<String, ProductionPlanGroupInfo> getProductionCapacityCoverage(Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap, CxMachineBaseInfoVo cxMachineInfo) {
        if (null == cxMachineInfo || CollectionUtils.isEmpty(estimateGroupCxAllocationMap)) {
            return Collections.emptyMap();
        }
        //成型机剩余产能能覆盖剩余排产净需求
        Map<String, ProductionPlanGroupInfo> capacityCoverageMap = new HashMap<>(estimateGroupCxAllocationMap.size());
        Integer remainingDays = cxMachineInfo.getRemainingDays();
        String brandCode = cxMachineInfo.getCxMachineBrandCode();
        estimateGroupCxAllocationMap.forEach((structureName, groupPlan) -> {
            Long minLhDayCapacityQty = groupPlan.getMinLhDayCapacityQty();
            if (null == minLhDayCapacityQty || minLhDayCapacityQty <= BigDecimal.ZERO.longValue()) {
                //todo 记录日志
                return;
            }
            Map<String, MonthPlanStructureLhRatioVo> lhRatioMap = groupPlan.getCxMachineLhRationMap();
            if (CollectionUtils.isEmpty(lhRatioMap)) {
                //todo 记录日志
                return;
            }
            MonthPlanStructureLhRatioVo lhRatio = lhRatioMap.get(brandCode);
            if (null == lhRatio) {
                //todo 记录日志
                return;
            }
            Integer ratio = lhRatio.getLhMachineMaxQty();
            if (null == ratio || ratio <= BigDecimal.ZERO.intValue()) {
                //todo 记录日志
                return;
            }
            //记录配比-需要传递
            cxMachineInfo.setRatio(ratio);
            Long remainingProductionQty = groupPlan.getRemainingProductionQty();
            if (remainingProductionQty <= BigDecimal.ZERO.longValue()) {
                //todo 记录日志
                return;
            }
            //成型剩余产能
            Long remainingCapacityQty = BigDecimal.valueOf(minLhDayCapacityQty).multiply(BigDecimal.valueOf(ratio)).multiply(BigDecimal.valueOf(remainingDays)).multiply(BigDecimal.valueOf(ProductionConstant.DOUBLE_MOULD_PRODUCTION)).longValue();
            if (remainingCapacityQty < remainingProductionQty) {
                return;
            }
            capacityCoverageMap.put(structureName, groupPlan);
        });
        return capacityCoverageMap;
    }

    /**
     * 剔除不匹配的结构
     * 不可作业结构/SKU,零度不匹配
     *
     * @param capacityCoverageMap
     * @param cxMachineInfo
     * @return
     */
    private static Map<String, ProductionPlanGroupInfo> excludeDisable(Map<String, ProductionPlanGroupInfo> capacityCoverageMap, CxMachineBaseInfoVo cxMachineInfo) {
        if (CollectionUtils.isEmpty(capacityCoverageMap) || null == cxMachineInfo) {
            return Collections.emptyMap();
        }
        String isZeroRack = cxMachineInfo.getIsZeroRack();
        Map<String, ProductionPlanGroupInfo> enableProductionMap = new HashMap<>(capacityCoverageMap.size());
        capacityCoverageMap.forEach((structureName, groupPlan) -> {
            if (!isZeroRack.equals(groupPlan.getIsZero())) {
                //todo 记录日志
                return;
            }
            if (cxMachineInfo.isNoProductionStructure(structureName)) {
                //todo 记录日志
                return;
            }
            List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlan.getGroupPlanData();
            if (CollectionUtils.isEmpty(groupPlanData)) {
                //todo 记录日志
                return;
            }
            Set<String> materialCodeSet = groupPlanData.stream().map(MonthPlanProductionRequirePlanVo::getMaterialCode).collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(materialCodeSet)) {
                //todo 记录日志
                return;
            }
            boolean isProduction = true;
            for (String materialCode : materialCodeSet) {
                if (cxMachineInfo.isNoProductionMaterial(materialCode)) {
                    isProduction = false;
                    break;
                }
            }
            if (!isProduction) {
                //todo 记录日志
                return;
            }
            enableProductionMap.put(structureName, groupPlan);
        });
        return enableProductionMap;
    }

    /**
     * 获取最合适的一个结构
     * 1、固定优先
     * 2、成型的前结构同规格(SKU的规格属性)优先
     * 3、成型的前结构同英寸(SKU的英寸属性)优先
     * 4、成型的前结构断面宽±10
     * 5、近1个月结构上机日期近的优先
     * 6、近3个月结构生产次数多的优先
     *
     * @param enableGroupPlanMap
     * @param cxMachineInfo
     * @return
     */
    private static ProductionPlanGroupInfo selectedOne(Map<String, ProductionPlanGroupInfo> enableGroupPlanMap, CxMachineBaseInfoVo cxMachineInfo) {
        if (CollectionUtils.isEmpty(enableGroupPlanMap)) {
            return null;
        }
        List<ProductionPlanGroupInfo> groupPlanList = new ArrayList<>(enableGroupPlanMap.size());
        enableGroupPlanMap.forEach((structureName, groupPlan) -> {
            groupPlan.setFixedPriority(cxMachineInfo.getFixedPriorityValue(groupPlan));
            groupPlanList.add(groupPlan);
        });
        if (CollectionUtils.isEmpty(groupPlanList)) {
            return null;
        }
        //1、取固定的
        Integer minFixedPriority = groupPlanList.stream().mapToInt(ProductionPlanGroupInfo::getFixedPriority).min().getAsInt();
        List<ProductionPlanGroupInfo> fixedGroupPlanList = groupPlanList.stream().filter(groupPlan -> minFixedPriority.equals(groupPlan.getFixedPriority())).collect(Collectors.toList());
        if (fixedGroupPlanList.size() == BigDecimal.ONE.intValue()) {
            return fixedGroupPlanList.get(BigDecimal.ZERO.intValue());
        }
        List<CxMachineAllocationPlanHelper> allocationList = cxMachineInfo.getAllocationList();
        CxMachineAllocationPlanHelper lastHelper = allocationList.get(allocationList.size() - BigDecimal.ONE.intValue());
        List<MonthPlanProductionRequirePlanVo> realProductionPlanList = lastHelper.getRealProductionPlanList();
        //2、与前结构含有同规格的优先
        List<ProductionPlanGroupInfo> sameSpecificationsList = fixedGroupPlanList.stream().filter(fixedPlan -> fixedPlan.hasSameSpecifications(realProductionPlanList)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sameSpecificationsList)) {
            sameSpecificationsList = fixedGroupPlanList;
        }
        if (sameSpecificationsList.size() == BigDecimal.ONE.intValue()) {
            return sameSpecificationsList.get(BigDecimal.ZERO.intValue());
        }
        //3、与前结构含有同英寸的优先
        List<ProductionPlanGroupInfo> sameProSizeList = sameSpecificationsList.stream().filter(sameSpecificationsPlan -> sameSpecificationsPlan.hasSameProSize(realProductionPlanList)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sameProSizeList)) {
            sameProSizeList = sameSpecificationsList;
        }
        if (sameProSizeList.size() == BigDecimal.ONE.intValue()) {
            return sameProSizeList.get(BigDecimal.ZERO.intValue());
        }
        //4、断面宽差值±10 todo 断面宽差值范围
        Integer diffValue = 0;
        List<ProductionPlanGroupInfo> sectionWidthList = sameProSizeList.stream().filter(sectionWidthPlan -> sectionWidthPlan.hasSectionWidthCondition(realProductionPlanList, diffValue)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sectionWidthList)) {
            sectionWidthList = sameProSizeList;
        }
        if (sectionWidthList.size() == BigDecimal.ONE.intValue()) {
            return sameProSizeList.get(BigDecimal.ZERO.intValue());
        }
        sectionWidthList.sort(Comparator.comparing(ProductionPlanGroupInfo::getLastBoardingDate, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(ProductionPlanGroupInfo::getProductionCount, Comparator.nullsLast(Comparator.reverseOrder())));
        return sectionWidthList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 设置同规格、同英寸，断面宽等信息
     *
     * @param context           排产上下文
     * @param fixedPriorityList 机台集合
     * @param addNewGroupPlan   新增结构
     */
    private static void setSameInfo(Context context, List<CxMachineBaseInfoVo> fixedPriorityList, ProductionPlanGroupInfo addNewGroupPlan) {
        //4、断面宽差值±10 todo 断面宽差值范围
        Integer diffValue = 0;
        //设置是否同规格，同英寸,断面宽
        fixedPriorityList.forEach(cxMachineInfo -> {
            List<CxMachineAllocationPlanHelper> allocationList = cxMachineInfo.getAllocationList();
            CxMachineAllocationPlanHelper lastHelper = allocationList.get(allocationList.size() - BigDecimal.ONE.intValue());
            List<MonthPlanProductionRequirePlanVo> realProductionPlanList = lastHelper.getRealProductionPlanList();
            String sameSpecifications = YesOrNoEnum.NO.getCode();
            if (addNewGroupPlan.hasSameSpecifications(realProductionPlanList)) {
                sameSpecifications = YesOrNoEnum.YES.getCode();
            }
            cxMachineInfo.setSameSpecifications(sameSpecifications);
            String sameProSize = YesOrNoEnum.NO.getCode();
            if (addNewGroupPlan.hasSameProSize(realProductionPlanList)) {
                sameProSize = YesOrNoEnum.YES.getCode();
            }
            cxMachineInfo.setSameProSize(sameProSize);
            String sectionWidthCondition = YesOrNoEnum.NO.getCode();
            if (addNewGroupPlan.hasSectionWidthCondition(realProductionPlanList, diffValue)) {
                sectionWidthCondition = YesOrNoEnum.YES.getCode();
            }
            cxMachineInfo.setSectionWidthCondition(sectionWidthCondition);
        });
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
