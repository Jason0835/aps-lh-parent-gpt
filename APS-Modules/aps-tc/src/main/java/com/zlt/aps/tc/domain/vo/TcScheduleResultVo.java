package com.zlt.aps.tc.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎侧排程结果专用模板导入导出视图。
 *
 * <p>A:W 为用户可见字段，X:Z 为安全回导所需的结果主键、施工版本和任务版本。
 * 字段顺序与 {@code excelModel/tcScheduleResult.xlsx} 第 1 行和第 5 行占位符一致。</p>
 */
@Data
@ApiModel(value = "胎侧排程结果导入导出实体", description = "胎侧排程结果专用模板导入导出实体")
public class TcScheduleResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 胎侧机台编码（A 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.machineCode")
    private String machineCode;

    /** 胎侧长度（B 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.sidewallLength")
    private BigDecimal sidewallLength;

    /** 成型余量（C 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.cxRemainQty")
    private BigDecimal cxRemainQty;

    /** 胎侧编码（D 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.sidewallCode")
    private String sidewallCode;

    /** 物料描述（E 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.materialDesc")
    private String materialDesc;

    /** 整条胶料组合编码（F 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.wholeGlueCode")
    private String wholeGlueCode;

    /** 库存（G 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.stockQty")
    private BigDecimal stockQty;

    /** 前日 CLASS3 计划量（H 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.lastDayPlanQty")
    private BigDecimal lastDayPlanQty;

    /** 前日 CLASS3 完成量（I 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.lastDayFinishQty")
    private BigDecimal lastDayFinishQty;

    /** 前日 CLASS3 顺序（J 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.lastDaySequence")
    private Integer lastDaySequence;

    /** CLASS1 计划量（K 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.class1PlanQty")
    private BigDecimal class1PlanQty;

    /** CLASS1 完成量（L 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.class1FinishQty")
    private BigDecimal class1FinishQty;

    /** CLASS1 顺序（M 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.class1Sequence")
    private Integer class1Sequence;

    /** CLASS2 计划量（N 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.class2PlanQty")
    private BigDecimal class2PlanQty;

    /** CLASS2 完成量（O 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.class2FinishQty")
    private BigDecimal class2FinishQty;

    /** CLASS2 顺序（P 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.class2Sequence")
    private Integer class2Sequence;

    /** CLASS3 计划量（Q 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.class3PlanQty")
    private BigDecimal class3PlanQty;

    /** CLASS3 完成量（R 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.class3FinishQty")
    private BigDecimal class3FinishQty;

    /** CLASS3 顺序（S 列）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.class3Sequence")
    private Integer class3Sequence;

    /** 成型计划产量（T 列，仅展示）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.cxPlanQty")
    private BigDecimal cxPlanQty;

    /** 卷曲长度（U 列，仅展示）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.curlRollLength")
    private BigDecimal curlRollLength;

    /** 成型机台编码（V 列，仅展示）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.cxMachineCode")
    private String cxMachineCode;

    /** 收尾类型（W 列，仅展示）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.type")
    private String type;

    /** 排程结果主键（X 列，隐藏）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.resultId")
    private Long resultId;

    /** 胎侧施工版本（Y 列，隐藏）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.constructionVersion")
    private String constructionVersion;

    /** 人工操作并发版本（Z 列，隐藏）。 */
    @Excel(name = "ui.data.column.tc.scheduleResultExcel.taskVersion")
    private Long taskVersion;

    /** Excel 行号，仅导入校验使用。 */
    @ApiModelProperty(value = "Excel行号")
    private Integer rowNum;

    /** 当前行错误，仅导入校验使用。 */
    @ApiModelProperty(value = "行错误信息")
    private List<String> errors = new ArrayList<>();

    /**
     * 按字段名读取同规则命名的班次字段。
     *
     * @param fieldName 字段名
     * @return 字段值
     * @throws IllegalArgumentException 字段不存在或无法访问时抛出
     */
    public Object getFieldValueByFieldName(String fieldName) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalArgumentException("无法读取胎侧Excel字段: " + fieldName, exception);
        }
    }
}
