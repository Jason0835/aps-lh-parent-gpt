package com.zlt.aps.lh.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 硫化精度计划导出VO
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "硫化精度计划导出VO", description = "硫化精度计划导出数据")
public class LhPrecisionPlanExportVO extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.lhPrecisionPlan.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编码")
    private String factoryCode;

    @Excel(name = "ui.data.column.lhPrecisionPlan.year")
    @ApiModelProperty(value = "计划年度")
    private BigDecimal year;

    @Excel(name = "ui.data.column.lhPrecisionPlan.machineCode")
    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    @Excel(name = "ui.data.column.lhPrecisionPlan.precisionType")
    @ApiModelProperty(value = "精度类型")
    private String precisionType;

    @Excel(name = "ui.data.column.lhPrecisionPlan.planDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date planDate;

    @Excel(name = "ui.data.column.lhPrecisionPlan.actualDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际执行日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date actualDate;

    @Excel(name = "ui.data.column.lhPrecisionPlan.daysToDue")
    @ApiModelProperty(value = "距离到期日剩余天数")
    private Integer daysToDue;

    @Excel(name = "ui.data.column.lhPrecisionPlan.dataSource", dictType = "lh_precision_data_source")
    @ApiModelProperty(value = "数据来源：0-同步，1-自动生成")
    private String dataSource;

    @Excel(name = "ui.lh.precision.plan.updateTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @Excel(name = "ui.data.column.remark")
    @ApiModelProperty(value = "备注")
    private String remark;
}
