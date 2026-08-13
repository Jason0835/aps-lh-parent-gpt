package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "胎侧DJ共享机台对象", description = "胎侧DJ共享机台对象")
@Data
@TableName(value = "T_TC_DJ_SHARED_MACHINE")
public class TcDjSharedMachine extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tc.djSharedMachine.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tc.djSharedMachine.machineCode")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 30)
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.tc.djSharedMachine.tcShiftCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "胎侧班次", name = "tcShiftCode")
    @TableField(value = "TC_SHIFT_CODE")
    private String tcShiftCode;

    @Excel(name = "ui.data.column.tc.djSharedMachine.djShiftCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "DJ班次", name = "djShiftCode")
    @TableField(value = "DJ_SHIFT_CODE")
    private String djShiftCode;

    @Excel(name = "ui.data.column.tc.djSharedMachine.enableStatus", dictType = "biz_yes_no")
    @ImportExcelValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否启用", name = "enableStatus")
    @TableField(value = "ENABLE_STATUS")
    private String enableStatus;

    @Excel(name = "ui.common.column.remark")
    @ImportExcelValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}