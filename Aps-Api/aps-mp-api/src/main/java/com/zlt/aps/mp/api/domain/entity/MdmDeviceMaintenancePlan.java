package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 基础数据-设备维护计划
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_DEVICE_MAINTENANCE_PLAN")
public class MdmDeviceMaintenancePlan extends BaseEntity implements Serializable {

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 计划类型:0-维修，1-保养
     */
    @TableField(value = "PLAN_TYPE")
    private Integer planType;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.docDeviceMaintenancePlan.factoryCode", sort = 50, dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.colume.year", sort = 10)
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 9999)
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.colume.month", sort = 20)
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 设备类型:0-成型机，1-硫化机，2-模具,3-洗模
     */
    @Excel(name = "ui.data.column.docDeviceMaintenancePlan.machineType", dictType = "mdm_machine_type", sort = 40)
    @ImportExcelValidated(required = true)
    @TableField(value = "MACHINE_TYPE")
    private Integer machineType;

    /**
     * 设备编号
     */
    @Excel(name = "ui.data.column.docDeviceMaintenancePlan.machineCode", sort = 60)
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @TableField(exist = false)
    private Long machineId;

    /**
     * 开始日期:yyyy-MM-DD HH
     */
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.docDeviceMaintenancePlan.startDate", dateFormat = "yyyy-MM-dd HH:mm:ss", sort = 70, width = 30)
    @ImportExcelValidated(required = true)
    @TableField(value = "BEGIN_DATE")
    private Date beginDate;

    /**
     * 结束日期:yyyy-MM-DD HH
     */
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.docDeviceMaintenancePlan.finallyDate", dateFormat = "yyyy-MM-dd HH:mm:ss", sort = 80, width = 30)
    @ImportExcelValidated(required = true)
    @TableField(value = "END_DAY")
    private Date endDay;

    /**
     * 行号
     */
    @TableField(exist = false)
    private Integer rowNum;
}