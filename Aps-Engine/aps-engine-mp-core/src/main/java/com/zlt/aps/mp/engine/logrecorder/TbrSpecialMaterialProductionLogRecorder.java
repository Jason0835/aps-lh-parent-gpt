package com.zlt.aps.mp.engine.logrecorder;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.mp.engine.enums.TbrMouldProductionLogType;
import com.zlt.aps.mp.engine.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * TBR 特殊原材料结构排产日志记录器
 *
 * @author ZLT
 * @date 20260205
 */
@Slf4j
public class TbrSpecialMaterialProductionLogRecorder {

    /**
     * 增加在机结构在产机台中有排产含有特殊原材料的分组计划日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，%s含有特殊原材料的分组计划====
     *
     * @param context 排程上下文
     * @param text 场景内容：如 1、在机结构在产机台中有排产 2、新结构挑选到 3、收尾机台：%s 有排产
     * @return
     */
    public static String addProductionSpecialMaterialInfoLog(Context context, String text) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，%s含有特殊原材料的分组计划====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                text);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SPECIAL_MATERIAL_GROUP_PRODUCTION, logContent);
        return logContent;
    }
    /**
     * 增加第一个特殊原材料结构分配
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，开始排产特殊原材料结构====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addFirstGroupProductionLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，开始排产特殊原材料结构====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SPECIAL_MATERIAL_GROUP_PRODUCTION, logContent);
        return logContent;
    }
}
