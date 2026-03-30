package com.zlt.aps.mp.engine.logrecorder;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.mp.engine.enums.TbrMouldProductionLogType;
import com.zlt.aps.mp.engine.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 分组计划-收尾业务日志记录器
 * TBR-结构名
 * PCR-英寸
 *
 * @author ZLT
 * @date 20260329
 */
@Slf4j
public class GroupPlanConclusionLogRecorder {

    /**
     * 增加分组计划开始判断收尾 -日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 排产机台：%s 进入收尾日判断业务 ====
     *
     * @param context       排程上下文
     * @param groupName     分组
     * @param cxMachineCode 排产机台
     * @return
     */
    public static String addGroupStartConclusionLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 排产机台：%s 进入收尾日判断业务 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_CONCLUSION, logContent);
        return logContent;
    }


    /**
     * 增加 没有分配信息-退出结构收尾业务逻辑日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有分配信息，退出收尾判断业务 ====
     *
     * @param context   排程上下文
     * @param groupName 分组
     * @return
     */
    public static String addNoAllocationInfoLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有分配信息，退出收尾判断业务 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_CONCLUSION, logContent);
        return logContent;
    }


    /**
     * 增加 没有低于最低硫化数，无需结构还无需收尾日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 高于最低成型硫化配比：%s，无需收尾 ====
     *
     * @param context    排程上下文
     * @param groupName  分组
     * @param minLhRatio 最低硫化配比
     * @return
     */
    public static String addNoConclusionInfoLog(Context context, String groupName, Integer minLhRatio) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 高于最低成型硫化配比：%s，无需收尾 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, minLhRatio);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_CONCLUSION, logContent);
        return logContent;
    }

    /**
     * 增加 没有成型硫化配比，退出分组收尾业务判断日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有结构成型硫化配比信息，退出结构收尾业务判断 ====
     *
     * @param context   排程上下文
     * @param groupName 分组
     * @return
     */
    public static String addNoLhRatioInfoLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有结构成型硫化配比信息，退出结构收尾业务判断 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_CONCLUSION, logContent);
        return logContent;
    }


    /**
     * 增加 成型机台没有分配信息错误，导致结构收尾业务错误 日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 成型机台：%s 没有分配信息导致结构收尾业务无法继续进行错误 ====
     *
     * @param context       排程上下文
     * @param groupName     分组
     * @param cxMachineCode 成型机台
     * @return
     */
    public static String addCxMachineNoAllocationInfoLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 成型机台：%s 没有分配信息导致结构收尾业务无法继续进行错误 ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_CONCLUSION, logContent);
        return logContent;
    }

}
