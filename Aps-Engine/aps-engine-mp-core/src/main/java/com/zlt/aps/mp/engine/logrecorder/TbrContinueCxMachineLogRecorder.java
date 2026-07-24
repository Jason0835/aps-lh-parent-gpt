package com.zlt.aps.mp.engine.logrecorder;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.mp.engine.enums.TbrMouldProductionLogType;
import com.zlt.aps.mp.engine.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 续作成型机台-排产日志记录器
 * TBR-结构名
 * PCR-英寸
 *
 * @author ZLT
 * @date 20260714
 */
@Slf4j
public class TbrContinueCxMachineLogRecorder {

    /**
     * 增加 在机结构下机时间调整(因成型机台指定结构排产范围)日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 因成型机台指定结构排产范围提前下机%s====
     *
     * @param context       排程上下文
     * @param groupName     分组名-结构
     * @param cxMachineCode 成型机台
     * @param adjustDays    调整天数
     * @return
     */
    public static String addBeforeOffByAppointLog(Context context, String groupName, String cxMachineCode, int adjustDays) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 因成型机台指定结构排产范围提前下机%s====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode, adjustDays);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_CONTINUE_SKU_FOR_MOULD_PRODUCTION, logContent);
        return logContent;
    }
}
