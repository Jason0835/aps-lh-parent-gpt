package com.zlt.aps.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tlt.aps.enums.ProductionPlanType;
import com.tlt.aps.enums.ProductionProcessesTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.daylimit.MouldAllocationInfoVo;
import com.zlt.aps.factory.daylimit.MouldShellBaseInfoVo;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ContinueGroupInfo;
import com.zlt.aps.factory.domain.dto.ContinueProductInfo;
import com.zlt.aps.factory.domain.dto.CxDevicePlanShutInfoHelper;
import com.zlt.aps.factory.domain.dto.MachineCountDto;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.mapper.*;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.logrecorder.TbrBeforeProductionGroupLogRecorder;
import com.zlt.aps.factory.service.*;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IPlanOrderSortConfigurationService;
import com.zlt.aps.maindata.service.IProductALevelService;
import com.zlt.aps.maindata.utils.FactoryParamUtils;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.ProductALevelVo;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排产调用数据获取服务类
 *
 * @author ZLT
 * @date 20251208
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionSchedulingDataServiceImpl implements ProductionSchedulingDataService {

    private final ProductMinConfigurationMapper productMinConfigurationMapper;

    private final MonthPlanRequireMapper monthPlanRequireMapper;

    private final MdmInterestRateEntityMapper interestRateMapper;

    private final MdmWorkWearInfoEntityMapper workWearInfoEntityMapper;

    private final MdmCapsuleChuckEntityMapper capsuleChuckEntityMapper;

    private final MdmWorkCalendarEntityMapper mdmWorkCalendarEntityMapper;

    private final FactoryProductionInitMapper factoryProductionInitMapper;

    private final FactoryMonthPlanCxInfoMapper factoryMonthPlanCxInfoMapper;

    private final FactoryMonthPlanProductInfoMapper factoryMonthPlanProductInfoMapper;

    private final FactoryProductionSchedulingMapper factoryProductionSchedulingMapper;

    private final FactoryMonthPlanProductMouldMapper factoryMonthPlanProductMouldMapper;

    private final FactoryEngineProductionVersionMapper factoryEngineProductionVersionMapper;

    private final FactoryMonthPlanProductLhCapacityMapper factoryMonthPlanProductLhCapacityMapper;

    private final FactoryMonthPlanContinueProductInfoMapper factoryMonthPlanContinueProductInfoMapper;

    private final FactoryMonthPlanSpecialMaterialInfoMapper factoryMonthPlanSpecialMaterialInfoMapper;

    private final FactoryMonthPlanProductConstructionMapper factoryMonthPlanProductConstructionMapper;

    private final MdmProductStockEntityMapper mdmProductStockEntityMapper;

    private final BaseDao baseDao;

    private final IFactoryParamService factoryParamService;

    private final IProductALevelService productALevelService;

    private final IPlanOrderSortConfigurationService sortConfigurationService;

    private final IFactoryMouldUsedStatusLogService factoryMouldUsedStatusLogService;

    private final IFactoryProductionMonthPlanInitService factoryProductionMonthPlanInitService;

    private final IFactoryProductionNoProductionPlanService factoryProductionNoProductionPlanService;

    private final IFactoryProductionDayProductionResultService factoryProductionDayProductionResultService;

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
    public MpFactoryProductionVersion getFactoryMonthPlanVersion(Context context) {
        QueryWrapper<MpFactoryProductionVersion> queryWrapper = new QueryWrapper();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("YEAR", context.getYear());
        queryWrapper.eq("MONTH", context.getMonth());
        queryWrapper.eq("MONTH_PLAN_VERSION", context.getMonthPlanVersion());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getCode());
        if (!Boolean.TRUE.equals(context.getInsertNewProductionVersion())) {
            queryWrapper.eq("PRODUCTION_VERSION", context.getProductionVersion());
            return factoryEngineProductionVersionMapper.selectOne(queryWrapper);
        }
        queryWrapper.isNull("PRODUCTION_INIT_VERSION");
        return factoryEngineProductionVersionMapper.selectOne(queryWrapper);
    }

    @Override
    public MpFactoryProductionVersion getFirstFactoryMonthPlanVersion(Context context) {
        QueryWrapper<MpFactoryProductionVersion> queryWrapper = new QueryWrapper();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("YEAR", context.getYear());
        queryWrapper.eq("MONTH", context.getMonth());
        queryWrapper.eq("MONTH_PLAN_VERSION", context.getMonthPlanVersion());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getCode());
        List<MpFactoryProductionVersion> dataList = factoryEngineProductionVersionMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(dataList)) {
            return null;
        }
        dataList.sort(Comparator.comparing(MpFactoryProductionVersion::getId));
        return dataList.get(BigDecimal.ZERO.intValue());
    }

    @Override
    public MpFactoryProductionVersion getFinalVersion(String factoryCode, Integer year, Integer month) {
        QueryWrapper<MpFactoryProductionVersion> queryWrapper = new QueryWrapper();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("YEAR", year);
        queryWrapper.eq("MONTH", month);
        queryWrapper.eq("IS_FINAL", YesOrNoEnum.YES.getCode());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getCode());
        queryWrapper.eq("PLAN_TYPE", ProductionPlanType.NORMAL.getPlanType());
        return factoryEngineProductionVersionMapper.selectOne(queryWrapper);
    }


    @Override
    public int updateFactoryProductionVersion(MpFactoryProductionVersion updateVersion) {
        if (null == updateVersion || null == updateVersion.getId()) {
            return 0;
        }
        return baseDao.update(updateVersion);
    }

    @Override
    public int updateProductionVersionInfo(MpFactoryProductionVersion updateVersion) {
        if (null == updateVersion || StringUtils.isBlank(updateVersion.getProductionVersion())) {
            return 0;
        }
        return factoryProductionSchedulingMapper.updateProductionVersionInfo(updateVersion);
    }

    @Override
    public int addFactoryProductionVersion(MpFactoryProductionVersion addVersion) {
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
    public List<CycleStructureMinLhMachineQtyVo> getCycleLhRatioInfo(Context context) {
        if (isEmptyFactoryAndYearMonth(context)) {
            return Collections.emptyList();
        }
        String factoryCode = context.getFactoryCode();
        Integer year = context.getYear();
        Integer month = context.getMonth();
        return factoryMonthPlanProductLhCapacityMapper.getCycleStructureMinLhRatioInfo(factoryCode, year, month);
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
    public List<ContinueGroupInfo> getContinueGroupInfo(String factoryCode, Integer year, Integer month, Integer lastDay) {
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || null == lastDay) {
            return Collections.emptyList();
        }
        return factoryMonthPlanContinueProductInfoMapper.getContinueGroupInfo(factoryCode, year, month, lastDay);
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
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderCxMachineInfoLog(context, cxMachineInfoList));
        if (CollectionUtils.isEmpty(cxMachineInfoList)) {
            return Collections.emptyMap();
        }
        Map<String, CxMachineBaseInfoVo> cxMachineInfoMap = cxMachineInfoList.stream().collect(Collectors.toMap(CxMachineBaseInfoVo::getCxMachineCode, Function.identity()));
        Map<String, CxDevicePlanShutInfoHelper> cxStopInfo = getCxMachineStopInfo(context);
        cxMachineInfoMap.forEach((cxMachineCode, cxMachineInfo) -> setCxMachineDayInfo(cxStopInfo, context, cxMachineInfo));
        return cxMachineInfoMap;
    }

    @Override
    public List<MdmWorkWearInfo> getWorkWearInfo(Context context) {
        if (isEmptyFactoryCode(context)) {
            return Collections.emptyList();
        }
        QueryWrapper<MdmWorkWearInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return workWearInfoEntityMapper.selectList(queryWrapper);
    }

    @Override
    public List<MdmCapsuleChuck> getCapsuleChuck(Context context) {
        if (isEmptyFactoryCode(context)) {
            return Collections.emptyList();
        }
        QueryWrapper<MdmCapsuleChuck> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return capsuleChuckEntityMapper.selectList(queryWrapper);
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
    public List<DpDemandPlan> getFactoryMonthPlan(Context context) {
        if (isEmptyFactoryAndRequireVersion(context)) {
            return Collections.emptyList();
        }
        QueryWrapper<DpDemandPlan> queryWrapper = new QueryWrapper();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("YEAR", context.getYear());
        queryWrapper.eq("MONTH", context.getMonth());
        if (StringUtils.isNotBlank(context.getMonthPlanVersion())) {
            queryWrapper.eq("MONTH_PLAN_VERSION", context.getMonthPlanVersion());
        }
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return monthPlanRequireMapper.selectList(queryWrapper);
    }

    @Override
    public List<ProductBaseInfoVo> getProductionMaterialInfo(Context context) {
        if (isEmptyFactoryAndRequireVersion(context)) {
            return Collections.emptyList();
        }
        String factoryCode = context.getFactoryCode();
        Integer year = context.getYear();
        Integer month = context.getMonth();
        String monthPlanVersion = context.getMonthPlanVersion();
        return factoryMonthPlanProductInfoMapper.getProductionMaterialInfo(factoryCode, year, month, monthPlanVersion);
    }

    @Override
    public List<MonthPlanProductConstructionInfoVo> getProductionConstructionInfo(Context context) {
        if (isEmptyFactoryAndRequireVersion(context)) {
            return Collections.emptyList();
        }
        String factoryCode = context.getFactoryCode();
        Integer year = context.getYear();
        Integer month = context.getMonth();
        String monthPlanVersion = context.getMonthPlanVersion();
        return factoryMonthPlanProductConstructionMapper.getConstructionByRequire(factoryCode, year, month, monthPlanVersion);
    }

    @Override
    public List<EmbryoSpecialMaterialInfoVo> getEmbryoSpecialMaterialInfo(Context context) {
        if (isEmptyFactoryAndProductionVersion(context)) {
            return Collections.emptyList();
        }
        String factoryCode = context.getFactoryCode();
        Integer year = context.getYear();
        Integer month = context.getMonth();
        String monthPlanVersion = context.getMonthPlanVersion();
        String productionVersion = context.getProductionVersion();
        return factoryMonthPlanSpecialMaterialInfoMapper.getSpecialMaterialEmbryoInfo(factoryCode, year, month, monthPlanVersion, productionVersion);
    }

    @Override
    public List<SpecialMaterialStockVo> getSpecialMaterialStockInfo(Context context) {
        if (isEmptyFactoryCode(context)) {
            return Collections.emptyList();
        }
        return factoryMonthPlanSpecialMaterialInfoMapper.getSpecialMaterialStockInfo(context.getFactoryCode());
    }

    /**
     * 获取成品库存
     *
     * @param context 排产上下文
     * @return
     */
    @Override
    public List<MdmProductStock> getMdmProductStock(Context context) {
        LambdaQueryWrapper<MdmProductStock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmProductStock::getFactoryCode, context.getFactoryCode());
        return mdmProductStockEntityMapper.selectList(queryWrapper);
    }

    @Override
    public List<MonthPlanProductionRequirePlanVo> getFactoryMonthPlanManufacturing(Context context) {
        if (isEmptyFactoryAndProductionVersion(context)) {
            return Collections.emptyList();
        }
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
    public List<MonthPlanProductMouldInfoVo> getProductionMouldInfo(Context context) {
        return factoryMonthPlanProductMouldMapper.getProductionMouldInfo(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion());
    }

    @Override
    public List<MonthPlanProductMouldInfoVo> getProductionMouldDeliveryInfo(Context context) {
        return factoryMonthPlanProductMouldMapper.getMouldDeliveryInfo(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionStartDate(), context.getProductionEndDate());
    }

    @Override
    public List<MonthPlanProductMouldInfoVo> getEnableProductionMouldInfo(Context context) {
        return factoryMonthPlanProductMouldMapper.getEnableProductionMouldInfo(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
    }

    @Override
    public List<MonthPlanProductMouldInfoVo> getEnableProductionMouldDeliveryInfo(Context context) {
        return factoryMonthPlanProductMouldMapper.getEnableMouldDeliveryInfo(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), context.getProductionStartDate(), context.getProductionEndDate());
    }

    @Override
    public List<MouldShellBaseInfoVo> getMouldShellInfo(Context context) {
        if (isEmptyFactoryCode(context)) {
            return Collections.emptyList();
        }
        return factoryMonthPlanProductMouldMapper.getMouldShellInfo(context.getFactoryCode());
    }

    @Override
    public List<MouldAllocationInfoVo> getMouldAllocationInfo(Context context) {
        if (isEmptyFactoryAndYearMonth(context)) {
            return Collections.emptyList();
        }
        return factoryMonthPlanProductMouldMapper.getMouldAllocationInfo(context.getFactoryCode(), context.getYear(), context.getMonth());
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
        return noProductionList.stream().collect(Collectors.toMap(FactoryNoProduction::getMaterialCode, Function.identity()));
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

    //先使用独立事务，看数据
    @Override
//    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveMonthPlanInit(List<MonthPlanProductionRequirePlanVo> monthPlanInitList) {
        if (CollectionUtils.isEmpty(monthPlanInitList)) {
            return;
        }
        List<ProductionMonthPlanInit> saveMonthPlanInitList = BeanCopyUtils.copyBeanList(monthPlanInitList, ProductionMonthPlanInit.class);
//        baseDao.insertBatch(saveMonthPlanInitList);
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
    public void saveNoProductionPlan(List<MonthPlanNoProductionPlan> noProductionPlanList) {
        if (CollectionUtils.isEmpty(noProductionPlanList)) {
            return;
        }
        noProductionPlanList.forEach(noProductionPlan -> {
            String noProductionReason = noProductionPlan.getReason();
            if (StringUtils.isNotBlank(noProductionReason)) {
                noProductionPlan.setReason(String.format("[%s]", noProductionReason));
            }
        });
        factoryProductionNoProductionPlanService.saveBatch(noProductionPlanList);
    }

    @Override
    public void saveMouldProductionDetailLog(List<FactoryMonthPlanMouldDayDetail> detailLogList) {
        if (CollectionUtils.isEmpty(detailLogList)) {
            return;
        }
        detailLogList.forEach(singleData -> {
            if (singleData.getInventorySalesRatio().compareTo(BigDecimal.ZERO) < BigDecimal.ZERO.intValue()) {
                singleData.setInventorySalesRatio(BigDecimal.ZERO);
            }
        });
        factoryProductionDayProductionResultDetailService.saveBatch(detailLogList);
    }

    @Override
    public void saveMouldProductionResult(List<FactoryMonthPlanMouldDayResult> dayResultList) {
        if (CollectionUtils.isEmpty(dayResultList)) {
            return;
        }
        dayResultList.forEach(singleData -> {
            if (null == singleData.getInventorySalesRatio()) {
                return;
            }
            if (singleData.getInventorySalesRatio().compareTo(BigDecimal.ZERO) < BigDecimal.ZERO.intValue()) {
                singleData.setInventorySalesRatio(BigDecimal.ZERO);
            }
        });
        factoryProductionDayProductionResultService.saveBatch(dayResultList);
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
    public void saveMouldUsedLog(List<MpMouldUsedStatusLog> usedLogList) {
        if (CollectionUtils.isEmpty(usedLogList)) {
            return;
        }
        factoryMouldUsedStatusLogService.saveBatch(usedLogList);
    }

    @Override
    public void saveGroupConversionResult(List<MpStructureAllocation> allocationResult) {
        if (CollectionUtils.isEmpty(allocationResult)) {
            return;
        }
        //数据不会太多
        baseDao.insertBatch(allocationResult);
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
        log.info(TbrBeforeProductionGroupLogRecorder.addReadCxMachineMaintenanceInfoLog(context, cxStopList));
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

    /**
     * 设置成型机的排产天数信息
     *
     * @param cxStopInfo    成型机维修信息
     * @param context       排产上下文
     * @param cxMachineInfo 成型机信息
     */
    private void setCxMachineDayInfo(Map<String, CxDevicePlanShutInfoHelper> cxStopInfo, Context context, CxMachineBaseInfoVo cxMachineInfo) {
        //成型硫化配比信息--为后续准备
        cxMachineInfo.setCxLhRatioMap(new HashMap<>());
        cxMachineInfo.setAllocationList(new ArrayList<>());
        cxMachineInfo.setAllocationDaySet(new HashSet<>());
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        if (StringUtils.isBlank(cxMachineCode)) {
            return;
        }
        Set<Integer> stopDaySet = new HashSet<>();
        //成型停产日-即本身维修停机日
        CxDevicePlanShutInfoHelper stopInfoHelper = cxStopInfo.get(cxMachineCode);
        if (null != stopInfoHelper) {
            stopDaySet = stopInfoHelper.getStopDaySet();
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
        Set<Integer> productionDaySet = new HashSet<>(64);
        for (Integer productionDay = ProductionConstant.MONTH_START_DAY; productionDay <= monthDays; productionDay++) {
            if (stopDaySet.contains(productionDay)) {
                continue;
            }
            productionDaySet.add(productionDay);
        }
        cxMachineInfo.setTheoryProductionDaySet(productionDaySet);
    }

    /**
     * 是否空的工厂、年份、月份、需求版本、排产版本条件
     *
     * @param context 排产上下文
     * @return
     */
    private boolean isEmptyFactoryAndProductionVersion(Context context) {
        boolean isEmptyFactoryAndRequireVersion = isEmptyFactoryAndRequireVersion(context);
        if (isEmptyFactoryAndRequireVersion) {
            return true;
        }
        return StringUtils.isBlank(context.getProductionVersion());
    }

    /**
     * 是否空的工厂、年份、月份、需求版本
     *
     * @param context 排产上下文
     * @return
     */
    private boolean isEmptyFactoryAndRequireVersion(Context context) {
        boolean isEmptyFactoryAndYearMonth = isEmptyFactoryAndYearMonth(context);
        if (isEmptyFactoryAndYearMonth) {
            return true;
        }
        return StringUtils.isBlank(context.getMonthPlanVersion());
    }

    /**
     * 是否空的工厂及年份、月份查询条件
     *
     * @param context 排产上下文
     * @return
     */
    private boolean isEmptyFactoryAndYearMonth(Context context) {
        boolean isEmptyFactoryCode = isEmptyFactoryCode(context);
        if (isEmptyFactoryCode) {
            return true;
        }
        return null == context.getYear() || null == context.getMonth();
    }

    /**
     * 是否空的工厂查询条件
     *
     * @param context 排产上下文
     * @return
     */
    private boolean isEmptyFactoryCode(Context context) {
        if (null == context) {
            return true;
        }
        return StringUtils.isBlank(context.getFactoryCode());
    }

}
