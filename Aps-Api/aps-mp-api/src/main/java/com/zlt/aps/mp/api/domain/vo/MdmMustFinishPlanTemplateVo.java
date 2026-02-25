package com.zlt.aps.mp.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 必须保证的客户月计划对象-导入模板
 */
@Data
public class MdmMustFinishPlanTemplateVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.mustFinishPlan.factoryCode", dictType = "biz_factory_name", sort = 10)
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.mustFinishPlan.year", sort = 50)
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.mustFinishPlan.month", sort = 60)
    private Integer month;

    /**
     * 客户编号
     */
    @Excel(name = "ui.data.column.mustFinishPlan.customCode", sort = 20)
    private String customCode;

    /**
     * 物料编号
     */
    @Excel(name = "ui.data.column.mustFinishPlan.productCode", sort = 70)
    private String productCode;

    /**
     * 库位类别
     */
    @Excel(name = "ui.data.column.mustFinishPlan.locationType", dictType = "biz_stor_type", sort = 90)
    private String locationType;
}
