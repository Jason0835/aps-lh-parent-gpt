package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 基础数据-成型机可用信息
 */
@ApiModel(value = "成型机可用信息", description = "成型机可用信息 ")
@Data
@TableName(value = "t_mdm_molding_machine_status")
public class MdmMoldingMachineStatus extends BaseEntity implements Serializable {

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.docMoldingMachineStatus.factoryCode", dictType = "biz_factory_name", sort = 40)
    @ApiModelProperty(value = "分厂编号")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.colume.year", sort = 10)
    @ApiModelProperty(value = "年份")
    @ImportExcelValidated(required = true, max = 9999)
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.colume.month", sort = 20)
    @ApiModelProperty(value = "月份")
    @ImportExcelValidated(required = true, min = 1, max = 12)
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 成型机ID
     */
    @ApiModelProperty(value = "成型机ID")
    @TableField(value = "MOLDING_MACHINE_ID")
    private Long moldingMachineId;

    /**
     * 成型机状态:0-禁用，1-可用
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.docMoldingMachineStatus.status", dictType = "sys_enable_disable", sort = 60)
    @ApiModelProperty(value = "成型机状态:0-禁用，1-可用")
    @TableField(value = "STATUS")
    private Integer status;

    /**
     * 备注
     */
    @Excel(name = "ui.data.column.remark", sort = 110)
    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    /**
     * 类别名称
     */
    @Excel(name = "ui.data.column.docMoldingMachineStatus.moldingMachineClassName", sort = 80)
    @TableField(exist = false)
    private String moldingMachineClassName;

    /**
     * 成型法:
     * 0-1次法
     * 1-2次法
     * 3-2鼓
     * 4-3鼓
     */
    @Excel(name = "ui.data.column.docMoldingMachineStatus.moldingMethod", dictType = "molding_method", sort = 90)
    @TableField(exist = false)
    private Integer moldingMethod;

    /**
     * 品名代码
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 5)
    @Excel(name = "ui.data.column.docMoldingMachineStatus.productTypeCode", sort = 50)
    @TableField(exist = false)
    private String productTypeCode;

    /**
     * 班次
     */
    @Excel(name = "ui.data.column.docMoldingMachineStatus.classes", sort = 100, cellType = Excel.ColumnType.NUMERIC)
    @TableField(exist = false)
    private Integer classes;

}