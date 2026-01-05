package com.zlt.aps.factory.scheduling.init;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.factory.enums.TbrMouldProductionLogType;
import com.zlt.aps.factory.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * TBR排产日志工具类型
 *
 * @author ZLT
 * @date 20251210
 */
@Slf4j
public class TbrProductionInitLogRecorder {
    /**
     * 增加开始初始化日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，排产初始化及检查开始====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addStartInitLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，排产初始化及检查开始====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.START_INIT, logContent);
        return logContent;
    }

    /**
     * 增加初始化结束日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，排产初始化及检查结束=====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addInitEndLog(Context context) {
        String initComplete = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，排产初始化及检查结束=====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.INIT_COMPLETE, initComplete);
        return initComplete;
    }

    /**
     * 增加初始化数据保存结束日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化数据存储结束=====
     *
     * @param context
     * @return
     */
    public static String addSaveInitDataLog(Context context) {
        String saveInitData = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化数据存储结束=====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SAVE_INIT, saveInitData);
        return saveInitData;
    }

    private TbrProductionInitLogRecorder() {

    }
}
