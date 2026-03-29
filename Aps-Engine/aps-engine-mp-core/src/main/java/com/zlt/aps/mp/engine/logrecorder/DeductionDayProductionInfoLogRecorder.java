package com.zlt.aps.mp.engine.logrecorder;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.mp.engine.enums.DeductionDayProductionTypeEnum;
import com.zlt.aps.mp.engine.enums.TbrMouldProductionLogType;
import com.zlt.aps.mp.engine.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 日排产信息扣除日志记录器
 * TBR-结构名
 * PCR-英寸
 *
 * @author ZLT
 * @date 20260329
 */
@Slf4j
public class DeductionDayProductionInfoLogRecorder {

    /**
     * 增加 日产能释放(日产能排产扣减) 日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s [%s]日产能开始释放====
     *
     * @param context           排程上下文
     * @param groupName         分组
     * @param deductionType     类型
     * @param cxMachineCode     机台信息
     * @param deductionDaysInfo 日信息
     * @return
     */
    public static String addStartGroupDeductionDayCapacityLog(Context context, String groupName, DeductionDayProductionTypeEnum deductionType, String cxMachineCode, String deductionDaysInfo) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s %s [%s]日[%s]产能开始释放====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode, deductionType.getDesc(), deductionDaysInfo);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_DEDUCTION_DAY, logContent);
        return logContent;
    }
}
