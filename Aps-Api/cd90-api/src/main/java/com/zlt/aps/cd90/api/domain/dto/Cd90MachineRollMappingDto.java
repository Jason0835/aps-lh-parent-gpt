package com.zlt.aps.cd90.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 90度裁断帘布大卷与机台的映射表
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-16
 */
@Data
@ApiModel(value = "Cd90MachineRollMappingDto对象", description = "90度裁断帘布大卷与机台的映射表")
public class Cd90MachineRollMappingDto extends ApsBaseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    private Long id;

    @ApiModelProperty(value = "帘布大卷代号")
    @Excel(name = "ui.bigRollColor.column.bigRollCode")
    @ImportValidated(isCode = true, maxLength = 30, required = true)
    private String bigRollCode;

    @ApiModelProperty(value = "机台id")
    private Long machineId;

    @ApiModelProperty(value = "生产线")
    @Excel(name = "ui.data.column.loss.line", importName = "ui.data.column.loss.line")
    @ImportValidated(required = true, maxLength = 30)
    private String machineName;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    private String remark;
}
