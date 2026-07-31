package com.zlt.aps.tq.engine.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎圈排程结果VO（6班次）
 *
 * <p>班次与实际时间对应关系（D=排程日期-2，即今天）：</p>
 * <ul>
 *   <li>1班：D日中班(14:00-22:00)    → 供应成型3班(D+1日夜班)</li>
 *   <li>2班：D+1日夜班(22:00-6:00)   → 供应成型4班(D+1日早班)</li>
 *   <li>3班：D+1日早班(6:00-14:00)   → 供应成型5班(D+1日中班)</li>
 *   <li>4班：D+1日中班(14:00-22:00)  → 供应成型6班(D+2日夜班)</li>
 *   <li>5班：D+2日夜班(22:00-6:00)   → 供应成型7班(D+2日早班)</li>
 *   <li>6班：D+2日早班(6:00-14:00)   → 供应成型8班(D+2日中班)，滚动排程</li>
 * </ul>
 *
 * <p>成型8班覆盖：D日早班、D日中班、D+1日夜早中、D+2日夜早中</p>
 * <p>胎圈6班覆盖：D日中班、D+1日夜早中、D+2日夜早</p>
 * <p>映射规律：胎圈N班 → 供应成型(N+2)班</p>
 */
@Data
public class TqScheduleResultVo extends ApsBaseDto {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    @ApiModelProperty(value = "对应的成型批次号")
    private String cxBatchNo;

    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String batchNo;

    @ApiModelProperty(value = "工单号，自动生成（批次号+4位定长自增序号）")
    private String orderNo;

    @ApiModelProperty(value = "胎圈代码")
    private String beadCode;

    @ApiModelProperty(value = "钢丝圈代码")
    private String steelRingCode;

    @ApiModelProperty(value = "三角胶代码")
    private String triangleGlueCode;

    @ApiModelProperty(value = "胶料代码")
    private String glueCode;

    @ApiModelProperty(value = "口型板代码")
    private String mouthPlateCode;

    @ApiModelProperty(value = "尺寸")
    private String specSize;

    @ApiModelProperty(value = "机台编号，多个逗号分割")
    private String machineCode;

    @ApiModelProperty(value = "单耗")
    private Double unitConsume;

    @ApiModelProperty(value = "库存数量")
    private Double stockQty;

    @ApiModelProperty(value = "库存供应成型时长，单位：小时")
    private Double supplyTime;

    // ==================== 胎圈6班次字段 ====================

    @ApiModelProperty(value = "1班(D日中班)计划量(条)")
    private Double class1PlanQty;

    @ApiModelProperty(value = "1班(D日中班)生产顺序")
    private Integer class1ProduceOrder;

    @ApiModelProperty(value = "1班(D日中班)系统原因分析")
    private String class1SysAnalysis;

    @ApiModelProperty(value = "1班(D日中班)手动输入原因分析")
    private String class1HandAnalysis;

    @ApiModelProperty(value = "2班(D+1日夜班)计划量(条)")
    private Double class2PlanQty;

    @ApiModelProperty(value = "2班(D+1日夜班)生产顺序")
    private Integer class2ProduceOrder;

    @ApiModelProperty(value = "2班(D+1日夜班)系统原因分析")
    private String class2SysAnalysis;

    @ApiModelProperty(value = "2班(D+1日夜班)手动输入原因分析")
    private String class2HandAnalysis;

    @ApiModelProperty(value = "3班(D+1日早班)计划量(条)")
    private Double class3PlanQty;

    @ApiModelProperty(value = "3班(D+1日早班)生产顺序")
    private Integer class3ProduceOrder;

    @ApiModelProperty(value = "3班(D+1日早班)系统原因分析")
    private String class3SysAnalysis;

    @ApiModelProperty(value = "3班(D+1日早班)手动输入原因分析")
    private String class3HandAnalysis;

    @ApiModelProperty(value = "4班(D+1日中班)计划量(条)")
    private Double class4PlanQty;

    @ApiModelProperty(value = "4班(D+1日中班)生产顺序")
    private Integer class4ProduceOrder;

    @ApiModelProperty(value = "4班(D+1日中班)系统原因分析")
    private String class4SysAnalysis;

    @ApiModelProperty(value = "4班(D+1日中班)手动输入原因分析")
    private String class4HandAnalysis;

    @ApiModelProperty(value = "5班(D+2日夜班)计划量(条)")
    private Double class5PlanQty;

    @ApiModelProperty(value = "5班(D+2日夜班)生产顺序")
    private Integer class5ProduceOrder;

    @ApiModelProperty(value = "5班(D+2日夜班)系统原因分析")
    private String class5SysAnalysis;

    @ApiModelProperty(value = "5班(D+2日夜班)手动输入原因分析")
    private String class5HandAnalysis;

    @ApiModelProperty(value = "6班(D+2日早班)计划量(条)")
    private Double class6PlanQty;

    @ApiModelProperty(value = "6班(D+2日早班)生产顺序")
    private Integer class6ProduceOrder;

    @ApiModelProperty(value = "6班(D+2日早班)系统原因分析")
    private String class6SysAnalysis;

    @ApiModelProperty(value = "6班(D+2日早班)手动输入原因分析")
    private String class6HandAnalysis;

    // ==================== 成型消耗量字段（对应成型CLASS1~CLASS8） ====================

    @ApiModelProperty(value = "对应成型1班(D日早班)的计划量，库存直接供应")
    private Integer cxClass1Plan;

    @ApiModelProperty(value = "对应成型2班(D日中班)的计划量，库存+当天早班产出供应")
    private Integer cxClass2Plan;

    @ApiModelProperty(value = "对应成型3班(D+1日夜班)的计划量，胎圈1班供应")
    private Integer cxClass3Plan;

    @ApiModelProperty(value = "对应成型4班(D+1日早班)的计划量，胎圈2班供应")
    private Integer cxClass4Plan;

    @ApiModelProperty(value = "对应成型5班(D+1日中班)的计划量，胎圈3班供应")
    private Integer cxClass5Plan;

    @ApiModelProperty(value = "对应成型6班(D+2日夜班)的计划量，胎圈4班供应")
    private Integer cxClass6Plan;

    @ApiModelProperty(value = "对应成型7班(D+2日早班)的计划量，胎圈5班供应")
    private Integer cxClass7Plan;

    @ApiModelProperty(value = "对应成型8班(D+2日中班)的计划量，胎圈6班供应")
    private Integer cxClass8Plan;

    // ==================== 其他业务字段 ====================

    /**
     * 月计划剩余量（用于前端展示和持久化）
     */
    @ApiModelProperty(value = "月计划剩余量", name = "monthSurplusQty")
    private Integer monthSurplusQty;

    /**
     * 剩余量
     */
    private double surplusQty;

    /**
     * 发布成功计数器，每点击一次发布并成功的话，计数器累加
     */
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    private Date newestPublishTime;

    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    private String markCloseOutTip;

    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "生产状态")
    private String productionStatus;

    /**
     * 寸口
     */
    private BigDecimal dimension;

    /**
     * 当天早班(D日早班)计划量，用于滚动衔接（昨天已排的、属于今天早班的胎圈计划量）
     */
    private Double todayMorningPlanQty;

    /**
     * 预计库存，晚班（19点）的剩余库存，仅用于计算可供时长
     */
    private Double planStockQty;

    /**
     * 机台code$胎胚代码，多个逗号分割，用来计算成型平均定额使用
     */
    private String quotaKeys;

    /**
     * 交接班库存（6班滚动计算后的最终库存结余）
     */
    private double classStock;

    /**
     * 库存供需比例，交接班库存/成型一天需求量
     */
    private double supplyDemandRatio;

    /**
     * 收尾规格标记，0：收尾1：非收尾
     */
    private String closeOutSpecFlag;

    /**
     * 是否走胎圈备库班数配置逻辑（0：否 1：是）
     * 触发条件：当前交接班库存不足以支撑1个班的量时触发
     */
    private String useBackupConfigFlag;

    /**
     * 命中的备库班数（来自 T_TQ_STOCK_SHIFT_CONFIG 配置的 SHIFT_COUNT）
     * 触发备库时一次性计算N个班的库存加到触发班次上
     */
    private Integer backupShiftCount;

    /**
     * 触发备库的胎圈班次（1-5）
     * 触发后该班次后续所有班次（K+1~6班）计划量置0
     */
    private Integer backupTriggerClass;

    /**
     * 触发备库后尚未排完的剩余备库量
     * <p>由 S2 阶段按阈值分摊后初始化，S3/S3.5 阶段每班排产后扣减，直至为0</p>
     * <p>用于 S3.5 阶段判断是否还有备库量需要回填到剩余产能</p>
     * <p>注意（Phase 2 重构）：S5.6 执行前会基于 backupTotalPlanQty 和实际已排产量重新计算此字段，
     * 修正 S3 阶段 deferToNextClass 不扣减导致的不一致问题</p>
     */
    private Double backupRemainingQty;

    /**
     * 触发备库时的初始备库总需求量（不含损耗率的纯需求量 × 损耗率乘数）
     * <p>由 S2 阶段 triggerBackupAndAllocate 初始化，用于 S5.6 重算 backupRemainingQty</p>
     * <p>重算公式：backupRemainingQty = backupTotalPlanQty - 6班实际已排产量合计</p>
     */
    private Double backupTotalPlanQty;

    /**
     * 备库分摊尾数合并的目标班次号（1-6）
     * <p>由 S2 阶段 triggerBackupAndAllocate 在触发取整合并时设置，
     * 标识该班次为尾数合并班次（计划量 = 阈值 + 尾数，如 500+9.6=509.6→510）</p>
     * <p>S3 阶段 getBackupInitAssignLimit 识别到该班次时放宽阈值限制，
     * 避免尾数合并后的计划量（如510）被 SYS1101029 阈值（如500）再次截断延后</p>
     */
    private Integer mergedTailClass;

    /**
     * 是否含试制/量试物料标记（1=是，0=否）
     * 关联SKU与示方书关系表，按物料编码取制造示方类型最新一条
     * 存在T(量试)或X(试制)物料时标记为1，在胎圈1班直接触发主动备库
     */
    private Integer embryoTypeFlag;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

    @ApiModelProperty(value = "是否未排（0:已排 1:未排）")
    private String unscheduledFlag;

    @ApiModelProperty(value = "未排原因")
    private String unscheduledReason;

    @ApiModelProperty(value = "分厂编码")
    private String factoryCode;

    /**
     * 规则解释JSON（TqRuleTrace.toExplainJson() 序列化结果）。
     *
     * <p>Phase 2 重构新增字段，用于持久化结构化证据，包含命中规则编码、结果和证据对象。</p>
     * <p>由 S6 {@code TqResultPersistHandler} 在批量落库前从 {@code context.getRuleTrace(beadCode)} 序列化填充。</p>
     */
    @ApiModelProperty(value = "规则解释JSON（命中规则编码、结果和证据，TqRuleTrace序列化结果）")
    private String explainJson;

}
