package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎胚最早可供硫化时间实体（结构切换时）
 * <p>
 * 定义各结构切换时胎胚最早可供硫化的时间，用于排程计算
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_EMBRYO_LH_TIME")
@ApiModel(value = "胎胚最早可供硫化时间")
public class CxEmbryoLhTime extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.cxEmbryoLhTime.structureName")
    @ApiModelProperty(value = "结构")
    @TableField("STRUCTURE_NAME")
    @ImportValidated(required = true, maxLength = 100)
    private String structureName;

    @Excel(name = "ui.data.column.cxEmbryoLhTime.earliestLhTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最早可供硫化时间")
    @TableField("EARLIEST_LH_TIME")
    @ImportValidated(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date earliestLhTime;

    @ApiModelProperty(value = "行数")
    @TableField(exist = false)
    private Integer rowNo;
}
