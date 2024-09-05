package com.zlt.aps.gdyy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;

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
@Getter
@TableName("T_GDYY_NOTE")
@ApiModel(value = "GdyyMattersAttention对象", description = "钢带大卷注意事项信息表")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class GdyyMattersAttention extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "钢带大卷编号")
    @TableField("BIG_ROLL_CODE")
    @Excel(name = "ui.common.column.gy.bigRollCode")
    @ImportValidated(isCode = true, maxLength = 30, required = true)
    private String bigRollCode;

    @ApiModelProperty(value = "注意事项（同个钢压大卷如果有多个注意事项，则需要配置多条记录）")
    @TableField("NOTES")
    @Excel(name = "ui.data.column.gdyy.scheduleResult.notes")
    @ImportValidated( maxLength = 100, required = true)
    private String notes;

    @ApiModelProperty(value = "生效开始时间")
    @TableField("START_TIME")
    @Excel(name = "ui.steelRollColor.column.startTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ImportValidated(date = true)
    private Date startTime;

    @ApiModelProperty(value = "生效结束时间")
    @TableField("END_TIME")
    @Excel(name = "ui.steelRollColor.column.endTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ImportValidated( date = true)
    private Date endTime;

    @ApiModelProperty(value = "状态，0--启用，1--禁用。")
    @TableField("STATUS")
    @Excel(name = "ui.bigRollColor.column.status",dictType="STATUS")
    @ImportValidated( required = true, isCode = true, maxLength = 1)
    private String status;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated( maxLength = 300)
    private String remark;
}
