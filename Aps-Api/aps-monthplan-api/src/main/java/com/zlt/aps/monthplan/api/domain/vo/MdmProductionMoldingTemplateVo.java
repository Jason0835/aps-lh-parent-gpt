package com.zlt.aps.monthplan.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;

/**
 * 分厂成型正在生产的品种-导入模板
 */
@Data
public class MdmProductionMoldingTemplateVo extends BaseEntity {

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
     * 分厂成型法
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.factoryProductionProduct.mouldMethod", dictType = "molding_method", sort = 80)
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;
}