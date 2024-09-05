package com.zlt.aps.xwyy.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 纤维压延帘布大卷与机台的映射表
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
@Data
@ApiModel(value = "XwyyMachineRollMappingDto对象", description = "纤维压延帘布大卷与机台的映射表")
public class XwyyMachineRollMappingDto extends ApsBaseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    private Long id;

    @ApiModelProperty(value = "帘布大卷编号")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    @Excel(name = "ui.bigRollColor.column.bigRollCode")
    private String bigRollCode;

    @ApiModelProperty(value = "机台id（对应T_XWYY_MACHINE_INFO表id）")
    private Long machineId;

    @ApiModelProperty(value = "生产线")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    @Excel(name = "ui.specifyMachine.column.machineName", importName = "ui.data.column.machine.machineCode")
    private String machineName;

    @ApiModelProperty(value = "备注", position = 50)
    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
