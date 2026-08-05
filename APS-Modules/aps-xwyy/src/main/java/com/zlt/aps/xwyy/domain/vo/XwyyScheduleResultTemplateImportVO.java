package com.zlt.aps.xwyy.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;

/** 纤维压延生产计划固定模板导入明细。 */
@Data
public class XwyyScheduleResultTemplateImportVO extends BaseEntity {
    /** 帘布大卷编号。 */
    @Excel(importName = "bigRollCode")
    private String bigRollCode;
    /** 一班生产顺位。 */
    @Excel(importName = "class1ProduceOrder")
    private BigDecimal class1ProduceOrder;
    /** 一班计划量。 */
    @Excel(importName = "class1PlanQty")
    private BigDecimal class1PlanQty;
    /** 二班生产顺位。 */
    @Excel(importName = "class2ProduceOrder")
    private BigDecimal class2ProduceOrder;
    /** 二班计划量。 */
    @Excel(importName = "class2PlanQty")
    private BigDecimal class2PlanQty;
    /** 三班生产顺位。 */
    @Excel(importName = "class3ProduceOrder")
    private BigDecimal class3ProduceOrder;
    /** 三班计划量。 */
    @Excel(importName = "class3PlanQty")
    private BigDecimal class3PlanQty;
    /** 四班生产顺位。 */
    @Excel(importName = "class4ProduceOrder")
    private BigDecimal class4ProduceOrder;
    /** 四班计划量。 */
    @Excel(importName = "class4PlanQty")
    private BigDecimal class4PlanQty;
    /** 五班生产顺位。 */
    @Excel(importName = "class5ProduceOrder")
    private BigDecimal class5ProduceOrder;
    /** 五班计划量。 */
    @Excel(importName = "class5PlanQty")
    private BigDecimal class5PlanQty;
    /** 六班生产顺位。 */
    @Excel(importName = "class6ProduceOrder")
    private BigDecimal class6ProduceOrder;
    /** 六班计划量。 */
    @Excel(importName = "class6PlanQty")
    private BigDecimal class6PlanQty;
    /** 七班生产顺位。 */
    @Excel(importName = "class7ProduceOrder")
    private BigDecimal class7ProduceOrder;
    /** 七班计划量。 */
    @Excel(importName = "class7PlanQty")
    private BigDecimal class7PlanQty;
    /** 八班生产顺位。 */
    @Excel(importName = "class8ProduceOrder")
    private BigDecimal class8ProduceOrder;
    /** 八班计划量。 */
    @Excel(importName = "class8PlanQty")
    private BigDecimal class8PlanQty;

    /**
     * 按字段模板动态读取班次计划量。
     *
     * @param fieldName Java字段名
     * @return 字段值
     */
    public Serializable getFieldValueByFieldName(String fieldName) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Serializable) field.get(this);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("XWYY导入字段不存在: " + fieldName, exception);
        }
    }
}
