package com.zlt.aps.common.engine.schedule.engine;

/**
 * TM/TC 自动排程需求量算法公共策略接口。
 *
 * @param <C> 排程上下文类型
 */
public interface IScheduleDemandQtyStrategy<C extends ScheduleContextModel<?, ?, ?, ?, ?>> {

    /**
     * 获取算法编码。
     *
     * @return 算法编码
     */
    String getAlgorithmCode();

    /**
     * 计算需求量。
     *
     * @param input   需求量输入对象
     * @param context 排程上下文
     * @return 需求量计算结果
     */
    ScheduleDemandQtyResultModel calculate(ScheduleDemandQtyInputModel input, C context);
}
