package com.zlt.aps.template.gdyy;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel(value="GdyyMattersAttention对象", description="钢带大卷注意事项信息表")
public class GdyyMattersAttentionTemp extends BaseEntity {

    @ApiModelProperty(value = "钢带大卷编号")
    @Excel(name = "ui.common.column.gy.bigRollCode")
    private String bigRollCode;

    @ApiModelProperty(value = "注意事项（同个钢压大卷如果有多个注意事项，则需要配置多条记录）")
    @Excel(name = "ui.data.column.gdyy.scheduleResult.notes")
    private String notes;

    @Excel(name = "ui.steelRollColor.column.startTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "生效开始时间")
    private Date startTime;

    @Excel(name = "ui.steelRollColor.column.endTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "生效结束时间")
    private Date endTime;

    @ApiModelProperty(value = "状态，0--启用，1--禁用。")
    @Excel(name = "ui.bigRollColor.column.status",dictType="STATUS")
    private String status;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.data.column.stock.remark")
    private String remark;
}
