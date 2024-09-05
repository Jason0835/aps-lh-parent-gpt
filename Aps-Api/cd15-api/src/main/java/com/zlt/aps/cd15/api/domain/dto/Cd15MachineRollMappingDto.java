package com.zlt.aps.cd15.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 15度裁断钢压大卷与机台的映射表
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
@Data
@ApiModel(value="Cd15MachineRollMapping对象", description="15度裁断钢压大卷与机台的映射表")
public class Cd15MachineRollMappingDto extends ApsBaseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "钢压大卷编号")
    @Excel(name="ui.common.column.gy.bigRollCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    private String bigRollCode;


    @ApiModelProperty(value = "机台id（对应T_CD15_MACHINE_INFO表id）")
    private Long machineId;

    @ApiModelProperty(value = "生产线")
    @Excel(name="ui.data.column.loss.line", importName = "ui.data.column.loss.line")
    @ImportValidated(required = true, maxLength = 30)
    private String machineName;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    private String remark;
}
