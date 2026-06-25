package com.zlt.aps.tm.engine.strategy;

import com.zlt.aps.tm.engine.domain.TmDemandQtyInput;
import com.zlt.aps.tm.engine.domain.TmDemandQtyResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;

/**
 * 胎面需求量算法策略接口。
 *
 * <p>用于按参数选择不同需求量算法。策略实现只返回计算结果，不直接修改任务链。</p>
 */
public interface ITmDemandQtyStrategy {

    /**
     * 获取算法编码。
     *
     * @return 算法编码，例如 1、2 或后续扩展编码
     */
    String getAlgorithmCode();

    /**
     * 计算需求量。
     *
     * @param input   需求量输入对象
     * @param context 胎面排程上下文
     * @return 需求量计算结果
     */
    TmDemandQtyResult calculate(TmDemandQtyInput input, TmScheduleContext context);
}
