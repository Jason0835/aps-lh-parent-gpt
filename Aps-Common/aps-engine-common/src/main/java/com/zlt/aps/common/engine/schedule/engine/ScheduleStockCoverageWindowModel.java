package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 剩余库存起排班数的运行态需求窗口。
 *
 * <p>该模型只服务于自动排程内存计算，不对应任何持久化表；偏移量 0 表示当前班。</p>
 */
@Data
public class ScheduleStockCoverageWindowModel {

    /**
     * 参数编码。
     */
    private String paramCode;

    /**
     * 参数来源：TABLE 或 DEFAULT。
     */
    private String paramSource;

    /**
     * 非法配置时的回退原因。
     */
    private String fallbackReason;

    /**
     * 从当前班开始参与库存覆盖判断的连续班数。
     */
    private Integer shiftCount;

    /**
     * 按相对班次偏移保存需求量，单位米。
     */
    private Map<Integer, BigDecimal> demandQtyByOffset = new LinkedHashMap<>();

    /**
     * 稳定外推区间的起始偏移；无外推时为空。
     */
    private Integer extrapolatedStartOffset;

    /**
     * 稳定外推区间的班数；无外推时为空。
     */
    private Integer extrapolatedShiftCount;

    /**
     * 稳定外推区间的单班需求量，单位米；无外推时为空。
     */
    private BigDecimal extrapolatedDemandQtyPerShift;

    /**
     * 窗口累计需求量，单位米。
     */
    private BigDecimal totalDemandQty;

    /**
     * 设置窗口逐班需求并复制容器，避免派生任务共享可变映射。
     *
     * @param demandQtyByOffset 逐班需求
     */
    public void setDemandQtyByOffset(Map<Integer, BigDecimal> demandQtyByOffset) {
        this.demandQtyByOffset = demandQtyByOffset == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(demandQtyByOffset);
    }

    /**
     * 创建窗口副本。
     *
     * @return 独立的窗口副本
     */
    public ScheduleStockCoverageWindowModel copy() {
        ScheduleStockCoverageWindowModel target = new ScheduleStockCoverageWindowModel();
        target.setParamCode(this.paramCode);
        target.setParamSource(this.paramSource);
        target.setFallbackReason(this.fallbackReason);
        target.setShiftCount(this.shiftCount);
        target.setDemandQtyByOffset(this.demandQtyByOffset);
        target.setExtrapolatedStartOffset(this.extrapolatedStartOffset);
        target.setExtrapolatedShiftCount(this.extrapolatedShiftCount);
        target.setExtrapolatedDemandQtyPerShift(this.extrapolatedDemandQtyPerShift);
        target.setTotalDemandQty(this.totalDemandQty);
        return target;
    }
}
