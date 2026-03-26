package com.zlt.aps.mp.engine.logrecorder;

import com.zlt.aps.mp.engine.daylimit.MouldProductionLimitTypeEnum;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.enums.TbrMouldProductionLogType;
import com.zlt.aps.mp.engine.utils.TbrProductionLogUtils;
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
     * 增加成型机台开始进行分组计划的模具排产日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s 成型机台：%s，结构：%s 开始进行模具排产====
     *
     * @param context       排程上下文
     * @param cxMachineCode 成型机台编码
     * @param groupName     分组名-结构
     * @return
     */
    public static String addStartCxMachineMouldProductionPlanLog(Context context, String cxMachineCode, String groupName) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s 成型机台：%s，结构：%s 开始进行模具排产====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                cxMachineCode, groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.START_CX_MACHINE_GROUP_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加分组结构排产成型机台没有需要排产的计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，没有待排产计划====
     *
     * @param context       排程上下文
     * @param groupName     分组名-结构
     * @param cxMachineCode 成型机台编码
     * @return
     */
    public static String addGroupCxMachineMouldNoPlanLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，没有待排产计划====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_MOULD_NO_PLAN_DATA_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加在机结构对在产机台排产没有计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有计划====
     *
     * @param context           排程上下文
     * @param groupName         分组名-结构
     * @param onLineMachineInfo 在产机台信息
     * @return
     */
    public static String addContinueGroupContinueCxMachineNoPlanLog(Context context, String groupName, String onLineMachineInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在产机台: %s 没有计划====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, onLineMachineInfo);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_ON_LINE_MACHINE_PLAN_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加在机结构对在产机台排产没有待排产计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有待排产计划====
     *
     * @param context           排程上下文
     * @param groupName         分组名-结构
     * @param onLineMachineInfo 在产机台信息
     * @return
     */
    public static String addContinueGroupContinueCxMachineNoProductionPlanLog(Context context, String groupName, String onLineMachineInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在产机台: %s 没有待排产计划====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, onLineMachineInfo);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_ON_LINE_MACHINE_PRODUCTION_PLAN_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加在机结构续作Sku没有收尾机台(无需排产同规格同花纹、共生胎同模具)日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在产机台: %s  没有找到续作Sku收尾的硫化组，无需排产同规格同花纹、共生胎同模具====
     *
     * @param context           排程上下文
     * @param productionStage   排产阶段
     * @param groupName         分组名-结构
     * @param onLineMachineInfo 在产机台信息
     * @param continueType      排产类型
     * @return
     */
    public static String addContinueGroupContinueSkuNoLhGroupLog(Context context, ProductionStageEnum productionStage, String groupName, String onLineMachineInfo, ContinueTypeEnum continueType) {
        String logContentFormat = " =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在产机台: %s  没有找到续作Sku收尾的硫化组，无需%s排产====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, onLineMachineInfo, continueType.getDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_ON_LINE_MACHINE_NO_LH_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加在机结构对在产机台排产没有找到可排产硫化分组日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 没有找到待待硫化组====
     *
     * @param context           排程上下文
     * @param groupName         分组名-结构
     * @param onLineMachineInfo 在产机台信息
     * @return
     */
    public static String addContinueGroupContinueCxMachineNoLhGroupLog(Context context, String groupName, String onLineMachineInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在产机台: %s 没有找到待待硫化组====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, onLineMachineInfo);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_ON_LINE_MACHINE_NO_LH_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加在机结构对在产机台可排产硫化组日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在产机台: %s 排产硫化组%s~%s====
     *
     * @param context           排程上下文
     * @param groupName         分组名-结构
     * @param onLineMachineInfo 在产机台信息
     * @param startDay          开始日
     * @param endDay            结束日
     * @return
     */
    public static String addContinueGroupContinueCxMachineLhGroupRangeLog(Context context, String groupName, String onLineMachineInfo, Integer startDay, Integer endDay) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在产机台: %s 排产硫化组%s~%s====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, onLineMachineInfo, startDay, endDay);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_ON_LINE_MACHINE_LH_GROUP_RANGE, logContent);
        return logContent;
    }

    /**
     * 增加获取结构排产硫化组排产日访问日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 排产机台：%s 获取到排产日范围：%s~%s====
     *
     * @param context           排程上下文
     * @param groupName         分组名-结构
     * @param onLineMachineInfo 在产机台信息
     * @param startDay          开始日
     * @param endDay            结束日
     * @return
     */
    public static String addGroupFindLhMachineRangeLog(Context context, String groupName, String onLineMachineInfo, Integer startDay, Integer endDay) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 排产机台：%s 获取到排产日范围：%s~%s====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, onLineMachineInfo, startDay, endDay);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_FIND_LH_MACHINE_RANGE, logContent);
        return logContent;
    }

    /**
     * 增加在机结构对在产机台可排产硫化组日期范围修正日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 排产机台：%s 排产硫化组修正后排产日范围：%s~%s====
     *
     * @param context           排程上下文
     * @param groupName         分组名-结构
     * @param onLineMachineInfo 在产机台信息
     * @param startDay          开始日
     * @param endDay            结束日
     * @return
     */
    public static String addContinueGroupContinueMachineCorrectLhGroupRangeLog(Context context, String groupName, String onLineMachineInfo, Integer startDay, Integer endDay) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 排产机台：%s 排产硫化组修正后排产日范围：%s~%s====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, onLineMachineInfo, startDay, endDay);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_ON_LINE_MACHINE_LH_GROUP_RANGE, logContent);
        return logContent;
    }

    /**
     * 增加在机结构对在产机台硫化组找到可排产Sku日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在产机台：%s 排产硫化组找到可排产Sku：%s====
     *
     * @param context           排程上下文
     * @param groupName         分组名-结构
     * @param onLineMachineInfo 成型机台
     * @param materialDesc      Sku
     * @return
     */
    public static String addContinueGroupLhGroupFindSkuLog(Context context, String groupName, String onLineMachineInfo, String materialDesc) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在产机台：%s 排产硫化组找到可排产Sku：%s====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, onLineMachineInfo, materialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_MOULD_FIND_SKU_LH_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加 在排产日最后找到的一个Sku日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 硫化组找到是否[%s]最后一个排产Sku：%s====
     *
     * @param context      排程上下文
     * @param groupName    分组名-结构
     * @param isLast       是否最后一个
     * @param materialDesc Sku
     * @return
     */
    public static String addIsLastFindSkuLog(Context context, String groupName, boolean isLast, String materialDesc) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 硫化组找到是否[%s]最后一个排产Sku：%s====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, isLast, materialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_MOULD_FIND_SKU_LH_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加在机结构-硫化组排产Sku没有可排产模具日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在产机台：%s 进行模具排产，物料：%s 没有找到合适的模具====
     *
     * @param context           排程上下文
     * @param groupName         分组名-结构
     * @param onLineMachineInfo 成型机台编码
     * @param materialDesc      Sku信息
     * @return
     */
    public static String addContinueLhGroupSkuNoFindMouldLog(Context context, String groupName, String onLineMachineInfo, String materialDesc) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在产机台：%s 进行模具排产，物料：%s 没有找到合适的模具====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, onLineMachineInfo, materialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_MOULD_SKU_NO_FIND_MOULD_LH_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加结构-硫化组排产Sku限制日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 进行模具排产，物料：%s 在[%s]日达到%s限制====
     *
     * @param context           排程上下文
     * @param groupName         分组名-结构
     * @param onLineMachineInfo 成型机台编码
     * @param skuInfo           Sku信息
     * @param startDay          时间
     * @param limitType         限制类型
     * @return
     */
    public static String addSkuProductionLimitLog(Context context, String groupName, String onLineMachineInfo, MonthPlanProductionRequirePlanVo skuInfo, Integer startDay, MouldProductionLimitTypeEnum limitType) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 进行模具排产，物料：%s 在[%s]日达到%s====";
        String materialDesc = skuInfo.getMaterialDesc();
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, onLineMachineInfo, materialDesc,
                startDay, limitType.getLimitDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.MOULD_SKU_LIMIT_LH_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加结构-硫化组排产Sku超出限制日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 进行模具排产，物料：%s 超出%s====
     *
     * @param context           排程上下文
     * @param groupName         分组名-结构
     * @param onLineMachineInfo 成型机台编码
     * @param skuInfo           Sku信息
     * @param mouldShellInfo    模壳信息
     * @return
     */
    public static String addLhGroupSkuLimitLog(Context context, String groupName, String onLineMachineInfo, MonthPlanProductionRequirePlanVo skuInfo, String mouldShellInfo, MouldProductionLimitTypeEnum limitType) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 机台：%s 进行模具排产，物料：%s 模壳型号：%s 超出%s====";
        String materialDesc = skuInfo.getMaterialDesc();
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, onLineMachineInfo, materialDesc, mouldShellInfo, limitType.getLimitDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.MOULD_SKU_LIMIT_LH_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加分组结构排产成型机台没有找到机台信息日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，没有找到机台====
     *
     * @param context       排程上下文
     * @param groupName     分组名-结构
     * @param cxMachineCode 成型机台编码
     * @return
     */
    public static String addGroupCxMachineMouldNoFindMachineInfoLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，没有找到机台====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_MOULD_NO_FIND_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加分组结构排产成型机台分组计划没有硫化配比信息日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，分组计划没有硫化配比信息====
     *
     * @param context       排程上下文
     * @param groupName     分组名-结构
     * @param cxMachineCode 成型机台编码
     * @return
     */
    public static String addGroupCxMachineMouldGroupNoRatioLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，分组计划没有硫化配比信息====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_MOULD_GROUP_NO_FIND_RATIO_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加分组结构排产成型机台分组计划没有找到机型的硫化配比信息日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，分组计划没有机型：%s硫化配比信息====
     *
     * @param context       排程上下文
     * @param groupName     分组名-结构
     * @param cxMachineCode 成型机台编码
     * @param brandCode     机型编号
     * @return
     */
    public static String addGroupCxMachineMouldGroupNoBrandRatioLog(Context context, String groupName, String cxMachineCode, String brandCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，分组计划没有机型：%s硫化配比信息====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, cxMachineCode, brandCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_MOULD_GROUP_NO_FIND_BRAND_RATIO_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加分组结构排产成型机台硫化组起始排产日超出收尾日日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，硫化组起始排产日：%s超出收尾日：%s====
     *
     * @param context       排程上下文
     * @param groupName     分组名-结构
     * @param cxMachineCode 成型机台编码
     * @param startDay      起始排产日
     * @param endDay        排产收尾日
     * @return
     */
    public static String addLhGroupStartLimitEndLog(Context context, String groupName, String cxMachineCode, Integer startDay, Integer endDay) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，硫化组起始排产日：%s超出收尾日：%s====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, cxMachineCode, startDay, endDay);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_MOULD_START_LIMIT_END_LH_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加分组结构排产成型机台硫化组没有找到可排产Sku日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，硫化组没有找到可排产Sku====
     *
     * @param context       排程上下文
     * @param groupName     分组名-结构
     * @param cxMachineCode 成型机台编码
     * @return
     */
    public static String addLhGroupNoFindSkuLog(Context context, String groupName, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，硫化组没有找到可排产Sku====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_MOULD_NO_FIND_SKU_LH_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加分组结构排产成型机台硫化组排产Sku没有可排产量日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，物料：%s 没有可排产量====
     *
     * @param context       排程上下文
     * @param groupName     分组名-结构
     * @param cxMachineCode 成型机台编码
     * @param materialDesc  Sku信息
     * @return
     */
    public static String addLhGroupSkuNoProductionQtyLog(Context context, String groupName, String cxMachineCode, String materialDesc) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，物料：%s 没有可排产量====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, cxMachineCode, materialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_MOULD_SKU_NO_PRODUCTION_QTY_LH_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加分组结构排产成型机台硫化组排产Sku没有可排产量日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，物料：%s 没有找到合适的模具====
     *
     * @param context       排程上下文
     * @param groupName     分组名-结构
     * @param cxMachineCode 成型机台编码
     * @param materialDesc  Sku信息
     * @return
     */
    public static String addLhGroupSkuNoFindMouldLog(Context context, String groupName, String cxMachineCode, String materialDesc) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 进行模具排产，物料：%s 没有找到合适的模具====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, cxMachineCode, materialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_MOULD_SKU_NO_FIND_MOULD_LH_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加分组结构排产成型机台硫化组使用模具排产Sku日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 物料：%s 使用模具[%s]排产 %s~%s====
     *
     * @param context       排程上下文
     * @param groupName     分组名-结构
     * @param cxMachineCode 成型机台编码
     * @param materialDesc  Sku信息
     * @param mouldInfo     模具信息
     * @param startDay      开始日
     * @param endDay        结束日
     * @return
     */
    public static String addLhGroupSkuUsedFindMouldProductionLog(Context context, String groupName, String cxMachineCode, String materialDesc, String mouldInfo, Integer startDay, Integer endDay) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 对成型机台：%s 物料：%s 使用模具[%s]排产 %s~%s====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode, materialDesc,
                mouldInfo, startDay, endDay);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_MOULD_SKU_USED_FIND_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加模具排产计划奇数余量处理
     *
     * @param context       排产上下文
     * @param materialDesc  物料描述
     * @param mouldInfo     模具信息
     * @param productionDay 排产日
     * @param size          余数条数
     * @return
     */
    public static String addMouldProductionLeftOverOddNumberPlan(Context context, String groupName, String materialDesc, String mouldInfo, Integer productionDay, Integer size) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 物料：%s 使用模具[%s]在[%s]日排产计划余量奇数 %s条====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, materialDesc,
                mouldInfo, productionDay, size);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.MOULD_PRODUCTION_PLAN, logContent);
        return logContent;
    }

    /**
     * 增加在机结构续作Sku开始模具排产日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku：%s 开始进行模具排产====
     *
     * @param context      排程上下文
     * @param groupName    分组名-结构
     * @param materialDesc Sku信息
     * @return
     */
    public static String addContinueSkuStartMouldLog(Context context, String groupName, String materialDesc) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku：%s 开始进行模具排产====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, materialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_CONTINUE_SKU_START_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加在机结构续作Sku使用模具进行模具排产日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku：%s 使用模具：%s 进行模具排产====
     *
     * @param context      排程上下文
     * @param groupName    分组名-结构
     * @param materialDesc Sku信息
     * @param mouldInfo    模具信息
     * @return
     */
    public static String addContinueSkuMouldProductionByMouldLog(Context context, String groupName, String materialDesc, String mouldInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku：%s 使用模具：%s 进行模具排产====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, materialDesc, mouldInfo);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_CONTINUE_SKU_FOR_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加在机结构续作Sku模具排产没有排产量日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku：%s 模具排产当前阶段没有排产量====
     *
     * @param context      排程上下文
     * @param groupName    分组名-结构
     * @param materialDesc Sku信息
     * @return
     */
    public static String addContinueSkuNoProductionQtyLog(Context context, String groupName, String materialDesc) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku：%s 模具排产当前阶段没有排产量====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, materialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_CONTINUE_SKU_MOULD_PRODUCTION_NO_QTY, logContent);
        return logContent;
    }

    /**
     * 增加在机结构续作Sku模具排产没有找到模具日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku：%s [%s]模具排产因[%s]没有找到模具====
     *
     * @param context         排程上下文
     * @param productionStage 排产阶段
     * @param groupName       分组名-结构
     * @param materialDesc    Sku信息
     * @param limitType       限制类型
     * @return
     */
    public static String addContinueSkuNoFindMouldLog(Context context, ProductionStageEnum productionStage, String groupName, String materialDesc, MouldProductionLimitTypeEnum limitType) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku：%s [%s]模具排产因[%s]没有找到模具====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, materialDesc, productionStage.getStageDesc(), limitType.getLimitDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_CONTINUE_SKU_NO_MOULD, logContent);
        return logContent;
    }

    /**
     * 增加在机结构续作Sku降膜排产没有结果日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku：%s 降膜排产没有结果====
     *
     * @param context      排程上下文
     * @param groupName    分组名-结构
     * @param materialDesc Sku信息
     * @return
     */
    public static String addContinueSkuNoProductionResultLog(Context context, String groupName, String materialDesc) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku：%s 降膜排产没有结果====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, materialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_CONTINUE_SKU_MOULD_PRODUCTION_NO_QTY, logContent);
        return logContent;
    }

    /**
     * 增加在机结构续作Sku开始同规格同花纹或是同生胎共模具物料模具排产日志记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku：%s 开始进行%s物料：%s 模具排产====
     *
     * @param context             排程上下文
     * @param groupName           分组名-结构
     * @param materialDesc        Sku信息
     * @param continueType        续作类型
     * @param currentMaterialDesc 当前Sku
     * @return
     */
    public static String addContinueSkuStartSameInfoMouldLog(Context context, String groupName, String materialDesc, ContinueTypeEnum continueType, String currentMaterialDesc) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku：%s 开始进行%s物料：%s 模具排产====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, materialDesc, continueType.getDesc(), currentMaterialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        if (ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN == continueType) {
            TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_CONTINUE_SKU_START_SAME_SPEC_MOULD_PRODUCTION, logContent);
        } else {
            TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_CONTINUE_SKU_START_SAME_EMBRYO_MOULD_PRODUCTION, logContent);
        }
        return logContent;
    }

    private TbrMouldProductionLogRecorder() {

    }
}
