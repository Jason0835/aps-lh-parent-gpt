package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;
import com.zlt.aps.cd90.engine.model.Cd90StockSource;

import java.util.List;

/**
 * 定时滚动目标班次库存读取边界。
 */
public interface Cd90RollingShiftStockService {

    /** 目标班次是否已保存至少一条库存快照。 */
    boolean exists(Cd90RollingTarget target);

    /** 加载目标班次库存；缺失或业务键重复时抛出明确异常。 */
    List<Cd90StockSource> loadRequired(Cd90RollingTarget target);

    /** 生成目标班次库存确定性指纹。 */
    String fingerprint(Cd90RollingTarget target);
}
