package com.zlt.aps.mp.engine.handler;


import com.zlt.aps.mp.engine.domain.Context;

/**
 * 分厂生产计划排产计算接口
 *
 * @author ZLT
 * @date 20250220
 */
public interface ProductionSchedulingService {

    /**
     * 一键生产生成
     *
     * @param context
     */
    void general(Context context);

    /**
     * 初始化及检查
     *
     * @param context
     */
    void init(Context context);

    /**
     * 模具排产
     *
     * @param context
     */
    void mouldingScheduling(Context context);

    /**
     * 删除某个排产版本信息
     *
     * @param context
     */
    void deleteVersion(Context context);

}
