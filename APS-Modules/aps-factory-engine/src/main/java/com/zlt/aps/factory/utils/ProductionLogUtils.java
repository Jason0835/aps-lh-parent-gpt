package com.zlt.aps.factory.utils;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.ProductionPlanGroupVo;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMonthPlanInit;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 排程流程日志工具类
 * 纯计算类
 *
 * @author ZLT
 * @date 20250317
 */
@Slf4j
public class ProductionLogUtils {
    /**
     * 增加初始化检查排产日志记录
     *
     * @param productionContext 排产上下文
     */
    public static void addInitCheckFinishLog(ProductionContext productionContext, String text) {
        String checkFinish = String.format("%s排产版本：%s，检查完成", text, productionContext.getProductionVersion());
        log.info(checkFinish);
        MouldProductionLog initCheckFinish = buildProductionLog(productionContext, null, MouldProductionLogType.INIT_LOG, checkFinish);
        saveProductionLog(productionContext, initCheckFinish);
    }

    /**
     * 增加不排产计划记录结果日志记录
     *
     * @param productionContext 排产上下文
     * @param count             不排产记录数
     */
    public static void addNoProductionRecordResultLog(ProductionContext productionContext, String text, int count) {
        String initNoProductionPlan = String.format("%s排产版本：%s，共提取不排产计划条数：%d", text, productionContext.getProductionVersion(), count);
        log.info(initNoProductionPlan);
        MouldProductionLog initNoProductionPlanLog = buildProductionLog(productionContext, null, MouldProductionLogType.INIT_LOG, initNoProductionPlan);
        saveProductionLog(productionContext, initNoProductionPlanLog);
    }

    /**
     * 增加初始化完成流程日志
     *
     * @param productionContext 排产上下文
     */
    public static void addInitFinishLog(ProductionContext productionContext, String text) {
        String initComplete = String.format("===分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划%s排产初始化及检查结束==========", productionContext.getFactoryCode(), productionContext.getYear(), productionContext.getMonth(), productionContext.getMonthPlanVersion(), text);
        log.info(initComplete);
        MouldProductionLog initCompleteLog = buildProductionLog(productionContext, null, MouldProductionLogType.INIT_LOG, initComplete);
        saveProductionLog(productionContext, initCompleteLog);
    }

    /**
     * 增加开始进行续作计划使用续作模具开始排产流程日志
     * =====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划续作规格使用续作模具排产开始====
     *
     * @param productionContext 排产上下文
     */
    public static void addStartContinuePlanContinueMouldProductionLog(ProductionContext productionContext) {
        String startContinueProductContinueMouldContent = String.format("=====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划续作规格使用续作模具排产开始====", productionContext.getFactoryCode(), productionContext.getYear(), productionContext.getMonth(), productionContext.getMonthPlanVersion());
        log.info(startContinueProductContinueMouldContent);
        MouldProductionLog startContinueProductContinueMouldLog = buildProductionLog(productionContext, null, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, startContinueProductContinueMouldContent);
        saveProductionLog(productionContext, startContinueProductContinueMouldLog);
    }

    /**
     * 增加开始进行搭配排产流程日志
     * =====分厂%s, 计划年月：%d-%d, 计划版本：%s，搭配排产开始====
     *
     * @param productionContext 排产上下文
     */
    public static void addMatchingProductionLog(ProductionContext productionContext) {
        String startMatchingProductMouldContent = String.format("=====分厂%s, 计划年月：%d-%d, 计划版本：%s，搭配规格排产开始====", productionContext.getFactoryCode(), productionContext.getYear(), productionContext.getMonth(), productionContext.getMonthPlanVersion());
        log.info(startMatchingProductMouldContent);
        MouldProductionLog startMatchingProductContinueMouldLog = buildProductionLog(productionContext, null, MouldProductionLogType.MATCHING_PRODUCTION_GROUP_LOG, startMatchingProductMouldContent);
        saveProductionLog(productionContext, startMatchingProductContinueMouldLog);
    }

    /**
     * 增加 没有需要搭配排产 流程日志
     * =====无需搭配补量，即产能已满====
     *
     * @param productionContext 排产上下文
     */
    public static void addNoNeedMatchingProductionLog(ProductionContext productionContext) {
        String capacityFullContent = "=====无需搭配补量，即产能已满====";
        log.info(capacityFullContent);
        MouldProductionLog startMatchingProductContinueMouldLog = buildProductionLog(productionContext, null, MouldProductionLogType.MATCHING_PRODUCTION_GROUP_LOG, capacityFullContent);
        saveProductionLog(productionContext, startMatchingProductContinueMouldLog);
    }

    /**
     * 增加 没有可搭配排产的规格 流程日志
     * =====搭配排产没有可搭配补量的规格信息====
     *
     * @param productionContext 排产上下文
     */
    public static void addNoMatchingProductDataLog(ProductionContext productionContext) {
        String noMatchingProductDataContent = "=====搭配排产没有可搭配补量的规格信息====";
        log.info(noMatchingProductDataContent);
        MouldProductionLog startMatchingProductContinueMouldLog = buildProductionLog(productionContext, null, MouldProductionLogType.MATCHING_PRODUCTION_GROUP_LOG, noMatchingProductDataContent);
        saveProductionLog(productionContext, startMatchingProductContinueMouldLog);
    }

    /**
     * 增加没有续作计划的排产流程日志
     * ===没有续作排产计划
     *
     * @param productionContext 排产上下文
     */
    public static void addNoContinuePlanProductionLog(ProductionContext productionContext) {
        String noPlanLogContent = "===没有续作排产计划";
        log.info(noPlanLogContent);
        MouldProductionLog noPlanLog = buildProductionLog(productionContext, null, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, noPlanLogContent);
        saveProductionLog(productionContext, noPlanLog);
    }

    /**
     * 增加开始分组排产流程日志
     * %s开始进行[%s]分组排产
     *
     * @param productionContext 排产上下文
     * @param groupInfo         分组信息
     * @param prefixText        前缀信息
     */
    public static void addGroupStartProductionLog(ProductionContext productionContext, ProductionPlanGroupVo groupInfo, String prefixText) {
        String startGroupContent = String.format("%s开始进行[%s]分组排产", prefixText, groupInfo.getGroup().getRemark());
        log.info(startGroupContent);
        MouldProductionLog startGroupProductionLog = buildProductionLog(productionContext, null, MouldProductionLogType.GROUP_PRODUCTION_LOG, startGroupContent);
        saveProductionLog(productionContext, startGroupProductionLog);
    }

    /**
     * 增加开始按寸口由大到小排产模式流程日志
     * %s开始进行按寸口由大到小排产方式排产
     *
     * @param productionContext 排产上下文
     * @param prefixText        前缀信息
     */
    public static void addProSizeModelStartProductionLog(ProductionContext productionContext, String prefixText) {
        String startProSizeModelProductionContent = String.format("%s开始进行按寸口由大到小排产方式排产", prefixText);
        log.info(startProSizeModelProductionContent);
        MouldProductionLog startProSizeModelProductionLog = buildProductionLog(productionContext, null, MouldProductionLogType.PRO_SIZE_MODEL_PRODUCTION_LOG, startProSizeModelProductionContent);
        saveProductionLog(productionContext, startProSizeModelProductionLog);
    }

    /**
     * 增加日排产量结果日志
     *
     * @param productionContext 排产上下文
     * @param logType           排产类型
     */
    public static void addDayProductionQtyResultLog(ProductionContext productionContext, MouldProductionLogType logType) {
        Map<String, Map<Integer, Long>> groupDayProductionQty = productionContext.getGroupDayProductionQtyInfo();
        String resultInfo = "";
        if (!CollectionUtils.isEmpty(groupDayProductionQty)) {
            resultInfo = JSON.toJSONString(groupDayProductionQty);
        }
        String dayProductionMouldQtyContent = String.format("日排产数结果：%s", resultInfo);
        MouldProductionLog dayProductionMouldQtyLog = buildProductionLog(productionContext, null, logType, dayProductionMouldQtyContent);
        saveProductionLog(productionContext, dayProductionMouldQtyLog);
    }

    /**
     * 增加日排产最大模具数控制信息日志
     *
     * @param productionContext 排产上下文
     * @param logType           排产类型
     */
    public static void addDayMaxMouldQtyResultLog(ProductionContext productionContext, MouldProductionLogType logType) {
        Map<String, Map<Integer, Integer>> dayMaxMouldQtyMap = productionContext.getGroupDayMaxMouldQtyInfo();
        String resultInfo = "";
        if (!CollectionUtils.isEmpty(dayMaxMouldQtyMap)) {
            resultInfo = JSON.toJSONString(dayMaxMouldQtyMap);
        }
        String dayMaxMouldQtyContent = String.format("日排产最大模具数控制信息：%s", resultInfo);
        MouldProductionLog dayMaxMouldQtyLog = buildProductionLog(productionContext, null, logType, dayMaxMouldQtyContent);
        saveProductionLog(productionContext, dayMaxMouldQtyLog);
    }

    /**
     * 增加日排产模具数量结果日志
     *
     * @param productionContext 排产上下文
     * @param logType           排产类型
     */
    public static void addDayProductionMouldQtyResultLog(ProductionContext productionContext, MouldProductionLogType logType) {
        Map<String, Map<Integer, Integer>> dayProductionMouldQtyMap = productionContext.getGroupDayProductionMouldQtyInfo();
        String resultInfo = "";
        if (!CollectionUtils.isEmpty(dayProductionMouldQtyMap)) {
            resultInfo = JSON.toJSONString(dayProductionMouldQtyMap);
        }
        String dayProductionMouldQtyContent = String.format("日排产模具数结果：%s", resultInfo);
        MouldProductionLog dayProductionMouldQtyLog = buildProductionLog(productionContext, null, logType, dayProductionMouldQtyContent);
        saveProductionLog(productionContext, dayProductionMouldQtyLog);
    }


    /**
     * 增加日排产模具数量结果日志
     *
     * @param productionContext     排产上下文
     * @param groupKey              分组维度key
     * @param logType               排产类型
     * @param productionMouldQtyMap 日排产模具数信息
     */
    public static void addCurrentGroupDayProductionMouldQtyLog(ProductionContext productionContext, String groupKey, Integer productionDate, MouldProductionLogType logType, Map<Integer, Integer> productionMouldQtyMap) {
        String resultInfo = "";
        if (!CollectionUtils.isEmpty(productionMouldQtyMap)) {
            resultInfo = JSON.toJSONString(productionMouldQtyMap);
        }
        String groupDayProductionMouldQtyContent = String.format("当前【%s】在%s日排产-模具数分布情况：%s", groupKey, productionDate, resultInfo);
        MouldProductionLog groupDayProductionMouldQtyLog = buildProductionLog(productionContext, null, logType, groupDayProductionMouldQtyContent);
        saveProductionLog(productionContext, groupDayProductionMouldQtyLog);
    }

    /**
     * 增加开始按寸口由大到小排产模式流程日志
     * %s开始进行寸口[%s]排产
     *
     * @param productionContext 排产上下文
     * @param proSize           排产寸口
     * @param prefixText        前缀信息
     */
    public static void addProSizeStartProductionLog(ProductionContext productionContext, BigDecimal proSize, String prefixText) {
        String startProSizeProductionContent = String.format("%s开始进行寸口[%s]排产", prefixText, proSize);
        log.info(startProSizeProductionContent);
        MouldProductionLog startProSizeProductionLog = buildProductionLog(productionContext, null, MouldProductionLogType.PRO_SIZE_MODEL_PRODUCTION_LOG, startProSizeProductionContent);
        saveProductionLog(productionContext, startProSizeProductionLog);
    }

    /**
     * 增加结束分组排产流程日志
     * %s[%s]分组排产结束
     *
     * @param productionContext 排产上下文
     * @param groupInfo         分组信息
     * @param prefixText        前缀信息
     */
    public static void addGroupEndProductionLog(ProductionContext productionContext, ProductionPlanGroupVo groupInfo, String prefixText) {
        //排产流程日志记录
        String endGroupContent = String.format("%s[%s]分组排产结束", prefixText, groupInfo.getGroup().getRemark());
        log.info(endGroupContent);
        MouldProductionLog endGroupProductionLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.GROUP_PRODUCTION_LOG, endGroupContent);
        saveProductionLog(productionContext, endGroupProductionLog);
    }

    /**
     * 增加有交期计划开始排产的流程日志
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param mouldSize         模具数量
     * @param singleCuringTime  单条硫化时间(包含每条间隔增加时间)
     */
    public static void addDeliveryDatePlanStartProductionLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Integer mouldSize, BigDecimal singleCuringTime) {
        String factoryCode = productionContext.getFactoryCode();
        Integer year = productionContext.getYear();
        Integer month = productionContext.getMonth();
        Long monthPlanId = productionPlan.getMonthPlanId();
        String productionVersion = productionPlan.getProductionVersion();
        String deliveryDateProductionLogFormat = "===分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划单计划%d 交期计划排产开始：预计交期日期：%s 计划排产量%d,可用模具数%d,单条硫化时间%d====";
        Date deliveryDateDue = productionPlan.getDeliveryDateDue();
        String deliveryDateText = "--";
        if (null != deliveryDateDue) {
            deliveryDateText = com.ruoyi.common.core.utils.DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, deliveryDateDue);
        }
        String deliveryDateProductionLogContent = String.format(deliveryDateProductionLogFormat, factoryCode, year, month, productionVersion, monthPlanId, deliveryDateText, productionPlan.getProductionQty(), mouldSize, singleCuringTime.longValue());
        log.info(deliveryDateProductionLogContent);
        MouldProductionLog deliveryDateProductionLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_DELIVERY_LOG, deliveryDateProductionLogContent);
        saveProductionLog(productionContext, deliveryDateProductionLog);
    }

    /**
     * 增加通用单计划开始排产流程日志记录
     * ===分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划单计划%d 通用计划排产开始：计划排产量%d,可用模具数%d,单条硫化时间%d====
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param mouldSize         可用模具数
     * @param singleCuringTime  硫化时间(包含单条间隔时间)
     */
    public static void addStartGeneralSinglePlanProductionLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Integer mouldSize, BigDecimal singleCuringTime) {
        String contentFormat = "===分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划单计划%d 通用计划排产开始：计划排产量%d,可用模具数%d,单条硫化时间%d====";
        String logInfoContent = String.format(contentFormat, productionContext.getFactoryCode(), productionContext.getYear(), productionContext.getMonth(), productionContext.getProductionVersion(), productionPlan.getMonthPlanId(), productionPlan.getProductionQty(), mouldSize, singleCuringTime.longValue());
        log.info(logInfoContent);
        MouldProductionLog logInfo = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, logInfoContent);
        saveProductionLog(productionContext, logInfo);
    }

    /**
     * 增加不同类型业务开始排产流程日志
     *
     * @param productionContext 排产上下文
     * @param businessTypeText  业务类型文本
     * @param logType           日志类型
     */
    public static void addStartTypeProductionLog(ProductionContext productionContext, String businessTypeText, MouldProductionLogType logType) {
        //排产流程开始日志记录
        String startSameProductionLogContent = String.format("=====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划%s排程开始====", productionContext.getFactoryCode(), productionContext.getYear(), productionContext.getMonth(), productionContext.getMonthPlanVersion(), businessTypeText);
        log.info(startSameProductionLogContent);
        MouldProductionLog startSameProductionLog = buildProductionLog(productionContext, null, logType, startSameProductionLogContent);
        saveProductionLog(productionContext, startSameProductionLog);
    }

    /**
     * 增加不同类型业务排产，没有排产计划流程日志
     *
     * @param productionContext 排产上下文
     * @param businessTypeText  业务类型文本
     * @param logType           日志类型
     */
    public static void addNoTypeProductionPlanLog(ProductionContext productionContext, String businessTypeText, MouldProductionLogType logType) {
        String noPlanLogContent = String.format("===%s排产：没有%s的物料排产计划", businessTypeText, businessTypeText);
        log.info(noPlanLogContent);
        MouldProductionLog noPlanLog = buildProductionLog(productionContext, null, logType, noPlanLogContent);
        saveProductionLog(productionContext, noPlanLog);
    }

    /**
     * 增加单计划排产前日志
     *
     * @param productionContext 排产上下文
     * @param productionPlan    需排产计划
     * @param logType           日志类型
     */
    public static void addBeforeSinglePlanProductionLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MouldProductionLogType logType) {
        String factoryCode = productionContext.getFactoryCode();
        Integer year = productionContext.getYear();
        Integer month = productionContext.getMonth();
        String productionVersion = productionContext.getProductionVersion();
        Long monthPlanId = productionPlan.getMonthPlanId();
        String singlePlanProductionStartLogContent = String.format("分厂%s,计划年月：%d-%d,排产版本:%s，单计划[%s]模具排产开始,计划排产量%d", factoryCode, year, month, productionVersion, monthPlanId, productionPlan.getProductionQty());
        log.info(singlePlanProductionStartLogContent);
        MouldProductionLog singlePlanProductionStartLog = buildProductionLog(productionContext, productionPlan, logType, singlePlanProductionStartLogContent);
        saveProductionLog(productionContext, singlePlanProductionStartLog);
    }

    /**
     * 增加模具没有产能的排产日志
     * %d :排产计划可用模具没有产能
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param logType           日志类型
     */
    public static void addNoCapacityByMouldLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MouldProductionLogType logType) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String noCapacityLogContent = String.format("%d :排产计划可用模具没有产能", monthPlanId);
        log.warn(noCapacityLogContent);
        MouldProductionLog noCapacityLog = buildProductionLog(productionContext, productionPlan, logType, noCapacityLogContent);
        saveProductionLog(productionContext, noCapacityLog);
    }

    /**
     * 增加排产计划没有可排模具排产的日志
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param logType           日志类型
     */
    public static void addNoEnableMouldProductionLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MouldProductionLogType logType) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String noEnableMouldLogContent = String.format("%d :排产计划没有可排模具", monthPlanId);
        log.warn(noEnableMouldLogContent);
        MouldProductionLog noEnableMouldLog = buildProductionLog(productionContext, productionPlan, logType, noEnableMouldLogContent);
        saveProductionLog(productionContext, noEnableMouldLog);
    }

    /**
     * 增加分组单计划排产前日志
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param logType           日志类型
     * @param groupName         分组名 有交期|通用
     */
    public static void addBeforeGroupSinglePlanProductionLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MouldProductionLogType logType, String groupName) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String groupPlanLogContent = String.format("===当前计划%d 为%s计划排产=====", monthPlanId, groupName);
        log.info(groupPlanLogContent);
        MouldProductionLog groupPlanLog = buildProductionLog(productionContext, productionPlan, logType, groupPlanLogContent);
        saveProductionLog(productionContext, groupPlanLog);
    }

    /**
     * 增加排产规格数限制日志及输出
     * [%d]计划,在[%d]日达到排产规格数限制，不能排产
     *
     * @param productionContext   排产上下文
     * @param logType             日志类型
     * @param productionPlan      排产计划
     * @param startProductionDate 排产日
     */
    public static void addProductionProductNumberLimitLog(ProductionContext productionContext, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo productionPlan, Integer startProductionDate) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String productionProductNumberLimitContent = String.format("[%d]计划,在[%d]日达到排产规格数限制(总规格数或是新增规格数)，不能排产", monthPlanId, startProductionDate);
        log.info(productionProductNumberLimitContent);
        MouldProductionLog productionProductNumberLimitLog = buildProductionLog(productionContext, productionPlan, logType, productionProductNumberLimitContent);
        saveProductionLog(productionContext, productionProductNumberLimitLog);
    }

    /**
     * 增加排产模具配比数限制日志及输出
     * [%d]计划,在[%d]日达到排产配比模具数限制，不能排产
     *
     * @param productionContext   排产上下文
     * @param logType             日志类型
     * @param productionPlan      排产计划
     * @param startProductionDate 排产日
     */
    public static void addProductionMouldQtyLimitLog(ProductionContext productionContext, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo productionPlan, Integer startProductionDate) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String productionMouldQtyLimitContent = String.format("[%d]计划,在[%d]日达到配比模具数限制，不能排产", monthPlanId, startProductionDate);
        log.info(productionMouldQtyLimitContent);
        MouldProductionLog productionMouldQtyLimitLog = buildProductionLog(productionContext, productionPlan, logType, productionMouldQtyLimitContent);
        saveProductionLog(productionContext, productionMouldQtyLimitLog);
    }

    /**
     * 增加不可连续排产流程日志
     *
     * @param productionContext  排产上下文
     * @param logType            日志类型
     * @param text               而外说明
     * @param productionPlan     排产计划
     * @param mouldCodeInfo      模具信息
     * @param nextProductionDate 后一个排产日
     * @param productionDate     当前排产日
     * @param productionQty      排产数量
     */
    public static void addNoContinueProductionByDayLimitCapacity(ProductionContext productionContext, MouldProductionLogType logType, String text, MonthPlanManufacturingRequirementVo productionPlan, String mouldCodeInfo, Integer nextProductionDate, Integer productionDate, Long productionQty) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String noContinueProductionFormat = "%s[%d]计划使用%s模具排产，在[%d]日没有排产量，导致在[%d]日排产量[%d]不能排";
        String noContinueProductionContext = String.format(noContinueProductionFormat, text, monthPlanId, mouldCodeInfo, nextProductionDate, productionDate, productionQty);
        log.info(noContinueProductionContext);
        MouldProductionLog noContinueProductionLog = buildProductionLog(productionContext, productionPlan, logType, noContinueProductionContext);
        saveProductionLog(productionContext, noContinueProductionLog);
    }

    /**
     * 增加不可连续排产流程日志
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param mouldCodeInfo     模具信息
     * @param productionDate    当前排产日
     * @param productionQty     排产数量
     */
    public static void addNoDayLeftOverQtyLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Integer productionDate, String mouldCodeInfo, Long productionQty) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String noContinueProductionFormat = "[%d]计划在[%d]日没有日剩余排产量，导致使用%s模具排产量[%d]不能排";
        String noContinueProductionContext = String.format(noContinueProductionFormat, monthPlanId, productionDate, mouldCodeInfo, productionQty);
        log.info(noContinueProductionContext);
        MouldProductionLog noContinueProductionLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.DAY_LEFT_OVER_LOG, noContinueProductionContext);
        saveProductionLog(productionContext, noContinueProductionLog);
    }

    /**
     * 增加查找拼模计划的流程日志记录
     * [%d]计划查找可[%s]拼模的规格计划......
     *
     * @param productionContext  排产上下文
     * @param productionPlan     排产计划
     * @param assemblingTypeText 拼模类型文本
     */
    public static void addFindAssemblingMouldPlanLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, String assemblingTypeText) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String findAssemblingPlanFormat = "[%d]计划查找可[%s]拼模的规格计划......";
        String findAssemblingPlanContext = String.format(findAssemblingPlanFormat, monthPlanId, assemblingTypeText);
        log.info(findAssemblingPlanContext);
        MouldProductionLog findAssemblingPlanLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.ASSEMBLING_MOULD_LOG, findAssemblingPlanContext);
        saveProductionLog(productionContext, findAssemblingPlanLog);
    }

    /**
     * 增加查找衔接分组信息日志
     *
     * @param productionContext   排产上下文
     * @param productionPlan      排产计划
     * @param productionGroupText 衔接分组内容
     */
    public static void addFindLinkProductionGroupLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, String productionGroupText) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String findLinkProductionGroupFormat = "[%d]计划查找可衔接分组信息：%s";
        String findLinkProductionGroupContext = String.format(findLinkProductionGroupFormat, monthPlanId, productionGroupText);
        log.info(findLinkProductionGroupContext);
        MouldProductionLog findLinkProductionGroupLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.FIND_PRODUCTION_GROUP_LOG, findLinkProductionGroupContext);
        saveProductionLog(productionContext, findLinkProductionGroupLog);
    }

    /**
     * 增加排产分组中时间差异处理日志
     *
     * @param productionContext  排产上下文
     * @param productionPlan     排产计划
     * @param diffProductionText 衔接分组内容
     */
    public static void addDiffDateProductionGroupLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, String diffProductionText) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String diffDateProductionGroupFormat = "[%d]计划双模排产分组时间不一致排产处理：%s";
        String diffDateProductionGroupContext = String.format(diffDateProductionGroupFormat, monthPlanId, diffProductionText);
        log.info(diffDateProductionGroupContext);
        MouldProductionLog diffDateProductionGroupLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.DIFF_DATE_PRODUCTION_GROUP_LOG, diffDateProductionGroupContext);
        saveProductionLog(productionContext, diffDateProductionGroupLog);
    }

    /**
     * 增加创建续作排产分组信息日志--初始的排产分组信息
     *
     * @param productionContext   排产上下文
     * @param productionPlan      排产计划
     * @param productionGroupText 续作分组内容
     */
    public static void addBuildContinueProductionGroupLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, String productionGroupText) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String buildContinueProductionGroupFormat = "[%d]计划创建续作初始排产分组信息：%s";
        String buildContinueProductionGroupContext = String.format(buildContinueProductionGroupFormat, monthPlanId, productionGroupText);
        log.info(buildContinueProductionGroupContext);
        MouldProductionLog buildContinueProductionGroupLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.BUILD_CONTINUE_PRODUCTION_GROUP_LOG, buildContinueProductionGroupContext);
        saveProductionLog(productionContext, buildContinueProductionGroupLog);
    }

    /**
     * 增加拼模排产计划需另外找拼模计划日志
     *
     * @param productionContext   排产上下文
     * @param productionPlan      拼模前规格计划
     * @param assemblingMouldPlan 找到的可拼模计划规格
     * @param sameEmbryoCodePlan  具有共生胎的另一个拼模计划规格
     */
    public static void addRejectAssemblingMouldPlanLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MonthPlanManufacturingRequirementVo assemblingMouldPlan, MonthPlanManufacturingRequirementVo sameEmbryoCodePlan) {
        String format = "对%s找到另外一个共生胎可拼模排产的规格%s，故而不与%s进行拼模排产";
        String context = String.format(format, assemblingMouldPlan.getProductCode(), sameEmbryoCodePlan.getProductCode(), productionPlan.getProductCode());
        log.info(context);
        MouldProductionLog resetFindAssemblingPlanLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.ASSEMBLING_MOULD_LOG, context);
        saveProductionLog(productionContext, resetFindAssemblingPlanLog);
    }

    /**
     * 增加拼模排产计划没有找到另外共生胎的拼模计划日志
     *
     * @param productionContext   排产上下文
     * @param productionPlan      拼模前规格计划
     * @param assemblingMouldPlan 找到的可拼模计划规格
     */
    public static void addNoRejectAssemblingMouldPlanLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MonthPlanManufacturingRequirementVo assemblingMouldPlan) {
        String format = "对%s没有找到另外一个共生胎可拼模排产的规格，故而可与%s进行拼模排产";
        String context = String.format(format, assemblingMouldPlan.getProductCode(), productionPlan.getProductCode());
        log.info(context);
        MouldProductionLog resetFindAssemblingPlanLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.ASSEMBLING_MOULD_LOG, context);
        saveProductionLog(productionContext, resetFindAssemblingPlanLog);
    }

    /**
     * 增加没有找到拼模模具的流程日志
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     */
    public static void addNoAssemblingMouldByMouldLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String noAssemblingMouldProductionFormat = "[%d]计划没有找到可拼模具的排产计划";
        String noAssemblingMouldProductionContext = String.format(noAssemblingMouldProductionFormat, monthPlanId);
        log.info(noAssemblingMouldProductionContext);
        MouldProductionLog noAssemblingMouldProductionLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.ASSEMBLING_MOULD_LOG, noAssemblingMouldProductionContext);
        saveProductionLog(productionContext, noAssemblingMouldProductionLog);
    }

    /**
     * 增加没有找到拼模计划流程日志
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     */
    public static void addNoAssemblingMouldLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String noAssemblingMouldProductionFormat = "[%d]计划没有找到可拼模排产的计划";
        String noAssemblingMouldProductionContext = String.format(noAssemblingMouldProductionFormat, monthPlanId);
        log.info(noAssemblingMouldProductionContext);
        MouldProductionLog noAssemblingMouldProductionLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.ASSEMBLING_MOULD_LOG, noAssemblingMouldProductionContext);
        saveProductionLog(productionContext, noAssemblingMouldProductionLog);
    }

    /**
     * 增加单模排产日志-记录，不一定会排
     * 计划ID: %d -阶段：%s ： 计划只用1副模具
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     */
    public static void addSingleMouldProductionLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan) {
        MouldProductionLog singleMouldLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, "计划只用1副模具");
        saveProductionLog(productionContext, singleMouldLog);
    }

    /**
     * 增加记录开始单模排产流程日志
     * [%d]计划使用[%s]模具[%s]排产,从%s日进行排产，需排产量[%d]
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param mouldInfo         模具信息
     * @param productionOrient  排产方向
     * @param dateRange         时间范围
     * @param needProductionQty 需排产量
     */
    public static void addStartSingleMouldProductionLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, String mouldInfo, ProductionOrientEnum productionOrient, String dateRange, Long needProductionQty) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String singleMouldProductionLogContent = String.format("[%d]计划使用[%s]模具[%s]排产,从%s日进行排产，需排产量[%d]", monthPlanId, mouldInfo, productionOrient.getDesc(), dateRange, needProductionQty);
        log.info(singleMouldProductionLogContent);
        MouldProductionLog singleMouldProductionLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_PRODUCTION_LOG, singleMouldProductionLogContent);
        saveProductionLog(productionContext, singleMouldProductionLog);
    }

    /**
     * 增加记录开始单模排产流程日志
     * [%d]计划使用[%s]模具[%s]排产,从%s日进行排产，还需要排产量[%d]
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param mouldInfo         模具信息
     * @param productionOrient  排产方向
     * @param dateRange         时间范围
     * @param needProductionQty 需排产量
     */
    public static void addSingleMouldProductionResultLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, String mouldInfo, ProductionOrientEnum productionOrient, String dateRange, Long needProductionQty) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String singleMouldProductionResultLogContent = String.format("[%d]计划使用[%s]模具[%s]排产,从%s日进行排产，还需要排产量[%d]", monthPlanId, mouldInfo, productionOrient.getDesc(), dateRange, needProductionQty);
        log.info(singleMouldProductionResultLogContent);
        MouldProductionLog singleMouldProductionResultLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_PRODUCTION_LOG, singleMouldProductionResultLogContent);
        saveProductionLog(productionContext, singleMouldProductionResultLog);
    }

    /**
     * [%d]计划使用[%s]模具[%s]排产,在[%d]日还需排产量[%d]
     * <p>
     * 计划使用模具排产前的日志记录
     *
     * @param productionContext 排产上下文
     * @param logType           日志类型
     * @param productionPlan    排产计划
     * @param mouldInfo         模具信息
     * @param productionOrient  排产方向
     * @param productionDate    排产日
     * @param needProductionQty 需排产量
     */
    public static void addBeforeMouldDayProductionLog(ProductionContext productionContext, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo productionPlan, String mouldInfo, ProductionOrientEnum productionOrient, Integer productionDate, Long needProductionQty) {
        String singleMouldProductionDateLogContent = String.format("[%d]计划使用[%s]模具[%s]排产,在[%d]日还需排产量[%d]", productionPlan.getMonthPlanId(), mouldInfo, productionOrient.getDesc(), productionDate, needProductionQty);
        log.info(singleMouldProductionDateLogContent);
        MouldProductionLog singleMouldProductionDateLog = buildProductionLog(productionContext, productionPlan, logType, singleMouldProductionDateLogContent);
        saveProductionLog(productionContext, singleMouldProductionDateLog);
    }

    /**
     * [%d]计划使用%s模具[%s]排产,从[%d]-[%d]日进行排产，需排产量[%d]
     *
     * @param productionContext 排产上下文
     * @param logType           日志类型
     * @param productionPlan    排产计划
     * @param mouldInfo         使用模具信息
     * @param productionOrient  排产方向
     * @param dateRange         排产日范围
     * @param needProductionQty 需排产量
     */
    public static void addProductionPlanCycleMouldStartLog(ProductionContext productionContext, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo productionPlan, String mouldInfo, ProductionOrientEnum productionOrient, String dateRange, Long needProductionQty) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        String startMouldProductionLogContent = String.format("[%d]计划使用%s模具[%s]排产,从%s日进行排产，需排产量[%d]", monthPlanId, mouldInfo, productionOrient.getDesc(), dateRange, needProductionQty);
        log.info(startMouldProductionLogContent);
        MouldProductionLog startMouldProductionLog = buildProductionLog(productionContext, productionPlan, logType, startMouldProductionLogContent);
        saveProductionLog(productionContext, startMouldProductionLog);
    }

    /**
     * [%d]计划使用[%s]模具[%s]排产,在[%d]日排产量[%d]
     * <p>
     * 计划使用模具在productionDate排产结果
     *
     * @param productionContext 排产上下文
     * @param logType           日志类型
     * @param productionPlan    排产计划
     * @param mouldInfo         模具信息
     * @param productionOrient  排产方向
     * @param productionDate    排产日
     * @param productionQty     排产量
     */
    public static void addProductionDateResultMouldProductionLog(ProductionContext productionContext, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo productionPlan, String mouldInfo, ProductionOrientEnum productionOrient, Integer productionDate, Long productionQty) {
        String singleMouldProductionDateResultLogContent = String.format("[%d]计划使用%s模具[%s]排产,在[%d]日排产量[%d]", productionPlan.getMonthPlanId(), mouldInfo, productionOrient.getDesc(), productionDate, productionQty);
        log.info(singleMouldProductionDateResultLogContent);
        MouldProductionLog singleMouldProductionDateResultLog = buildProductionLog(productionContext, productionPlan, logType, singleMouldProductionDateResultLogContent);
        saveProductionLog(productionContext, singleMouldProductionDateResultLog);
    }

    /**
     * [%d]计划使用[%s]、[%s]模具[%s]排产,从[%d]-[%d]日进行排产，还需要排产量[%d]
     * 增加计划使用模具在周期内的排产结果
     *
     * @param productionContext     排产上下文
     * @param logType               日志类型
     * @param productionPlan        排产计划
     * @param mouldInfo             模具信息
     * @param productionOrient      排产方向
     * @param dateRange             排产日范围(开始~结束日)
     * @param leftOverProductionQty 剩余需排产量
     */
    public static void addProductionCycleResultMouldProductionLog(ProductionContext productionContext, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo productionPlan, String mouldInfo, ProductionOrientEnum productionOrient, String dateRange, Long leftOverProductionQty) {
        String sumMouldProductionLogContent = String.format("[%d]计划使用%s模具[%s]排产,从%s日进行排产，还需要排产量[%d]", productionPlan.getMonthPlanId(), mouldInfo, productionOrient.getDesc(), dateRange, leftOverProductionQty);
        log.info(sumMouldProductionLogContent);
        MouldProductionLog sumMouldProductionLog = buildProductionLog(productionContext, productionPlan, logType, sumMouldProductionLogContent);
        saveProductionLog(productionContext, sumMouldProductionLog);
    }

    /**
     * 增加在续作计划开始排产前流程日志
     * ===开始对续作计划:%s - %s 使用续作模具排产，需排产量%d
     *
     * @param productionContext 排产上下文
     * @param logType           日志类型
     * @param continuePlan      续作计划
     */
    public static void addBeforeContinuePlanProductionLog(ProductionContext productionContext, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo continuePlan) {
        String startContinuePlanProductionContent = String.format("===开始对续作计划:%s - %s 使用续作模具排产，需排产量%d", continuePlan.getMonthPlanId(), continuePlan.getProductCode(), continuePlan.getProductionQty());
        log.info(startContinuePlanProductionContent);
        MouldProductionLog startContinuePlanProductionLog = buildProductionLog(productionContext, continuePlan, logType, startContinuePlanProductionContent);
        saveProductionLog(productionContext, startContinuePlanProductionLog);
    }

    /**
     * 增加续作模具在startProductionDate排产日志
     * ===续作计划:%s 使用续作模具:%s 在[%d]日排产，排产量:%d
     *
     * @param productionContext   排产上下文
     * @param logType             日志类型
     * @param continuePlan        续作计划
     * @param mouldCodeInfo       模具信息
     * @param startProductionDate 排产日
     * @param productionQty       排产量
     */
    public static void addContinueMouldProductionLog(ProductionContext productionContext, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo continuePlan, String mouldCodeInfo, Integer startProductionDate, Long productionQty) {
        String continueMouldDayProductionContent = String.format("===续作计划:%s 使用续作模具:%s 在[%d]日排产，排产量:%d", continuePlan.getMonthPlanId(), mouldCodeInfo, startProductionDate, productionQty);
        log.info(continueMouldDayProductionContent);
        MouldProductionLog continueMouldDayProductionLog = buildProductionLog(productionContext, continuePlan, logType, continueMouldDayProductionContent);
        saveProductionLog(productionContext, continueMouldDayProductionLog);
    }

    /**
     * 增加没有续作模具排产续作计划
     * ===续作计划没有续作模具
     *
     * @param productionContext 排产上下文
     * @param logType           日志类型
     * @param continuePlan      续作计划
     */
    public static void addNoContinueMouldProductionContinueProductPlan(ProductionContext productionContext, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo continuePlan) {
        String noContinueMouldContent = "===续作计划没有续作模具";
        log.info(noContinueMouldContent);
        MouldProductionLog noContinueMouldLog = buildProductionLog(productionContext, continuePlan, logType, noContinueMouldContent);
        saveProductionLog(productionContext, noContinueMouldLog);
    }

    /**
     * 增加续作模具没有可用模具排产续作计划日志
     *
     * @param productionContext 排产上下文
     * @param logType           日志类型
     * @param continuePlan      续作计划
     */
    public static void addNoEnableContinueMouldProductionContinueProductPlan(ProductionContext productionContext, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo continuePlan) {
        String noEnableMouldContent = "===续作计划的续作模具没有可用模具";
        log.info(noEnableMouldContent);
        MouldProductionLog noEnableMouldLog = buildProductionLog(productionContext, continuePlan, logType, noEnableMouldContent);
        saveProductionLog(productionContext, noEnableMouldLog);
    }

    /**
     * 增加续作计划使用续作模具开始排产前的流程日志
     * ===续作计划:%s 使用续作模具:%s 排产，需排产量:%d
     *
     * @param productionContext 排产上下文
     * @param continuePlan      续作计划
     * @param mouldInfo         使用模具信息
     */
    public static void addBeforeContinuePlanProductionByContinueMouldLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo continuePlan, String mouldInfo) {
        String continueMouldStartProductionContent = String.format("===续作计划:%s 使用续作模具:%s 排产，需排产量:%d", continuePlan.getMonthPlanId(), mouldInfo, continuePlan.getProductionQty());
        log.info(continueMouldStartProductionContent);
        MouldProductionLog continueMouldStartProductionLog = buildProductionLog(productionContext, continuePlan, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, continueMouldStartProductionContent);
        saveProductionLog(productionContext, continueMouldStartProductionLog);
    }

    /**
     * 增加续作计划使用续作模具排产后结果流程日志
     * ===续作计划:%s 使用续作模具:%s 排产，排产前需排产量 %d 排产后还需排产量:%d
     *
     * @param productionContext 排产上下文
     * @param continuePlan      续作计划
     * @param mouldInfo         续作模具信息
     * @param needProductionQty 还需排产量
     */
    public static void addResultContinuePlanProductionByContinueMouldLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo continuePlan, String mouldInfo, Long needProductionQty) {
        String continueMouldProductionResultContent = String.format("===续作计划:%s 使用续作模具:%s 排产，排产前需排产量 %d 排产后还需排产量:%d", continuePlan.getMonthPlanId(), mouldInfo, continuePlan.getProductionQty(), needProductionQty);
        log.info(continueMouldProductionResultContent);
        MouldProductionLog continueMouldProductionResultLog = buildProductionLog(productionContext, continuePlan, MouldProductionLogType.CONTINUE_MOULD_GENERAL_LOG, continueMouldProductionResultContent);
        saveProductionLog(productionContext, continueMouldProductionResultLog);
    }

    /**
     * 增加续作计划排产后日志
     *
     * @param productionContext 排产上下文
     * @param logType           日志类型
     * @param continuePlan      续作计划
     */
    public static void addAfterContinuePlanProductionLog(ProductionContext productionContext, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo continuePlan) {
        String continuePlanProductionContinueMouldContent = "续作计划使用续作模具排产完毕";
        log.info(continuePlanProductionContinueMouldContent);
        MouldProductionLog continuePlanProductionContinueMouldLog = buildProductionLog(productionContext, continuePlan, logType, continuePlanProductionContinueMouldContent);
        saveProductionLog(productionContext, continuePlanProductionContinueMouldLog);
    }

    /**
     * 增加前后不同业务类型排产，后一业务开始排产流程日志
     *
     * @param productionContext 排产上下文
     * @param businessTypeText  后一业务类型文本
     * @param logType           日志类型
     * @param previous          前一计划
     * @param currentPlan       当前排产计划
     */
    public static void addBeforeBusinessProductionLog(ProductionContext productionContext, String businessTypeText, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo previous, MonthPlanManufacturingRequirementVo currentPlan) {
        String productionLogContent = String.format("====%s排产：前一条排产计划ID：%s，排产计划ID：%s，计划量：%d, 单条硫化秒：%d", businessTypeText, previous.getMonthPlanId(), currentPlan.getMonthPlanId(), currentPlan.getProductionQty(), currentPlan.getCuringTime().longValue());
        log.info(productionLogContent);
        MouldProductionLog productionLog = buildProductionLog(productionContext, currentPlan, logType, productionLogContent);
        saveProductionLog(productionContext, productionLog);
    }

    /**
     * 增加业务类型排产后结果流程日志
     *
     * @param productionContext 排产上下文
     * @param businessTypeText  业务类型文本
     * @param logType           日志类型
     * @param currentPlan       当前排产计划
     */
    public static void addAfterBusinessProductionLog(ProductionContext productionContext, String businessTypeText, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo currentPlan) {
        String sameProductionEndLogContent = String.format("====%s排产结束：排产计划ID：%s，计划量：%d", businessTypeText, currentPlan.getMonthPlanId(), currentPlan.getProductionQty());
        log.info(sameProductionEndLogContent);
        MouldProductionLog sameProductionEndLog = buildProductionLog(productionContext, currentPlan, logType, sameProductionEndLogContent);
        saveProductionLog(productionContext, sameProductionEndLog);
    }

    /**
     * 增加共用生胎排产前日志记录
     *
     * @param productionContext    排产上下文
     * @param previous             前一计划
     * @param sameConstructionPlan 共用生胎后一计划
     */
    public static void addBeforeSameConstructionProductionLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo previous, MonthPlanManufacturingRequirementVo sameConstructionPlan) {
        String productionLogContent = String.format("====共用生胎排产：前一条排产计划ID：%s，排产计划ID：%s，计划量：%d, 单条硫化秒：%d", previous.getMonthPlanId(), sameConstructionPlan.getMonthPlanId(), sameConstructionPlan.getProductionQty(), sameConstructionPlan.getCuringTime().longValue());
        log.info(productionLogContent);
        MouldProductionLog productionLog = buildProductionLog(productionContext, sameConstructionPlan, MouldProductionLogType.SAME_CONSTRUCTION_LOG, productionLogContent);
        saveProductionLog(productionContext, productionLog);
    }

    /**
     * 跨组同规格优先排产前日志记录
     *
     * @param productionContext 排产上下文
     * @param previous          前一计划
     * @param sameProductPlan   跨组同规格后一计划
     */
    public static void addBeforeCrossGroupSameProductProductionLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo previous, MonthPlanManufacturingRequirementVo sameProductPlan) {
        //排产流程日志记录
        String productionLogContent = String.format("====跨组同规格排产：前一条排产计划ID：%s，排产计划ID：%s，计划量：%d, 单条硫化秒：%d", previous.getMonthPlanId(), sameProductPlan.getMonthPlanId(), sameProductPlan.getProductionQty(), sameProductPlan.getCuringTime().longValue());
        log.info(productionLogContent);
        MouldProductionLog productionLog = buildProductionLog(productionContext, sameProductPlan, MouldProductionLogType.SAME_PRODUCT_LOG, productionLogContent);
        saveProductionLog(productionContext, productionLog);
    }

    /**
     * 增加续作规格满月排产日志
     *
     * @param productionContext 排产上下文
     * @param logType           日志类型
     * @param productCode       物料规格
     */
    public static void addStartFullMonthProductionLog(ProductionContext productionContext, MouldProductionLogType logType, String productCode) {
        //排产流程日志记录
        String productionLogContent = String.format("====对计划使用满月排产模式排产，排产计划规格%s", productCode);
        log.info(productionLogContent);
        MouldProductionLog productionLog = buildProductionLog(productionContext, null, logType, productionLogContent);
        saveProductionLog(productionContext, productionLog);
    }

    /**
     * 增加使用模具对计划执行满月超产排产日志
     *
     * @param productionContext 排产上下文
     * @param logType           日志类型
     * @param continuePlan      计划
     * @param mouldInfo         模具信息
     */
    public static void addMouldFullMonthProductionLog(ProductionContext productionContext, MouldProductionLogType logType, MonthPlanManufacturingRequirementVo continuePlan, String mouldInfo) {
        String fullMonthProductionLogContent = String.format("====对计划%s执行满月续排超产排产模式，使用模具%s满月排产", continuePlan.getMonthPlanId(), mouldInfo);
        log.info(fullMonthProductionLogContent);
        MouldProductionLog sameProductionEndLog = buildProductionLog(productionContext, continuePlan, logType, fullMonthProductionLogContent);
        saveProductionLog(productionContext, sameProductionEndLog);
    }

    /**
     * 跨组同规格优先排产结果日志记录
     *
     * @param productionContext 排产上下文
     * @param sameProductPlan   跨组同规格排产计划
     */
    public static void addAfterCrossGroupSameProductProductionLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo sameProductPlan) {
        String sameProductionEndLogContent = String.format("====跨组同规格排产结束：排产计划ID：%s，计划量：%d", sameProductPlan.getMonthPlanId(), sameProductPlan.getProductionQty());
        log.info(sameProductionEndLogContent);
        MouldProductionLog sameProductionEndLog = buildProductionLog(productionContext, sameProductPlan, MouldProductionLogType.SAME_PRODUCT_LOG, sameProductionEndLogContent);
        saveProductionLog(productionContext, sameProductionEndLog);
    }

    /**
     * 增加双模排产不排单日志
     *
     * @param noProductionReason 双模排产不排单信息
     * @param productionContext  排产上下文
     * @param productionPlan     排产计划
     * @param logType            排产类型
     */
    public static void addDoubleMouldNoProductionSingle(String noProductionReason, ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MouldProductionLogType logType) {
        String doubleMouldEndLogContent = String.format("双模排产结果：%s", noProductionReason);
        log.info(doubleMouldEndLogContent);
        //排产流程日志记录
        MouldProductionLog doubleMouldEndLog = buildProductionLog(productionContext, productionPlan, logType, doubleMouldEndLogContent);
        saveProductionLog(productionContext, doubleMouldEndLog);
    }

    /**
     * 增加因规格已经超成型产能-不再排产的日志记录
     * 规格因S型已经超成型产能不能加模继续排产
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     */
    public static void addSkipProductionByExceedCapacity(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan) {
        String skipProductionContent = "规格因S型已经超成型产能不能加模继续排产";
        log.info(skipProductionContent);
        //排产流程日志记录
        MouldProductionLog skipProductionLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SKIP_PRODUCTION_PLAN_LOG, skipProductionContent);
        saveProductionLog(productionContext, skipProductionLog);
    }

    /**
     * 增加排产量与产能消耗不一致日志信息
     * %s计划规格%s排产量%s与产能预占量%s不一致
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param productionQty     排产量
     * @param preemptionQty     产能消耗量
     * @param minLimitQty       剩余产能量
     */
    public static void addProductionQtyDiffPreemptionQty(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Long productionQty, Long preemptionQty, Long minLimitQty) {
        String diffContent = String.format("分组key：%s %s计划规格%s排产量%s、产能预占量%s，当前产能剩余量%s", productionPlan.getSizeCapacityGroupKey(), productionPlan.getMonthPlanId(), productionPlan.getProductCode(), productionQty, preemptionQty, minLimitQty);
        log.info(diffContent);
        //排产流程日志记录
        MouldProductionLog diffPreemptionLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.PLAN_PREEMPTION_QTY_LOG, diffContent);
        saveProductionLog(productionContext, diffPreemptionLog);
    }

    /**
     * 增加计算使用硫化时间消耗日志
     *
     * @param productionContext   排产上下文
     * @param productionPlan      排产计划
     * @param usedCuringTime      使用硫化时间
     * @param changeSubSecond     换模时间
     * @param cleanMouldSubSecond 洗模时间
     * @param nextDaySubtractTime 跨天时间
     */
    public static void addUseCuringTimeInfo(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Long productionQty, BigDecimal usedCuringTime, BigDecimal changeSubSecond, BigDecimal cleanMouldSubSecond, BigDecimal nextDaySubtractTime) {
        String useCuringTimeContent = String.format("分组key：%s %s计划规格%s排产量%s、使用硫化时间(s)%s，换模时间(s)%s，洗模时间(s)%s，跨天时间(s)%s", productionPlan.getSizeCapacityGroupKey(), productionPlan.getMonthPlanId(), productionPlan.getProductCode(), productionQty, usedCuringTime, changeSubSecond, cleanMouldSubSecond, nextDaySubtractTime);
        log.info(useCuringTimeContent);
        //排产流程日志记录
        MouldProductionLog useCuringTimeContentLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.PLAN_PREEMPTION_QTY_LOG, useCuringTimeContent);
        saveProductionLog(productionContext, useCuringTimeContentLog);
    }

    /**
     * 增加方向排产超出成型产能记录
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param preemptionQty     消耗量
     * @param minLimitQty       剩余量
     */
    public static void addReverseProductionExceedCapacity(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Long preemptionQty, Long minLimitQty) {
        String reverseContent = String.format("分组key：%s %s计划规格%s反向排产出现超成型产能，消耗量%s、剩余量%s", productionPlan.getSizeCapacityGroupKey(), productionPlan.getMonthPlanId(), productionPlan.getProductCode(), preemptionQty, minLimitQty);
        log.info(reverseContent);
        //排产流程日志记录
        MouldProductionLog reverseContentLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.PLAN_PREEMPTION_QTY_LOG, reverseContent);
        saveProductionLog(productionContext, reverseContentLog);
    }

    /**
     * 增加排产量与产能消耗不一致日志信息
     * %s续作计划规格%s排产量%s、产能预占量%s
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param productionQty     排产量
     * @param preemptionQty     产能消耗量
     */
    public static void addProductionQtyContinuePreemptionQty(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Long productionQty, Long preemptionQty) {
        String continueContent = String.format("分组key：%s %s续作计划规格%s排产量%s、产能预占量%s", productionPlan.getSizeCapacityGroupKey(), productionPlan.getMonthPlanId(), productionPlan.getProductCode(), productionQty, preemptionQty);
        log.info(continueContent);
        //排产流程日志记录
        MouldProductionLog continuePreemptionLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.PLAN_PREEMPTION_QTY_LOG, continueContent);
        saveProductionLog(productionContext, continuePreemptionLog);
    }

    /**
     * 增加排产量与产能消耗量日志信息
     * %s计划拼模规格%s排产量%s、产能预占量%s
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param productionQty     排产量
     * @param preemptionQty     产能消耗量
     */
    public static void addProductionQtyAssemblingPreemptionQty(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Long productionQty, Long preemptionQty) {
        String assemblingContent = String.format("分组key：%s %s计划拼模规格%s排产量%s、产能预占量%s", productionPlan.getSizeCapacityGroupKey(), productionPlan.getMonthPlanId(), productionPlan.getProductCode(), productionQty, preemptionQty);
        log.info(assemblingContent);
        //排产流程日志记录
        MouldProductionLog assemblingPreemptionLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.PLAN_PREEMPTION_QTY_LOG, assemblingContent);
        saveProductionLog(productionContext, assemblingPreemptionLog);
    }

    /**
     * 增加超出当日剩余量排产-因当日排产过
     * "%s: 在%s日排产产能占用量%s
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param productionDate    排产日
     * @param productionQty     排产量
     */
    public static void addExceedCapacityByCurrentProductionQty(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Integer productionDate, Long productionQty) {
        String exceedQtyContent = String.format("分组key：%s 【%s】计划超出产能因在【%s】日已经排产，产能占用量【%s】", productionPlan.getSizeCapacityGroupKey(), productionPlan.getMonthPlanId(), productionDate, productionQty);
        log.info(exceedQtyContent);
        MouldProductionLog exceedQtyLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.PLAN_PREEMPTION_QTY_LOG, exceedQtyContent);
        saveProductionLog(productionContext, exceedQtyLog);
    }

    /**
     * 增加超出当日剩余量排产-因前日排产过
     * "%s: 在%s日排产产能占用量%s
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param productionDate    排产日
     * @param productionQty     排产量
     */
    public static void addExceedCapacityByBeforeProductionQty(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Integer productionDate, Long productionQty) {
        String exceedQtyContent = String.format("分组key：%s 【%s】计划超出产能因在【%s】日有排产，产能占用量【%s】", productionPlan.getSizeCapacityGroupKey(), productionPlan.getMonthPlanId(), productionDate, productionQty);
        log.info(exceedQtyContent);
        MouldProductionLog exceedQtyLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.PLAN_PREEMPTION_QTY_LOG, exceedQtyContent);
        saveProductionLog(productionContext, exceedQtyLog);
    }

    /**
     * 增加 计划产能占用量- 日志
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param productionDate    排产日
     * @param consumeQty        占用量
     */
    public static void addPreemptionConsumePlanQty(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Integer productionDate, Long consumeQty) {
        String planConsumeQtyContent = String.format("分组key：%s 【%s】计划，规格【%s】在【%s】日有排产，产能占用量【%s】", productionPlan.getSizeCapacityGroupKey(), productionPlan.getMonthPlanId(), productionPlan.getProductCode(), productionDate, consumeQty);
        log.info(planConsumeQtyContent);
        MouldProductionLog planConsumeQtyContentLog = buildProductionLog(productionContext, productionPlan, MouldProductionLogType.PLAN_PREEMPTION_QTY_LOG, planConsumeQtyContent);
        saveProductionLog(productionContext, planConsumeQtyContentLog);
    }

    /**
     * 增加日排产占用量日志
     * "%s: 在%s日排产产能占用量%s
     *
     * @param productionContext    排产上下文
     * @param productionDate       排产日
     * @param sizeCapacityGroupKey 排产分组key
     * @param productionQty        排产量
     */
    public static void addPreemptionConsumeQty(ProductionContext productionContext, Integer productionDate, String sizeCapacityGroupKey, Long productionQty) {
        String consumeQtyContent = String.format("【%s】: 在【%s】日排产产能占用量【%s】", sizeCapacityGroupKey, productionDate, productionQty);
        log.info(consumeQtyContent);
        MouldProductionLog consumeQtyLog = buildProductionLog(productionContext, null, MouldProductionLogType.PLAN_PREEMPTION_QTY_LOG, consumeQtyContent);
        saveProductionLog(productionContext, consumeQtyLog);
    }

    /**
     * 增加日排产占用量日志
     * "%s: 在%s日排产产能占用量%s
     *
     * @param productionContext    排产上下文
     * @param productionDate       排产日
     * @param sizeCapacityGroupKey 排产分组key
     * @param productionQty        排产量
     * @param currentQty           当前量
     * @param sumQty               总量
     */
    public static void addPreemptionConsumeQtyAndCurrent(ProductionContext productionContext, Integer productionDate, String sizeCapacityGroupKey, Long productionQty, Long currentQty, Long sumQty) {
        String consumeQtyAndCurrentContent = String.format("【%s】: 在【%s】日排产产能占用量【%s】，当前量【%s】，总量【%s】", sizeCapacityGroupKey, productionDate, productionQty, currentQty, sumQty);
        log.info(consumeQtyAndCurrentContent);
        MouldProductionLog consumeQtyAndCurrentContentLog = buildProductionLog(productionContext, null, MouldProductionLogType.PLAN_PREEMPTION_QTY_LOG, consumeQtyAndCurrentContent);
        saveProductionLog(productionContext, consumeQtyAndCurrentContentLog);
    }

    /**
     * 增加双模排产排产结果日志
     *
     * @param noProductionReason 双模排产-产能不足
     * @param productionContext  排产上下文
     * @param productionPlan     排产计划
     * @param logType            排产类型
     */
    public static void addDoubleMouldProductionResult(String noProductionReason, ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, MouldProductionLogType logType) {
        String doubleMouldEndLogContent = String.format("双模排产结果：%s", noProductionReason);
        log.info(doubleMouldEndLogContent);
        //排产流程日志记录
        MouldProductionLog doubleMouldEndLog = buildProductionLog(productionContext, productionPlan, logType, doubleMouldEndLogContent);
        saveProductionLog(productionContext, doubleMouldEndLog);
    }

    /**
     * 增加共用生胎排产结果日志记录
     *
     * @param productionContext    排产上下文
     * @param sameConstructionPlan 共用生胎排产计划
     */
    public static void addAfterSameConstructionProductionLog(ProductionContext productionContext, MonthPlanManufacturingRequirementVo sameConstructionPlan) {
        String sameConstructionLogContent = String.format("====共用生胎排产结束：排产计划ID：%s，计划量：%d", sameConstructionPlan.getMonthPlanId(), sameConstructionPlan.getProductionQty());
        log.info(sameConstructionLogContent);
        MouldProductionLog sameConstructionLog = buildProductionLog(productionContext, sameConstructionPlan, MouldProductionLogType.SAME_CONSTRUCTION_LOG, sameConstructionLogContent);
        saveProductionLog(productionContext, sameConstructionLog);
    }

    /**
     * [%d][有交期]计划%s模具排产完成
     * <p>
     * 增加计划排产完成日志
     *
     * @param productionContext  排产上下文
     * @param productionPlan     排产计划
     * @param isDeliveryDatePlan 是否为有交期计划
     * @param logType            日志类型
     * @param text               文本说明，一般为单或是多
     */
    public static void addPlanProductionFinishLog(ProductionContext productionContext, boolean isDeliveryDatePlan, MonthPlanManufacturingRequirementVo productionPlan, MouldProductionLogType logType, String text) {
        String planText = "";
        if (isDeliveryDatePlan) {
            planText = "有交期";
        }
        String endPlanProductionLogContent = String.format("[%d]%s计划%s模具排产完成。。。", productionPlan.getMonthPlanId(), planText, text);
        log.info(endPlanProductionLogContent);
        MouldProductionLog endPlanProductionLog = buildProductionLog(productionContext, productionPlan, logType, endPlanProductionLogContent);
        saveProductionLog(productionContext, endPlanProductionLog);
    }

    /**
     * 保存模具排程排产流程日志
     *
     * @param productionContext 排产上下文
     * @param productionLog     模具排产日志对象
     */
    private static void saveProductionLog(ProductionContext productionContext, MouldProductionLog productionLog) {
        if (null == productionLog) {
            return;
        }
        MouldProductionLogType logType = MouldProductionLogType.getInstance(productionLog.getLogType());
        String logContent = String.format("计划ID: %d 物料编码: %s -阶段：%s ： %s", productionLog.getMonthPlanId(), productionLog.getProductCode(), logType.getDesc(), productionLog.getLogContent());
        productionContext.getLogBuilder().append(logContent).append(System.lineSeparator()).append("===================").append(System.lineSeparator());
    }

    /**
     * 构建模具排程日志对象
     *
     * @param productionContext 排产上下文
     * @param monthPlan         排产计划
     * @param logType           日志类型
     * @param logContent        日志内容
     */
    public static MouldProductionLog buildProductionLog(ProductionContext productionContext, ProductionMonthPlanInit monthPlan, MouldProductionLogType logType, String logContent) {
        MouldProductionLog log = new MouldProductionLog();
        log.setWorkNo(productionContext.getOperationWorkNo());
        log.setFactoryCode(productionContext.getFactoryCode());
        log.setYear(productionContext.getYear());
        log.setMonth(productionContext.getMonth());
        log.setMonthPlanVersion(productionContext.getMonthPlanVersion());
        log.setProductionVersion(productionContext.getProductionVersion());
        log.setLogContent(logContent);
        log.setLogType(logType.getTypeValue());
        if (null != monthPlan) {
            log.setMonthPlanId(monthPlan.getMonthPlanId());
            log.setProductCode(monthPlan.getProductCode());
        }
        return log;
    }

    private ProductionLogUtils() {

    }
}
