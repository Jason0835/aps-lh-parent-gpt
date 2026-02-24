package com.zlt.aps.mp.engine.logrecorder;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.mp.engine.utils.TbrProductionLogUtils;
import com.zlt.aps.mp.engine.enums.TbrMouldProductionLogType;
import lombok.extern.slf4j.Slf4j;

/**
 * TBR排产日志工具类型
 *
 * @author ZLT
 * @date 20251210
 */
@Slf4j
public class TbrProductionInitLogRecorder {
    /**
     * 增加开始初始化日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，排产初始化及检查开始====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addStartInitLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，排产初始化及检查开始====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.START_INIT, logContent);
        return logContent;
    }

    /**
     * 增加初始化读取参数数据为空日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化读取业务参数数据为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addInitParamEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化读取业务参数数据为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.INIT_GET_PARAM_DATA, logContent);
        return logContent;
    }

    /**
     * 增加初始化计算损耗值日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化物料：%s 损耗值 %s====
     *
     * @param context      排程上下文
     * @param materialDesc 物料描述
     * @param lossQty      总损耗量
     * @return
     */
    public static String addInitLossQtyLog(Context context, String materialDesc, Integer lossQty) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化物料：%s 损耗值 %s====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), materialDesc, lossQty);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.INIT_LOSS_QTY, logContent);
        return logContent;
    }

    /**
     * 增加物料基础数据读取为空日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化获取物料基础数据为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addMaterialInfoEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化获取物料基础数据为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.INIT_MATERIAL_DATA, logContent);
        return logContent;
    }

    /**
     * 增加没有找到物料基础数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化物料：%s 没有找到物料基础数据====
     *
     * @param context      排程上下文
     * @param materialDesc 物料描述
     * @return
     */
    public static String addSingleMaterialInfoEmptyLog(Context context, String materialDesc) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化物料：%s 没有找到物料基础数据====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), materialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.INIT_SINGLE_MATERIAL_DATA, logContent);
        return logContent;
    }

    /**
     * 增加Sku施工信息读取为空日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化获取Sku施工信息数据为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addConstructionInfoEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化获取Sku施工信息数据为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.INIT_CONSTRUCTION_DATA, logContent);
        return logContent;
    }

    /**
     * 增加没有找到物料施工关系日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化物料：%s 没有找到物料施工关系====
     *
     * @param context      排程上下文
     * @param materialDesc 物料描述
     * @return
     */
    public static String addSingleConstructionInfoEmptyLog(Context context, String materialDesc) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化物料：%s 没有找到物料施工关系====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), materialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.INIT_SINGLE_CONSTRUCTION_DATA, logContent);
        return logContent;
    }

    /**
     * 增加Sku与模具关系数据为空日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，初始化读取到模具关系配置为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addMouldRelationEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，初始化读取到模具关系配置为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.INIT_MOULD_RELATION_INFO_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加新模具到货数据为空日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，初始化读取到模具到货计划为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addMouldDeliveryEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，初始化读取到模具到货计划为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.INIT_MOULD_DELIVERY_INFO_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加Sku没有找到模具关系日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，初始化 物料：%s 没有找到模具关系====
     *
     * @param context      排程上下文
     * @param materialDesc 物料描述
     * @return
     */
    public static String addSingleMouldRelationEmptyLog(Context context, String materialDesc) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，初始化 物料：%s 没有找到模具关系====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), materialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.INIT_SINGLE_MOULD_INFO_EMPTY, logContent);
        return logContent;
    }

    /**
     * 增加Sku日硫化信息读取为空日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化获取Sku日硫化信息数据为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addDayLhCapacityInfoEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化获取Sku日硫化信息数据为空====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.INIT_DAY_LH_CAPACITY_DATA, logContent);
        return logContent;
    }

    /**
     * 增加没有找到物料施工关系日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化物料：%s 没有找到日硫化量配置====
     *
     * @param context      排程上下文
     * @param materialDesc 物料描述
     * @return
     */
    public static String addSingleDayLhCapacityInfoEmptyLog(Context context, String materialDesc) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化物料：%s 没有找到日硫化量配置====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), materialDesc);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.INIT_SINGLE_DAY_LH_CAPACITY_DATA, logContent);
        return logContent;
    }

    /**
     * 增加初始化结束日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，排产初始化及检查结束=====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addInitEndLog(Context context) {
        String initComplete = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，排产初始化及检查结束=====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.INIT_COMPLETE, initComplete);
        return initComplete;
    }

    /**
     * 增加初始化数据保存结束日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化数据存储结束=====
     *
     * @param context
     * @return
     */
    public static String addSaveInitDataLog(Context context) {
        String saveInitData = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s，初始化数据存储结束=====", context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SAVE_INIT, saveInitData);
        return saveInitData;
    }

    private TbrProductionInitLogRecorder() {

    }
}
