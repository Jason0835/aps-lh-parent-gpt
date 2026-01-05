package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.factory.enums.TbrMouldProductionLogType;
import com.zlt.aps.factory.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * TBR 结构分组排产日志记录器
 *
 * @author ZLT
 * @date 20260105
 */
@Slf4j
public class TbrProductionGroupLogRecorder {
    /**
     * 增加开始结构日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，分组结构排产开始====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addStartGroupLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，分组结构排产开始====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.START_GROUP, logContent);
        return logContent;
    }

    /**
     * 增加获取排产计划数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，获取排产版本计划数据结束====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addGetProductionVersionDataLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，获取排产版本计划数据结束====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.END_GET_VERSION_DATA, logContent);
        return logContent;
    }

    /**
     * 增加获取排产参数日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取排产参数====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addReaderProductionParamLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取排产参数====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.END_READER_PARAM_DATA, logContent);
        return logContent;
    }

    /**
     * 增加获读取特殊原材料日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取特殊原材料信息为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addReaderSpecialMaterialEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到特殊原材料信息为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SPECIAL_MATERIAL_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加获读取特殊原材料库存日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取特殊原材料库存为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addReaderSpecialMaterialStockEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到特殊原材料库存为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SPECIAL_MATERIAL_STOCK_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加获读取特殊原材料库存日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到生产日历信息为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addProductionCalendarEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到生产日历信息为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.PRODUCTION_CALENDAR_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加没有停工日日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有停工日====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addNoStopCalendarLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有停工日====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.STOP_DAY_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加成型机基础数据为空日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到成型机基础信息为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addCxMachineInfoEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到成型机基础信息为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CX_MACHINE_BASE_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加成型机基础数据为空日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到成型机维修信息为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addCxMachineMaintenanceInfoEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到成型机维修信息为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CX_MACHINE_MAINTENANCE_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加Sku与模具关系数据为空日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具关系配置为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addMouldRelationEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具关系配置为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.MOULD_RELATION_INFO_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加新模具到货数据为空日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具到货计划为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addMouldDeliveryEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具到货计划为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.MOULD_DELIVERY_INFO_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加成型硫化配比数据为空日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到结构成型硫化配比为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addCxLhGroupRatioEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到结构成型硫化配比为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CX_GROUP_LH_RATIO_EMPTY, logContent);
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
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SINGLE_GROUP_LH_RATIO_EMPTY, logContent);
        log.info(logContent);
        return logContent;
    }

    /**
     * 增加续作Sku没有数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有获取到续作Sku信息====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addContinueSkuEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有获取到续作Sku信息====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_SKU_DATA_EMPTY, logContent);
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
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_NO_SELECTED_CX_MACHINE, logContent);
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
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_NO_SELECTED_ZERO_MATCH_CX_MACHINE, logContent);
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
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_NO_SELECTED_LIMIT_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加结构没有匹配到成型机-限制生产日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 机型：%s 没有成型硫化配比配置====
     *
     * @param context       排程上下文
     * @param groupName     分组名
     * @param isZeroRack    分组是否要求零度
     * @param cxMachineCode 成型机台
     * @param brandCode     机型
     * @return
     */
    public static String addGroupNoSelectedNoRatioLog(Context context, String groupName, String isZeroRack, String cxMachineCode, String brandCode) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 零度：%s 成型机台：%s 机型：%s 没有成型硫化配比配置====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName, isZeroRack, cxMachineCode, brandCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_NO_SELECTED_RATIO_CX_MACHINE, logContent);
        return logContent;
    }

    /**
     * 增加结构没有获取到合适的成型机数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 没有合适的成型机====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addGroupNoSelectedCxMachineLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s, 结构：%s 没有合适的成型机====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.GROUP_NO_SELECTED_CX_MACHINE, logContent);
        return logContent;
    }

    private TbrProductionGroupLogRecorder() {

    }
}
