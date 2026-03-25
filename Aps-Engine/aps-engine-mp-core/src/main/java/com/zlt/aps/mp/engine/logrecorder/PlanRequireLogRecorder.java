package com.zlt.aps.mp.engine.logrecorder;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.mp.engine.enums.TbrRequireLogType;
import com.zlt.aps.mp.engine.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 需求计算日志记录器
 * TBR-结构名
 * PCR-英寸
 *
 * @author ZLT
 * @date 20260325
 */
@Slf4j
public class PlanRequireLogRecorder {

    /**
     * 增加结构下主花纹需求测算日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构-主花纹 %s 实单量 %s 总净需求量 %s 储备比例 %s 模具产能 %s 有效需求量 %s====
     *
     * @param context        排程上下文
     * @param groupKey       结构+主花纹
     * @param actualQuantity 实单量
     * @param sumNetQty      净需求
     * @param mouldCapacity  模具产能
     * @return
     */
    public static String addRequireEstimateInfoLog(Context context, String groupKey, Integer actualQuantity, Integer sumNetQty, Integer mouldCapacity) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构-主花纹 %s 实单量 %s 总净需求量 %s 模具产能 %s 有效需求量 %s====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupKey, actualQuantity, sumNetQty,
                mouldCapacity, Math.min(sumNetQty, mouldCapacity));
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrRequireLogType.REQUIRE_ESTIMATE, logContent);
        return logContent;
    }

    /**
     * 增加周期结构需求测算日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构 %s 实单量 %s 总净需求量 %s 储备比例 %s 模具产能 %s 最大需求量 %s====
     *
     * @param context        排程上下文
     * @param percent        周期储备比例
     * @param groupKey       结构+主花纹
     * @param actualQuantity 实单量
     * @param sumNetQty      净需求
     * @param mouldCapacity  有效产能
     * @param maxCycleQty    最大量(实单+最大储备量)
     * @return
     */
    public static String addGroupRequireEstimateInfoLog(Context context, Integer percent, String groupKey, Integer actualQuantity, Integer sumNetQty, Integer mouldCapacity, Integer maxCycleQty) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构 %s 实单量 %s 总净需求量 %s 储备比例 %s 模具产能 %s 最大需求量 %s====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupKey, actualQuantity, sumNetQty, percent,
                mouldCapacity, maxCycleQty);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrRequireLogType.REQUIRE_ESTIMATE, logContent);
        return logContent;
    }

    /**
     * 增加结构下主花纹需求测算日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构-主花纹 %s 实单量 %s 总净需求量 %s 储备比例 %s 有效需求量 %s ====
     *
     * @param context        排程上下文
     * @param groupKey       结构+主花纹
     * @param actualQuantity 实单量
     * @param sumNetQty      净需求
     * @param percent        储备比例
     * @param effectiveQty   有效需求量
     * @return
     */
    public static String addRequireEstimateLog(Context context, String groupKey, Integer actualQuantity, Integer sumNetQty, Integer percent, Integer effectiveQty) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构-主花纹 %s 实单量 %s 总净需求量 %s 储备比例 %s 有效需求量 %s ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupKey, actualQuantity, sumNetQty,
                percent, effectiveQty);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrRequireLogType.REQUIRE_ESTIMATE, logContent);
        return logContent;
    }

    /**
     * 增加结构下主花纹模具空出产能日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构-主花纹 %s 有效需求量 %s  储备比例 %s 模具产能 %s 富余量 %s ====
     *
     * @param context       排程上下文
     * @param groupKey      结构+主花纹
     * @param effectiveQty  有效需求量
     * @param percent       储备比例
     * @param mouldCapacity 模具产能
     * @return
     */
    public static String addMouldMoreCapacityLog(Context context, String groupKey, Integer effectiveQty, Integer percent, Integer mouldCapacity) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构-主花纹 %s 有效需求量 %s  储备比例 %s 模具产能 %s 富余量 %s ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupKey, effectiveQty, percent, mouldCapacity, mouldCapacity - effectiveQty);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrRequireLogType.REQUIRE_ESTIMATE, logContent);
        return logContent;
    }

    /**
     * 增加结构富余空出产能，纯储备量日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构 %s 富余模具 %s  储备比例 %s 纯储备量 %s ====
     *
     * @param context         排程上下文
     * @param groupName       结构
     * @param totalVacateQty  储备模具多出产能
     * @param percent         储备比例
     * @param totalWaitAddQty 纯储备量
     * @return
     */
    public static String addMouldMoreCapacityWaitQtyLog(Context context, String groupName, Integer totalVacateQty, Integer percent, Integer totalWaitAddQty) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构 %s 富余模具 %s  储备比例 %s 纯储备量 %s ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, totalVacateQty, percent, totalWaitAddQty);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrRequireLogType.REQUIRE_ESTIMATE, logContent);
        return logContent;
    }

    /**
     * 增加结构下主花纹模具空出产能日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构-主花纹 %s 储备需求量 %s  储备比例 %s 模具产能 %s 最多可储备量 %s ====
     *
     * @param context       排程上下文
     * @param groupKey      结构+主花纹
     * @param cycleQty      周期储备量
     * @param percent       储备比例
     * @param mouldCapacity 模具产能
     * @return
     */
    public static String addAllCycleQtyLog(Context context, String groupKey, Integer cycleQty, Integer percent, Integer mouldCapacity) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构-主花纹 %s 储备需求量 %s  储备比例 %s 模具产能 %s 最多可储备量 %s ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupKey, cycleQty, percent, mouldCapacity, Math.min(cycleQty, mouldCapacity));
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrRequireLogType.REQUIRE_ESTIMATE, logContent);
        return logContent;
    }
}
