package com.zlt.aps.lh.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
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
public class LhPrecisionPlanImportVO extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.lhPrecisionPlan.factoryCode", dictType = "biz_factory_name")
    @ImportValidated(required = true, maxLength = 30)
    @ApiModelProperty(value = "分厂编码")
    private String factoryCode;

    @Excel(name = "ui.data.column.lhPrecisionPlan.machineCode")
    @ImportValidated(required = true, maxLength = 30)
    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    @Excel(name = "ui.data.column.lhPrecisionPlan.planDate", dateFormat = "yyyy-MM-dd")
    @ImportValidated(required = true, date = true)
    @ApiModelProperty(value = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date planDate;

    @Excel(name = "ui.data.column.lhPrecisionPlan.actualDate", dateFormat = "yyyy-MM-dd")
    @ImportValidated(required = true, date = true)
    @ApiModelProperty(value = "实际执行日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date actualDate;

    @Excel(name = "ui.data.column.remark")
    @ImportValidated(maxLength = 300)
    @ApiModelProperty(value = "备注")
    private String remark;
}
