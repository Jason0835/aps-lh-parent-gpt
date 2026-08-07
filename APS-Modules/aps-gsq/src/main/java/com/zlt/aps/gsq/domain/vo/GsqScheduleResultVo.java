package com.zlt.aps.gsq.domain.vo;

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
 * 钢丝圈排程结果专用模板导入导出视图。
 *
 * <p>字段名与专用模板 {@code excelModel/gsqScheduleResult.xlsx} 第 1 行（隐藏元数据行）
 * 的 {@code {fieldName}} 占位符、第 5 行 {@code {.fieldName}} 列表占位符一一对应。
 * 导出时由 {@code setExportTitleFieldName} 将 {@link Excel#name()} 的国际化回写值写入第 1 行；
 * 导入时按第 1 行国际化表头匹配列号，逐行解析为本对象列表。</p>
 *
 * <p>列含义（以当前模板为准）：A 钢丝圈代码、B 个数（导出忽略，导入不解析）、C 成型余量、
 * D 物料描述、E 规格（对应实体 proSize）、F 库存、G:H 前日计划/完成、
 * I:J 1 班、K:L 2 班、M:N 3 班（各含计划/完成）、O 成型产量、P 标准要求、Q 成型机台、
 * R 钢丝圈机台（新增）、S 排程结果主键（隐藏，安全回导用）。</p>
 *
 * @author APS
 */
@Data
@ApiModel(value = "钢丝圈排程结果导入导出实体", description = "钢丝圈排程结果专用模板导入导出实体")
public class GsqScheduleResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 钢丝圈代码（A 列）。 */
    @ApiModelProperty(value = "钢丝圈代码")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.steelRingCode")
    private String steelRingCode;

    /** 个数（B 列，导出时暂不填充，导入时不解析）。 */
    @ApiModelProperty(value = "个数")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.count")
    private BigDecimal count;

    /** 成型余量（C 列）。 */
    @ApiModelProperty(value = "成型余量")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.cxRemainQty")
    private BigDecimal cxRemainQty;

    /** 物料描述（D 列）。 */
    @ApiModelProperty(value = "物料描述")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.materialDesc")
    private String materialDesc;

    /** 规格（E 列，对应实体 proSize）。 */
    @ApiModelProperty(value = "规格")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.specifications")
    private String specifications;

    /** 库存（F 列）。 */
    @ApiModelProperty(value = "库存")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.stockQty")
    private BigDecimal stockQty;

    /** 前日计划量（G 列）。 */
    @ApiModelProperty(value = "前日计划量")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.lastDayPlanQty")
    private BigDecimal lastDayPlanQty;

    /** 前日完成量（H 列）。 */
    @ApiModelProperty(value = "前日完成量")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.lastDayFinishQty")
    private BigDecimal lastDayFinishQty;

    /** 1 班计划量（I 列）。 */
    @ApiModelProperty(value = "1班计划量")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.class1PlanQty")
    private BigDecimal class1PlanQty;

    /** 1 班完成量（J 列）。 */
    @ApiModelProperty(value = "1班完成量")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.class1FinishQty")
    private BigDecimal class1FinishQty;

    /** 2 班计划量（K 列）。 */
    @ApiModelProperty(value = "2班计划量")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.class2PlanQty")
    private BigDecimal class2PlanQty;

    /** 2 班完成量（L 列）。 */
    @ApiModelProperty(value = "2班完成量")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.class2FinishQty")
    private BigDecimal class2FinishQty;

    /** 3 班计划量（M 列）。 */
    @ApiModelProperty(value = "3班计划量")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.class3PlanQty")
    private BigDecimal class3PlanQty;

    /** 3 班完成量（N 列）。 */
    @ApiModelProperty(value = "3班完成量")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.class3FinishQty")
    private BigDecimal class3FinishQty;

    /** 成型产量（O 列，导出汇总展示用，不落库）。 */
    @ApiModelProperty(value = "成型产量")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.cxPlanQty")
    private BigDecimal cxPlanQty;

    /** 标准要求/卷曲长度（P 列，暂放空）。 */
    @ApiModelProperty(value = "标准要求")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.curlRollLength")
    private BigDecimal curlRollLength;

    /** 成型机台编码（Q 列，仅展示）。 */
    @ApiModelProperty(value = "成型机台编码")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.cxMachineCode")
    private String cxMachineCode;

    /** 钢丝圈机台编码（R 列，新增，导入时用于定位/新增排程）。 */
    @ApiModelProperty(value = "钢丝圈机台编码")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.machineCode")
    private String machineCode;

    /** 排程结果主键（S 列，隐藏，安全回导用）。 */
    @ApiModelProperty(value = "排程结果主键")
    @Excel(name = "ui.data.column.gsq.scheduleResultExcel.resultId")
    private Long resultId;

    /** Excel 行号（从 1 开始），仅导入行级校验使用，不参与模板解析。 */
    @ApiModelProperty(value = "Excel行号")
    private Integer rowNum;

    /** 当前行解析和业务校验错误，仅导入行级校验使用，不参与模板解析。 */
    @ApiModelProperty(value = "行错误信息")
    private List<String> errors = new ArrayList<>();

    /**
     * 按字段名动态读取同规则命名的班次字段。
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
            throw new IllegalArgumentException("无法读取钢丝圈Excel字段: " + fieldName, exception);
        }
    }
}