package com.zlt.aps.mp.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.enums.ProductionPlanType;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ContinueGroupInfo;
import com.zlt.aps.mp.engine.domain.dto.ContinueProductInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.logrecorder.TbrBeforeProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.mapper.*;
import com.zlt.aps.mp.engine.service.*;
import com.zlt.aps.utils.BeanCopyUtils;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排产调用数据获取服务类
 * 月份排产模块-数据业务实现
 *
 * @author ZLT
 * @date 20251208
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthProductionDataServiceImpl extends AbstractDataService implements MonthProductionDataService {

    private final BaseDao baseDao;

    private final MpStructureAllocationMapper mpStructureAllocationMapper;

    private final FactoryProductionInitMapper factoryProductionInitMapper;

    private final FactoryProductionSchedulingMapper factoryProductionSchedulingMapper;

    private final FactoryEngineProductionVersionMapper factoryEngineProductionVersionMapper;

    private final FactoryMonthPlanContinueProductInfoMapper factoryMonthPlanContinueProductInfoMapper;

    private final IFactoryMouldUsedStatusLogService factoryMouldUsedStatusLogService;

    private final IFactoryProductionMonthPlanInitService factoryProductionMonthPlanInitService;

    private final IFactoryProductionNoProductionPlanService factoryProductionNoProductionPlanService;

    private final IFactoryProductionDayProductionResultService factoryProductionDayProductionResultService;

    private final IFactoryProductionDayProductionResultDetailService factoryProductionDayProductionResultDetailService;

    @Override
    public MpFactoryProductionVersion getFactoryMonthPlanVersion(Context context) {
        QueryWrapper<MpFactoryProductionVersion> queryWrapper = new QueryWrapper();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("YEAR", context.getYear());
        queryWrapper.eq("MONTH", context.getMonth());
        queryWrapper.eq("MONTH_PLAN_VERSION", context.getMonthPlanVersion());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getCode());
        //不需要插入版本记录，则需要带入排产版本号查询
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
    public int addFactoryProductionVersion(MpFactoryProductionVersion addVersion) {
        if (null == addVersion) {
            return BigDecimal.ZERO.intValue();
        }
        addVersion.setId(null);
        return baseDao.insert(addVersion);
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
            return BigDecimal.ZERO.intValue();
        }
        return baseDao.update(updateVersion);
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
    public void deletedGroupProductionData(Context context) {
        String productionVersion = context.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return;
        }
        factoryProductionSchedulingMapper.deleteProductionGroupVersion(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
    }

    @Override
    public void deletedMouldProductionData(Context context) {
        String productionVersion = context.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return;
        }
        factoryProductionSchedulingMapper.deleteProductionVersionAfterGroup(context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
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
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveMouldProductionLog(MouldProductionLog productionLog) {
        if (null == productionLog) {
            return;
        }
        baseDao.insert(productionLog);
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
    public void saveMouldUsedLog(List<MpMouldUsedStatusLog> usedLogList) {
        if (CollectionUtils.isEmpty(usedLogList)) {
            return;
        }
        factoryMouldUsedStatusLogService.saveBatch(usedLogList);
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
    public List<ContinueProductInfo> getContinueProductionInfo(String factoryCode, Integer year, Integer month, Integer lastDay) {
        //取得上个月最后一天的排产信息
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || null == lastDay) {
            return Collections.emptyList();
        }
        return factoryMonthPlanContinueProductInfoMapper.getContinueProductInfo(factoryCode, year, month, lastDay);
    }

    @Override
    public List<ContinueGroupInfo> getContinueGroupInfo(MpFactoryProductionVersion previousVersion, Integer lastDay) {
        if (null == previousVersion) {
            return Collections.emptyList();
        }
        String factoryCode = previousVersion.getFactoryCode();
        Integer year = previousVersion.getYear();
        Integer month = previousVersion.getMonth();
        String monthPlanVersion = previousVersion.getMonthPlanVersion();
        String productionVersion = previousVersion.getProductionVersion();
        if (StringUtils.isBlank(monthPlanVersion) || StringUtils.isBlank(productionVersion)) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || null == lastDay) {
            return Collections.emptyList();
        }
        return factoryMonthPlanContinueProductInfoMapper.getContinueGroupInfo(factoryCode, year, month, monthPlanVersion, productionVersion, lastDay);
    }

    @Override
    public List<MpStructureAllocation> getHistoryStructureAllocationInfo(Context context) {
        //往前取3个月的定稿版本
        List<String> finalVersionList = getLatestMonthFinalVersion(context, 3);
        if (CollectionUtils.isEmpty(finalVersionList)) {
            log.info(TbrBeforeProductionGroupLogRecorder.addReaderNoCxMachineHistoryFinalVersionInfoLog(context));
            return Collections.emptyList();
        }
        //获取定稿的结构排产信息
        QueryWrapper<MpStructureAllocation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.in("PRODUCTION_VERSION", finalVersionList);
        queryWrapper.eq("PLAN_TYPE", ProductionPlanType.NORMAL.getPlanType());
        List<MpStructureAllocation> allocationList = mpStructureAllocationMapper.selectList(queryWrapper);
        log.info(TbrBeforeProductionGroupLogRecorder.addReaderCxMachineHistoryInfoLog(context, allocationList, finalVersionList));
        if (CollectionUtils.isEmpty(allocationList)) {
            return Collections.emptyList();
        }
        return allocationList;
    }

    @Override
    public List<MpStructureAllocation> getStructureAllocationInfoByProductionVersion(Context context) {
        if (isEmptyFactoryAndProductionVersion(context)) {
            return Collections.emptyList();
        }
        QueryWrapper<MpStructureAllocation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", context.getFactoryCode());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.in("PRODUCTION_VERSION", context.getProductionVersion());
        queryWrapper.eq("PLAN_TYPE", ProductionPlanType.NORMAL.getPlanType());
        List<MpStructureAllocation> allocationList = mpStructureAllocationMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(allocationList)) {
            return Collections.emptyList();
        }
        return allocationList;
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
            if (null == singleData.getAverageSaleQty()) {
                singleData.setInventorySalesRatio(null);
                return;
            }
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
    public void saveProductionStatisticsResult(List<MpMonthPlanStatistics> productionStatisticsResultList) {
        if (CollectionUtils.isEmpty(productionStatisticsResultList)) {
            return;
        }
        //数据不会太多
        baseDao.insertBatch(productionStatisticsResultList);
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

    /**
     * 构建获取当前排产月往前months月份的定稿版本信息
     *
     * @param context 当前排产信息
     * @param months  月份数
     * @return
     */
    private List<String> getLatestMonthFinalVersion(Context context, int months) {
        if (months <= BigDecimal.ZERO.intValue()) {
            return Collections.emptyList();
        }
        List<String> productionVersionList = new ArrayList<>();
        LocalDate currentProductionMonth = LocalDate.of(context.getYear(), context.getMonth(), ProductionConstant.MONTH_START_DAY);
        for (int index = BigDecimal.ZERO.intValue(); index < months; index++) {
            LocalDate previousMonth = currentProductionMonth.minusMonths(BigDecimal.ONE.intValue());
            MpFactoryProductionVersion previousMonthVersion = getFinalVersion(context.getFactoryCode(), previousMonth.getYear(), previousMonth.getMonthValue());
            //赋值当前月
            currentProductionMonth = previousMonth;
            if (null != previousMonthVersion) {
                productionVersionList.add(previousMonthVersion.getProductionVersion());
            }
        }
        return productionVersionList;
    }
}
