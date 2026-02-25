package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;

import java.io.Serializable;

/**
 * 分厂成型正在生产的品种
 *
 * @TableName T_MDM_PRODUCTION_MOLDING
 */
@Data
@TableName(value = "T_MDM_PRODUCTION_MOLDING")
public class MdmProductionMolding extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 年份
     */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 9999)
    @Excel(name = "ui.data.column.factoryProductionProduct.year", sort = 10)
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @Excel(name = "ui.data.column.factoryProductionProduct.month", sort = 20)
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 物料编号
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.factoryProductionProduct.productCode", sort = 30)
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.factoryProductionProduct.factoryCode", sort = 50)
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 等级名称
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.factoryProductionProduct.machineCode", sort = 60)
    @TableField(value = "MOLDING_MACHINE_CODE")
    private String moldingMachineCode;

    /**
     * 施工代号
     */
    @TableField(value = "CONSTRUCTION_CODE")
    private String constructionCode;

    /**
     * 分厂成型法
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.factoryProductionProduct.mouldMethod", dictType = "molding_method", sort = 80)
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    @TableField(exist = false)
    private Integer isDelete;

    @TableField(exist = false)
    private String remark;
}