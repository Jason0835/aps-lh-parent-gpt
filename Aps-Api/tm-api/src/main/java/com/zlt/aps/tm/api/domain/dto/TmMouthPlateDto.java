package com.zlt.aps.tm.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

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
public class TmMouthPlateDto extends ApsBaseDto {

    private static final long serialVersionUID = 1110056585174675867L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    private Long id;

    @Excel(name = "ui.data.column.mouthPlateCode", sort = 10)
    @ApiModelProperty(value = "口型板编号。一个口型板编号可以对应多个机台。", position = 20)
    @NotBlank(message = "口型板代码不能为空")
    @ImportValidated(name = "ui.data.column.mouthPlateCode", required = true, isCode = true, maxLength = 30)
    private String mouthPlateCode;

    @ApiModelProperty(value = "机台id", position = 30)
    private Long machineId;

    @Excel(name = "ui.data.column.mouthPlateStatus", sort = 30, dictType = "STATUS")
    @ApiModelProperty(value = "状态，0--启用，1--禁用。", position = 40)
    @ImportValidated(name = "ui.data.column.mouthPlateStatus", maxLength = 6,required = true)
    private String status;

    @Excel(name = "ui.specifyMachine.column.machineName", sort = 20)
    @ApiModelProperty(value = "生产线", position = 50)
    @ImportValidated(required = true, maxLength = 30)
    private String machineName;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}
