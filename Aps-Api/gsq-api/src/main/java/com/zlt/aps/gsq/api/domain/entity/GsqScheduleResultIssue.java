package com.zlt.aps.gsq.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 钢丝圈排程结果下发对象
 * 用于将钢丝圈排程结果下发到MES系统
 * 一条钢丝圈排程结果（6班）拆分为3条下发记录，分别对应D日、D+1日、D+2日
 *
 * <p>6班→3天拆分映射：
 * <ul>
 *   <li>Day1(D日)：MID=钢丝圈1班</li>
 *   <li>Day2(D+1日)：NIGHT=钢丝圈2班, DAY=钢丝圈3班, MID=钢丝圈4班</li>
 *   <li>Day3(D+2日)：NIGHT=钢丝圈5班, DAY=钢丝圈6班</li>
 * </ul>
 * TQ_CLASS1~6_PLAN全量传递到每条记录
 *
 * @author APS
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "钢丝圈排程结果下发对象", description = "钢丝圈排程结果下发对象")
public class GsqScheduleResultIssue extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 排程日期（MES目标日期，D日/D+1日/D+2日） */
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    private LocalDate scheduleDate;

    /** 成型批次号 */
    @ApiModelProperty(value = "成型批次号", name = "cxBatchNo")
    private String cxBatchNo;

    /** 钢丝圈批次号 */
    @ApiModelProperty(value = "钢丝圈批次号", name = "batchNo")
    private String batchNo;

    /** 工单号 */
    @ApiModelProperty(value = "工单号", name = "orderNo")
    private String orderNo;

    /** 钢丝圈代码 */
    @ApiModelProperty(value = "钢丝圈代码", name = "steelRingCode")
    private String steelRingCode;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    private String materialCode;

    /** 钢丝圈类型 */
    @ApiModelProperty(value = "钢丝圈类型", name = "steelType")
    private String steelType;

    /** 胎胚描述 */
    @ApiModelProperty(value = "胎胚描述", name = "embryoSpecDesc")
    private String embryoSpecDesc;

    /** 单耗 */
    @ApiModelProperty(value = "单耗", name = "unitConsume")
    private Double unitConsume;

    /** 钢丝缠绕盘代码 */
    @ApiModelProperty(value = "钢丝缠绕盘代码", name = "twiningDiscCode")
    private String twiningDiscCode;

    /** 英寸 */
    @ApiModelProperty(value = "英寸", name = "proSize")
    private String proSize;

    /** 机台编号 */
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    private String machineCode;

    /** 库存数量 */
    @ApiModelProperty(value = "库存数量", name = "stockQty")
    private Double stockQty;

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

    // ========== 对应胎圈1~6班消耗量 ==========
    /** 对应胎圈1班消耗量 */
    @ApiModelProperty(value = "对应胎圈1班消耗量", name = "tqClass1Plan")
    private Integer tqClass1Plan;

    /** 对应胎圈2班消耗量 */
    @ApiModelProperty(value = "对应胎圈2班消耗量", name = "tqClass2Plan")
    private Integer tqClass2Plan;

    /** 对应胎圈3班消耗量 */
    @ApiModelProperty(value = "对应胎圈3班消耗量", name = "tqClass3Plan")
    private Integer tqClass3Plan;

    /** 对应胎圈4班消耗量 */
    @ApiModelProperty(value = "对应胎圈4班消耗量", name = "tqClass4Plan")
    private Integer tqClass4Plan;

    /** 对应胎圈5班消耗量 */
    @ApiModelProperty(value = "对应胎圈5班消耗量", name = "tqClass5Plan")
    private Integer tqClass5Plan;

    /** 对应胎圈6班消耗量 */
    @ApiModelProperty(value = "对应胎圈6班消耗量", name = "tqClass6Plan")
    private Integer tqClass6Plan;

    // ========== 状态字段 ==========
    /** 收尾规格标记(0：收尾，1：非收尾) */
    @ApiModelProperty(value = "收尾规格标记", name = "closeOutSpecFlag")
    private String closeOutSpecFlag;

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
