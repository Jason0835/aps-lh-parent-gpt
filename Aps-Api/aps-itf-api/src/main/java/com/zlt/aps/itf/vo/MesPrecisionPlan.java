package com.zlt.aps.itf.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * MES精度计划中间表实体
 * 对应表：MES_PRECISION_PLAN
 * 统一存储成型精度计划和硫化精度计划下发数据，通过PRECISION_TYPE区分
 * PRECISION_TYPE值：硫化精度 / 成型精度
 *
 * @author APS Team
 */
@Data
@TableName(value = "MES_PRECISION_PLAN")
@ApiModel(value = "MES精度计划中间表实体", description = "MES精度计划中间表，统一存储成型和硫化精度计划")
public class MesPrecisionPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID", name = "id")
    @TableField(value = "ID")
    private Long id;

    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @ApiModelProperty(value = "精度类型：硫化精度/成型精度", name = "precisionType")
    @TableField(value = "PRECISION_TYPE")
    private String precisionType;

    @ApiModelProperty(value = "计划排程精度日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private LocalDate scheduleDate;

    @ApiModelProperty(value = "计划日期", name = "planDate")
    @TableField(value = "PLAN_DATE")
    private LocalDate planDate;

    @ApiModelProperty(value = "版本号", name = "dataVersion")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码", name = "companyCode")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
