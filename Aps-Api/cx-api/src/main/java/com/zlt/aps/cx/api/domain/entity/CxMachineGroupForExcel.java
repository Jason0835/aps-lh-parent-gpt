package com.zlt.aps.cx.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 成型机组对象 t_cx_machine_group
 *
 * @author zlt
 * @date 2021-12-16
 */
@ApiModel(value = "成型机组导入导出对象", description = "成型机组导入导出对象 ")
@Data
public class CxMachineGroupForExcel extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    private Long id;
    /**
     * 机台组名
     */
    @ImportValidated(required = true, maxLength = 100)
    @Excel(name = "ui.data.column.machineGroup.groupName", sort = 1)
    @ApiModelProperty(value = "机台组名")
    private String groupName;

    /**
     * 可投产班数
     */
    @ImportValidated(required = true, isInteger = true, min = 0, max = 99999)
    @Excel(name = "ui.data.column.machineGroup.productShift", sort = 2)
    @ApiModelProperty(value = "可投产班数")
    private Long productShift;

    @ImportValidated(maxLength = 20)
    @Excel(name = "ui.data.column.cxScheduleResult.cxMachineCode", sort = 3)
    @ApiModelProperty(value = "成型机台")
    private String cxMachineCode;

    /**
     * 备注
     */
    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark", sort = 4)
    @ApiModelProperty(value = "备注")
    private String remark;


}
