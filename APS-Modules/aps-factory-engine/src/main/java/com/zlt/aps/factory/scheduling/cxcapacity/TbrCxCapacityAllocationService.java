package com.zlt.aps.factory.scheduling.cxcapacity;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.factory.basedataassemble.history.ProductionHistoryHandler;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.handler.CalculateStructureCxMachineNumber;
import com.zlt.aps.factory.handler.ContinueSkuCalculator;
import com.zlt.aps.factory.handler.GroupProductionConversionHandler;
import com.zlt.aps.factory.handler.MouldProductionResultHandler;
import com.zlt.aps.factory.logrecorder.*;
import com.zlt.aps.factory.scheduling.*;
import com.zlt.aps.factory.service.DpRequireDataService;
import com.zlt.aps.factory.service.MonthProductionDataService;
import com.zlt.aps.factory.service.ProductionMdmDataService;
import com.zlt.aps.factory.handler.InitNoProductionRecordHandler;
import com.zlt.aps.factory.utils.NoProductionPlanUtils;
import com.zlt.aps.factory.utils.ProductionCycleUtils;
import com.zlt.aps.monthplan.api.domain.entity.*;
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
 * 工厂TBR业务轮胎成型产能分配
 * 主要完成按结构进行成型产能分配
 * 1、按结构汇总净需求量，粗算结构所需成型机台数
 * 2、从上个月的月度排产计划，获取在产结构-即续作结构
 * 3、如果续作结构需求量减少，导致需要提前释放续作结构的成型机台数，则优先释放配比机台数多的
 * 4、续作结构排产完毕后，对续作结构中收尾的成型机台，进行反向查找下个结构(需成型机台剩余产能能满足结构净需求)
 * 5、对剩余还有需求量的结构，按结构优先级，挑选优先级最高的结构，匹配能分配的成型机台
 * 5.1、固定机台优先，是否零度结构 = 零度供料架
 * 5.2、成型机当前排产结构是否与挑选结构含有同规格(结构向下SKU只要有一个同规格则认为同规格)
 * 5.3、成型机当前排产结构是否与挑选结构含有同英寸(结构向下SKU只要有一个同英寸则认为同英寸)
 * 5.4、成型机当前排产结构是否与挑选结构的断面宽±10优先
 * 5.5、近1个月历史结构在期日期近的优先(最后一个排产日)
 * 5.6、近3个月历史结构在机次数多的优先(一个月算一次)
 *
 * @author ZLT
 * @date 20251209
 */
@Slf4j
@Service(value = "tbrWholeProductionService")
public class TbrCxCapacityAllocationService extends AbstractDataLoaderService {

    private final FormalProductionHandler formalProductionHandler;

    private final SimulateProductionHandler simulateProductionHandler;

    private final ClearProductionInfoHandler clearProductionInfoHandler;

    private final SpecialMaterialScheduleHandler cxSpecialMaterialScheduleHandler;

    private final CalculateStructureCxMachineNumber calculateStructureCxMachineNumber;

    private final ProductionCxMachineCalculationHandler productionCxMachineCalculationHandler;

    private final AdjustContinueSkuProductionQtyHandler adjustContinueSkuProductionQtyHandler;

    private final InitNoProductionRecordHandler initNoProductionRecordHandler;

    public TbrCxCapacityAllocationService(ProductionMdmDataService dataService,
                                          DpRequireDataService dpRequireDataService,
                                          MonthProductionDataService monthProductionDataService,
                                          FormalProductionHandler formalProductionHandler,
                                          ProductionHistoryHandler productionHistoryHandler,
                                          SimulateProductionHandler simulateProductionHandler,
                                          ClearProductionInfoHandler clearProductionInfoHandler,
                                          InitNoProductionRecordHandler initNoProductionRecordHandler,
                                          SpecialMaterialScheduleHandler cxSpecialMaterialScheduleHandler,
                                          CalculateStructureCxMachineNumber calculateStructureCxMachineNumber,
                                          ProductionCxMachineCalculationHandler productionCxMachineCalculationHandler,
                                          AdjustContinueSkuProductionQtyHandler adjustContinueSkuProductionQtyHandler) {
        super(dataService, dpRequireDataService, monthProductionDataService, productionHistoryHandler);
        this.formalProductionHandler = formalProductionHandler;
        this.simulateProductionHandler = simulateProductionHandler;
        this.clearProductionInfoHandler = clearProductionInfoHandler;
        this.initNoProductionRecordHandler = initNoProductionRecordHandler;
        this.cxSpecialMaterialScheduleHandler = cxSpecialMaterialScheduleHandler;
        this.calculateStructureCxMachineNumber = calculateStructureCxMachineNumber;
        this.productionCxMachineCalculationHandler = productionCxMachineCalculationHandler;
        this.adjustContinueSkuProductionQtyHandler = adjustContinueSkuProductionQtyHandler;
    }

    /**
     * 结构排产
     * 1、构建排产Tbr排产上下文(设置排产版本信息、构建排产周期信息)
     * 2、根据工厂、年份、月份、需求版本号获取排产需求(前面初始化部分已经处理-故而从初始化表中获取t_mp_proc_month_plan_init)
     * 3、构建排产前的基础配置数据获取
     * 3.1、工厂排产参数配置读取：t_mp_factory_param
     * 3.2、胎胚需要使用的特殊材料信息 t_mdm_material_consume_detail
     * 4、按结构分组，并根据结构+主花纹的最大模具数，控制结构的合理最大排产量，以此数据来估算使用的机台数
     * 5、对在机结构进行在产机台分配(需要根据模拟排产续作Sku部分来分配)
     * 5.1、续作Sku分为3步：续作Sku使用续作模具数排产高优先级部分、接着排续作Sku同规格同花纹部分的高优先级量，最后排同生胎、共模具部分的高优先级量
     * 6、在机结构在产机台分配完后，继续对在机结构的新增Sku模拟模具排产，同时确定是否需要提前收尾，最终确认收尾时间点
     * 7、对在产机台有收尾的机台，反向匹配分组计划(机台剩余产能能够覆盖需求量，并满足匹配条件)
     * 8、对新增结构和在机结构剩余量进行产能分配，比较结构的优先级，确认最高优先级结构，挑选机台
     * 8.1、确认机台与分组计划关系后，进行模拟模具排产，确定其准确的收尾时间
     * 9、每排完一次匹配，则进行机台反向选择分组计划
     * 9.1、每确定一组机台与分组计划关系后，进行模拟模具排产，确定其准确的收尾时间
     * 10、模拟完成后，得到最终结构排产结果(结构与机台的上机时间~收尾时间、机台结构间的衔接)
     * 11、根据最终结果排产结果，按结构维度重新开始正式模具排产
     * 11.1、优先在机机构排产
     * 11.1.1、在机结构续作Sku先排->续作Sku的同规格同花纹->续作Sku的同生胎同模具
     * 11.1.2、在机结构新增Sku排产
     * 11.2、新增结构排产
     *
     * @param context 排产上下文
     * @param userObj 用户数据
     */
    @Override
    public void run(Context context, Object userObj) {
        //0、创建排产上下文
        TbrProductionContext productionContext = (TbrProductionContext) buildProductionContext(context);
        //开始进行成型产能分配-结构排产
        log.info(TbrBeforeProductionGroupLogRecorder.addStartGroupLog(productionContext));
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
            this.initNoProductionRecordHandler.initNoProductionRecord(productionContext, singlePlan);
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
        // 结构特殊材料排产
//        cxSpecialMaterialScheduleHandler.specialMaterialSchedule(productionContext);
        //6、对续作结构进行在产成型机台分配(测算在产成型机台的收尾点以及可能月初释放的机台)-并记录在机结构的收尾点机台信息
        List<CxMachineAllocationPlanHelper> continueAllocationList = productionCxMachineCalculationHandler.allocationContinueAndProductionContinue(productionContext, estimateGroupCxAllocationMap, cxContinueInfoMap);
        KeyInformationLogRecorder.recorderContinueAllocationGroupInfoLog(productionContext, estimateGroupCxAllocationMap, cxContinueInfoMap, continueAllocationList);
        // 7、详设:
        //      （5）特别场景：在排产时，我们的原则是续作优先，若共用模具情况下，续作高优先级的已没有，
        //                   存在续作中优先级的，但有高优先级。这时，续作中优先级的要先排？
        //             		   处理方案：首先，需要算一下模具的产能，如果能把高优先级+续作的中优先级全部能包过来，那么就续作优先；
        //                   如果不能包过来，就需要把中优先级中途下机，下机的时间点是，剩余的模具产能，正好能把高优先级产完。
        adjustContinueSkuProductionQtyHandler.adjustContinueSkuProductionQty(estimateGroupCxAllocationMap, continueAllocationList, cxContinueInfoMap, productionContext);
        //8、进行模拟模具排产
        log.info(TbrSimulateProductionLogRecorder.addStartMouldProductionLog(productionContext));
        simulateProductionHandler.productionGroupPlan(productionContext, estimateGroupCxAllocationMap, continueAllocationList, cxContinueInfoMap);
        //9、保存结构成型排程结果
        List<MpStructureAllocation> allAllocationList = saveStructureInfo(productionContext);
        //10、第二轮排产
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
        Map<Long, Integer> sumProductionMap = saveMouldProductionInfo(productionContext);
        //设置不排原因
        formalProductionHandler.setNoProductionReasonAfterResult(productionContext, estimateGroupCxAllocationMap, sumProductionMap);
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
     * 9：根据成型信息，得到结构排产结果
     * 即结构转产信息
     *
     * @param productionContext
     */
    private List<MpStructureAllocation> saveStructureInfo(TbrProductionContext productionContext) {
        List<MpStructureAllocation> allAllocationList = GroupProductionConversionHandler.getFinalResult(productionContext);
        if (CollectionUtils.isEmpty(allAllocationList)) {
            return Collections.emptyList();
        }
        getMonthProductionDataService().saveGroupConversionResult(allAllocationList);
        return allAllocationList;
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
    private Map<Long, Integer> saveMouldProductionInfo(TbrProductionContext productionContext) {
        //模具排产明细日志
        List<FactoryMonthPlanMouldDayDetail> detailLogList = MouldProductionResultHandler.getMouldProductionResult(productionContext);
        if (CollectionUtils.isEmpty(detailLogList)) {
            return Collections.emptyMap();
        }
        getMonthProductionDataService().saveMouldProductionDetailLog(detailLogList);
        //构建未排信息
        Map<Long, Integer> sumProductionMap = calculateProductionResult(detailLogList);
        //构建汇总的排产结果
        List<FactoryMonthPlanMouldDayResult> dayResultList = MouldProductionResultHandler.getSummaryBySkuResult(detailLogList, productionContext);
        getMonthProductionDataService().saveMouldProductionResult(dayResultList);
        return sumProductionMap;
    }

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

}