package com.zlt.aps.gdyy.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;

/** 钢带压延生产计划固定模板导入明细。 */
@Data
public class GdyyScheduleResultTemplateImportVO {
    /** Excel明细行号。 */
    private Integer excelRowNum;
    /** 钢带大卷编号。 */
    private String bigRollCode;
    /** 一班计划量。 */
    private Double class1PlanQty;
    /** 二班计划量。 */
    private Double class2PlanQty;
    /** 三班计划量。 */
    private Double class3PlanQty;
    /** 四班计划量。 */
    private Double class4PlanQty;
    /** 五班计划量。 */
    private Double class5PlanQty;
    /** 六班计划量。 */
    private Double class6PlanQty;
    /** 七班计划量。 */
    private Double class7PlanQty;
    /** 八班计划量。 */
    private Double class8PlanQty;

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
            throw new IllegalArgumentException("GDYY导入字段不存在: " + fieldName, exception);
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
            throw new IllegalArgumentException("GDYY导入字段不存在: " + fieldName, exception);
        }
    }
}
