package com.zlt.aps.cx.api.domain.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 成型投产班次同机台硫化班次限定设置对象 t_cx_product_machine_limit
 * 
 * @author zlt
 * @date 2022-01-08
 */
@ApiModel(value = "成型投产班次同机台硫化班次限定设置对象", description = "成型投产班次同机台硫化班次限定设置对象 ")
@Data
public class CxProductMachineLimit extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 同机台最小平均值 */
    @Excel(name = "ui.data.column.machineLimit.minAvgShift")
    @ApiModelProperty(value = "同机台最小平均值")
    private Double minAvgShift;

    /** 同机台最大平均值 */
    @Excel(name = "ui.data.column.machineLimit.maxAvgShift")
    @ApiModelProperty(value = "同机台最大平均值")
    private Double maxAvgShift;

    /** 班数 */
    @Excel(name = "ui.data.column.machineLimit.shiftParams")
    @ApiModelProperty(value = "班数")
    private Double shiftParams;

    /** 删除标识 */
    @ApiModelProperty(value = "班数")
    private String delFlag;





}
