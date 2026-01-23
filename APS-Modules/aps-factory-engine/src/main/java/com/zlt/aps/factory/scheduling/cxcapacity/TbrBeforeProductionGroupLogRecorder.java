package com.zlt.aps.factory.scheduling.cxcapacity;

import com.tlt.aps.constant.StringConstant;
import com.zlt.aps.factory.daylimit.MouldAllocationInfoVo;
import com.zlt.aps.factory.daylimit.MouldShellBaseInfoVo;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ContinueGroupInfo;
import com.zlt.aps.factory.domain.dto.ContinueProductInfo;
import com.zlt.aps.factory.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.TbrMouldProductionLogType;
import com.zlt.aps.factory.utils.TbrProductionLogUtils;
import com.zlt.aps.monthplan.api.domain.entity.MdmCapsuleChuck;
import com.zlt.aps.monthplan.api.domain.entity.MdmWorkWearInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到特殊原材料数据为空====";
        if (!CollectionUtils.isEmpty(specialMaterialInfo)) {
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
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到特殊原材料库存数据为空====";
        if (!CollectionUtils.isEmpty(specialMaterialStockInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到特殊原材料库存数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加获读取工作日历日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到生产日历信息为空====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到生产日历数据====
     *
     * @param context           排程上下文
     * @param productionDayInfo 生产日历信息
     * @return
     */
    public static String addReaderProductionCalendarLog(Context context, List<ProductionDayInfoVo> productionDayInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到生产日历数据为空====";
        if (!CollectionUtils.isEmpty(productionDayInfo)) {
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
    public static String addReaderStopCalendarLog(Context context, List<ProductionDayInfoVo> stopDays) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有停工日====";
        if (!CollectionUtils.isEmpty(stopDays)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到有停工日设置====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加成型机基础数据读取日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到成型机基础数据为空====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到成型机基础数据====
     *
     * @param context       排程上下文
     * @param cxMachineInfo 成型机信息
     * @return
     */
    public static String addReaderCxMachineInfoLog(Context context, List<CxMachineBaseInfoVo> cxMachineInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到成型机基础数据为空====";
        if (!CollectionUtils.isEmpty(cxMachineInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到成型机基础数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加成型机基础数据读取日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到成型机维修数据为空====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到成型机维修数据====
     *
     * @param context    排程上下文
     * @param cxStopInfo 成型机停机信息
     * @return
     */
    public static String addReadCxMachineMaintenanceInfoLog(Context context, List<CxDevicePlanShutInfoVo> cxStopInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到成型机维修数据为空====";
        if (!CollectionUtils.isEmpty(cxStopInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到成型机维修数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加工装台账数据读取日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到工装台账数据为空====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到工装台账数据====
     *
     * @param context         排程上下文
     * @param allWorkWearInfo 工装台账信息
     * @return
     */
    public static String addReaderWorkWearInfoLog(Context context, List<MdmWorkWearInfo> allWorkWearInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到工装台账数据为空====";
        if (!CollectionUtils.isEmpty(allWorkWearInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到工装台账数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加Sku与模具关系数据读取日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具关系配置数据为空====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具关系配置数据====
     *
     * @param context          排程上下文
     * @param productMouldInfo 模具关系
     * @return
     */
    public static String addReaderMouldRelationLog(Context context, List<MonthPlanProductMouldInfoVo> productMouldInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具关系配置数据为空====";
        if (!CollectionUtils.isEmpty(productMouldInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具关系配置数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加Sku与模具关系读取数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到可用模具关系为空====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到可用模具关系数据====
     *
     * @param context                 排程上下文
     * @param enableMouldRelationInfo 可用模具信息
     * @return
     */
    public static String addEnableMouldRelationLog(Context context, List<MonthPlanProductMouldInfoVo> enableMouldRelationInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到可用模具关系数据为空====";
        if (!CollectionUtils.isEmpty(enableMouldRelationInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到可用模具关系数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加新模具到货数据读取日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具到货计划数据为空====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到模具到货计划数据====
     *
     * @param context           排程上下文
     * @param mouldDeliveryInfo 模具到货信息
     * @return
     */
    public static String addReaderMouldDeliveryLog(Context context, List<MonthPlanProductMouldInfoVo> mouldDeliveryInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具到货计划为空====";
        if (!CollectionUtils.isEmpty(mouldDeliveryInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到模具到货计划数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加成型硫化配比数据读取日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到结构成型硫化配比为空====
     *
     * @param context              排程上下文
     * @param structureLhRatioInfo 成型硫化配比信息
     * @return
     */
    public static String addReaderCxLhGroupRatioLog(Context context, List<MonthPlanStructureLhRatioVo> structureLhRatioInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到结构成型硫化配比数据为空====";
        if (!CollectionUtils.isEmpty(structureLhRatioInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到结构成型硫化配比数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加模壳数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模壳数据为空====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到模壳数据====
     *
     * @param context        排程上下文
     * @param mouldShellInfo 模壳数据信息
     * @return
     */
    public static String addReaderMouldShellLog(Context context, List<MouldShellBaseInfoVo> mouldShellInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模壳数据为空====";
        if (!CollectionUtils.isEmpty(mouldShellInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到模壳数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加胶囊卡盘数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到胶囊卡盘数据为空====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到胶囊卡盘数据====
     *
     * @param context             排程上下文
     * @param allCapsuleChuckInfo 胶囊卡盘信息
     * @return
     */
    public static String addReaderCapsuleChuckLog(Context context, List<MdmCapsuleChuck> allCapsuleChuckInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到胶囊卡盘数据为空====";
        if (!CollectionUtils.isEmpty(allCapsuleChuckInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到胶囊卡盘数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加模具分配配比数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具分配比例配置数据为空====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到模具分配比例配置数据====
     *
     * @param context             排程上下文
     * @param mouldAllocationInfo 模具分配比例信息
     * @return
     */
    public static String addReaderMouldAllocationLog(Context context, List<MouldAllocationInfoVo> mouldAllocationInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到模具分配比例配置数据为空====";
        if (!CollectionUtils.isEmpty(mouldAllocationInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到模具分配比例配置数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加读取前一个月的排产版本日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有读取到前一个月[%d-%d]的定稿排产版本====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到前一个月[%d-%d]的定稿排产版本 需求版本 %s 排产版本 %s ====
     *
     * @param context       排程上下文
     * @param previousMonth 排产参数配置
     * @return
     */
    public static String addReaderPreviousMonthLog(Context context, LocalDate previousMonth, MpFactoryProductionVersion previousVersion) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有读取到前一个月[%d-%d]的定稿排产版本====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                previousMonth.getYear(), previousMonth.getMonthValue());
        if (null != previousVersion) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到前一个月[%d-%d]的定稿排产版本 需求版本 %s 排产版本 %s====";
            logContent = String.format(logContentFormat,
                    context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                    previousMonth.getYear(), previousMonth.getMonthValue(), previousVersion.getMonthPlanVersion(), previousVersion.getProductionVersion());
        }
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
    public static String addReaderPreviousMonthProductionCalendarLog(Context context, List<ProductionDayInfoVo> productionDayInfo, LocalDate previousMonth) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，读取到前一个月[%d-%d]生产日历数据为空====";
        if (!CollectionUtils.isEmpty(productionDayInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到前一个月[%d-%d]生产日历数据====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                previousMonth.getYear(), previousMonth.getMonthValue());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加读取续作Sku日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有获取续作Sku信息====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到续作Sku信息====
     *
     * @param context         排程上下文
     * @param continueSkuInfo 续作Sku信息
     * @return
     */
    public static String addReadContinueSkuDataLog(Context context, List<ContinueProductInfo> continueSkuInfo) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有获取续作Sku信息====";
        if (!CollectionUtils.isEmpty(continueSkuInfo)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到续作Sku信息====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }

    /**
     * 增加读取在机结构日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有获取到在机结构信息====
     * 或是
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到在机结构信息====
     *
     * @param context               排程上下文
     * @param continueGroupInfoList 续作Sku信息
     * @return
     */
    public static String addReadContinueGroupDataLog(Context context, List<ContinueGroupInfo> continueGroupInfoList) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，没有获取到在机结构信息====";
        if (!CollectionUtils.isEmpty(continueGroupInfoList)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，已读取到在机结构信息====";
        }
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BEFORE_PRODUCTION_DATA_LOADING, logContent);
        return logContent;
    }


    /**
     * 增加在机分组计划没有在产机台信息日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机分组 %s 没有在产机台====
     *
     * @param context          排程上下文
     * @param groupName        分组名
     * @param onLineMachineSet 续作在产机台
     * @return
     */
    public static String addContinueGroupNoOnLineMachineLog(Context context, String groupName, Set<String> onLineMachineSet) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机分组 %s 没有在产机台====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        if (!CollectionUtils.isEmpty(onLineMachineSet)) {
            logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机分组 %s 在产机台: %s====";
            String onLineMachineInfo = String.join(StringConstant.COMMA, onLineMachineSet);
            logContent = String.format(logContentFormat,
                    context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                    groupName, onLineMachineInfo);
        }
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.CONTINUE_GROUP_NO_ON_LINE_MACHINE_EMPTY, logContent);
        return logContent;
    }

    private TbrBeforeProductionGroupLogRecorder() {

    }
}
