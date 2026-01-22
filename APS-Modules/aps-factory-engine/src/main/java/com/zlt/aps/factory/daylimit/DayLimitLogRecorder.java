package com.zlt.aps.factory.daylimit;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.factory.enums.TbrMouldProductionLogType;
import com.zlt.aps.factory.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 日排产限制日志记录器
 * TBR-结构名
 * PCR-英寸
 *
 * @author ZLT
 * @date 20260122
 */
@Slf4j
public class DayLimitLogRecorder {

    /**
     * 增加日换模次数使用日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 换模次数 + 1 后总换模次数 %s ====
     *
     * @param context         排程上下文
     * @param changeMouldDay  换模日
     * @param changeMouldInfo 换模信息
     * @param sumUsedCount    总换模次数
     * @return
     */
    public static String addChangeMouldUsedLog(Context context, Integer changeMouldDay, String changeMouldInfo, Integer sumUsedCount) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 换模次数 + 1 后总换模次数 %s ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                changeMouldDay, changeMouldInfo, sumUsedCount);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    /**
     * 增加日换模次数使用减量日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 换模次数 - 1 后总换模次数 %s ====
     *
     * @param context         排程上下文
     * @param changeMouldDay  换模日
     * @param changeMouldInfo 换模信息
     * @param sumUsedCount    总换模次数
     * @return
     */
    public static String addDeductionChangeMouldUsedLog(Context context, Integer changeMouldDay, String changeMouldInfo, Integer sumUsedCount) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 换模次数 - 1 后总换模次数 %s ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                changeMouldDay, changeMouldInfo, sumUsedCount);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    /**
     * 增加日切换结构次数使用日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 切换分组(结构)次数 + 1 后总切换次数 %s ====
     *
     * @param context         排程上下文
     * @param changeGroupDay  切换结构日
     * @param changeGroupInfo 切换信息
     * @param sumUsedCount    总切换次数
     * @return
     */
    public static String addChangeGroupUsedLog(Context context, Integer changeGroupDay, String changeGroupInfo, Integer sumUsedCount) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 切换分组(结构)次数 + 1 后总切换次数 %s ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                changeGroupDay, changeGroupInfo, sumUsedCount);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    /**
     * 增加日切换结构次数使用-1日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 切换分组(结构)次数 - 1 后总切换次数 %s ====
     *
     * @param context         排程上下文
     * @param changeGroupDay  切换结构日
     * @param changeGroupInfo 切换信息
     * @param sumUsedCount    总切换次数
     * @return
     */
    public static String addDeductionChangeGroupUsedLog(Context context, Integer changeGroupDay, String changeGroupInfo, Integer sumUsedCount) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 切换分组(结构)次数 - 1 后总切换次数 %s ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                changeGroupDay, changeGroupInfo, sumUsedCount);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    private DayLimitLogRecorder() {

    }
}
