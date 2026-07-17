package com.zlt.aps.tc.engine.strategy;

import com.zlt.aps.tc.engine.domain.TcDemandQtyInput;
import com.zlt.aps.tc.engine.domain.TcDemandQtyResult;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;

/**
 * 胎侧需求量算法策略接口。
 *
 * <p>用于按参数选择不同需求量算法。策略实现只返回计算结果，不直接修改任务链。</p>
 */
public interface ITcDemandQtyStrategy {

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
     * @param context 胎侧排程上下文
     * @return 需求量计算结果
     */
    TcDemandQtyResult calculate(TcDemandQtyInput input, TcScheduleContext context);
}
