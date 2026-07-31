package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15StockSource;

import java.util.List;

/**
 * 定时滚动目标班次库存读取边界。
 */
public interface Cd15RollingShiftStockService {

    /** 目标班次是否已保存至少一条库存快照。 */
    boolean exists(Cd15RollingTarget target);

    /** 加载目标班次库存；缺失或业务键重复时抛出明确异常。 */
    List<Cd15StockSource> loadRequired(Cd15RollingTarget target);

    /** 生成目标班次库存确定性指纹。 */
    String fingerprint(Cd15RollingTarget target);
}
