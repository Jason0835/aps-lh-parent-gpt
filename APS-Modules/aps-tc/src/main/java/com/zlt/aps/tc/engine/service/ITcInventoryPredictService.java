package com.zlt.aps.tc.engine.service;

import com.zlt.aps.tc.engine.domain.TcScheduleContext;

/**
 * 胎侧库存预测步骤服务。
 *
 * <p>负责读取库存和损耗相关数据并计算供应时长。骨架阶段不实现具体库存算法。</p>
 */
public interface ITcInventoryPredictService {

    /**
     * 执行库存预测。
     *
     * @param context 胎侧排程上下文，方法会按实现补充库存预测结果
     */
    void predict(TcScheduleContext context);
}
