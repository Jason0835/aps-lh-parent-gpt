package com.zlt.aps.cx.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 成型排程结果表（单表结构）
 *
 * <p>每条记录代表一个成型机台+胎胚+物料编号在排程周期内的8班次排产计划。
 * 不再使用主子表结构，子表字段拍平到 CLASS1~8 对应的班次列中。
 *
 * <p>每个班次包含以下维度（原子表字段）：
 * <ul>
 *   <li>PLAN_QTY - 计划数</li>
 *   <li>TRIP_NO - 车次号</li>
 *   <li>TRIP_CAPACITY - 本车次容量（整车条数）</li>
 *   <li>STOCK_HOURS - 库存可供硫化时长</li>
 *   <li>SEQUENCE - 顺位</li>
 *   <li>PLAN_START_TIME - 计划开始时间</li>
 *   <li>PLAN_END_TIME - 计划结束时间</li>
 *   <li>ANALYSIS_INPUT - 原因分析手工输入</li>
 *   <li>FINISH_QTY - 完成量</li>
 *   <li>ANALYSIS - 原因分析</li>
 *   <li>RECIPE_TYPE - 示方书类型</li>
 *   <li>RECIPE_NO - 示方书编号</li>
 * </ul>
 *
 * <p>对应表：T_CX_SCHEDULE_RESULT
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_SCHEDULE_RESULT")
@ApiModel(value = "成型排程结果对象", description = "成型排程结果表（单表）")
public class CxScheduleResult extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /** 分厂编码 */
    @Excel(name = "ui.data.column.cxPrecisionPlan.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编码")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.cxScheduleResult.cxBatchNo")
    @ApiModelProperty(value = "成型批次号")
    @TableField("CX_BATCH_NO")
    private String cxBatchNo;

    @ApiModelProperty(value = "生产状态：0-未生产；1-生产中；2-已收尾")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    @Excel(name = "ui.data.column.cxScheduleResult.scheduleDate", dateFormat = "yyyy-MM-dd")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;

    @Excel(name = "ui.data.column.cxScheduleResult.orderNo")
    @ApiModelProperty(value = "工单号")
    @TableField("ORDER_NO")
    private String orderNo;

    @Excel(name = "ui.data.column.cxScheduleResult.isRelease", dictType = "IS_RELEASE")
    @ApiModelProperty(value = "是否发布：0--未发布，1--已发布")
    @TableField("IS_RELEASE")
    private String isRelease;

    @Excel(name = "ui.data.column.cxScheduleResult.cxMachineCode")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "成型机台编号")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    @ApiModelProperty(value = "成型机台名称")
    @TableField("CX_MACHINE_NAME")
    private String cxMachineName;

    @ApiModelProperty(value = "成型机台类型")
    @TableField("CX_MACHINE_TYPE")
    private String cxMachineType;

    @ApiModelProperty(value = "硫化排程任务序号")
    @TableField("LH_SCHEDULE_IDS")
    private String lhScheduleIds;

    @Excel(name = "ui.data.column.cxScheduleResult.lhMachineCode")
    @ApiModelProperty(value = "硫化机台编号")
    @TableField("LH_MACHINE_CODE")
    private String lhMachineCode;

    @ApiModelProperty(value = "硫化机台名称")
    @TableField("LH_MACHINE_NAME")
    private String lhMachineName;

    //@Excel(name = "ui.data.column.cxScheduleResult.lhMachineQty")
    @ApiModelProperty(value = "硫化机使用总模数")
    @TableField("LH_MACHINE_QTY")
    private BigDecimal lhMachineQty;

    @Excel(name = "ui.data.column.cxScheduleResult.materialCode")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "物料编号", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    @Excel(name = "ui.data.column.cxScheduleResult.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    @Excel(name = "ui.data.column.cxScheduleResult.embryoCode")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "胎胚代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @Excel(name = "ui.data.column.cxScheduleResult.mainMaterialDesc")
    @ApiModelProperty(value = "主物料(胎胚描述)", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    @ApiModelProperty(value = "胎胚寸口")
    @TableField("SPEC_DIMENSION")
    private BigDecimal specDimension;

    //@Excel(name = "ui.data.column.cxScheduleResult.structureName")
    @ApiModelProperty(value = "结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @Excel(name = "ui.data.column.cxScheduleResult.totalStock")
    @ApiModelProperty(value = "胎胚库存")
    @TableField("TOTAL_STOCK")
    private BigDecimal totalStock;

    //@Excel(name = "ui.data.column.cxScheduleResult.bomDataVersion")
    @ApiModelProperty(value = "施工版本信息")
    @TableField("BOM_DATA_VERSION")
    private String bomDataVersion;

    @ApiModelProperty(value = "胎胚总计划量")
    @TableField("PRODUCT_NUM")
    private BigDecimal productNum;

    @Excel(name = "ui.data.column.cxScheduleResult.cxRemainQty")
    @ApiModelProperty(value = "成型余量")
    @TableField("CX_REMAIN_QTY")
    private BigDecimal cxRemainQty;

    @Excel(name = "ui.data.column.cxScheduleResult.lhRemainQty")
    @ApiModelProperty(value = "硫化余量")
    @TableField("LH_REMAIN_QTY")
    private BigDecimal lhRemainQty;

    @Excel(name = "ui.data.column.cxScheduleResult.lhClassQty")
    @ApiModelProperty(value = "硫化班产")
    @TableField("LH_CLASS_QTY")
    private BigDecimal lhClassQty;

    // ==================== 一班 ====================
    @Excel(name = "ui.data.column.cxScheduleResult.class1PlanQty")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "一班计划数")
    @TableField("CLASS1_PLAN_QTY")
    private BigDecimal class1PlanQty;

    @ApiModelProperty(value = "一班原因分析手工输入")
    @TableField("CLASS1_ANALYSIS_INPUT")
    private String class1AnalysisInput;

    @Excel(name = "ui.data.column.cxScheduleResult.class1FinishQty")
    @ApiModelProperty(value = "一班完成量")
    @TableField("CLASS1_FINISH_QTY")
    private BigDecimal class1FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class1Analysis")
    @ApiModelProperty(value = "一班原因分析")
    @TableField("CLASS1_ANALYSIS")
    private String class1Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class1RecipeType", dictType = "trial_status")
    @ApiModelProperty(value = "一班示方书类型")
    @TableField("CLASS1_RECIPE_TYPE")
    private String class1RecipeType;

    @ApiModelProperty(value = "一班示方书编号")
    @TableField("CLASS1_RECIPE_NO")
    private String class1RecipeNo;

    // ==================== 二班 ====================
    @Excel(name = "ui.data.column.cxScheduleResult.class2PlanQty")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "二班计划数")
    @TableField("CLASS2_PLAN_QTY")
    private BigDecimal class2PlanQty;

    @ApiModelProperty(value = "二班原因分析手工输入")
    @TableField("CLASS2_ANALYSIS_INPUT")
    private String class2AnalysisInput;

    @Excel(name = "ui.data.column.cxScheduleResult.class2FinishQty")
    @ApiModelProperty(value = "二班完成量")
    @TableField("CLASS2_FINISH_QTY")
    private BigDecimal class2FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class2Analysis")
    @ApiModelProperty(value = "二班原因分析")
    @TableField("CLASS2_ANALYSIS")
    private String class2Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class2RecipeType", dictType = "trial_status")
    @ApiModelProperty(value = "二班示方书类型")
    @TableField("CLASS2_RECIPE_TYPE")
    private String class2RecipeType;

    @ApiModelProperty(value = "二班示方书编号")
    @TableField("CLASS2_RECIPE_NO")
    private String class2RecipeNo;

    // ==================== 三班 ====================
    @Excel(name = "ui.data.column.cxScheduleResult.class3PlanQty")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "三班计划数")
    @TableField("CLASS3_PLAN_QTY")
    private BigDecimal class3PlanQty;

    @ApiModelProperty(value = "三班原因分析手工输入")
    @TableField("CLASS3_ANALYSIS_INPUT")
    private String class3AnalysisInput;

    @Excel(name = "ui.data.column.cxScheduleResult.class3FinishQty")
    @ApiModelProperty(value = "三班完成量")
    @TableField("CLASS3_FINISH_QTY")
    private BigDecimal class3FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class3Analysis")
    @ApiModelProperty(value = "三班原因分析")
    @TableField("CLASS3_ANALYSIS")
    private String class3Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class3RecipeType", dictType = "trial_status")
    @ApiModelProperty(value = "三班示方书类型")
    @TableField("CLASS3_RECIPE_TYPE")
    private String class3RecipeType;

    @ApiModelProperty(value = "三班示方书编号")
    @TableField("CLASS3_RECIPE_NO")
    private String class3RecipeNo;

    // ==================== 四班 ====================
    @Excel(name = "ui.data.column.cxScheduleResult.class4PlanQty")
    @ApiModelProperty(value = "四班计划数")
    @TableField("CLASS4_PLAN_QTY")
    private BigDecimal class4PlanQty;

    @ApiModelProperty(value = "四班原因分析手工输入")
    @TableField("CLASS4_ANALYSIS_INPUT")
    private String class4AnalysisInput;

    @Excel(name = "ui.data.column.cxScheduleResult.class4FinishQty")
    @ApiModelProperty(value = "四班完成量")
    @TableField("CLASS4_FINISH_QTY")
    private BigDecimal class4FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class4Analysis")
    @ApiModelProperty(value = "四班原因分析")
    @TableField("CLASS4_ANALYSIS")
    private String class4Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class4RecipeType", dictType = "trial_status")
    @ApiModelProperty(value = "四班示方书类型")
    @TableField("CLASS4_RECIPE_TYPE")
    private String class4RecipeType;

    //@Excel(name = "ui.data.column.cxScheduleResult.class4RecipeNo")
    @ApiModelProperty(value = "四班示方书编号")
    @TableField("CLASS4_RECIPE_NO")
    private String class4RecipeNo;

    // ==================== 五班 ====================
    @Excel(name = "ui.data.column.cxScheduleResult.class5PlanQty")
    @ApiModelProperty(value = "五班计划数")
    @TableField("CLASS5_PLAN_QTY")
    private BigDecimal class5PlanQty;

    @ApiModelProperty(value = "五班原因分析手工输入")
    @TableField("CLASS5_ANALYSIS_INPUT")
    private String class5AnalysisInput;

    @Excel(name = "ui.data.column.cxScheduleResult.class5FinishQty")
    @ApiModelProperty(value = "五班完成量")
    @TableField("CLASS5_FINISH_QTY")
    private BigDecimal class5FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class5Analysis")
    @ApiModelProperty(value = "五班原因分析")
    @TableField("CLASS5_ANALYSIS")
    private String class5Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class5RecipeType", dictType = "trial_status")
    @ApiModelProperty(value = "五班示方书类型")
    @TableField("CLASS5_RECIPE_TYPE")
    private String class5RecipeType;

    @ApiModelProperty(value = "五班示方书编号")
    @TableField("CLASS5_RECIPE_NO")
    private String class5RecipeNo;

    // ==================== 六班 ====================
    @Excel(name = "ui.data.column.cxScheduleResult.class6PlanQty")
    @ApiModelProperty(value = "六班计划数")
    @TableField("CLASS6_PLAN_QTY")
    private BigDecimal class6PlanQty;

    @ApiModelProperty(value = "六班原因分析手工输入")
    @TableField("CLASS6_ANALYSIS_INPUT")
    private String class6AnalysisInput;

    @Excel(name = "ui.data.column.cxScheduleResult.class6FinishQty")
    @ApiModelProperty(value = "六班完成量")
    @TableField("CLASS6_FINISH_QTY")
    private BigDecimal class6FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class6Analysis")
    @ApiModelProperty(value = "六班原因分析")
    @TableField("CLASS6_ANALYSIS")
    private String class6Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class6RecipeType", dictType = "trial_status")
    @ApiModelProperty(value = "六班示方书类型")
    @TableField("CLASS6_RECIPE_TYPE")
    private String class6RecipeType;

    @ApiModelProperty(value = "六班示方书编号")
    @TableField("CLASS6_RECIPE_NO")
    private String class6RecipeNo;

    // ==================== 七班 ====================
    @Excel(name = "ui.data.column.cxScheduleResult.class7PlanQty")
    @ApiModelProperty(value = "七班计划数")
    @TableField("CLASS7_PLAN_QTY")
    private BigDecimal class7PlanQty;

    @ApiModelProperty(value = "七班原因分析手工输入")
    @TableField("CLASS7_ANALYSIS_INPUT")
    private String class7AnalysisInput;

    @Excel(name = "ui.data.column.cxScheduleResult.class7FinishQty")
    @ApiModelProperty(value = "七班完成量")
    @TableField("CLASS7_FINISH_QTY")
    private BigDecimal class7FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class7Analysis")
    @ApiModelProperty(value = "七班原因分析")
    @TableField("CLASS7_ANALYSIS")
    private String class7Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class7RecipeType", dictType = "trial_status")
    @ApiModelProperty(value = "七班示方书类型")
    @TableField("CLASS7_RECIPE_TYPE")
    private String class7RecipeType;

    @ApiModelProperty(value = "七班示方书编号")
    @TableField("CLASS7_RECIPE_NO")
    private String class7RecipeNo;

    // ==================== 八班 ====================
    @Excel(name = "ui.data.column.cxScheduleResult.class8PlanQty")
    @ApiModelProperty(value = "八班计划数")
    @TableField("CLASS8_PLAN_QTY")
    private BigDecimal class8PlanQty;

    @ApiModelProperty(value = "八班原因分析手工输入")
    @TableField("CLASS8_ANALYSIS_INPUT")
    private String class8AnalysisInput;

    @Excel(name = "ui.data.column.cxScheduleResult.class8FinishQty")
    @ApiModelProperty(value = "八班完成量")
    @TableField("CLASS8_FINISH_QTY")
    private BigDecimal class8FinishQty;

    @Excel(name = "ui.data.column.cxScheduleResult.class8Analysis")
    @ApiModelProperty(value = "八班原因分析")
    @TableField("CLASS8_ANALYSIS")
    private String class8Analysis;

    @Excel(name = "ui.data.column.cxScheduleResult.class8RecipeType", dictType = "trial_status")
    @ApiModelProperty(value = "八班示方书类型")
    @TableField("CLASS8_RECIPE_TYPE")
    private String class8RecipeType;

    @ApiModelProperty(value = "八班示方书编号")
    @TableField("CLASS8_RECIPE_NO")
    private String class8RecipeNo;

    // ==================== 其他字段 ====================
    @ApiModelProperty(value = "收尾提示标识：0-提示收尾；1-不需要提示")
    @TableField("MARK_CLOSE_OUT_TIP")
    private String markCloseOutTip;

    //@Excel(name = "ui.data.column.cxScheduleResult.dataSource", dictType = "cx_schedule_data_source")
    @ApiModelProperty(value = "数据来源：0-自动排程；1-插单；2-导入")
    @TableField("DATA_SOURCE")
    private String dataSource;

    @ApiModelProperty(value = "特殊要求")
    @TableField("SPECIAL_REQUIREMENTS")
    private String specialRequirements;

    // ==================== 非持久化字段 ====================
    @ApiModelProperty(value = "班次1开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class1StartTime;

    @ApiModelProperty(value = "班次1结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class1EndTime;

    @ApiModelProperty(value = "班次2开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class2StartTime;

    @ApiModelProperty(value = "班次2结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class2EndTime;

    @ApiModelProperty(value = "班次3开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class3StartTime;

    @ApiModelProperty(value = "班次3结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class3EndTime;

    @ApiModelProperty(value = "班次4开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class4StartTime;

    @ApiModelProperty(value = "班次4结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class4EndTime;

    @ApiModelProperty(value = "班次5开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class5StartTime;

    @ApiModelProperty(value = "班次5结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class5EndTime;

    @ApiModelProperty(value = "班次6开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class6StartTime;

    @ApiModelProperty(value = "班次6结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class6EndTime;

    @ApiModelProperty(value = "班次7开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class7StartTime;

    @ApiModelProperty(value = "班次7结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class7EndTime;

    @ApiModelProperty(value = "班次8开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class8StartTime;

    @ApiModelProperty(value = "班次8结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date class8EndTime;

    @ApiModelProperty(value = "颜色标记：orange-快收尾(余量小于阈值)；yellow-新开规格；blue-试制量试；空-普通")
    @TableField("COLOR_TAG")
    private String colorTag;

    @ApiModelProperty(value = "子表明细列表")
    @TableField(exist = false)
    private List<CxScheduleDetail> details;
}
