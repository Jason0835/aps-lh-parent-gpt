package com.zlt.aps.factory.scheduling.mouldproduction;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.factory.basedataassemble.history.ProductionHistoryHandler;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.handler.*;
import com.zlt.aps.factory.logrecorder.KeyInformationLogRecorder;
import com.zlt.aps.factory.logrecorder.TbrBeforeProductionGroupLogRecorder;
import com.zlt.aps.factory.logrecorder.TbrMouldFormalProductionLogRecorder;
import com.zlt.aps.factory.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.factory.scheduling.AbstractDataLoaderService;
import com.zlt.aps.factory.scheduling.BaseDataContainer;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.scheduling.cxcapacity.AdjustContinueSkuProductionQtyHandler;
import com.zlt.aps.factory.scheduling.cxcapacity.ClearProductionInfoHandler;
import com.zlt.aps.factory.scheduling.cxcapacity.FormalProductionHandler;
import com.zlt.aps.factory.scheduling.cxcapacity.ProductionCxMachineCalculationHandler;
import com.zlt.aps.factory.service.DpRequireDataService;
import com.zlt.aps.factory.service.MonthProductionDataService;
import com.zlt.aps.factory.service.ProductionMdmDataService;
import com.zlt.aps.factory.utils.NoProductionPlanUtils;
import com.zlt.aps.factory.utils.ProductionCycleUtils;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.enums.ProductionProcessStage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工厂TBR业务轮胎结构排产业务
 * 主要根据结构产能分配进行模具排产
 *
 * @author
 */
@Slf4j
@Service(value = "tbrMouldProductionService")
public class TbrMouldProductionService extends AbstractDataLoaderService {

    private final FormalProductionHandler formalProductionHandler;

    private final ClearProductionInfoHandler clearProductionInfoHandler;

    private final InitNoProductionRecordHandler initNoProductionRecordHandler;

    private final DayProductionStatisticsHandler dayProductionStatisticsHandler;

    private final CalculateStructureCxMachineNumber calculateStructureCxMachineNumber;

    private final ProductionCxMachineCalculationHandler productionCxMachineCalculationHandler;

    private final AdjustContinueSkuProductionQtyHandler adjustContinueSkuProductionQtyHandler;

    public TbrMouldProductionService(ProductionMdmDataService dataService,
                                     DpRequireDataService dpRequireDataService,
                                     ProductionHistoryHandler productionHistoryHandler,
                                     FormalProductionHandler formalProductionHandler,
                                     MonthProductionDataService monthProductionDataService,
                                     ClearProductionInfoHandler clearProductionInfoHandler,
                                     InitNoProductionRecordHandler initNoProductionRecordHandler,
                                     DayProductionStatisticsHandler dayProductionStatisticsHandler,
                                     CalculateStructureCxMachineNumber calculateStructureCxMachineNumber,
                                     ProductionCxMachineCalculationHandler productionCxMachineCalculationHandler,
                                     AdjustContinueSkuProductionQtyHandler adjustContinueSkuProductionQtyHandler) {
        super(dataService, dpRequireDataService, monthProductionDataService, productionHistoryHandler);
        this.formalProductionHandler = formalProductionHandler;
        this.clearProductionInfoHandler = clearProductionInfoHandler;
        this.initNoProductionRecordHandler = initNoProductionRecordHandler;
        this.dayProductionStatisticsHandler = dayProductionStatisticsHandler;
        this.calculateStructureCxMachineNumber = calculateStructureCxMachineNumber;
        this.productionCxMachineCalculationHandler = productionCxMachineCalculationHandler;
        this.adjustContinueSkuProductionQtyHandler = adjustContinueSkuProductionQtyHandler;
    }

    @Override
    public void run(Context context, Object userObj) {
        if (null == context.getInsertNewProductionVersion()) {
            context.setInsertNewProductionVersion(Boolean.FALSE);
        }
        if (null == context.getProductionProcessStage()) {
            context.setProductionProcessStage(ProductionProcessStage.STAGE_MOULDING);
        }
        //0、创建排产上下文
        TbrProductionContext productionContext = (TbrProductionContext) buildProductionContext(context);
        deleteOldData(productionContext);
        //1、获取排产计划信息
        List<MonthPlanProductionRequirePlanVo> requirePlanList = getMonthProductionDataService().getFactoryMonthPlanManufacturing(productionContext);
        log.info(TbrBeforeProductionGroupLogRecorder.addGetProductionVersionDataLog(productionContext));
        if (CollectionUtils.isEmpty(requirePlanList)) {
            throw new BusinessException(I18nUtil.getMessage("alg.data.production.noRequirePlan"));
        }
        //设置初始的排产量数据信息
        requirePlanList.forEach(singlePlan -> {
            log.info(TbrBeforeProductionGroupLogRecorder.addSetInitPlanInfoLog(context, singlePlan));
            singlePlan.initProductionDataInfo();
            initNoProductionRecordHandler.initNoProductionRecord(productionContext, singlePlan);
        });
        //2、初始排产需要的基础数据，成型、模具关系、成型硫化配比、计划初始库销比
        log.info(TbrBeforeProductionGroupLogRecorder.addStartBeforeProductionDataLog(productionContext));
        initProductionBaseData(productionContext, requirePlanList);
        saveMouldUsedLog(productionContext);
        //3、按结构分组，汇总结构净需求量，粗算需要的机台数 记录日志-粗算成型机台数，并赋值结构指定的机台集合
        log.info(TbrProductionGroupLogRecorder.addStartGroupCalculateCapacityLog(productionContext));
        Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap = calculateStructureCxMachineNumber.calculateStructureCxMachineNumber(productionContext, requirePlanList);
        setGroupFixedCxMachineInfo(productionContext, estimateGroupCxAllocationMap);
        productionContext.setGroupProductionInfo(estimateGroupCxAllocationMap);
        /**
         * 5、构建续作信息
         * 5.1、获取上个月度的月度定稿排产计划，得到在产Sku
         * 5.2、获取上个月度的结构排产信息，得到在产结构在产机台信息
         * 并构建续作Sku及其对应结构及结构在产成型机、在产SKU和SKU使用模具数
         */
        Map<String, CxContinueInfoHelper> cxContinueInfoMap = getContinueInfo(productionContext);
        //汇总续作Sku信息
        statisticsGroupContinueInfo(productionContext, estimateGroupCxAllocationMap, cxContinueInfoMap);
        KeyInformationLogRecorder.recorderInitGroupInfoLog(productionContext, estimateGroupCxAllocationMap, cxContinueInfoMap);
        //6、对续作结构进行在产成型机台分配(测算在产成型机台的收尾点以及可能月初释放的机台)-并记录在机结构的收尾点机台信息
        List<CxMachineAllocationPlanHelper> continueAllocationList = productionCxMachineCalculationHandler.allocationContinueAndProductionContinue(productionContext, estimateGroupCxAllocationMap, cxContinueInfoMap);
        KeyInformationLogRecorder.recorderContinueAllocationGroupInfoLog(productionContext, estimateGroupCxAllocationMap, cxContinueInfoMap, continueAllocationList);
        // 7、详设:
        //      （5）特别场景：在排产时，我们的原则是续作优先，若共用模具情况下，续作高优先级的已没有，
        //                   存在续作中优先级的，但有高优先级。这时，续作中优先级的要先排？
        //             		   处理方案：首先，需要算一下模具的产能，如果能把高优先级+续作的中优先级全部能包过来，那么就续作优先；
        //                   如果不能包过来，就需要把中优先级中途下机，下机的时间点是，剩余的模具产能，正好能把高优先级产完。
        adjustContinueSkuProductionQtyHandler.adjustContinueSkuProductionQty(estimateGroupCxAllocationMap, continueAllocationList, cxContinueInfoMap, productionContext);
        //获取结构排产信息
        List<MpStructureAllocation> allAllocationList = getMonthProductionDataService().getStructureAllocationInfoByProductionVersion(productionContext);
        log.info(TbrMouldFormalProductionLogRecorder.addStartMouldFormalLog(productionContext));
        //清除模拟排产信息
        clearProductionInfoHandler.clearProductionData(productionContext);
        //重新构建分组计划的硫化组限制信息
        resetBeforeFormalProduction(productionContext, estimateGroupCxAllocationMap, allAllocationList);
        log.info(TbrMouldFormalProductionLogRecorder.addResetDataFinishLog(productionContext));
        formalProductionHandler.productionContinueGroup(productionContext, estimateGroupCxAllocationMap, cxContinueInfoMap);
        //11、最后搭配排产 TODO 报错，先注释掉
//        MatchingProductionHandler.matchingProduction(productionContext, estimateGroupCxAllocationMap, structureLhRatioList);
        //12、保存模具排产结果
        Map<Long, Integer> sumProductionMap = saveMouldProductionInfo(productionContext, estimateGroupCxAllocationMap);
        //保存未排计划明细
        saveNoProductionPlanResult(productionContext, sumProductionMap);
    }

    /**
     * 0：构建业务排产上下文
     *
     * @param context
     * @return
     */
    @Override
    protected Context buildProductionContext(Context context) {
        //全钢业务
        if (ProductTypeEnum.WHOLE_STEEL == context.getProductType()) {
            return buildTbrProductionContext(context);
        }
        //主要为-半钢业务
        return buildDefaultProductionContext(context);
    }

    /**
     * 设置生产版本号，如果已经有生产版本号，则不进行设置
     * 否则根据当前时间戳及版本号前缀设置
     * 已有生产版本号，则根据生产版本号删除旧有数据
     *
     * @param productionContext
     */
    private void deleteOldData(TbrProductionContext productionContext) {
        String productionVersion = productionContext.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            throw new BusinessException(I18nUtil.getMessage("alg.data.alter.message.productionVersionNoEmpty"));
        }
        //删除版本已有数据-初始化、分组产能排产不删除
        getMonthProductionDataService().deletedMouldProductionData(productionContext);
    }

    /**
     * 2.2：保存模具的历史
     *
     * @param productionContext
     */
    private void saveMouldUsedLog(TbrProductionContext productionContext) {
        Map<String, ProductionMouldInfoVo> allMouldInfoMap = productionContext.getBaseDataContainer().getAllMouldInfoMap();
        if (CollectionUtils.isEmpty(allMouldInfoMap)) {
            return;
        }
        List<MpMouldUsedStatusLog> usedLogList = new ArrayList<>();
        allMouldInfoMap.forEach((mouldCode, detailInfo) -> {
            MpMouldUsedStatusLog usedLog = new MpMouldUsedStatusLog();
            usedLogList.add(usedLog);
            usedLog.setMouldCode(mouldCode);
            usedLog.setMouldStatus(detailInfo.getMouldStatus());
            usedLog.setFactoryCode(productionContext.getFactoryCode());
            usedLog.setYear(productionContext.getYear());
            usedLog.setMonth(productionContext.getMonth());
            usedLog.setMonthPlanVersion(productionContext.getMonthPlanVersion());
            usedLog.setProductionVersion(productionContext.getProductionVersion());
            usedLog.setPlanType(productionContext.getPlanType());
            usedLog.setOwerFactoryCode(productionContext.getFactoryCode());
            usedLog.setRelationType(detailInfo.getRelationType().getRelationType());
            if (!YesOrNoEnum.YES.getCode().equals(detailInfo.getMouldStatus())) {
                usedLog.setUsedDays(BigDecimal.ZERO.intValue());
                return;
            }
            if (CollectionUtils.isEmpty(detailInfo.getProductionDaySet())) {
                usedLog.setUsedDays(BigDecimal.ZERO.intValue());
                return;
            }
            usedLog.setUsedDays(detailInfo.getProductionDaySet().size());
        });
        if (CollectionUtils.isEmpty(usedLogList)) {
            return;
        }
        getMonthProductionDataService().saveMouldUsedLog(usedLogList);
    }

    /**
     * 3：设置结构的指定机台信息
     *
     * @param productionContext 排产上下文
     * @param allGroupMap       所有分组信息
     */
    private void setGroupFixedCxMachineInfo(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupMap) {
        if (CollectionUtils.isEmpty(allGroupMap)) {
            return;
        }
        Map<String, CxMachineBaseInfoVo> allCxMachineInfoMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineInfoMap)) {
            return;
        }
        List<CxMachineBaseInfoVo> allCxMachineInfo = allCxMachineInfoMap.values().stream().collect(Collectors.toList());
        allGroupMap.forEach((structureName, groupInfo) -> {
            List<CxMachineBaseInfoVo> hasFixedList = allCxMachineInfo.stream().filter(singleMachineInfo -> singleMachineInfo.hasFixedMachine(groupInfo)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(hasFixedList)) {
                return;
            }
            groupInfo.setFixedCxMachineSet(hasFixedList.stream().map(CxMachineBaseInfoVo::getCxMachineCode).collect(Collectors.toSet()));
        });
    }

    /**
     * 5.1：获取续作排产信息
     * 续作的分组信息(结构)，对应的成型产能机台和续作的SKU，使用模具-硫化机台数
     * key = structureName(TBR)
     * CxContinueInfoHelper.continueSkuMouldNumberMap = { key = materialDesc : value = 胎胚、硫化机台数(模具数)等}
     *
     * @param context 排产上下文
     * @return
     */
    private Map<String, CxContinueInfoHelper> getContinueInfo(Context context) {
        //获取前一个月的排产版本信息
        String factoryCode = context.getFactoryCode();
        LocalDate previousMonth = context.getPreviousMonth();
        Integer year = previousMonth.getYear();
        Integer month = previousMonth.getMonthValue();
        MpFactoryProductionVersion previousVersion = getMonthProductionDataService().getFinalVersion(factoryCode, year, month);
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderPreviousMonthLog(context, previousMonth, previousVersion));
        if (null == previousVersion) {
            return Collections.emptyMap();
        }
        //根据排产版本信息，确认最后一天的排产SKU信息(包含结构、SKU、使用模具数)
        Context previousContext = new Context();
        previousContext.setFactoryCode(factoryCode);
        previousContext.setYear(year);
        previousContext.setMonth(month);
        previousContext.setProductionStartDate(previousVersion.getProductionStartDate());
        previousContext.setProductionEndDate(previousVersion.getProductionEndDate());
        //获取上个排产周期的工作日历
        List<ProductionDayInfoVo> previousProductionDayInfo = getDataService().getProductCalendar(previousContext);
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderPreviousMonthProductionCalendarLog(context, previousProductionDayInfo, previousMonth));
        //确认最后排产日
        Integer lastDay = ProductionCycleUtils.getLastProductionDay(previousVersion, previousProductionDayInfo);
        if (lastDay <= BigDecimal.ZERO.intValue()) {
            return Collections.emptyMap();
        }
        //获取上个排产周期最后排产日的排产信息
        List<ContinueProductInfo> continueProductionInfoList = getMonthProductionDataService().getContinueProductionInfo(factoryCode, year, month, lastDay);
        log.info(TbrBeforeProductionGroupLogRecorder.addReadContinueSkuDataLog(context, continueProductionInfoList));
        //获取续作结构--结构转产表
        Map<String, Set<String>> continueGroupInfo = getContinueGroupInfo(context, factoryCode, year, month, lastDay);
        //构建续作分组信息(TBR为结构，PCR为英寸)
        BaseDataContainer baseDataContainer = ((TbrProductionContext) context).getBaseDataContainer();
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = baseDataContainer.getCxMachineBaseInfo();
        setContinueGroupByProduct(context, continueProductionInfoList, continueGroupInfo);
        //设置对应的最新成型硫化配比信息
        List<MonthPlanStructureLhRatioVo> structureLhRatioList = baseDataContainer.getStructureLhRatioList();
        Map<String, CxContinueInfoHelper> initMap = CxContinueInfoHelper.createGroupInfo(continueProductionInfoList, cxMachineBaseInfo, structureLhRatioList);
        //删除无在产机台的在机结构-脏数据
        initMap.entrySet().removeIf(entry -> CollectionUtils.isEmpty(entry.getValue().getCxMachineCodeSet()));
        return initMap;
    }

    /**
     * 5.2：汇总续作信息
     * 在机结构-续作Sku有排产量的胎胚和使用模具数
     * 机构计划-在产成型机信息初始化
     *
     * @param allGroupPlanMap      分组计划信息
     * @param allCxContinueInfoMap 续作分组信息
     */
    private void statisticsGroupContinueInfo(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, Map<String, CxContinueInfoHelper> allCxContinueInfoMap) {
        if (CollectionUtils.isEmpty(allGroupPlanMap) || CollectionUtils.isEmpty(allCxContinueInfoMap)) {
            return;
        }
        allGroupPlanMap.forEach((structureName, groupPlanInfo) -> {
            CxContinueInfoHelper cxContinueInfoHelper = allCxContinueInfoMap.get(structureName);
            if (null == cxContinueInfoHelper) {
                log.info(TbrProductionGroupLogRecorder.addGroupNoContinueGroupLog(context, structureName));
                return;
            }
            log.info(TbrProductionGroupLogRecorder.addOnLineGroupSetUpDataLog(context, structureName));
            ContinueSkuCalculator.setContinueSkuPlanDemandQty(context, groupPlanInfo, cxContinueInfoHelper);
            ContinueSkuCalculator.initContinueCxMachineLimit(context, groupPlanInfo, cxContinueInfoHelper);
        });
    }

    /**
     * 10：在正式排产前重新构建分组限制信息
     *
     * @param context           排产上下文
     * @param allGroupPlanInfo  所有分组计划对象
     * @param allAllocationList 分组转产配置
     */
    private void resetBeforeFormalProduction(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, List<MpStructureAllocation> allAllocationList) {
        //根据分组转产配置，重新构建分组的限制信息
        allGroupPlanInfo.forEach((groupName, groupProductionInfo) -> {
            List<MpStructureAllocation> groupAllocationList;
            if (CollectionUtils.isEmpty(allAllocationList)) {
                groupAllocationList = new ArrayList<>();
            } else {
                groupAllocationList = allAllocationList.stream().filter(singleAllocation -> groupName.equals(singleAllocation.getStructureName())).collect(Collectors.toList());
            }
            //重新设置分配的机台
            Set<String> allocationSet = groupAllocationList.stream().map(MpStructureAllocation::getCxMachineCode).collect(Collectors.toSet());
            groupProductionInfo.setAllocationCxMachineCodeSet(allocationSet);
            groupProductionInfo.buildDayProductionLimitInfoByStructureAllocation(context, groupAllocationList);
        });
    }

    /**
     * 12：根据模具信息，保存模具排产结果
     *
     * @param productionContext
     */
    private Map<Long, Integer> saveMouldProductionInfo(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupPlanMap) {
        //模具排产明细日志
        List<FactoryMonthPlanMouldDayDetail> detailLogList = MouldProductionResultHandler.getMouldProductionResult(productionContext);
        if (CollectionUtils.isEmpty(detailLogList)) {
            return Collections.emptyMap();
        }
        getMonthProductionDataService().saveMouldProductionDetailLog(detailLogList);
        //构建已排产计划及对应排产量
        Map<Long, Integer> sumProductionMap = calculateProductionResult(detailLogList);
        //设置不排原因
        formalProductionHandler.setNoProductionReasonAfterResult(productionContext, allGroupPlanMap, sumProductionMap);
        //构建汇总的排产结果
        List<FactoryMonthPlanMouldDayResult> dayResultList = MouldProductionResultHandler.getSummaryBySkuResult(detailLogList, productionContext);
        getMonthProductionDataService().saveMouldProductionResult(dayResultList);
        //日排产统计信息
        List<MpMonthPlanStatistics> productionStatisticsList = dayProductionStatisticsHandler.buildDayProductionStatisticsResult(productionContext);
        getMonthProductionDataService().saveProductionStatisticsResult(productionStatisticsList);
        return sumProductionMap;
    }

    /**
     * 保存未排数据
     *
     * @param productionContext 排产上下文
     * @param sumProductionMap  已排产计划及排产量
     */
    private void saveNoProductionPlanResult(TbrProductionContext productionContext, Map<Long, Integer> sumProductionMap) {
        Map<Long, MonthPlanProductionRequirePlanVo> productionPlanMap = productionContext.getAllProductionPlan();
        if (CollectionUtils.isEmpty(productionPlanMap)) {
            return;
        }
        List<MonthPlanNoProductionPlan> noProductionPlanList = NoProductionPlanUtils.buildNoProductionPlanList(productionPlanMap, productionContext.getNoProductionRecordMap(), sumProductionMap);
        if (CollectionUtils.isEmpty(noProductionPlanList)) {
            return;
        }
        getMonthProductionDataService().saveNoProductionPlan(noProductionPlanList);
    }

    /**
     * 0.1：构建全钢排产上下文
     * 设置排产版本号：为空时生产排产版本号
     * 设置操作批次号
     * 设置日志记录器实例
     * 设置排产周期信息
     *
     * @param context
     * @return
     */
    private TbrProductionContext buildTbrProductionContext(Context context) {
        TbrProductionContext productionContext = new TbrProductionContext();
        BeanUtils.copyProperties(context, productionContext);
        //基础数据容器存储
        productionContext.setBaseDataContainer(new BaseDataContainer());
        context.setProductionVersion(productionContext.createNewProductionVersion());
        context.setOperationWorkNo(productionContext.createNewOperationWorkNo());
        if (null == context.getLogBuilder()) {
            StringBuilder logBuilder = new StringBuilder();
            context.setLogBuilder(logBuilder);
            productionContext.setLogBuilder(logBuilder);
        }
        setProductionCycleInfo(productionContext);
        return productionContext;
    }

    /**
     * 0.2：构建默认的排产上下文
     * 主要为半钢业务
     * 设置排产版本号：为空时生产排产版本号
     * 设置操作批次号
     * 设置日志记录器实例
     * 设置排产周期信息
     *
     * @param context
     * @return
     */
    private ProductionContext buildDefaultProductionContext(Context context) {
        ProductionContext productionContext = new ProductionContext();
        BeanUtils.copyProperties(context, productionContext);
        context.setProductionVersion(productionContext.createNewProductionVersion());
        context.setOperationWorkNo(productionContext.createNewOperationWorkNo());
        if (null == context.getLogBuilder()) {
            StringBuilder logBuilder = new StringBuilder();
            context.setLogBuilder(logBuilder);
            productionContext.setLogBuilder(logBuilder);
        }
        setProductionCycleInfo(productionContext);
        return productionContext;
    }

    /**
     * 0.1~2.1：设置排产周期信息等信息
     * 根据排产版本号，得到排产版本包含排产周期，
     *
     * @param context
     */
    private void setProductionCycleInfo(Context context) {
        MpFactoryProductionVersion productionVersion = getMonthProductionDataService().getFactoryMonthPlanVersion(context);
        if (null == productionVersion) {
            return;
        }
        context.setPlanType(productionVersion.getPlanType());
        Date productionStartDate = productionVersion.getProductionStartDate();
        context.setProductionStartDate(productionStartDate);
        context.setStartDay(com.zlt.aps.factory.utils.DateUtils.getDaysByMonth(productionStartDate));
        context.setProductionEndDate(productionVersion.getProductionEndDate());
    }

    /**
     * 5.1.1：获取工厂年份-月份的最后一天排产的分组信息
     * TBR-结构
     * PCR-英寸、寸别、寸口
     *
     * @param context     排产上下文
     * @param factoryCode 工厂
     * @param year        年份
     * @param month       月份
     * @param lastDay     最后一天
     * @return
     */
    private Map<String, Set<String>> getContinueGroupInfo(Context context, String factoryCode, Integer year, Integer month, Integer lastDay) {
        List<ContinueGroupInfo> continueGroupInfoList = getMonthProductionDataService().getContinueGroupInfo(factoryCode, year, month, lastDay);
        log.info(TbrBeforeProductionGroupLogRecorder.addReadContinueGroupDataLog(context, continueGroupInfoList));
        if (CollectionUtils.isEmpty(continueGroupInfoList)) {
            return Collections.emptyMap();
        }
        Map<String, List<ContinueGroupInfo>> continueGroupInfoMap = continueGroupInfoList.stream().collect(Collectors.groupingBy(ContinueGroupInfo::getGroupName));
        Map<String, Set<String>> continueGroupInfo = new HashMap<>();
        continueGroupInfoMap.forEach((groupName, continueCxMachineInfoList) -> {
            if (CollectionUtils.isEmpty(continueCxMachineInfoList)) {
                return;
            }
            Set<String> continueCxMachineSet = continueCxMachineInfoList.stream().map(ContinueGroupInfo::getCxMachineCode).collect(Collectors.toSet());
            continueGroupInfo.put(groupName, continueCxMachineSet);
        });
        return continueGroupInfo;
    }

    /**
     * 5.1.2：对续作的Sku设置在产机台信息
     * 按分组名匹配
     * TRB为结构
     * PCR为英寸
     *
     * @param continueSkuInfo   续作的Sku规格
     * @param continueGroupInfo 续作的分组信息-含机台
     */
    private void setContinueGroupByProduct(Context context, List<ContinueProductInfo> continueSkuInfo, Map<String, Set<String>> continueGroupInfo) {
        if (CollectionUtils.isEmpty(continueSkuInfo) || CollectionUtils.isEmpty(continueGroupInfo)) {
            return;
        }
        continueSkuInfo.forEach(continueSku -> {
            String groupName = continueSku.getGroupName();
            if (StringUtils.isBlank(groupName)) {
                return;
            }
            Set<String> onLineMachineSet = continueGroupInfo.get(groupName);
            log.info(TbrBeforeProductionGroupLogRecorder.addContinueGroupNoOnLineMachineLog(context, groupName, continueSku.getMaterialDesc(), onLineMachineSet));
            continueSku.setContinueCxMachineCodeSet(onLineMachineSet);
        });
    }

    /**
     * 汇总计划的排产量信息
     *
     * @param detailList 排产结果信息
     * @return
     */
    private Map<Long, Integer> calculateProductionResult(List<FactoryMonthPlanMouldDayDetail> detailList) {
        Map<Long, Integer> sumMonthPlanMap = new HashMap<>();
        detailList.forEach(productionDetail -> {
            Long monthPlanId = productionDetail.getMonthPlanId();
            Integer productionQty = productionDetail.getTotalQty();
            if (null == productionQty) {
                productionQty = BigDecimal.ZERO.intValue();
            }
            Integer plannedProductionQty = sumMonthPlanMap.get(monthPlanId);
            if (null == plannedProductionQty) {
                plannedProductionQty = BigDecimal.ZERO.intValue();
            }
            sumMonthPlanMap.put(monthPlanId, plannedProductionQty + productionQty);
        });
        return sumMonthPlanMap;
    }
}
