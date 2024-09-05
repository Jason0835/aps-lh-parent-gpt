package com.zlt.aps.template.tm;

import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 胎面口型板信息维护
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-05-27
 */
@Data
@ApiModel(value = "TmMouthPlate对象", description = "胎面口型板信息维护")
public class TmMouthPlateTemp  {

    private static final long serialVersionUID = 1110056585174675867L;

    @Excel(name = "ui.data.column.mouthPlateCode", sort = 10)
    @ApiModelProperty(value = "口型板编号。一个口型板编号可以对应多个机台。")
    private String mouthPlateCode;

    @Excel(name = "ui.specifyMachine.column.machineName", sort = 20)
    @ApiModelProperty(value = "机台code")
    private String machineCode;

    @Excel(name = "ui.data.column.mouthPlateStatus", sort = 30, dictType = "STATUS")
    @ApiModelProperty(value = "状态，0--启用，1--禁用。")
    private String status;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注")
    private String remark;
}
