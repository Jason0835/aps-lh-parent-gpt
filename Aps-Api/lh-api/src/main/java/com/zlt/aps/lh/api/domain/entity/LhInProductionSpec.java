package com.zlt.aps.lh.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 硫化机台当前生产规格对象 t_lh_in_production_spec
 *
 * @author chen
 * @date 2022-03-23
 */
@ApiModel(value = "硫化机台当前生产规格对象", description = "硫化机台当前生产规格对象 ")
@Data
@EqualsAndHashCode(callSuper = true)
public class LhInProductionSpec extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应序列SEQ_LH_IN_PRODUCTION_SPEC
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 生产日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.inProductionSpec.productDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "生产日期")
    private Date productDate;

    /**
     * 硫化机台编号
     */
    @Excel(name = "ui.data.column.inProductionSpec.lhMachineCode")
    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;

    /**
     * 硫化机台名称
     */
    @ApiModelProperty(value = "硫化机台名称")
    private String lhMachineName;

    /**
     * 蒸锅编号
     */
    @Excel(name = "ui.data.column.inProductionSpec.lhStreamCode")
    @ApiModelProperty(value = "蒸锅编号")
    private String lhStreamCode;

    /**
     * SAP品号
     */
    @Excel(name = "ui.data.column.inProductionSpec.sapCode")
    @ApiModelProperty(value = "SAP品号")
    private String sapCode;

    /**
     * 左右模信息，左模L,右模R
     */
    @Excel(name = "ui.data.column.inProductionSpec.leftRightMold")
    @ApiModelProperty(value = "左右模信息，左模L,右模R")
    private String leftRightMold;

    /**
     * 是否空模，0-否，1-是
     */
    @Excel(name = "ui.data.column.inProductionSpec.isEmptyMold")
    @ApiModelProperty(value = "是否空模")
    private String isEmptyMold;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name="ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    private String remark;
}
