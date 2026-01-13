package com.zlt.aps.factory.domain.vo;

import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.CxMachineFixedPriorityEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxLhProductionHelper;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.dto.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.handler.ContinuousProductionDayHandler;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 成型基础信息对象
 *
 * @author ZLT
 * @date 20251215
 */
@Data
public class CxMachineBaseInfoVo implements Serializable {

    /**
     * 工厂编号
     */
    private String factoryCode;

    /**
     * 成型机台
     */
    private String cxMachineCode;

    /**
     * 成型机型-品牌
     */
    private String cxMachineBrandCode;

    /**
     * 类型 机械等
     */
    private String cxMachineTypeCode;
    /**
     * 是否零度供料架
     */
    private String isZeroRack;

    /**
     * 硫化上限
     */
    private Integer lhMachineMaxQty;

    /**
     * 固定结构1
     */
    private String fixedStructure1;

    /**
     * 固定结构2
     */
    private String fixedStructure2;

    /**
     * 固定结构3
     */
    private String fixedStructure3;

    /**
     * 固定SKU
     */
    private String fixedMaterialCode;

    /**
     * 不可作业结构
     */
    private String disableStructure;

    /**
     * 不可作业SKU
     */
    private String disableMaterialCode;

    /**
     * 停产日(包含维修及全局停产日)信息
     */
    private Set<Integer> stopDayInfo;
    /**
     * 最大可排产天数
     */
    private Integer maxProductionDays;

    /**
     * 剩余可分配天数
     */
    private Integer remainingDays;
    /**
     * 非续作结构使用-当前硫化配比
     */
    private Integer ratio;
    /**
     * 针对计划的固定优先级
     */
    private Integer fixedPriority;
    /**
     * 分配的分组计划集合(TBR按结构)
     */
    private List<CxMachineAllocationPlanHelper> allocationList;
    /**
     * 计划是否同规格
     */
    private String sameSpecifications;
    /**
     * 计划是否同英寸
     */
    private String sameProSize;
    /**
     * 计划是否断面宽范围
     */
    private String sectionWidthCondition;
    /**
     * 成型硫化配比最后一天排产分组信息
     */
    private Map<Integer, CxLhProductionHelper> cxLhRatioMap;
    /**
     * 日排产限制--只在第一轮按机台分配中使用-机台反选和计划挑选机台
     */
    private Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo;

    /**
     * 获取剩余产能，以剩余天数*此时的硫化配比
     * 用于判断后续剩余产能判断
     *
     * @return
     */
    public Integer getRemainCapacity() {
        Integer currentRemainDays = remainingDays;
        if (null == currentRemainDays) {
            currentRemainDays = BigDecimal.ZERO.intValue();
        }
        Integer currentRatio = ratio;
        if (null == currentRatio) {
            currentRatio = BigDecimal.ZERO.intValue();
        }
        return currentRemainDays * currentRatio;
    }

    /**
     * 获取固定信息的优先级
     * 固定SKU的优先级最高，其次是固定结构1,
     * 再次固定结构2，最后固定结构3
     *
     * @return
     */
    public Integer getFixedPriorityValue(ProductionPlanGroupInfo groupPlanInfo) {
        if (null == groupPlanInfo) {
            return CxMachineFixedPriorityEnum.DEFAULT.getPriorityValue();
        }
        //无固定配置
        if (!hasFixed()) {
            return CxMachineFixedPriorityEnum.DEFAULT.getPriorityValue();
        }
        String structureName = groupPlanInfo.getGroupName();
        Integer fixedPriorityValue = getFixedStructurePriority(fixedStructure1, CxMachineFixedPriorityEnum.FIXED_STRUCTURE_FIRST, structureName).getPriorityValue();
        Integer fixedPriorityValue2 = getFixedStructurePriority(fixedStructure2, CxMachineFixedPriorityEnum.FIXED_STRUCTURE_SECOND, structureName).getPriorityValue();
        fixedPriorityValue = Math.min(fixedPriorityValue, fixedPriorityValue2);
        Integer fixedPriorityValue3 = getFixedStructurePriority(fixedStructure3, CxMachineFixedPriorityEnum.FIXED_STRUCTURE_THIRD, structureName).getPriorityValue();
        fixedPriorityValue = Math.min(fixedPriorityValue, fixedPriorityValue3);
        Integer fixedPrioritySku = getFixedMaterialCodePriority(groupPlanInfo).getPriorityValue();
        return Math.min(fixedPriorityValue, fixedPrioritySku);
    }

    /**
     * 判定机台是否为结构指定机台
     * 需要判断 指定结构和指定Sku
     * 先判断指定结构，后判断指定Sku
     *
     * @param groupPlanInfo 结构信息
     * @return
     */
    public boolean hasFixedMachine(ProductionPlanGroupInfo groupPlanInfo) {
        if (null == groupPlanInfo) {
            return false;
        }
        //无固定配置
        if (!hasFixed()) {
            return false;
        }
        //判定结构
        Set<String> fixedStructureSet = new HashSet<>();
        if (StringUtils.isNotBlank(fixedStructure1)) {
            fixedStructureSet.addAll(Stream.of(fixedStructure1.split(StringConstant.COMMA)).collect(Collectors.toSet()));
        }
        if (StringUtils.isNotBlank(fixedStructure2)) {
            fixedStructureSet.addAll(Stream.of(fixedStructure2.split(StringConstant.COMMA)).collect(Collectors.toSet()));
        }
        if (StringUtils.isNotBlank(fixedStructure3)) {
            fixedStructureSet.addAll(Stream.of(fixedStructure3.split(StringConstant.COMMA)).collect(Collectors.toSet()));
        }
        if (fixedStructureSet.contains(groupPlanInfo.getGroupName())) {
            return true;
        }
        //判定Sku
        Set<String> fixedMaterialCodeSet = new HashSet<>();
        if (StringUtils.isNotBlank(fixedMaterialCode)) {
            fixedMaterialCodeSet.addAll(Stream.of(fixedMaterialCode.split(StringConstant.COMMA)).collect(Collectors.toSet()));
        }
        if (CollectionUtils.isEmpty(fixedMaterialCodeSet)) {
            return false;
        }
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return false;
        }
        for (MonthPlanProductionRequirePlanVo singlePlan : groupPlanData) {
            if (fixedMaterialCodeSet.contains(singlePlan.getMaterialCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据固定结构值及优先级，得到其真实优先级
     *
     * @param fixedStructure 固定结构
     * @param fixedPriority  固定优先级
     * @param structureName  结构名
     * @return
     */
    private CxMachineFixedPriorityEnum getFixedStructurePriority(String fixedStructure, CxMachineFixedPriorityEnum fixedPriority, String structureName) {
        if (StringUtils.isBlank(fixedStructure) || StringUtils.isBlank(structureName)) {
            return CxMachineFixedPriorityEnum.DEFAULT;
        }
        Set<String> fixedStructureSet = Stream.of(fixedStructure.split(StringConstant.COMMA)).collect(Collectors.toSet());
        if (fixedStructureSet.contains(structureName)) {
            return fixedPriority;
        }
        return CxMachineFixedPriorityEnum.DEFAULT;
    }

    /**
     * 获取固定SKU的优先级值
     *
     * @param groupPlanInfo
     * @return
     */
    private CxMachineFixedPriorityEnum getFixedMaterialCodePriority(ProductionPlanGroupInfo groupPlanInfo) {
        if (StringUtils.isBlank(fixedMaterialCode)) {
            return CxMachineFixedPriorityEnum.DEFAULT;
        }
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return CxMachineFixedPriorityEnum.DEFAULT;
        }
        Set<String> materialCodePlanSet = groupPlanData.stream().map(MonthPlanProductionRequirePlanVo::getMaterialCode).collect(Collectors.toSet());
        Set<String> fixedMaterialCodeSet = Stream.of(fixedMaterialCode.split(StringConstant.COMMA)).collect(Collectors.toSet());
        for (String materialCode : materialCodePlanSet) {
            if (fixedMaterialCodeSet.contains(materialCode)) {
                return CxMachineFixedPriorityEnum.FIXED_SKU;
            }
        }
        return CxMachineFixedPriorityEnum.DEFAULT;
    }

    /**
     * 是否有固定
     * fixedStructure1,fixedStructure2,fixedStructure3,fixedMaterialCode
     * 都为空，则无固定 = false;
     *
     * @return
     */
    private boolean hasFixed() {
        if (StringUtils.isNotBlank(fixedStructure1)) {
            return true;
        }
        if (StringUtils.isNotBlank(fixedStructure2)) {
            return true;
        }
        if (StringUtils.isNotBlank(fixedStructure3)) {
            return true;
        }
        if (StringUtils.isNotBlank(fixedMaterialCode)) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否为不可作业结构
     * true 表示是不可作业结构
     * false 表示不是不可作业结构
     *
     * @param structureName 结构名
     * @return
     */
    public boolean isNoProductionStructure(String structureName) {
        if (StringUtils.isBlank(structureName)) {
            return false;
        }
        if (StringUtils.isBlank(disableStructure)) {
            return false;
        }
        Set<String> disableStructureSet = Stream.of(disableStructure.split(StringConstant.COMMA)).collect(Collectors.toSet());
        return disableStructureSet.contains(structureName);
    }

    /**
     * 判断是否为不可作业SKU
     * true 表示是不可作业SKU
     * false 表示不是不可作业SKU
     *
     * @param materialCode 物料编码
     * @return
     */
    public boolean isNoProductionMaterial(String materialCode) {
        if (StringUtils.isBlank(materialCode)) {
            return false;
        }
        if (StringUtils.isBlank(disableMaterialCode)) {
            return false;
        }
        Set<String> disableMaterialSet = Stream.of(disableMaterialCode.split(StringConstant.COMMA)).collect(Collectors.toSet());
        return disableMaterialSet.contains(materialCode);
    }

    /**
     * 新增分配的分组计划信息
     *
     * @param addAllocationPlan
     */
    public void addAllocationPlanInfo(CxMachineAllocationPlanHelper addAllocationPlan) {
        if (null == addAllocationPlan) {
            return;
        }
        if (null == allocationList) {
            allocationList = new ArrayList<>();
        }
        allocationList.add(addAllocationPlan);
    }

    /**
     * 获取成型机台下，最早收尾的硫化机台组
     *
     * @return
     */
    public CxLhProductionHelper getEarliestConclusionLhGroup() {
        //获取成型硫化组
        if (CollectionUtils.isEmpty(cxLhRatioMap)) {
            return null;
        }
        List<CxLhProductionHelper> cxLhGroupList = new ArrayList<>(cxLhRatioMap.values());
        //按最后排产日，进行升序排序
        cxLhGroupList.sort(Comparator.comparing(CxLhProductionHelper::getProductionDay).thenComparing(CxLhProductionHelper::getLhGroupNo));
        //取得第一条：即最早收尾的硫化组
        return cxLhGroupList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 根据选择的Sku判断其符合胎胚种类数限制及其上机时间点和排产结束日
     *
     * @param context         排产上下文
     * @param addSkuInfo      需要上机的Sku
     * @param selectedLhGroup 预计选中
     * @return
     */
    public CxLhProductionHelper getCorrectProductionDateRange(Context context, MonthPlanProductionRequirePlanVo addSkuInfo, CxLhProductionHelper selectedLhGroup) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || null == selectedLhGroup) {
            return null;
        }
        String productionEmbryoCode = addSkuInfo.getEmbryoCode();
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        List<GroupPlanCxLhCapacityLimitHelper> hasAddSkuList = dayLimitList.stream().filter(dayLimit -> !dayLimit.isReachLimitByEmbryoCode(productionEmbryoCode)).collect(Collectors.toList());
        //说明达到胎胚种类数限制
        if (CollectionUtils.isEmpty(hasAddSkuList)) {
            return null;
        }
        //拷贝，否则数据丢失
        CxLhProductionHelper newLhGroup = new CxLhProductionHelper();
        BeanUtils.copyProperties(selectedLhGroup, newLhGroup);
        Set<Integer> productionDaySet = hasAddSkuList.stream().map(GroupPlanCxLhCapacityLimitHelper::getDay).collect(Collectors.toSet());
        Set<Integer> resultSet = ContinuousProductionDayHandler.getEarliestContinuousRange(productionDaySet, context.getStopDays());
        List<Integer> sortList = new ArrayList<>(resultSet);
        Collections.sort(sortList);
        int size = sortList.size();
        newLhGroup.updateProductionDateRange(sortList.get(BigDecimal.ZERO.intValue()), sortList.get(size - BigDecimal.ONE.intValue()));
        return newLhGroup;
    }

    /**
     * 设置成型机与当前加入的分组排产计划是否同规格、同花纹、同英寸、断面宽等信息
     *
     * @param addGroupPlan 即将要加入的分组计划
     * @param diffValue    断面宽差值范围
     */
    public void setSameInfoByCurrentGroupPlan(ProductionPlanGroupInfo addGroupPlan, Integer diffValue) {
        //没有排产信息，默认匹配
        if (CollectionUtils.isEmpty(allocationList)) {
            sameSpecifications = YesOrNoEnum.YES.getCode();
            sameProSize = YesOrNoEnum.YES.getCode();
            sectionWidthCondition = YesOrNoEnum.YES.getCode();
            return;
        }
        //取得最后一个分配的分组结构计划
        CxMachineAllocationPlanHelper lastHelper = allocationList.get(allocationList.size() - BigDecimal.ONE.intValue());
        List<MonthPlanProductionRequirePlanVo> realProductionPlanList = lastHelper.getRealProductionPlanList();
        String sameSpecifications = YesOrNoEnum.NO.getCode();
        if (addGroupPlan.hasSameSpecifications(realProductionPlanList)) {
            sameSpecifications = YesOrNoEnum.YES.getCode();
        }
        this.sameSpecifications = sameSpecifications;
        String sameProSize = YesOrNoEnum.NO.getCode();
        if (addGroupPlan.hasSameProSize(realProductionPlanList)) {
            sameProSize = YesOrNoEnum.YES.getCode();
        }
        this.sameProSize = sameProSize;
        String sectionWidthCondition = YesOrNoEnum.NO.getCode();
        if (addGroupPlan.hasSectionWidthCondition(realProductionPlanList, diffValue)) {
            sectionWidthCondition = YesOrNoEnum.YES.getCode();
        }
        this.sectionWidthCondition = sectionWidthCondition;
    }

    /**
     * 获取成型机台当前可分配的起始日
     *
     * @return
     */
    public Integer getAllocationStartDay() {
        if (CollectionUtils.isEmpty(allocationList)) {
            return ProductionConstant.MONTH_START_DAY;
        }
        CxMachineAllocationPlanHelper lastHelper = allocationList.get(allocationList.size() - BigDecimal.ONE.intValue());
        return lastHelper.getEndDay() + BigDecimal.ONE.intValue();
    }
}
