package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成型机产能信息及在产信息
 *
 * @author ZLT
 * @date 20250708
 */
@Data
public class BaseMoldingMachineInfoVo extends BaseMoldingMachineCapacityVo {
    /**
     * 成型机编号
     */
    @ApiModelProperty(value = "成型机编号", name = "moldingMachineCode")
    private String moldingMachineCode;

    /**
     * 成型法:来源于数据字典MACHINE_TYPE
     */
    @ApiModelProperty(value = "成型法:来源于数据字典MACHINE_TYPE", name = "mouldMethod")
    private String mouldMethod;
    /**
     * 成型机类型
     */
    @ApiModelProperty(value = "成型机类型", name = "moldingMachineClsType")
    private Long moldingMachineClsType;
    /**
     * 成型机类型名称
     */
    @ApiModelProperty(value = "成型机类型名称", name = "moldingMachineClsName")
    private String moldingMachineClsName;
    /**
     * 胎休布类型
     */
    @ApiModelProperty(value = "胎休布类型", name = "carcassClothType")
    private Integer carcassClothType;
    /**
     * 寸口天产能定额
     */
    @ApiModelProperty(value = "寸口天产能定额", name = "proSizeQuotaQtyMap")
    private Map<BigDecimal, Long> proSizeQuotaQtyMap;

    /**
     * 成型机寸口对应硫化机配比
     */
    @ApiModelProperty(value = "成型机寸口对应硫化机配比", name = "moldingMachineProSizeSulfurizationMachineMap")
    private Map<BigDecimal, BigDecimal> moldingMachineProSizeSulfurizationMachineMap;

    /**
     * 当前排产寸口
     */
    @ApiModelProperty(value = "当前排产寸口", name = "currentProSize")
    private BigDecimal currentProSize;
    /**
     * 当前排产生胎个数
     */
    @ApiModelProperty(value = "当前排产生胎个数", name = "currentEmbryoCodeNumber")
    private Integer currentEmbryoCodeNumber;
    /**
     * 标记18寸二次法大鼓特殊限定--0 不特殊 1 特殊
     */
    private Integer specialRestrictions;
    /**
     * 确认寸口后的天产能定额，每次重新分配前会重新赋值
     */
    private Long proSizeQuotaQty;
    /**
     * 确认寸口后对应的天最大模具数，配比 * 2，每次重新分配前会重新赋值
     */
    private Integer dayMaxMouldQty;
    /**
     * 分配的寸口
     */
    private List<BigDecimal> proSizeList;
    /**
     * 已分配的寸口需求信息
     */
    private List<MoldingMachineAllocationInfoVo> proSizeRequirementList;
    /**
     * 剩余产能天数--分配时使用
     */
    private Integer leftOverCapacityDays;
    /**
     * 需求所需要的天数
     */
    private Integer requirementNeedDays;
    /**
     * 标记分配完成--不再参与分配
     */
    private Boolean finishAllocation;
    /**
     * 一次法
     */
    private final String SINGLE_STAGE_TIRE = "1";
    /**
     * 最大可分配寸口
     */
    private static final Integer MAX_ALLOCATION_PRO_SIZE = 2;

    /**
     * 是否有下一个
     *
     * @return
     */
    public boolean isNext() {
        if (CollectionUtils.isEmpty(proSizeList)) {
            return false;
        }
        if (proSizeList.size() > BigDecimal.ONE.intValue()) {
            return true;
        }
        return false;
    }

    /**
     * 获取差值，第二轮分配使用
     *
     * @return
     */
    public Integer getDifferenceDays() {
        if (null == requirementNeedDays || null == leftOverCapacityDays) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(requirementNeedDays - leftOverCapacityDays);
    }

    /**
     * 还有剩余的成型产能
     *
     * @return
     */
    public boolean isHasCapacity() {
        if (null == leftOverCapacityDays) {
            return false;
        }
        return leftOverCapacityDays > BigDecimal.ZERO.intValue();
    }

    /**
     * 获取已分配的寸口
     *
     * @return
     */
    public Set<BigDecimal> getAssignedProSize() {
        if (CollectionUtils.isEmpty(proSizeList)) {
            return Collections.emptySet();
        }
        return proSizeList.stream().collect(Collectors.toSet());
    }

    /**
     * 是否分配的是两个寸口或是两种胎体布层级
     * 一次法：因一次法产能都是多层，故而对一次法的单段条件：只有一个寸口
     * 二次法：二次法产能是区分单层、多层，故而对二次法的单段条件：只有一个寸口且只有一个胎体层级
     *
     * @return
     */
    public boolean isAllocationTwo() {
        if (CollectionUtils.isEmpty(proSizeRequirementList)) {
            return false;
        }
        Integer two = BigDecimal.ONE.intValue() + BigDecimal.ONE.intValue();
        Set<BigDecimal> proSizeSet = proSizeRequirementList.stream().map(MoldingMachineAllocationInfoVo::getProSize).collect(Collectors.toSet());
        //一次法
        if (SINGLE_STAGE_TIRE.equals(mouldMethod)) {
            if (CollectionUtils.isEmpty(proSizeSet)) {
                return false;
            }
            return proSizeSet.size() == two;
        }
        //二次法
        Set<Integer> carcassClothTypeSet = proSizeRequirementList.stream().map(MoldingMachineAllocationInfoVo::getTireFabricNumber).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(proSizeSet) && CollectionUtils.isEmpty(carcassClothTypeSet)) {
            return false;
        }
        boolean sizeTwo = false;
        if (!CollectionUtils.isEmpty(proSizeSet)) {
            sizeTwo = proSizeSet.size() == two;
        }
        boolean carcassClothTypeTow = false;
        if (!CollectionUtils.isEmpty(carcassClothTypeSet)) {
            carcassClothTypeTow = carcassClothTypeSet.size() == two;
        }
        return sizeTwo || carcassClothTypeTow;
    }

    /**
     * 获取分配寸口中最小的寸口
     * 如果分配已经存在两个寸口，则返回Long的最大值
     * 如果没有分配寸口，则为零
     * 否则就是一个寸口
     *
     * @return
     */
    public BigDecimal getMinAssignedProSize() {
        Set<BigDecimal> allocationProSizeSet = getAssignedProSize();
        if (!CollectionUtils.isEmpty(allocationProSizeSet) && allocationProSizeSet.size() > BigDecimal.ONE.intValue()) {
            return BigDecimal.valueOf(Long.MAX_VALUE);
        }
        if (CollectionUtils.isEmpty(allocationProSizeSet)) {
            return BigDecimal.ZERO;
        }
        return new ArrayList<>(allocationProSizeSet).get(BigDecimal.ZERO.intValue());
    }

    /**
     * 判断能否参与挤占产能分配
     * 1、分配完成的不参与分配挤占
     * 2、多层需求只能匹配多层成型产能
     * 3、可产对应寸口
     * 4、已经分配过两个寸口则不再参与分配挤占
     * 5、分配寸口高于proSize的不参与分配挤占
     *
     * @param proSize          需产寸口
     * @param tireFabricNumber 胎体层级
     * @return
     */
    public Boolean isCrowdOutAllocation(BigDecimal proSize, Integer tireFabricNumber) {
        if (finishAllocation) {
            return false;
        }
        //多层级
        if (tireFabricNumber > BigDecimal.ONE.intValue() && !carcassClothType.equals(tireFabricNumber)) {
            return false;
        }
        //不生产寸口
        if (CollectionUtils.isEmpty(proSizeQuotaQtyMap) || !proSizeQuotaQtyMap.containsKey(proSize)) {
            return false;
        }
        //已经切换过寸口的，不再参与分配，大于proSize则不参与挤占分配，不能挤占寸口大的
        BigDecimal minAssignedProSize = getMinAssignedProSize();
        if (minAssignedProSize.compareTo(proSize) >= BigDecimal.ZERO.intValue()) {
            return false;
        }
        return true;
    }

    /**
     * 判断能否参与再次产能分配
     * 1、分配完成的不参与分配
     * 2、多层需求只能匹配多层成型产能
     * 3、寸口须有产能
     * 4、已经分配过两个寸口则不再参与分配
     *
     * @param proSize          需产寸口
     * @param tireFabricNumber 胎体层级
     * @return
     */
    public Boolean isNoCrowdOutAllocation(BigDecimal proSize, Integer tireFabricNumber) {
        if (finishAllocation) {
            return false;
        }
        //多层级
        if (tireFabricNumber > BigDecimal.ONE.intValue() && !carcassClothType.equals(tireFabricNumber)) {
            return false;
        }
        //不生产寸口
        if (CollectionUtils.isEmpty(proSizeQuotaQtyMap) || !proSizeQuotaQtyMap.containsKey(proSize)) {
            return false;
        }
        Set<BigDecimal> allocationProSizeSet = getAssignedProSize();
        if (CollectionUtils.isEmpty(allocationProSizeSet)) {
            allocationProSizeSet = new HashSet<>();
        }
        allocationProSizeSet.add(proSize);
        if (allocationProSizeSet.size() > MAX_ALLOCATION_PRO_SIZE) {
            return false;
        }
        return true;
    }

    /**
     * 是否首次分配寸口
     * 已分配完成，则不是首次分配
     *
     * @param proSize 寸口
     * @return
     */
    public boolean isFirstAllocation(BigDecimal proSize) {
        if (finishAllocation) {
            return false;
        }
        Set<BigDecimal> allocationProSizeSet = getAssignedProSize();
        return !allocationProSizeSet.contains(proSize);
    }

    /**
     * 判断不可切换的成型产能
     *
     * @param limitDays 限定条件，剩余天数小于限定值limitDays时，不进行切换寸口
     * @return
     */
    public boolean isNoSwitchProSize(Integer limitDays) {
        if (finishAllocation) {
            return false;
        }
        if (CollectionUtils.isEmpty(proSizeRequirementList)) {
            return false;
        }
        if (leftOverCapacityDays <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        if (leftOverCapacityDays >= limitDays) {
            return false;
        }
        return true;
    }

    /**
     * 增加成型产能分配的需求信息
     *
     * @param moldingMachineAllocationInfo
     */
    public void addAllocationRequirementInfo(MoldingMachineAllocationInfoVo moldingMachineAllocationInfo) {
        if (null == moldingMachineAllocationInfo) {
            return;
        }
        if (null == proSizeList) {
            proSizeList = new ArrayList<>();
        }
        proSizeList.add(moldingMachineAllocationInfo.getProSize());
        if (null == proSizeRequirementList) {
            proSizeRequirementList = new ArrayList<>();
        }
        proSizeRequirementList.add(moldingMachineAllocationInfo);
    }

    /**
     * 成型产能是否严格匹配 成型法和寸口
     *
     * @param mouldMethod      成型法
     * @param tireFabricNumber 胎体层级
     * @return
     */
    public boolean moldingMachineByStrictMatch(String mouldMethod, Integer tireFabricNumber) {
        if (StringUtils.isBlank(mouldMethod) || null == tireFabricNumber) {
            return false;
        }
        if (!mouldMethod.equals(this.mouldMethod)) {
            return false;
        }
        return tireFabricNumber.equals(this.carcassClothType);
    }

    /**
     * 获取寸口产能分组key
     * 在产寸口|*|成型法|*|胎体布层级
     *
     * @return
     */
    public String getSizeCapacityGroupKey() {
        if (null == currentProSize) {
            return null;
        }
        String groupKey = "%s|*|%s|*|%s";
        return String.format(groupKey, currentProSize, mouldMethod, carcassClothType);
    }
}
