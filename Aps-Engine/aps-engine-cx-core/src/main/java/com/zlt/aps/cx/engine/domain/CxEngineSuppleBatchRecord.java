package com.zlt.aps.cx.engine.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 成型前日计划增补批次对象 t_cx_supple_batch_record
 * 
 * @author Joran.zhang
 * @date 2022-02-09
 */
@ApiModel(value = "成型前日计划增补批次对象", description = "成型前日计划增补批次对象 ")
@Data
public class CxEngineSuppleBatchRecord extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 成型增补计划批次号 */
    @ApiModelProperty(value = "成型增补计划批次号")
    private String suppleBatchNo;

    /** 状态：0-未确认；1-已确认 */
    @Excel(name = "ui.data.column.record.status")
    @ApiModelProperty(value = "状态：0-未确认；1-已确认")
    private String status;

    /** 增补日期 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.record.suppleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "增补日期")
    private Date suppleDate;

    /**
     * 增补日期搜索条件
     */
    private String suppleDateStr;
}
