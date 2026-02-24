package com.zlt.aps.mp.engine.logrecorder;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.enums.TbrMouldProductionLogType;
import com.zlt.aps.mp.engine.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * TBR 模具正式排产日志记录器
 *
 * @author ZLT
 * @date 20260107
 */
@Slf4j
public class TbrMouldFormalProductionLogRecorder {
    /**
     * 增加开始分组计划模具正式排产日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始分组计划模具排产====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addStartMouldFormalLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始分组计划模具排产====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.FORMAL_MOULD_START, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模具正式排产数据重置完成日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始分组计划模具排产数据重置完成====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addResetDataFinishLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始分组计划模具排产数据重置完成====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.FORMAL_MOULD_RESET_DATA_FINISH, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模具正式排产没有需要排产的数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始分组计划模具排产没有排产的数据====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addDataEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始分组计划模具排产没有排产的数据====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.FORMAL_MOULD_DATA_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模具正式排产-排产在机结构日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始分组计划模具排产排产在机结构====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addProductionContinueGroupLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始分组计划模具排产,排产在机结构====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.FORMAL_MOULD_CONTINUE_GROUP_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模具正式排产-排产在机结构续作Sku日志信息记录
     * "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始在机结构 %s 续作Sku 模具排产====
     *
     * @param context   排程上下文
     * @param groupName 分组名 TBR 结构名
     * @param type      续作类型
     * @return
     */
    public static String addProductionContinueGroupSingleGroupLog(Context context, String groupName, ContinueTypeEnum type) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始在机结构 %s %s 模具排产====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, type.getDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.FORMAL_MOULD_CONTINUE_GROUP_SINGLE_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模具正式排产-排产在机结构新增Sku日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始在机结构 %s 新增Sku 模具排产====
     *
     * @param context   排程上下文
     * @param groupName 分组名 TBR 结构名
     * @return
     */
    public static String addProductionContinueGroupSingleGroupAddSkuLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始在机结构 %s 新增Sku 模具排产====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.FORMAL_MOULD_CONTINUE_GROUP_SINGLE_ADD_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模具正式排产-排产在机结构没有排产计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 没有排产计划====
     *
     * @param context   排程上下文
     * @param groupName 分组名 TBR 结构名
     * @param type      续作类型
     * @return
     */
    public static String addProductionContinueGroupNoGroupPlanLog(Context context, String groupName, ContinueTypeEnum type) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 没有排产计划====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, type.getDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.FORMAL_MOULD_CONTINUE_GROUP_SINGLE_GROUP_NO_PLAN, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模具正式排产-排产在机结构没有分配到机台日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 没有分配到机台产能====
     *
     * @param context   排程上下文
     * @param groupName 分组名 TBR 结构名
     * @param type      续作类型
     * @return
     */
    public static String addProductionContinueGroupNoAllocationCxMachineLog(Context context, String groupName, ContinueTypeEnum type) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 没有分配到机台产能====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, type.getDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.FORMAL_MOULD_CONTINUE_GROUP_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模具正式排产-排产在机结构没有续作Sku日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 没有续作Sku信息====
     *
     * @param context   排程上下文
     * @param groupName 分组名 TBR 结构名
     * @param type      续作类型
     * @return
     */
    public static String addProductionContinueGroupNoContinueSkuLog(Context context, String groupName, ContinueTypeEnum type) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 没有续作Sku信息====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, type.getDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.FORMAL_MOULD_CONTINUE_GROUP_SINGLE_GROUP_NO_CONTINUE_SKU, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模具正式排产-新增结构排产日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始新增结构 %s 模具排产====
     *
     * @param context   排程上下文
     * @param groupName 分组名 TBR 结构名
     * @return
     */
    public static String addProductionAddGroupSingleGroupLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始新增结构 %s 模具排产====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.FORMAL_MOULD_ADD_GROUP_SINGLE_GROUP, logContent);
        return logContent;
    }

    private TbrMouldFormalProductionLogRecorder() {

    }
}
