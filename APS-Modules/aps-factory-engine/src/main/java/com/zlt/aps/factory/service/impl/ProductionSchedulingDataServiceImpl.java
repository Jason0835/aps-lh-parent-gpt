package com.zlt.aps.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.factory.domain.dto.MachineCountDto;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.mapper.*;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.service.*;
import com.zlt.aps.factory.utils.DateUtils;
import com.zlt.aps.factory.utils.JsonUtils;
import com.zlt.aps.factory.utils.MouldBaseUtils;
import com.zlt.aps.factory.utils.ProductionProcessUtils;
import com.zlt.aps.maindata.mapper.MdmInterestRateEntityMapper;
import com.zlt.aps.maindata.mapper.ProductMinConfigurationMapper;
import com.zlt.aps.maindata.service.*;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.NoProductionDayMouldVo;
import com.zlt.aps.monthplan.api.domain.vo.ProductALevelVo;
import com.zlt.aps.monthplan.api.enums.MouldNoProductionType;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排产调用数据获取服务类
 *
 * @author ZLT
 * @date 20250220
 */
@Service
@RequiredArgsConstructor
public class ProductionSchedulingDataServiceImpl implements ProductionSchedulingDataService {

    private final ProductMinConfigurationMapper productMinConfigurationMapper;

    private final MonthPlanRequireMapper monthPlanRequireMapper;

    private final MdmInterestRateEntityMapper interestRateMapper;

    private final FactoryProductionInitMapper factoryProductionInitMapper;

    private final FactoryNoProductionRecordMapper factoryNoProductionRecordMapper;

    private final FactoryProductionSchedulingMapper factoryProductionSchedulingMapper;

    private final FactoryEngineProductionVersionMapper factoryEngineProductionVersionMapper;

    private final FactoryProductionMouldConfigurationMapper factoryProductionMouldConfigurationMapper;

    private final BaseDao baseDao;

    private final IFactoryParamService factoryParamService;

    private final IProductALevelService productALevelService;

    private final IMdmProductionCalendarService productionCalendarService;

    private final IPlanOrderSortConfigurationService sortConfigurationService;

    private final ISizeCapacityConfigurationService sizeCapacityConfigurationService;

    private final ITireCapacityConfigurationService tireCapacityConfigurationService;

    private final IFactoryProductionGroupResultService factoryProductionGroupResultService;

    private final IFactoryProductionMonthPlanInitService factoryProductionMonthPlanInitService;

    private final IFactoryProductionNoProductionPlanService factoryProductionNoProductionPlanService;

    private final IFactoryProductionNoProductionRecordService factoryProductionNoProductionRecordService;

    private final IFactoryProductionDayProductionResultService factoryProductionDayProductionResultService;

    private final IFactoryMonthPlanProductionDayResultService monthPlanProductionDayResultService;

    private final IFactoryMonthPlanPreProductionCapacityService factoryMonthPlanPreProductionCapacityService;

    private final IFactoryProductionMouldProductionResultService factoryProductionMouldProductionResultService;

    private final IFactoryProductionDayProductionResultDetailService factoryProductionDayProductionResultDetailService;

    @Override
    public FactoryProductionVersion getFactoryMonthPlanVersion(ProductionContext context) {
        QueryWrapper<FactoryProductionVersion> queryWrapper = new QueryWrapper();
        queryWrapper.eq(true, "FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq(true, "YEAR", context.getYear());
        queryWrapper.eq(true, "MONTH", context.getMonth());
        queryWrapper.eq(true, "MONTH_PLAN_VERSION", context.getMonthPlanVersion());
        queryWrapper.isNull("PRODUCTION_INIT_VERSION");
        return factoryEngineProductionVersionMapper.selectOne(queryWrapper);
    }

    @Override
    public int updateFactoryProductionVersion(FactoryProductionVersion updateVersion) {
        if (null == updateVersion || null == updateVersion.getId()) {
            return 0;
        }
        return baseDao.update(updateVersion);
    }

    @Override
    public int updateProductionVersionInfo(FactoryProductionVersion updateVersion) {
        if (null == updateVersion || StringUtils.isBlank(updateVersion.getProductionVersion())) {
            return 0;
        }
        return factoryProductionSchedulingMapper.updateProductionVersionInfo(updateVersion);
    }

    @Override
    public int addFactoryProductionVersion(FactoryProductionVersion addVersion) {
        if (null == addVersion) {
            return 0;
        }
        addVersion.setId(null);
        return baseDao.insert(addVersion);
    }

    @Override
    public List<ProductionCalendarVO> getProductCalendar(ProductionContext context) {
        List<MdmProductionCalendar> calendarList;
        //按自然月排产
        if (context.isNaturalMonth()) {
            MdmProductionCalendar factoryMonthQuery = new MdmProductionCalendar();
            factoryMonthQuery.setFactoryCode(context.getFactoryCode());
            factoryMonthQuery.setYear(context.getYear());
            factoryMonthQuery.setMonth(context.getMonth());
            calendarList = productionCalendarService.selectMdmProductionCalendarList(factoryMonthQuery);
        } else {
            //20250519 ZLT 非自然月
            Integer startDay = context.getProductionParam().getMonthCycleStartDay();
            LocalDate previousMonth = context.getPreviousMonth();
            LocalDate productionStartDay = LocalDate.of(previousMonth.getYear(), previousMonth.getMonthValue(), startDay);
            LocalDate productionEndDay = LocalDate.of(context.getYear(), context.getMonth(), startDay - 1);
            Date productionStartDate = DateUtils.getDate(productionStartDay);
            Date productionEndDate = DateUtils.getDate(productionEndDay);
            calendarList = productionCalendarService.getDateRangeCalendarList(context.getFactoryCode(), productionStartDate, productionEndDate);
        }
        if (CollectionUtils.isEmpty(calendarList)) {
            return Collections.emptyList();
        }
        return BeanCopyUtils.copyBeanList(calendarList, ProductionCalendarVO.class);

    }

    @Override
    public List<MdmProductConstruction> getProductConstruction(ProductionContext context) {
        return factoryProductionSchedulingMapper.getConstructionByRequire(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
    }

    @Override
    public Map<String, BaseConstructionVersionInfoVo> getBaseConstructionInfo() {
        List<BaseConstructionVersionInfoVo> baseConstructionInfoList = factoryProductionSchedulingMapper.getBaseConstructionInfo();
        if (CollectionUtils.isEmpty(baseConstructionInfoList)) {
            return Collections.emptyMap();
        }
        return baseConstructionInfoList.stream().collect(Collectors.toMap(BaseConstructionVersionInfoVo::getEmbryoCode, Function.identity()));
    }

    @Override
    public List<ProductBaseInfoVo> getProductBaseInfo(ProductionContext context) {
        return factoryProductionSchedulingMapper.getProductBaseInfoByRequire(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
    }

    @Override
    public List<SaleMonthPlanRequire> getFactoryMonthPlan(ProductionContext productionContext) {
        QueryWrapper<SaleMonthPlanRequire> queryWrapper = new QueryWrapper();
        queryWrapper.eq("FACTORY_CODE", productionContext.getFactoryCode());
        queryWrapper.eq("YEAR", productionContext.getYear());
        queryWrapper.eq("MONTH", productionContext.getMonth());
        queryWrapper.eq("MONTH_PLAN_VERSION", productionContext.getMonthPlanVersion());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return monthPlanRequireMapper.selectList(queryWrapper);
    }

    @Override
    public List<MonthPlanManufacturingRequirementVo> getFactoryMonthPlanManufacturing(ProductionContext productionContext) {
        QueryWrapper<ProductionMonthPlanInit> queryWrapper = new QueryWrapper();
        queryWrapper.eq(true, "FACTORY_CODE", productionContext.getFactoryCode());
        queryWrapper.eq(true, "YEAR", productionContext.getYear());
        queryWrapper.eq(true, "MONTH", productionContext.getMonth());
        queryWrapper.eq(true, "MONTH_PLAN_VERSION", productionContext.getMonthPlanVersion());
        queryWrapper.eq(true, "PRODUCTION_VERSION", productionContext.getProductionVersion());
        List<ProductionMonthPlanInit> dataList = factoryProductionInitMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyList();
        }
        return BeanCopyUtils.copyBeanList(dataList, MonthPlanManufacturingRequirementVo.class);
    }

    @Override
    public Map<String, FactoryParam> getFactoryParamConfiguration(String factoryCode, String productTypeCode) {
        FactoryParam query = new FactoryParam();
        query.setFactoryCode(factoryCode);
        query.setProductTypeCode(productTypeCode);
        List<FactoryParam> paramList = factoryParamService.getFacParamByList(query);
        if (CollectionUtils.isEmpty(paramList)) {
            return Collections.emptyMap();
        }
        return paramList.stream().collect(Collectors.toMap(FactoryParam::getParamCode, Function.identity()));
    }

    @Override
    public Map<String, ProductALevelVo> getProductDamageConfiguration(String factoryCode, String productTypeCode) {
        ProductALevel query = new ProductALevel();
        query.setFactoryCode(factoryCode);
        query.setProductTypeCode(productTypeCode);
        List<ProductALevelVo> productALevelList = productALevelService.getProductALevelList(query);
        if (CollectionUtils.isEmpty(productALevelList)) {
            return Collections.emptyMap();
        }
        return productALevelList.stream().collect(Collectors.toMap(ProductALevelVo::getProductCode, Function.identity()));
    }

    @Override
    public Map<String, Long> getMinimumLotSizeConfiguration(ProductionContext productionContext) {
        String factoryCode = productionContext.getFactoryCode();
        Integer year = productionContext.getYear();
        Integer month = productionContext.getMonth();
        String monthPlanVersion = productionContext.getMonthPlanVersion();
        QueryWrapper<ProductMinConfiguration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("PRODUCT_CODE", "*");
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        ProductMinConfiguration general = productMinConfigurationMapper.selectOne(queryWrapper);
        Long defaultMin = BigDecimal.ZERO.longValue();
        if (null != general) {
            defaultMin = Long.valueOf(general.getMinQty());
        }
        List<ProductMinConfiguration> requireMinConfigurationList = factoryProductionSchedulingMapper.getRequireMinConfiguration(factoryCode, year, month, monthPlanVersion);
        if (CollectionUtils.isEmpty(requireMinConfigurationList)) {
            return Collections.emptyMap();
        }
        Map<String, Long> minimumLotSizeMap = new HashMap<>();
        for (ProductMinConfiguration requireMinConfiguration : requireMinConfigurationList) {
            Integer minQty = requireMinConfiguration.getMinQty();
            String productCode = requireMinConfiguration.getProductCode();
            if (StringUtils.isBlank(productCode)) {
                continue;
            }
            if (null == minQty) {
                minimumLotSizeMap.put(productCode, defaultMin);
            } else {
                minimumLotSizeMap.put(productCode, Long.valueOf(minQty));
            }
        }
        return minimumLotSizeMap;
    }

    @Override
    public List<ProductionGroupVo> getFactoryProductionGroupConfiguration(String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            return Collections.emptyList();
        }
        return factoryProductionSchedulingMapper.getFactoryProductionGroupConfiguration(factoryCode);
    }

    @Override
    public List<MouldInfoVO> getMonthEnableMouldConfiguration(ProductionContext context) {
        List<MouldInfoVO> monthEnableList = factoryProductionSchedulingMapper.getMonthEnableMouldConfiguration(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        if (CollectionUtils.isEmpty(monthEnableList)) {
            return Collections.emptyList();
        }
        List<MouldInfoVO> mouldInfoList = new ArrayList<>();
        monthEnableList.stream().forEach(monthEnable -> {
            MouldInfoVO mouldInfo = buildMouldInfo(monthEnable, context);
            mouldInfoList.add(mouldInfo);
        });
        return mouldInfoList;
    }

    @Override
    public List<ProductMouldConfigurationVo> getProductionMouldInfoConfiguration(ProductionContext context) {
        return factoryProductionSchedulingMapper.getProductionMouldRelation(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
    }

    @Override
    public Map<String, FactoryNoProduction> getFactoryNoProductionConfiguration(String factoryCode, Integer year, Integer month) {
        List<FactoryNoProduction> noProductionList = factoryProductionSchedulingMapper.getFactoryNoProductionConfiguration(factoryCode, year, month);
        if (CollectionUtils.isEmpty(noProductionList)) {
            return Collections.emptyMap();
        }
        return noProductionList.stream().collect(Collectors.toMap(FactoryNoProduction::getProductCode, Function.identity()));
    }

    @Override
    public List<SizeCapacityConfiguration> getSizeCapacityConfiguration(String factoryCode, Integer year, Integer month) {
        return sizeCapacityConfigurationService.getConfigurationByFactoryYearAndMonth(factoryCode, year, month);
    }

    @Override
    public List<TireCapacityConfiguration> getTireCapacityConfiguration(String factoryCode, Integer year, Integer month) {
        return tireCapacityConfigurationService.getConfigurationByFactoryYearAndMonth(factoryCode, year, month);
    }

    @Override
    public List<ProductMouldInfoVO> getEnableUseProductMouldConfiguration(ProductionContext context) {
        List<ProductMouldConfigurationVo> productMouldConfigurationList = factoryProductionSchedulingMapper.getProductionMouldRelation(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
        if (CollectionUtils.isEmpty(productMouldConfigurationList)) {
            return Collections.emptyList();
        }
        Map<String, ProductMouldInfoVO> productMouldInfoMap = new HashMap<>();
        productMouldConfigurationList.stream().forEach(productMouldConfiguration -> {
            String productCode = productMouldConfiguration.getProductCode();
            ProductMouldInfoVO productMouldInfo = productMouldInfoMap.get(productCode);
            if (null == productMouldInfo) {
                productMouldInfo = new ProductMouldInfoVO();
            }
            productMouldInfo.setProductCode(productCode);
            Map<String, String> mouldMap = productMouldInfo.getMouldMap();
            if (null == mouldMap) {
                mouldMap = new HashMap<>();
            }
            mouldMap.put(productMouldConfiguration.getMouldCode(), productMouldConfiguration.getSpecCode());
            productMouldInfo.setMouldMap(mouldMap);
            productMouldInfoMap.put(productCode, productMouldInfo);
        });
        Map<String, MouldInfoVO> mouldInfoMap = context.getMouldInfoMap();
        productMouldInfoMap.forEach((productCode, productionMouldInfo) -> {
            Set<String> mouldSet = productionMouldInfo.getMouldMap().keySet();
            if (CollectionUtils.isEmpty(mouldSet)) {
                productionMouldInfo.setMouldInfoList(Collections.emptyList());
                return;
            }
            List<MouldInfoVO> mouldInfoList = new ArrayList<>();
            mouldSet.forEach(mouldCode -> {
                MouldInfoVO mouldBaseInfo = mouldInfoMap.get(mouldCode);
                if (null != mouldBaseInfo) {
                    mouldInfoList.add(mouldBaseInfo);
                }
            });
            productionMouldInfo.setMouldInfoList(mouldInfoList);
        });
        return productMouldInfoMap.values().stream().collect(Collectors.toList());
    }

    @Override
    public List<MouldInfoVO> getMouldMaintenanceConfiguration(ProductionContext context) {
        String factoryCode = context.getFactoryCode();
        String monthPlanVersion = context.getMonthPlanVersion();
        List<MouldMaintenanceConfigurationVo> maintenanceList;
        if (context.isNaturalMonth()) {
            maintenanceList = factoryProductionSchedulingMapper.getFactoryMouldMaintenanceConfiguration(factoryCode, context.getYear(), context.getMonth(), monthPlanVersion);
        } else {
            Date startDate = context.getProductionStartDate();
            Date endDate = context.getProductionEndDate();
            maintenanceList = factoryProductionSchedulingMapper.getFactoryMouldMaintenanceConfigurationByDateRange(factoryCode, startDate, endDate, monthPlanVersion);
        }
        if (CollectionUtils.isEmpty(maintenanceList)) {
            return Collections.emptyList();
        }
        Map<String, MouldInfoVO> maintenanceMouldMap = new HashMap<>();
        List<MouldInfoVO> mouldInfoList = new ArrayList<>();
        ZoneId zoneId = ZoneId.systemDefault();
        maintenanceList.stream().forEach(maintenanceConfiguration -> {
            String mouldCode = maintenanceConfiguration.getMouldCode();
            MouldInfoVO mouldInfo = MouldBaseUtils.buildMouldInfo(maintenanceMouldMap.get(mouldCode), maintenanceConfiguration, zoneId, context);
            mouldInfoList.add(mouldInfo);
        });
        return mouldInfoList;
    }

    @Override
    public List<MdmInterestRate> getInterestRateConfiguration() {
        return interestRateMapper.selectList(new QueryWrapper<>());
    }

    @Override
    public void deletedInitData(ProductionContext context) {
        String productionVersion = context.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return;
        }
        factoryProductionSchedulingMapper.deleteProductionInitVersion(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
    }

    @Override
    public void deletedMouldProductionData(ProductionContext context) {
        String productionVersion = context.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return;
        }
        factoryProductionSchedulingMapper.deleteProductionMouldVersion(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
    }

    @Override
    public List<MouldProductionProductVo> getContinueProductAndMould(ProductionContext context) {
        //TODO 获取排产前一天的硫化续作规格，重新推论续作规格及续作模具
        QueryWrapper<ProductionMouldConfiguration> queryWrapper = new QueryWrapper();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("YEAR", context.getYear());
        queryWrapper.eq("MONTH", context.getMonth());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        List<ProductionMouldConfiguration> configurationList = factoryProductionMouldConfigurationMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(configurationList)) {
            return Collections.emptyList();
        }
        return BeanCopyUtils.copyBeanList(configurationList, MouldProductionProductVo.class);
    }

    @Override
    public void saveNoProductionPlanRecord(List<MonthPlanNoProductionRecord> factoryNoProductionPlanList) {
        if (CollectionUtils.isEmpty(factoryNoProductionPlanList)) {
            return;
        }
        factoryProductionNoProductionRecordService.saveBatch(factoryNoProductionPlanList);
    }

    @Override
    public int deletedNoProductionRecord(ProductionContext context) {
        return factoryNoProductionRecordMapper.deletedNoProductionRecord(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
    }

    @Override
    public void saveMouldPreCapacity(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> preCapacityList) {
        if (CollectionUtils.isEmpty(preCapacityList)) {
            return;
        }
        String language = Locale.SIMPLIFIED_CHINESE.toString();
        List<MonthPlanPreProductionCapacity> capacityList = new ArrayList<>();
        preCapacityList.stream().forEach(preAllocation -> {
            if (preAllocation.getFactProdReqQty() <= BigDecimal.ZERO.longValue()) {
                return;
            }
            MonthPlanPreProductionCapacity preProductionCapacity = BeanCopyUtils.copyBean(preAllocation, MonthPlanPreProductionCapacity.class);
            preProductionCapacity.setPreProductionQty(preAllocation.getProductionQty());
            preProductionCapacity.setId(null);
            if (StringUtils.isNotBlank(preAllocation.getNoProductionReason())) {
                String reason = JsonUtils.parseJsonRemark(preAllocation.getNoProductionReason(), language);
                preProductionCapacity.setRemark(reason);
            }
            Set<String> mouldCodeSet = preAllocation.getPreemptMouldCodeSet();
            if (!CollectionUtils.isEmpty(mouldCodeSet)) {
                preProductionCapacity.setMouldCodeInfo(mouldCodeSet.stream().collect(Collectors.joining(",")));
            }
            preProductionCapacity.setFactoryCode(productionContext.getFactoryCode());
            preProductionCapacity.setYear(productionContext.getYear());
            preProductionCapacity.setMonth(productionContext.getMonth());
            preProductionCapacity.setMonthPlanVersion(productionContext.getMonthPlanVersion());
            preProductionCapacity.setProductionVersion(productionContext.getProductionVersion());
            capacityList.add(preProductionCapacity);
        });
        if (CollectionUtils.isEmpty(capacityList)) {
            return;
        }
        factoryMonthPlanPreProductionCapacityService.saveBatch(capacityList);
    }

    @Override
    public void saveMonthPlanInit(List<MonthPlanManufacturingRequirementVo> monthPlanInitList) {
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            return;
        }
        List<ProductionMonthPlanInit> saveMonthPlanInitList = BeanCopyUtils.copyBeanList(monthPlanInitList, ProductionMonthPlanInit.class);
        factoryProductionMonthPlanInitService.saveBatch(saveMonthPlanInitList);
    }

    @Override
    public List<PlanOrderSortConfiguration> getProductionConfiguration(ProductionContext context) {
        List<PlanOrderSortConfiguration> sortConfigurationList = sortConfigurationService.getProductionConfiguration(context.getFactoryCode());
        if (CollectionUtils.isEmpty(sortConfigurationList)) {
            return Collections.emptyList();
        }
        return sortConfigurationList;
    }

    @Override
    public void updateProductionSequence(List<MonthPlanManufacturingRequirementVo> productionSequenceList) {
        if (CollectionUtils.isEmpty(productionSequenceList)) {
            return;
        }
        List<ProductionMonthPlanInit> saveMonthPlanInitList = BeanCopyUtils.copyBeanList(productionSequenceList, ProductionMonthPlanInit.class);
        factoryProductionMonthPlanInitService.updateBatchById(saveMonthPlanInitList);
    }

    @Override
    public void saveMouldProductionDetail(List<MonthPlanProductionResultDetail> detailList) {
        if (CollectionUtils.isEmpty(detailList)) {
            return;
        }
        factoryProductionDayProductionResultDetailService.saveBatch(detailList);
    }

    @Override
    public void saveNoProductionPlan(List<MonthPlanNoProductionPlan> noProductionPlanList) {
        if (CollectionUtils.isEmpty(noProductionPlanList)) {
            return;
        }
        noProductionPlanList.stream().forEach(noProductionPlan -> {
            String noProductionReason = noProductionPlan.getReason();
            if (StringUtils.isNotBlank(noProductionReason)) {
                noProductionPlan.setReason(String.format("[%s]", noProductionReason));
            }
        });
        factoryProductionNoProductionPlanService.saveBatch(noProductionPlanList);
    }

    @Override
    public void saveMouldProductionSummary(List<MonthPlanMouldingDayResult> dayList) {
        if (CollectionUtils.isEmpty(dayList)) {
            return;
        }
        dayList.stream().forEach(dayResult -> {
            String mergeInfo = dayResult.getMergeInfo();
            if (StringUtils.isNotBlank(mergeInfo)) {
                dayResult.setMergeInfo(String.format("[%s]", mergeInfo));
            }
        });
        factoryProductionDayProductionResultService.saveBatch(dayList);
    }

    @Override
    public void saveMonthPlanProductionResult(List<MonthPlanProductionDayResult> dayProductionResultList) {
        if (CollectionUtils.isEmpty(dayProductionResultList)) {
            return;
        }
        monthPlanProductionDayResultService.saveBatch(dayProductionResultList);
    }

    @Override
    public void saveMouldingProductionResult(List<MouldingProductionResultHelper> mouldingProductionResultList) {
        if (CollectionUtils.isEmpty(mouldingProductionResultList)) {
            return;
        }
        factoryProductionMouldProductionResultService.saveBatch(mouldingProductionResultList);
    }

    @Override
    public void saveProductionGroupResult(List<ProductionGroupResultHelper> productionGroupResultList) {
        if (CollectionUtils.isEmpty(productionGroupResultList)) {
            return;
        }
        factoryProductionGroupResultService.saveBatch(productionGroupResultList);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveMouldProductionLog(MouldProductionLog productionLog) {
        if (null == productionLog) {
            return;
        }
        baseDao.insert(productionLog);
    }

    @Override
    public MachineCountDto getMachineNumberInfo(String factoryCode) {
        if (StringUtils.isBlank(factoryCode)) {
            return new MachineCountDto(BigDecimal.ZERO.intValue(), BigDecimal.ZERO.intValue());
        }
        Integer formingMachineCount = factoryProductionSchedulingMapper.getFormingMachineCount(factoryCode);
        if (null == formingMachineCount) {
            formingMachineCount = BigDecimal.ZERO.intValue();
        }
        Integer vulcanizationMachineCount = factoryProductionSchedulingMapper.getVulcanizationMachineCount(factoryCode);
        if (null == vulcanizationMachineCount) {
            vulcanizationMachineCount = BigDecimal.ZERO.intValue();
        }
        return new MachineCountDto(formingMachineCount, vulcanizationMachineCount);
    }

    @Override
    public Set<String> getExportOemBrand(String factoryCode) {
        return factoryParamService.getNoStockUpPlanBrand(factoryCode);
    }

    @Override
    public Set<String> getGreaterAverageValueProductInfo(String factoryCode, Integer year, Integer month, Integer averageValue) {
        if (StringUtils.isBlank(factoryCode) || null == averageValue) {
            return Collections.emptySet();
        }
        List<ProductAverageSaleVo> averageSaleList = factoryProductionSchedulingMapper.getFactoryAverageSaleProduct(factoryCode, year, month, averageValue);
        if (CollectionUtils.isEmpty(averageSaleList)) {
            return Collections.emptySet();
        }
        return averageSaleList.stream().map(ProductAverageSaleVo::getProductCode).collect(Collectors.toSet());
    }

    /**
     * 构建模具信息对象
     *
     * @param baseInfo
     * @param context
     * @return
     */
    private MouldInfoVO buildMouldInfo(MouldInfoVO baseInfo, ProductionContext context) {
        MouldInfoVO mouldInfo = MouldBaseUtils.buildBaseMouldInfo(baseInfo);
        //月份最大天数
        Integer maxDays = context.getMonthDays();
        //停工日
        Set<Integer> stopDays = context.getFactoryStopDays();
        //每天工作时限
        BigDecimal dayCuringTime = ProductionProcessUtils.getDayWorkHours(context);
        //不可排产日列表
        Map<Integer, NoProductionDayMouldVo> noProductionDayMap = new HashMap<>(maxDays);
        //可排产日列表
        Map<Integer, BigDecimal> productionDayMap = new HashMap<>(maxDays);
        BigDecimal totalCuringTime = BigDecimal.ZERO;
        for (int productionDay = BigDecimal.ONE.intValue(); productionDay <= maxDays; productionDay++) {
            //不可排产
            if (stopDays.contains(productionDay)) {
                NoProductionDayMouldVo noProductionDay = new NoProductionDayMouldVo();
                noProductionDay.setDay(productionDay);
                noProductionDay.setNoProductionType(MouldNoProductionType.STOP_DAY);
                noProductionDayMap.put(productionDay, noProductionDay);
                continue;
            }
            //可排产
            productionDayMap.put(productionDay, dayCuringTime);
            totalCuringTime = totalCuringTime.add(dayCuringTime);
        }
        mouldInfo.setTotalSeconds(totalCuringTime);
        mouldInfo.setLeftOverSeconds(totalCuringTime);
        mouldInfo.setPreemptLeftOverSeconds(totalCuringTime);
        mouldInfo.setNoProductionDayList(noProductionDayMap);
        mouldInfo.setProductionDayList(productionDayMap);
        return mouldInfo;
    }

}
