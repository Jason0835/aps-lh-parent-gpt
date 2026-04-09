package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * APS模具清洗预警计划
 *
 * @author zlt
 * @since 2025/12/25
 */
@ApiModel(value = "APS模具清洗预警计划", description = "APS模具清洗预警计划")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_MOULD_CLEAN_WARN")
public class MdmMouldCleanWarn extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "厂别")
    @Excel(name = "ui.data.column.mouldCleanWarn.factoryCode")
    @ImportExcelValidated(required = true)
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "分公司编码")
//    @Excel(name = "ui.data.column.mouldCleanWarn.companyCode")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "硫化机台")
    @Excel(name = "ui.data.column.mouldCleanWarn.lhCode")
    @ImportExcelValidated(required = true)
    @TableField(value = "LH_CODE")
    private String lhCode;

    @ApiModelProperty(value = "上机时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "ui.data.column.mouldCleanWarn.operTime", width = 30)
    @ImportExcelValidated(required = true)
    @TableField(value = "OPER_TIME")
    private Date operTime;

    @ApiModelProperty(value = "首次清洗时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "ui.data.column.mouldCleanWarn.firstWashTime", width = 30)
    @TableField(value = "FIRST_WASH_TIME")
    private Date firstWashTime;

    @ApiModelProperty(value = "二次清洗时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "ui.data.column.mouldCleanWarn.secondWashTime", width = 30)
    @ImportExcelValidated(required = true)
    @TableField(value = "SECOND_WASH_TIME")
    private Date secondWashTime;

    @ApiModelProperty(value = "版本号")
//    @Excel(name = "ui.data.column.mouldCleanWarn.dataVersion")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "上机时间开始")
    @TableField(exist = false)
    private String operTimeBegin;

    @ApiModelProperty(value = "上机时间结束")
    @TableField(exist = false)
    private String operTimeEnd;
}
