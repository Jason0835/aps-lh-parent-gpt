package com.zlt.aps.dj.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 垫胶定点机台表
 * </p>
 *
 * @author zlt
 * @since 2026-06-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_DJ_SPECIFY_MACHINE")
public class DjSpecifyMachine extends BaseEntity{

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "垫胶代码")
    @Excel(name="ui.dj.specifyMachine.column.paddingCode")
    @ImportExcelValidated(name = "ui.dj.specifyMachine.column.paddingCode", required = true, isCode = true, maxLength = 20)
    @TableField("PADDING_CODE")
    private String paddingCode;

    @ApiModelProperty(value = "机台")
    @Excel(name="ui.specifyMachine.column.machineName")
    @ImportExcelValidated(name = "ui.specifyMachine.column.machineName", required = true, isCode = true, maxLength = 30)
    @TableField("MACHINE_CODE")
    private String machineCode;

    @ApiModelProperty(value = "线路，数据维护在数据字典：0-生产线、1-备用线")
    @Excel(name="ui.specifyMachine.column.lineType" ,dictType="LINE_TYPE")
    @ImportExcelValidated(name = "ui.specifyMachine.column.lineType",required = true, maxLength = 9)
    @TableField("LINE_TYPE")
    private String lineType;

    @ApiModelProperty(value = "作业类型，数据维护在数据字典：0-限制作业；1-不可作业")
    @Excel(name="ui.specifyMachine.column.jobType",dictType="JOB_TYPE")
    @ImportExcelValidated(name = "ui.specifyMachine.column.jobType",required = true, maxLength = 12)
    @TableField("JOB_TYPE")
    private String jobType;

    @Excel(name = "ui.data.column.info.remark")
    @ImportExcelValidated(name = "ui.data.column.info.remark", maxLength = 100)
    @ApiModelProperty(value = "备注")
    @TableField(value = "REMARK")
    private String remark;
}
