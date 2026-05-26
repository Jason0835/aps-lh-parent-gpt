package com.zlt.aps.mp.engine.utils;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.ProductionStageLogRecorder;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.mp.engine.enums.TbrMouldProductionLogType;
import com.zlt.aps.mp.engine.enums.TbrRequireLogType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * TBR排产日志工具类型
 *
 * @author ZLT
 * @date 20251210
 */
@Slf4j
public class TbrProductionLogUtils {
    /**
     * 时间格式
     */
    private static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS = DateTimeFormatter.ofPattern(DateUtils.YYYY_MM_DD_HH_MM_SS);

    /**
     * 日志记录器中加入排产日志信息
     *
     * @param context            排产上下文
     * @param productionPlanInfo 排产计划信息
     * @param logType            排产环节类型
     * @param detailContent      明细内容
     */
    public static void addProductionLog(Context context, ProductionPlanLogDto productionPlanInfo, TbrMouldProductionLogType logType, String detailContent) {
        StringBuilder logBuilder = getCurrentStageLogBuilder(context);
        if (null == productionPlanInfo || null == logBuilder) {
            return;
        }
        String date = YYYY_MM_DD_HH_MM_SS.format(LocalDateTime.now());
        String logContent;
        if (null == productionPlanInfo.getProductionPlanId()) {
            logContent = String.format("%s -阶段：%s ： %s", date, logType.getDesc(), detailContent);
        } else {
            logContent = String.format("%s 计划ID: %d 物料编码: %s 物料描述：%s -阶段：%s ： %s", date, productionPlanInfo.getProductionPlanId(), productionPlanInfo.getProductCode(), productionPlanInfo.getProductDesc(), logType.getDesc(), detailContent);
        }
        logBuilder.append(logContent).append(System.lineSeparator()).append("===================").append(System.lineSeparator());
    }

    /**
     * 日志记录器中加入排产日志信息
     *
     * @param context            排产上下文
     * @param productionPlanInfo 排产计划信息
     * @param logType            排产环节类型
     * @param detailContent      明细内容
     */
    public static void addProductionLog(Context context, ProductionPlanLogDto productionPlanInfo, TbrRequireLogType logType, String detailContent) {
        StringBuilder logBuilder = getCurrentStageLogBuilder(context);
        if (null == productionPlanInfo || null == logBuilder) {
            return;
        }
        String date = YYYY_MM_DD_HH_MM_SS.format(LocalDateTime.now());
        String logContent;
        if (null == productionPlanInfo.getProductionPlanId()) {
            logContent = String.format("%s -阶段：%s ： %s", date, logType.getDesc(), detailContent);
        } else {
            logContent = String.format("%s 计划ID: %d 物料编码: %s 物料描述：%s -阶段：%s ： %s", date, productionPlanInfo.getProductionPlanId(), productionPlanInfo.getProductCode(), productionPlanInfo.getProductDesc(), logType.getDesc(), detailContent);
        }
        logBuilder.append(logContent).append(System.lineSeparator()).append("===================").append(System.lineSeparator());
    }

    /**
     * 获取当前阶段排产的日志记录器
     *
     * @param context 排产上下文
     * @return
     */
    public static StringBuilder getCurrentStageLogBuilder(Context context) {
        List<ProductionStageLogRecorder> logBuilderList = context.getLogBuilderList();
        if (CollectionUtils.isEmpty(logBuilderList)) {
            return null;
        }
        int endIndex = context.getLogBuilderList().size() - BigDecimal.ONE.intValue();
        return context.getLogBuilderList().get(endIndex).getLogBuilder();
    }

    private TbrProductionLogUtils() {

    }
}
