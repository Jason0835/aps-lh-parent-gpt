package com.zlt.aps.tq.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 胎圈排程结果下发对象
 * 用于将胎圈排程结果下发到MES系统
 * 一条胎圈排程结果（6班）拆分为3条下发记录，分别对应D日、D+1日、D+2日
 *
 * @author APS
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "胎圈排程结果下发对象", description = "胎圈排程结果下发对象")
public class TqScheduleResultIssue extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 排程日期（MES目标日期，D日/D+1日/D+2日） */
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    private LocalDate scheduleDate;

    /** 成型批次号 */
    @ApiModelProperty(value = "成型批次号", name = "cxBatchNo")
    private String cxBatchNo;

    /** 胎圈批次号 */
    @ApiModelProperty(value = "胎圈批次号", name = "batchNo")
    private String batchNo;

    /** 工单号 */
    @ApiModelProperty(value = "工单号", name = "orderNo")
    private String orderNo;

    /** 胎圈代码 */
    @ApiModelProperty(value = "胎圈代码", name = "beadCode")
    private String beadCode;

    /** SAP物料编码 */
    @ApiModelProperty(value = "SAP物料编码", name = "sapMaterialCode")
    private String sapMaterialCode;

    /** 钢丝圈代码 */
    @ApiModelProperty(value = "钢丝圈代码", name = "steelRingCode")
    private String steelRingCode;

    /** 三角胶代码 */
    @ApiModelProperty(value = "三角胶代码", name = "triangleGlueCode")
    private String triangleGlueCode;

    /** 胶料代码 */
    @ApiModelProperty(value = "胶料代码", name = "glueCode")
    private String glueCode;

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

    // ========== 对应成型3~8班计划量 ==========
    /** 对应成型3班计划量 */
    @ApiModelProperty(value = "对应成型3班计划量", name = "cxClass3Plan")
    private Double cxClass3Plan;

    /** 对应成型4班计划量 */
    @ApiModelProperty(value = "对应成型4班计划量", name = "cxClass4Plan")
    private Double cxClass4Plan;

    /** 对应成型5班计划量 */
    @ApiModelProperty(value = "对应成型5班计划量", name = "cxClass5Plan")
    private Double cxClass5Plan;

    /** 对应成型6班计划量 */
    @ApiModelProperty(value = "对应成型6班计划量", name = "cxClass6Plan")
    private Double cxClass6Plan;

    /** 对应成型7班计划量 */
    @ApiModelProperty(value = "对应成型7班计划量", name = "cxClass7Plan")
    private Double cxClass7Plan;

    /** 对应成型8班计划量 */
    @ApiModelProperty(value = "对应成型8班计划量", name = "cxClass8Plan")
    private Double cxClass8Plan;

    // ========== 状态字段 ==========
    /** 收尾提示标识(0:提示收尾；1:不需要提示) */
    @ApiModelProperty(value = "收尾提示标识", name = "markCloseOutTip")
    private String markCloseOutTip;

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
