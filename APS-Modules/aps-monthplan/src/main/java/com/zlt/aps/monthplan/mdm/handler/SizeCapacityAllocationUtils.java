package com.zlt.aps.monthplan.mdm.handler;

import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.FormingMethodTypeEnum;
import com.tlt.aps.enums.WorkWearTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.domain.vo.SizeCapacityParamVo;
import com.zlt.aps.monthplan.api.domain.vo.BaseMoldingMachineInfoVo;
import com.zlt.aps.monthplan.api.domain.vo.MoldingMachineAllocationInfoVo;
import com.zlt.aps.monthplan.mdm.dto.SizeCapacityAllocationDto;
import com.zlt.aps.monthplan.mdm.dto.SizeCapacityAllocationResultDto;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 寸口产能分配工具辅助类
 *
 * @author ZLT
 * @date 20250812
 */
public class SizeCapacityAllocationUtils {
    /**
     * 硫化模台数 2
     */
    private static final Integer CURING_MOULD_TABLE_NUMBER = 2;
    /**
     * 一台成型产能只能换一次寸口，故而分配寸口的值只能是2
     */
    private static final Integer CHANGE_PRO_SIZE_MATCH = 2;

    /**
     * 获取成型产能挑选规则：
     * 限制的在后，再获取剩余天数多的，其次获取天产能大的，最后才是编号
     *
     * @return
     */
    public static Comparator getMoldingMachineCapacitySort() {
        return Comparator.comparing(BaseMoldingMachineInfoVo::getSpecialRestrictions)
                .thenComparing(BaseMoldingMachineInfoVo::getLeftOverCapacityDays)
                .thenComparing(BaseMoldingMachineInfoVo::getProSizeQuotaQty, Comparator.reverseOrder())
                .thenComparing(BaseMoldingMachineInfoVo::getMoldingMachineCode);
    }

    /**
     * 获取成型产能挤占挑选规则：
     * 按分配寸口小的优先->剩余产能天数大的优先->机型天产能大的优先，最后才是编号
     *
     * @return
     */
    public static Comparator getCrowdOutSort() {
        return Comparator.comparing(BaseMoldingMachineInfoVo::getMinAssignedProSize)
                .thenComparing(BaseMoldingMachineInfoVo::getLeftOverCapacityDays, Comparator.reverseOrder())
                .thenComparing(BaseMoldingMachineInfoVo::getProSizeQuotaQty, Comparator.reverseOrder())
                .thenComparing(BaseMoldingMachineInfoVo::getMoldingMachineCode);
    }

    /**
     * 按寸口，设置对应成型产能的对应寸口的天最大产能
     * 此时，没有进行换规格消耗
     *
     * @param moldingMachineCapacityList 成型产能列表
     * @param proSize                    寸口
     */
    public static void setDayMaxProSizeCapacityInfo(List<BaseMoldingMachineInfoVo> moldingMachineCapacityList, BigDecimal proSize) {
        if (CollectionUtils.isEmpty(moldingMachineCapacityList)) {
            return;
        }
        moldingMachineCapacityList.stream().forEach(moldingMachineCapacity -> {
            Map<BigDecimal, Long> proSizeQuotaQtyMap = moldingMachineCapacity.getProSizeQuotaQtyMap();
            Map<BigDecimal, BigDecimal> proSizeSulfurizationMap = moldingMachineCapacity.getMoldingMachineProSizeSulfurizationMachineMap();
            if (CollectionUtils.isEmpty(proSizeQuotaQtyMap)) {
                moldingMachineCapacity.setProSizeQuotaQty(null);
                return;
            }
            if (!proSizeQuotaQtyMap.containsKey(proSize)) {
                moldingMachineCapacity.setProSizeQuotaQty(null);
                return;
            }
            moldingMachineCapacity.setProSizeQuotaQty(proSizeQuotaQtyMap.get(proSize));
            Integer dayMouldQty;
            BigDecimal sulfurationRatio = proSizeSulfurizationMap.get(proSize);
            if (null == sulfurationRatio) {
                dayMouldQty = BigDecimal.ZERO.intValue();
            } else {
                dayMouldQty = BigDecimal.valueOf(CURING_MOULD_TABLE_NUMBER).multiply(sulfurationRatio).setScale(0, RoundingMode.HALF_UP).intValue();
            }
            moldingMachineCapacity.setDayMaxMouldQty(dayMouldQty);
        });
    }

    /**
     * 根据需求量，计算各成型产能完成需求量所属天数
     *
     * @param moldingMachineInfoList 成型产能集合列表
     * @param param                  寸口分配参数
     * @param requirementQty         需求量
     */
    public static void setRequirementQtyNeedDays(List<BaseMoldingMachineInfoVo> moldingMachineInfoList, SizeCapacityParamVo param, Long requirementQty) {
        if (CollectionUtils.isEmpty(moldingMachineInfoList) || null == requirementQty || requirementQty < BigDecimal.ZERO.intValue()) {
            return;
        }
        moldingMachineInfoList.stream().forEach(haseLeftOverCapacity -> setRequirementNeedDays(haseLeftOverCapacity, param, requirementQty));
    }

    /**
     * 剩余天数处理，不进行寸口切换
     * 支持两种场景：
     * 1、剩余天数小于最小天数参数限定，则不进行切换
     * 2、分配完成后，最后的剩余天数也不进行切换
     *
     * @param moldingMachineCapacity 成型产能对象
     * @param globalRequirementMap   所有需求信息
     */
    public static void noChangeProSizeByLeftOverDays(BaseMoldingMachineInfoVo moldingMachineCapacity, Map<String, SizeCapacityAllocationResultDto> globalRequirementMap) {
        if (null == moldingMachineCapacity) {
            return;
        }
        List<MoldingMachineAllocationInfoVo> proSizeRequirementList = moldingMachineCapacity.getProSizeRequirementList();
        if (CollectionUtils.isEmpty(proSizeRequirementList)) {
            return;
        }
        int maxSize = proSizeRequirementList.size();
        MoldingMachineAllocationInfoVo lastAllocation = proSizeRequirementList.get(maxSize - BigDecimal.ONE.intValue());
        SizeCapacityAllocationResultDto requirement = globalRequirementMap.get(lastAllocation.getSizeCapacityGroupKey());
        if (null == requirement) {
            return;
        }
        Integer realAllocationDays = lastAllocation.getAllocationDays();
        Integer addAllocationDays = moldingMachineCapacity.getLeftOverCapacityDays();
        lastAllocation.setAllocationDays(realAllocationDays + addAllocationDays);
        moldingMachineCapacity.setFinishAllocation(true);
        //剩余需求量
        Long leftOverQtyRequirement = requirement.getLeftOverQty();
        //增加分配的需求量
        Long dayCapacityQty = lastAllocation.getProSizeQuotaQty();
        Long subtractQty = dayCapacityQty * addAllocationDays;
        requirement.setLeftOverQty(leftOverQtyRequirement - subtractQty);
        List<SizeCapacityAllocationDto> allocationList = requirement.getAllocationList();
        if (CollectionUtils.isEmpty(allocationList)) {
            return;
        }
        List<SizeCapacityAllocationDto> moldingMachineCapacityList = allocationList.stream().filter(moldingMachineAllocationInfo -> moldingMachineAllocationInfo.getMoldingMachineCode().equals(moldingMachineCapacity.getMoldingMachineCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(moldingMachineCapacityList)) {
            return;
        }
        SizeCapacityAllocationDto moldingMachineAllocationInfo = moldingMachineCapacityList.get(0);
        Integer allocationDays = moldingMachineAllocationInfo.getAllocationDay();
        moldingMachineAllocationInfo.setAllocationDay(allocationDays + addAllocationDays);
    }

    /**
     * 获取待分配需求
     * 寸口小于proSize。
     *
     * @param globalRequirementMap 所有需求集合
     * @param mouldMethodType      成型法
     * @param tireFabricNumber     胎体层级
     * @param proSize              寸口
     * @return
     */
    public static List<SizeCapacityAllocationResultDto> getNeedAllocationRequirementList(Map<String, SizeCapacityAllocationResultDto> globalRequirementMap, FormingMethodTypeEnum mouldMethodType, Integer tireFabricNumber, BigDecimal proSize) {
        if (CollectionUtils.isEmpty(globalRequirementMap) || null == mouldMethodType || null == proSize || null == tireFabricNumber) {
            return Collections.emptyList();
        }
        List<SizeCapacityAllocationResultDto> allRequirementList = new ArrayList<>(globalRequirementMap.values());
        List<SizeCapacityAllocationResultDto> leftOverRequirementList = allRequirementList.stream().filter(requirement -> requirement.isNeedNextAllocation(mouldMethodType, tireFabricNumber, proSize)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(leftOverRequirementList)) {
            return Collections.emptyList();
        }
        return leftOverRequirementList;
    }

    /**
     * 20251027 是否达到特殊限制
     * 1、二次法18寸是否还可进行分配
     * 2、一次法20寸是否还可进行分配
     *
     * @param moldingMachineCapacity    被分配的成型产能
     * @param param                     寸口产能分配参数配置
     * @param needAllocationRequirement 需要分配的需求
     * @return
     */
    public static boolean isReachLimitNumber(BaseMoldingMachineInfoVo moldingMachineCapacity, SizeCapacityParamVo param, SizeCapacityAllocationResultDto needAllocationRequirement) {
        BigDecimal proSize = needAllocationRequirement.getProSize();
        String workWearType = needAllocationRequirement.getWorkWearType();
        String mouldMethod = needAllocationRequirement.getMouldMethod();
        Integer tireFabricNumber = needAllocationRequirement.getTireFabricNumber();
        String moldingMachineCode = moldingMachineCapacity.getMoldingMachineCode();
        return param.isReachLimitNumber(proSize, workWearType, mouldMethod, tireFabricNumber, moldingMachineCode);
    }

    /**
     * 对moldingMachineCapacity成型产能，分配needAllocationRequirement需求
     * 按需求量分配成型天数
     *
     * @param moldingMachineCapacity    成型产能对象
     * @param param                     寸口产能分配参数
     * @param needAllocationRequirement 分配需求量对象
     */
    public static void allocation(BaseMoldingMachineInfoVo moldingMachineCapacity, SizeCapacityParamVo param, SizeCapacityAllocationResultDto needAllocationRequirement) {
        if (null == moldingMachineCapacity || null == needAllocationRequirement) {
            return;
        }
        Long requirementQty = needAllocationRequirement.getLeftOverQty();
        if (requirementQty <= BigDecimal.ZERO.longValue()) {
            return;
        }
        Integer leftOverDays = moldingMachineCapacity.getLeftOverCapacityDays();
        if (leftOverDays <= BigDecimal.ZERO.intValue()) {
            return;
        }
        Long dayCapacityQty = calculateDayCapacity(moldingMachineCapacity, param);
        if (dayCapacityQty <= BigDecimal.ZERO.longValue()) {
            return;
        }
        //20250917 ZLT 特殊限制控制 1、二次法18寸是否还可进行分配 2、一次法20寸是否还可进行分配
        boolean isReachLimit = isReachLimitNumber(moldingMachineCapacity, param, needAllocationRequirement);
        if (isReachLimit) {
            needAllocationRequirement.setFinishAllocation(true);
            return;
        }
        //需要分配的天数
        Integer days = BigDecimal.valueOf(requirementQty).divide(BigDecimal.valueOf(dayCapacityQty), 0, RoundingMode.UP).intValue();
//        if (moldingMachineCapacity.isFirstAllocation(needAllocationRequirement.getProSize())) {
//            //首次分配寸口
//            if (days < param.getMinAllocationDay()) {
//                days = param.getMinAllocationDay();
//            }
//        }
        Integer allocationDays;
        if (days > leftOverDays) {
            allocationDays = leftOverDays;
        } else {
            allocationDays = days;
        }
        //新的成型产能剩余天数
        Integer moldingMachineLeftOverDays = leftOverDays - allocationDays;
        moldingMachineCapacity.setLeftOverCapacityDays(moldingMachineLeftOverDays);
        //更新需求分配信息和成型产能分配信息
        updateAllocationInfo(param, moldingMachineCapacity, needAllocationRequirement, requirementQty, dayCapacityQty, allocationDays);
        //20250917 ZLT 增加二次法18寸已分配信息
//        param.addSpecialRestriction(proSize, workWearType, mouldMethod, tireFabricNumber, moldingMachineCode);
    }

    /**
     * 采用挤占式分配，不能挤占比自己高的寸口
     *
     * @param moldingMachineCapacity    成型产能对象
     * @param param                     寸口分配参数
     * @param globalRequirementMap      所有需求信息
     * @param needAllocationRequirement 需要分配的需求信息
     */
    public static void allocationByCrowdOut(BaseMoldingMachineInfoVo moldingMachineCapacity, SizeCapacityParamVo param, Map<String, SizeCapacityAllocationResultDto> globalRequirementMap, SizeCapacityAllocationResultDto needAllocationRequirement) {
        if (null == moldingMachineCapacity || null == needAllocationRequirement) {
            return;
        }
        Long requirementQty = needAllocationRequirement.getLeftOverQty();
        if (requirementQty <= BigDecimal.ZERO.longValue()) {
            return;
        }
        Long dayCapacityQty = calculateDayCapacity(moldingMachineCapacity, param);
        if (dayCapacityQty <= BigDecimal.ZERO.longValue()) {
            return;
        }
        //20251027 ZLT 特殊限制控制 1、二次法18寸是否还可进行分配 2、一次法20寸是否还可进行分配
        boolean isReachLimit = isReachLimitNumber(moldingMachineCapacity, param, needAllocationRequirement);
        if (isReachLimit) {
            needAllocationRequirement.setFinishAllocation(true);
            return;
        }
        Integer leftOverDays = moldingMachineCapacity.getLeftOverCapacityDays();
        //需要分配的天数
        Integer days = BigDecimal.valueOf(requirementQty).divide(BigDecimal.valueOf(dayCapacityQty), 0, RoundingMode.UP).intValue();
        //可满足需求量，空成型产能
        if (days <= leftOverDays && CollectionUtils.isEmpty(moldingMachineCapacity.getAssignedProSize())) {
            updateAllocationInfo(param, moldingMachineCapacity, needAllocationRequirement, requirementQty, dayCapacityQty, days);
            moldingMachineCapacity.setLeftOverCapacityDays(leftOverDays - days);
            return;
        }
        //可满足需求量，不是空成型产能
        if (days <= leftOverDays && !CollectionUtils.isEmpty(moldingMachineCapacity.getAssignedProSize())) {
            boolean hasProSize = moldingMachineCapacity.getAssignedProSize().contains(needAllocationRequirement.getProSize());
            updateAllocationInfo(param, moldingMachineCapacity, needAllocationRequirement, requirementQty, dayCapacityQty, leftOverDays);
            moldingMachineCapacity.setLeftOverCapacityDays(leftOverDays - days);
            if (!hasProSize) {
                moldingMachineCapacity.setFinishAllocation(true);
            }
            return;
        }
        //20250916 ZLT 没有开启挤占
        if (!FactoryConstant.YES_VALUE.equals(param.getOpenCrowdOut())) {
            //剩余天数 < 最小天数限定
            if (leftOverDays < param.getMaxLeftOverDays()) {
                SizeCapacityAllocationUtils.noChangeProSizeByLeftOverDays(moldingMachineCapacity, globalRequirementMap);
                return;
            }
            updateAllocationInfo(param, moldingMachineCapacity, needAllocationRequirement, requirementQty, dayCapacityQty, leftOverDays);
            moldingMachineCapacity.setLeftOverCapacityDays(BigDecimal.ZERO.intValue());
            moldingMachineCapacity.setFinishAllocation(true);
            return;
        }
        //不能满足需求量-此时需要挤占前面的寸口需求
        Integer maxAllocationDays = param.getMonthMaxDays();
        if (days >= maxAllocationDays) {
            //整台占用
            wholeOccupyHandler(param, moldingMachineCapacity, maxAllocationDays, globalRequirementMap, needAllocationRequirement, requirementQty, dayCapacityQty);
            return;
        }
        //部分挤占
        partialOccupyHandler(param, moldingMachineCapacity, days, globalRequirementMap, needAllocationRequirement, requirementQty, dayCapacityQty);
    }

    /**
     * 更新分配信息，包含：
     * 1、对需求信息进行更新，增加分配的成型产能信息
     * 2、对成型产能信息更新，增加分配的需求信息
     *
     * @param param                        产能分配参数
     * @param allocationMoldingMachineInfo 分配的成型产能信息对象
     * @param needAllocationRequirement    需分配成型产能的需求信息对象
     * @param needRequirementQty           需要分配的需求量
     * @param dayCapacityQty               天产能(扣除换规格消耗)
     * @param allocationDays               分配天数
     */
    public static void updateAllocationInfo(SizeCapacityParamVo param, BaseMoldingMachineInfoVo allocationMoldingMachineInfo, SizeCapacityAllocationResultDto needAllocationRequirement, Long needRequirementQty, Long dayCapacityQty, Integer allocationDays) {
        Long allocationRequirementQty = allocationDays * dayCapacityQty;
        //新的剩余需求量
        needAllocationRequirement.setLeftOverQty(needRequirementQty - allocationRequirementQty);
        //需求分配信息
        SizeCapacityAllocationDto allocationInfo = buildBaseAllocationInfo(needAllocationRequirement, allocationMoldingMachineInfo, allocationDays, dayCapacityQty);
        needAllocationRequirement.addAllocationInfo(allocationInfo);
        //成型产能分配信息
        MoldingMachineAllocationInfoVo moldingMachineAllocationInfo = buildMoldingMachineAllocationInfo(needAllocationRequirement, allocationDays, dayCapacityQty, Math.min(needRequirementQty, allocationRequirementQty));
        moldingMachineAllocationInfo.setDayMaxMouldQty(allocationMoldingMachineInfo.getDayMaxMouldQty());
        allocationMoldingMachineInfo.addAllocationRequirementInfo(moldingMachineAllocationInfo);
        //20250917 ZLT 特殊限制控制记录 1、增加二次法18寸已分配信息 2、增加20寸一次法已分配信息
        BigDecimal proSize = needAllocationRequirement.getProSize();
        String workWearType = needAllocationRequirement.getWorkWearType();
        String mouldMethod = needAllocationRequirement.getMouldMethod();
        Integer tireFabricNumber = needAllocationRequirement.getTireFabricNumber();
        String moldingMachineCode = allocationMoldingMachineInfo.getMoldingMachineCode();
        param.addSpecialRestriction(proSize, workWearType, mouldMethod, tireFabricNumber, moldingMachineCode);
    }

    /**
     * 获取可以一台满足剩余需求量的成型产能集合
     * 1、还需要的天数不可超过1个月的天数
     * 2、剩余天数 > 还需要的天数
     *
     * @param haseLeftOverCapacityList 符合条件的剩余成型产能
     * @param param                    寸口产能参数
     * @return
     */
    public static List<BaseMoldingMachineInfoVo> getSatisfyByOneMoldingMachineInfo(List<BaseMoldingMachineInfoVo> haseLeftOverCapacityList, SizeCapacityParamVo param) {
        if (CollectionUtils.isEmpty(haseLeftOverCapacityList)) {
            return Collections.emptyList();
        }
        List<BaseMoldingMachineInfoVo> hasNoPassWholeMoldingMachineList = haseLeftOverCapacityList.stream().filter(haseLeftOverCapacity -> haseLeftOverCapacity.getRequirementNeedDays() <= param.getMonthMaxDays()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasNoPassWholeMoldingMachineList)) {
            return Collections.emptyList();
        }
        List<BaseMoldingMachineInfoVo> hasCoverRequirementMoldingMachineList = hasNoPassWholeMoldingMachineList.stream().filter(haseLeftOverCapacity -> haseLeftOverCapacity.getLeftOverCapacityDays() >= haseLeftOverCapacity.getRequirementNeedDays()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasCoverRequirementMoldingMachineList)) {
            return Collections.emptyList();
        }
        return hasCoverRequirementMoldingMachineList;
    }

    /**
     * 计算成型的天产能信息
     *
     * @param moldingMachine 成型信息
     * @param param          参数
     * @return
     */
    public static Long calculateDayCapacity(BaseMoldingMachineInfoVo moldingMachine, SizeCapacityParamVo param) {
        //得到天产能
        Long dayMaxCapacityQty = moldingMachine.getProSizeQuotaQty();
        Integer currentEmbryoCodeNumber = moldingMachine.getCurrentEmbryoCodeNumber();
        return calculateDayCapacity(moldingMachine.getMouldMethod(), currentEmbryoCodeNumber, dayMaxCapacityQty, param);
    }

    /**
     * 获取可分配的成型产能列表--严格匹配
     * 1、先按成型法匹配过滤
     * 2、再按胎体层级，多层找多层，单层找单层
     * 3、在按寸口匹配--寸口班产中存在匹配寸口
     * 4、20250917 增加18寸二次法的特殊处理，大鼓工装类型的匹配
     *
     * @param proSize                寸口
     * @param workWearType           工装类别
     * @param mouldMethod            成形法
     * @param tireFabricNumber       胎体布层级
     * @param moldingMachineInfoList 可挑选的成型产能集合
     * @return
     */
    public static List<BaseMoldingMachineInfoVo> getEnableAllocationMoldingMachineByStrictMatch(BigDecimal proSize, String workWearType, String mouldMethod, Integer tireFabricNumber, List<BaseMoldingMachineInfoVo> moldingMachineInfoList) {
        if (CollectionUtils.isEmpty(moldingMachineInfoList)) {
            return Collections.emptyList();
        }
        //层级匹配--严格按层级、成型法
        List<BaseMoldingMachineInfoVo> tireFabricNumberList = moldingMachineInfoList.stream().filter(moldingMachine -> moldingMachine.moldingMachineByStrictMatch(mouldMethod, tireFabricNumber)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tireFabricNumberList)) {
            return Collections.emptyList();
        }
        //18寸二次法
        boolean isSpecialRestrictions = FormingMethodTypeEnum.TWO_STAGE_TIRE.getMethodValue().equals(mouldMethod) && WorkWearTypeEnum.PRO_SIZE_18.equals(proSize);
        //寸口匹配
        List<BaseMoldingMachineInfoVo> enableAllocationList = new ArrayList<>();
        tireFabricNumberList.stream().forEach(tireFabricMachine -> {
            //寸口不支持
            if (!tireFabricMachine.getProSizeQuotaQtyMap().containsKey(proSize)) {
                return;
            }
            //20250917 ZLT 18寸二次法大鼓匹配
            if (isSpecialRestrictions) {
                //大鼓需求，非大鼓产能
                if (WorkWearTypeEnum.BIG_DRUM.getTypeValue().equals(workWearType) && YesOrNoEnum.NO.getValue().equals(tireFabricMachine.getSpecialRestrictions())) {
                    return;
                }
                //非大鼓需求，大鼓产能
                if (!WorkWearTypeEnum.BIG_DRUM.getTypeValue().equals(workWearType) && YesOrNoEnum.YES.getValue().equals(tireFabricMachine.getSpecialRestrictions())) {
                    return;
                }
            }
            //设置寸口的天产能
            tireFabricMachine.setProSizeQuotaQty(tireFabricMachine.getProSizeQuotaQtyMap().get(proSize));
            enableAllocationList.add(tireFabricMachine);
        });
        if (CollectionUtils.isEmpty(enableAllocationList)) {
            return Collections.emptyList();
        }
        //只获取有产能的成型产能
        List<BaseMoldingMachineInfoVo> leftOverCapacityList = enableAllocationList.stream().filter(enableAllocationMachineCapacity -> enableAllocationMachineCapacity.getLeftOverCapacityDays() > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        return leftOverCapacityList;
    }

    /**
     * 获取匹配寸口+成型法+胎体布层级(多层只能是多层，单层可以是多层也可以是单层)的有剩余产能的成型产能
     * 1、获取成型产能还有剩余天数
     * 2、成型产能成型法匹配
     * 3、成型产能的胎体层级匹配：多层只能匹配多层，单层可匹配单层和多层
     * 4、成型产能寸口班产包含需要匹配的寸口
     *
     * @param proSize                寸口
     * @param mouldMethod            成型法
     * @param tireFabricNumber       胎体布层级
     * @param moldingMachineInfoList 成型产能集合
     * @return
     */
    public static List<BaseMoldingMachineInfoVo> getEnableAllocationMoldingMachineCapacity(BigDecimal proSize, String mouldMethod, Integer tireFabricNumber, List<BaseMoldingMachineInfoVo> moldingMachineInfoList) {
        if (CollectionUtils.isEmpty(moldingMachineInfoList)) {
            return Collections.emptyList();
        }
        List<BaseMoldingMachineInfoVo> hasLeftOverList = moldingMachineInfoList.stream().filter(leftOverMoldingMachine -> leftOverMoldingMachine.isHasCapacity()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasLeftOverList)) {
            return Collections.emptyList();
        }
        List<BaseMoldingMachineInfoVo> mouldMethodList = hasLeftOverList.stream().filter(moldingMachine -> mouldMethod.equals(moldingMachine.getMouldMethod())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mouldMethodList)) {
            return Collections.emptyList();
        }
        //多层级，则只能是多层级的
        List<BaseMoldingMachineInfoVo> tireFabricList;
        if (tireFabricNumber > BigDecimal.ONE.intValue()) {
            tireFabricList = mouldMethodList.stream().filter(moldingMachine -> tireFabricNumber.equals(moldingMachine.getCarcassClothType())).collect(Collectors.toList());
        } else {
            tireFabricList = mouldMethodList;
        }
        if (CollectionUtils.isEmpty(tireFabricList)) {
            return Collections.emptyList();
        }
        //寸口匹配
        List<BaseMoldingMachineInfoVo> enableAllocationList = new ArrayList<>();
        tireFabricList.stream().forEach(tireFabricMachine -> {
            if (!tireFabricMachine.getProSizeQuotaQtyMap().containsKey(proSize)) {
                return;
            }
            tireFabricMachine.setProSizeQuotaQty(tireFabricMachine.getProSizeQuotaQtyMap().get(proSize));
            enableAllocationList.add(tireFabricMachine);
        });
        if (CollectionUtils.isEmpty(enableAllocationList)) {
            return Collections.emptyList();
        }
        return enableAllocationList;
    }

    /**
     * 获取非挤占式-匹配寸口+成型法+胎体布层级(多层只能是多层，单层可以是多层也可以是单层)的有剩余产能的成型产能
     * 1、获取成型产能还有剩余天数
     * 2、成型产能成型法匹配
     * 3、成型产能的胎体层级匹配：多层只能匹配多层，单层可匹配单层和多层
     * 4、成型产能寸口班产包含需要匹配的寸口
     *
     * @param proSize                寸口
     * @param mouldMethod            成型法
     * @param tireFabricNumber       胎体布层级
     * @param moldingMachineInfoList 成型产能集合
     * @return
     */
    public static List<BaseMoldingMachineInfoVo> getEnableAllocationMoldingMachineCapacityByNoCrowdOut(BigDecimal proSize, String mouldMethod, Integer tireFabricNumber, List<BaseMoldingMachineInfoVo> moldingMachineInfoList) {
        if (CollectionUtils.isEmpty(moldingMachineInfoList)) {
            return Collections.emptyList();
        }
        List<BaseMoldingMachineInfoVo> hasLeftOverList = moldingMachineInfoList.stream().filter(leftOverMoldingMachine -> leftOverMoldingMachine.isHasCapacity()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasLeftOverList)) {
            return Collections.emptyList();
        }
        List<BaseMoldingMachineInfoVo> mouldMethodList = hasLeftOverList.stream().filter(moldingMachine -> mouldMethod.equals(moldingMachine.getMouldMethod())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mouldMethodList)) {
            return Collections.emptyList();
        }
        //多层级，则只能是多层级的
        List<BaseMoldingMachineInfoVo> tireFabricList;
        if (tireFabricNumber > BigDecimal.ONE.intValue()) {
            tireFabricList = mouldMethodList.stream().filter(moldingMachine -> tireFabricNumber.equals(moldingMachine.getCarcassClothType())).collect(Collectors.toList());
        } else {
            tireFabricList = mouldMethodList;
        }
        if (CollectionUtils.isEmpty(tireFabricList)) {
            return Collections.emptyList();
        }
        //寸口匹配
        List<BaseMoldingMachineInfoVo> enableAllocationList = new ArrayList<>();
        tireFabricList.stream().forEach(tireFabricMachine -> {
            if (!tireFabricMachine.getProSizeQuotaQtyMap().containsKey(proSize)) {
                return;
            }
            Set<BigDecimal> proSizeSet = tireFabricMachine.getAssignedProSize();
            if (CollectionUtils.isEmpty(proSizeSet)) {
                proSizeSet = new HashSet<>();
            }
            proSizeSet.add(proSize);
            if (proSizeSet.size() > CHANGE_PRO_SIZE_MATCH) {
                return;
            }
            tireFabricMachine.setProSizeQuotaQty(tireFabricMachine.getProSizeQuotaQtyMap().get(proSize));
            enableAllocationList.add(tireFabricMachine);
        });
        if (CollectionUtils.isEmpty(enableAllocationList)) {
            return Collections.emptyList();
        }
        return enableAllocationList;
    }

    /**
     * 获取在产寸口成型产能已分配完毕的成型产能集合
     *
     * @param proSize                寸口
     * @param mouldMethod            成型法
     * @param tireFabricNumber       胎体布层级
     * @param moldingMachineInfoList 成型产能集合
     * @return
     */
    public static List<BaseMoldingMachineInfoVo> getContinueMoldingMachineCapacity(BigDecimal proSize, String mouldMethod, Integer tireFabricNumber, List<BaseMoldingMachineInfoVo> moldingMachineInfoList) {
        if (CollectionUtils.isEmpty(moldingMachineInfoList)) {
            return Collections.emptyList();
        }
        List<BaseMoldingMachineInfoVo> noLeftOverList = moldingMachineInfoList.stream().filter(leftOverMoldingMachine -> null != leftOverMoldingMachine.getCurrentProSize() && leftOverMoldingMachine.getLeftOverCapacityDays() <= BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(noLeftOverList)) {
            return Collections.emptyList();
        }
        List<BaseMoldingMachineInfoVo> mouldMethodList = noLeftOverList.stream().filter(moldingMachine -> mouldMethod.equals(moldingMachine.getMouldMethod())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mouldMethodList)) {
            return Collections.emptyList();
        }
        //多层级，则只能是多层级的
        List<BaseMoldingMachineInfoVo> tireFabricList;
        if (tireFabricNumber > BigDecimal.ONE.intValue()) {
            tireFabricList = mouldMethodList.stream().filter(moldingMachine -> tireFabricNumber.equals(moldingMachine.getCarcassClothType())).collect(Collectors.toList());
        } else {
            tireFabricList = mouldMethodList;
        }
        if (CollectionUtils.isEmpty(tireFabricList)) {
            return Collections.emptyList();
        }
        //寸口匹配
        List<BaseMoldingMachineInfoVo> enableAllocationList = new ArrayList<>();
        tireFabricList.stream().forEach(tireFabricMachine -> {
            if (!tireFabricMachine.getProSizeQuotaQtyMap().containsKey(proSize)) {
                return;
            }
            tireFabricMachine.setProSizeQuotaQty(tireFabricMachine.getProSizeQuotaQtyMap().get(proSize));
            enableAllocationList.add(tireFabricMachine);
        });
        if (CollectionUtils.isEmpty(enableAllocationList)) {
            return Collections.emptyList();
        }
        return enableAllocationList;

    }

    /**
     * 获取可分配的成型产能列表，
     * 多层需求只能是多层产能，单层需求可多层也可单层
     * 1、先按成型法匹配过滤
     * 2、再按胎体层级，多层产能可生产单层需求
     * 3、在按寸口匹配--在产班产
     *
     * @param proSize                寸口
     * @param mouldMethod            成形法
     * @param tireFabricNumber       胎体布层级
     * @param moldingMachineInfoList 可挑选的成型产能集合
     * @return
     */
    public static List<BaseMoldingMachineInfoVo> getEnableAllocationMoldingMachine(BigDecimal proSize, String mouldMethod, Integer tireFabricNumber, List<BaseMoldingMachineInfoVo> moldingMachineInfoList) {
        if (CollectionUtils.isEmpty(moldingMachineInfoList)) {
            return Collections.emptyList();
        }
        List<BaseMoldingMachineInfoVo> mouldMethodList = moldingMachineInfoList.stream().filter(moldingMachine -> mouldMethod.equals(moldingMachine.getMouldMethod())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mouldMethodList)) {
            return Collections.emptyList();
        }
        //多层级，则只能是多层级的
        List<BaseMoldingMachineInfoVo> tireFabricList;
        if (tireFabricNumber > BigDecimal.ONE.intValue()) {
            tireFabricList = mouldMethodList.stream().filter(moldingMachine -> tireFabricNumber.equals(moldingMachine.getCarcassClothType())).collect(Collectors.toList());
        } else {
            tireFabricList = mouldMethodList;
        }
        if (CollectionUtils.isEmpty(tireFabricList)) {
            return Collections.emptyList();
        }
        //寸口匹配
        List<BaseMoldingMachineInfoVo> enableAllocationList = new ArrayList<>();
        tireFabricList.stream().forEach(tireFabricMachine -> {
            if (!tireFabricMachine.getProSizeQuotaQtyMap().containsKey(proSize)) {
                return;
            }
            tireFabricMachine.setProSizeQuotaQty(tireFabricMachine.getProSizeQuotaQtyMap().get(proSize));
            enableAllocationList.add(tireFabricMachine);
        });
        if (CollectionUtils.isEmpty(enableAllocationList)) {
            return Collections.emptyList();
        }
        List<BaseMoldingMachineInfoVo> leftOverCapacityList = enableAllocationList.stream().filter(enableAllocationMachineCapacity -> enableAllocationMachineCapacity.getLeftOverCapacityDays() > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        return leftOverCapacityList;
    }

    /**
     * 根据需求，设置成型产能所需要的天数
     *
     * @param moldingMachine 成型产能
     * @param param          寸口分配参数
     * @param requirementQty 分配的需求量
     */
    private static void setRequirementNeedDays(BaseMoldingMachineInfoVo moldingMachine, SizeCapacityParamVo param, Long requirementQty) {
        if (null == moldingMachine || requirementQty <= BigDecimal.ZERO.longValue()) {
            return;
        }
        moldingMachine.setRequirementNeedDays(null);
        Long realDayCapacityQty = calculateDayCapacity(moldingMachine, param);
        Integer needDays = BigDecimal.valueOf(requirementQty).divide(BigDecimal.valueOf(realDayCapacityQty), 0, RoundingMode.UP).intValue();
        moldingMachine.setRequirementNeedDays(needDays);
    }

    /**
     * 计算成型的天产能信息
     *
     * @param mouldMethod             成型法
     * @param currentEmbryoCodeNumber 当前规格数
     * @param dayMaxCapacityQty       寸口的天产能
     * @param param                   参数
     * @return
     */
    private static Long calculateDayCapacity(String mouldMethod, Integer currentEmbryoCodeNumber, Long dayMaxCapacityQty, SizeCapacityParamVo param) {
        //减去切换扣减
        Integer changeCount = param.getDefaultCount();
        if (null != currentEmbryoCodeNumber) {
            changeCount = currentEmbryoCodeNumber;
        }
        if (null != param.getAdditionalCount()) {
            changeCount = changeCount + param.getAdditionalCount();
        }
        Long needSubtractQty;
        if (FormingMethodTypeEnum.SINGLE_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            needSubtractQty = Long.valueOf(changeCount * param.getOneMouldMethodSubtractQty());
        } else {
            needSubtractQty = Long.valueOf(changeCount * param.getTwoMouldMethodSubtractQty());
        }
        return dayMaxCapacityQty - needSubtractQty;
    }

    /**
     * 构建分配信息
     *
     * @param needAllocationRequirement 分配需求
     * @param moldingMachineCapacity    分配成型产能
     * @param allocationDays            分配天数
     * @param dayCapacityQty            分配天产能
     * @return
     */
    private static SizeCapacityAllocationDto buildBaseAllocationInfo(SizeCapacityAllocationResultDto needAllocationRequirement, BaseMoldingMachineInfoVo moldingMachineCapacity, Integer allocationDays, Long dayCapacityQty) {
        SizeCapacityAllocationDto allocationInfo = new SizeCapacityAllocationDto();
        //需求信息
        allocationInfo.setProSize(needAllocationRequirement.getProSize());
        allocationInfo.setMouldMethod(needAllocationRequirement.getMouldMethod());
        allocationInfo.setTireFabricNumber(needAllocationRequirement.getTireFabricNumber());
        allocationInfo.setDemandQty(needAllocationRequirement.getDemandQty());
        allocationInfo.setNetDemandQty(needAllocationRequirement.getNetDemandQty());
        allocationInfo.setStockUpDemandQty(needAllocationRequirement.getStockUpDemandQty());
        //分配信息
        allocationInfo.setAllocationDay(allocationDays);
        allocationInfo.setDayCapacity(dayCapacityQty);
        allocationInfo.setDayMaxMouldQty(moldingMachineCapacity.getDayMaxMouldQty());
        allocationInfo.setMoldingMachineCode(moldingMachineCapacity.getMoldingMachineCode());
        allocationInfo.setMoldingMachineClsType(moldingMachineCapacity.getMoldingMachineClsType());
        allocationInfo.setMoldingMachineClsName(moldingMachineCapacity.getMoldingMachineClsName());
        return allocationInfo;
    }

    /**
     * 构建成型产能--分配信息对象
     * 实际分配量不一定 = 分配天数 * 天产能
     *
     * @param needAllocationRequirement 分配需求信息
     * @param allocationDays            分配天数
     * @param dayCapacityQty            天产能
     * @param realAllocationQty         实际分配量
     * @return
     */
    private static MoldingMachineAllocationInfoVo buildMoldingMachineAllocationInfo(SizeCapacityAllocationResultDto needAllocationRequirement, Integer allocationDays, Long dayCapacityQty, Long realAllocationQty) {
        MoldingMachineAllocationInfoVo allocationInfo = new MoldingMachineAllocationInfoVo();
        //寸口、工装类别、成型法、胎体布层级
        allocationInfo.setProSize(needAllocationRequirement.getProSize());
        allocationInfo.setWorkWearType(needAllocationRequirement.getWorkWearType());
        allocationInfo.setMouldMethod(needAllocationRequirement.getMouldMethod());
        allocationInfo.setTireFabricNumber(needAllocationRequirement.getTireFabricNumber());
        allocationInfo.setAllocationDays(allocationDays);
        allocationInfo.setProSizeQuotaQty(dayCapacityQty);
        allocationInfo.setAllocationQty(realAllocationQty);
        return allocationInfo;
    }

    /**
     * 整台挤占处理
     *
     * @param param                     产能分配参数
     * @param moldingMachineCapacity    成型产能信息对象
     * @param allocationDays            分配天数
     * @param globalRequirementMap      所有需求信息对象
     * @param needAllocationRequirement 需要调整的需求信息对象
     * @param requirementQty            需求量
     * @param dayCapacityQty            对应天产能(扣除消耗)
     */
    private static void wholeOccupyHandler(SizeCapacityParamVo param, BaseMoldingMachineInfoVo moldingMachineCapacity, Integer allocationDays, Map<String, SizeCapacityAllocationResultDto> globalRequirementMap, SizeCapacityAllocationResultDto needAllocationRequirement, Long requirementQty, Long dayCapacityQty) {
        //整台占用
        List<MoldingMachineAllocationInfoVo> proSizeRequirementList = moldingMachineCapacity.getProSizeRequirementList();
        moldingMachineCapacity.setProSizeRequirementList(new ArrayList<>());
        moldingMachineCapacity.setProSizeList(new ArrayList<>());
        updateAllocationInfo(param, moldingMachineCapacity, needAllocationRequirement, requirementQty, dayCapacityQty, allocationDays);
        moldingMachineCapacity.setLeftOverCapacityDays(BigDecimal.ZERO.intValue());
        moldingMachineCapacity.setFinishAllocation(true);
        if (CollectionUtils.isEmpty(proSizeRequirementList)) {
            return;
        }
        //需求释放--需要补上挤占的需求
        releaseCapacity(proSizeRequirementList, moldingMachineCapacity, globalRequirementMap);
        return;
    }

    /**
     * 部分挤占处理
     *
     * @param param                     产能分配参数
     * @param moldingMachineCapacity    成型产能信息对象
     * @param allocationDays            需要占用的天数
     * @param globalRequirementMap      全部需求信息对象
     * @param needAllocationRequirement 需要分配需求信息对象
     * @param requirementQty            需求量
     * @param dayCapacityQty            寸口天产能(扣除消耗)
     */
    private static void partialOccupyHandler(SizeCapacityParamVo param, BaseMoldingMachineInfoVo moldingMachineCapacity, Integer allocationDays, Map<String, SizeCapacityAllocationResultDto> globalRequirementMap, SizeCapacityAllocationResultDto needAllocationRequirement, Long requirementQty, Long dayCapacityQty) {
        if (null == needAllocationRequirement || null == moldingMachineCapacity) {
            return;
        }
        List<MoldingMachineAllocationInfoVo> proSizeRequirementList = moldingMachineCapacity.getProSizeRequirementList();
        if (CollectionUtils.isEmpty(proSizeRequirementList)) {
            return;
        }
        Integer maxSize = proSizeRequirementList.size();
        Integer leftOverDays = moldingMachineCapacity.getLeftOverCapacityDays();
        //需挤占出来的天数
        Integer occupyDays = allocationDays - leftOverDays;
        List<MoldingMachineAllocationInfoVo> occupyList = new ArrayList<>();
        //从最后一个开始挤占，需求释放
        for (int index = maxSize - 1; index >= 0; index--) {
            if (occupyDays <= BigDecimal.ZERO.intValue()) {
                break;
            }
            MoldingMachineAllocationInfoVo occupyInfo = proSizeRequirementList.get(index);
            Integer allocatedDays = occupyInfo.getAllocationDays();
            if (allocatedDays <= BigDecimal.ZERO.longValue()) {
                continue;
            }
            if (allocatedDays <= occupyDays) {
                occupyDays = occupyDays - allocatedDays;
                occupyInfo.setAllocationDays(BigDecimal.ZERO.intValue());
                occupyList.add(occupyInfo);
                occupyInfo.setAllocationQty(BigDecimal.ZERO.longValue());
            } else {
                //构建挤占信息
                MoldingMachineAllocationInfoVo realOccupyInfo = buildOccupyInfo(occupyInfo, occupyDays);
                occupyList.add(realOccupyInfo);
                //更新原分配信息
                Long dayCapacity = occupyInfo.getProSizeQuotaQty();
                Long occupyQty = dayCapacity * occupyDays;
                Long allocatedQty = occupyInfo.getAllocationQty();
                occupyInfo.setAllocationDays(allocatedDays - occupyDays);
                occupyDays = BigDecimal.ZERO.intValue();
                if (allocatedQty <= occupyQty) {
                    occupyInfo.setAllocationQty(BigDecimal.ZERO.longValue());
                } else {
                    occupyInfo.setAllocationQty(allocatedQty - occupyQty);
                }
            }
        }
        //过滤掉分配数为零的数据
        List<MoldingMachineAllocationInfoVo> afterOccupyInfoList = proSizeRequirementList.stream().filter(allocatedInfo -> allocatedInfo.getAllocationDays() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        moldingMachineCapacity.setProSizeRequirementList(afterOccupyInfoList);
        //寸口重新设置
        List<BigDecimal> proSizeList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(afterOccupyInfoList)) {
            afterOccupyInfoList.stream().forEach(newAllocationInfo -> {
                proSizeList.add(newAllocationInfo.getProSize());
            });
        }
        moldingMachineCapacity.setProSizeList(proSizeList);
        updateAllocationInfo(param, moldingMachineCapacity, needAllocationRequirement, requirementQty, dayCapacityQty, allocationDays);
        //需要补上挤占的需求
        addOccupyCapacityByRequirement(occupyList, globalRequirementMap);
        moldingMachineCapacity.setLeftOverCapacityDays(BigDecimal.ZERO.intValue());
        moldingMachineCapacity.setFinishAllocation(true);
        return;
    }

    /**
     * 对成型产能对象释放已分配的需求信息
     *
     * @param releaseRequirementList 需要释放的分配需求信息集合
     * @param moldingMachineCapacity 成型产能对象
     * @param globalRequirementMap   所有需求信息
     */
    private static void releaseCapacity(List<MoldingMachineAllocationInfoVo> releaseRequirementList, BaseMoldingMachineInfoVo moldingMachineCapacity, Map<String, SizeCapacityAllocationResultDto> globalRequirementMap) {
        if (CollectionUtils.isEmpty(releaseRequirementList) || null == moldingMachineCapacity) {
            return;
        }
        releaseRequirementList.stream().forEach(singleAllocation -> {
            String requirementKey = singleAllocation.getSizeCapacityGroupKey();
            SizeCapacityAllocationResultDto requirementInfo = globalRequirementMap.get(requirementKey);
            if (null == requirementInfo) {
                return;
            }
            requirementInfo.addCrowdOut(singleAllocation);
            rejectAllAllocationByMoldingMachineInfo(requirementInfo, moldingMachineCapacity);
        });
    }

    /**
     * 对挤占的需求，加入原有需求的剩余量中
     *
     * @param releaseRequirementList 需要释放的分配需求信息集合
     * @param globalRequirementMap   所有需求信息
     */
    private static void addOccupyCapacityByRequirement(List<MoldingMachineAllocationInfoVo> releaseRequirementList, Map<String, SizeCapacityAllocationResultDto> globalRequirementMap) {
        if (CollectionUtils.isEmpty(releaseRequirementList)) {
            return;
        }
        releaseRequirementList.stream().forEach(singleAllocation -> {
            String requirementKey = singleAllocation.getSizeCapacityGroupKey();
            SizeCapacityAllocationResultDto requirementInfo = globalRequirementMap.get(requirementKey);
            if (null == requirementInfo) {
                return;
            }
            requirementInfo.addCrowdOut(singleAllocation);
        });
    }

    /**
     * 在needAllocationRequirement已占用的成型产能中全部释放moldingMachineCapacity
     *
     * @param needAllocationRequirement 需求信息对象
     * @param moldingMachineCapacity    成型产能对象
     */
    private static void rejectAllAllocationByMoldingMachineInfo(SizeCapacityAllocationResultDto needAllocationRequirement, BaseMoldingMachineInfoVo moldingMachineCapacity) {
        if (null == needAllocationRequirement || null == moldingMachineCapacity) {
            return;
        }
        List<SizeCapacityAllocationDto> allocationList = needAllocationRequirement.getAllocationList();
        if (CollectionUtils.isEmpty(allocationList)) {
            return;
        }
        List<SizeCapacityAllocationDto> resetAllocationList = new ArrayList<>();
        allocationList.stream().forEach(singleAllocation -> {
            if (singleAllocation.getMoldingMachineCode().equals(moldingMachineCapacity.getMoldingMachineCode())) {
                return;
            }
            resetAllocationList.add(singleAllocation);
        });
        needAllocationRequirement.setAllocationList(resetAllocationList);
    }

    /**
     * 构建挤占的需求量信息
     *
     * @param originAllocationInfo 原分配信息
     * @param occupyDays           挤占天数
     */
    private static MoldingMachineAllocationInfoVo buildOccupyInfo(MoldingMachineAllocationInfoVo originAllocationInfo, Integer occupyDays) {
        MoldingMachineAllocationInfoVo realOccupyInfo = new MoldingMachineAllocationInfoVo();
        BeanUtils.copyProperties(originAllocationInfo, realOccupyInfo);
        Long dayCapacity = originAllocationInfo.getProSizeQuotaQty();
        Long occupyQty = dayCapacity * occupyDays;
        Long allocatedQty = originAllocationInfo.getAllocationQty();
        realOccupyInfo.setAllocationQty(Math.min(occupyQty, allocatedQty));
        return realOccupyInfo;
    }

    private SizeCapacityAllocationUtils() {

    }
}
