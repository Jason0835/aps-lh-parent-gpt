package com.zlt.aps.gsq.engine.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 钢丝圈排程结果VO（6班次制）。
 *
 * <p>班次与实际时间对应关系（D=排程日期-1，即今天；排程日期=T+1）：</p>
 * <ul>
 *   <li>1班：D日中班(14:00-22:00)    → 供应胎圈2班(D+1日夜班)</li>
 *   <li>2班：D+1日夜班(22:00-6:00)   → 供应胎圈3班(D+1日早班)</li>
 *   <li>3班：D+1日早班(6:00-14:00)   → 供应胎圈4班(D+1日中班)</li>
 *   <li>4班：D+1日中班(14:00-22:00)  → 供应胎圈5班(D+2日夜班)</li>
 *   <li>5班：D+2日夜班(22:00-6:00)   → 供应胎圈6班(D+2日早班)</li>
 *   <li>6班：D+2日早班(6:00-14:00)   → 供应胎圈7班(D+2日中班，需估值)</li>
 * </ul>
 *
 * <p>映射规律：钢丝圈N班 → 供应胎圈(N+1)班</p>
 *
 * @author APS
 */
@Data
public class GsqScheduleResultVo extends ApsBaseDto {

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_GSQ_SCHEDULE")
    private Long id;

    @ApiModelProperty(value = "排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date scheduleDate;

    @ApiModelProperty(value = "对应的胎圈批次号")
    private String tqBatchNo;

    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String batchNo;

    @ApiModelProperty(value = "工单号，自动生成（批次号+4位定长自增序号）")
    private String orderNo;

    @ApiModelProperty(value = "钢丝类型")
    private String steelType;

    @ApiModelProperty(value = "钢丝圈代码")
    private String steelRingCode;

    @ApiModelProperty(value = "钢丝缠绕盘代码")
    private String twiningDiscCode;

    @ApiModelProperty(value = "英寸")
    private String proSize;

    @ApiModelProperty(value = "排列")
    private String rank;

    @ApiModelProperty(value = "寸口")
    private BigDecimal dimension;

    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    @ApiModelProperty(value = "单耗")
    private Double unitConsume;

    @ApiModelProperty(value = "库存数量")
    private Double stockQty;

    @ApiModelProperty(value = "库存供应胎圈时长，单位：小时")
    private Double supplyTime;

    // ==================== 6个班次的顺序/计划量/原因分析 ====================

    @ApiModelProperty(value = "1班顺序（D日中班）")
    private Integer class1Sequence;

    @ApiModelProperty(value = "1班计划量（D日中班）")
    private Double class1PlanQty;

    @ApiModelProperty(value = "1班原因分析（D日中班）")
    private String class1Analysis;

    @ApiModelProperty(value = "2班顺序（D+1日夜班）")
    private Integer class2Sequence;

    @ApiModelProperty(value = "2班计划量（D+1日夜班）")
    private Double class2PlanQty;

    @ApiModelProperty(value = "2班原因分析（D+1日夜班）")
    private String class2Analysis;

    @ApiModelProperty(value = "3班顺序（D+1日早班）")
    private Integer class3Sequence;

    @ApiModelProperty(value = "3班计划量（D+1日早班）")
    private Double class3PlanQty;

    @ApiModelProperty(value = "3班原因分析（D+1日早班）")
    private String class3Analysis;

    @ApiModelProperty(value = "4班顺序（D+1日中班）")
    private Integer class4Sequence;

    @ApiModelProperty(value = "4班计划量（D+1日中班）")
    private Double class4PlanQty;

    @ApiModelProperty(value = "4班原因分析（D+1日中班）")
    private String class4Analysis;

    @ApiModelProperty(value = "5班顺序（D+2日夜班）")
    private Integer class5Sequence;

    @ApiModelProperty(value = "5班计划量（D+2日夜班）")
    private Double class5PlanQty;

    @ApiModelProperty(value = "5班原因分析（D+2日夜班）")
    private String class5Analysis;

    @ApiModelProperty(value = "6班顺序（D+2日早班）")
    private Integer class6Sequence;

    @ApiModelProperty(value = "6班计划量（D+2日早班）")
    private Double class6PlanQty;

    @ApiModelProperty(value = "6班原因分析（D+2日早班）")
    private String class6Analysis;

    // ==================== 6个班次的机台分配与定额 ====================

    @ApiModelProperty(value = "1班机台编号（D日中班）")
    private String class1MachineCode;
    @ApiModelProperty(value = "1班机台定额（D日中班）")
    private Double class1MachineQuota;

    @ApiModelProperty(value = "2班机台编号（D+1日夜班）")
    private String class2MachineCode;
    @ApiModelProperty(value = "2班机台定额（D+1日夜班）")
    private Double class2MachineQuota;

    @ApiModelProperty(value = "3班机台编号（D+1日早班）")
    private String class3MachineCode;
    @ApiModelProperty(value = "3班机台定额（D+1日早班）")
    private Double class3MachineQuota;

    @ApiModelProperty(value = "4班机台编号（D+1日中班）")
    private String class4MachineCode;
    @ApiModelProperty(value = "4班机台定额（D+1日中班）")
    private Double class4MachineQuota;

    @ApiModelProperty(value = "5班机台编号（D+2日夜班）")
    private String class5MachineCode;
    @ApiModelProperty(value = "5班机台定额（D+2日夜班）")
    private Double class5MachineQuota;

    @ApiModelProperty(value = "6班机台编号（D+2日早班）")
    private String class6MachineCode;
    @ApiModelProperty(value = "6班机台定额（D+2日早班）")
    private Double class6MachineQuota;

    /** 机台寸口（用于机台过滤策略，运行时辅助字段） */
    private BigDecimal machineInch;

    // ==================== 收尾/发布/状态相关 ====================

    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    private String markCloseOutTip;

    @ApiModelProperty(value = "是否发布")
    private String isRelease;

    @ApiModelProperty(value = "生产状态")
    private String productionStatus;

    @ApiModelProperty(value = "收尾规格标记，0：收尾，1：非收尾")
    private String closeOutSpecFlag;

    @ApiModelProperty(value = "未排标识：1=未排产（无可用机台或超产能）")
    private String unscheduledFlag;

    @ApiModelProperty(value = "保鲜期超期标记：1=存在超期，0=无超期")
    private String freshExpiredFlag;

    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    private Integer publishSuccessCount;

    @ApiModelProperty(value = "最新发布时间")
    private Date newestPublishTime;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

    @ApiModelProperty(value = "分厂编码")
    private String factoryCode;

    // ==================== 对应胎圈1~6班消耗量（回填自胎圈排程结果） ====================

    /** 对应胎圈1班消耗量 */
    private Double tqClass1Plan;
    /** 对应胎圈2班消耗量 */
    private Double tqClass2Plan;
    /** 对应胎圈3班消耗量 */
    private Double tqClass3Plan;
    /** 对应胎圈4班消耗量 */
    private Double tqClass4Plan;
    /** 对应胎圈5班消耗量 */
    private Double tqClass5Plan;
    /** 对应胎圈6班消耗量 */
    private Double tqClass6Plan;
    /** 对应胎圈7班消耗量（末班估值） */
    private Double tqClass7Plan;

    // ==================== 库存与切换相关 ====================

    /** 前日早班计划量（昨日1班/2班剩余库存来源） */
    private Double lastMidPlanQty;

    /** 交接班库存 */
    private Double classStock;

    /** 预计库存 */
    private Double planStockQty;

    /** 库存供需比例，交接班库存/胎圈一天需求量 */
    private Double supplyDemandRatio;

    /** 工艺参数（运行时辅助参数传递，不持久化） */
    @ApiModelProperty(value = "工艺参数Map，运行时传递，不持久化")
    private transient Map<String, Object> params = new HashMap<>();

    /**
     * 获取次日总计划量（2班+3班），用于排序。
     */
    public Double getNextDayTotalQty() {
        return BigDecimalUtil.add(this.class2PlanQty, this.class3PlanQty);
    }

    /**
     * 获取6班次计划量总和。
     */
    public Double getTotalPlanQty() {
        return BigDecimalUtil.add(
                this.class1PlanQty, this.class2PlanQty, this.class3PlanQty,
                this.class4PlanQty, this.class5PlanQty, this.class6PlanQty);
    }
}
