package com.zlt.aps.cd90.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;

/** CD90固定生产计划模板导入明细。 */
@Data
public class Cd90ScheduleResultTemplateImportVO extends BaseEntity {
    /** 机台编码。 */
    @ImportExcelValidated(required = true)
    @Excel(importName = "machineCode")
    private String machineCode;
    /** 单耗，模板单位为米。 */
    @Excel(importName = "unitConsume")
    private BigDecimal unitConsume;
    /** 月计划剩余量。 */
    @Excel(importName = "planSurplusQty")
    private BigDecimal planSurplusQty;
    /** 帘布代号。 */
    @ImportExcelValidated(required = true)
    @Excel(importName = "clothCode")
    private String clothCode;
    /** 帘布大卷编号。 */
    @ImportExcelValidated(required = true)
    @Excel(importName = "bigRollCode")
    private String bigRollCode;
    /** 库排号。 */
    @Excel(importName = "storageLaneCode")
    private String storageLaneCode;
    /** 一班计划量。 */
    @Excel(importName = "class1PlanQty")
    private Double class1PlanQty;
    /** 一班完成量。 */
    @Excel(importName = "class1FinishQty")
    private Double class1FinishQty;
    /** 二班计划量。 */
    @Excel(importName = "class2PlanQty")
    private Double class2PlanQty;
    /** 二班完成量。 */
    @Excel(importName = "class2FinishQty")
    private Double class2FinishQty;
    /** 三班计划量。 */
    @Excel(importName = "class3PlanQty")
    private Double class3PlanQty;
    /** 三班完成量。 */
    @Excel(importName = "class3FinishQty")
    private Double class3FinishQty;
    /** 成型机台编码，多个值使用逗号分隔。 */
    @Excel(importName = "cxMachineCodes")
    private String cxMachineCodes;

    /**
     * 按字段模板动态读取班次值。
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
            throw new IllegalArgumentException("CD90导入字段不存在: " + fieldName, exception);
        }
    }
}
