package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 直裁定点机台配置。
 */
@Data
@ApiModel(value = "直裁定点机台配置", description = "直裁定点机台配置")
@TableName("t_cd90_specify_machine")
public class Cd90SpecifyMachine extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd90SpecifyMachine.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 帘布代号 */
    @ApiModelProperty(value = "帘布代号", name = "clothCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("CLOTH_CODE")
    @Excel(name = "ui.data.column.cd90SpecifyMachine.clothCode")
    private String clothCode;

    /** 机台编号 */
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @ImportExcelValidated(required = true, maxLength = 30)
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.cd90SpecifyMachine.machineCode")
    private String machineCode;

    /** 线路类型 */
    @ApiModelProperty(value = "线路类型", name = "lineType")
    @TableField("LINE_TYPE")
    private String lineType;

    /** 作业类型 */
    @ApiModelProperty(value = "作业类型", name = "jobType")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("JOB_TYPE")
    @Excel(name = "ui.data.column.cd90SpecifyMachine.jobType", dictType = "JOB_TYPE")
    private String jobType;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField("REMARK")
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
