package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import lombok.Data;

import java.time.LocalDate;

/**
 * 前置选机冻结的下机需求，日目标请求先完成指定业务日数量，再计算释放。
 *
 * @author APS
 * @date 2026/11/13
 */
@Data
public class TimedMachineOffRequest {
    /** 来源 SKU，正式数量消费仍使用原账本。 */
    private SkuScheduleDTO sourceSku;
    /** 原选机规则确定的最早下机业务日。 */
    private LocalDate startDate;
    /** 清零前构造的真实物理机台产能画像，双模单控包含两侧。 */
    private ContinuationEndingMachineProfile profile;

    /**
     * 日目标切片前的完整资源容量，供释放后各班次容量恢复使用；普通请求为空。
     */
    private ContinuationEndingMachineProfile releaseCapacityProfile;

    /**
     * 本物理机台完整业务日目标量，包含已完成夜班；为空时沿用普通时间下机规则。
     */
    private Integer dailyTargetQty;

    /**
     * 目标日已实际完成的数量，只在T日读取，冻结后各阶段不得重复扣减。
     */
    private Integer completedDayQty;

    /**
     * 判断是否按日目标完成后下机，零目标同样属于有效请求。
     *
     * @return 是否具有明确的本机当日目标
     */
    public boolean hasDailyQuantityTarget() {
        return dailyTargetQty != null;
    }

    /**
     * 计算目标业务日尚需安排的产量，调用前已校验完成量未超过目标。
     *
     * @return 当日目标扣除已完成量后的计划量
     */
    public int requiredDayPlanQty() {
        return dailyTargetQty - completedDayQty;
    }
}
