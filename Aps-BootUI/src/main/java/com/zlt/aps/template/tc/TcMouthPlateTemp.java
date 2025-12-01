package com.zlt.aps.template.tc;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 胎侧口型板信息维护
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-01
 */
@Data
@ApiModel(value="TcMouthPlate对象", description="胎侧口型板信息维护")
public class TcMouthPlateTemp extends ApsBaseEntity {

    private static final long serialVersionUID = 1110056585174675867L;

    @Excel(name = "ui.data.column.mouthPlateCode", sort = 10)
    @ApiModelProperty(value = "口型板编号。一个口型板编号可以对应多个机台。", position = 20)
    @ImportValidated(name = "ui.data.column.mouthPlateCode",required = true,isCode = true,maxLength = 30)
    private String mouthPlateCode;

    @Excel(name = "ui.specifyMachine.column.machineName", sort = 20)
    @ApiModelProperty(value = "生产线", position = 30)
    private String machineId;

    @Excel(name = "ui.data.column.mouthPlateStatus", sort = 30, dictType = "STATUS")
    @ApiModelProperty(value = "状态，0--启用，1--禁用。", position = 40)
    @ImportValidated(name = "ui.data.column.mouthPlateStatus",maxLength = 6)
    private String status;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}
