package com.zlt.aps.mdm.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 设备计划停机查询机台参数
 *
 * @author Chen
 * @since 2025/12/26
 */
@Data
public class MdmDevicePlanShutQueryMachineParamVo implements Serializable {

    /**
     * 机台类型，字典：device_shut_machine_type；硫化、成型、压出、裁断、压延、密炼；
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmDevicePlanShut.machineType", dictType = "device_shut_machine_type")
    @ApiModelProperty(value = "机台类型，字典：machine_type；硫化、成型、压出、裁断、压延、密炼；", name = "machineType")
    @TableField(value = "MACHINE_TYPE")
    private String machineType;

    //------ 成型机参数 -------

    /**
     * 工厂编号
     */
    @Excel(name = "ui.data.column.mdmMoldingMachine.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 成型机编码
     */
    @Excel(name = "ui.data.column.mdmMoldingMachine.cxMachineCode")
    @ApiModelProperty(value = "成型机编码", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 成型机类型
     */
    @Excel(name = "ui.data.column.mdmMoldingMachine.cxMachineBrandCode", dictType = "biz_machine_brand")
    @ApiModelProperty(value = "成型机类型", name = "cxMachineBrandCode")
    @TableField(value = "CX_MACHINE_BRAND_CODE")
    private String cxMachineBrandCode;

    /**
     * 类型
     */
    @Excel(name = "ui.data.column.mdmMoldingMachine.cxMachineTypeCode", dictType = "biz_class_type")
    @ApiModelProperty(value = "类型", name = "cxMachineTypeCode")
    @TableField(value = "CX_MACHINE_TYPE_CODE")
    private String cxMachineTypeCode;

    /**
     * 反包方式
     */
    @Excel(name = "ui.data.column.mdmMoldingMachine.rollOverType")
    @ApiModelProperty(value = "反包方式", name = "rollOverType")
    @TableField(value = "ROLL_OVER_TYPE")
    private String rollOverType;

    /**
     * 是否有零度供料架 数据字典 biz_yes_no 1 是 0 否
     */
    @Excel(name = "ui.data.column.mdmMoldingMachine.isZeroRack", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否有零度供料架 数据字典 biz_yes_no 1 是 0 否", name = "isZeroRack")
    @TableField(value = "IS_ZERO_RACK")
    private String isZeroRack;

    /**
     * 硫化机上限
     */
    @Excel(name = "ui.data.column.mdmMoldingMachine.lhMachineMaxQty")
    @ApiModelProperty(value = "硫化机上限", name = "lhMachineMaxQty")
    @TableField(value = "LH_MACHINE_MAX_QTY")
    private Integer lhMachineMaxQty;

    /**
     * 设备最大日产量
     */
    @Excel(name = "ui.data.column.mdmMoldingMachine.maxDayCapacity")
    @ApiModelProperty(value = "设备最大日产量", name = "maxDayCapacity")
    @TableField(value = "MAX_DAY_CAPACITY")
    private Integer maxDayCapacity;

    // ----- 硫化机参数 -----

    /**
     * 机台编号
     */
    @Excel(name = "ui.data.column.info.machineCode", sort = 20)
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.info.machineName", sort = 30)
    @ApiModelProperty(value = "机台名称", name = "machineName")
    @TableField(value = "MACHINE_NAME")
    private String machineName;

    /**
     * 寸口信息
     */
//    @Excel(name = "ui.data.column.info.dimension",sort = 50)
    @ApiModelProperty(value = "寸口信息", name = "dimension")
    @TableField(value = "DIMENSION")
    private BigDecimal dimension;

    /**
     * 向心机构，数据字典取值（如：0-无，1-有）
     */
    @ApiModelProperty(value = "向心机构，数据字典IS_HAVE取值", name = "centripetalMechanism")
    @TableField(value = "CENTRIPETAL_MECHANISM")
    private String centripetalMechanism;

    /**
     * 生产寸口范围下限，单位：英寸
     */
    @Excel(name = "ui.data.column.info.dimensionMinimum", sort = 60)
    @ApiModelProperty(value = "生产寸口范围下限，单位：英寸", name = "dimensionMinimum")
    @TableField(value = "DIMENSION_MINIMUM")
    private BigDecimal dimensionMinimum;

    /**
     * 生产寸口范围上限，单位：英寸
     */
    @Excel(name = "ui.data.column.info.dimensionMaximum", sort = 70)
    @ApiModelProperty(value = "生产寸口范围上限，单位：英寸", name = "dimensionMaximum")
    @TableField(value = "DIMENSION_MAXIMUM")
    private BigDecimal dimensionMaximum;

    /**
     * 班制，如：三班制、两班制，对应数据字典 CLASS_SHIFT
     */
    @ApiModelProperty(value = "班制，如：三班制、两班制，对应数据字典 CLASS_SHIFT", name = "classShift")
    @TableField(value = "CLASS_SHIFT")
    private String classShift;

    /**
     * 最大使用模具数量，范围 0-255
     */
    @Excel(name = "ui.data.column.info.maxMoldNum", sort = 80)
    @ApiModelProperty(value = "模台数", name = "maxMoldNum")
    @TableField(value = "MAX_MOLD_NUM")
    private Integer maxMoldNum;

    /**
     * 生产定额，单班一次生产量，单位：条
     */
    @Excel(name = "ui.data.column.info.quota", sort = 90)
    @ApiModelProperty(value = "生产定额，单班一次生产量，单位：条", name = "quota")
    @TableField(value = "QUOTA")
    private Integer quota;

    /**
     * 开机班次，如：中班、夜班，对应数据字典 CLASS_NUM
     */
    @ApiModelProperty(value = "开机班次，如：中班、夜班，对应数据字典 CLASS_NUM", name = "openMachineClass")
    @TableField(value = "OPEN_MACHINE_CLASS")
    private String openMachineClass;

    /**
     * 是否启用
     */
    @Excel(name = "ui.data.column.info.status", sort = 100, dictType = "sys_enable_disable")
    @ApiModelProperty(value = "是否启用，字典：sys_enable_disable", name = "status")
    @TableField(value = "STATUS")
    private String status;

    @ApiModelProperty(value = "模具关系", name = "moldRelationList")
    @TableField(value = "MOLD_RELATION_LIST")
    private String moldRelationList;

    @Excel(name = "ui.data.column.info.machineOrder", sort = 42)
    @ApiModelProperty(value = "机台顺序", name = "machineOrder")
    @TableField(value = "MACHINE_ORDER")
    private Integer machineOrder;

}
