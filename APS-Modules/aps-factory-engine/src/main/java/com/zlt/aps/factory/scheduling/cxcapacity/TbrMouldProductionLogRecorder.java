package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.factory.enums.TbrMouldProductionLogType;
import com.zlt.aps.factory.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * TBR 模具排产日志记录器
 *
 * @author ZLT
 * @date 20260105
 */
@Slf4j
public class TbrMouldProductionLogRecorder {
    /**
     * 增加分组结构排产成型机台没有需要排产的计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，没有待排产计划====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addGroupCxMachineMouldNoPlanLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，没有待排产计划====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_MOULD_NO_PLAN_DATA_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加分组结构排产成型机台没有找到机台信息日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，没有找到机台====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addGroupCxMachineMouldNoFindMachineInfoLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，没有找到机台====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_MOULD_NO_FIND_CX_MACHINE, logContent);
        return logContent;
    }

    private TbrMouldProductionLogRecorder() {

    }
}
