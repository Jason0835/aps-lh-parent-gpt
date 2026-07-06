package com.zlt.aps.tm.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 胎面排程结果下发对象
 * 用于将胎面排程结果下发到MES系统
 * 一条胎面排程结果（6班）拆分为3条下发记录，分别对应D日、D+1日、D+2日
 *
 * @author APS
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "胎面排程结果下发对象", description = "胎面排程结果下发对象")
public class TmScheduleResultIssue extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 排程日期（MES目标日期，D日/D+1日/D+2日） */
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    private LocalDate scheduleDate;

    /** 胎面批次号 */
    @ApiModelProperty(value = "胎面批次号", name = "batchNo")
    private String batchNo;

    /** 工单号 */
    @ApiModelProperty(value = "工单号", name = "orderNo")
    private String orderNo;

    /** 胎面代码 */
    @ApiModelProperty(value = "胎面代码", name = "treadCode")
    private String treadCode;

    /** SAP物料编码 */
    @ApiModelProperty(value = "SAP物料编码", name = "sapMaterialCode")
    private String sapMaterialCode;

    /** 主胶料编码 */
    @ApiModelProperty(value = "主胶料编码", name = "glueCode")
    private String glueCode;

    /** 基部胶编码 */
    @ApiModelProperty(value = "基部胶编码", name = "baseGlueCode")
    private String baseGlueCode;

    /** 整条胶料组合编码 */
    @ApiModelProperty(value = "整条胶料组合编码", name = "wholeGlueCode")
    private String wholeGlueCode;

    /** 胶料顺序 */
    @ApiModelProperty(value = "胶料顺序", name = "glueSeq")
    private String glueSeq;

    /** 口型板代码 */
    @ApiModelProperty(value = "口型板代码", name = "mouthPlateCode")
    private String mouthPlateCode;

    /** 尺寸 */
    @ApiModelProperty(value = "尺寸", name = "specSize")
    private String specSize;

    /** 机台编号 */
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    private String machineCode;

    /** 单耗 */
    @ApiModelProperty(value = "单耗", name = "unitConsume")
    private Double unitConsume;

    /** 库存数量 */
    @ApiModelProperty(value = "库存数量", name = "stockQty")
    private Double stockQty;

    /** 库存供应成型时长（小时） */
    @ApiModelProperty(value = "库存供应成型时长", name = "supplyTime")
    private Double supplyTime;

    // ========== 中班(14:00-22:00) ==========
    /** 中班计划量 */
    @ApiModelProperty(value = "中班计划量", name = "midPlanQty")
    private Double midPlanQty;

    /** 中班生产顺序 */
    @ApiModelProperty(value = "中班生产顺序", name = "midProduceOrder")
    private Integer midProduceOrder;

    /** 中班系统原因分析 */
    @ApiModelProperty(value = "中班系统原因分析", name = "midSysAnalysis")
    private String midSysAnalysis;

    /** 中班手工输入原因分析 */
    @ApiModelProperty(value = "中班手工输入原因分析", name = "midHandAnalysis")
    private String midHandAnalysis;

    // ========== 夜班(22:00-06:00) ==========
    /** 夜班计划量 */
    @ApiModelProperty(value = "夜班计划量", name = "nightPlanQty")
    private Double nightPlanQty;

    /** 夜班生产顺序 */
    @ApiModelProperty(value = "夜班生产顺序", name = "nightProduceOrder")
    private Integer nightProduceOrder;

    /** 夜班系统原因分析 */
    @ApiModelProperty(value = "夜班系统原因分析", name = "nightSysAnalysis")
    private String nightSysAnalysis;

    /** 夜班手工输入原因分析 */
    @ApiModelProperty(value = "夜班手工输入原因分析", name = "nightHandAnalysis")
    private String nightHandAnalysis;

    // ========== 早班(06:00-14:00) ==========
    /** 早班计划量 */
    @ApiModelProperty(value = "早班计划量", name = "dayPlanQty")
    private Double dayPlanQty;

    /** 早班生产顺序 */
    @ApiModelProperty(value = "早班生产顺序", name = "dayProduceOrder")
    private Integer dayProduceOrder;

    /** 早班系统原因分析 */
    @ApiModelProperty(value = "早班系统原因分析", name = "daySysAnalysis")
    private String daySysAnalysis;

    /** 早班手工输入原因分析 */
    @ApiModelProperty(value = "早班手工输入原因分析", name = "dayHandAnalysis")
    private String dayHandAnalysis;

    // ========== 次日中班(次日14:00-22:00) ==========
    /** 次日中班计划量 */
    @ApiModelProperty(value = "次日中班计划量", name = "nextMidPlanQty")
    private Double nextMidPlanQty;

    /** 次日中班生产顺序 */
    @ApiModelProperty(value = "次日中班生产顺序", name = "nextMidProduceOrder")
    private Integer nextMidProduceOrder;

    /** 次日中班系统原因分析 */
    @ApiModelProperty(value = "次日中班系统原因分析", name = "nextMidSysAnalysis")
    private String nextMidSysAnalysis;

    /** 次日中班手工输入原因分析 */
    @ApiModelProperty(value = "次日中班手工输入原因分析", name = "nextMidHandAnalysis")
    private String nextMidHandAnalysis;

    // ========== 状态字段 ==========
    /** 收尾提示标识(0:提示收尾；1:不需要提示) */
    @ApiModelProperty(value = "收尾提示标识", name = "markCloseOutTip")
    private String markCloseOutTip;

    /** 是否收尾任务 */
    @ApiModelProperty(value = "是否收尾任务", name = "tailFlag")
    private String tailFlag;

    /** 生产状态 */
    @ApiModelProperty(value = "生产状态", name = "productionStatus")
    private String productionStatus;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    private String remark;

    /** 版本号 */
    @ApiModelProperty(value = "版本号", name = "dataVersion")
    private String dataVersion;

    /** 分公司编码 */
    @ApiModelProperty(value = "分公司编码", name = "companyCode")
    private String companyCode;

    /** 厂别 */
    @ApiModelProperty(value = "厂别", name = "factoryCode")
    private String factoryCode;
}
