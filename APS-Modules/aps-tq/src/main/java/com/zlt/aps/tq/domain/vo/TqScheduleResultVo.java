package com.zlt.aps.tq.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎圈排程结果专用模板导入导出视图。
 *
 * <p>字段名与专用模板 {@code excelModel/tqScheduleResult.xlsx} 第 1 行（隐藏元数据行）
 * 的 {@code {fieldName}} 占位符、第 5 行 {@code {.fieldName}} 列表占位符一一对应。
 * 导出时由 {@code setExportTitleFieldName} 将 {@link Excel#name()} 的国际化回写值写入第 1 行；
 * 导入时按第 1 行国际化表头匹配列号，逐行解析为本对象列表。</p>
 *
 * <p>列含义（以当前模板为准）：A 胎圈代码、B 个数（导出忽略，导入不解析）、C 成型余量、
 * D 物料描述、E 规格、F 胶种、G 胎圈机台、H 库存、I:J 前日计划/完成、
 * K:L 1 班、M:N 2 班、O:P 3 班（各含计划/完成）、Q 成型产量、R 标准要求、S 成型机台、T 状态。</p>
 *
 * @author APS
 */
@Data
@ApiModel(value = "胎圈排程结果导入导出实体", description = "胎圈排程结果模板导入导出实体")
public class TqScheduleResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 胎圈代码（A 列，隐藏第 1 行表头匹配用）。 */
    @ApiModelProperty(value = "胎圈代码")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.beadCode")
    private String beadCode;

    /** 个数（B 列，导出时暂不填充，导入时不解析）。 */
    @ApiModelProperty(value = "个数")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.count")
    private BigDecimal count;

    /** 成型余量（C 列）。 */
    @ApiModelProperty(value = "成型余量")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.cxRemainQty")
    private BigDecimal cxRemainQty;

    /** 物料描述（D 列）。 */
    @ApiModelProperty(value = "物料描述")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.materialDesc")
    private String materialDesc;

    /** 规格（E 列，对应实体 proSize）。 */
    @ApiModelProperty(value = "规格")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.specifications")
    private String specifications;

    /** 整条胶料组合编码（F 列）。 */
    @ApiModelProperty(value = "整条胶料组合编码")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.wholeGlueCode")
    private String wholeGlueCode;

    /** 胎圈机台编码（G 列）。 */
    @ApiModelProperty(value = "胎圈机台编码")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.machineCode")
    private String machineCode;

    /** 库存（H 列）。 */
    @ApiModelProperty(value = "库存")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.stockQty")
    private BigDecimal stockQty;

    /** 前日计划量（I 列）。 */
    @ApiModelProperty(value = "前日计划量")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.lastDayPlanQty")
    private BigDecimal lastDayPlanQty;

    /** 前日完成量（J 列）。 */
    @ApiModelProperty(value = "前日完成量")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.lastDayFinishQty")
    private BigDecimal lastDayFinishQty;

    /** 1 班计划量（K 列）。 */
    @ApiModelProperty(value = "1班计划量")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.class1PlanQty")
    private BigDecimal class1PlanQty;

    /** 1 班完成量（L 列）。 */
    @ApiModelProperty(value = "1班完成量")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.class1FinishQty")
    private BigDecimal class1FinishQty;

    /** 2 班计划量（M 列）。 */
    @ApiModelProperty(value = "2班计划量")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.class2PlanQty")
    private BigDecimal class2PlanQty;

    /** 2 班完成量（N 列）。 */
    @ApiModelProperty(value = "2班完成量")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.class2FinishQty")
    private BigDecimal class2FinishQty;

    /** 3 班计划量（O 列）。 */
    @ApiModelProperty(value = "3班计划量")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.class3PlanQty")
    private BigDecimal class3PlanQty;

    /** 3 班完成量（P 列）。 */
    @ApiModelProperty(value = "3班完成量")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.class3FinishQty")
    private BigDecimal class3FinishQty;

    /** 成型产量（Q 列，导出汇总展示用，不落库）。 */
    @ApiModelProperty(value = "成型产量")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.cxPlanQty")
    private BigDecimal cxPlanQty;

    /** 标准要求/卷曲长度（R 列，暂放空）。 */
    @ApiModelProperty(value = "标准要求")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.curlRollLength")
    private BigDecimal curlRollLength;

    /** 成型机台编码（S 列，仅展示）。 */
    @ApiModelProperty(value = "成型机台编码")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.cxMachineCode")
    private String cxMachineCode;

    /** 类型（T 列，收尾标识，导出展示用）。 */
    @ApiModelProperty(value = "类型")
    @Excel(name = "ui.data.column.tq.scheduleResultExcel.type")
    private String type;

    /** 排程结果主键（隐藏列，安全回导用）。
     *  当前模板未包含此列，导入时为 null 表示新增。 */
    @ApiModelProperty(value = "排程结果主键")
    private Long resultId;

    /** 人工操作并发版本（隐藏列，安全回导用）。
     *  当前模板未包含此列，导入时为 null 表示新增。 */
    @ApiModelProperty(value = "任务版本号")
    private Long taskVersion;

    /** Excel 行号（从 1 开始），仅导入行级校验使用，不参与模板解析。 */
    @ApiModelProperty(value = "Excel行号")
    private Integer rowNum;

    /** 当前行解析和业务校验错误，仅导入行级校验使用，不参与模板解析。 */
    @ApiModelProperty(value = "行错误信息")
    private List<String> errors = new ArrayList<>();
}
