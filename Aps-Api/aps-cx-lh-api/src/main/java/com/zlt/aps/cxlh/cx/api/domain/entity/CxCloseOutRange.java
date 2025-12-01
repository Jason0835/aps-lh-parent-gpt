package com.zlt.aps.cxlh.cx.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 成型收尾范围系数对象 t_cx_close_out_range
 * 
 * @author zlt
 * @date 2021-12-28
 */
@ApiModel(value = "成型收尾范围系数对象", description = "成型收尾范围系数对象 ")
@Data
public class CxCloseOutRange extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 范围名称 */
    @Excel(name = "ui.data.column.closeOutRange.rangeName")
    @ApiModelProperty(value = "范围名称")
    private String rangeName;

    /** 收尾范围(下限) */
    @Excel(name = "ui.data.column.closeOutRange.closeOutRangeMinimum")
    @ApiModelProperty(value = "收尾范围(下限)")
    private Long closeOutRangeMinimum;

    /** 收尾范围(上限) */
    @Excel(name = "ui.data.column.closeOutRange.closeOutRangeMaximum")
    @ApiModelProperty(value = "收尾范围(上限)")
    private Long closeOutRangeMaximum;

    /** 系数值 */
    @Excel(name = "ui.data.column.closeOutRange.rangeValue")
    @ApiModelProperty(value = "系数值")
    private Double rangeValue;

    /** 删除标识 */
    @ApiModelProperty(value = "删除标识")
    private String delFlag;





}
