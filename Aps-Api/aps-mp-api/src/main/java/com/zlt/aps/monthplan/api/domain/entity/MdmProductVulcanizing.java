package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 基础数据-硫化机正在生产品种(钭交厂使用）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "T_MDM_PRODUCT_VULCANIZING")
public class MdmProductVulcanizing extends BaseEntity implements Serializable {

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.vulcanization.factoryCode", dictType = "biz_factory_name")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 9999)
    @Excel(name = "ui.data.column.vulcanization.year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @Excel(name = "ui.data.column.vulcanization.month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 硫化机ID
     */
    // @Excel(name = "ui.data.column.vulcanization.vulcanizingMachineId")
    @NotNull
    @TableField(value = "VULCANIZING_MACHINE_ID")
    private Long vulcanizingMachineId;

    /**
     * 硫化机编号
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 30)
    @Excel(name = "ui.data.column.vulcanization.vulcanizingMachineCode")
    @NotNull
    @TableField(value = "VULCANIZING_MACHINE_CODE")
    private String vulcanizingMachineCode;

    /**
     * 物料编号
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.vulcanization.productCode")
    @NotNull
    @TableField(value = "PRODUCT_CODE")
    private String productCode;


    /**
     * 品名
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 64)
    @Excel(name = "ui.data.column.docVulcanizingMachine.productTypeCode", dictType = "biz_product_type")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 模具编号
     */
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.vulcanization.mouldCode")
    @NotNull
    @TableField(value = "MOULD_CODE")
    private String mouldCode;
}
