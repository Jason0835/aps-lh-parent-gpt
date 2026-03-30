package com.zlt.aps.mp.engine.logrecorder;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.mp.engine.enums.TbrMouldProductionLogType;
import com.zlt.aps.mp.engine.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 分组计划延长收尾日志记录器
 * TBR-结构名
 * PCR-英寸
 *
 * @author ZLT
 * @date 20260330
 */
@Slf4j
public class GroupTimeExtensionConclusionLogRecorder {

    /**
     * 增加 分组计划开始进行延长收尾业务 -日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 进入延长收尾业务处理 ====
     *
     * @param context       排程上下文
     * @param groupName     分组
     * @param cxMachineCode 机台信息
     * @return
     */
    public static String addGroupStartTimeExtensionConclusionLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 进入延长收尾业务处理 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_TIME_EXTENSION_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加 分组计划无需延长收尾 -日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 无需延长收尾 ====
     *
     * @param context       排程上下文
     * @param groupName     分组
     * @param cxMachineCode 机台信息
     * @return
     */
    public static String addNoTimeExtensionConclusionHandlerLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 无需延长收尾 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_TIME_EXTENSION_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加 分组计划机台已整月不可延长 -日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 已整月不可延长 ====
     *
     * @param context       排程上下文
     * @param groupName     分组
     * @param cxMachineCode 机台信息
     * @return
     */
    public static String addNoTimeExtensionConclusionHandlerByEndLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 已整月不可延长 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_TIME_EXTENSION_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加 分组计划延长收尾处理前的数据清理 -日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 延长收尾处理前开始清理排产数据 ====
     *
     * @param context       排程上下文
     * @param groupName     分组
     * @param cxMachineCode 机台信息
     * @return
     */
    public static String addTimeExtensionConclusionHandlerStartClearDataLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 延长收尾处理前开始清理排产数据 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_TIME_EXTENSION_CONCLUSION, logContent);
        return logContent;
    }
}
