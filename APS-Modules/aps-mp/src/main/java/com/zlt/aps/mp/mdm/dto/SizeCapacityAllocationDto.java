package com.zlt.aps.mp.mdm.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 寸口产能配置过渡对象
 * 分配到成型机
 *
 * @author ZLT
 * @date 20250709
 */
@Data
public class SizeCapacityAllocationDto implements Serializable {
    /**
     * 成型机编号
     */
    @ApiModelProperty(value = "成型机编号", name = "moldingMachineCode")
    private String moldingMachineCode;
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
     * 寸口（保留2位小数）
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private BigDecimal proSize;

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
     * 天产能
     */
    @ApiModelProperty(value = "天产能", name = "dayCapacity")
    private Long dayCapacity;
    /**
     * 天最大模具数，配比 * 2
     */
    @ApiModelProperty(value = "天最大模具数", name = "dayMaxMouldQty")
    private Integer dayMaxMouldQty;
    /**
     * 分配天数
     */
    @ApiModelProperty(value = "分配天数", name = "allocationDay")
    private Integer allocationDay;
    /**
     * 下一寸口，第二轮分配时，赋值，不一定有值
     */
    private BigDecimal nextProSize;
    /**
     * 剩余产能量--只在第二轮分配使用
     */
    private Long leftOverCapacityQty;
    /**
     * 剩余最大天数--只在第二轮分配使用
     */
    private Integer leftOverDays;
    /**
     * 下一寸口天产能，第二轮分配时赋值，不一定有值
     */
    private Long nextProSizeDayCapacity;
    /**
     * 下一配置的key，用以构建树级结构
     */
    private String nextGroupKey;
    /**
     * 父级配置的key，用以构建树级结构
     */
    private String superGroupKey;
    /**
     * 自身Key，构建树形结构使用
     */
    private String oneselfKey;

    /**
     * 分组Key
     * 类型|*|天产能
     *
     * @return
     */
    public String getGroupKey() {
        String groupKey = "%s|*|%s";
        return String.format(groupKey, moldingMachineClsType, dayCapacity);
    }

    /**
     * 是否独立配置，不合并
     *
     * @return
     */
    public boolean isAlone() {
        return StringUtils.isNotBlank(nextGroupKey) || StringUtils.isNotBlank(superGroupKey);
    }

    public SizeCapacityAllocationDto() {
    }

    /**
     * 构造函数，基础数据
     *
     * @param proSize          寸口
     * @param mouldMethod      成型法
     * @param tireFabricNumber 胎体布层级
     * @param demandQty        总需求
     * @param netDemandQty     净需求
     * @param stockUpDemandQty 备货需求
     */
    public SizeCapacityAllocationDto(BigDecimal proSize, String mouldMethod, Integer tireFabricNumber, Long demandQty, Long netDemandQty, Long stockUpDemandQty) {
        this.proSize = proSize;
        this.mouldMethod = mouldMethod;
        this.tireFabricNumber = tireFabricNumber;
        this.demandQty = demandQty;
        this.netDemandQty = netDemandQty;
        this.stockUpDemandQty = stockUpDemandQty;
    }
}
