package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.enums.TbrMouldProductionLogType;
import com.zlt.aps.factory.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * TBR 结构分组排产日志记录器
 *
 * @author ZLT
 * @date 20260105
 */
@Slf4j
public class TbrProductionGroupLogRecorder {

    /**
     * 增加结构粗算产能日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构主花纹：%s 粗算产能：总需求 %s 使用最大模具数 %s 在%s天最大可排产量%s ====
     *
     * @param context              排程上下文
     * @param groupMainPatternName 分组+主花纹名
     * @param sumQty               总需求量
     * @param maxMouldNumber       最大模具数
     * @param maxCapacity          最大排产量
     * @return
     */
    public static String addGroupCalculateCapacityLog(Context context, String groupMainPatternName, Integer sumQty, Integer maxMouldNumber, Integer maxCapacity) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构主花纹：%s 粗算产能：总需求 %s 使用最大模具数 %s 在%s天最大可排产量%s ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupMainPatternName, sumQty, maxMouldNumber, context.getMonthDays(), maxCapacity);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_MAIN_PATTERN_CAPACITY_INFO, logContent);
        return logContent;
    }

    /**
     * 增加结构主花纹下使用的模具最大信息日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构+主花纹：%s 下理论最大模具数：%s 分配模具数：%s 最终最大可使用模具数 %s
     *
     * @param context          排程上下文
     * @param controlDimension 控制维度
     * @param maxMouldNumber   理论最大模具数
     * @param allocationNumber 模具分配数
     * @return
     */
    public static String addGroupMainPatternMaxMouldNumberLog(Context context, String controlDimension, Integer allocationNumber, Integer maxMouldNumber) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构+主花纹：%s 下理论最大模具数：%s 分配模具数：%s 最终最大可使用模具数 %s";
        Integer result = Math.min(allocationNumber, maxMouldNumber);
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                controlDimension, maxMouldNumber, allocationNumber, result);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_MAIN_PATTERN_CAPACITY_INFO, logContent);
        return logContent;
    }

    /**
     * 增加开始结构粗算成型机台数日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，开始进行分组计划粗算成型机台数====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addStartGroupCalculateCapacityLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，开始进行分组计划粗算成型机台数====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.START_GROUP_CAPACITY_CALCULATE, logContent);
        return logContent;
    }

    /**
     * 增加结构粗算总产能日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 粗算总产能：%s ====
     *
     * @param context     排程上下文
     * @param groupName   分组
     * @param maxCapacity 最大排产量
     * @return
     */
    public static String addGroupCalculateCapacityLog(Context context, String groupName, Integer maxCapacity) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 粗算总产能：%s ====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, maxCapacity);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SUM_CAPACITY_INFO, logContent);
        return logContent;
    }

    /**
     * 增加结构粗算成型机台数日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 粗算：最大排产量：%s 最低硫化机台数：%s 最小日硫化量：%s 需排产天数：%s 估算机台数：%s====
     *
     * @param context           排程上下文
     * @param groupName         分组
     * @param maxCapacity       最大排产量
     * @param minLhMachineCount 最小硫化机台数
     * @param minDayQty         最小日产能(单模)
     * @param sumProductionDay  总生产天数
     * @param cxMachineCount    估算机台数
     * @return
     */
    public static String addGroupCalculateCxMachineCountLog(Context context,
                                                            String groupName,
                                                            Integer maxCapacity,
                                                            Integer minLhMachineCount,
                                                            Integer minDayQty,
                                                            Integer sumProductionDay,
                                                            BigDecimal cxMachineCount) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 粗算：最大排产量：%s 最低硫化机台数：%s 最小日硫化量：%s 需排产天数：%s 估算机台数：%s====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, maxCapacity, minLhMachineCount, minDayQty,
                sumProductionDay, cxMachineCount);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SUM_CAPACITY_CX_MACHINE_INFO, logContent);
        return logContent;
    }

    /**
     * 增加结构没有获取到成型硫化配比数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s没有得到成型硫化配比====
     *
     * @param context   排程上下文
     * @param groupName 分组名
     * @return
     */
    public static String addGroupLhRatioEmptyLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s没有得到成型硫化配比====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SUM_CAPACITY_CX_MACHINE_INFO, logContent);
        return logContent;
    }

    /**
     * 增加结构为非在机机构日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s没有续作信息，非在机结构====
     *
     * @param context   排程上下文
     * @param groupName 分组名
     * @return
     */
    public static String addGroupNoContinueGroupLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 为非在机结构，没有续作Sku信息====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_NO_CONTINUE_GROUP_INFO, logContent);
        return logContent;
    }

    /**
     * 增加在机结构数据设置日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构：%s设置续作Sku需求量及在机结构配比硫化组====
     *
     * @param context   排程上下文
     * @param groupName 分组名
     * @return
     */
    public static String addOnLineGroupSetUpDataLog(Context context, String groupName) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构：%s设置续作Sku需求量及在机结构配比硫化组====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.ON_LINE_GROUP_SET_UP_DATA_INFO, logContent);
        return logContent;
    }

    /**
     * 增加结构为在机机构没有排产计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在机，但没有排产计划====
     *
     * @param context   排程上下文
     * @param groupName 分组名
     * @return
     */
    public static String addContinueGroupNoGroupPlanLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在机，但没有排产计划====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_NO_PRODUCTION_PLAN_INFO, logContent);
        return logContent;
    }

    /**
     * 增加结构为在机机构没有续作Sku信息日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在机，没有续作Sku信息====
     *
     * @param context   排程上下文
     * @param groupName 分组名
     * @return
     */
    public static String addContinueGroupNoContinueSkuLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在机，没有续作Sku信息====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_NO_CONTINUE_SKU_INFO, logContent);
        return logContent;
    }

    /**
     * 增加结构为在机机构没有续作Sku排产计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在机，没有续作排产计划====
     *
     * @param context   排程上下文
     * @param groupName 分组名
     * @return
     */
    public static String addContinueGroupContinueSkuEmptyPlanLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 在机，没有续作排产计划====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_CONTINUE_SKU_EMPTY_INFO, logContent);
        return logContent;
    }

    /**
     * 增加结构为在机机构没有续作Sku排产计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku %s 在机，没有排产计划====
     *
     * @param context      排程上下文
     * @param groupName    分组名
     * @param materialDesc 续作Sku
     * @return
     */
    public static String addContinueGroupContinueSkuNoPlanLog(Context context, String groupName, String materialDesc) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，结构：%s 续作Sku %s 在机，没有排产计划====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, materialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_CONTINUE_SKU_NO_PLAN_INFO, logContent);
        return logContent;
    }

    /**
     * 增加没有在机分组计划排产日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机分组计划没有数据，故而在机分组环节无需排产====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addContinueSkuNoContinueGroupProductionLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机分组计划没有数据，故而在机分组环节无需排产====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_NO_CONTINUE_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加没有在产分组机台可反向匹配分组计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在产分组机台反向匹配分组计划没有收尾的机台====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addNoContinueGroupReverseProductionLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在产分组机台反向匹配分组计划没有收尾的机台====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_MACHINE_NO_CLOSING_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加收尾机台没有在基础信息中匹配到日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台没有在基础信息中找到====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addReverseCxMachineNoExistBaseInfoLog(Context context) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台没有在基础信息中找到====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.REVERSE_MACHINE_NO_FIND_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加收尾机台没有分配结构日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台：%s 没有分配排产计划====
     *
     * @param context       排程上下文
     * @param cxMachineInfo 机台信息
     * @return
     */
    public static String addReverseCxMachineNoExistBaseInfoLog(Context context, CxMachineBaseInfoVo cxMachineInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台：%s 没有分配排产计划====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                cxMachineInfo.getCxMachineCode());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.REVERSE_MACHINE_NO_FIND_GROUP_PLAN, logContent);
        return logContent;
    }

    /**
     * 增加收尾机台没有可分配产能日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台：%s 没有可分配产能====
     *
     * @param context       排程上下文
     * @param cxMachineInfo 机台信息
     * @return
     */
    public static String addReverseCxMachineNoRemainingCapacityLog(Context context, CxMachineBaseInfoVo cxMachineInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台：%s 没有可分配产能====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                cxMachineInfo.getCxMachineCode());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.REVERSE_MACHINE_NO_REMAINING_CAPACITY, logContent);
        return logContent;
    }

    /**
     * 增加收尾机台没有找到产能覆盖的分组计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台：%s 没有找到产能可覆盖的分组计划====
     *
     * @param context       排程上下文
     * @param cxMachineInfo 机台信息
     * @return
     */
    public static String addReverseCxMachineNoFindCapacityPlanLog(Context context, CxMachineBaseInfoVo cxMachineInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台：%s 没有找到产能可覆盖的分组计划====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                cxMachineInfo.getCxMachineCode());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.REVERSE_MACHINE_CAPACITY_NO_COVER_PLAN, logContent);
        return logContent;
    }

    /**
     * 增加收尾机台没有找到产能可覆盖又能机台匹配的分组计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台：%s 没有找到产能可覆盖又能机台匹配的分组计划====
     *
     * @param context       排程上下文
     * @param cxMachineInfo 机台信息
     * @return
     */
    public static String addReverseCxMachineNoFindMatchPlanLog(Context context, CxMachineBaseInfoVo cxMachineInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台：%s 没有找到产能可覆盖又能机台匹配的分组计划====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                cxMachineInfo.getCxMachineCode());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.REVERSE_MACHINE_CAPACITY_COVER_NO_MATCH_PLAN, logContent);
        return logContent;
    }

    /**
     * 增加可排产天数小于最小上机天数日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，机台：%s 结构：%s 英寸：%s 可排产天数[%s] < 最低上机天数[%s]====
     *
     * @param context            排程上下文
     * @param cxMachineInfo      机台信息
     * @param groupName          分组名
     * @param proSize            英寸
     * @param realProductionDays 可排产天数
     * @param minAllocationDays  最低上机天数
     * @return
     */
    public static String addLowMinAllocationDayLog(Context context, CxMachineBaseInfoVo cxMachineInfo, String groupName, String proSize, Integer realProductionDays, Integer minAllocationDays) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，机台：%s 结构：%s 英寸：%s 可排产天数[%s] < 最低上机天数[%s]====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                cxMachineInfo.getCxMachineCode(), groupName, proSize, realProductionDays, minAllocationDays);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.MACHINE_MATCH_PLAN, logContent);
        return logContent;
    }

    /**
     * 增加收尾机台找到匹配的分组计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台：%s 找到匹配的计划分组：%====
     *
     * @param context       排程上下文
     * @param cxMachineInfo 机台信息
     * @param groupPlan     分组计划信息
     * @return
     */
    public static String addReverseCxMachineSelectedGroupPlanLog(Context context, CxMachineBaseInfoVo cxMachineInfo, ProductionPlanGroupInfo groupPlan) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台：%s 匹配到的计划分组：%s====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                cxMachineInfo.getCxMachineCode(), groupPlan.getGroupName());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.REVERSE_MACHINE_SELECTED_GROUP_PLAN, logContent);
        return logContent;
    }

    /**
     * 增加收尾机台查找下一分组计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台：%s 还有剩余产能，查找下一组计划====
     *
     * @param context       排程上下文
     * @param cxMachineInfo 机台信息
     * @return
     */
    public static String addReverseCxMachineFindNextGroupPlanLog(Context context, CxMachineBaseInfoVo cxMachineInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，收尾机台：%s 还有剩余产能，查找下一组计划====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                cxMachineInfo.getCxMachineCode());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.REVERSE_MACHINE_SELECTED_NEXT_GROUP_PLAN, logContent);
        return logContent;
    }

    /**
     * 增加没有获取到下一个优先级分组数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有获取到下一组优先级的分组计划====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addNoGetAddGroupPlanLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有获取到下一组优先级的分组计划====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.NO_NEXT_ADD_GROUP_PLAN, logContent);
        return logContent;
    }

    /**
     * 增加结构没有获取到合适的成型机-没有剩余产能数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 没有剩余产能的成型机台====
     *
     * @param context   排程上下文
     * @param groupName 分组名
     * @return
     */
    public static String addGroupNoSelectedLeftOverCapacityLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 没有剩余产能的成型机台====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SELECTED_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加结构没有匹配到成型机-零度不匹配日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台:%s 零度供料架：%s 不匹配====
     *
     * @param context           排程上下文
     * @param groupName         分组名
     * @param isZeroRack        分组是否要求零度
     * @param cxMachineCode     成型机台
     * @param machineIsZeroRack 成型机台零度供料架
     * @return
     */
    public static String addGroupNoSelectedZeroMatchLog(Context context, String groupName, String isZeroRack, String cxMachineCode, String machineIsZeroRack) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台:%s 零度供料架：%s 不匹配====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, isZeroRack, cxMachineCode, machineIsZeroRack);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_CX_MACHINE_BASE_MACHE, logContent);
        return logContent;
    }

    /**
     * 增加结构没有匹配到成型机-限制生产日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 限制生产====
     *
     * @param context       排程上下文
     * @param groupName     分组名
     * @param isZeroRack    分组是否要求零度
     * @param cxMachineCode 成型机台
     * @return
     */
    public static String addGroupNoSelectedLimitLog(Context context, String groupName, String isZeroRack, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 限制生产====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, isZeroRack, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_CX_MACHINE_BASE_MACHE, logContent);
        return logContent;
    }

    /**
     * 增加结构没有匹配到成型机-分组计划没有排产计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 分组计划没有待排产计划====
     *
     * @param context       排程上下文
     * @param groupName     分组名
     * @param isZeroRack    分组是否要求零度
     * @param cxMachineCode 成型机台
     * @return
     */
    public static String addGroupNoSelectedGroupNoProductionLog(Context context, String groupName, String isZeroRack, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 分组计划没有待排产计划====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, isZeroRack, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_CX_MACHINE_BASE_MACHE, logContent);
        return logContent;
    }

    /**
     * 增加结构没有匹配到成型机-分组计划没有物料编码信息日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 分组计划没有物料编码信息====
     *
     * @param context       排程上下文
     * @param groupName     分组名
     * @param isZeroRack    分组是否要求零度
     * @param cxMachineCode 成型机台
     * @return
     */
    public static String addGroupNoSelectedGroupMaterialDescExceptionLog(Context context, String groupName, String isZeroRack, String cxMachineCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 分组计划没有物料编码信息====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, isZeroRack, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_CX_MACHINE_BASE_MACHE, logContent);
        return logContent;
    }

    /**
     * 增加结构没有匹配到成型机-限制生产日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 机型：%s 没有成型硫化配比配置====
     *
     * @param context         排程上下文
     * @param groupName       分组名
     * @param isZeroRack      分组是否要求零度
     * @param cxMachineCode   成型机台
     * @param machineTypeCode 机型
     * @return
     */
    public static String addGroupNoSelectedNoRatioLog(Context context, String groupName, String isZeroRack, String cxMachineCode, String machineTypeCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 机型：%s 没有成型硫化配比配置====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, isZeroRack, cxMachineCode, machineTypeCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_CX_MACHINE_BASE_MACHE, logContent);
        return logContent;
    }

    /**
     * 增加结构匹配到成型机-初步被选中日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 机型：%s 初步被选中====
     *
     * @param context         排程上下文
     * @param groupName       分组名
     * @param isZeroRack      分组是否要求零度
     * @param cxMachineCode   成型机台
     * @param machineTypeCode 机型
     * @return
     */
    public static String addGroupSelectedCxMachineCodeLog(Context context, String groupName, String isZeroRack, String cxMachineCode, String machineTypeCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 机型：%s 初步被选中", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, isZeroRack, cxMachineCode, machineTypeCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_CX_MACHINE_BASE_MACHE, logContent);
        return logContent;
    }

    /**
     * 增加结构匹配到成型机-最终被选定-最大产能日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 机型：%s 本轮产能大优先被选定====
     *
     * @param context         排程上下文
     * @param groupName       分组名
     * @param isZeroRack      分组是否要求零度
     * @param cxMachineCode   成型机台
     * @param machineTypeCode 机型
     * @return
     */
    public static String addSelectedFinalByMaxCapacityMachineLog(Context context, String groupName, String isZeroRack, String cxMachineCode, String machineTypeCode) {
        String logContentFormat = " =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 机型：%s 本轮产能大优先被选定====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, isZeroRack, cxMachineCode, machineTypeCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SELECTED_FINAL_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加成型机切换结构日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 成型机台：%s 在[%s]日切换成结构：%s ====
     *
     * @param context         排程上下文
     * @param cxMachineCode   成型机台
     * @param changeDay       切换日
     * @param changeGroupName 切换后的分组
     * @return
     */
    public static String addCxMachineChangeGroupLog(Context context, String cxMachineCode, Integer changeDay, String changeGroupName) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 成型机台：%s 在[%s]日切换成结构：%s ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                cxMachineCode, changeDay, changeGroupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SELECTED_FINAL_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加成型机切换结构日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 成型机台：%s 在[%s]日提前收尾结构 %s 切换结构使用量释放 ====
     *
     * @param context         排程上下文
     * @param cxMachineCode   成型机台
     * @param changeDay       切换日
     * @param changeGroupName 切换后的分组
     * @return
     */
    public static String addReleaseCxMachineChangeGroupLog(Context context, String cxMachineCode, Integer changeDay, String changeGroupName) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 成型机台：%s 在[%s]日提前收尾结构 %s 切换结构使用量释放 ====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                cxMachineCode, changeDay, changeGroupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SELECTED_FINAL_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加结构匹配到成型机-最终被选定日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 机型：%s 本轮固定优先被选定====
     *
     * @param context         排程上下文
     * @param groupName       分组名
     * @param isZeroRack      分组是否要求零度
     * @param cxMachineCode   成型机台
     * @param machineTypeCode 机型
     * @return
     */
    public static String addGroupSelectedFixedFinalCxMachineCodeLog(Context context, String groupName, String isZeroRack, String cxMachineCode, String machineTypeCode) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 机型：%s 本轮固定优先被选定====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, isZeroRack, cxMachineCode, machineTypeCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SELECTED_FINAL_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加结构匹配到成型机-最终被选定日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 机型：%s 本轮最终被选定====
     *
     * @param context         排程上下文
     * @param groupName       分组名
     * @param isZeroRack      分组是否要求零度
     * @param cxMachineCode   成型机台
     * @param machineTypeCode 机型
     * @return
     */
    public static String addGroupSelectedFinalCxMachineCodeLog(Context context, String groupName, String isZeroRack, String cxMachineCode, String machineTypeCode) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 机型：%s 本轮(规格、英寸、断面宽)最终被选定====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, isZeroRack, cxMachineCode, machineTypeCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SELECTED_FINAL_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加没有找到满足最小排产天数日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 因成型工装、日产能最打排产天数[%s]不满足最小排产天数[%s]====
     *
     * @param context   排程上下文
     * @param groupName 分组名
     * @return
     */
    public static String addGroupNoReachMinAllocationDayLog(Context context, String groupName, Integer maxLeftOverDays, Integer minAllocationDays) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 因成型工装、日产能最大可排产天数[%s]不满足最小排产天数[%s]====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, maxLeftOverDays, minAllocationDays);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SELECTED_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加结构没有获取到合适的成型机数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 没有合适的成型机====
     *
     * @param context   排程上下文
     * @param groupName 分组名
     * @return
     */
    public static String addGroupNoSelectedCxMachineLog(Context context, String groupName) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 没有合适的成型机====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SELECTED_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加达到每日结构切换限制日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 成型机：%s 达到每日切换结构[%s]次限制====
     *
     * @param context       排程上下文
     * @param cxMachineCode 机台编码
     * @param maxLimit      限制次数
     * @return
     */
    public static String addChangeGroupLimitCxMachineLog(Context context, String cxMachineCode, Integer maxLimit) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 成型机：%s 达到每日切换结构[%s]次限制====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                cxMachineCode, maxLimit);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SELECTED_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加结构获取指定机台日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 从[%s]指定机台中挑选====
     *
     * @param context          排程上下文
     * @param groupName        分组名
     * @param fixedMachineInfo 限定机台信息
     * @return
     */
    public static String addGroupSelectedFixedCxMachineLog(Context context, String groupName, String fixedMachineInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 从[%s]指定机台中挑选====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, fixedMachineInfo);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SELECTED_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加结构没有从指定机台获取到机台日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 指定机台[%s]中没有合适机台====
     *
     * @param context          排程上下文
     * @param groupName        分组名
     * @param fixedMachineInfo 限定机台信息
     * @return
     */
    public static String addGroupNoSelectedForFixedCxMachineLog(Context context, String groupName, String fixedMachineInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 指定机台[%s]中没有合适机台====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, fixedMachineInfo);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_SELECTED_CX_MACHINE, logContent);
        return logContent;
    }

    private TbrProductionGroupLogRecorder() {

    }
}
