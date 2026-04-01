package com.zlt.aps.lh.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 硫化精度计划查询VO
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "硫化精度计划查询VO", description = "硫化精度计划查询条件")
public class LhPrecisionPlanVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    @ApiModelProperty(value = "机台名称")
    private String machineName;

    @ApiModelProperty(value = "精度类型")
    private String precisionType;

    @ApiModelProperty(value = "计划日期-开始")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDateStart;

    @ApiModelProperty(value = "计划日期-结束")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDateEnd;

    @ApiModelProperty(value = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    @ApiModelProperty(value = "实际执行日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualDate;

    @ApiModelProperty(value = "到期日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    @ApiModelProperty(value = "距离到期日剩余天数")
    private Integer daysToDue;

    @ApiModelProperty(value = "上次保养日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastMaintenanceDate;

    @ApiModelProperty(value = "完成情况：0-未完成，1-已完成")
    private String completionStatus;

    @ApiModelProperty(value = "计划年度")
    private BigDecimal year;

    @ApiModelProperty(value = "预警状态：0-未预警，1-已预警")
    private String warningStatus;

    @ApiModelProperty(value = "预警触发日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate warningDate;

    @ApiModelProperty(value = "是否已发送预警：0-未发送，1-已发送")
    private String isWarningSent;

    @ApiModelProperty(value = "数据来源：0-同步，1-自动生成")
    private String dataSource;

    @ApiModelProperty(value = "MES来源ID")
    private Long mesSourceId;

    @ApiModelProperty(value = "分公司编码")
    private String companyCode;

    @ApiModelProperty(value = "分厂编码")
    private String factoryCode;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "创建时间-开始")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeStart;

    @ApiModelProperty(value = "创建时间-结束")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeEnd;

    @ApiModelProperty(value = "备注")
    private String remark;
}
