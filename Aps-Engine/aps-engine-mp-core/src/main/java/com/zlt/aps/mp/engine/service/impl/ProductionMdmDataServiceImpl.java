package com.zlt.aps.mp.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.enums.ProductionProcessesTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.MouldAllocationInfoVo;
import com.zlt.aps.mp.engine.daylimit.MouldShellBaseInfoVo;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.MachineCountDto;
import com.zlt.aps.mp.engine.domain.vo.*;
import com.zlt.aps.mp.engine.logrecorder.TbrBeforeProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.mapper.*;
import com.zlt.aps.mp.engine.service.ProductionMdmDataService;
import com.zlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.mp.engine.domain.dto.CxDevicePlanShutInfoHelper;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IProductALevelService;
import com.zlt.aps.maindata.utils.FactoryParamUtils;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.vo.ProductALevelVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
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
public class ProductionMdmDataServiceImpl extends AbstractDataService implements ProductionMdmDataService {

    private final MdmInterestRateEntityMapper interestRateMapper;

    private final MdmWorkWearInfoEntityMapper workWearInfoEntityMapper;

    private final MdmCapsuleChuckEntityMapper capsuleChuckEntityMapper;

    private final MdmProductStockEntityMapper mdmProductStockEntityMapper;

    private final MdmWorkCalendarEntityMapper mdmWorkCalendarEntityMapper;

    private final FactoryMonthPlanCxInfoMapper factoryMonthPlanCxInfoMapper;

    private final FactoryMonthPlanProductInfoMapper factoryMonthPlanProductInfoMapper;

    private final FactoryProductionSchedulingMapper factoryProductionSchedulingMapper;

    private final FactoryMonthPlanProductMouldMapper factoryMonthPlanProductMouldMapper;

    private final FactoryMonthPlanProductLhCapacityMapper factoryMonthPlanProductLhCapacityMapper;

    private final FactoryMonthPlanSpecialMaterialInfoMapper factoryMonthPlanSpecialMaterialInfoMapper;

    private final FactoryMonthPlanProductConstructionMapper factoryMonthPlanProductConstructionMapper;

    private final IFactoryParamService factoryParamService;

    private final IProductALevelService productALevelService;

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
        //20260130 月计划暂不考虑成型维修停机
        Map<String, CxMachineBaseInfoVo> cxMachineInfoMap = cxMachineInfoList.stream().collect(Collectors.toMap(CxMachineBaseInfoVo::getCxMachineCode, Function.identity()));
        Map<String, CxDevicePlanShutInfoHelper> cxStopInfo = Collections.emptyMap();
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
        if (isEmptyFactoryAndYearMonth(context)) {
            return Collections.emptyList();
        }

        // 需要取排产年月上一个月份库存
        Date yearMonth = DateUtils.parseDate(context.getYear() + "-" + context.getMonth() + "-" + 1);
        Date lastYearMonth = DateUtils.addMonths(yearMonth, -1);
        Integer queryYear = DateUtils.getYear(lastYearMonth);
        Integer queryMonth = DateUtils.getMonth(lastYearMonth);
        return factoryMonthPlanSpecialMaterialInfoMapper.getSpecialMaterialStockInfo(context.getFactoryCode(), queryYear, queryMonth);
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
    public List<MonthPlanProductMouldInfoVo> getEnableProductionFinalMouldInfo(Context context) {
        return factoryMonthPlanProductMouldMapper.getEnableProductionFinalMouldInfo(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
    }

    @Override
    public List<MonthPlanProductMouldInfoVo> getEnableProductionMouldDeliveryInfo(Context context) {
        return factoryMonthPlanProductMouldMapper.getEnableMouldDeliveryInfo(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), context.getProductionStartDate(), context.getProductionEndDate());
    }

    @Override
    public List<MonthPlanProductMouldInfoVo> getEnableProductionFinalMouldDeliveryInfo(Context context) {
        return factoryMonthPlanProductMouldMapper.getEnableFinalMouldDeliveryInfo(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(), context.getProductionStartDate(), context.getProductionEndDate());
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
    public List<MdmInterestRate> getInterestRateConfiguration() {
        return interestRateMapper.selectList(new QueryWrapper<>());
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
     * 月度计划暂不考虑
     * 根据排产上下文，获取对应的月计划-成型维修停机信息
     *
     * @param context 排产上下文
     * @return
     */
    @Deprecated
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

}
