package com.zlt.aps.factory.scheduling;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.ProductionGroupTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.factory.basedataassemble.history.CxMachineProductionHistoryInfo;
import com.zlt.aps.factory.basedataassemble.history.GroupPlanProductionHistoryInfo;
import com.zlt.aps.factory.basedataassemble.history.ProductionHistoryHandler;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.daylimit.*;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.DayVulcanizationModeEnum;
import com.zlt.aps.factory.logrecorder.TbrBeforeProductionGroupLogRecorder;
import com.zlt.aps.factory.logrecorder.TbrProductionInitLogRecorder;
import com.zlt.aps.factory.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import com.zlt.aps.factory.scheduling.init.ProductionInitParamConfiguration;
import com.zlt.aps.factory.service.DpRequireDataService;
import com.zlt.aps.factory.service.MonthProductionDataService;
import com.zlt.aps.factory.service.ProductionMdmDataService;
import com.zlt.aps.factory.utils.MouldRelationDeduplicator;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.enums.ProductionProcessStage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 抽象的排产业务类
 * 主要实现一些公用的业务处理
 *
 * @author ZLT
 * @date 20250220
 */
@Slf4j
public abstract class AbstractProductionBusinessService implements IProductionBusinessService {
    /**
     * 主数据数据提供接口
     */
    private final ProductionMdmDataService dataService;
    /**
     * 需求计划服务数据提供接口
     */
    private final DpRequireDataService dpRequireDataService;
    /**
     * 月度排产计划服务数据提供接口
     */
    private final MonthProductionDataService monthProductionDataService;

    public AbstractProductionBusinessService(ProductionMdmDataService dataService,
                                             DpRequireDataService dpRequireDataService,
                                             MonthProductionDataService monthProductionDataService) {
        this.dataService = dataService;
        this.dpRequireDataService = dpRequireDataService;
        this.monthProductionDataService = monthProductionDataService;
    }

    // 定义 Handler 成员变量
    protected ProductionHistoryHandler productionHistoryHandler;

    // 提供 Setter 方法，方便子类注入
    public void setProductionHistoryHandler(ProductionHistoryHandler productionHistoryHandler) {
        this.productionHistoryHandler = productionHistoryHandler;
    }

    /**
     * 构建业务排产上下文
     *
     * @param context
     * @return
     */
    protected Context buildProductionContext(Context context) {
        //全钢业务
        if (ProductTypeEnum.WHOLE_STEEL == context.getProductType()) {
            return buildTbrProductionContext(context);
        }
        //主要为-半钢业务
        return buildDefaultProductionContext(context);
    }

    /**
     * 保存日志
     *
     * @param context
     */
    protected void saveProductionProcessLog(Context context, ProductionProcessStage processStage) {
        StringBuilder logBuilder = context.getLogBuilder();
        String logContent = logBuilder.toString();
        if (StringUtils.isBlank(logContent)) {
            return;
        }
        logContent = String.format("%s流程日志:%s%s", processStage.getDesc(), System.lineSeparator(), logContent);
        MouldProductionLog log = new MouldProductionLog();
        log.setFactoryCode(context.getFactoryCode());
        log.setYear(context.getYear());
        log.setMonth(context.getMonth());
        log.setMonthPlanVersion(context.getMonthPlanVersion());
        log.setProductionVersion(context.getProductionVersion());
        log.setPlanType(context.getPlanType());
        log.setWorkNo(context.getOperationWorkNo());
        log.setLogContent(logContent);
        monthProductionDataService.saveMouldProductionLog(log);
    }

    /**
     * 构建全钢排产上下文
     *
     * @param context
     * @return
     */
    private TbrProductionContext buildTbrProductionContext(Context context) {
        TbrProductionContext productionContext = new TbrProductionContext();
        BeanUtils.copyProperties(context, productionContext);
        context.setProductionVersion(productionContext.createNewProductionVersion());
        context.setOperationWorkNo(productionContext.createNewOperationWorkNo());
        StringBuilder logBuilder = new StringBuilder();
        context.setLogBuilder(logBuilder);
        productionContext.setLogBuilder(logBuilder);
        setProductionCycleInfo(productionContext);
        return productionContext;
    }

    /**
     * 构建默认的排产上下文
     * 主要为半钢业务
     *
     * @param context
     * @return
     */
    private ProductionContext buildDefaultProductionContext(Context context) {
        ProductionContext productionContext = new ProductionContext();
        BeanUtils.copyProperties(context, productionContext);
        context.setProductionVersion(productionContext.createNewProductionVersion());
        context.setOperationWorkNo(productionContext.createNewOperationWorkNo());
        StringBuilder logBuilder = new StringBuilder();
        context.setLogBuilder(logBuilder);
        productionContext.setLogBuilder(logBuilder);
        setProductionCycleInfo(productionContext);
        return productionContext;
    }

    /**
     * 设置排产周期信息等信息
     *
     * @param context
     */
    private void setProductionCycleInfo(Context context) {
        Integer cycleStartDay = dataService.getProductionCycleConfiguration(context);
        context.setStartDay(cycleStartDay);
        Integer year = context.getYear();
        Integer month = context.getMonth();
        //自然月
        if (context.isNaturalMonth()) {
            LocalDate productionMonth = context.getCurrentMonth();
            Integer monthDays = productionMonth.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
            context.setProductionStartDate(com.zlt.aps.factory.utils.DateUtils.getDate(productionMonth));
            context.setProductionEndDate(com.zlt.aps.factory.utils.DateUtils.getDate(year, month, monthDays));
            return;
        }
        //非自然月
        LocalDate previousMonth = context.getPreviousMonth();
        context.setProductionStartDate(com.zlt.aps.factory.utils.DateUtils.getDate(previousMonth.getYear(), previousMonth.getMonthValue(), cycleStartDay));
        context.setProductionEndDate(com.zlt.aps.factory.utils.DateUtils.getDate(year, month, cycleStartDay - 1));
    }

    public ProductionMdmDataService getDataService() {
        return dataService;
    }

    public DpRequireDataService getDpRequireDataService() {
        return dpRequireDataService;
    }

    public MonthProductionDataService getMonthProductionDataService() {
        return monthProductionDataService;
    }

    /**
     * 根据工厂编码 + 年月 + 需求计划版本，获取对应的月需要排产的需求计划
     *
     * @param productionContext
     * @return
     */
    protected List<MonthPlanProductionRequirePlanVo> getMonthPlanRequirePlan(TbrProductionContext productionContext) {
        //得到制造需求计划
        List<DpDemandPlan> monthPlanRequireList = dpRequireDataService.getFactoryMonthPlan(productionContext);
        if (CollectionUtils.isEmpty(monthPlanRequireList)) {
            String planListIsNull = I18nUtil.getMessage("alg.data.alter.message.planListIsNull");
            throw new BusinessException(String.format(planListIsNull, productionContext.getYear(), productionContext.getMonth(), productionContext.getMonthPlanVersion()));
        }
        List<MonthPlanProductionRequirePlanVo> productionPlanList = new ArrayList<>();
        monthPlanRequireList.forEach(require -> {
            MonthPlanProductionRequirePlanVo productionPlan = MonthPlanProductionRequirePlanVo.buildInitProductionPlan(productionContext, productionContext.getProductionVersion(), require);
            productionPlanList.add(productionPlan);
        });
        return productionPlanList;
    }

    /**
     * 获取初始化业务的参数设定
     *
     * @param productionContext
     * @return
     */
    protected ProductionInitParamConfiguration createInitParamConfiguration(TbrProductionContext productionContext) {
        ProductionInitParamConfiguration configuration = new ProductionInitParamConfiguration();
        List<String> paramCodeList = new ArrayList<>(16);
        paramCodeList.add(MonthPlanEnums.OPEN_PREEMPTION_MOULD.getCode());
        paramCodeList.add(MonthPlanEnums.OPEN_LEVEL_RATIO.getCode());
        paramCodeList.add(MonthPlanEnums.DAY_VULCANIZATION_MODE.getCode());
        Map<String, Object> paramConfigurationMap = getDataService().getFactoryParamByCondition(productionContext, paramCodeList);
        if (CollectionUtils.isEmpty(paramConfigurationMap)) {
            log.info(TbrProductionInitLogRecorder.addInitParamEmptyLog(productionContext));
            return configuration;
        }
        configuration.setOpenPreemptionMouldCapacity((String) paramConfigurationMap.get(MonthPlanEnums.OPEN_PREEMPTION_MOULD.getCode()));
        configuration.setOpenLevelRatio((String) paramConfigurationMap.get(MonthPlanEnums.OPEN_LEVEL_RATIO.getCode()));
        //日硫化量获取
        String dayVulcanizationParam = (String) paramConfigurationMap.get(MonthPlanEnums.DAY_VULCANIZATION_MODE.getCode());
        if (StringUtils.isBlank(dayVulcanizationParam)) {
            configuration.setDayVulcanizationQtyConfiguration(DayVulcanizationModeEnum.STANDARD_CAPACITY);
        } else {
            configuration.setDayVulcanizationQtyConfiguration(DayVulcanizationModeEnum.getInstance(dayVulcanizationParam));
        }
        return configuration;
    }

    /**
     * 获取物料基础信息
     * key = materialDesc: value = MdmMaterialInfo
     * 对物料描述去重(数据问题，应该源头控制)
     *
     * @param productionContext
     * @return
     */
    protected Map<String, ProductBaseInfoVo> getMaterialInfo(TbrProductionContext productionContext) {
        List<ProductBaseInfoVo> productBaseInfoList = getDataService().getProductionMaterialInfo(productionContext);
        if (CollectionUtils.isEmpty(productBaseInfoList)) {
            log.info(TbrProductionInitLogRecorder.addMaterialInfoEmptyLog(productionContext));
            return Collections.emptyMap();
        }
        return productBaseInfoList.stream().collect(Collectors.toMap(ProductBaseInfoVo::getMaterialDesc, Function.identity(), (before, after) -> before));
    }

    /**
     * 获取需要排产的SKU的施工配置信息
     * key = materialCode: value = List<MonthPlanProductConstructionInfoVo>
     *
     * @param productionContext
     * @return
     */
    protected Map<String, List<MonthPlanProductConstructionInfoVo>> getProductionConstructionInfo(TbrProductionContext productionContext) {
        List<MonthPlanProductConstructionInfoVo> constructionInfoList = getDataService().getProductionConstructionInfo(productionContext);
        if (CollectionUtils.isEmpty(constructionInfoList)) {
            log.info(TbrProductionInitLogRecorder.addConstructionInfoEmptyLog(productionContext));
            return Collections.emptyMap();
        }
        return constructionInfoList.stream().collect(Collectors.groupingBy(MonthPlanProductConstructionInfoVo::getMaterialCode));
    }

    /**
     * 获取需要排产的SKU的模具配置信息
     * key = materialDesc: value = List<MonthPlanProductMouldInfoVo>
     *
     * @param productionContext
     * @return
     */
    protected Map<String, List<MonthPlanProductMouldInfoVo>> getProductionMouldInfo(TbrProductionContext productionContext) {
        //已有模具的配置关系
        List<MonthPlanProductMouldInfoVo> productMouldInfoList = getDataService().getProductionMouldInfo(productionContext);
        //新模具到货计划关系
        List<MonthPlanProductMouldInfoVo> mouldDeliveryList = getDataService().getProductionMouldDeliveryInfo(productionContext);
        List<MonthPlanProductMouldInfoVo> allMouldRelationInfoList = MouldRelationDeduplicator.deduplicateAndMerge(productMouldInfoList, mouldDeliveryList);
        if (CollectionUtils.isEmpty(allMouldRelationInfoList)) {
            return Collections.emptyMap();
        }
        return allMouldRelationInfoList.stream().collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getMaterialDesc));
    }

    /**
     * 2.1.9.1：获取需要排产的SKU的模具配置信息
     * key = materialDesc: value = List<MonthPlanProductMouldInfoVo>
     *
     * @param productionContext 排产上下文
     * @return
     */
    private Map<String, List<MonthPlanProductMouldInfoVo>> getProductionMouldInfo2(TbrProductionContext productionContext) {
        //已有模具的配置关系
        List<MonthPlanProductMouldInfoVo> productMouldInfoList = getDataService().getEnableProductionMouldInfo(productionContext);
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderMouldRelationLog(productionContext, productMouldInfoList));
        //新模具到货计划关系
        List<MonthPlanProductMouldInfoVo> mouldDeliveryList = getDataService().getEnableProductionMouldDeliveryInfo(productionContext);
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderMouldDeliveryLog(productionContext, mouldDeliveryList));
        List<MonthPlanProductMouldInfoVo> allMouldRelationInfoList = MouldRelationDeduplicator.deduplicateAndMerge(productMouldInfoList, mouldDeliveryList);
        if (CollectionUtils.isEmpty(allMouldRelationInfoList)) {
            return Collections.emptyMap();
        }
        //构建所有模具关系的模具信息
        Map<String, List<MonthPlanProductMouldInfoVo>> skuModuleMap = allMouldRelationInfoList.stream().collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getMaterialDesc));
        Map<String, ProductionMouldInfoVo> allMouldInfo = createProductionMouldInfo(productionContext, skuModuleMap);
        productionContext.getBaseDataContainer().setAllMouldInfoMap(allMouldInfo);
        //取状态可用的模具
        List<MonthPlanProductMouldInfoVo> enableMouldRelationInfoList = allMouldRelationInfoList.stream().filter(singleRelationInfo -> YesOrNoEnum.YES.getCode().equals(singleRelationInfo.getMouldStatus())).collect(Collectors.toList());
        log.info(TbrBeforeProductionGroupLogRecorder.addEnableMouldRelationLog(productionContext, enableMouldRelationInfoList));
        if (CollectionUtils.isEmpty(enableMouldRelationInfoList)) {
            return Collections.emptyMap();
        }
        return enableMouldRelationInfoList.stream().collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getMaterialDesc));
    }

    /**
     * 2.1.9.2：根据物料可用模具关系，构建排产信息
     *
     * @param productionContext   排产上下文
     * @param mouldAssociationMap sku模具关系(包含新模具到货计划)
     * @return
     */
    private Map<String, ProductionMouldInfoVo> createProductionMouldInfo(TbrProductionContext productionContext, Map<String, List<MonthPlanProductMouldInfoVo>> mouldAssociationMap) {
        if (CollectionUtils.isEmpty(mouldAssociationMap)) {
            return Collections.emptyMap();
        }
        Map<String, ProductionMouldInfoVo> mouldInfoMap = new HashMap<>();
        mouldAssociationMap.forEach((materialDesc, associationList) -> {
            if (CollectionUtils.isEmpty(associationList)) {
                return;
            }
            //关系信息
            associationList.forEach(associationInfo -> {
                String mouldCode = associationInfo.getMouldCode();
                if (StringUtils.isBlank(mouldCode)) {
                    return;
                }
                ProductionMouldInfoVo productionMouldInfo = mouldInfoMap.get(mouldCode);
                if (null == productionMouldInfo) {
                    productionMouldInfo = ProductionMouldInfoVo.createEmptyProductionMouldInfo(associationInfo);
                    if (null == productionMouldInfo) {
                        return;
                    }
                    //设置模具的可排产日集合
                    productionMouldInfo.setProductionDayInfo(productionContext, associationInfo.getBoardingDate());
                    mouldInfoMap.put(mouldCode, productionMouldInfo);
                }
                //加入关联关系
                productionMouldInfo.getAssociationMaterialSet().add(materialDesc);
            });
        });
        return mouldInfoMap;
    }

    /**
     * 2.1.10：获取结构+主花纹的模具分配比例控制信息
     *
     * @param context
     * @return
     */
    private Map<String, MouldAllocationInfoVo> getGroupMainPatternAllocationInfo(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<MouldAllocationInfoVo> mouldAllocationInfoList = getDataService().getMouldAllocationInfo(productionContext);
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderMouldAllocationLog(context, mouldAllocationInfoList));
        if (CollectionUtils.isEmpty(mouldAllocationInfoList)) {
            return Collections.emptyMap();
        }
        Map<String, MouldAllocationInfoVo> groupMainPatternMap = mouldAllocationInfoList.stream().collect(Collectors.toMap(MouldAllocationInfoVo::getDuplicateKey, Function.identity(), (before, after) -> after));
        //根据排产周期，转换成每日量控制
        Set<Integer> productionDaySet = context.getProductionDay();
        groupMainPatternMap.forEach((controlDimension, allocationInfo) -> {
            Map<Integer, MouldAllocationDayInfoHelper> dayLimitInfoMap = new HashMap<>(productionDaySet.size());
            productionDaySet.forEach(productionDay -> {
                MouldAllocationDayInfoHelper dayLimit = MouldAllocationDayInfoHelper.buildInit(controlDimension, productionDay, allocationInfo.getAllocationQty());
                dayLimitInfoMap.put(productionDay, dayLimit);
            });
            allocationInfo.setDayLimitInfoMap(dayLimitInfoMap);
        });
        return groupMainPatternMap;
    }

    /**
     * 2.1.11：获取模壳台账信息，并加入新模具到货的模壳默认无上限
     *
     * @param context
     * @return
     */
    private Map<String, MouldShellBaseInfoVo> getMouldShellInfo(Context context) {
        Map<String, MouldShellBaseInfoVo> mouldShellMap = new HashMap<>();
        MouldShellBaseInfoVo noLimit = MouldShellBaseInfoVo.createNoLimit(ProductionConstant.NEW_MOULD_DELIVERY_SHELL);
        mouldShellMap.put(noLimit.getMouldSetCode(), noLimit);
        List<MouldShellBaseInfoVo> mouldShellList = getDataService().getMouldShellInfo(context);
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderMouldShellLog(context, mouldShellList));
        if (CollectionUtils.isEmpty(mouldShellList)) {
            return mouldShellMap;
        }
        for (MouldShellBaseInfoVo mouldShell : mouldShellList) {
            mouldShellMap.put(mouldShell.getMouldSetCode(), mouldShell);
        }
        Set<Integer> productionDaySet = context.getProductionDay();
        mouldShellMap.forEach((mouldSetCode, shellBaseInfo) -> {
            Map<Integer, MouldShellDayInfoHelper> dayLimitInfoMap = new HashMap<>();
            productionDaySet.forEach(productionDay -> {
                MouldShellDayInfoHelper dayLimitInfo = MouldShellDayInfoHelper.buildInit(mouldSetCode, productionDay, shellBaseInfo.getTotalQty());
                dayLimitInfoMap.put(productionDay, dayLimitInfo);
            });
            shellBaseInfo.setDayLimitInfoMap(dayLimitInfoMap);
        });
        return mouldShellMap;
    }

    /**
     * 2.1.12：获取胶囊卡盘台账信息
     *
     * @param context 排产上下文
     * @return
     */
    private Map<String, CapsuleChuckInfoVo> getCapsuleChuckInfo(Context context) {
        List<MdmCapsuleChuck> allCapsuleChuckList = getDataService().getCapsuleChuck(context);
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderCapsuleChuckLog(context, allCapsuleChuckList));
        if (CollectionUtils.isEmpty(allCapsuleChuckList)) {
            return Collections.emptyMap();
        }
        Map<Long, MdmCapsuleChuck> capsuleChuckGroupMap = allCapsuleChuckList.stream().collect(Collectors.toMap(MdmCapsuleChuck::getId, Function.identity()));
        Map<String, CapsuleChuckInfoVo> capsuleChuckLimitMap = new HashMap<>(capsuleChuckGroupMap.size());
        capsuleChuckGroupMap.forEach((configurationId, capsuleChuckInfo) -> {
            CapsuleChuckInfoVo limitInfo = CapsuleChuckInfoVo.builder(context, capsuleChuckInfo);
            if (null == limitInfo) {
                return;
            }
            capsuleChuckLimitMap.put(limitInfo.getGroupId(), limitInfo);
        });
        return capsuleChuckLimitMap;
    }

    /**
     * 2.1.13：获取计划对应结构成型硫化配比信息
     * 计划内的结构
     *
     * @param context         排产上下文
     * @param requirePlanList 需求计划信息
     * @return
     */
    private List<MonthPlanStructureLhRatioVo> getLhRatioConfiguration(Context context, List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return Collections.emptyList();
        }
        //提取结构查询条件
        Set<String> structureNameMap = requirePlanList.stream().map(MonthPlanProductionRequirePlanVo::getStructureName).collect(Collectors.toSet());
        List<String> structureNameList = new ArrayList<>(structureNameMap);
        List<MonthPlanStructureLhRatioVo> structureLhRatioList = getDataService().getLhRatioInfo(context, structureNameList);
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderCxLhGroupRatioLog(context, structureLhRatioList));
        if (CollectionUtils.isEmpty(structureLhRatioList)) {
            return Collections.emptyList();
        }
        //机型为空值，表示所有机型匹配
        structureLhRatioList.forEach(singleRatio -> {
            if (StringUtils.isNotBlank(singleRatio.getCxMachineTypeCode())) {
                return;
            }
            singleRatio.setCxMachineTypeCode(ProductionConstant.ALL_BRAND_CODE_MATCH);
        });
        //周期结构硫化配比
        List<CycleStructureMinLhMachineQtyVo> cycleStructureMinLhRatioList = getDpRequireDataService().getCycleLhRatioInfo(context);
        Map<String, Integer> cycleStructureMinLhRatioMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(cycleStructureMinLhRatioList)) {
            cycleStructureMinLhRatioList.forEach(cycleStructureMinLhRatio -> {
                cycleStructureMinLhRatioMap.put(cycleStructureMinLhRatio.getStructureName(), null == cycleStructureMinLhRatio.getMonthMinLhMachineQty() ? cycleStructureMinLhRatio.getMinLhMachineQty() : cycleStructureMinLhRatio.getMonthMinLhMachineQty());
            });
        }
        //常规结构的最低硫化配比
        Integer defaultMinLhRatio = ((TbrProductionContext) context).getBaseDataContainer().getParamConfiguration().getNoCycleProductionMinLhMachineNumber();
        structureLhRatioList.forEach(structureLhRatio -> {
            String structureName = structureLhRatio.getStructureName();
            structureLhRatio.setLhMachineMinQty(defaultMinLhRatio);
            //如果是周期，则换成周期
            if (cycleStructureMinLhRatioMap.containsKey(structureName)) {
                structureLhRatio.setLhMachineMinQty(cycleStructureMinLhRatioMap.get(structureName));
                return;
            }
        });
        return structureLhRatioList;
    }

    /**
     * 2.1.15：构建分组+主花纹的模具信息
     * TBR 为结构
     *
     * @param productionContext 排产上下文
     * @return
     */
    private void buildGroupMainPatternInfo(TbrProductionContext productionContext) {
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        Map<String, List<MonthPlanProductMouldInfoVo>> mouldAssociationMap = baseDataContainer.getSkuMouldRelationMap();
        Map<String, ProductionMouldInfoVo> allMouldMap = baseDataContainer.getMouldInfoMap();
        if (CollectionUtils.isEmpty(mouldAssociationMap) || CollectionUtils.isEmpty(allMouldMap)) {
            baseDataContainer.setGroupMainPatternMouldRelationMap(Collections.emptyMap());
            return;
        }
        List<MonthPlanProductMouldInfoVo> allRelationList = new ArrayList<>();
        mouldAssociationMap.forEach((materialDesc, relationList) -> {
            if (CollectionUtils.isEmpty(relationList)) {
                return;
            }
            allRelationList.addAll(relationList);
        });
        if (CollectionUtils.isEmpty(allRelationList)) {
            baseDataContainer.setGroupMainPatternMouldRelationMap(Collections.emptyMap());
            return;
        }
        Map<String, List<MonthPlanProductMouldInfoVo>> groupMainPatternMap = allRelationList.stream().collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getStructureNameAndMainPattern));
        Map<String, List<ProductionMouldInfoVo>> groupMainPatternMouldMap = new HashMap<>();
        groupMainPatternMap.forEach((groupNameAndMainPattern, relationList) -> {
            if (CollectionUtils.isEmpty(relationList)) {
                return;
            }
            List<ProductionMouldInfoVo> groupMainPatternList = new ArrayList<>();
            Set<String> mouldCodeSet = new HashSet<>();
            relationList.forEach(singleRelation -> {
                String mouldCode = singleRelation.getMouldCode();
                if (mouldCodeSet.contains(mouldCode)) {
                    return;
                }
                mouldCodeSet.add(mouldCode);
                if (allMouldMap.containsKey(mouldCode)) {
                    groupMainPatternList.add(allMouldMap.get(mouldCode));
                }
            });
            if (CollectionUtils.isEmpty(groupMainPatternList)) {
                return;
            }
            groupMainPatternMouldMap.put(groupNameAndMainPattern, groupMainPatternList);
        });
        baseDataContainer.setGroupMainPatternMouldRelationMap(groupMainPatternMouldMap);
    }

    /**
     * 获取SKU的日硫化产能信息
     * key = materialDesc: value = MonthPlanProductLhCapacityVo
     *
     * @param productionContext 排产上下文
     * @param mode              日硫化量模式
     * @return
     */
    protected Map<String, MonthPlanProductLhCapacityVo> getProductLhCapacityInfo(TbrProductionContext productionContext, DayVulcanizationModeEnum mode) {
        List<MonthPlanProductLhCapacityVo> lhCapacityList = getDataService().getProductLhCapacityInfo(productionContext);
        if (CollectionUtils.isEmpty(lhCapacityList)) {
            log.info(TbrProductionInitLogRecorder.addDayLhCapacityInfoEmptyLog(productionContext));
            return Collections.emptyMap();
        }
        //计算日硫化产能
        lhCapacityList.forEach(lhCapacity -> lhCapacity.calculateDayVulcanizationQty(mode));
        return lhCapacityList.stream().collect(Collectors.toMap(MonthPlanProductLhCapacityVo::getMaterialDesc, Function.identity(), (before, after) -> after));
    }

    /**
     * 2.1：排产前基础数据初始化
     *
     * @param productionContext 排产上下文
     * @param requirePlanList   排产计划
     */
    protected void initProductionBaseData(TbrProductionContext productionContext, List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        //1、获取排产参数设定
        ProductionCapacityParamConfiguration paramConfiguration = createParamConfiguration(productionContext);
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderProductionParamLog(productionContext, paramConfiguration));
        if (null == paramConfiguration) {
            paramConfiguration = new ProductionCapacityParamConfiguration();
        }
        productionContext.getBaseDataContainer().setParamConfiguration(paramConfiguration);
        //2、特殊材料的胎胚配置信息
        specialMaterialInfoHandler(productionContext);
        //3、超6个成品库存信息
        overSixMonthStockHandler(productionContext);
        //4、初始化库销比、标记是否按总需求排产
        initProductionRequirePlanInfo(productionContext, requirePlanList);
        //5、获取周期内的生产日历信息
        setMonthProductionDays(productionContext);
        //6、构建全局日排产限制信息
        buildDayCapacityLimitInfo(productionContext);
        //7、获取成型机台信息--日产信息
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = getDataService().getCxMachineBaseInfo(productionContext);
        productionContext.getBaseDataContainer().setCxMachineBaseInfo(cxMachineBaseInfo);
        //8、成型鼓
        Map<String, Map<String, TireDrumInfoVo>> workWearTypeInfoMap = getWorkWearInfo(productionContext);
        productionContext.getBaseDataContainer().setTireDrumInfoMap(workWearTypeInfoMap);
        //9、获取SKU模具配置信息
        Map<String, List<MonthPlanProductMouldInfoVo>> mouldRelationMap = getProductionMouldInfo2(productionContext);
        Map<String, ProductionMouldInfoVo> mouldInfoMap = createProductionMouldInfo(productionContext, mouldRelationMap);
        productionContext.getBaseDataContainer().setMouldInfoMap(mouldInfoMap);
        productionContext.getBaseDataContainer().setSkuMouldRelationMap(mouldRelationMap);
        //10、结构模具分配配比
        Map<String, MouldAllocationInfoVo> mouldAllocationMap = getGroupMainPatternAllocationInfo(productionContext);
        productionContext.getBaseDataContainer().setGroupMainPatternAllocationLimitMap(mouldAllocationMap);
        //11、获取模壳配置信息
        Map<String, MouldShellBaseInfoVo> mouldShellMap = getMouldShellInfo(productionContext);
        productionContext.getBaseDataContainer().setMouldShellMap(mouldShellMap);
        //12、获取胶囊卡盘配置信息
        Map<String, CapsuleChuckInfoVo> capsuleChuckInfoMap = getCapsuleChuckInfo(productionContext);
        productionContext.getBaseDataContainer().setCapsuleChuckInfoMap(capsuleChuckInfoMap);
        //13、获取结构的硫化配比
        List<MonthPlanStructureLhRatioVo> structureLhRatioList = getLhRatioConfiguration(productionContext, requirePlanList);
        productionContext.getBaseDataContainer().setStructureLhRatioList(structureLhRatioList);
        //16、机台近3个月的生产历史信息
        List<MpStructureAllocation> historyAllocationList = monthProductionDataService.getHistoryStructureAllocationInfo(productionContext);
        Map<String, CxMachineProductionHistoryInfo> cxMachineProductionHistoryInfo = productionHistoryHandler.buildCxMachineProductionHistory(productionContext, historyAllocationList);
        productionContext.getBaseDataContainer().setCxMachineProductionHistoryInfo(cxMachineProductionHistoryInfo);
        Map<String, GroupPlanProductionHistoryInfo> groupPlanHistoryInfoMap = productionHistoryHandler.buildGroupPlanProductionHistory(productionContext, historyAllocationList);
        productionContext.getBaseDataContainer().setGroupPlanHistoryInfoMap(groupPlanHistoryInfoMap);
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return;
        }
        Set<String> isSetStructureNameSet = new HashSet<>();
        //14、根据计划的物料描述，补充模具关系中的物料结构名
        requirePlanList.forEach(requirePlan -> {
            String materialDesc = requirePlan.getMaterialDesc();
            if (StringUtils.isBlank(materialDesc)) {
                return;
            }
            if (isSetStructureNameSet.contains(materialDesc)) {
                return;
            }
            isSetStructureNameSet.add(materialDesc);
            List<MonthPlanProductMouldInfoVo> mouldRelationList = mouldRelationMap.get(requirePlan.getMaterialDesc());
            if (CollectionUtils.isEmpty(mouldRelationList)) {
                return;
            }
            mouldRelationList.forEach(mouldRelation -> {
                mouldRelation.setStructureName(requirePlan.getStructureName());
            });
        });
        //15、构建结构、主花纹的模具信息
        buildGroupMainPatternInfo(productionContext);
    }

    /**
     * 2.1.1：获取初始化业务的参数设定
     *
     * @param productionContext
     * @return
     */
    protected ProductionCapacityParamConfiguration createParamConfiguration(TbrProductionContext productionContext) {
        List<String> paramCodeList = new ArrayList<>(64);
        //日排产相关
        paramCodeList.add(MonthPlanEnums.DAY_CHANGE_GROUP_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_MOULD_LH_MACHINE_NUMBER.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY_DIFF.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.SINGLE_CX_EMBRYO_CODE_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.DAY_MAX_CAPACITY.getCode());
        paramCodeList.add(MonthPlanEnums.DAY_MIN_CAPACITY.getCode());
        //排产控制相关
        paramCodeList.add(MonthPlanEnums.SUM_PRODUCTION_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.HEIGHT_DIFF_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.SKU_SECOND_PRODUCTION.getCode());
        paramCodeList.add(MonthPlanEnums.BOOST_PRODUCTION_TYPE_VALUE.getCode());
        paramCodeList.add(MonthPlanEnums.MATCHING_BOOST_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.MAX_BOOST_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.MIN_PRODUCTION_DAYS.getCode());
        paramCodeList.add(MonthPlanEnums.MIN_ALLOCATION_DAYS.getCode());
        paramCodeList.add(MonthPlanEnums.NO_CYCLE_PRODUCTION_MIN_LH_MACHINE_NUMBER.getCode());
        //降膜排产相关
        paramCodeList.add(MonthPlanEnums.DEDUCT_MOULD_MIN_LH_MACHINE_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.FIRST_NEAR_DEAD_LINE_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.FIRST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.SECOND_NEAR_DEAD_LINE_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.SECOND_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode());
        paramCodeList.add(MonthPlanEnums.LAST_NEAR_DEAD_LINE_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.LAST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode());
        //其他
        paramCodeList.add(MonthPlanEnums.SECTION_WIDTH_DIFF_VALUE.getCode());
        //获取数据
        Map<String, Object> paramConfigurationMap = getDataService().getFactoryParamByCondition(productionContext, paramCodeList);
        if (CollectionUtils.isEmpty(paramConfigurationMap)) {
            return null;
        }
        ProductionCapacityParamConfiguration configuration = new ProductionCapacityParamConfiguration();
        //排产控制相关
        Object minProductionDaysValue = paramConfigurationMap.get(MonthPlanEnums.MIN_PRODUCTION_DAYS.getCode());
        if (null == minProductionDaysValue) {
            configuration.setMinProductionDays(BigDecimal.ZERO.intValue());
        } else {
            configuration.setMinProductionDays((Integer) minProductionDaysValue);
        }
        configuration.setMinAllocationDays((Integer) paramConfigurationMap.get(MonthPlanEnums.MIN_ALLOCATION_DAYS.getCode()));
        configuration.setNoCycleProductionMinLhMachineNumber((Integer) paramConfigurationMap.get(MonthPlanEnums.NO_CYCLE_PRODUCTION_MIN_LH_MACHINE_NUMBER.getCode()));
        String boostProductionTypeValue = (String) paramConfigurationMap.get(MonthPlanEnums.BOOST_PRODUCTION_TYPE_VALUE.getCode());
        if (StringUtils.isBlank(boostProductionTypeValue)) {
            configuration.setBoostProductionType(Collections.emptySet());
        } else {
            configuration.setBoostProductionType(Stream.of(boostProductionTypeValue.split(StringConstant.COMMA)).collect(Collectors.toSet()));
        }
        configuration.setMaxBoostDay((Integer) paramConfigurationMap.get(MonthPlanEnums.MAX_BOOST_DAY.getCode()));
        configuration.setMatchingBoostDay((Integer) paramConfigurationMap.get(MonthPlanEnums.MATCHING_BOOST_DAY.getCode()));
        configuration.setSkuSecondProduction((Integer) paramConfigurationMap.get(MonthPlanEnums.SKU_SECOND_PRODUCTION.getCode()));
        configuration.setHeightDiffQty((Integer) paramConfigurationMap.get(MonthPlanEnums.HEIGHT_DIFF_QTY.getCode()));
        configuration.setSumProductionQty((Integer) paramConfigurationMap.get(MonthPlanEnums.SUM_PRODUCTION_QTY.getCode()));
        //日排产相关
        configuration.setDayChangeGroupCount((Integer) paramConfigurationMap.get(MonthPlanEnums.DAY_CHANGE_GROUP_COUNT.getCode()));
        configuration.setChangeMouldLhMachineNumber((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_MOULD_LH_MACHINE_NUMBER.getCode()));
        configuration.setChangeMouldFirstQty((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode()));
        configuration.setChangeTypeBlockQtyDiff((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY_DIFF.getCode()));
        configuration.setChangeTypeBlockQty((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode()));
        configuration.setChangeTypeBlockMaxQty((Integer) paramConfigurationMap.get(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode()));
        configuration.setSingleCxEmbryoCodeCount((Integer) paramConfigurationMap.get(MonthPlanEnums.SINGLE_CX_EMBRYO_CODE_COUNT.getCode()));
        configuration.setDayMaxCapacity((Integer) paramConfigurationMap.get(MonthPlanEnums.DAY_MAX_CAPACITY.getCode()));
        configuration.setDayMinCapacity((Integer) paramConfigurationMap.get(MonthPlanEnums.DAY_MIN_CAPACITY.getCode()));
        //降膜排产相关
        configuration.setDeductMouldMinLhMachineCount((Integer) paramConfigurationMap.get(MonthPlanEnums.DEDUCT_MOULD_MIN_LH_MACHINE_COUNT.getCode()));
        configuration.setFirstNearDeadLineMaxLhMachineCount((Integer) paramConfigurationMap.get(MonthPlanEnums.FIRST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode()));
        configuration.setFirstNearDeadLineDay((Integer) paramConfigurationMap.get(MonthPlanEnums.FIRST_NEAR_DEAD_LINE_DAY.getCode()));
        configuration.setSecondNearDeadLineMaxLhMachineCount((Integer) paramConfigurationMap.get(MonthPlanEnums.SECOND_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode()));
        configuration.setSecondNearDeadLineDay((Integer) paramConfigurationMap.get(MonthPlanEnums.SECOND_NEAR_DEAD_LINE_DAY.getCode()));
        configuration.setLastNearDeadLineMaxLhMachineCount((Integer) paramConfigurationMap.get(MonthPlanEnums.LAST_NEAR_DEAD_LINE_MAX_LH_MACHINE_COUNT.getCode()));
        configuration.setLastNearDeadLineDay((Integer) paramConfigurationMap.get(MonthPlanEnums.LAST_NEAR_DEAD_LINE_DAY.getCode()));
        //其它
        configuration.setSectionWidthDiffValue((Integer) paramConfigurationMap.get(MonthPlanEnums.SECTION_WIDTH_DIFF_VALUE.getCode()));
        return configuration;
    }

    /**
     * 2.1.2：根据排产信息，获取特殊原材料的配置信息 包含：
     * 1、特殊原材料的胎胚
     * 2、特殊原材料的库存及可转化的轮胎条数
     *
     * @param productionContext 排产单位
     */
    private void specialMaterialInfoHandler(TbrProductionContext productionContext) {
        List<EmbryoSpecialMaterialInfoVo> specialMaterialInfoList = getDataService().getEmbryoSpecialMaterialInfo(productionContext);
        Map<String, Map<String, BigDecimal>> embryoSpecialMaterialMap = new HashMap<>();
        Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap = new HashMap<>();
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderSpecialMaterialLog(productionContext, specialMaterialInfoList));
        if (CollectionUtils.isEmpty(specialMaterialInfoList)) {
            productionContext.getBaseDataContainer().setEmbryoSpecialMaterialInfoMap(embryoSpecialMaterialMap);
            productionContext.setSpecialMaterialInfoMap(specialMaterialInfoMap);
            return;
        }
        //转化胎胚号-特殊材料
        Map<String, List<EmbryoSpecialMaterialInfoVo>> allSpecialMaterialMap = specialMaterialInfoList.stream().collect(Collectors.groupingBy(EmbryoSpecialMaterialInfoVo::getEmbryoCode));
        allSpecialMaterialMap.forEach((embryoCode, rawMaterialList) -> {
            if (CollectionUtils.isEmpty(rawMaterialList)) {
                return;
            }
            Map<String, BigDecimal> rawMaterialConfigurationMap = embryoSpecialMaterialMap.get(embryoCode);
            if (null == rawMaterialConfigurationMap) {
                rawMaterialConfigurationMap = new HashMap<>();
                embryoSpecialMaterialMap.put(embryoCode, rawMaterialConfigurationMap);
            }
            for (EmbryoSpecialMaterialInfoVo embryoSpecialMaterialInfo : rawMaterialList) {
                String specialMaterialCode = embryoSpecialMaterialInfo.getChildMaterialCode();
                if (org.apache.commons.lang3.StringUtils.isBlank(specialMaterialCode)) {
                    continue;
                }
                BigDecimal dosage = embryoSpecialMaterialInfo.getDosage();
                rawMaterialConfigurationMap.put(specialMaterialCode, dosage);
            }
        });
        productionContext.getBaseDataContainer().setEmbryoSpecialMaterialInfoMap(embryoSpecialMaterialMap);
        //构建特殊原材料库存信息
        specialMaterialStockHandler(productionContext);
    }

    /**
     * 2.1.3：加载超6个月的库存信息
     *
     * @param productionContext
     */
    private void overSixMonthStockHandler(TbrProductionContext productionContext) {
        List<MdmProductStock> stockList = getDataService().getMdmProductStock(productionContext);
        //过滤库存为空的值
        Map<String, Integer> overSixMonthStockMap = stockList.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getMaterialDesc()) && null != s.getStockQty())
                .collect(Collectors.groupingBy(MdmProductStock::getMaterialDesc,
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream().filter(s -> ApsConstant.TRUE.equals(s.getIsExceedSixMonth()))
                                        .collect(Collectors.summingInt(MdmProductStock::getStockQty)))));
        productionContext.setOverSixMonthStockMap(overSixMonthStockMap);
    }

    /**
     * 2.1.2.1：构建特殊原材料的库存信息对象
     *
     * @param productionContext
     */
    private void specialMaterialStockHandler(TbrProductionContext productionContext) {
        //获取特殊材料库存信息
        List<SpecialMaterialStockVo> specialMaterialStockList = getDataService().getSpecialMaterialStockInfo(productionContext);
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderSpecialMaterialStockLog(productionContext, specialMaterialStockList));
        if (CollectionUtils.isEmpty(specialMaterialStockList)) {
            productionContext.setSpecialMaterialInfoMap(new HashMap<>());
            return;
        }
        Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap = new HashMap<>();
        //构建库存对应的可生产量
        specialMaterialStockList.forEach(specialMaterialStockInfo -> {
            String specialMaterialCode = specialMaterialStockInfo.getMaterialCode();
            if (org.apache.commons.lang3.StringUtils.isBlank(specialMaterialCode)) {
                return;
            }
            Map<Long, SpecialMaterialInfoVo> standardLengthMap = specialMaterialInfoMap.get(specialMaterialCode);
            if (null == standardLengthMap) {
                standardLengthMap = new HashMap<>();
                specialMaterialInfoMap.put(specialMaterialCode, standardLengthMap);
            }
            standardLengthMap.put(specialMaterialStockInfo.getStandardLength(), SpecialMaterialInfoVo.createInitInfo(specialMaterialStockInfo));
        });
        productionContext.setSpecialMaterialInfoMap(specialMaterialInfoMap);
    }

    /**
     * 2.1.4：初始化排产计划，主要进行按sku分组和初始化库销比
     *
     * @param productionContext 排产上下文
     * @param requirePlanList   需求计划列表
     */
    private void initProductionRequirePlanInfo(TbrProductionContext productionContext, List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        productionContext.setAllProductionPlan(new HashMap<>());
        productionContext.setAllSkuProductionPlan(new HashMap<>());
        productionContext.setSkuPlannedQtyMap(new HashMap<>());
        productionContext.setSkuWastageQtyMap(new HashMap<>());
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return;
        }
        //按计划Id分组，全局存储
        productionContext.setAllProductionPlan(requirePlanList.stream().collect(Collectors.toMap(MonthPlanProductionRequirePlanVo::getMonthPlanId, Function.identity())));
        //按物料描述分组，全局存储
        List<MonthPlanProductionRequirePlanVo> effectiveList = requirePlanList.stream().filter(singlePlan -> StringUtils.isNotBlank(singlePlan.getMaterialDesc())).collect(Collectors.toList());
        Map<String, List<MonthPlanProductionRequirePlanVo>> skuRequirePlanMap;
        if (CollectionUtils.isEmpty(effectiveList)) {
            skuRequirePlanMap = new HashMap<>();
        } else {
            skuRequirePlanMap = effectiveList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        }
        productionContext.setAllSkuProductionPlan(skuRequirePlanMap);
        //已排产量和损耗量为零
        ProductionCapacityParamConfiguration param = productionContext.getBaseDataContainer().getParamConfiguration();
        skuRequirePlanMap.forEach((materialDesc, productionPlanList) -> {
            productionContext.getSkuPlannedQtyMap().put(materialDesc, BigDecimal.ZERO.intValue());
            productionContext.getSkuWastageQtyMap().put(materialDesc, BigDecimal.ZERO.intValue());
            if (CollectionUtils.isEmpty(productionPlanList)) {
                return;
            }
            //是否含有特殊原材料的SKU 是否按总需求排产-默认 = 否
            productionPlanList.forEach(requirePlan -> {
                requirePlan.setIsProductionBySum(Constant.FALSE);
                requirePlan.setIsSpecialMaterials(YesOrNoEnum.NO.getCode());
                String embryoCode = requirePlan.getEmbryoCode();
                if (StringUtils.isNotBlank(embryoCode) && productionContext.getBaseDataContainer().getEmbryoSpecialMaterialInfoMap().containsKey(embryoCode)) {
                    requirePlan.setIsSpecialMaterials(YesOrNoEnum.YES.getCode());
                }
            });
            List<MonthPlanProductionRequirePlanVo> hasProductionList = productionPlanList.stream().filter(plan -> plan.hasProduction()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(hasProductionList)) {
                return;
            }
            //总需求量小于一定值
            Integer sumProductionQty = hasProductionList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getNetQty).sum();
            if (sumProductionQty <= param.getSumProductionQty()) {
                productionPlanList.forEach(requirePlan -> requirePlan.setIsProductionBySum(Constant.TRUE));
            }
            //总需求量与高优先级量差值小于一定值
            Integer sumHeightProductionQty = hasProductionList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getHeightQty).sum();
            if (sumProductionQty - sumHeightProductionQty <= param.getHeightDiffQty()) {
                productionPlanList.forEach(requirePlan -> requirePlan.setIsProductionBySum(Constant.TRUE));
            }
        });
        //周期结构-按总需求排产
        requirePlanList.forEach(requirePlan -> {
            //周期排产按总量排产
            if (ProductionGroupTypeEnum.CYCLE.getGroupType().equals(requirePlan.getStructureType())) {
                requirePlan.setIsProductionBySum(Constant.TRUE);
            }
        });
        //计算初始的库销比
        requirePlanList.forEach(requirePlan -> requirePlan.calculateInventorySalesRatio(BigDecimal.ZERO.intValue()));
    }

    /**
     * 2.1.5：设置工厂的排产日信息
     * 包含 停产日及开停产的产能比例
     * t_mdm_work_calendar
     *
     * @param context
     */
    private void setMonthProductionDays(Context context) {
        List<ProductionDayInfoVo> productionDayInfoList = getDataService().getProductCalendar(context);
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderProductionCalendarLog(context, productionDayInfoList));
        Integer maxBoostDays = ((TbrProductionContext) context).getBaseDataContainer().getParamConfiguration().getMaxBoostDay();
        if (CollectionUtils.isEmpty(productionDayInfoList)) {
            context.setCapacityRatioMap(Collections.emptyMap());
            context.setStopDays(Collections.emptySet(), maxBoostDays);
            throw new BusinessException(I18nUtil.getMessage("alg.data.production.noConfigurationCalendar"));
        }
        //排产开始日
        Date productionStartDate = context.getProductionStartDate();
        //开产比例设置
        Map<Integer, Integer> startProductionRatioMap = new HashMap<>(context.getMonthDays());
        List<ProductionDayInfoVo> startProductionDays = productionDayInfoList.stream().filter(productionDayInfo -> YesOrNoEnum.YES.getCode().equals(productionDayInfo.getDayFlag())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(startProductionDays)) {
            startProductionDays.forEach(startProductionInfo -> {
                Date startProduction = startProductionInfo.getProductionDate();
                Integer startDay = com.zlt.aps.factory.utils.DateUtils.getIntervalDays(productionStartDate, startProduction);
                startProductionRatioMap.put(startDay, startProductionInfo.getRate());
            });
        }
        context.setCapacityRatioMap(startProductionRatioMap);
        //停产设置
        List<ProductionDayInfoVo> stopDays = productionDayInfoList.stream().filter(productionDayInfo -> YesOrNoEnum.NO.getCode().equals(productionDayInfo.getDayFlag())).collect(Collectors.toList());
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderStopCalendarLog(context, stopDays));
        if (CollectionUtils.isEmpty(stopDays)) {
            context.setStopDays(Collections.emptySet(), maxBoostDays);
            return;
        }
        Set<Integer> stopDaySet = new HashSet<>(context.getMonthDays());
        stopDays.forEach(stopProductionInfo -> {
            Date stopProduction = stopProductionInfo.getProductionDate();
            Integer stopDay = com.zlt.aps.factory.utils.DateUtils.getIntervalDays(productionStartDate, stopProduction);
            stopDaySet.add(stopDay);
        });
        context.setStopDays(stopDaySet, maxBoostDays);
    }

    /**
     * 2.1.6：构建日产能限制对象信息
     *
     * @param productionContext 排产上下文
     */
    private void buildDayCapacityLimitInfo(TbrProductionContext productionContext) {
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        DayCapacityLimitVo dayCapacityLimit = new DayCapacityLimitVo(Collections.emptyMap());
        Set<Integer> productionDayList = productionContext.getProductionDay();
        if (CollectionUtils.isEmpty(productionDayList)) {
            baseDataContainer.setDayCapacityLimit(dayCapacityLimit);
            return;
        }
        Set<Integer> openDay = productionContext.getProductionDayAfterStop();
        ProductionCapacityParamConfiguration paramConfiguration = baseDataContainer.getParamConfiguration();
        Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap = new HashMap<>(productionDayList.size());
        Map<Integer, Integer> startProductionRatioMap = productionContext.getCapacityRatioMap();
        productionDayList.forEach(productionDay -> {
            Integer ratio = startProductionRatioMap.get(productionDay);
            //20260127 开产时，只是量放一半，日产限制还是放大到100
            if (openDay.contains(productionDay)) {
                ratio = ProductionConstant.PERCENTAGE;
            }
            DayCapacityLimitHelper dayInitLimit = DayCapacityLimitHelper.createInit(productionDay, paramConfiguration, ratio);
            dayCapacityLimitMap.put(productionDay, dayInitLimit);
        });
        dayCapacityLimit.updateWholeDayLimitInfo(dayCapacityLimitMap);
        baseDataContainer.setDayCapacityLimit(dayCapacityLimit);
    }

    /**
     * 2.1.8：获取成型鼓工装台账信息
     *
     * @param productionContext 排产上下文
     * @return
     */
    private Map<String, Map<String, TireDrumInfoVo>> getWorkWearInfo(TbrProductionContext productionContext) {
        List<MdmWorkWearInfo> allWorkWearInfo = getDataService().getWorkWearInfo(productionContext);
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderWorkWearInfoLog(productionContext, allWorkWearInfo));
        if (CollectionUtils.isEmpty(allWorkWearInfo)) {
            return Collections.emptyMap();
        }
        //按鼓类型分组
        Map<String, List<MdmWorkWearInfo>> workWearTypeGroup = allWorkWearInfo.stream().collect(Collectors.groupingBy(MdmWorkWearInfo::getWorkWearType));
        Map<String, Map<String, TireDrumInfoVo>> workWearTypeInfoMap = new HashMap<>();
        workWearTypeGroup.forEach((workWearType, configurationList) -> {
            if (CollectionUtils.isEmpty(configurationList)) {
                return;
            }
            Map<String, TireDrumInfoVo> singleTypeMap = new HashMap<>();
            configurationList.forEach(singleConfiguration -> {
                TireDrumInfoVo tireDrumInfo = TireDrumInfoVo.builder(productionContext, singleConfiguration);
                singleTypeMap.put(tireDrumInfo.getGroupId(), tireDrumInfo);
            });
            workWearTypeInfoMap.put(workWearType, singleTypeMap);
        });
        return workWearTypeInfoMap;
    }


}
