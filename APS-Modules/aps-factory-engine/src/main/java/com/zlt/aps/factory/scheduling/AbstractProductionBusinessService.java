package com.zlt.aps.factory.scheduling;

import com.ruoyi.common.core.utils.DateUtils;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.*;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.ProductionLimitTypeEnum;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import com.zlt.aps.factory.scheduling.moulding.GroupPlanProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.*;
import com.zlt.aps.maindata.domain.vo.DaySizeCapacityVo;
import com.zlt.aps.maindata.utils.FactoryParamUtils;
import com.zlt.aps.maindata.utils.SizeCapacityUtils;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.MaterialInfoGrossRateJsonVo;
import com.zlt.aps.monthplan.api.domain.vo.NoProductionDayMouldVo;
import com.zlt.aps.monthplan.api.domain.vo.ProductALevelVo;
import com.zlt.aps.monthplan.api.enums.MouldNoProductionType;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import com.zlt.aps.monthplan.api.enums.ProductionTypeEnum;
import com.zlt.common.utils.PubUtil;
import com.zlt.common.utils.SafeCompute;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * 数据提供接口
     */
    private final ProductionSchedulingDataService dataService;

    public AbstractProductionBusinessService(ProductionSchedulingDataService dataService) {
        this.dataService = dataService;
    }

    /**
     * 构建空的分组排产上下文
     *
     * @param context           上下文
     * @param productionContext 排产上下文
     * @param group             分组信息
     * @return
     */
    protected GroupPlanProductionContext buildEmptyGroupContext(Context context, ProductionContext productionContext, ProductionFirstSortOptionsEnum group) {
        GroupPlanProductionContext groupContext = new GroupPlanProductionContext();
        BeanUtils.copyProperties(context, groupContext);
        groupContext.setProductionContext(productionContext);
        ProductionPlanGroupVo groupDate = new ProductionPlanGroupVo();
        groupDate.setGroup(group);
        groupDate.setGroupPlanList(Collections.emptyList());
        groupContext.setProductionPlanGroup(groupDate);
        return groupContext;
    }

    /**
     * 根据初始的上下文，构建基础的排产上下文信息
     * 补充月生产最大天数：根据年份、月份得到该月最大的理论月天数
     * 并根据生产日历配置，得到最终的最大生产天数信息
     * 同时，设置工厂的停车日集合数据
     * 20250519 ZLT 因分自然月和非自然月两种方式，
     * 导致月份排产天数及停工日集合不能在此方法中初始化
     *
     * @param context
     * @return
     */
    protected ProductionContext buildProductionContext(Context context) {
        ProductionContext productionContext = new ProductionContext();
        BeanUtils.copyProperties(context, productionContext);
        productionContext.setOperationWorkNo(DateUtils.dateTimeNow());
        productionContext.setLogBuilder(new StringBuilder());
        productionContext.setProductionSchedulePlanMap(new HashMap<>());
        productionContext.setNoAssemblingMouldProductSet(new HashSet<>());
        productionContext.setExceedCapacityProductMap(new HashMap<>());
        return productionContext;
    }

    /**
     * 根据排产上下文信息，初始化排产的系统控制参数配置
     *
     * @param context
     */
    protected void initSysParams(ProductionContext context) {
        Map<String, FactoryParam> factoryParams = dataService.getFactoryParamConfiguration(context.getFactoryCode(), context.getProductType().getValue());
        Map<String, Object> paramMap = context.getFactoryParams();
        if (null == paramMap) {
            paramMap = new HashMap<>();
        }
        if (CollectionUtils.isEmpty(factoryParams)) {
            context.setFactoryParams(paramMap);
            return;
        }
        for (Map.Entry<String, FactoryParam> entry : factoryParams.entrySet()) {
            String paramCode = entry.getKey();
            paramMap.put(paramCode, getParamValue(paramCode, entry.getValue()));
        }
        ProductionParamConfiguration productionParam = buildProductionParam(paramMap, context);
        context.setProductionParam(productionParam);
        context.setFactoryParams(paramMap);
    }

    /**
     * 20250519 ZLT 初始化月份排产天信息
     * 包含排产月份最大可排产天数、
     * 停工信息、日排产最大量设置
     * 日排产最大规格数设置
     *
     * @param productionContext 排产上下文
     */
    protected void initMonthProductionDays(ProductionContext productionContext) {
        Integer year = productionContext.getYear();
        Integer month = productionContext.getMonth();
        LocalDate productionMonth = LocalDate.of(year, month, ProductionConstant.MONTH_START_DAY);
        //月份天数--自然月排产
        Integer monthDays = productionMonth.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        productionContext.setProductionStartDate(com.zlt.aps.factory.utils.DateUtils.getDate(productionMonth));
        productionContext.setProductionEndDate(com.zlt.aps.factory.utils.DateUtils.getDate(year, month, monthDays));
        //非自然月排产--则天数要重新计算-即为前一个月的天数
        if (!productionContext.isNaturalMonth()) {
            Integer startDay = productionContext.getProductionParam().getMonthCycleStartDay();
            LocalDate previousMonth = productionContext.getPreviousMonth();
            monthDays = previousMonth.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
            productionContext.setProductionStartDate(com.zlt.aps.factory.utils.DateUtils.getDate(previousMonth.getYear(), previousMonth.getMonthValue(), startDay));
            productionContext.setProductionEndDate(com.zlt.aps.factory.utils.DateUtils.getDate(year, month, startDay - 1));
        }
        productionContext.setMonthDays(monthDays);
        //20250927 ZLT 特殊天产能控制
        productionContext.getProductionParam().updateSpecialDayLimitByCycle(productionContext.getProductionStartDate(), productionContext.getProductionEndDate());
        //重新计算月份可生产天数--减去停开工日
        List<ProductionCalendarVO> productionCalendarList = dataService.getProductCalendar(productionContext);
        Set<Integer> stopDays;
        if (productionContext.isNaturalMonth()) {
            stopDays = com.zlt.aps.factory.utils.DateUtils.calculateStopDays(productionCalendarList);
        } else {
            stopDays = com.zlt.aps.factory.utils.DateUtils.calculateStopDaysByNoNaturalMonth(productionContext, productionCalendarList);
        }
        //停工日集合
        productionContext.setFactoryStopDays(stopDays);
        productionContext.setMonthWorkDays(monthDays - (CollectionUtils.isEmpty(stopDays) ? 0 : stopDays.size()));
        //设置月排产天信息
        setWholeMonthWorkDayInfo(productionContext);
        //设置排产控制值信息
        setProductionControlValue(productionContext);
        return;
    }

    /**
     * 更新排产版本的月份排产模式及开始、结束排产日信息
     *
     * @param productionContext
     */
    protected void updateProductionVersionInfo(ProductionContext productionContext) {
        String productionVersion = productionContext.getProductionVersion();
        FactoryProductionVersion factoryProductionVersion = new FactoryProductionVersion();
        factoryProductionVersion.setFactoryCode(productionContext.getFactoryCode());
        factoryProductionVersion.setYear(productionContext.getYear());
        factoryProductionVersion.setMonth(productionContext.getMonth());
        factoryProductionVersion.setMonthPlanVersion(productionContext.getMonthPlanVersion());
        //20250519 ZLT 设置月份排产模式自然月或非自然月及开始、结束排产日期
        setProductionVersionCycleInfo(factoryProductionVersion, productionVersion, productionContext);
        getDataService().updateProductionVersionInfo(factoryProductionVersion);
    }

    /**
     * 初始化物料及物料施工阶段
     *
     * @param context
     */
    protected void initProductInfo(ProductionContext context) {
        //基础的施工信息
        Map<String, BaseConstructionVersionInfoVo> baseConstructionInfoMap = dataService.getBaseConstructionInfo();
        context.setBaseConstructionInfoMap(baseConstructionInfoMap);
        //物料基础信息
        initProductBaseInfo(context);
        //物料施工信息，确认施工阶段
        List<MdmProductConstruction> productConstructionList = dataService.getProductConstruction(context);
        Map<String, ConstructionStageEnum> constructionStageMap = new HashMap<>();
        Map<String, Map<String, ProductConstructionInfoVo>> constructionConfigurationMap = new HashMap<>();
        if (CollectionUtils.isEmpty(productConstructionList)) {
            context.setConstructionStageMap(constructionStageMap);
            context.setConstructionConfigurationMap(constructionConfigurationMap);
            return;
        }
        productConstructionList.stream().forEach(productConstruction -> {
            String productCode = productConstruction.getProductCode();
            String constructionCode = productConstruction.getConstructionCode();
            ConstructionStageEnum stage = ConstructionStageEnum.matchByConstructionCode(constructionCode);
            if (null != stage) {
                constructionStageMap.put(productCode, stage);
            }
            String specCode = productConstruction.getSpecCode();
            Map<String, ProductConstructionInfoVo> productConstructionConfigurationMap = constructionConfigurationMap.get(productCode);
            if (null == productConstructionConfigurationMap) {
                productConstructionConfigurationMap = new HashMap<>();
            }
            ProductConstructionInfoVo productConstructionInfo = productConstructionConfigurationMap.get(specCode);
            if (null != productConstructionInfo) {
                return;
            }
            productConstructionInfo = new ProductConstructionInfoVo();
            productConstructionInfo.setProductCode(productCode);
            productConstructionInfo.setConstructionCode(constructionCode);
            productConstructionInfo.setSpecCode(specCode);
            productConstructionInfo.setEmbryoCode(productConstruction.getEmbryoCode());
            productConstructionInfo.setMouldMethod(productConstruction.getMouldMethod());
            productConstructionInfo.setSummerCuringTime(productConstruction.getCuringTime());
            productConstructionInfo.setWinterCuringTime(productConstruction.getCuringTime2());
            productConstructionInfo.setMouldClampingPressure(productConstruction.getMouldClampingPressure());
            productConstructionInfo.setMoldCavity(productConstruction.getMoldCavity());
            productConstructionInfo.setConstructionStage(stage);
            productConstructionConfigurationMap.put(specCode, productConstructionInfo);
            constructionConfigurationMap.put(productCode, productConstructionConfigurationMap);
        });
        context.setConstructionStageMap(constructionStageMap);
        context.setConstructionConfigurationMap(constructionConfigurationMap);
    }

    /**
     * 初始化最小批量
     *
     * @param context
     */
    protected void initMinimumLotSizeConfiguration(ProductionContext context) {
        Map<String, Long> productMinConfigurationMap = dataService.getMinimumLotSizeConfiguration(context);
        context.setMinimumLotSizeMap(productMinConfigurationMap);
    }

    /**
     * 设置品种拆A率
     * 根据分厂、品名，获取拆A率，并按物料编码分组
     *
     * @param context
     */
    protected void initProductALevel(ProductionContext context) {
        Map<String, ProductALevelVo> productDamageConfiguration = dataService.getProductDamageConfiguration(context.getFactoryCode(), context.getProductType().getValue());
        Map<String, BigDecimal> productDamageMap = context.getProductDamageMap();
        if (null == productDamageMap) {
            productDamageMap = new HashMap<>();
        }
        if (CollectionUtils.isEmpty(productDamageConfiguration)) {
            context.setProductDamageMap(productDamageMap);
            return;
        }
        Set<String> exportOemBrand = dataService.getExportOemBrand(context.getFactoryCode());
        for (Map.Entry<String, ProductALevelVo> entry : productDamageConfiguration.entrySet()) {
            ProductALevelVo productALevel = entry.getValue();
            //TODO 先暂时采用参数配置方式
            if (exportOemBrand.contains(productALevel.getBrand())) {
                BigDecimal oeeValue = context.getProductionParam().getExportOemBrandOee();
                if (null != oeeValue) {
                    productDamageMap.put(entry.getKey(), oeeValue);
                } else {
                    productDamageMap.put(entry.getKey(), BigDecimal.valueOf(1.96));
                }
            } else {
                productDamageMap.put(entry.getKey(), productALevel.getALevel());
            }
        }
        context.setProductDamageMap(productDamageMap);
    }

    /**
     * 初始化物料的利润等级值
     *
     * @param context
     */
    protected void initProfitInfo(ProductionContext context) {
        //获取利率优先等级配置
        List<MdmInterestRate> interestRateList = dataService.getInterestRateConfiguration();
        //获取物料毛利率配置
        Map<String, ProductBaseInfoVo> productInfoMap = context.getProductInfoMap();
        Map<String, Integer> productLocationProfitGradeMap = new HashMap<>();
        if (CollectionUtils.isEmpty(productInfoMap)) {
            context.setProductLocationProfitGradeMap(productLocationProfitGradeMap);
            return;
        }
        //物料配置
        productInfoMap.entrySet().stream().forEach(entry -> {
            ProductBaseInfoVo productBaseInfo = entry.getValue();
            ProductCommonTypeEnum commonType = ProductCommonTypeEnum.getInstance(productBaseInfo.getCommonType());
            if (null == commonType) {
                return;
            }
            String productCode = productBaseInfo.getProductCode();
            List<MaterialInfoGrossRateJsonVo> rateList = productBaseInfo.getRateList();
            ProductUtils.setProductLocationProfit(productLocationProfitGradeMap, rateList, productCode, commonType, interestRateList);
        });
        context.setProductLocationProfitGradeMap(productLocationProfitGradeMap);
    }

    /**
     * 初始化排产分组信息
     *
     * @param context 排产上下文
     */
    protected void initProductionGroupInfo(ProductionContext context) {
        List<ProductionGroupVo> productionGroupData = dataService.getFactoryProductionGroupConfiguration(context.getFactoryCode());
        if (CollectionUtils.isEmpty(productionGroupData)) {
            return;
        }
        Integer groupValue = BigDecimal.ONE.intValue();
        String groupKeyFormat = "TV-%s";
        Map<String, ProductionGroupInfoDto> productionGroupInfoMap = new HashMap<>();
        for (ProductionGroupVo productionGroup : productionGroupData) {
            Integer groupCount = productionGroup.getGroupCount();
            for (Integer index = BigDecimal.ZERO.intValue(); index < groupCount; index++) {
                String productionGroupValue = String.format(groupKeyFormat, groupValue);
                ProductionGroupInfoDto groupInfo = ProductionGroupUtils.createEmptyProductionGroup(context, productionGroupValue, productionGroup.getMouldNumber());
                productionGroupInfoMap.put(groupInfo.getProductionGroupValue(), groupInfo);
                groupValue = groupValue + BigDecimal.ONE.intValue();
            }
        }
        context.setProductionGroupInfoMap(productionGroupInfoMap);
    }

    /**
     * 初始化模具基础信息
     * 月度模具可用列表
     * 包含，模具的可排产日列表和不可排产日列表
     * 模具关联的物料个数，模具总硫化时间
     *
     * @param context
     */
    protected void initMouldBaseInfo(ProductionContext context) {
        //月度总可用模具信息
        Map<String, MouldInfoVO> enableMap = new HashMap<>();
        //根据制造需求计划得到模具月度可用且本身可用列表--月度可用
        List<MouldInfoVO> monthEnableList = dataService.getMonthEnableMouldConfiguration(context);
        if (!CollectionUtils.isEmpty(monthEnableList)) {
            monthEnableList.stream().forEach(monthEnable -> enableMap.put(monthEnable.getMouldCode(), monthEnable));
        }
        //根据制造需求计划得到模具维修返厂列表--月度配置有覆盖月度配置，没有则加入，防止月度配错
        List<MouldInfoVO> maintenanceEnableList = dataService.getMouldMaintenanceConfiguration(context);
        if (!CollectionUtils.isEmpty(maintenanceEnableList)) {
            maintenanceEnableList.stream().forEach(returnEnable -> enableMap.put(returnEnable.getMouldCode(), returnEnable));
        }
        List<MouldInfoVO> mouldInfoList = enableMap.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(mouldInfoList)) {
            context.setMouldInfoMap(Collections.emptyMap());
            context.setSameMouldMap(Collections.emptyMap());
            context.setMouldRelationProductMap(Collections.emptyMap());
            context.setProductRelationMouldMap(Collections.emptyMap());
            context.setProductRelationSpecCodeMouldMap(Collections.emptyMap());
            return;
        }
        //物料配置的模具列表
        List<ProductMouldConfigurationVo> productMouldConfigurationList = dataService.getProductionMouldInfoConfiguration(context);
        initProductMouldRelationInfo(context, productMouldConfigurationList, enableMap.keySet());
        //获取模具共用的物料数
        Map<String, Long> mouldCodeGroupVosMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(productMouldConfigurationList)) {
            mouldCodeGroupVosMap = productMouldConfigurationList.stream().collect(Collectors.groupingBy(ProductMouldConfigurationVo::getMouldCode, Collectors.counting()));
        }
        //为了流式写法，需要一次性赋值
        Map<String, Long> finalMouldCodeGroupVosMap = mouldCodeGroupVosMap;
        Map<String, MouldInfoVO> mouldInfoMap = mouldInfoList.stream().map(mouldInfo -> {
            //默认不是续作模具
            mouldInfo.setIsContinue(YesOrNoEnum.NO.getValue());
            Long assocaiationCount = finalMouldCodeGroupVosMap.get(mouldInfo.getMouldCode());
            if (null == assocaiationCount) {
                assocaiationCount = BigDecimal.ZERO.longValue();
            }
            mouldInfo.setAssocaiationCount(assocaiationCount.intValue());
            return mouldInfo;
        }).distinct().collect(Collectors.toMap(MouldInfoVO::getMouldCode, Function.identity()));
        context.setMouldInfoMap(mouldInfoMap);
        //按模具大类分组
        context.setSameMouldMap(context.getMouldInfoMap().values().stream().collect(Collectors.groupingBy(MouldInfoVO::getMouldClass, Collectors.toList())));
    }

    /**
     * 初始化续作信息
     * 包含续作规格以及对应的模具号
     *
     * @param context
     */
    protected void initContinueInfo(ProductionContext context) {
        List<MouldProductionProductVo> continueList = dataService.getContinueProductAndMould(context);
        if (CollectionUtils.isEmpty(continueList)) {
            context.setContinueProductMap(Collections.emptyMap());
            return;
        }
        Map<String, List<MouldProductionProductVo>> continueProductionGroup = continueList.stream().collect(Collectors.groupingBy(MouldProductionProductVo::getProductionGroupValue));
        //续作分组排产信息
        Map<String, ContinueProductionGroupVo> continueProductionGroupMap = ProductionGroupUtils.buildContinueProductionGroupInfo(continueProductionGroup);
        context.setContinueProductionGroupMap(continueProductionGroupMap);
        //续作规格信息
        Map<String, List<MouldProductionProductVo>> groupMap = continueList.stream().collect(Collectors.groupingBy(MouldProductionProductVo::getProductCode));
        context.setContinueProductMap(groupMap);
        //续作模具信息
        Map<String, MouldInfoVO> mouldInfoMap = context.getMouldInfoMap();
        if (CollectionUtils.isEmpty(mouldInfoMap)) {
            return;
        }
        continueList.stream().forEach(mouldProductionProduct -> {
            String mouldCode = mouldProductionProduct.getMouldCode();
            if (StringUtils.isBlank(mouldCode)) {
                return;
            }
            MouldInfoVO hasMould = mouldInfoMap.get(mouldCode);
            if (null == hasMould) {
                return;
            }
            hasMould.setIsContinue(YesOrNoEnum.YES.getValue());
            hasMould.setContinueProductCode(mouldProductionProduct.getProductCode());
            hasMould.setContinueProductionGroupValue(mouldProductionProduct.getProductionGroupValue());
            hasMould.setContinueMouldNumber(mouldProductionProduct.getMouldNumber());
            hasMould.setContinueMouldQty(mouldProductionProduct.getMouldQty());
        });
    }

    /**
     * 初始化需满月排产的续作规格集合信息
     *
     * @param productionContext
     */
    protected void initContinueFullMonthProductInfo(ProductionContext productionContext) {
        ProductionParamConfiguration productionParam = productionContext.getProductionParam();
        String openValue = productionParam.getIsOpenContinueFullMonthProduction();
        //没有开启
        if (!ProductionConstant.YES_VALUE.equalsIgnoreCase(openValue)) {
            productionContext.setContinueFullMonthProductionSet(Collections.emptySet());
            return;
        }
        Integer averageValue = productionParam.getFullMonthProductionQty();
        if (null == averageValue && averageValue <= BigDecimal.ZERO.intValue()) {
            productionContext.setContinueFullMonthProductionSet(Collections.emptySet());
            return;
        }
        Set<String> productSet = dataService.getGreaterAverageValueProductInfo(productionContext.getFactoryCode(), productionContext.getYear(), productionContext.getMonth(), averageValue);
        productionContext.setContinueFullMonthProductionSet(productSet);
    }

    /**
     * 初始化产能控制配置
     *
     * @param productionContext
     */
    protected void initCapacityConfiguration(ProductionContext productionContext) {
        initTireCapacityConfiguration(productionContext);
        initSizeCapacityConfiguration(productionContext);
    }

    /**
     * 初始化产能控制配置
     *
     * @param productionContext
     */
    protected void initSizeCapacityConfiguration(ProductionContext productionContext) {
        String factoryCode = productionContext.getFactoryCode();
        Integer year = productionContext.getYear();
        Integer month = productionContext.getMonth();
        List<SizeCapacityConfiguration> sizeCapacityConfigurationList = dataService.getSizeCapacityConfiguration(factoryCode, year, month);
        //月份工作天数
        Integer monthWorkDays = productionContext.getMonthWorkDays();
        Integer monthDays = productionContext.getMonthDays();
        Map<String, Long> sizeMonthCapacityMap = new HashMap<>();
        Map<Integer, Map<String, Long>> daySizeCapacityMap = new HashMap<>();
        Map<Integer, Map<String, Integer>> dayMaxMouldQtyMap = new HashMap<>();
        Map<Integer, Long> dayMaxCapacityMap = productionContext.getDayMaxCapacityMap();
        if (CollectionUtils.isEmpty(sizeCapacityConfigurationList)) {
            productionContext.setSizeMonthCapacityMap(sizeMonthCapacityMap);
            productionContext.setDaySizeCapacityMap(daySizeCapacityMap);
            productionContext.setDayMaxMouldQtyMap(dayMaxMouldQtyMap);
            return;
        }
        List<DaySizeCapacityVo> treeList = SizeCapacityUtils.buildTree(sizeCapacityConfigurationList);
        treeList.stream().forEach(daySizeCapacity -> {
            CapacityControlUtils.buildSizeCapacityControlInfo(productionContext, daySizeCapacity, BigDecimal.ZERO.intValue(), sizeMonthCapacityMap, daySizeCapacityMap, monthWorkDays, monthDays, dayMaxMouldQtyMap);
        });
        daySizeCapacityMap.entrySet().stream().forEach(entry -> {
            Integer day = entry.getKey();
            Map<String, Long> sizeCapacityByDayMap = entry.getValue();
            if (CollectionUtils.isEmpty(sizeCapacityByDayMap)) {
                dayMaxCapacityMap.put(day, BigDecimal.ZERO.longValue());
                return;
            }
            Long sumQty = BigDecimal.ZERO.longValue();
            for (Map.Entry<String, Long> sizeCapacityEntry : sizeCapacityByDayMap.entrySet()) {
                sumQty = sumQty + sizeCapacityEntry.getValue();
            }
            Long dayLimitQty = dayMaxCapacityMap.get(day);
            if (null == dayLimitQty) {
                dayMaxCapacityMap.put(day, sumQty);
                return;
            }
            if (dayLimitQty > sumQty) {
                dayMaxCapacityMap.put(day, sumQty);
            }
        });
        productionContext.setSizeMonthCapacityMap(sizeMonthCapacityMap);
        productionContext.setDaySizeCapacityMap(daySizeCapacityMap);
        productionContext.setDayMaxMouldQtyMap(dayMaxMouldQtyMap);
        productionContext.setDayMaxCapacityMap(dayMaxCapacityMap);
    }

    /**
     * 初始化轮胎类型产能分配
     *
     * @param productionContext
     */
    protected void initTireCapacityConfiguration(ProductionContext productionContext) {
        String factoryCode = productionContext.getFactoryCode();
        Integer year = productionContext.getYear();
        Integer month = productionContext.getMonth();
        List<TireCapacityConfiguration> tireCapacityConfigurationList = dataService.getTireCapacityConfiguration(factoryCode, year, month);
        Map<String, Long> tireCapacityMap = new HashMap<>();
        if (CollectionUtils.isEmpty(tireCapacityConfigurationList)) {
            productionContext.setTireCapacityMap(tireCapacityMap);
            return;
        }
        tireCapacityConfigurationList.stream().forEach(tireCapacityConfiguration -> {
            String groupKey = tireCapacityConfiguration.getGroupKey();
            tireCapacityMap.put(groupKey, Long.valueOf(tireCapacityConfiguration.getMonthCapacity()));
        });
        productionContext.setTireCapacityMap(tireCapacityMap);
    }

    /**
     * 根据续作信息，物料与施工关系
     * 物料与模具关系，设置排产计划的
     * 规格代号，胎胚代码及成形法
     * 如果是续作规格，则直接使用续作的规格代号和胎胚代码
     * 否则需根据物料与模具关系及施工关系，选一次法的规格代号和胎胚代码
     *
     * @param productionContext
     */
    protected void setProductionPlanInfo(ProductionContext productionContext) {
        Map<Long, MonthPlanManufacturingRequirementVo> monthPlanInitMap = productionContext.getMonthPlanInitMap();
        if (CollectionUtils.isEmpty(monthPlanInitMap)) {
            return;
        }
        Map<String, ProductProductionHelper> productionHelperMap = new HashMap<>();
        monthPlanInitMap.entrySet().stream().forEach(entry -> {
            MonthPlanManufacturingRequirementVo requirementPlan = entry.getValue();
            String productCode = requirementPlan.getProductCode();
            if (StringUtils.isBlank(productCode)) {
                return;
            }
            ProductProductionHelper helper = productionHelperMap.get(productCode);
            if (null == helper) {
                helper = ProductionPlanUtils.getProductProductionInfo(productCode, productionContext, requirementPlan.getProdReqPlan());
                productionHelperMap.put(productCode, helper);
            }
            ProductionPlanUtils.setCuringTime(productionContext, requirementPlan, helper.getProductConstructionInfo());
            String embryoCode = helper.getEmbryoCode();
            String constructionCode = helper.getConstructionCode();
            requirementPlan.setSpecCode(helper.getSpecCode());
            requirementPlan.setEmbryoCode(embryoCode);
            requirementPlan.setConstructionCode(constructionCode);
            ConstructionStageEnum stage = ConstructionStageEnum.matchByConstructionCode(constructionCode);
            requirementPlan.setConstructionStageType(stage);
            if (null != stage) {
                requirementPlan.setConstructionStage(stage.getStage());
            } else {
                requirementPlan.setConstructionStage(null);
            }
            //20250708 胎体布层级数
            if (StringUtils.isNotBlank(embryoCode)) {
                BaseConstructionVersionInfoVo constructionInfo = productionContext.getBaseConstructionInfoMap().get(embryoCode);
                if (null != constructionInfo && constructionInfo.getLayerLevelNumber() > BigDecimal.ONE.intValue()) {
                    requirementPlan.setOriginalTireFabricNumber(BigDecimal.ONE.intValue() + BigDecimal.ONE.intValue());
                } else {
                    requirementPlan.setOriginalTireFabricNumber(BigDecimal.ONE.intValue());
                }
            } else {
                requirementPlan.setOriginalTireFabricNumber(BigDecimal.ONE.intValue());
            }
            requirementPlan.setTireFabricNumber(requirementPlan.getOriginalTireFabricNumber());
            requirementPlan.setMouldMethod(helper.getMouldMethod());
            requirementPlan.setMouldClampingPressure(helper.getMouldClampingPressure());
            requirementPlan.setMoldCavity(helper.getMoldCavity());
            requirementPlan.setSpecCodeInfo(helper.getSpecCodeInfo());
        });
    }

    /**
     * 对共用模具进行产能预占计算
     *
     * @param productionContext 排产上下文
     * @param monthPlanList     排产计划集合
     */
    protected void calculatedPreemptMouldCapacity(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> monthPlanList) {
        if (CollectionUtils.isEmpty(monthPlanList)) {
            return;
        }
        Map<String, MouldInfoVO> allMouldMap = productionContext.getMouldInfoMap();
        if (CollectionUtils.isEmpty(allMouldMap)) {
            return;
        }
        //获取模具信息
        Map<String, Set<String>> mouldRelationProductMap = productionContext.getMouldRelationProductMap();
        if (CollectionUtils.isEmpty(mouldRelationProductMap)) {
            return;
        }
        //提取共用模具的物料编码集合
        Set<String> shareMouldProductCodeSet = new HashSet<>();
        mouldRelationProductMap.entrySet().stream().forEach(entry -> {
            Set<String> shareMouldSet = entry.getValue();
            if (CollectionUtils.isEmpty(shareMouldSet)) {
                return;
            }
            if (shareMouldSet.size() <= BigDecimal.ZERO.intValue()) {
                return;
            }
            shareMouldProductCodeSet.addAll(shareMouldSet);
        });
        if (CollectionUtils.isEmpty(shareMouldProductCodeSet)) {
            return;
        }
        //提取共用模具的计划
        List<MonthPlanManufacturingRequirementVo> shareMouldRequirePlanList = new ArrayList<>();
        monthPlanList.stream().forEach(monthPlan -> {
            //20250606 标记不排产的剔除，因寸口、轮胎类型产能限制提前标记
            if (YesOrNoEnum.NO.getValue().equals(monthPlan.getIsProduction())) {
                return;
            }
            String productCode = monthPlan.getProductCode();
            if (!shareMouldProductCodeSet.contains(productCode)) {
                return;
            }
            if (!monthPlan.isEffectivePlan()) {
                return;
            }
            //续作满月排产规格，则跳过
            if (productionContext.getContinueFullMonthProductionSet().contains(productCode)) {
                return;
            }
            shareMouldRequirePlanList.add(monthPlan);
        });
        //先续作计划销售需求预占续作模具
        List<MonthPlanManufacturingRequirementVo> continueSaleRequirementPlanList = ProductUtils.getContinueSaleRequirementPlan(shareMouldRequirePlanList);
        MouldUtils.continueSaleRequirePreemptCapacity(continueSaleRequirementPlanList, productionContext);
        //所有计划再次预占所有模具
        MouldBaseUtils.generalPreemptCapacity(shareMouldRequirePlanList, productionContext);
    }

    /**
     * 保存模具产能预占信息
     *
     * @param productionContext 排产上下文
     * @param monthPlanList     排产计划信息
     */
    protected void saveMouldPreCapacity(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> monthPlanList) {
        if (CollectionUtils.isEmpty(monthPlanList)) {
            return;
        }
        //保存(模具)预占产能分配
        getDataService().saveMouldPreCapacity(productionContext, monthPlanList);
    }

    /**
     * 设置生产版本号，如果生产版本号已经有则不进行生成
     * 否则需要重新生成生产版本号
     *
     * @param productionContext
     */
    protected void setProductionVersion(ProductionContext productionContext) {
        String productionVersion = productionContext.getProductionVersion();
        if (StringUtils.isNotBlank(productionVersion)) {
            return;
        }
        String prefix = productionContext.getPrefixVersion();
        if (StringUtils.isBlank(prefix)) {
            prefix = "";
        }
        productionVersion = prefix + DateUtils.dateTimeNow();
        productionContext.setProductionVersion(productionVersion);
        //分厂排程版本更新或是插入记录
        FactoryProductionVersion factoryProductionVersion = dataService.getFactoryMonthPlanVersion(productionContext);
        if (null != factoryProductionVersion) {
            //20250519 ZLT 设置月份排产模式自然月或非自然月及开始、结束排产日期
            setProductionVersionCycleInfo(factoryProductionVersion, productionVersion, productionContext);
            getDataService().updateFactoryProductionVersion(factoryProductionVersion);
            return;
        }
        factoryProductionVersion = new FactoryProductionVersion();
        factoryProductionVersion.setFactoryCode(productionContext.getFactoryCode());
        factoryProductionVersion.setYear(productionContext.getYear());
        factoryProductionVersion.setMonth(productionContext.getMonth());
        factoryProductionVersion.setMonthPlanVersion(productionContext.getMonthPlanVersion());
        factoryProductionVersion.setProductTypeCode(productionContext.getProductType().getValue());
        factoryProductionVersion.setIsFinal(YesOrNoEnum.NO.getValue());
        //20250519 ZLT 设置月份排产模式自然月或非自然月及开始、结束排产日期
        setProductionVersionCycleInfo(factoryProductionVersion, productionVersion, productionContext);
        getDataService().addFactoryProductionVersion(factoryProductionVersion);
    }

    /**
     * 标记计划是否继作
     *
     * @param monthPlanInitList
     * @param context
     */
    protected void markIsContinue(List<MonthPlanManufacturingRequirementVo> monthPlanInitList, ProductionContext context) {
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            return;
        }
        Map<String, List<MouldProductionProductVo>> continueMap = context.getContinueProductMap();
        if (CollectionUtils.isEmpty(continueMap)) {
            monthPlanInitList.stream().forEach(monthPlanInit -> monthPlanInit.setIsContinue(Constant.FALSE));
            return;
        }
        monthPlanInitList.stream().forEach(monthPlanInit -> {
            monthPlanInit.setIsContinue(Constant.FALSE);
            if (!CollectionUtils.isEmpty(continueMap.get(monthPlanInit.getProductCode()))) {
                monthPlanInit.setIsContinue(Constant.TRUE);
            }
        });
    }

    /**
     * 特殊轮胎类型产能控制
     *
     * @param productionContext 排产上下文
     * @param monthPlanInitList 排产计划
     */
    protected void tireTypeCapacityControl(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> monthPlanInitList) {
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            return;
        }
        //先按SAP分组，小于最小批量的不进行排产
        ProductionPlanUtils.rejectFallShortOfMinQty(monthPlanInitList, productionContext, true);
        //进行轮胎类型产能控制
        tireCapacityControl(monthPlanInitList, productionContext);
    }

    /**
     * 标记计划是否不排产
     * 根据分厂的不排产配置
     *
     * @param monthPlanInitList
     * @param context
     */
    protected void markFactoryNoProduction(List<MonthPlanManufacturingRequirementVo> monthPlanInitList, ProductionContext context) {
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            return;
        }
        Map<String, FactoryNoProduction> factoryNoProductionMap = dataService.getFactoryNoProductionConfiguration(context.getFactoryCode(), context.getYear(), context.getMonth());
        context.setFactoryNoProductionMap(factoryNoProductionMap);
        for (MonthPlanManufacturingRequirementVo monthPlanInitVO : monthPlanInitList) {
            FactoryNoProduction noProduction = factoryNoProductionMap.get(monthPlanInitVO.getProductCode());
            if (null == noProduction) {
                monthPlanInitVO.setIsFactoryProduction(Constant.FALSE);
            } else {
                monthPlanInitVO.setIsFactoryProduction(Constant.TRUE);
            }
        }
    }

    /**
     * 产能控制计算
     *
     * @param monthPlanInitList
     * @param productionContext
     */
    protected void capacityControl(List<MonthPlanManufacturingRequirementVo> monthPlanInitList, ProductionContext productionContext) {
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            return;
        }
        //剔除不可排计划
        List<MonthPlanManufacturingRequirementVo> effectivePlanList = monthPlanInitList.stream().filter(monthPlan -> monthPlan.isCapacityControlPlan()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectivePlanList)) {
            return;
        }
        //挑选只有一次法
        List<MonthPlanManufacturingRequirementVo> oneMouldMethodList = effectivePlanList.stream().filter(productionPlan -> productionPlan.isOnlyFirstMethod()).collect(Collectors.toList());
        //可一次法也可二次法
        List<MonthPlanManufacturingRequirementVo> hasChangeMouldMethodList = effectivePlanList.stream().filter(productionPlan -> productionPlan.hasChangeSpecCode()).collect(Collectors.toList());
        Map<String, Long> sizeMonthCapacityMap = productionContext.getSizeMonthCapacityMap();
        //一次法还有剩余产能的寸口
        Map<String, Long> occupiedMap = new HashMap<>();
        //一次法已经满产能的寸口
        Set<String> needChangeSet = new HashSet<>();
        //先仅能一次法成型的产能控制
        CapacityControlUtils.onlyOneMethodCapacityControl(productionContext, oneMouldMethodList, sizeMonthCapacityMap, occupiedMap, needChangeSet);
        //转换成型法
        CapacityControlUtils.oneMethodChangeMethod(hasChangeMouldMethodList, needChangeSet, occupiedMap, sizeMonthCapacityMap);
        //获取二次法
        List<MonthPlanManufacturingRequirementVo> twoMouldMethodList = effectivePlanList.stream().filter(effectivePlan -> FormingMethodTypeEnum.TWO_STAGE_TIRE.getMethodValue().equals(effectivePlan.getMouldMethod())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(twoMouldMethodList)) {
            return;
        }
        CapacityControlUtils.capacityAllocationByTireFabric(twoMouldMethodList, productionContext);
//        Map<String, List<MonthPlanManufacturingRequirementVo>> sizeCapacityMap = twoMouldMethodList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getSizeCapacityGroupKey));
//        sizeCapacityMap.entrySet().stream().forEach(sizeCapacityEntry -> {
//            String sizeGroupKey = sizeCapacityEntry.getKey();
//            CapacityControlUtils.sizeCapacityLimit(sizeGroupKey, sizeMonthCapacityMap, sizeCapacityEntry.getValue(), productionContext);
//        });
//        Map<String, List<MonthPlanManufacturingRequirementVo>> productGroupPlanMap = effectivePlanList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getProductCode));
//        //按排产优先级排序
//        effectivePlanList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
//
//        Map<String, Long> planSizeProductionMap = new HashMap<>();
//        effectivePlanList.stream().forEach(effectivePlan -> {
//            Long productionQty = effectivePlan.getProductionQty();
//            if (productionQty <= BigDecimal.ZERO.longValue()) {
//                return;
//            }
//            //只有一个成型法，不可转换
//            if (!effectivePlan.hasChangeSpecCode()) {
//                CapacityControlUtils.getRealProductionQtyBySizeLimit(sizeMonthCapacityMap, planSizeProductionMap, effectivePlan, productionQty);
//                return;
//            }
//            String productCode = effectivePlan.getProductCode();
//            List<MonthPlanManufacturingRequirementVo> allProductCodePlanList = productGroupPlanMap.get(productCode);
//            String mouldMethod = effectivePlan.getMouldMethod();
//            Long sumProductionQty = allProductCodePlanList.stream().mapToLong(MonthPlanManufacturingRequirementVo::getProductionQty).sum();
//            if (CapacityControlUtils.isChangeMouldMethod(effectivePlan, sizeMonthCapacityMap, planSizeProductionMap, sumProductionQty)) {
//                //转换成型法
//                FormingMethodTypeEnum change = FormingMethodTypeEnum.getChangeType(mouldMethod);
//                allProductCodePlanList.stream().forEach(changeSpecCodePlan -> changeSpecCodePlan.changeSpecCode(change));
//            }
//            CapacityControlUtils.getRealProductionQtyBySizeLimit(sizeMonthCapacityMap, planSizeProductionMap, effectivePlan, productionQty);
//        });
    }

    /**
     * 设置排产顺序前的规格汇总备货量
     *
     * @param monthPlanInitList
     */
    protected void summaryProductCodeStockUpQty(List<MonthPlanManufacturingRequirementVo> monthPlanInitList) {
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            return;
        }
        //按规格分组
        Map<String, List<MonthPlanManufacturingRequirementVo>> productCodeGroupMap = monthPlanInitList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getProductCode));
        productCodeGroupMap.entrySet().stream().forEach(productCodeGroupEntry -> {
            List<MonthPlanManufacturingRequirementVo> groupList = productCodeGroupEntry.getValue();
            if (CollectionUtils.isEmpty(groupList)) {
                return;
            }
            List<MonthPlanManufacturingRequirementVo> stockUpDemandList = groupList.stream().filter(requirementPlan -> YesOrNoEnum.YES.getValue().equals(requirementPlan.getIsStockUp())).collect(Collectors.toList());
            Long stockUpDemandQty = BigDecimal.ZERO.longValue();
            if (!CollectionUtils.isEmpty(stockUpDemandList)) {
                stockUpDemandQty = stockUpDemandList.stream().collect(Collectors.summingLong(MonthPlanManufacturingRequirementVo::getProductionQty));
            }
            Long originalStockUpQty = stockUpDemandQty;
            groupList.stream().forEach(productionPlan -> productionPlan.setSummaryStockUpDemandQty(originalStockUpQty));
        });
    }

    /**
     * 汇总规格的总的可排产量
     *
     * @param monthPlanInitList 排产计划集合
     */
    protected void summaryProductCodeProductionQty(List<MonthPlanManufacturingRequirementVo> monthPlanInitList) {
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            return;
        }
        Map<String, List<MonthPlanManufacturingRequirementVo>> productCodeGroupMap = monthPlanInitList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getProductCode));
        productCodeGroupMap.entrySet().stream().forEach(productCodeGroupEntry -> {
            List<MonthPlanManufacturingRequirementVo> groupList = productCodeGroupEntry.getValue();
            if (CollectionUtils.isEmpty(groupList)) {
                return;
            }
            Long summaryProductionQty = groupList.stream().collect(Collectors.summingLong(MonthPlanManufacturingRequirementVo::getProductionQty));
            List<MonthPlanManufacturingRequirementVo> netDemandList = groupList.stream().filter(requirementPlan -> YesOrNoEnum.NO.getValue().equals(requirementPlan.getIsStockUp())).collect(Collectors.toList());
            Long netDemandQty = BigDecimal.ZERO.longValue();
            if (!CollectionUtils.isEmpty(netDemandList)) {
                netDemandQty = netDemandList.stream().collect(Collectors.summingLong(MonthPlanManufacturingRequirementVo::getProductionQty));
            }
            Long realNetDemandQty = netDemandQty;
            groupList.stream().forEach(productionPlan -> {
                productionPlan.setSummaryProductionQty(summaryProductionQty);
                productionPlan.setSummaryNetDemandQty(realNetDemandQty);
            });
        });
    }

    /**
     * 轮胎产能控制计算
     *
     * @param monthPlanInitList 需求计划
     * @param productionContext 排产上下文
     */
    protected void tireCapacityControl(List<MonthPlanManufacturingRequirementVo> monthPlanInitList, ProductionContext productionContext) {
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            return;
        }
        Map<String, Long> tireCapacityMap = productionContext.getTireCapacityMap();
        if (CollectionUtils.isEmpty(tireCapacityMap)) {
            return;
        }
        //剔除不可排计划
        List<MonthPlanManufacturingRequirementVo> effectivePlanList = monthPlanInitList.stream().filter(monthPlan -> monthPlan.isCapacityControlPlan()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectivePlanList)) {
            return;
        }
        //分组--轮胎类型+寸口
        Map<String, List<MonthPlanManufacturingRequirementVo>> tireRequireMap = effectivePlanList.stream().collect(Collectors.groupingBy(MonthPlanManufacturingRequirementVo::getTireCapacityGroupKey));
        //根据轮胎类型寸口限制，剔除优先级低的需求量
        tireCapacityMap.entrySet().stream().forEach(tireCapacityEntry -> CapacityControlUtils.tireCapacityLimit(tireCapacityEntry, tireRequireMap, productionContext));
    }

    /**
     * 初始化 模具产能，并根据模具满产能调整可排产量
     * 获取物料可用模具配置信息-通过物料与模具关系、模具状态、分厂月模块状态获取
     * 可用模具数，以及根据最大模具产能看模具产能是否满足
     *
     * @param monthPlanInitList
     * @param context
     */
    protected void initMouldCapacity(List<MonthPlanManufacturingRequirementVo> monthPlanInitList, ProductionContext context) {
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            return;
        }
        List<ProductMouldInfoVO> productMouldInfoList = getDataService().getEnableUseProductMouldConfiguration(context);
        if (CollectionUtils.isEmpty(productMouldInfoList)) {
            return;
        }
        BigDecimal addCuringTime = BigDecimal.ZERO;
        Integer addCuringTimeParam = (Integer) context.getFactoryParams().get(FactoryConstant.SYS_PARAM_INTERVAL_TIME_OF_EMBRYO_EXCHANGE);
        if (null == addCuringTimeParam) {
            addCuringTime = BigDecimal.valueOf(addCuringTimeParam);
        }
        //按物料分组，得到其可用模具数
        Map<String, ProductMouldInfoVO> productMouldMap = productMouldInfoList.stream().collect(Collectors.toMap(ProductMouldInfoVO::getProductCode, Function.identity()));
        //设置模具可用数量及预估模具最大排产量
        for (MonthPlanManufacturingRequirementVo monthPlanInit : monthPlanInitList) {
            //20250606 标记不排产的剔除，因寸口、轮胎类型产能限制提前标记
            if (YesOrNoEnum.NO.getValue().equals(monthPlanInit.getIsProduction())) {
                continue;
            }
            String productCode = monthPlanInit.getProductCode();
            ProductMouldInfoVO productMouldInfo = productMouldMap.get(productCode);
            if (null == productMouldInfo) {
                monthPlanInit.setMouldQty(BigDecimal.ZERO.intValue());
                continue;
            }
            List<MouldInfoVO> mouldInfoList = productMouldInfo.getMouldInfoList();
            if (CollectionUtils.isEmpty(mouldInfoList)) {
                monthPlanInit.setMouldQty(BigDecimal.ZERO.intValue());
                continue;
            }
            //可用模具数据
            Set<String> mouldNoSet = mouldInfoList.stream().map(MouldInfoVO::getMouldNo).collect(Collectors.toSet());
            monthPlanInit.setMouldNoInfo(String.join(StringConstant.COMMA, new ArrayList<>(mouldNoSet)));
            monthPlanInit.setMouldQty(mouldInfoList.size());
            if (null == monthPlanInit.getCuringTime()) {
                continue;
            }
            /*
             * 计算模具满产产能
             * 物料排产硫化时间 = 物料单条硫化时间 + 单条硫化增加时间;
             * 模具日产能 = 硫化机1天工时上限（小时） * 3600 / 物料排产硫化时间;
             * 单模月产能 = 模具可排产日 * 模具日产能;
             * 模具满产产量 = SUM(可用模具月产能)
             */
            BigDecimal fullMouldQty = BigDecimal.ZERO;
            for (MouldInfoVO mouldInfo : mouldInfoList) {
                Map<Integer, BigDecimal> productionDayList = mouldInfo.getProductionDayList();
                if (CollectionUtils.isEmpty(productionDayList)) {
                    continue;
                }
                //到秒
                BigDecimal curingTime = monthPlanInit.getCuringTime().add(addCuringTime);
                for (Map.Entry<Integer, BigDecimal> entry : productionDayList.entrySet()) {
                    BigDecimal dayQty = SafeCompute.div(entry.getValue(), curingTime, 0, BigDecimal.ROUND_DOWN);
                    fullMouldQty = fullMouldQty.add(dayQty);
                }
            }
            monthPlanInit.setMouldFullQty(fullMouldQty.longValue());
            if (monthPlanInit.getFactProdReqQty() <= monthPlanInit.getMouldFullQty()) {
                continue;
            }
            //可排产量大于模具满产能-则修改为模具满产能
            monthPlanInit.setProductionQty(monthPlanInit.getMouldFullQty());
            //扣除超出模具产能数:%1$s.
            Long noProductionQty = monthPlanInit.getFactProdReqQty() - monthPlanInit.getMouldFullQty();
            String noProductionReason = NoProductionReasonUtils.getOverModCaps(noProductionQty);
            monthPlanInit.addNoProductionReasonAndQty(noProductionReason, noProductionQty);
        }
    }

    /**
     * 根据分组信息，更新初始化列表的计划排产顺序信息
     * 初始化计划一定会有ID
     *
     * @param groupData
     */
    protected void updateProductionSequence(List<ProductionPlanGroupVo> groupData) {
        if (CollectionUtils.isEmpty(groupData)) {
            return;
        }
        Map<Long, MonthPlanManufacturingRequirementVo> monthPlanInitMap = new HashMap<>();
        groupData.stream().forEach(productionPlanGroup -> {
            List<MonthPlanManufacturingRequirementVo> productionSequenceList = productionPlanGroup.getGroupPlanList();
            if (CollectionUtils.isEmpty(productionSequenceList)) {
                return;
            }
            productionSequenceList.stream().forEach(productionSequence -> monthPlanInitMap.put(productionSequence.getMonthPlanId(), productionSequence));
        });
        if (CollectionUtils.isEmpty(monthPlanInitMap)) {
            return;
        }
        List<MonthPlanManufacturingRequirementVo> productionSequenceList = monthPlanInitMap.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
        dataService.updateProductionSequence(productionSequenceList);
    }

    /**
     * 单模具排产(两种情形)
     * 一种：物料可用模具只有一副；另外一种则物料可用模具大于1，但为单数，排到最后一副模具
     * <p>
     * 取得计划需要排产量，物料的硫化时间，配置的单条间隔硫化时间
     * 从起始日一直排到截止日，直到可排产量 = 0 或是 模具产能不足为止
     *
     * @param mouldInfo         排产模具
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param needProductionQty 需要排产量
     */
    protected void singleMouldProduction(SingleMouldSelectedMouldTableHelper selectedMouldTable, MouldInfoVO mouldInfo, ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, Long needProductionQty) {
        Long monthPlanId = productionPlan.getMonthPlanId();
        MonthPlanManufacturingRequirementVo originalPlan = productionContext.getMonthPlanInitMap().get(monthPlanId);
        String sizeCapacityKey = productionPlan.getSizeCapacityGroupKey();
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(productionPlan, productionContext);
        //获取模具当前排产日
        Integer mouldStartProductionDate = mouldInfo.getBeginDay();
        //获取模具排产截止日
        Integer mouldEndDate = mouldInfo.getEndDay();
        //模具排产方向
        ProductionOrientEnum productionOrient = mouldInfo.getProductionOrient();
        List<MouldInfoVO> productionMouldList = new ArrayList<>();
        productionMouldList.add(mouldInfo);
        //移动规格位置，pancd+ 2025.03.28
//        moveProductPosition(mouldInfo, productionContext);
        //20250719 ZLT 获取排产分组信息
        Integer productionGroupStartDate = null;
        Integer productionGroupEndDate = null;
        MouldTableInfoDto selectedMouldTableInfo = null;
        if (null != selectedMouldTable) {
            selectedMouldTableInfo = selectedMouldTable.getSelectedMouldTableInfo();
            productionGroupStartDate = selectedMouldTable.getProductionGroupStartDate();
            productionGroupEndDate = selectedMouldTable.getProductionGroupEndDate();
        }
        //排产起始日 取衔接分组的起始日
        Integer startProductionDate;
        if (null == productionGroupStartDate) {
            startProductionDate = mouldStartProductionDate;
        } else {
            startProductionDate = Math.max(productionGroupStartDate, mouldStartProductionDate);
        }
        //排产截止日
        Integer endDate;
        if (null == productionGroupEndDate) {
            endDate = mouldEndDate;
        } else {
            endDate = Math.min(productionGroupEndDate, mouldEndDate);
        }
        String mouldCode = mouldInfo.getMouldCode();
        String mouldCodeInfo = String.format("[%s]", mouldCode);
        String productCode = productionPlan.getProductCode();
        String dateRange = String.format("[%d]-[%d]", mouldInfo.getBeginDay(), endDate);
        //排产流程日志打印及保存记录 [%d]计划使用[%s]模具[%s]排产,从[%d]-[%d]日进行排产，需排产量[%d]
        ProductionLogUtils.addStartSingleMouldProductionLog(productionContext, productionPlan, mouldCodeInfo, productionOrient, dateRange, needProductionQty);
        //开始单模排产
        ProductionInfoVo finalProductionInfo = null;
        for (; MouldUtils.isDateProduction(startProductionDate, endDate, productionOrient); ) {
            if (needProductionQty <= 0) {
                break;
            }
            //日开始排产：日志打印及保存记录 [%d]计划使用[%s]模具[%s]排产,在[%d]日还需排产量[%d]
            ProductionLogUtils.addBeforeMouldDayProductionLog(productionContext, MouldProductionLogType.SINGLE_PLAN_PRODUCTION_LOG, productionPlan, mouldCode, productionOrient, startProductionDate, needProductionQty);
            //得到下一个排产日
            Integer nextProductionDate = MouldUtils.getNextProductionDate(productionContext, startProductionDate, productionOrient);
            //20251013 ZLT 校验成型硫化配比控制
            ProductionLimitTypeEnum limitType = productionContext.isReachTheLimit(false, productionOrient, startProductionDate, productionMouldList.size(), productionPlan);
            if (ProductionLimitTypeEnum.DAY_MOULD_QTY_LIMIT == limitType) {
                ProductionLogUtils.addProductionMouldQtyLimitLog(productionContext, MouldProductionLogType.SINGLE_PLAN_PRODUCTION_LOG, productionPlan, startProductionDate);
                startProductionDate = nextProductionDate;
                continue;
            }
            //20250622 校验是否达到规格数(一个是达到天的总规格数限制，一个是达到天的新增规格数限制)
            if (!productionContext.isAddProduct(false, productionOrient, startProductionDate, productCode, productionPlan)) {
                if (!productionContext.getFactoryStopDays().contains(startProductionDate)) {
                    ProductionLogUtils.addProductionProductNumberLimitLog(productionContext, MouldProductionLogType.SINGLE_PLAN_PRODUCTION_LOG, productionPlan, startProductionDate);
                }
                startProductionDate = nextProductionDate;
                continue;
            }
            //得到单日排产信息 20250903 续作排产标记
            DayProductionPlanInfoVo dayProductionPlanInfo = new DayProductionPlanInfoVo(monthPlanId, productCode, sizeCapacityKey, startProductionDate, needProductionQty, singleCuringTime, false);
            ProductionInfoVo productionInfo = MouldUtils.calculateProductionQty(mouldInfo, dayProductionPlanInfo, productionContext, false);
            Long productionQty = productionInfo.getProductionQty();
            //日排产结果：日志打印和保存记录 [%d]计划使用[%s]模具[%s]排产,在[%d]日排产量[%d]
            ProductionLogUtils.addProductionDateResultMouldProductionLog(productionContext, MouldProductionLogType.SINGLE_PLAN_PRODUCTION_LOG, productionPlan, mouldCodeInfo, productionOrient, startProductionDate, productionQty);
            if (productionQty < BigDecimal.ZERO.longValue()) {
                startProductionDate = nextProductionDate;
                continue;
            }
            //20250624 拼模排产--记录起始排产日
            if (productionQty > BigDecimal.ZERO.intValue() && productionContext.isAssemblingMouldProduction() && null == productionContext.getAssemblingMouldStartDay()) {
                productionContext.setAssemblingMouldStartDay(startProductionDate);
            }
            //剩余还需排产量
            Long leftOverNeedProductionQty = needProductionQty - productionQty;
            finalProductionInfo = productionInfo;
            //更新模具排产信息--模具排产列表等
            updateMouldDayProductionInfo(selectedMouldTableInfo, mouldInfo, productionPlan, productionInfo, productionContext, false);
            //20251011 ZLT 增加天排产模具数信息
            handlerProductionMouldQty(productionContext, productionOrient, productionPlan, startProductionDate, leftOverNeedProductionQty, productionMouldList, false);
            //更新需排产量
            needProductionQty = leftOverNeedProductionQty;
            //迭代排产日
            startProductionDate = nextProductionDate;
        }
        //单模排产结束：日志打印及保存记录 [%d]计划使用[%s]模具[%s]排产,从[%d]-[%d]日进行排产，还需要排产量[%d]
        ProductionLogUtils.addSingleMouldProductionResultLog(productionContext, productionPlan, mouldCodeInfo, productionOrient, dateRange, needProductionQty);
        //更新模具当前排产信息
        if (null != finalProductionInfo) {
            MouldUtils.setMouldCurrentProductionInfo(mouldInfo, finalProductionInfo, productionPlan.getProductCode(), productionContext);
        }
        originalPlan.setIsProduction(YesOrNoEnum.NO.getValue());
        setNoProductionReason(originalPlan, needProductionQty);
//        String noProductionReason = "";
//        if (needProductionQty > 0) {
//            //模具产能不足
//            noProductionReason = NoProductionReasonUtils.getMouldNotEnough();
//            if (Boolean.TRUE.equals(originalPlan.getIsCapacityLimit())) {
//                noProductionReason = NoProductionReasonUtils.getDayLimit();
//            }
//        }
//        originalPlan.addNoProductionReasonAndQty(noProductionReason, needProductionQty);
    }

    /**
     * 根据剩余还需排产量，设置不排产原因
     *
     * @param originalPlan      排产计划
     * @param needProductionQty 还需排产量
     */
    private void setNoProductionReason(MonthPlanManufacturingRequirementVo originalPlan, Long needProductionQty) {
        if (needProductionQty <= BigDecimal.ZERO.intValue()) {
            originalPlan.addNoProductionReasonAndQty("", needProductionQty);
            return;
        }
        //模具产能不足
        String noProductionReason = NoProductionReasonUtils.getMouldNotEnough();
        if (Boolean.TRUE.equals(originalPlan.getIsCapacityLimit())) {
            noProductionReason = NoProductionReasonUtils.getDayLimit();
        }
        originalPlan.addNoProductionReasonAndQty(noProductionReason, needProductionQty);
    }

    /**
     * 更新模具的日排产信息
     * 如果是洗模日则对模具增加不排产日信息中的洗模日
     * 如果排产量为零，则不进行更新
     * 否则，更新模具的使用硫化时间，剩余硫化时间、排产日剩余的硫化时间
     * 模具排产日的排产信息列表增加数据
     *
     * @param selectedTable        排产分组的模台信息对象
     * @param mouldInfo            模具信息
     * @param productionPlan       排产计划
     * @param productionInfo       排产信息
     * @param productionContext    排产上下文
     * @param isContinueProduction 是否续作模具排产
     */
    protected void updateMouldDayProductionInfo(MouldTableInfoDto selectedTable, MouldInfoVO mouldInfo, MonthPlanManufacturingRequirementVo productionPlan, ProductionInfoVo productionInfo, ProductionContext productionContext, boolean isContinueProduction) {
        //当前排产日
        Integer productionDate = productionInfo.getProductionDate();
        String productCode = productionPlan.getProductCode();
        //洗模日
        if (ProductionTypeEnum.MOULD_CLEANING_DAY == productionInfo.getProductionType()) {
            NoProductionDayMouldVo noProductionDayMould = new NoProductionDayMouldVo();
            noProductionDayMould.setDay(productionDate);
            noProductionDayMould.setNoProductionType(MouldNoProductionType.MOULD_CLEANING_DAY);
            //20250411 洗模日不可直接无产能，通过配置扣减产能，故而不能加入不可排产日
            mouldInfo.getCleanDayList().put(productionDate, noProductionDayMould);
            //洗模排产流程日志记录
            String clearDayMould = String.format("计划[%d]在[%d]日被判断为洗模日", productionPlan.getMonthPlanId(), productionDate);
            MouldProductionLog clearMouldDayLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_PRODUCTION_LOG, clearDayMould);
            saveProductionLog(productionContext, clearMouldDayLog);
        }
        //处理是否需要跨天减产能--换规格
        handlerNextDaySubtractLeftOverTime(mouldInfo, productionInfo, productionContext);
        String sizeCapacityKey = productionPlan.getSizeCapacityGroupKey();
        //使用硫化时间(包含换模、洗模、换规格时间)
        BigDecimal usedCuringTime = productionInfo.getUsedCuringTime();
        //20250414 增加每日最大排产量控制 增加每日排产量汇总--需要加入换模，洗模，换规格的消耗量
        if (BigDecimalUtils.safeCompare(usedCuringTime, BigDecimal.ZERO) > BigDecimal.ZERO.intValue()) {
            updateCapacityConsumeQty(productionContext, productionPlan, productionInfo, mouldInfo.getProductionOrient(), sizeCapacityKey);
        }
        Long productionQty = productionInfo.getProductionQty();
        //没有排产数量
        if (productionQty <= 0) {
            return;
        }
        //20250421 增加每日排产规格数
        productionContext.addDayProductNumber(mouldInfo.getProductionOrient(), productionDate, productCode, productionPlan, isContinueProduction);
        //模具日排产信息列表数据
        MouldDayProductionVo mouldDayProductionInfo = MouldBaseUtils.buildMouldDayProductionInfo(productionPlan, mouldInfo, null, productionInfo);
        //20250716 ZLT 放入模台排产信息列表中
        ProductionGroupUtils.addMouldTableProductionInfo(productionContext, selectedTable, productionDate, mouldDayProductionInfo);
        List<MouldDayProductionVo> dayProductionList = mouldInfo.getDayProductionMap().get(productionDate);
        if (null == dayProductionList) {
            dayProductionList = new ArrayList<>();
        }
        dayProductionList.add(mouldDayProductionInfo);
        mouldInfo.getDayProductionMap().put(productionDate, dayProductionList);
        //增加使用硫化时间、更新剩余硫化时间
        mouldInfo.setUsedSeconds(mouldInfo.getUsedSeconds().add(usedCuringTime));
        mouldInfo.setLeftOverSeconds(mouldInfo.getTotalSeconds().subtract(mouldInfo.getUsedSeconds()));
        //更新模具的排产日剩余硫化时间
        BigDecimal dayLeftOverSeconds = mouldInfo.getProductionDayList().get(productionDate);
        dayLeftOverSeconds = dayLeftOverSeconds.subtract(usedCuringTime);
        mouldInfo.getProductionDayList().put(productionDate, dayLeftOverSeconds);
        //剩余硫化时间小于单条硫化时间，则表示当前排产日已经排产完毕(同规格都不行，换规格更不行)
        setFinishDayAndFinishFlag(mouldInfo, dayLeftOverSeconds, productionDate, productionInfo.getSingleCuringTime(), productionContext);
    }

    /**
     * 移动规格位置
     * 用于解决反向排产出现间断问题
     *
     * @param mouldInfoVO       模具信息
     * @param productionContext 规格上下文
     */
    protected void moveProductPosition(MouldInfoVO mouldInfoVO, ProductionContext productionContext) {
        ProductionOrientEnum productionOrient = mouldInfoVO.getProductionOrient();
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            //正向排产，不存在移动的问题
            return;
        }
        //1. 获取模具日排产规格及日剩余硫化时间
        Map<Integer, List<MouldDayProductionVo>> dayProductionMap = mouldInfoVO.getDayProductionMap();
        if (PubUtil.isEmpty(dayProductionMap)) {
            return;
        }
        int startDay = 0;
        //月份最大天数
        int endDay = productionContext.getMonthDays();
        Map<Integer, BigDecimal> remainLhTimeMap = mouldInfoVO.getProductionDayList();
        for (int iDay = ProductionConstant.MONTH_START_DAY; iDay <= endDay; iDay++) {
            //获取间断起始日
            if (startDay == 0 && PubUtil.isEmpty(mouldInfoVO.getDayProductionMap().get(iDay)) &&
                    !MouldUtils.checkIsNoPlaningDay(iDay, mouldInfoVO)) {
                startDay = iDay;
            }
            //获取需要移动的规格及计划量合计
            Map<Long, MouldDayProductionVo> moveProductionMap = getNeedMoveProductionMapAndSubtractDayQty(iDay, mouldInfoVO, productionContext);
            if (PubUtil.isEmpty(moveProductionMap)) {
                //1.1 若不需要移动，继续
                continue;
            }

            for (Map.Entry<Long, MouldDayProductionVo> entry : moveProductionMap.entrySet()) {
                //1.2 按计划段移动
                Long monthPlanId = entry.getKey();
                MouldDayProductionVo moveProductionVo = entry.getValue();
                String productCode = moveProductionVo.getProductCode();
                Long needProductionQty = moveProductionVo.getProductionQty();
                MonthPlanManufacturingRequirementVo moveProductionPlan = productionContext.getMonthPlanInitMap().get(monthPlanId);
                String sizeCapacityKey = moveProductionPlan.getSizeCapacityGroupKey();
                BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(moveProductionPlan, productionContext);
                for (int i = startDay; i <= endDay; i++) {
                    if (needProductionQty <= 0) {
                        //当前规格段退出，换一个规格段
                        break;
                    }
                    //1.3 前1天存在剩余硫化时间，先补足 20250903 续作排产标识-false
                    if (i != ProductionConstant.MONTH_START_DAY && BigDecimalUtils.safeCompare(remainLhTimeMap.get(i - 1), BigDecimal.ZERO) > 0) {
                        DayProductionPlanInfoVo dayProductionPlanInfo = new DayProductionPlanInfoVo(monthPlanId, productCode, sizeCapacityKey, i - 1, needProductionQty, singleCuringTime, false);
                        ProductionInfoVo productionInfo = MouldUtils.calculateProductionQty(mouldInfoVO, dayProductionPlanInfo, productionContext, false);
                        needProductionQty = needProductionQty - productionInfo.getProductionQty();
                        //更新模具日排产信息
                        updateMouldDayProductionInfo(null, mouldInfoVO, moveProductionPlan, productionInfo, productionContext, false);
                    }
                    //1.4 先移除洗模日
                    removeCleaningDay(i, mouldInfoVO);
                    //1.5 补录当天 20250903 续作排产标识-false
                    DayProductionPlanInfoVo dayProductionPlanInfo = new DayProductionPlanInfoVo(monthPlanId, productCode, sizeCapacityKey, i, needProductionQty, singleCuringTime, false);
                    ProductionInfoVo productionInfo = MouldUtils.calculateProductionQty(mouldInfoVO, dayProductionPlanInfo, productionContext, false);
                    needProductionQty = needProductionQty - productionInfo.getProductionQty();
                    //更新模具日排产信息
                    updateMouldDayProductionInfo(null, mouldInfoVO, moveProductionPlan, productionInfo, productionContext, false);
                }
            }

            //2.正常只会有一个间断，匹配到，直接退出
            break;
        }
    }

    /**
     * 移动规格位置
     * 用于解决反向排产出现间断问题
     *
     * @param productionContext 排产上下文
     * @param first             第一个模具
     * @param second            第二个模具
     */
    protected void moveProductPosition(ProductionContext productionContext, MouldInfoVO first, MouldInfoVO second) {
        ProductionOrientEnum productionOrient = first.getProductionOrient();
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            //正向排产，不存在移动的问题
            return;
        }
        //1. 获取模具日排产规格及日剩余硫化时间
        Map<Integer, List<MouldDayProductionVo>> firstDayProductionMap = first.getDayProductionMap();
        Map<Integer, List<MouldDayProductionVo>> secondDayProductionMap = second.getDayProductionMap();
        if (CollectionUtils.isEmpty(firstDayProductionMap) && CollectionUtils.isEmpty(secondDayProductionMap)) {
            return;
        }
        String mouldCodeInfo = String.format("[%s]、[%s]", first.getMouldCode(), second.getMouldCode());
        //月份最大天数
        int endDay = productionContext.getMonthDays();
        int intervalStartDay = MouldUtils.getIntervalDay(productionContext, first, second);
        //从间断日开始移动
        for (int productionDate = intervalStartDay; productionDate <= endDay; productionDate++) {
            //获取需要移动的规格及计划量合计
            Map<Long, MouldDayProductionVo> mergeMoveProductionMap = getNeedMoveProductionMapAndSubtractDayQty(productionDate, productionContext, first, second);
            if (PubUtil.isEmpty(mergeMoveProductionMap)) {
                //1.1 若不需要移动，继续
                continue;
            }
            String moveProductFormat = "开始进行规格移动，从[%d]日开始使用%s模具移动";
            String moveProductContext = String.format(moveProductFormat, productionDate, mouldCodeInfo);
            log.info(moveProductContext);
            MouldProductionLog moveProductLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, moveProductContext);
            saveProductionLog(productionContext, moveProductLog);

            for (Map.Entry<Long, MouldDayProductionVo> entry : mergeMoveProductionMap.entrySet()) {
                //1.2 按计划段移动
                Long monthPlanId = entry.getKey();
                MouldDayProductionVo moveProductionVo = entry.getValue();
                String productCode = moveProductionVo.getProductCode();
                Long needProductionQty = moveProductionVo.getProductionQty();
                for (int i = intervalStartDay; i <= endDay; i++) {
                    if (needProductionQty <= 1) {
                        //当前规格段退出，换一个规格段
                        break;
                    }
                    String moveMothPlanFormat = "开始对[%d]计划进行规格移动，移到[%d]日，使用%s模具排产，需移动量[%d]";
                    String moveMothPlanContext = String.format(moveMothPlanFormat, monthPlanId, i, mouldCodeInfo, needProductionQty);
                    log.info(moveMothPlanContext);
                    MouldProductionLog moveMothPlanLog = ProductionLogUtils.buildProductionLog(productionContext, productionContext.getMonthPlanInitMap().get(monthPlanId), MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, moveProductContext);
                    saveProductionLog(productionContext, moveMothPlanLog);

                    DoubleMouldProductionParamHelper helper = new DoubleMouldProductionParamHelper(productCode, i - 1, i, i + 1, needProductionQty, monthPlanId, first, second);
                    DoubleMouldProductionResultHelper result = doubleMouldProduction(helper, productionContext, first.getProductionOrient(), true);
                    needProductionQty = result.getNeedProductionQty();
                }
            }
        }
    }

    /**
     * 双模排产方式-排产
     *
     * @param doubleMouldProductionHelper 排产信息
     * @param productionContext           排产上下文
     * @param productionOrient            排产方向
     * @param isMove                      是否为移动环节
     */
    protected DoubleMouldProductionResultHelper doubleMouldProduction(DoubleMouldProductionParamHelper doubleMouldProductionHelper, ProductionContext productionContext, ProductionOrientEnum productionOrient, boolean isMove) {
        String productCode = doubleMouldProductionHelper.getProductCode();
        Integer startProductionDate = doubleMouldProductionHelper.getStartProductionDate();
        Long needProductionQty = doubleMouldProductionHelper.getNeedProductionQty();
        Long monthPlanId = doubleMouldProductionHelper.getMonthPlanId();
        MonthPlanManufacturingRequirementVo productionPlan = productionContext.getMonthPlanInitMap().get(monthPlanId);
        String sizeCapacityKey = productionPlan.getSizeCapacityGroupKey();
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(productionPlan, productionContext);
        List<MouldInfoVO> productionMouldList = doubleMouldProductionHelper.getProductionMouldList();
        MouldInfoVO first = doubleMouldProductionHelper.getFirst();
        MouldInfoVO second = doubleMouldProductionHelper.getSecond();
        String mouldCodeInfo = String.format("[%s]、[%s]", first.getMouldCode(), second.getMouldCode());
        String moveText = "";
        if (isMove) {
            moveText = "反向排产移动：";
        }
        //双模排产量 20250903 续作排产标识-false
        DayProductionPlanInfoVo dayProductionPlanInfo = new DayProductionPlanInfoVo(monthPlanId, productCode, sizeCapacityKey, startProductionDate, needProductionQty, singleCuringTime, false);
        Map<String, ProductionInfoVo> productionInfoMap = MouldUtils.calculateProductionQty(productionMouldList, dayProductionPlanInfo, productionContext, false);
        ProductionInfoVo firstProductionInfo = productionInfoMap.get(first.getMouldCode());
        ProductionInfoVo secondProductionInfo = productionInfoMap.get(second.getMouldCode());
        //双模单日总排产量
        Long productionQty = firstProductionInfo.getProductionQty() + secondProductionInfo.getProductionQty();
        //剩余还需排产量
        Long leftOverNeedProductionQty = needProductionQty - productionQty;
        Integer nextProductionDate = doubleMouldProductionHelper.getNextProductionDate();
        //双模单日排产结果：日志打印及保存记录
        String mouldProductionDateLogContent = String.format("%s[%d]计划使用%s模具[%s]排产,在[%d]日排产量[%d]", moveText, monthPlanId, mouldCodeInfo, productionOrient.getDesc(), startProductionDate, productionQty);
        log.info(mouldProductionDateLogContent);
        MouldProductionLog mouldProductionDateLog = ProductionLogUtils.buildProductionLog(productionContext, productionPlan, MouldProductionLogType.SINGLE_PLAN_GENERAL_LOG, mouldProductionDateLogContent);
        saveProductionLog(productionContext, mouldProductionDateLog);
        //更新模具日排产信息
        updateMouldDayProductionInfo(null, first, productionPlan, firstProductionInfo, productionContext, false);
        updateMouldDayProductionInfo(null, second, productionPlan, secondProductionInfo, productionContext, false);
        return new DoubleMouldProductionResultHelper(nextProductionDate, leftOverNeedProductionQty, firstProductionInfo, secondProductionInfo);
    }

    /**
     * 处理排产模具数量
     *
     * @param productionContext         排产上下文
     * @param productionOrient          排产方向
     * @param productionPlan            当前排产计划
     * @param leftOverNeedProductionQty 当前排产计划剩余排产量
     * @param mouldList                 排产模具
     * @param isContinueProduction      是否续作排产标记
     */
    protected void handlerProductionMouldQty(ProductionContext productionContext, ProductionOrientEnum productionOrient, MonthPlanManufacturingRequirementVo productionPlan, Integer startProductionDate, Long leftOverNeedProductionQty, List<MouldInfoVO> mouldList, boolean isContinueProduction) {
        //停工日跳过
        if (productionContext.getFactoryStopDays().contains(startProductionDate)) {
            return;
        }
        Set<String> joinedMouldSet = productionContext.getDayProductionFinishMouldMap().get(startProductionDate);
        //判断模具在startProductionDate日没有剩余排产量，则加入
        int productionMouldQty = BigDecimal.ZERO.intValue();
        BigDecimal singleCuringTime = ProductUtils.getSingleCuringTime(productionPlan, productionContext);
        for (MouldInfoVO mouldInfo : mouldList) {
            if (mouldInfo.getNoProductionDayList().containsKey(startProductionDate)) {
                continue;
            }
            //判断是否重复加入
            String mouldCode = mouldInfo.getMouldCode();
            if (joinedMouldSet.contains(mouldCode)) {
                continue;
            }
            BigDecimal dayLeftOverSeconds = mouldInfo.getProductionDayList().get(startProductionDate);
            if (null == dayLeftOverSeconds) {
                dayLeftOverSeconds = BigDecimal.ZERO;
            }
            if (dayLeftOverSeconds.compareTo(singleCuringTime) < BigDecimal.ZERO.intValue()) {
                joinedMouldSet.add(mouldCode);
                productionMouldQty = productionMouldQty + BigDecimal.ONE.intValue();
            }
        }
//        int productionMouldQty = mouldList.size();
//        boolean isDouble = (productionMouldQty == 2);
//        //判断productCode 是否到了收尾
//        if (ProductionPlanUtils.isEndByProductCode(isDouble, leftOverNeedProductionQty, otherNoProductionPlanList)) {
//            return;
//        }
        //增加天的排产模具数
        productionContext.addDayProductionMouldQty(productionOrient, startProductionDate, productionPlan, isContinueProduction, productionMouldQty);
    }

    /**
     * 设置整月可排产天信息
     *
     * @param productionContext 排产上下文
     */
    private void setWholeMonthWorkDayInfo(ProductionContext productionContext) {
        Set<Integer> stopDays = productionContext.getFactoryStopDays();
        Integer monthDays = productionContext.getMonthDays();
        Set<Integer> workDaySet = new HashSet<>();
        for (Integer day = ProductionConstant.MONTH_START_DAY; day <= monthDays; day++) {
            if (!CollectionUtils.isEmpty(stopDays) && stopDays.contains(day)) {
                continue;
            }
            workDaySet.add(day);
        }
        productionContext.setWholeMonthWorkDaySet(workDaySet);
    }

    /**
     * 设置排产控制值信息
     * 20250411
     * 增加每日排产总量控制--总控，不区分寸口|*|成型法
     * 增加细化到寸口|*|成型法的每日排产总量控制、
     * 每日排产新增最大规格数总量控制
     * 每日排产总量汇总--细化到寸口|*|成型法、
     * 每日排产规格数汇总
     *
     * @param productionContext
     */
    private void setProductionControlValue(ProductionContext productionContext) {
        Set<Integer> stopDays = productionContext.getFactoryStopDays();
        Integer monthDays = productionContext.getMonthDays();
        ProductionParamConfiguration productionParam = productionContext.getProductionParam();
        //每日排产量汇总集合--细化到寸口|*|成型法控制
        Map<Integer, Map<String, Long>> dayProductionQtyMap = new HashMap<>();
        //每日排产SAP集合
        Map<Integer, Set<String>> dayProductNumberMap = new HashMap<>();
        //每日最大新增规格数控制
        Map<Integer, Integer> dayAddedMaxProductNumberMap = new HashMap<>();
        //每日新增的规格SAP集合
        Map<Integer, Set<String>> dayAddedProductNumberMap = new HashMap<>();
        //每日最大排产量总控--忽略寸口|*|成型法
        Map<Integer, Long> dayMaxCapacityMap = new HashMap<>();
        //20251011 ZLT 每日排产模具数--成型硫化对等使用
        Map<Integer, Map<String, Integer>> dayProductionMouldQtyMap = new HashMap<>();
        Map<Integer, Set<String>> dayProductionFinishMouldMap = new HashMap<>();
        //每日最大新增规格数控制值
        Integer addedMaxProductCount = productionParam.getDayAddedProductCount();
        if (null == addedMaxProductCount) {
            addedMaxProductCount = Integer.MAX_VALUE;
        }
        for (Integer day = ProductionConstant.MONTH_START_DAY; day <= monthDays; day++) {
            if (!CollectionUtils.isEmpty(stopDays) && stopDays.contains(day)) {
                continue;
            }
            dayProductNumberMap.put(day, new HashSet<>());
            dayProductionQtyMap.put(day, new HashMap<>());
            dayProductionMouldQtyMap.put(day, new HashMap<>());
            dayAddedMaxProductNumberMap.put(day, addedMaxProductCount);
            dayAddedProductNumberMap.put(day, new HashSet<>());
            dayProductionFinishMouldMap.put(day, new HashSet<>());
            //每日最大排产量控制值 ZLT 与特殊天产能控制合并
            Long dayCapacity = productionParam.getSpecialDayLimitMap().get(day);
            if (null == dayCapacity) {
                dayCapacity = productionParam.getDayMaxProductionQty();
            }
            dayMaxCapacityMap.put(day, dayCapacity);
        }
        productionContext.setDayProductCodeMap(dayProductNumberMap);
        productionContext.setDayProductionQtyMap(dayProductionQtyMap);
        productionContext.setDayAddedProductLimitMap(dayAddedMaxProductNumberMap);
        productionContext.setDayAddProductMap(dayAddedProductNumberMap);
        productionContext.setDayMaxCapacityMap(dayMaxCapacityMap);
        productionContext.setDayProductionMouldQtyMap(dayProductionMouldQtyMap);
        productionContext.setDayProductionFinishMouldMap(dayProductionFinishMouldMap);
    }

    /**
     * 获取需要移动的规格Map
     * 并扣减对应的天的排产数量
     *
     * @param productionDate    当前天
     * @param productionContext 排产上下文
     * @param first             第一副模
     * @param second            第二副模
     * @return LinkMap<计划, 需要移动的规格对象>
     */
    private Map<Long, MouldDayProductionVo> getNeedMoveProductionMapAndSubtractDayQty(int productionDate, ProductionContext productionContext, MouldInfoVO first, MouldInfoVO second) {
        Map<Long, MouldDayProductionVo> mergeMoveProductionMap = new LinkedHashMap<>();
        Map<Long, MouldDayProductionVo> firstMoveProductionMap = getNeedMoveProductionMapAndSubtractDayQty(productionDate, first, productionContext);
        Map<Long, MouldDayProductionVo> secondMoveProductionMap = getNeedMoveProductionMapAndSubtractDayQty(productionDate, second, productionContext);
        if (!CollectionUtils.isEmpty(firstMoveProductionMap)) {
            firstMoveProductionMap.entrySet().forEach(entry -> mergeMoveProductionMap.put(entry.getKey(), entry.getValue()));
        }
        if (!CollectionUtils.isEmpty(secondMoveProductionMap)) {
            secondMoveProductionMap.entrySet().forEach(entry -> {
                Long monthPlanId = entry.getKey();
                MouldDayProductionVo mouldDayProduction = mergeMoveProductionMap.get(monthPlanId);
                if (null == mouldDayProduction) {
                    mergeMoveProductionMap.put(monthPlanId, entry.getValue());
                    return;
                }
                Long sumProductionQty = mouldDayProduction.getProductionQty() + entry.getValue().getProductionQty();
                mouldDayProduction.setProductionQty(sumProductionQty);
                mergeMoveProductionMap.put(monthPlanId, mouldDayProduction);
            });
        }
        return mergeMoveProductionMap;
    }

    /**
     * 获取需要移动的规格Map
     * 并扣减对应的天的排产数量
     *
     * @param iDay 当前天
     * @return LinkMap<计划, 需要移动的规格对象>
     */
    private Map<Long, MouldDayProductionVo> getNeedMoveProductionMapAndSubtractDayQty(int iDay, MouldInfoVO mouldInfoVO, ProductionContext productionContext) {
        Map<Integer, List<MouldDayProductionVo>> dayProductionMap = mouldInfoVO.getDayProductionMap();
        Map<Long, MouldDayProductionVo> moveProductMap = new LinkedHashMap<>();
        List<MouldDayProductionVo> dayProductionVos = dayProductionMap.get(iDay);
        if (PubUtil.isNotEmpty(dayProductionVos)) {
            return moveProductMap;
        }
        if (MouldUtils.checkIsNoPlaningDay(iDay, mouldInfoVO)) {
            //不排产日，则退出
            return moveProductMap;
        }
        //月份最大天数
        int endDay = productionContext.getMonthDays();
        //当天规格列表为空,表示可能存在间断；若后面存在规格列表，则表示存在间断；
        for (int i = iDay + 1; i <= endDay; i++) {
            List<MouldDayProductionVo> day1ProductionVos = dayProductionMap.get(i);
            if (CollectionUtils.isEmpty(day1ProductionVos)) {
                continue;
            }
            for (MouldDayProductionVo mouldDayProductionVo : day1ProductionVos) {
                Long monthPlanId = mouldDayProductionVo.getMonthPlanId();
                MonthPlanManufacturingRequirementVo productionPlan = productionContext.getMonthPlanInitMap().get(monthPlanId);
                MouldDayProductionVo moveProductVo = moveProductMap.get(monthPlanId);
                if (moveProductVo == null) {
                    moveProductVo = new MouldDayProductionVo();
                    BeanUtils.copyProperties(mouldDayProductionVo, moveProductVo);
                } else {
                    moveProductVo.setProductionQty(moveProductVo.getProductionQty() + mouldDayProductionVo.getProductionQty());
                }
                //20250414 增加单天最大排产量控制，移动时需要减量
                productionContext.moveDayProductionQty(mouldDayProductionVo.getProductionDate(), productionPlan.getSizeCapacityGroupKey(), mouldDayProductionVo.getProductionQty());
                moveProductMap.put(monthPlanId, moveProductVo);
            }

            //清空被累加的日排产信息
            BigDecimal dayWorkHours = ProductionProcessUtils.getDayWorkHours(productionContext);
            mouldInfoVO.getProductionDayList().put(i, dayWorkHours);
            mouldInfoVO.getDayProductionMap().put(i, new ArrayList<>());
            mouldInfoVO.setUsedSeconds(BigDecimalUtils.sub(mouldInfoVO.getUsedSeconds(), dayWorkHours));
        }
        return moveProductMap;
    }


    /**
     * 排除洗模日
     *
     * @param iDay
     * @param mouldInfoVO
     * @return
     */
    private void removeCleaningDay(int iDay, MouldInfoVO mouldInfoVO) {
        if (PubUtil.isEmpty(mouldInfoVO.getNoProductionDayList()) || mouldInfoVO.getNoProductionDayList().get(iDay) == null) {
            return;
        }
        if (MouldNoProductionType.MOULD_CLEANING_DAY.equals(mouldInfoVO.getNoProductionDayList().get(iDay).getNoProductionType())) {
            //若当天是洗模日
            mouldInfoVO.getNoProductionDayList().remove(iDay);
        }
    }

    /**
     * 保存排产结果
     * 包含 排产明细及汇总
     *
     * @param productionContext
     */
    protected Map<Long, Long> saveProductionResult(ProductionContext productionContext) {
        Map<String, MouldInfoVO> mouldInfoMap = productionContext.getMouldInfoMap();
        if (CollectionUtils.isEmpty(mouldInfoMap)) {
            return Collections.emptyMap();
        }
        //模具排产结果辅助记录
        List<MouldingProductionResultHelper> mouldingProductionResultList = ProductionPlanResultUtils.buildMouldProductionResult(productionContext);
        dataService.saveMouldingProductionResult(mouldingProductionResultList);
        //分组排产结果辅助记录
        List<ProductionGroupResultHelper> productionGroupResultList = ProductionGroupUtils.buildProductionGroupResult(productionContext);
        dataService.saveProductionGroupResult(productionGroupResultList);
        List<MonthPlanProductionResultDetail> detailList = ProductionPlanResultUtils.buildProductionResultDetailList(productionContext);
        Map<Long, Long> sumMonthPlanMap = new HashMap<>();
        detailList.forEach(productionDetail -> {
            Long monthPlanId = productionDetail.getMonthPlanId();
            Long productionQty = productionDetail.getTotalQty();
            if (null == productionQty) {
                productionQty = BigDecimal.ZERO.longValue();
            }
            Long plannedProductionQty = sumMonthPlanMap.get(monthPlanId);
            if (null == plannedProductionQty) {
                plannedProductionQty = BigDecimal.ZERO.longValue();
            }
            sumMonthPlanMap.put(monthPlanId, plannedProductionQty + productionQty);
        });
        //合并形成汇总
        List<MonthPlanMouldingDayResult> summaryResultList = ProductionPlanResultUtils.getSummaryResult(detailList, productionContext);
        //按SKU合并结果
        List<MonthPlanProductionDayResult> dayProductionResultList = ProductionPlanResultUtils.getSummaryByProductCodeResult(detailList, productionContext);
        dataService.saveMouldProductionDetail(detailList);
        dataService.saveMouldProductionSummary(summaryResultList);
        dataService.saveMonthPlanProductionResult(dayProductionResultList);
        return sumMonthPlanMap;
    }

    /**
     * 保存未排计划列表
     *
     * @param productionContext 排产上下文
     * @param sumProductionMap  已排计划信息
     */
    protected void saveNoProductionPlanResult(ProductionContext productionContext, Map<Long, Long> sumProductionMap) {
        Map<Long, MonthPlanManufacturingRequirementVo> productionPlanMap = productionContext.getMonthPlanInitMap();
        if (CollectionUtils.isEmpty(productionPlanMap)) {
            return;
        }
        List<MonthPlanNoProductionPlan> noProductionPlanList = ProductionPlanResultUtils.buildNoProductionPlanList(productionPlanMap, productionContext.getNoProductionRecordMap(), sumProductionMap);
        if (CollectionUtils.isEmpty(noProductionPlanList)) {
            return;
        }
        dataService.saveNoProductionPlan(noProductionPlanList);
    }

    /**
     * 保存模具排程排产流程日志
     *
     * @param productionContext productionContext
     * @param productionLog
     */
    protected void saveProductionLog(ProductionContext productionContext, MouldProductionLog productionLog) {
        if (null == productionLog) {
            return;
        }
        MouldProductionLogType logType = MouldProductionLogType.getInstance(productionLog.getLogType());
        String logContent = String.format("计划ID: %d -阶段：%s ： %s", productionLog.getMonthPlanId(), logType.getDesc(), productionLog.getLogContent());
        productionContext.getLogBuilder().append(logContent).append(System.lineSeparator()).append("===================").append(System.lineSeparator());
    }

    /**
     * 保存最后一批日志
     *
     * @param productionContext
     */
    protected void saveLastLogs(ProductionContext productionContext, MouldProductionLogType logType) {
        StringBuilder logBuilder = productionContext.getLogBuilder();
        String logContent = logBuilder.toString();
        if (StringUtils.isBlank(logContent)) {
            return;
        }
        MouldProductionLog log = new MouldProductionLog();
        log.setWorkNo(productionContext.getOperationWorkNo());
        log.setLogContent(logContent);
        log.setLogType(logType.getTypeValue());
        log.setMonthPlanVersion(productionContext.getMonthPlanVersion());
        log.setProductionVersion(productionContext.getProductionVersion());
        log.setFactoryCode(productionContext.getFactoryCode());
        log.setYear(productionContext.getYear());
        log.setMonth(productionContext.getMonth());
        dataService.saveMouldProductionLog(log);
    }

    /**
     * 设置分厂排产周期相关信息
     * 标记是自然月排产还是非自然月排产
     * 排产周期的起始日期
     *
     * @param factoryProductionVersion 分厂排产信息对象
     * @param productionVersion        排产版本号
     * @param productionContext        排产上下文
     */
    private void setProductionVersionCycleInfo(FactoryProductionVersion factoryProductionVersion, String productionVersion, ProductionContext productionContext) {
        factoryProductionVersion.setProductionInitVersion(productionVersion);
        factoryProductionVersion.setProductionVersion(productionVersion);
        //20250519 ZLT 设置月份排产模式自然月或非自然月及开始、结束排产日期
        factoryProductionVersion.setProductionStartDate(productionContext.getProductionStartDate());
        factoryProductionVersion.setProductionEndDate(productionContext.getProductionEndDate());
        factoryProductionVersion.setIsNaturalMonth(YesOrNoEnum.YES.getValue());
        if (!productionContext.isNaturalMonth()) {
            factoryProductionVersion.setIsNaturalMonth(YesOrNoEnum.NO.getValue());
        }
    }

    /**
     * 初始化物料基础信息
     *
     * @param context
     */
    private void initProductBaseInfo(ProductionContext context) {
        List<ProductBaseInfoVo> productInfoList = dataService.getProductBaseInfo(context);
        Map<String, ProductBaseInfoVo> productInfoMap = new HashMap<>();
        if (CollectionUtils.isEmpty(productInfoList)) {
            context.setProductInfoMap(productInfoMap);
            return;
        }
        context.setProductInfoMap(productInfoList.stream().collect(Collectors.toMap(ProductBaseInfoVo::getProductCode, Function.identity())));
    }

    /**
     * 根据排产参数设定，构建排产参数对象
     *
     * @param paramMap 分厂排产参数设定集合
     * @return
     */
    private ProductionParamConfiguration buildProductionParam(Map<String, Object> paramMap, ProductionContext productionContext) {
        ProductionParamConfiguration productionParam = new ProductionParamConfiguration();
        //排产周期
        productionParam.setMonthCycleStartDay((Integer) paramMap.get(FactoryConstant.SYS_PARAM_MONTH_CYCLE_START_DAY));
        productionParam.setSummerMonth((Integer) paramMap.get(FactoryConstant.SYS_PARAM_SUMMER_MONTH));
        productionParam.setWinterMonth((Integer) paramMap.get(FactoryConstant.SYS_PARAM_WINTER_MONTH));
        productionParam.setDayMaxProductCount((Integer) paramMap.get(FactoryConstant.SYS_PARAM_DAY_MAX_PRODUCT_COUNT));
        //20250909 是否开启按寸口由大到小排产模式
        productionParam.setOpenProSizeProductionModel((String) paramMap.get(FactoryConstant.SYS_PARAM_OPEN_PRO_SIZE_PRODUCTION_MODEL));
        //20250622 每日最大新增规格数
        productionParam.setDayAddedProductCount((Integer) paramMap.get(FactoryConstant.SYS_PARAM_DAY_ADDED_MAX_PRODUCT_COUNT));
        //外贸贴牌规格的OEE率
        productionParam.setExportOemBrandOee((BigDecimal) paramMap.get(FactoryConstant.SYS_PARAM_EXPORT_OEM_BRAND_OEE));
        //同规格跨组排产
        productionParam.setSameProductProductionQty((Integer) paramMap.get(FactoryConstant.SYS_PARAM_SAME_PRODUCT_LIMIT));
        //同寸口跨组排产
        productionParam.setSameProSizeProductionQty((Integer) paramMap.get(FactoryConstant.SYS_PARAM_SAME_PRO_SIZE_LIMIT));
        //共用生胎优先排产
        productionParam.setIsSameConstructionProduction((String) paramMap.get(FactoryConstant.SYS_PARAM_IS_SAME_CONSTRUCTION));
        //20250624 拼模排产参数
        productionParam.setAssemblingMouldProductionQty((Integer) paramMap.get(FactoryConstant.SYS_PARAM_ASSEMBLING_MOULD_PRODUCTION_QTY));
        productionParam.setMouldClampingPressureDiff((Integer) paramMap.get(FactoryConstant.SYS_PARAM_MOULD_CLAMPING_PRESSURE_DIFF));
        productionParam.setCuringTimeDiff((Integer) paramMap.get(FactoryConstant.SYS_PARAM_CURING_TIME_DIFF));
        productionParam.setPlanQtyDiff((Integer) paramMap.get(FactoryConstant.SYS_PARAM_PLAN_QTY_DIFF));

        //是否开启续作满月排产模式
        productionParam.setIsOpenContinueFullMonthProduction((String) paramMap.get(FactoryConstant.SYS_PARAM_CONTINUE_FULL_MOON_PRODUCTION));
        //续作满月排产模式-月平均销量值
        productionParam.setFullMonthProductionQty((Integer) paramMap.get(FactoryConstant.SYS_PARAM_MONTH_AVERAGE_VALUE));
        //续作满月排产模式-续作需求需要排产到的天计算，即要用最大天数-该值
        productionParam.setFullMonthProductionDay((Integer) paramMap.get(FactoryConstant.SYS_PARAM_FULL_MONTH_DAY));
        //20250927 ZLT 特殊天产能控制
        String specialDayLimitValue = (String) paramMap.get(FactoryConstant.SYS_SPECIAL_DAY_LIMIT_COUNT);
        productionParam.setSpecialDayLimitMap(ProductionCycleUtils.analysisSpecialDayLimit(specialDayLimitValue));
        //每日最大排产量，采用寸口+成型法更为细致的控制--20250628 再次启用
        Integer vulcanizationMachineAvgQty = (Integer) paramMap.get(FactoryConstant.SYS_PARAM_VULCANIZATION_MACHINE_AVG_QTY);
        if (null == vulcanizationMachineAvgQty) {
            return productionParam;
        }
        MachineCountDto machineCount = dataService.getMachineNumberInfo(productionContext.getFactoryCode());
        Integer vulcanizationMachineCount = machineCount.getVulcanizationMachineCount();
        if (vulcanizationMachineCount == 0) {
            return productionParam;
        }
        Integer dayMaxVulcanizationQty = vulcanizationMachineAvgQty * vulcanizationMachineCount;
        productionParam.setDayMaxProductionQty(Long.valueOf(dayMaxVulcanizationQty));

        return productionParam;
    }

    /**
     * 设置物料配置的模具列表
     * 及模具配置的物料列表
     *
     * @param context
     * @param productMouldConfigurationList
     * @param enableMouldSet                月度可用模具
     */
    private void initProductMouldRelationInfo(ProductionContext context, List<ProductMouldConfigurationVo> productMouldConfigurationList, Set<String> enableMouldSet) {
        if (CollectionUtils.isEmpty(productMouldConfigurationList)) {
            context.setMouldRelationProductMap(Collections.emptyMap());
            context.setProductRelationMouldMap(Collections.emptyMap());
            context.setProductRelationSpecCodeMouldMap(Collections.emptyMap());
            return;
        }
        Map<String, Set<String>> productRelationMouldMap = new HashMap<>();
        Map<String, Map<String, Set<String>>> productRelationSpecCodeMouldMap = new HashMap<>();
        Map<String, Set<String>> mouldRelationProductMap = new HashMap<>();
        productMouldConfigurationList.stream().forEach(configuration -> {
            String productCode = configuration.getProductCode();
            String mouldCode = configuration.getMouldCode();
            String specCode = configuration.getSpecCode();
            if (StringUtils.isBlank(productCode) || StringUtils.isBlank(mouldCode) || StringUtils.isBlank(specCode)) {
                return;
            }
            //不可用，不纳入
            if (!enableMouldSet.contains(mouldCode)) {
                return;
            }
            //物料关联的硫化模具
            ProductUtils.setProductRelationMould(productRelationMouldMap, productRelationSpecCodeMouldMap, productCode, mouldCode, specCode);
            //模具关联的物料编码
            MouldBaseUtils.setMouldRelationProduct(mouldRelationProductMap, mouldCode, productCode);
        });
        context.setMouldRelationProductMap(mouldRelationProductMap);
        context.setProductRelationMouldMap(productRelationMouldMap);
        context.setProductRelationSpecCodeMouldMap(productRelationSpecCodeMouldMap);
    }

    /**
     * 处理跨天扣产能问题
     *
     * @param mouldInfo         排产模具
     * @param productionInfo    排产信息
     * @param productionContext 排产上下文
     */
    private void handlerNextDaySubtractLeftOverTime(MouldInfoVO mouldInfo, ProductionInfoVo productionInfo, ProductionContext productionContext) {
        //是否需要跨天扣减产能
        BigDecimal nextDaySubtractTime = productionInfo.getNextDaySubtractTime();
        if (BigDecimalUtils.safeCompare(nextDaySubtractTime, BigDecimal.ZERO) >= 0) {
            return;
        }

        Integer productionDate = productionInfo.getProductionDate();
        //当天剩余硫化时间置为零
        mouldInfo.getProductionDayList().put(productionDate, BigDecimal.ZERO);
        mouldInfo.getProductionFinishDayList().add(productionDate);
        ProductionOrientEnum productionOrient = mouldInfo.getProductionOrient();
        //得到下一个排产日
        Integer nextDate = MouldUtils.getNextDate(productionDate, productionOrient);
        if (nextDate > productionContext.getMonthDays() || nextDate < ProductionConstant.MONTH_START_DAY) {
            return;
        }
        //已经排完日不可扣减
        if (mouldInfo.getProductionFinishDayList().contains(nextDate)) {
            return;
        }
        BigDecimal nextDateLeftOverSecond = mouldInfo.getProductionDayList().get(nextDate);
        if (null == nextDateLeftOverSecond) {
            return;
        }
        nextDateLeftOverSecond = nextDateLeftOverSecond.add(nextDaySubtractTime);
        mouldInfo.getProductionDayList().put(nextDate, nextDateLeftOverSecond);
    }

    /**
     * 更新产能占用量
     *
     * @param productionContext 排产上下文
     * @param productionPlan    排产计划
     * @param productionInfo    排产信息
     * @param productionOrient  排产方向
     * @param sizeCapacityKey   产能分组key 寸口|*|成型法|*|胎体布层级
     */
    private void updateCapacityConsumeQty(ProductionContext productionContext, MonthPlanManufacturingRequirementVo productionPlan, ProductionInfoVo productionInfo, ProductionOrientEnum productionOrient, String sizeCapacityKey) {
        Integer productionDate = productionInfo.getProductionDate();
        //增加使用硫化时间、剩余硫化时间
        BigDecimal usedCuringTime = productionInfo.getUsedCuringTime();
        //20250414 增加每日最大排产量控制 增加每日排产量汇总--需要加入换模，洗模，换规格的消耗量
        BigDecimal nextDaySubtractTime = productionInfo.getNextDaySubtractTime();
        ProductionLogUtils.addPreemptionConsumePlanQty(productionContext, productionPlan, productionDate, null);
        if (BigDecimalUtils.safeCompare(nextDaySubtractTime, BigDecimal.ZERO) >= BigDecimal.ZERO.intValue()) {
            Long usedDayQty = usedCuringTime.divide(productionInfo.getSingleCuringTime(), 0, RoundingMode.DOWN).longValue();
            ProductionLogUtils.addPreemptionConsumePlanQty(productionContext, productionPlan, productionDate, usedDayQty);
            productionContext.addDayProductionQty(productionDate, sizeCapacityKey, usedDayQty);
            return;
        }
        //出现跨天换规格或是洗模产能消耗
        Long productionDateUsedQty = usedCuringTime.add(nextDaySubtractTime).divide(productionInfo.getSingleCuringTime(), 0, RoundingMode.DOWN).longValue();
        ProductionLogUtils.addPreemptionConsumePlanQty(productionContext, productionPlan, productionDate, productionDateUsedQty);
        productionContext.addDayProductionQty(productionDate, sizeCapacityKey, productionDateUsedQty);
        Integer nextDate = MouldUtils.getNextDate(productionDate, productionOrient);
        //停产日不占
        if (productionContext.getFactoryStopDays().contains(nextDate)) {
            return;
        }
        //下一日占用产能
        Long nextDateUsedQty = BigDecimal.ZERO.subtract(nextDaySubtractTime).divide(productionInfo.getSingleCuringTime(), 0, RoundingMode.DOWN).longValue();
        ProductionLogUtils.addPreemptionConsumePlanQty(productionContext, productionPlan, nextDate, nextDateUsedQty);
        productionContext.addDayProductionQty(nextDate, sizeCapacityKey, nextDateUsedQty);
    }

    /**
     * 设置排产完毕日及标记模具排产完毕
     *
     * @param mouldInfo          模具信息
     * @param dayLeftOverSeconds 剩余硫化时间 单位到秒
     * @param productionDate     排产日
     * @param singleCuringTime   单条硫化时间(包含间隔增加时间)单位到秒
     * @param productionContext  排产上下文
     */
    private void setFinishDayAndFinishFlag(MouldInfoVO mouldInfo, BigDecimal dayLeftOverSeconds, Integer productionDate, BigDecimal singleCuringTime, ProductionContext productionContext) {
        ProductionOrientEnum productionOrient = mouldInfo.getProductionOrient();
        //剩余硫化时间小于单条硫化时间，则表示当前排产日已经排产完毕(同规格都不行，换规格更不行)
        if (dayLeftOverSeconds.compareTo(singleCuringTime) < BigDecimal.ZERO.intValue()) {
            mouldInfo.getProductionFinishDayList().add(productionDate);
            if (ProductionOrientEnum.FORWARD == productionOrient && productionDate.equals(productionContext.getMonthDays())) {
                mouldInfo.setIsFinish(Boolean.TRUE);
            }
            if (ProductionOrientEnum.REVERSE == productionOrient && mouldInfo.getProductionFinishDayList().size() == mouldInfo.getProductionDayList().keySet().size()) {
                mouldInfo.setIsFinish(Boolean.TRUE);
            }
        }
        if (ProductionOrientEnum.FORWARD == productionOrient) {
            return;
        }
        //反向排产需要记录已排完日期
        if (productionDate.equals(productionContext.getMonthDays())) {
            return;
        }
        Integer previousDate = productionDate + 1;
        //存在，则表示前一天已经排完
        if (mouldInfo.getDayProductionMap().containsKey(previousDate)) {
            mouldInfo.getProductionFinishDayList().add(previousDate);
        }
    }

    /**
     * 获取参数值
     *
     * @param paramCode
     * @param paramConfiguration
     * @return
     */
    private Object getParamValue(String paramCode, FactoryParam paramConfiguration) {
        if (StringUtils.isBlank(paramCode)) {
            return null;
        }
        if (null == paramConfiguration) {
            throw new RuntimeException(String.format("系统参数【%s】不存在.", paramCode));
        }
        return FactoryParamUtils.getParamValue(paramConfiguration);
    }

    public ProductionSchedulingDataService getDataService() {
        return dataService;
    }
}
