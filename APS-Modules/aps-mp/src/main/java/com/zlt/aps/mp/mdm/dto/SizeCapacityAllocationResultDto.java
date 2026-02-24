package com.zlt.aps.mp.mdm.dto;

import com.zlt.aps.enums.FormingMethodTypeEnum;
import com.zlt.aps.enums.WorkWearTypeEnum;
import com.zlt.aps.monthplan.api.domain.vo.MoldingMachineAllocationInfoVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 寸口产能需求分配结果过渡对象
 *
 * @author ZLT
 * @date 20250709
 */
@Data
public class SizeCapacityAllocationResultDto implements Serializable {
    /**
     * 寸口（保留2位小数）
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private BigDecimal proSize;
    /**
     * 工装类别
     */
    private String workWearType;

    /**
     * 成型法 取数据字典molding_method的编码
     */
    @ApiModelProperty(value = "成型法 取数据字典molding_method的编码", name = "mouldMethod")
    private String mouldMethod;
    /**
     * 胎体布层级
     */
    @ApiModelProperty(value = "胎体布层级", name = "tireFabricNumber")
    private Integer tireFabricNumber;
    /**
     * 总需求
     */
    @ApiModelProperty(value = "总需求", name = "demandQty")
    private Long demandQty;

    /**
     * 净需求
     */
    @ApiModelProperty(value = "净需求", name = "netDemandQty")
    private Long netDemandQty;

    /**
     * 备货需求
     */
    @ApiModelProperty(value = "备货需求", name = "stockUpDemandQty")
    private Long stockUpDemandQty;
    /**
     * 剩余待分配量
     */
    private Long leftOverQty;
    /**
     * 标记是否分配完成
     */
    private Boolean finishAllocation;
    /**
     * 分配产能列表
     */
    private List<SizeCapacityAllocationDto> allocationList;

    /**
     * 获取寸口产能分组key
     * 寸口|*|工装类别|*|成型法|*|胎体布层级
     *
     * @return
     */
    public String getSizeCapacityGroupKey() {
        String groupKey = "%s|*|%s|*|%s|*|%s";
        return String.format(groupKey, proSize, workWearType, mouldMethod, tireFabricNumber);
    }

    /**
     * 是否还需要进行分配
     * 标记没有分配完成，且还有剩余需分配量
     *
     * @return
     */
    public boolean isNeedAllocation() {
        if (finishAllocation) {
            return false;
        }
        return leftOverQty > BigDecimal.ZERO.longValue();
    }

    /**
     * 是否为单层在产寸口还需要分配的需求
     * 1、单层胎体布
     * 2、还有剩余量
     * 3、寸口在在产寸口集合中
     *
     * @param productionProSizeSet 在产寸口集合
     * @return
     */
    public boolean isNeedAllocationSingleTireFabricNumber(Set<BigDecimal> productionProSizeSet) {
        if (tireFabricNumber != BigDecimal.ONE.intValue()) {
            return false;
        }
        if (leftOverQty <= BigDecimal.ZERO.longValue()) {
            return false;
        }
        if (!productionProSizeSet.contains(proSize)) {
            return false;
        }
        return true;
    }

    /**
     * 是否需要下一次分配
     * 成型法 = mouldMethodType
     * 胎体层级 = tireFabricNumber
     * 寸口小于proSize
     * 还有剩余量
     *
     * @param mouldMethodType
     * @param tireFabricNumber
     * @param proSize
     * @return
     */
    public boolean isNeedNextAllocation(FormingMethodTypeEnum mouldMethodType, Integer tireFabricNumber, BigDecimal proSize) {
        if (finishAllocation) {
            return false;
        }
        if (!mouldMethod.equals(mouldMethodType.getMethodValue())) {
            return false;
        }
        if (!this.tireFabricNumber.equals(tireFabricNumber)) {
            return false;
        }
        if (this.proSize.compareTo(proSize) >= BigDecimal.ZERO.intValue()) {
            return false;
        }
        return leftOverQty > BigDecimal.ZERO.longValue();
    }

    /**
     * 增加挤占分配量
     * 已分配完全被挤占
     *
     * @param crowdOutInfo 挤占出来的分配信息
     */
    public void addCrowdOut(MoldingMachineAllocationInfoVo crowdOutInfo) {
        if (null == leftOverQty) {
            leftOverQty = BigDecimal.ZERO.longValue();
        }
        leftOverQty = leftOverQty + crowdOutInfo.getAllocationQty();
    }

    /**
     * 是否为二次法18寸需求
     *
     * @return
     */
    public boolean isSpecialRestrictions() {
        if (!FormingMethodTypeEnum.TWO_STAGE_TIRE.getMethodValue().equals(mouldMethod)) {
            return false;
        }
        if (WorkWearTypeEnum.PRO_SIZE_18.equals(proSize)) {
            return true;
        }
        return false;
    }

    /**
     * 增加成型产能分配信息
     *
     * @param addAllocationInfo 需增加的产能分配信息
     */
    public void addAllocationInfo(SizeCapacityAllocationDto addAllocationInfo) {
        if (null == allocationList) {
            allocationList = new ArrayList<>();
        }
        allocationList.add(addAllocationInfo);
    }

    /**
     * 判断是否还需要进行成型产能分配
     *
     * @param tireFabricNumber 胎体布层级
     * @param proSize          寸口
     * @param mouldMethod      成型法
     * @return
     */
    public boolean isNeedAllocation(Integer tireFabricNumber, BigDecimal proSize, String mouldMethod) {
        if (finishAllocation) {
            return false;
        }
        if (!this.tireFabricNumber.equals(tireFabricNumber)) {
            return false;
        }
        if (!this.mouldMethod.equals(mouldMethod)) {
            return false;
        }
        if (!this.proSize.equals(proSize)) {
            return false;
        }
        if (leftOverQty <= BigDecimal.ZERO.longValue()) {
            return false;
        }
        return true;
    }

    /**
     * 构建空分配信息的需求分配对象
     *
     * @param proSize          寸口
     * @param workWearType     工装类别
     * @param mouldMethod      成型法
     * @param tireFabricNumber 胎体布层级
     * @param demandQty        总需求
     * @param netDemandQty     净需求
     * @param stockUpDemandQty 备货需求
     * @return
     */
    public static SizeCapacityAllocationResultDto buildEmptyAllocationInfo(BigDecimal proSize, String workWearType, String mouldMethod, Integer tireFabricNumber, Long demandQty, Long netDemandQty, Long stockUpDemandQty) {
        SizeCapacityAllocationResultDto emptyResult = new SizeCapacityAllocationResultDto();
        emptyResult.setProSize(proSize);
        emptyResult.setWorkWearType(workWearType);
        emptyResult.setMouldMethod(mouldMethod);
        emptyResult.setTireFabricNumber(tireFabricNumber);
        emptyResult.setDemandQty(demandQty);
        emptyResult.setNetDemandQty(netDemandQty);
        emptyResult.setStockUpDemandQty(stockUpDemandQty);
        //分配信息
        emptyResult.setLeftOverQty(demandQty);
        emptyResult.setAllocationList(new ArrayList<>());
        emptyResult.setFinishAllocation(false);
        return emptyResult;
    }
}
