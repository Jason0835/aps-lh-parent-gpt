package com.zlt.aps.tq.engine.strategy;

import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;

/**
 * 胎圈需求量计算策略接口。
 *
 * <p>S2.2 需求量计算阶段调用，负责：</p>
 * <ol>
 *   <li>备库班数配置匹配（按成型机台数）</li>
 *   <li>备库触发判断（主动/被动）</li>
 *   <li>收尾判断（基于胎胚关联汇总）</li>
 *   <li>备库总量计算与触发标记</li>
 * </ol>
 *
 * <p>本策略产出的是"是否触发备库"、"触发班次"、"备库总量"、"是否收尾规格"等中间状态，
 * 实际的 6 班计划量分摊由 {@link ITqPlanQtyStrategy} 完成。</p>
 *
 * <p>已知实现：</p>
 * <ul>
 *   <li>{@code DEFAULT}：默认需求量算法（沿用原 TqDemandCalcHandler 中的备库/收尾判断逻辑）</li>
 * </ul>
 *
 * @author APS
 */
public interface ITqDemandQtyStrategy {

    /**
     * 获取策略编码（用于参数 {@code TQ_DEMAND_QTY_STRATEGY_CODE} 路由）。
     *
     * @return 策略编码
     */
    String getStrategyCode();

    /**
     * 执行需求量计算，产出备库触发标记和收尾判断。
     *
     * <p>本方法应直接修改 {@link TqScheduleResultVo} 中的以下字段：</p>
     * <ul>
     *   <li>{@code backupTriggerClass}：备库触发班次（0=未触发，1~5=触发班次）</li>
     *   <li>{@code backupShiftCount}：备库班数 N</li>
     *   <li>{@code closeOutSpecFlag}：收尾标识（0=收尾，1=非收尾）</li>
     *   <li>{@code useBackupConfigFlag}：是否使用备库配置（0=否，1=是）</li>
     * </ul>
     *
     * @param scheduleVo 排程结果 VO
     * @param context    排程上下文（用于读取备库配置、胎胚关联、月计划余量，写入规则证据）
     */
    void calcDemandQty(TqScheduleResultVo scheduleVo, TqScheduleContext context);
}
