package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SizeCapacityConfiguration.java
 * 描    述：寸口产能配置对象 t_mdm_size_capacity
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-06-04
 */

@Data
@TableName(value = "T_MDM_SIZE_CAPACITY")
@ApiModel(value = "寸口产能配置对象", description = "寸口产能配置对象 ")
public class SizeCapacityConfiguration extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.sizeCapacity.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.sizeCapacity.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.sizeCapacity.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.sizeCapacity.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 寸口（保留2位小数）
     */
    @Excel(name = "ui.data.column.sizeCapacity.proSize", readConverterExp = "保留2位小数")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 工装类别 0 通用 1 大鼓
     */
    @Excel(name = "ui.data.column.sizeCapacity.workWearType")
    @ApiModelProperty(value = "工装类别", name = "workWearType")
    @TableField(value = "WORK_WEAR_TYPE")
    private String workWearType;
    /**
     * 成型法 取数据字典molding_method的编码
     */
    @Excel(name = "ui.data.column.sizeCapacity.mouldMethod")
    @ApiModelProperty(value = "成型法 取数据字典molding_method的编码", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;
    /**
     * 胎休布类型 1 单层 2 多层
     */
    @Excel(name = "ui.data.column.sizeCapacity.carcassClothType")
    @ApiModelProperty(value = "胎休布类型", name = "carcassClothType")
    @TableField(value = "CARCASS_CLOTH_TYPE")
    private Integer carcassClothType;

    /**
     * 总需求量(扣除超出模具产能)
     */
    @Excel(name = "ui.data.column.sizeCapacity.effectiveDemandQty")
    @ApiModelProperty(value = "总需求量", name = "effectiveDemandQty")
    @TableField(value = "EFFECTIVE_DEMAND_QTY")
    private Long effectiveDemandQty;

    /**
     * 净需求量(扣除超出模具产能)
     */
    @Excel(name = "ui.data.column.sizeCapacity.effectiveNetDemandQty")
    @ApiModelProperty(value = "净需求量", name = "effectiveNetDemandQty")
    @TableField(value = "EFFECTIVE_NET_DEMAND_QTY")
    private Long effectiveNetDemandQty;

    /**
     * 备货需求量(扣除超出模具产能)
     */
    @Excel(name = "ui.data.column.sizeCapacity.effectiveStockUpDemandQty")
    @ApiModelProperty(value = "备货需求量", name = "effectiveStockUpDemandQty")
    @TableField(value = "EFFECTIVE_STOCK_UP_DEMAND_QTY")
    private Long effectiveStockUpDemandQty;

    /**
     * 成型机类型ID
     */
    @Excel(name = "ui.data.column.sizeCapacity.moldingMachineClsType")
    @ApiModelProperty(value = "成型机类型名称", name = "moldingMachineClsType")
    @TableField(value = "MOLDING_MACHINE_CLS_TYPE")
    private Long moldingMachineClsType;

    /**
     * 成型机类型名称
     */
    @Excel(name = "ui.data.column.sizeCapacity.moldingMachineClsName")
    @ApiModelProperty(value = "成型机类型名称", name = "moldingMachineClsName")
    @TableField(value = "MOLDING_MACHINE_CLS_NAME")
    private String moldingMachineClsName;
    /**
     * 天产能
     */
    @Excel(name = "ui.data.column.sizeCapacity.dayCapacity")
    @ApiModelProperty(value = "天产能", name = "dayCapacity")
    @TableField(value = "DAY_CAPACITY")
    private Integer dayCapacity;
    /**
     * 天最大模具数
     */
    @ApiModelProperty(value = "天最大模具数", name = "maxMouldQty")
    @TableField(value = "MAX_MOULD_QTY")
    private Integer maxMouldQty;
    /**
     * 下一寸口（保留2位小数）
     */
    @Excel(name = "ui.data.column.sizeCapacity.nextProSize", readConverterExp = "保=留2位小数")
    @ApiModelProperty(value = "下一寸口", name = "nextProSize")
    @TableField(value = "NEXT_PRO_SIZE", updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal nextProSize;

    /**
     * 机台数
     */
    @Excel(name = "ui.data.column.sizeCapacity.machineNumber")
    @ApiModelProperty(value = "机台数", name = "machineNumber")
    @TableField(value = "MACHINE_NUMBER")
    private BigDecimal machineNumber;

    /**
     * 分配整月的机台数
     */
    @Excel(name = "ui.data.column.sizeCapacity.wholeMachineNumber")
    @ApiModelProperty(value = "机台数", name = "wholeMachineNumber")
    @TableField(value = "WHOLE_MACHINE_NUMBER")
    private Integer wholeMachineNumber;

    /**
     * 不能整月的机台天数
     */
    @Excel(name = "ui.data.column.sizeCapacity.remainingDays")
    @ApiModelProperty(value = "机台天数", name = "remainingDays")
    @TableField(value = "REMAINING_DAYS")
    private Integer remainingDays;
    /**
     * 最大机台数
     */
    @Excel(name = "ui.data.column.sizeCapacity.maxMachineNumber")
    @ApiModelProperty(value = "机台数", name = "maxMachineNumber")
    @TableField(value = "MAX_MACHINE_NUMBER")
    private Integer maxMachineNumber;

    /**
     * 自身Key，构建树形结构使用
     */
    @ApiModelProperty(value = "自身Key", name = "oneselfKey")
    @TableField(value = "ONESELF_KEY")
    private String oneselfKey;

    /**
     * 下一组的key
     */
    @ApiModelProperty(value = "机台数", name = "nextGroupKey")
    @TableField(value = "NEXT_GROUP_KEY")
    private String nextGroupKey;

    /**
     * 上一组的key
     */
    @ApiModelProperty(value = "机台数", name = "superGroupKey")
    @TableField(value = "SUPER_GROUP_KEY")
    private String superGroupKey;

    /**
     * 获取分组key
     * 寸口|*|工装类型|*|成型法|*|胎体布层级
     *
     * @return
     */
    public String getGroupKey() {
        String groupKey = "%s|*|%s|*|%s|*|%s";
        return String.format(groupKey, getProSize(), getWorkWearType(), getMouldMethod(), getCarcassClothType());
    }

    /**
     * 配置合并key标记
     * 寸口|*|工装类型|*|成型法|*|胎体布层级|*|成型机类型
     *
     * @return
     */
    public String getMergeConfigurationKey() {
        String mergeKey = "%s|*|%s|*|%s|*|%s|*|%s";
        return String.format(mergeKey, getProSize(), getWorkWearType(), getMouldMethod(), getCarcassClothType(), getMoldingMachineClsType());
    }

    /**
     * 获取分组key
     * 寸口|*|工装类别|*|成型法|*|成型机类型
     *
     * @return
     */
    public String getTreeGroupKey() {
        String groupKey = "%s|*|%s|*|%s|*|%s";
        return String.format(groupKey, getProSize(), getWorkWearType(), getMouldMethod(), getMoldingMachineClsType());
    }

    /**
     * 获取下一寸口的分组Key
     * 寸口|*|成型法|*|成型机类型
     *
     * @return
     */
    public String getNextTreeGroupKey() {
        if (null == getNextProSize()) {
            return "";
        }
        String groupKey = "%s|*|%s|*|%s";
        return String.format(groupKey, getNextProSize(), getMouldMethod(), getMoldingMachineClsType());
    }

    /**
     * 重新计算机器数
     *
     * @param monthDays
     */
    public void resetMachineNumber(Integer monthDays) {
        Integer add = remainingDays;
        if (null == add) {
            add = BigDecimal.ZERO.intValue();
        }
        Integer whole = wholeMachineNumber;
        if (null == whole) {
            whole = BigDecimal.ZERO.intValue();
        }
        BigDecimal addRemaining = BigDecimal.valueOf(add).divide(BigDecimal.valueOf(monthDays), 1, RoundingMode.HALF_UP);
        machineNumber = BigDecimal.valueOf(whole).add(addRemaining);
    }
}