package com.zlt.aps.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tlt.aps.enums.ProductionPlanType;
import com.tlt.aps.enums.ProductionProcessesTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ContinueProductInfo;
import com.zlt.aps.factory.domain.dto.CxDevicePlanShutInfoHelper;
import com.zlt.aps.factory.domain.dto.MachineCountDto;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.mapper.*;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.service.*;
import com.zlt.aps.factory.utils.MouldBaseUtils;
import com.zlt.aps.factory.utils.ProductionProcessUtils;
import com.zlt.aps.maindata.mapper.MdmInterestRateEntityMapper;
import com.zlt.aps.maindata.mapper.MdmWorkCalendarEntityMapper;
import com.zlt.aps.maindata.mapper.ProductMinConfigurationMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IPlanOrderSortConfigurationService;
import com.zlt.aps.maindata.service.IProductALevelService;
import com.zlt.aps.maindata.service.ITireCapacityConfigurationService;
import com.zlt.aps.maindata.utils.FactoryParamUtils;
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
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排产调用数据获取服务类
 *
 * @author ZLT
 * @date 20251208
 */
@Service
@RequiredArgsConstructor
public class ProductionSchedulingDataServiceImpl implements ProductionSchedulingDataService {

    private final ProductMinConfigurationMapper productMinConfigurationMapper;

    private final MonthPlanRequireMapper monthPlanRequireMapper;

    private final MdmInterestRateEntityMapper interestRateMapper;

    private final MdmWorkCalendarEntityMapper mdmWorkCalendarEntityMapper;

    private final FactoryProductionInitMapper factoryProductionInitMapper;

    private final FactoryMonthPlanCxInfoMapper factoryMonthPlanCxInfoMapper;

    private final FactoryMonthPlanProductInfoMapper factoryMonthPlanProductInfoMapper;

    private final FactoryProductionSchedulingMapper factoryProductionSchedulingMapper;

    private final FactoryMonthPlanProductMouldMapper factoryMonthPlanProductMouldMapper;

    private final FactoryEngineProductionVersionMapper factoryEngineProductionVersionMapper;

    private final FactoryMonthPlanProductLhCapacityMapper factoryMonthPlanProductLhCapacityMapper;

    private final FactoryMonthPlanContinueProductInfoMapper factoryMonthPlanContinueProductInfoMapper;

    private final FactoryMonthPlanProductConstructionMapper factoryMonthPlanProductConstructionMapper;

    private final BaseDao baseDao;

    private final IFactoryParamService factoryParamService;

    private final IProductALevelService productALevelService;

    private final IPlanOrderSortConfigurationService sortConfigurationService;

    private final ITireCapacityConfigurationService tireCapacityConfigurationService;

    private final IFactoryProductionGroupResultService factoryProductionGroupResultService;

    private final IFactoryProductionMonthPlanInitService factoryProductionMonthPlanInitService;

    private final IFactoryProductionNoProductionPlanService factoryProductionNoProductionPlanService;

    private final IFactoryProductionDayProductionResultService factoryProductionDayProductionResultService;

    private final IFactoryProductionMouldProductionResultService factoryProductionMouldProductionResultService;

    private final IFactoryProductionDayProductionResultDetailService factoryProductionDayProductionResultDetailService;

    @Override
    public Integer getProductionCycleConfiguration(Context context) {
        return factoryParamService.getMonthStartDay(context.getFactoryCode(), context.getProductType());
    }

    @Override
    public Map<String, Object> getFactoryParamByCondition(Context context, List<String> paramCodeList) {
        List<FactoryParam> paramConfigurationList = factoryParamService.getFactoryParamByCondition(context.getFactoryCode(), context.getProductType().getValue(), paramCodeList);
        if (CollectionUtils.isEmpty(paramConfigurationList)) {
            return Collections.emptyMap();
        }
        Map<String, FactoryParam> paramConfigurationMap = paramConfigurationList.stream().collect(Collectors.toMap(FactoryParam::getParamCode, Function.identity()));
        Map<String, Object> paramValueMap = new HashMap<>(paramConfigurationMap.size());
        //数据类型转换
        paramConfigurationMap.forEach((key, paramConfiguration) -> paramValueMap.put(key, getParamValue(paramConfiguration)));
        return paramValueMap;
    }

    @Override
    public FactoryProductionVersion getFactoryMonthPlanVersion(Context context) {
        QueryWrapper<FactoryProductionVersion> queryWrapper = new QueryWrapper();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("YEAR", context.getYear());
        queryWrapper.eq("MONTH", context.getMonth());
        queryWrapper.eq("MONTH_PLAN_VERSION", context.getMonthPlanVersion());
        queryWrapper.eq("PRODUCTION_VERSION", context.getProductionVersion());
        return factoryEngineProductionVersionMapper.selectOne(queryWrapper);
    }

    @Override
    public FactoryProductionVersion getFinalVersion(String factoryCode, Integer year, Integer month) {
        QueryWrapper<FactoryProductionVersion> queryWrapper = new QueryWrapper();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("YEAR", year);
        queryWrapper.eq("MONTH", month);
        queryWrapper.eq("IS_FINAL", YesOrNoEnum.YES.getCode());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getCode());
        queryWrapper.eq("PLAN_TYPE", ProductionPlanType.NORMAL.getPlanType());
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
    public List<ProductionDayInfoVo> getProductCalendar(Context context) {
        String factoryCode = context.getFactoryCode();
        Date productionStartDate = context.getProductionStartDate();
        Date productionEndDate = context.getProductionEndDate();
        if (StringUtils.isBlank(factoryCode) || null == productionStartDate || null == productionEndDate) {
            return Collections.emptyList();
        }
        QueryWrapper<MdmWorkCalendar> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("PROC_CODE", ProductionProcessesTypeEnum.MONTH_PLAN.getProcCode());
        queryWrapper.ge("PRODUCTION_DATE", productionStartDate);
        queryWrapper.le("PRODUCTION_DATE", productionEndDate);
        List<MdmWorkCalendar> configurationList = mdmWorkCalendarEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(configurationList)) {
            return Collections.emptyList();
        }
        return BeanCopyUtils.copyBeanList(configurationList, ProductionDayInfoVo.class);
    }

    @Override
    public List<MonthPlanStructureLhRatioVo> getLhRatioInfo(Context context, List<String> structureNameList) {
        String factoryCode = context.getFactoryCode();
        if (StringUtils.isBlank(factoryCode) || CollectionUtils.isEmpty(structureNameList)) {
            return Collections.emptyList();
        }
        List<MonthPlanStructureLhRatioVo> configurationList = factoryMonthPlanProductLhCapacityMapper.getStructureLhRatioInfo(factoryCode, structureNameList);
        if (CollectionUtils.isEmpty(configurationList)) {
            return Collections.emptyList();
        }
        return configurationList;
    }

    @Override
    public List<ContinueProductInfo> getContinueProductionInfo(String factoryCode, Integer year, Integer month, Integer lastDay) {
        //取得上个月最后一天的排产信息
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || null == lastDay) {
            return Collections.emptyList();
        }
        return factoryMonthPlanContinueProductInfoMapper.getContinueProductInfo(factoryCode, year, month, lastDay);
    }

    @Override
    public Map<String, CxMachineBaseInfoVo> getCxMachineBaseInfo(Context context) {
        String factoryCode = context.getFactoryCode();
        Date productionStartDate = context.getProductionStartDate();
        Date productionEndDate = context.getProductionEndDate();
        if (StringUtils.isBlank(factoryCode) || null == productionStartDate || null == productionEndDate) {
            return Collections.emptyMap();
        }
        List<CxMachineBaseInfoVo> cxMachineInfoList = factoryMonthPlanCxInfoMapper.getMachineBaseInfo(factoryCode);
        if (CollectionUtils.isEmpty(cxMachineInfoList)) {
            return Collections.emptyMap();
        }
        Map<String, CxMachineBaseInfoVo> cxMachineInfoMap = cxMachineInfoList.stream().collect(Collectors.toMap(CxMachineBaseInfoVo::getCxMachineCode, Function.identity()));
        Map<String, CxDevicePlanShutInfoHelper> cxStopInfo = getCxMachineStopInfo(context);
        cxMachineInfoMap.forEach((cxMachineCode, cxMachineInfo) -> {
            CxDevicePlanShutInfoHelper stopInfoHelper = cxStopInfo.get(cxMachineCode);
            if (null == stopInfoHelper) {
                return;
            }
            //本身维修停机日
            Set<Integer> stopDaySet = stopInfoHelper.getStopDaySet();
            if (null == stopDaySet) {
                stopDaySet = new HashSet<>();
            }
            //全局停工日
            Set<Integer> wholeStop = context.getStopDays();
            if (!CollectionUtils.isEmpty(wholeStop)) {
                stopDaySet.addAll(wholeStop);
            }
            Integer monthDays = context.getMonthDays();
            Integer maxProductionDays = monthDays - stopDaySet.size();
            //排产日信息
            cxMachineInfo.setStopDayInfo(stopDaySet);
            cxMachineInfo.setMaxProductionDays(maxProductionDays);
            cxMachineInfo.setRemainingDays(maxProductionDays);
        });
        return cxMachineInfoMap;
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
    public List<SaleMonthPlanRequire> getFactoryMonthPlan(Context context) {
        QueryWrapper<SaleMonthPlanRequire> queryWrapper = new QueryWrapper();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("YEAR", context.getYear());
        queryWrapper.eq("MONTH", context.getMonth());
        queryWrapper.eq("MONTH_PLAN_VERSION", context.getMonthPlanVersion());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return monthPlanRequireMapper.selectList(queryWrapper);
    }

    @Override
    public List<ProductBaseInfoVo> getProductionMaterialInfo(Context context) {
        return factoryMonthPlanProductInfoMapper.getProductionMaterialInfo(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
    }

    @Override
    public List<MonthPlanProductConstructionInfoVo> getProductionConstructionInfo(Context context) {
        return factoryMonthPlanProductConstructionMapper.getConstructionByRequire(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
    }

    @Override
    public List<MonthPlanProductionRequirePlanVo> getFactoryMonthPlanManufacturing(Context context) {
        QueryWrapper<ProductionMonthPlanInit> queryWrapper = new QueryWrapper();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("YEAR", context.getYear());
        queryWrapper.eq("MONTH", context.getMonth());
        queryWrapper.eq("MONTH_PLAN_VERSION", context.getMonthPlanVersion());
        queryWrapper.eq("PRODUCTION_VERSION", context.getProductionVersion());
        List<ProductionMonthPlanInit> dataList = factoryProductionInitMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyList();
        }
        return BeanCopyUtils.copyBeanList(dataList, MonthPlanProductionRequirePlanVo.class);
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
    public List<MonthPlanProductMouldInfoVo> getProductionMouldInfo(Context context) {
        return factoryMonthPlanProductMouldMapper.getProductionMouldInfo(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
    }

    @Override
    public List<MonthPlanProductMouldInfoVo> getProductionMouldDeliveryInfo(Context context) {
        return factoryMonthPlanProductMouldMapper.getMouldDeliveryInfo(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionStartDate(), context.getProductionEndDate());
    }

    @Override
    public List<MonthPlanProductLhCapacityVo> getProductLhCapacityInfo(Context context) {
        return factoryMonthPlanProductLhCapacityMapper.getProductionLhCapacityInfo(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
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
    public List<TireCapacityConfiguration> getTireCapacityConfiguration(String factoryCode, Integer year, Integer month) {
        return tireCapacityConfigurationService.getConfigurationByFactoryYearAndMonth(factoryCode, year, month);
    }

    @Override
    public List<ProductMouldInfoVO> getEnableUseProductMouldConfiguration(ProductionContext context) {
        return Collections.emptyList();
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
    public void deletedInitData(Context context) {
        String productionVersion = context.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return;
        }
        factoryProductionSchedulingMapper.deleteProductionInitVersion(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
    }

    @Override
    public void deletedMouldProductionData(Context context) {
        String productionVersion = context.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return;
        }
        factoryProductionSchedulingMapper.deleteProductionMouldVersion(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
    }

    @Override
    public void saveMonthPlanInit(List<MonthPlanProductionRequirePlanVo> monthPlanInitList) {
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
//            String mergeInfo = dayResult.getMergeInfo();
//            if (StringUtils.isNotBlank(mergeInfo)) {
//                dayResult.setMergeInfo(String.format("[%s]", mergeInfo));
//            }
        });
        factoryProductionDayProductionResultService.saveBatch(dayList);
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

    /**
     * 获取参数值,转化成对应数据类型值
     *
     * @param paramConfiguration 配置信息
     * @return
     */
    private Object getParamValue(FactoryParam paramConfiguration) {
        if (null == paramConfiguration) {
            return null;
        }
        return FactoryParamUtils.getParamValue(paramConfiguration);
    }

    /**
     * 根据排产上下文，获取对应的月计划-成型维修停机信息
     *
     * @param context 排产上下文
     * @return
     */
    private Map<String, CxDevicePlanShutInfoHelper> getCxMachineStopInfo(Context context) {
        String factoryCode = context.getFactoryCode();
        Date productionStartDate = context.getProductionStartDate();
        Date productionEndDate = context.getProductionEndDate();
        if (StringUtils.isBlank(factoryCode) || null == productionStartDate || null == productionEndDate) {
            return Collections.emptyMap();
        }
        //获取月计划-成型维修停机信息
        List<CxDevicePlanShutInfoVo> cxStopList = factoryMonthPlanCxInfoMapper.getDevicePlanShutInfo(factoryCode, productionStartDate, productionEndDate);
        if (CollectionUtils.isEmpty(cxStopList)) {
            return Collections.emptyMap();
        }
        //按成型机分组
        Map<String, List<CxDevicePlanShutInfoVo>> cxGroupMap = cxStopList.stream().collect(Collectors.groupingBy(CxDevicePlanShutInfoVo::getCxMachineCode));
        Map<String, CxDevicePlanShutInfoHelper> cxStopMap = new HashMap<>(cxGroupMap.size());
        //提取在排产周期范围内的停产日信息
        cxGroupMap.forEach((cxMachineCode, stopInfoList) -> {
            if (CollectionUtils.isEmpty(stopInfoList)) {
                return;
            }
            Set<Integer> stopDaySet = new HashSet<>();
            stopInfoList.forEach(stopInfo -> {
                stopInfo.setProductionStartDate(productionStartDate);
                stopInfo.setProductionEndDate(productionEndDate);
                stopDaySet.addAll(stopInfo.getStopDayInfo());
            });
            CxDevicePlanShutInfoHelper helper = new CxDevicePlanShutInfoHelper(cxMachineCode, stopDaySet);
            cxStopMap.put(cxMachineCode, helper);
        });
        return cxStopMap;
    }
}
