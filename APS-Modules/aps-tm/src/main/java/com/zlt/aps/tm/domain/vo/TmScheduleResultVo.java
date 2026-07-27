package com.zlt.aps.tm.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎面排程结果模板导入导出视图。
 *
 * <p>字段名与专用模板 {@code excelModel/tmScheduleResult.xlsx} 第 1 行（隐藏元数据行）
 * 的 {@code {fieldName}} 占位符、第 5 行 {@code {.fieldName}} 列表占位符一一对应。
 * 导出时由 {@code setExportTitleFieldName} 将 {@link Excel#name()} 的国际化回写值写入第 1 行；
 * 导入时按第 1 行国际化表头匹配列号，逐行解析为本对象列表。</p>
 *
 * <p>列含义（以当前模板为准）：A 胎面机台、B 胎面长度、C 成型余量、D 胎面编码、E 物料描述、
 * F 胶种、G 库存、H:I 前日计划/完成、J 前日顺序、K:M 1 班、N:P 2 班、Q:S 3 班（各含计划/完成/顺序）、
 * T 成型产量、U 卷曲长度、V 成型机台、W 类型。</p>
 */
@ApiModel(value = "胎面排程结果导入导出实体", description = "胎面排程结果模板导入导出实体")
@Data
public class TmScheduleResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 胎面机台编码（A 列，隐藏第 1 行表头匹配用）。 */
    @ApiModelProperty(value = "胎面机台编码")
    @Excel(name = "ui.data.column.tm.scheduleResult.machineCode")
    private String machineCode;

    /** 胎面长度（B 列）。 */
    @ApiModelProperty(value = "胎面长度")
    @Excel(name = "ui.data.column.tm.scheduleResult.treadShoulderLength")
    private BigDecimal treadLength;

    /** 成型余量（C 列）。 */
    @ApiModelProperty(value = "成型余量")
    @Excel(name = "ui.data.column.tm.scheduleResult.cxRemainQty")
    private BigDecimal cxRemainQty;

    /** 胎面编码（D 列）。 */
    @ApiModelProperty(value = "胎面编码")
    @Excel(name = "ui.data.column.tm.scheduleResult.treadCode")
    private String treadCode;

    /** 物料描述（E 列）。 */
    @ApiModelProperty(value = "物料描述")
    @Excel(name = "ui.data.column.tm.scheduleResult.materialDesc")
    private String materialDesc;

    /** 整条胶料组合编码（F 列）。 */
    @ApiModelProperty(value = "整条胶料组合编码")
    @Excel(name = "ui.data.column.tm.scheduleResult.wholeGlueCode")
    private String wholeGlueCode;

    /** 库存（G 列）。 */
    @ApiModelProperty(value = "库存")
    @Excel(name = "ui.data.column.tm.scheduleResult.sixClockStockQty")
    private BigDecimal stockQty;

    /** 前日计划量（H 列）。 */
    @ApiModelProperty(value = "前日计划量")
    @Excel(name = "ui.data.column.tm.scheduleResult.lastDayPlanQty")
    private BigDecimal lastDayPlanQty;

    /** 前日完成量（I 列）。 */
    @ApiModelProperty(value = "前日完成量")
    @Excel(name = "ui.data.column.tm.scheduleResult.lastDayFinishQty")
    private BigDecimal lastDayFinishQty;

    /** 前日顺序（J 列）。 */
    @ApiModelProperty(value = "前日顺序")
    @Excel(name = "ui.data.column.tm.scheduleResult.lastDaySequence")
    private Integer lastDaySequence;

    /** 1 班计划量（K 列）。 */
    @ApiModelProperty(value = "1班计划量")
    @Excel(name = "ui.data.column.tm.scheduleResult.class1PlanQty")
    private BigDecimal class1PlanQty;

    /** 1 班完成量（L 列）。 */
    @ApiModelProperty(value = "1班完成量")
    @Excel(name = "ui.data.column.tm.scheduleResult.class1FinishQty")
    private BigDecimal class1FinishQty;

    /** 1 班顺序（M 列）。 */
    @ApiModelProperty(value = "1班顺序")
    @Excel(name = "ui.data.column.tm.scheduleResult.class1Sequence")
    private Integer class1Sequence;

    /** 2 班计划量（N 列）。 */
    @ApiModelProperty(value = "2班计划量")
    @Excel(name = "ui.data.column.tm.scheduleResult.class2PlanQty")
    private BigDecimal class2PlanQty;

    /** 2 班完成量（O 列）。 */
    @ApiModelProperty(value = "2班完成量")
    @Excel(name = "ui.data.column.tm.scheduleResult.class2FinishQty")
    private BigDecimal class2FinishQty;

    /** 2 班顺序（P 列）。 */
    @ApiModelProperty(value = "2班顺序")
    @Excel(name = "ui.data.column.tm.scheduleResult.class2Sequence")
    private Integer class2Sequence;

    /** 3 班计划量（Q 列）。 */
    @ApiModelProperty(value = "3班计划量")
    @Excel(name = "ui.data.column.tm.scheduleResult.class3PlanQty")
    private BigDecimal class3PlanQty;

    /** 3 班完成量（R 列）。 */
    @ApiModelProperty(value = "3班完成量")
    @Excel(name = "ui.data.column.tm.scheduleResult.class3FinishQty")
    private BigDecimal class3FinishQty;

    /** 3 班顺序（S 列）。 */
    @ApiModelProperty(value = "3班顺序")
    @Excel(name = "ui.data.column.tm.scheduleResult.class3Sequence")
    private Integer class3Sequence;

    /** 成型产量（T 列，导出汇总展示用，不落库）。 */
    @ApiModelProperty(value = "成型产量")
    @Excel(name = "ui.data.column.tm.scheduleResult.cxPlanQty")
    private BigDecimal cxPlanQty;

    /** 卷曲长度（U 列）。 */
    @ApiModelProperty(value = "卷曲长度")
    @Excel(name = "ui.data.column.tm.scheduleResult.curlRollLength")
    private BigDecimal curlRollLength;

    /** 成型机台编码（V 列）。 */
    @ApiModelProperty(value = "成型机台编码")
    @Excel(name = "ui.data.column.tm.scheduleResult.cxMachineCode")
    private String cxMachineCode;

    /** 类型（W 列，收尾标识，导出展示用）。 */
    @ApiModelProperty(value = "类型")
    @Excel(name = "ui.data.column.tm.scheduleResult.type")
    private String type;

    /** Excel 行号（从 1 开始），仅导入行级校验使用，不参与模板解析。 */
    @ApiModelProperty(value = "Excel行号")
    private Integer rowNum;

    /** 当前行解析和业务校验错误，仅导入行级校验使用，不参与模板解析。 */
    @ApiModelProperty(value = "行错误信息")
    private List<String> errors = new ArrayList<>();
}
