package com.zlt.aps.mp.engine.logrecorder;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.mp.engine.enums.TbrMouldProductionLogType;
import com.zlt.aps.mp.engine.utils.TbrProductionLogUtils;

/**
 * 对剩余不满足最短上机天数的机台，进行最后的补充分配
 * 日志记录器
 *
 * @author ZLT
 * @date 20260316
 */
public class SupplementCxMachineDistributionLogRecorder {
    /**
     * 增加开始进行成型机台补充分配日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，开始对剩余不足5天机台，进行补充分配 ====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addStartSupplementLog(Context context) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，开始对剩余不足5天机台，进行补充分配 ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SUPPLEMENT_CX_MACHINE_DISTRIBUTION, logContent);
        return logContent;
    }

    /**
     * 增加没有剩余产能机台则不进行补充分配日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有剩余产能机台，无需补充分配 ====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addNoLeftOverCxMachineLog(Context context) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有剩余产能机台，无需补充分配 ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SUPPLEMENT_CX_MACHINE_DISTRIBUTION, logContent);
        return logContent;
    }

    /**
     * 增加补充分配日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有剩余计划或是剩余机台 ====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addNoLeftOverGroupLog(Context context) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有剩余计划需要分配 ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SUPPLEMENT_CX_MACHINE_DISTRIBUTION, logContent);
        return logContent;
    }

    /**
     * 增加需要补充分配的机台信息日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，机台：[]需要进行月尾补充分配 ====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addNeedSupplementAllocationInfoLog(Context context, String cxMachineInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，机台：[%s]需要进行月尾补充分配 ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion()
                , cxMachineInfo);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SUPPLEMENT_CX_MACHINE_DISTRIBUTION, logContent);
        return logContent;
    }
}
