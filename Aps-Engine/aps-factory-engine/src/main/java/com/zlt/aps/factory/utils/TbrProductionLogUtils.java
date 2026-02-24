package com.zlt.aps.factory.utils;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.factory.enums.TbrMouldProductionLogType;
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
     * 日志记录器中加入排产日志信息
     *
     * @param context            排产上下文
     * @param productionPlanInfo 排产计划信息
     * @param logType            排产环节类型
     */
    public static void addProductionLog(Context context, ProductionPlanLogDto productionPlanInfo, TbrMouldProductionLogType logType, String detailContent) {
        if (null == productionPlanInfo) {
            return;
        }
        String date = DateUtils.dateTimeNow(DateUtils.YYYY_MM_DD_HH_MM_SS);
        String logContent;
        if (null == productionPlanInfo.getProductionPlanId()) {
            logContent = String.format("%s -阶段：%s ： %s", date, logType.getDesc(), detailContent);
        } else {
            logContent = String.format("%s 计划ID: %d 物料编码: %s 物料描述：%s -阶段：%s ： %s", date, productionPlanInfo.getProductionPlanId(), productionPlanInfo.getProductCode(), productionPlanInfo.getProductDesc(), logType.getDesc(), detailContent);
        }
        context.getLogBuilder().append(logContent).append(System.lineSeparator()).append("===================").append(System.lineSeparator());
    }

    private TbrProductionLogUtils() {

    }
}
