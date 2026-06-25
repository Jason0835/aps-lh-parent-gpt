package com.zlt.aps.tm.engine.strategy;

import com.zlt.aps.tm.engine.domain.TmDemandQtyInput;
import com.zlt.aps.tm.engine.domain.TmDemandQtyResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;

/**
 * 固定编码需求量策略测试桩。
 *
 * <p>仅用于验证策略注册表按编码返回策略，不承载真实排程算法。</p>
 */
public class FixedDemandQtyStrategy implements ITmDemandQtyStrategy {

    @Override
    public String getAlgorithmCode() {
        return "A";
    }

    @Override
    public TmDemandQtyResult calculate(TmDemandQtyInput input, TmScheduleContext context) {
        return new TmDemandQtyResult();
    }
}
