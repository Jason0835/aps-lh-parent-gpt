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
 * APS模具清洗计划
 *
 * @author zlt
 * @since 2025/12/25
 */
@ApiModel(value = "APS模具清洗计划", description = "APS模具清洗计划")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_MOULD_CLEAN_PLAN")
public class MdmMouldCleanPlan extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "厂别")
    @Excel(name = "ui.data.column.mouldCleanPlan.factoryCode")
    @ImportExcelValidated(required = true)
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "硫化机台")
    @Excel(name = "ui.data.column.mouldCleanPlan.lhCode")
    @ImportExcelValidated(required = true)
    @TableField(value = "LH_CODE")
    private String lhCode;

    @ApiModelProperty(value = "清洗日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Excel(name = "ui.data.column.mouldCleanPlan.cleanTime", width = 30)
    @ImportExcelValidated(required = true)
    @TableField(value = "CLEAN_TIME")
    private Date cleanTime;

    @ApiModelProperty(value = "清洗类型")
    @Excel(name = "ui.data.column.mouldCleanPlan.cleanType", dictType = "MOULD_CLEAN_TYPE")
    @ImportExcelValidated(required = true)
    @TableField(value = "CLEAN_TYPE")
    private String cleanType;

    @ApiModelProperty(value = "数据来源")
    @Excel(name = "ui.data.column.mouldCleanPlan.dataSource")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "清洗日期开始")
    @TableField(exist = false)
    private Date cleanTimeBegin;

    @ApiModelProperty(value = "清洗日期结束")
    @TableField(exist = false)
    private Date cleanTimeEnd;
}
