package com.zlt.aps.common.engine.schedule.rolling;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时自动滚动专用轻量快照，不映射数据库实体。
 */
@Data
public class RollingMaterialSnapshot {
    /**
     * 本次来源已证明全部按需求生产；默认false保持普通物料规则。
     */
    private boolean wholeRollExempt = false;
    /**
     * 物料编码。
     */
    private String materialCode = null;
    /**
     * 当日有效唯一机台绑定。
     */
    private String boundMachine = null;
    /**
     * 主侧目标班原计划合计。
     */
    private BigDecimal initialPlanQty = BigDecimal.ZERO;
    /**
     * 目标班完成合计。
     */
    private BigDecimal finishQty = BigDecimal.ZERO;
    /**
     * 目标班预计库存。
     */
    private BigDecimal stockQty = BigDecimal.ZERO;
    /**
     * 报表六点净库存，顺位排序覆盖班数的基数；来源缺失时为 null。
     */
    private BigDecimal sixClockStockQty = null;
    /**
     * 目标班（本班）成型需求米数，与六点库存一起构成顺位排序的覆盖窗口首项。
     */
    private BigDecimal targetDemandQty = null;
    /**
     * 下一物理班起逐班需求。
     */
    private List<BigDecimal> futureDemand = new ArrayList<>();
    /**
     * 保护需求是否来源完整。
     */
    private boolean demandComplete = false;
    /**
     * 下修阈值窗口内的前若干班需求是否都有真实来源，供库存清零轮判断使用。
     */
    private boolean downWindowDemandComplete = false;
    /**
     * 本轮是否命中库存清零前置轮：预计库存不含当班计划已覆盖下修阈值窗口需求。
     */
    private boolean stockDownToZero = false;
    /**
     * 阈值方向。
     */
    private String direction = "NONE";
    /**
     * 结构化阻断或未承接原因。
     */
    private String reason = null;
    /**
     * 上修保护累计需求米数。
     */
    private BigDecimal upDemandQty = BigDecimal.ZERO;
    /**
     * 完成量保护后的基础目标。
     */
    private BigDecimal formulaQty = BigDecimal.ZERO;
    /**
     * 结算后的目标总量。
     */
    private BigDecimal targetQty = BigDecimal.ZERO;
    /**
     * 最终实际计划合计。
     */
    private BigDecimal actualQty = BigDecimal.ZERO;
    /**
     * 实际新增承接米数。
     */
    private BigDecimal acceptedQty = BigDecimal.ZERO;
    /**
     * 未满足上修米数。
     */
    private BigDecimal gapQty = BigDecimal.ZERO;
    /**
     * 初始供应班数排序快照。
     */
    private BigDecimal coverage = BigDecimal.ZERO;
    /**
     * 顺位排序用覆盖班数：以六点净库存为基数、从目标班起逐班扣减成型需求。
     * <p>与 {@link #coverage} 的区别在于基数是六点库存而非目标班预计库存，
     * 不受目标班之前各班计划量与需求递推影响；六点库存来源缺失时回退为 {@link #coverage}。</p>
     */
    private BigDecimal orderCoverage = BigDecimal.ZERO;
    /**
     * 已覆盖全部已知需求窗口。
     */
    private boolean windowCapped = false;
    /**
     * 窗口消耗后余量。
     */
    private BigDecimal endingAvailableQty = BigDecimal.ZERO;
    /**
     * 现行最低起排米数。
     */
    private BigDecimal minimumBatch = BigDecimal.ZERO;
    /**
     * 只读解析得到的未来备库需求米数，不含已由预计库存消耗的当前需求。
     */
    private BigDecimal stockUpDemandQty = BigDecimal.ZERO;
    /**
     * 备库需求来源及完整窗口是否可信；缺失来源不能按零需求补库。
     */
    private boolean stockUpDemandComplete = false;
    /**
     * 后置备库阶段经损耗、卷长及整数米结算的目标总计划。
     */
    private BigDecimal stockUpTargetQty = BigDecimal.ZERO;
    /**
     * 后置备库阶段实际追加米数，不包含第一阶段补最低量。
     */
    private BigDecimal stockUpAcceptedQty = BigDecimal.ZERO;
    /**
     * 后置备库阶段结束后尚未达到目标的米数，不写入原保供缺口。
     */
    private BigDecimal stockUpGapQty = BigDecimal.ZERO;
    /**
     * 备库资料或承接受限原因，仅用于后置优化解释，不污染原滚动终态。
     */
    private String stockUpSkipReason = null;
    /**
     * 本轮主动下修使用的合法最低/备库保护总量，不作为让位底线。
     */
    private BigDecimal replenishmentProtectedQty = BigDecimal.ZERO;
    /**
     * 实际让位米数。
     */
    private BigDecimal yieldedQty = BigDecimal.ZERO;
    /**
     * 可信施工与既有任务模板。
     */
    private RollingTaskSnapshot template = null;
    /**
     * 通过领域规则的机台评分快照。
     */
    private List<RollingCandidateSnapshot> candidates = new ArrayList<>();
    /**
     * 仅必要时构造的解释证据。
     */
    private Map<String, Object> evidence = new LinkedHashMap<>();
}
