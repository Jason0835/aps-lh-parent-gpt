package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 钢丝圈排产参数（6班次制）。
 *
 * <p>参数来源：T_GSQ_PARAMS 表，对应 SYS16xxx 分组参数。</p>
 *
 * @author APS
 */
@Data
public class GsqScheduleParams {

    // ==================== 基础参数 ====================

    /** 仅投产阶段规格排产标识：0=全部规格，1=仅投产阶段规格 */
    private String productionStage;

    /** 工装车整车容量（SYS1602001，默认120） */
    private Double toolCapacity;

    /** 损耗率（SYS1601002，默认0.02） */
    private Double lossRate;

    /** 库存损耗率 */
    private Double stockLossRate;

    /** 预生产库存倍数 */
    private BigDecimal stockRatio;

    /** 收尾提醒数量（SYS1601007，月计划余量低于此值触发收尾） */
    private Double closeOutNum;

    /** 单最少排产量 */
    private Double minPlanQty;

    /** 往前一班合并计划量阈值 */
    private Double mergeThreshold;

    /** 预生产库存天数 */
    private Double productStockDay;

    /** 大需求量阈值（SYS1601008，超过此值算大需求量规格） */
    private Double largeDemand;

    /** 大尺寸规格阈值（默认17，超过该尺寸的规格不集中在一个班做） */
    private BigDecimal bigSizeSpec;

    // ==================== 班次与供应时长参数 ====================

    /** 班次时长（SYS1601004，单位：小时，默认8） */
    private Double classHours;

    /** 备库班数（SYS1601003，默认1） */
    private Double stockShiftCount;

    /** 需求系数（SYS1601001，钢丝圈:胎圈=1:1，默认1） */
    private Double demandCoefficient;

    /** 供应时长（默认12小时） */
    private Double supplyTime;

    /** 供应时长警告阈值（单位：班次，默认3班，低于此值触发库存预测规则证据） */
    private Double supplyTimeThreshold;

    /** 库消比高阈值（SYS1605001，默认2.0） */
    private BigDecimal stockConsumeRatioHigh;

    /** 库消比低阈值（SYS1605002，默认0.5） */
    private BigDecimal stockConsumeRatioLow;

    /** 均分阈值（默认500，需求量超过该值早夜班对半分） */
    private BigDecimal equalShareThreshold;

    // ==================== 保鲜期参数（钢丝圈独有） ====================

    /** 保鲜期小时数（SYS1601005，默认72小时） */
    private Double freshPeriodHours;

    // ==================== 产能与机台参数 ====================

    /** 包布机单机班产上限（SYS1602002，默认1500） */
    private Double wrappingMachineQuota;

    /** 包布机总台数（默认4） */
    private Integer wrappingMachineCount;

    /** 工装车总数（SYS1602004） */
    private Integer cartTotalCount;

    /** 单班规格最大上机次数（SYS1602003） */
    private Integer maxSpecSwitchPerClass;

    // ==================== 切换时长参数（钢丝圈独有） ====================

    /** 钢丝圈规格切换时长（SYS1603001，默认0.5小时） */
    private Double specSwitchTime;

    /** 切英寸时长（SYS1603002，默认1.5小时） */
    private Double inchSwitchTime;

    /** 换盘时长（SYS1603003，钢丝圈独有，默认1小时） */
    private Double wireSwitchTime;

    // ==================== 停产协调参数 ====================

    /** 胎圈停产预排班数（SYS1604001，胎圈停产1天时按此班数排产） */
    private Integer tqStopPreShiftCount;

    /** 胎圈停产触发开产天数（SYS1604002，默认2天） */
    private Integer tqStopReopenDays;

    /** 开产库存补量阈值（SYS1604003） */
    private Double reopenStockThreshold;

    // ==================== 末班估值参数 ====================

    /** 末班估值开关（SYS1606001，1=开启，0=关闭） */
    private String lastShiftEstimateEnabled;

    /** 末班估值取均值班数（SYS1606002，默认3，取胎圈4~6班均值） */
    private Integer lastShiftEstimateClassCount;

    // ==================== 强制班次规格 ====================

    /** 强制排在夜班的规格列表 */
    private String[] midSpec;

    /** 强制排在早班的规格列表 */
    private String[] nightSpec;
}
