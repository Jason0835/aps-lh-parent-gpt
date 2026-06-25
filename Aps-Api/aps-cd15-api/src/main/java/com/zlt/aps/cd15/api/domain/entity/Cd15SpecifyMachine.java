package com.zlt.aps.cd15.api.domain.entity;

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
 * 斜裁定点机台配置。
 */
@Data
@ApiModel(value = "斜裁定点机台配置", description = "斜裁定点机台配置")
@TableName("t_cd15_specify_machine")
public class Cd15SpecifyMachine extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15SpecifyMachine.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 钢带代码 */
    @ApiModelProperty(value = "钢带代码", name = "steelStripCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("STEEL_STRIP_CODE")
    @Excel(name = "ui.data.column.cd15SpecifyMachine.steelStripCode")
    private String steelStripCode;

    /** 机台编号 */
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @ImportExcelValidated(required = true, maxLength = 30)
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.cd15SpecifyMachine.machineCode")
    private String machineCode;

    /** 线路类型（仅保留数据库和实体字段，不参与当前业务） */
    @ApiModelProperty(value = "线路类型", name = "lineType")
    @TableField("LINE_TYPE")
    private String lineType;

    /** 作业类型 */
    @ApiModelProperty(value = "作业类型", name = "jobType")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("JOB_TYPE")
    @Excel(name = "ui.data.column.cd15SpecifyMachine.jobType", dictType = "JOB_TYPE")
    private String jobType;
}
