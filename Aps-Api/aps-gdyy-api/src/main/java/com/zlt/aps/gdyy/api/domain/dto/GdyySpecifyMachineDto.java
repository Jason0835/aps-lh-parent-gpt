package com.zlt.aps.gdyy.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 钢带压延定点机台表
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Data
@ApiModel(value = "GdyySpecifyMachine对象", description = "钢带压延定点机台表")
public class GdyySpecifyMachineDto extends ApsBaseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "钢带大卷代码")
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.gdyy.specifyMachine.column.bigRollCode")
    private String bigRollCode;

    @ApiModelProperty(value = "机台id（对应T_GDYY_MACHINE_INFO表id）")
    private Long machineId;

    @ApiModelProperty(value = "机台code")
    private String machineCode;

    @ApiModelProperty(value = "生产线")
    @ImportValidated(required = true)
    @Excel(name = "ui.specifyMachine.column.machineName")
    private String machineName;

    @ApiModelProperty(value = "线路，数据维护在数据字典：0-生产线、1-备用线")
    @ImportValidated(required = true)
    @Excel(name = "ui.specifyMachine.column.lineType", dictType = "LINE_TYPE")
    private String lineType;

    @ApiModelProperty(value = "作业类型，数据维护在数据字典：0-限制作业；1-不可作业")
    @ImportValidated(required = true)
    @Excel(name = "ui.specifyMachine.column.jobType", dictType = "JOB_TYPE")
    private String jobType;

    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.data.column.stock.remark")
    private String remark;
}
