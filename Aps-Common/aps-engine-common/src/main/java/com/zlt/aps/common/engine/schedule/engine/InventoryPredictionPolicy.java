package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 库存预测领域策略端口。
 *
 * @param <C> 排程上下文类型
 * @param <F> 库存预测结果类型
 */
public interface InventoryPredictionPolicy<C, F extends ScheduleInventoryForecast> {

    /** @param context 待校验上下文 */
    void validateContext(C context);

    /**
     * 获取可排任务中的产品编码。
     *
     * @param context 排程上下文
     * @return 去重后的可排任务产品编码
     */
    List<String> getTaskProductCodes(C context);

    /**
     * 处理库存缺失策略。
     *
     * @param context 排程上下文
     * @param productCodes 产品编码
     * @param stockMap 库存映射
     * @param scheduleDate 排程日期
     */
    void handleMissingStock(C context, List<String> productCodes,
                            Map<String, BigDecimal> stockMap, Date scheduleDate);

    /** @param context 排程上下文 @return 是否使用示方书模式 */
    boolean isRecipeMode(C context);

    /**
     * 查询指定工厂、指定排程日期对应的前一天全部六点库存。
     *
     * <p>返回值不仅用于可排任务，还用于发现仅有库存的产品。</p>
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 排程日期
     * @return 产品编码到六点净库存的映射
     */
    Map<String, BigDecimal> querySixClockStock(String factoryCode, Date scheduleDate);

    /** @return 早班需求量 */
    Map<String, BigDecimal> queryFirstShiftDemand(String factoryCode, Date scheduleDate,
                                                   List<String> productCodes, boolean useRecipe);

    /** @return 早班计划量 */
    Map<String, BigDecimal> queryFirstShiftPlan(String factoryCode, Date scheduleDate,
                                                 List<String> productCodes);

    /** @param productCode 产品编码 @return 新预测结果 */
    F createForecast(String productCode);
}
