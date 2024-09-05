package com.zlt.aps.gdyy.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 钢带大卷注意事项信息表
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-10
 */
@Data
@ApiModel(value="GdyyMattersAttention对象", description="钢带大卷注意事项信息表")
public class GdyyMattersAttentionDto extends ApsBaseDto implements Serializable{

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    private Long id;

    @ApiModelProperty(value = "钢带大卷编号")
    @Excel(name = "ui.common.column.gy.bigRollCode")
    @ImportValidated(name = "ui.common.column.gy.bigRollCode", isCode = true, maxLength = 30, required = true)
    private String bigRollCode;

    @ApiModelProperty(value = "注意事项（同个钢压大卷如果有多个注意事项，则需要配置多条记录）")
    @Excel(name = "ui.data.column.gdyy.scheduleResult.notes")
    @ImportValidated(name = "ui.data.column.gdyy.scheduleResult.notes", maxLength = 100, required = true)
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.steelRollColor.column.startTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "生效开始时间")
    @ImportValidated(name = "ui.steelRollColor.column.startTime", date = true)
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.steelRollColor.column.endTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "生效结束时间")
    @ImportValidated(name = "ui.steelRollColor.column.endTime", date = true)
    private Date endTime;

    @ApiModelProperty(value = "状态，0--启用，1--禁用。")
    @Excel(name = "ui.bigRollColor.column.status",dictType="STATUS")
    @ImportValidated(name = "ui.bigRollColor.column.status", required = true, isCode = true, maxLength = 1)
    private String status;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(name = "ui.column.stock.remark", maxLength = 300)
    private String remark;
}
