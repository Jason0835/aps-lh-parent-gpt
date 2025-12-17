package com.zlt.aps.factory.domain.vo;

import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.CxMachineFixedPriorityEnum;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
        if (hasFixed()) {
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
        Set<String> fixedMaterialCodeSet = Stream.of(disableMaterialCode.split(StringConstant.COMMA)).collect(Collectors.toSet());
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
}
