package com.zlt.aps.factory.utils;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.factory.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.factory.enums.TbrMouldProductionLogType;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;

/**
 * TBR排产日志工具类型
 *
 * @author ZLT
 * @date 20251210
 */
@Slf4j
public class TbrProductionLogUtils {
    /**
     * 增加开始初始化日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，排产初始化及检查开始====
     *
     * @param productionContext 排程上下文
     * @return
     */
    public static String addStartInitLog(TbrProductionContext productionContext) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，排产初始化及检查开始====", productionContext.getFactoryCode(), productionContext.getYear(), productionContext.getMonth(), productionContext.getMonthPlanVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        addProductionLog(productionContext, productionPlanInfo, TbrMouldProductionLogType.START_INIT, logContent);
        return logContent;
    }

    /**
     * 增加初始化结束日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，排产初始化及检查结束=====
     *
     * @param productionContext 排程上下文
     * @return
     */
    public static String addInitEndLog(TbrProductionContext productionContext) {
        String initComplete = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，排产初始化及检查结束=====", productionContext.getFactoryCode(), productionContext.getYear(), productionContext.getMonth(), productionContext.getMonthPlanVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        addProductionLog(productionContext, productionPlanInfo, TbrMouldProductionLogType.INIT_COMPLETE, initComplete);
        return initComplete;
    }

    /**
     * 增加初始化数据保存结束日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化数据存储结束=====
     *
     * @param productionContext
     * @return
     */
    public static String addSaveInitDataLog(TbrProductionContext productionContext) {
        String saveInitData = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化数据存储结束=====", productionContext.getFactoryCode(), productionContext.getYear(), productionContext.getMonth(), productionContext.getMonthPlanVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        addProductionLog(productionContext, productionPlanInfo, TbrMouldProductionLogType.SAVE_INIT, saveInitData);
        return saveInitData;
    }

    /**
     * 日志记录器中加入排产日志信息
     *
     * @param productionContext  排产上下文
     * @param productionPlanInfo 排产计划信息
     * @param logType            排产环节类型
     */
    private static void addProductionLog(TbrProductionContext productionContext, ProductionPlanLogDto productionPlanInfo, TbrMouldProductionLogType logType, String detailContent) {
        if (null == productionPlanInfo) {
            return;
        }
        String date = DateUtils.dateTimeNow(DateUtils.YYYY_MM_DD_HH_MM_SS);
        String logContent = String.format("%s 计划ID: %d 物料编码: %s 物料描述：%s -阶段：%s ： %s", date, productionPlanInfo.getProductionPlanId(), productionPlanInfo.getProductCode(), productionPlanInfo.getProductDesc(), logType.getDesc(), detailContent);
        productionContext.getLogBuilder().append(logContent).append(System.lineSeparator()).append("===================").append(System.lineSeparator());
    }

    private TbrProductionLogUtils() {

    }
}
