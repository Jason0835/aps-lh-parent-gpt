package com.zlt.aps.factory.service;

import com.zlt.aps.factory.domain.Context;

/**
 * 分厂生产计划排产计算接口
 *
 * @author ZLT
 * 20250219
 */
public interface IMonthPlanProductionSchedulingService {

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
     * 一件进行计划排产，包含初始化、排成型、排硫化
     *
     * @param context
     */
    void general(Context context);

    /**
     * 删除某个排产计划
     *
     * @param context
     */
    void deleteVersion(Context context);

    /**
     * 计算某个需求的最大可排产量
     * 剔除分厂不排产、模具产能计算、轮胎类型控制等
     *
     * @param context
     */
    void calculateSizeCapacityRequire(Context context);
}
