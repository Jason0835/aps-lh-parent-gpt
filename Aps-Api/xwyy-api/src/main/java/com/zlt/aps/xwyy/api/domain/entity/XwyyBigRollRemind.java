package com.zlt.aps.xwyy.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 帘布大卷原线提醒对象 t_xwyy_big_roll_remind
 * 
 * @author chen
 * @date 2022-04-27
 */
@ApiModel(value = "帘布大卷原线提醒对象", description = "帘布大卷原线提醒对象 ")
@Data
public class XwyyBigRollRemind extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_PUBLIC */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 帘布大卷编号 */
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    @Excel(name = "ui.data.column.bigRollRemind.bigRollCode")
    @ApiModelProperty(value = "帘布大卷编号")
    private String bigRollCode;

    /** 提醒标识 */
    @Excel(name = "ui.data.column.bigRollRemind.remindFlag", dictType = "ISORNOT")
    @ApiModelProperty(value = "提醒标识")
    private String remindFlag;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注")
    private String remark;
}
