package com.zlt.aps.common.engine.schedule.rolling;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 定时自动滚动专用输入与输出上下文，不映射数据库实体。
 */
@Data
public class RollingPlanningInput {
    /**
     * 领域纯规则回调：按实时账本刷新候选评分，不允许数据库访问。
     */
    @com.alibaba.fastjson.annotation.JSONField(serialize = false, deserialize = false)
    private java.util.function.BiConsumer<RollingMaterialSnapshot, RollingPlanningInput> candidateRefresh;
    /**
     * 物料初始快照。
     */
    private Map<String, RollingMaterialSnapshot> materials = new LinkedHashMap<>();
    /**
     * 机台主账本。
     */
    private Map<String, RollingMachineSnapshot> machines = new LinkedHashMap<>();
    /**
     * 上修阈值班数。
     */
    private BigDecimal upThreshold = BigDecimal.ONE;
    /**
     * 下修触发班数。
     */
    private BigDecimal downThreshold = new BigDecimal("4");
    /**
     * 下修目标班数。
     */
    private BigDecimal downTarget = new BigDecimal("3");
    /**
     * 结果行最小计划量，取值SYS0804016；非正数表示不启用补齐。
     */
    private BigDecimal minimumPlanQty = BigDecimal.ZERO;
    /**
     * 是否启用余量填谷轮：前面各轮补完后仍有剩余产能时，按班给未达需求窗口的已开机规格补量。
     */
    private boolean remainingCapacityFillEnabled = true;
    /**
     * 是否启用缺料规格顺位提前（胎面定时滚动专用）：
     * 上修方向（缺料）规格整规格排到续作规格之后、其余规格之前，不要求本行计划量必须增加。
     * 关闭时保持原行级口径（仅计划量增加的行参与提前），胎侧等共用规划器不启用。
     */
    private boolean shortageFirstEnabled = false;
    /**
     * 换胶独立固定损失米数。
     */
    private BigDecimal glueLossQty = BigDecimal.ZERO;
    /**
     * 换胶时间分钟。
     */
    private BigDecimal glueMinutes = BigDecimal.ZERO;
    /**
     * 规格或口型切换分钟。
     */
    private BigDecimal specMinutes = BigDecimal.ZERO;
    /**
     * 物理班起点毫秒。
     */
    private long shiftStart = 0L;
    /**
     * 统一进度采集时刻毫秒。
     */
    private long progressTime = 0L;
    /**
     * 整批业务终态。
     */
    private String status = null;
    /**
     * 是否实际改变数量顺序时间。
     */
    private boolean planChanged = false;
    /**
     * 是否已由TM/TC主侧装载后置目标；其他独立规划调用保持原行为。
     */
    private boolean postReplenishmentEnabled = false;
}
