package com.zlt.aps.common.engine.schedule.rolling;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 定时自动滚动专用轻量快照，不映射数据库实体。
 */
@Data
public class RollingTaskSnapshot {
    /**
     * 当前生产班次的纯试制量试最低量；null保持原整卷规则。
     */
    private BigDecimal demandOnlyMinimumQty = null;
    /**
     * 结果标识；试算新行使用临时标识。
     */
    private String key = null;
    /**
     * 既有结果主键。
     */
    private Long resultId = null;
    /**
     * 产品物料编码。
     */
    private String materialCode = null;
    /**
     * 绑定机台。
     */
    private String machineCode = null;
    /**
     * 本轮初始计划米数。
     */
    private BigDecimal initialPlanQty = BigDecimal.ZERO;
    /**
     * 当前计划米数。
     */
    private BigDecimal planQty = BigDecimal.ZERO;
    /**
     * 最终计划量整数向上取整前的行级数量。
     */
    private BigDecimal roundingBeforeQty = null;
    /**
     * 行级整数向上取整增加的数量。
     */
    private BigDecimal roundingAddedQty = BigDecimal.ZERO;
    /**
     * 行级数量结算规则标记。
     */
    private String roundingRule = null;
    /**
     * 目标班完成米数。
     */
    private BigDecimal finishQty = BigDecimal.ZERO;
    /**
     * 有效卷长米数。
     */
    private BigDecimal curlLength = null;
    /**
     * 生产速度，统一米每小时。
     */
    private BigDecimal speed = null;
    /**
     * 时间估算速度来源：SPECIFIC、MACHINE_DEFAULT、SHIFT_CAPACITY或MISSING。
     */
    private String speedSource = null;
    /**
     * 无法估算时间的具体原因，不影响数量规划。
     */
    private String timeUnavailableReason = null;
    /**
     * 主胶料。
     */
    private String glueCode = null;
    /**
     * 基部胶料。
     */
    private String baseGlueCode = null;
    /**
     * 口型板。
     */
    private String mouthPlateCode = null;
    /**
     * 目标班顺序。
     */
    private Integer sequence = null;
    /**
     * 预计开始毫秒值，使用不可变数值隔离沙箱。
     */
    private Long startTime = null;
    /**
     * 预计结束毫秒值。
     */
    private Long endTime = null;
    /**
     * 发布状态允许调整。
     */
    private boolean editable = true;
    /**
     * 收尾任务。
     */
    private boolean tailTask = false;

    /**
     * 复制全部不可变字段，候选试算不共享可变任务。
     *
     * @return 独立任务快照
     */
    public RollingTaskSnapshot copy() {
        RollingTaskSnapshot result = new RollingTaskSnapshot();
        result.demandOnlyMinimumQty = this.demandOnlyMinimumQty;
        result.key = this.key;
        result.resultId = this.resultId;
        result.materialCode = this.materialCode;
        result.machineCode = this.machineCode;
        result.initialPlanQty = this.initialPlanQty;
        result.planQty = this.planQty;
        result.roundingBeforeQty = this.roundingBeforeQty;
        result.roundingAddedQty = this.roundingAddedQty;
        result.roundingRule = this.roundingRule;
        result.finishQty = this.finishQty;
        result.curlLength = this.curlLength;
        result.speed = this.speed;
        result.speedSource = this.speedSource;
        result.timeUnavailableReason = this.timeUnavailableReason;
        result.glueCode = this.glueCode;
        result.baseGlueCode = this.baseGlueCode;
        result.mouthPlateCode = this.mouthPlateCode;
        result.sequence = this.sequence;
        result.startTime = this.startTime;
        result.endTime = this.endTime;
        result.editable = this.editable;
        result.tailTask = this.tailTask;
        return result;
    }
}
