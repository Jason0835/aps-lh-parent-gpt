package com.zlt.aps.factory.daylimit;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.enums.TbrMouldProductionLogType;
import com.zlt.aps.factory.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 日排产限制日志记录器
 * TBR-结构名
 * PCR-英寸
 *
 * @author ZLT
 * @date 20260122
 */
@Slf4j
public class DayLimitLogRecorder {

    /**
     * 增加日换模次数使用日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 换模次数 + 1 后总换模次数 %s ====
     *
     * @param context         排程上下文
     * @param changeMouldDay  换模日
     * @param changeMouldInfo 换模信息
     * @param sumUsedCount    总换模次数
     * @return
     */
    public static String addChangeMouldUsedLog(Context context, Integer changeMouldDay, String changeMouldInfo, Integer sumUsedCount) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 换模次数 + 1 后总换模次数 %s ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                changeMouldDay, changeMouldInfo, sumUsedCount);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    /**
     * 增加日换模次数使用减量日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 换模次数 - 1 后总换模次数 %s ====
     *
     * @param context         排程上下文
     * @param changeMouldDay  换模日
     * @param changeMouldInfo 换模信息
     * @param sumUsedCount    总换模次数
     * @return
     */
    public static String addDeductionChangeMouldUsedLog(Context context, Integer changeMouldDay, String changeMouldInfo, Integer sumUsedCount) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 换模次数 - 1 后总换模次数 %s ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                changeMouldDay, changeMouldInfo, sumUsedCount);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    /**
     * 增加分组分配成型的最小产能日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 增加分配量[%d]后总分配量[%d]====
     *
     * @param context          排程上下文
     * @param allocationDay    分配日
     * @param allocationKey    分配Key信息
     * @param allocationQty    本次分配量
     * @param sumAllocationQty 总分配量
     * @return
     */
    public static String addCxMachineGroupUsedLog(Context context, Integer allocationDay, String allocationKey, Integer allocationQty, Integer sumAllocationQty) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 增加分配量[%d]后总分配量[%d]====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                allocationDay, allocationKey, allocationQty, sumAllocationQty);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    /**
     * 增加日换模次数使用减量日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 提前收尾分配量减[%d]后总分配量[%d] ====
     *
     * @param context          排程上下文
     * @param allocationDay    分配日
     * @param allocationKey    分配Key信息
     * @param allocationQty    本次分配量
     * @param sumAllocationQty 总分配量
     * @return
     */
    public static String addDeductionCxMachineGroupUsedLog(Context context, Integer allocationDay, String allocationKey, Integer allocationQty, Integer sumAllocationQty) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 提前收尾分配量减[%d]后总分配量[%d] ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                allocationDay, allocationKey, allocationQty, sumAllocationQty);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    /**
     * 增加日切换结构次数使用日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 切换分组(结构)次数 + 1 后总切换次数 %s ====
     *
     * @param context         排程上下文
     * @param changeGroupDay  切换结构日
     * @param changeGroupInfo 切换信息
     * @param sumUsedCount    总切换次数
     * @return
     */
    public static String addChangeGroupUsedLog(Context context, Integer changeGroupDay, String changeGroupInfo, Integer sumUsedCount) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 切换分组(结构)次数 + 1 后总切换次数 %s ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                changeGroupDay, changeGroupInfo, sumUsedCount);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    /**
     * 增加日切换结构次数使用-1日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 切换分组(结构)次数 - 1 后总切换次数 %s ====
     *
     * @param context         排程上下文
     * @param changeGroupDay  切换结构日
     * @param changeGroupInfo 切换信息
     * @param sumUsedCount    总切换次数
     * @return
     */
    public static String addDeductionChangeGroupUsedLog(Context context, Integer changeGroupDay, String changeGroupInfo, Integer sumUsedCount) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日 %s 切换分组(结构)次数 - 1 后总切换次数 %s ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                changeGroupDay, changeGroupInfo, sumUsedCount);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    /**
     * 增加达到日控制限制日志信息记录
     * 1、日产能上限
     * 2、成型工装数量上限
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，机台：%s 结构：%s 英寸：%s 已达到%s限制====
     *
     * @param context       排程上下文
     * @param cxMachineInfo 机台信息
     * @param groupName     分组名
     * @param proSize       英寸
     * @param limitType     限制类型
     * @return
     */
    public static String addReachDayControlLimitLog(Context context, CxMachineBaseInfoVo cxMachineInfo, String groupName, String proSize, GroupAllocationCapacityLimitTypeEnum limitType) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，机台：%s 结构：%s 英寸：%s 已达到%s限制====";
        String cxMachineCode = "";
        if (null != cxMachineInfo) {
            cxMachineCode = cxMachineInfo.getCxMachineCode();
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                cxMachineCode, groupName, proSize, limitType.getLimitDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    /**
     * 增加日控制限制日志信息记录
     * 1、日产能上限
     * 2、成型工装数量上限
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，机台：%s 结构：%s 英寸：%s %s剩余排产天数：[%s]====
     *
     * @param context       排程上下文
     * @param cxMachineInfo 机台信息
     * @param groupName     分组名
     * @param proSize       英寸
     * @param limitType     限制类型
     * @return
     */
    public static String addLeftOverDayControlLimitLog(Context context, CxMachineBaseInfoVo cxMachineInfo, String groupName, String proSize, GroupAllocationCapacityLimitTypeEnum limitType, String daysInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，机台：%s 结构：%s 英寸：%s %s剩余排产天数：[%s]====";
        String cxMachineCode = "";
        if (null != cxMachineInfo) {
            cxMachineCode = cxMachineInfo.getCxMachineCode();
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                cxMachineCode, groupName, proSize, limitType.getLimitDesc(), daysInfo);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    /**
     * 增加释放-日排产量日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日使用[%s]模具排产Sku[%s]-排产量[%s]，其中需生产量[%s]损耗量[%s]后，日总排产量 %s====
     *
     * @param context           排程上下文
     * @param productionDay     排产日
     * @param mouldCode         排产模具
     * @param materialDesc      排产Sku
     * @param realProductionQty 实际排产量
     * @param productionQty     需生产量
     * @param lossQty           损耗量
     * @param sumQty            总排产量
     * @return
     */
    public static String addDayProductionInfoLog(Context context, Integer productionDay, String mouldCode, String materialDesc, Integer realProductionQty, Integer productionQty, Integer lossQty, Integer sumQty) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日使用[%s]模具排产Sku[%s]-排产量[%s]，其中需生产量[%s]损耗量[%s]后，日总排产量 %s====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                productionDay, mouldCode, materialDesc,
                realProductionQty, productionQty, lossQty, sumQty);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    /**
     * 增加日排产量日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日使用[%s]模具排产Sku[%s]-因结构提前收尾，收尾量[%s]，其中排产量[%s]损耗量[%s]后，日总排产占用量 %s====
     *
     * @param context           排程上下文
     * @param productionDay     排产日
     * @param mouldCodeInfo     排产模具
     * @param materialDesc      排产Sku
     * @param realProductionQty 实际排产量
     * @param productionQty     需生产量
     * @param lossQty           损耗量
     * @param sumQty            总排产量
     * @return
     */
    public static String addDeductionDayProductionInfoLog(Context context, Integer productionDay, String mouldCodeInfo, String materialDesc, Integer realProductionQty, Integer productionQty, Integer lossQty, Integer sumQty) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在[%s]日使用[%s]模具排产Sku[%s]-因结构提前收尾，释放占用量[%s]，其中排产量[%s]损耗量[%s]后，日总排产占用量 %s====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                productionDay, mouldCodeInfo, materialDesc,
                realProductionQty, productionQty, lossQty, sumQty);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.DAY_LIMIT_CONTROL, logContent);
        return logContent;
    }

    private DayLimitLogRecorder() {

    }
}
