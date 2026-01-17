package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.factory.domain.vo.EmbryoSpecialMaterialInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionDayInfoVo;
import com.zlt.aps.factory.domain.vo.SpecialMaterialStockVo;
import com.zlt.aps.factory.enums.TbrMouldProductionLogType;
import com.zlt.aps.factory.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 分组计划排产前-数据准备日志记录器
 * TBR-结构名
 * PCR-英寸
 *
 * @author ZLT
 * @date 20260105
 */
@Slf4j
public class TbrBeforeProductionGroupLogRecorder {
    /**
     * 增加开始结构日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，分组结构排产开始====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addStartGroupLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，分组结构排产开始====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
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
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，获取排产版本计划数据结束====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.END_GET_VERSION_DATA, logContent);
        return logContent;
    }


    /**
     * 增加获取排产计划数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，物料：%s 计划Id：%s 设置初始的排产数据====
     *
     * @param context    排程上下文
     * @param singlePlan 计划
     * @return
     */
    public static String addSetInitPlanInfoLog(Context context, MonthPlanProductionRequirePlanVo singlePlan) {
        String materialDesc = singlePlan.getMaterialDesc();
        Long monthPlanId = singlePlan.getMonthPlanId();
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，物料：%s 计划Id：%s 设置初始的排产数据====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                materialDesc, monthPlanId);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.PLAN_INIT_DATA, logContent);
        return logContent;
    }

    /**
     * 增加获排产前的数据加载日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，开始排产前数据准备加载====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addStartBeforeProductionDataLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，开始排产前数据准备加载====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.START_BEFORE_PRODUCTION_DATA, logContent);
        return logContent;
    }

    /**
     * 增加获取排产参数日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取排产参数====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取排产参数====
     *
     * @param context            排程上下文
     * @param paramConfiguration 排产参数配置
     * @return
     */
    public static String addReaderProductionParamLog(Context context, ProductionCapacityParamConfiguration paramConfiguration) {
        String logContentFormat;
        if (null == paramConfiguration) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到排产参数为空====";
        } else {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取排产参数====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加获读取特殊原材料日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取特殊原材料信息为空====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到特殊原材料数据====
     *
     * @param context             排程上下文
     * @param specialMaterialInfo 特殊原材料清单
     * @return
     */
    public static String addReaderSpecialMaterialLog(Context context, List<EmbryoSpecialMaterialInfoVo> specialMaterialInfo) {
        String logContentFormat;
        if (CollectionUtils.isEmpty(specialMaterialInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到特殊原材料数据为空====";
        } else {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到特殊原材料数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加获读取特殊原材料库存日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取特殊原材料库存为空====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到特殊原材料库存数据====
     *
     * @param context                  排程上下文
     * @param specialMaterialStockInfo 特殊原材料库存信息
     * @return
     */
    public static String addReaderSpecialMaterialStockLog(Context context, List<SpecialMaterialStockVo> specialMaterialStockInfo) {
        String logContentFormat;
        if (CollectionUtils.isEmpty(specialMaterialStockInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到特殊原材料库存数据为空====";
        } else {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到特殊原材料库存数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加获读取特殊原材料库存日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到生产日历信息为空====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到生产日历数据====
     *
     * @param context           排程上下文
     * @param productionDayInfo 生产日历信息
     * @return
     */
    public static String addProductionCalendarLog(Context context, List<ProductionDayInfoVo> productionDayInfo) {
        String logContentFormat;
        if (CollectionUtils.isEmpty(productionDayInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到生产日历数据为空====";
        } else {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到生产日历数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加没有停工日日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有停工日====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到有停工日设置===="
     *
     * @param context  排程上下文
     * @param stopDays 停产信息
     * @return
     */
    public static String addStopCalendarLog(Context context, List<ProductionDayInfoVo> stopDays) {
        String logContentFormat;
        if (CollectionUtils.isEmpty(stopDays)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有停工日====";
        } else {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到有停工日设置====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
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
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到成型机基础信息为空====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
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
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到成型机维修信息为空====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
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
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具关系配置为空====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加Sku与模具关系数据为空日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到可用模具关系为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addEnableMouldRelationEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到可用模具关系为空====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
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
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具到货计划为空====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
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
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到结构成型硫化配比为空====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加模具分配配比数据为空日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具分配比例配置为空====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addMouldAllocationEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具分配比例配置为空====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
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
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有获取到续作Sku信息====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }


    /**
     * 增加在机分组计划没有在产机台信息日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机分组 %s 没有在产机台====
     *
     * @param context   排程上下文
     * @param groupName 分组名
     * @return
     */
    public static String addContinueGroupNoOnLineMachineLog(Context context, String groupName) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机分组 %s 没有在产机台====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_NO_ON_LINE_MACHINE_EMPTY, logContent);
        return logContent;
    }

    private TbrBeforeProductionGroupLogRecorder() {

    }
}
