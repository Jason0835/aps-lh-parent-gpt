package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 基础数据-硫化机可用信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "T_MDM_VULCANIZING_MACH_STATUS")
public class MdmVulcanizingMachStatus extends BaseEntity implements Serializable {

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.docVulcanizationMachStatus.factoryCode", sort = 40, dictType = "biz_factory_name")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.colume.year", sort = 10)
    @ImportExcelValidated(required = true)
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.colume.month", sort = 20)
    @ImportExcelValidated(required = true)
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 硫化机ID
     */
    @TableField(value = "VULCANIZING_MACHINE_ID")
    private Long vulcanizingMachineId;

    /**
     * 成型机状态:0-禁用，1-可用
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.docVulcanizationMachStatus.status", dictType = "sys_enable_disable", sort = 60)
    @TableField(value = "STATUS")
    private Integer status;

    /**
     * 备注
     */
    @Excel(name = "ui.data.column.remark", sort = 80)
    @TableField("REMARK")
    private String remark;

}