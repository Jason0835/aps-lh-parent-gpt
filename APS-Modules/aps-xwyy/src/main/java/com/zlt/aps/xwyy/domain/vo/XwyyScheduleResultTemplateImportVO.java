package com.zlt.aps.xwyy.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;

/** 纤维压延生产计划固定模板导入明细。 */
@Data
public class XwyyScheduleResultTemplateImportVO {
    /** Excel明细行号。 */
    private Integer excelRowNum;
    /** 帘布大卷编号。 */
    private String bigRollCode;
    /** 一班计划量。 */
    private BigDecimal class1PlanQty;
    /** 二班计划量。 */
    private BigDecimal class2PlanQty;
    /** 三班计划量。 */
    private BigDecimal class3PlanQty;
    /** 四班计划量。 */
    private BigDecimal class4PlanQty;
    /** 五班计划量。 */
    private BigDecimal class5PlanQty;
    /** 六班计划量。 */
    private BigDecimal class6PlanQty;
    /** 七班计划量。 */
    private BigDecimal class7PlanQty;
    /** 八班计划量。 */
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

    /**
     * 按字段模板动态设置班次计划量。
     *
     * @param fieldName Java字段名
     * @param value 字段值
     */
    public void setFieldValueByFieldName(String fieldName, Object value) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(this, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("XWYY导入字段不存在: " + fieldName, exception);
        }
    }
}
