package com.zlt.aps.template.cx;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 定点机台配置列对象 t_specify_machine_list
 *
 * @author zlt
 * @date 2021-06-11
 */
@ApiModel(value = "定点机台配置对象", description = "定点机台配置对象 ")
public class CxMatchingSpecifyMachineListTemp extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 工序数据维护在数据字典(PROCEDURE_CODE)：0-硫化，1-成型，2胎面，3-胎侧，4-内衬，5-胎圈，6-钢丝圈，7 -15度裁断，8-90度裁断，9-钢带压延，10-纤维压延
     */
    @ImportValidated(required = true, isCode = true)
    @Excel(name = "ui.data.column.cx.machine.procedureCode", dictType = "PROCEDURE_CODE")
    @ApiModelProperty(value = "工序", position = 30)
    private String procedureCode;

    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.machine.machineName")
    @ImportValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "机台名称", position = 60)
    private String machineName;


    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 70)
    private String remark;

}
