package com.zlt.aps.cx.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 成型排程中夜班完成量对象 t_mid_night_shift_finish
 * 
 * @author chen
 * @date 2022-02-25
 */
@ApiModel(value = "成型排程中夜班完成量对象", description = "成型排程中夜班完成量对象 ")
@Data
@EqualsAndHashCode(callSuper = true)
public class MidNightShiftFinish extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应序列SEQ_CX_FINISH_QTY */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 完成日期 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.midNightFinish.finishDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "完成日期")
    private Date finishDate;

    /** 成型机台编号 */
    @Excel(name = "ui.data.column.cxScheduleResult.cxMachineCode")
    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;

    /** 胎胚SAP品号 */
    @Excel(name = "ui.data.column.midNightFinish.sapCode")
    @ApiModelProperty(value = "胎胚SAP品号")
    private String sapCode;

    /** 胎胚代码 */
    @Excel(name = "ui.data.column.midNightFinish.embryoCode")
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /** 胎胚施工版本 */
    @Excel(name = "ui.data.column.midNightFinish.bomDataVersion")
    @ApiModelProperty(value = "胎胚施工版本")
    private String bomDataVersion;

    /** 一班(中班)完成量 */
    @Excel(name = "ui.data.column.midNightFinish.class1FinishQty")
    @ApiModelProperty(value = "一班(中班)完成量")
    private Integer class1FinishQty;

    /** 二班(夜班)完成量 */
    @Excel(name = "ui.data.column.midNightFinish.class2FinishQty")
    @ApiModelProperty(value = "二班(夜班)完成量")
    private Integer class2FinishQty;
}
