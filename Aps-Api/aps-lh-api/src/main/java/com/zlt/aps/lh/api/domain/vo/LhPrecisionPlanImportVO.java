package com.zlt.aps.lh.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 硫化精度计划导入VO
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "硫化精度计划导入VO", description = "硫化精度计划导入数据")
public class LhPrecisionPlanImportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.lhPrecisionPlan.machineCode")
    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    @Excel(name = "ui.data.column.lhPrecisionPlan.planDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date planDate;

    @Excel(name = "ui.data.column.lhPrecisionPlan.actualDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际执行日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date actualDate;

    @Excel(name = "ui.data.column.remark")
    @ApiModelProperty(value = "备注")
    private String remark;
}
