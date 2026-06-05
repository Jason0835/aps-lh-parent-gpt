package com.zlt.aps.mp.factory.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.AjaxResultUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.constant.IncrementConstant;
import com.zlt.aps.enums.ProductionGroupTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import com.zlt.aps.maindata.enums.EventModuleTypeEnum;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.enums.ReleaseStatusEnum;
import com.zlt.aps.maindata.event.publisher.EventPublisher;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IRawSpecialMaterialRecordService;
import com.zlt.aps.mp.adjust.mapper.MpAdjustResultEntityMapper;
import com.zlt.aps.mp.adjust.mapper.MpAdjustStructureInEntityMapper;
import com.zlt.aps.mp.api.IFinalAndAdjustResultInterface;
import com.zlt.aps.mp.api.domain.dto.MonthPlanFinalizedEventDto;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.vo.AdjustsCxMachineVo;
import com.zlt.aps.mp.api.domain.vo.DailyMouldAvailabilityResult;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.mp.common.utils.GroupedMapWithOrder;
import com.zlt.aps.mp.common.utils.StringUtil;
import com.zlt.aps.mp.common.utils.poi.WorksheetData;
import com.zlt.aps.mp.demand.mapper.DpDemandPlanSumEntityMapper;
import com.zlt.aps.mp.demand.mapper.MpPredictionDetailEntityMapper;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.mp.engine.enums.DayVulcanizationModeEnum;
import com.zlt.aps.mp.engine.utils.DateUtils;
import com.zlt.aps.mp.factory.dto.MpSkuAdjustInfoVo;
import com.zlt.aps.mp.factory.event.MonthPlanFinalizedEvent;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanMouldDayResultEntityMapper;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.mp.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.mp.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.mp.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.aps.mp.factory.service.IMpStructureAllocationService;
import com.zlt.aps.mp.factory.service.MpSkuAdjustInfoService;
import com.zlt.aps.mp.mdm.dto.DataDTO;
import com.zlt.aps.mp.mdm.handler.DataManager;
import com.zlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.utils.IncrementService;
import com.zlt.aps.utils.JsonUtils;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ApsNumberUtils.intValue;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProductionFinalResultServiceImpl.java
 * 描    述：FactoryMonthPlanProductionFinalResultServiceImpl工厂月生产计划-最终排产计划定稿业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class FactoryMonthPlanProductionFinalResultServiceImpl extends AbstractDocService<FactoryMonthPlanProductionFinalResult> implements IFactoryMonthPlanProductionFinalResultService {

    private final static String SHEET_NAME = "%d年%d月排产";

    private final BaseDao baseDao;

    @Autowired
    private FactoryMonthPlanMouldDayResultEntityMapper resultMapper;
    @Autowired
    private FactoryMonthPlanProductionFinalResultEntityMapper finalMapper;
    @Autowired
    private MpFactoryProductionVersionMapper factoryProductionVersionMapper;
    @Resource
    private IncrementService incrementService;
    @Autowired
    private EventPublisher eventPublisher;
    @Autowired
    private FactoryProductionVersionServiceImpl factoryProductionVersionService;
    @Autowired
    private MpPredictionDetailEntityMapper mpPredictionDetailEntityMapper;
    @Autowired
    private MpAdjustResultEntityMapper mpAdjustResultEntityMapper;
    @Autowired
    private DpDemandPlanSumEntityMapper dpDemandPlanSumEntityMapper;
    @Autowired
    private IRawSpecialMaterialRecordService rawSpecialMaterialRecordService;
    @Autowired
    private MoldCavityInsertMaxValueCalculatorImpl moldCavityInsertMaxValueCalculator;
    @Autowired
    private MpStructureAllocationEntityMapper mpStructureAllocationEntityMapper;
    @Autowired
    private MpTrialPlanEntityMapper mpTrialPlanEntityMapper;
    @Autowired
    private MdmSkuConstructionRefEntityMapper mdmSkuConstructionRefEntityMapper;
    @Autowired
    private IFactoryParamService factoryParamService;
    @Autowired
    private MdmSkuLhCapacityEntityMapper mdmSkuLhCapacityEntityMapper;
    @Autowired
    private MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;
    @Autowired
    private MdmCycleSchStruConfEntityMapper mdmCycleSchStruConfEntityMapper;
    @Autowired
    private IMpStructureAllocationService mpStructureAllocationService;
    @Autowired
    private MpAdjustStructureInEntityMapper mpAdjustStructureInEntityMapper;

    private final MpSkuAdjustInfoService mpSkuAdjustInfoService;
    @Autowired
    private DataManager dataManager;

    @Override
    protected String getDocTypeCode() {
        return "";
    }

    @Override
    public List<FactoryMonthPlanProductionFinalResult> getDataList(FactoryMonthPlanProductionFinalResult condition) {
        QueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, condition);
        queryWrapper.orderByAsc("STRUCTURE_NAME", "MAIN_PATTERN", "MAIN_MATERIAL_DESC");
        List<FactoryMonthPlanProductionFinalResult> dataList = this.finalMapper.selectList(queryWrapper);
        dealList(dataList);
        return dataList;
    }

    @Override
    public Map<String, Integer> calculateStructureFrequency() {
        // 获取当前年月
        YearMonth currentYearMonth = YearMonth.now();
        YearMonth startYearMonth = currentYearMonth.minusMonths(12);
        String yearMonth = String.format("%s%02d", startYearMonth.getYear(), startYearMonth.getMonthValue());
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                .ge(FactoryMonthPlanProductionFinalResult::getYearMonth, Integer.valueOf(yearMonth))
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<FactoryMonthPlanProductionFinalResult> list = finalMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> structureFrequencyMap = Maps.newHashMap();
        Map<String, List<FactoryMonthPlanProductionFinalResult>> map = list.stream().collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getMaterialCode));
        map.forEach((materialCode, value) -> {
            Set<Integer> yearMonths = value.stream().map(FactoryMonthPlanProductionFinalResult::getYearMonth).collect(Collectors.toSet());
            structureFrequencyMap.put(materialCode, yearMonths.size());
        });
        return structureFrequencyMap;
    }

    @Override
    public int calculateStructureFrequency(String materialCode) {
        // 获取当前年月
        YearMonth currentYearMonth = YearMonth.now();
        YearMonth startYearMonth = currentYearMonth.minusMonths(12);
        String yearMonth = String.format("%s%02d", startYearMonth.getYear(), startYearMonth.getMonthValue());
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                .eq(FactoryMonthPlanProductionFinalResult::getMaterialCode, materialCode)
                .ge(FactoryMonthPlanProductionFinalResult::getYearMonth, Integer.valueOf(yearMonth))
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<FactoryMonthPlanProductionFinalResult> list = finalMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(list)) {
            return BigDecimal.ZERO.intValue();
        }
        Set<Integer> yearMonths = list.stream().map(FactoryMonthPlanProductionFinalResult::getYearMonth).collect(Collectors.toSet());
        return yearMonths.size();
    }

    @Override
    public Map<String, Integer> calculateMonthSurplus(String requireVersion, List<MdmProductStock> finishedProductStocks, Map<String, MdmMaterialInfo> materialInfoMap) {
        if (CollectionUtils.isEmpty(finishedProductStocks)) {
            return Collections.emptyMap();
        }
        List<Date> stockDates = finishedProductStocks.stream().map(MdmProductStock::getStockDate).filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(stockDates)) {
            return Collections.emptyMap();
        }
        Date maxDate = stockDates.stream()
                .filter(Objects::nonNull)
                .max(Date::compareTo).orElse(null);
        if (null == maxDate) {
            return Collections.emptyMap();
        }
        int year = DateUtils.getYear(maxDate);
        int month = DateUtils.getMonthsByYear(maxDate);
        int stockDay = DateUtils.getDaysByMonth(maxDate);
        // 获取当前年月
        String yearMonth = String.format("%s%02d", year, month);
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                .eq(FactoryMonthPlanProductionFinalResult::getYearMonth, Integer.valueOf(yearMonth))
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals = finalMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(factoryMonthPlanProdFinals)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> monthSurplusMap = Maps.newHashMap();
        List<MdmMonthSurplus> result = Lists.newArrayList();
        Map<String, List<FactoryMonthPlanProductionFinalResult>> groupByMaterialCode = this.getGroupMonthProdFinalPlanByMaterialCode(factoryMonthPlanProdFinals);
        groupByMaterialCode.forEach((key, value) -> {
            int planSurplusQty = this.calculateMonthSurplus(value, stockDay);
            if (planSurplusQty <= BigDecimal.ZERO.longValue()) {
                return;
            }
            MdmMonthSurplus entity = new MdmMonthSurplus();
            entity.setBaseVale(null);
            entity.setIsDelete(ApsConstant.APS_YES_NO_0);
            entity.setPlanSurplusQty(BigDecimal.valueOf(planSurplusQty));
            entity.setFactoryCode(value.get(0).getFactoryCode());
            entity.setYear(value.get(0).getYear());
            entity.setMonth(value.get(0).getMonth());
            entity.setRequireVersion(requireVersion);
            entity.setProductTypeCode(value.get(0).getProductTypeCode());
            if (materialInfoMap.containsKey(value.get(0).getMaterialCode())) {
                entity.setBrand(materialInfoMap.get(value.get(0).getMaterialCode()).getBrand());
            }
            entity.setMaterialCode(value.get(0).getMaterialCode());
            entity.setMaterialDesc(value.get(0).getMaterialDesc());
            entity.setStructureName(value.get(0).getStructureName());
            result.add(entity);
            monthSurplusMap.put(key, planSurplusQty);
        });
        if (CollectionUtils.isNotEmpty(result)) {
            this.baseDao.insertBatch(result);
        }
        return monthSurplusMap;
    }


    @Override
    public Map<String, Integer> calculateMonthSurplusNoSave(List<MdmProductStock> finishedProductStocks, String yearMonth, int days) {
        if (CollectionUtils.isEmpty(finishedProductStocks)) {
            return Collections.emptyMap();
        }
        List<Date> stockDates = finishedProductStocks.stream().map(MdmProductStock::getStockDate).filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(stockDates)) {
            return Collections.emptyMap();
        }
        Date maxDate = stockDates.stream()
                .filter(Objects::nonNull)
                .max(Date::compareTo).orElse(null);
        if (null == maxDate) {
            return Collections.emptyMap();
        }
        int year = DateUtils.getYear(maxDate);
        int month = DateUtils.getMonthsByYear(maxDate);
        int stockDay = DateUtils.getDaysByMonth(maxDate);
        // 获取当前年月
        yearMonth = String.format("%s%02d", year, month);
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                .eq(FactoryMonthPlanProductionFinalResult::getYearMonth, Integer.valueOf(yearMonth))
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);

        List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals = finalMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(factoryMonthPlanProdFinals)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> monthSurplusMap = Maps.newHashMap();
        Map<String, List<FactoryMonthPlanProductionFinalResult>> groupByMaterialCode = this.getGroupMonthProdFinalPlanByMaterialCode(factoryMonthPlanProdFinals);
        groupByMaterialCode.forEach((key, value) -> {
            int planSurplusQty = this.calculateMonthSurplus(value, stockDay);
            if (planSurplusQty <= BigDecimal.ZERO.longValue()) {
                return;
            }
            if (!value.isEmpty()) {
                String materialCode = value.get(0).getMaterialCode();
                monthSurplusMap.put(materialCode, planSurplusQty);
            }
        });
        return monthSurplusMap;
    }


    private int calculateMonthSurplus(List<FactoryMonthPlanProductionFinalResult> productionFinalResults, int stockDay) {
        int totalMonthSuplus = BigDecimal.ZERO.intValue();
        //统计汇总值
        Integer[] dayList = FactoryConstant.PRODUCTION_CYCLE;
        for (FactoryMonthPlanProductionFinalResult productionFinalResult : productionFinalResults) {
            for (Integer day : dayList) {
                if (day < stockDay) {
                    continue;
                }
                String fieldName = "day".concat(String.valueOf(day));
                int dayValue;
                Object value = productionFinalResult.getFieldValueByFieldName(fieldName);
                if (null == value) {
                    dayValue = BigDecimal.ZERO.intValue();
                } else {
                    dayValue = (Integer) value;
                }
                totalMonthSuplus = totalMonthSuplus + dayValue;
            }
        }
        return totalMonthSuplus;
    }


    @Override
    public List<FactoryMonthPlanMouldDayResult> findProductionFinalResult(MpFactoryProductionVersion finalVersion) {
        if (null == finalVersion) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FactoryMonthPlanMouldDayResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanMouldDayResult.class)
                .eq(FactoryMonthPlanMouldDayResult::getFactoryCode, finalVersion.getFactoryCode())
                .eq(FactoryMonthPlanMouldDayResult::getYear, finalVersion.getYear())
                .eq(FactoryMonthPlanMouldDayResult::getMonth, finalVersion.getMonth())
                .eq(FactoryMonthPlanMouldDayResult::getMonthPlanVersion, finalVersion.getMonthPlanVersion())
                .eq(FactoryMonthPlanMouldDayResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        return resultMapper.selectList(queryWrapper);
    }

    @Override
    public List<FactoryMonthPlanMouldDayResult> findProductionFinalResult(MpFactoryProductionVersion currentFinalVersion, Set<String> monthPlanVersions) {
        if (null == currentFinalVersion) {
            return Collections.emptyList();
        }
        List<FactoryMonthPlanMouldDayResult> result = new ArrayList<>();
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper =
                Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                        .eq(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, currentFinalVersion.getMonthPlanVersion())
                        .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<FactoryMonthPlanProductionFinalResult> list = this.finalMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(list)) {
            list.forEach(item -> {
                FactoryMonthPlanMouldDayResult entity = BeanCopyUtils.copyBean(item, FactoryMonthPlanMouldDayResult.class);
                result.add(entity);
            });
        }
        if (!CollectionUtils.isEmpty(monthPlanVersions)) {
            final int batchSize = 1000;
            List<String> versionList = new ArrayList<>(monthPlanVersions);
            for (int i = 0; i < versionList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, versionList.size());
                List<String> batchVersions = versionList.subList(i, end);
                LambdaQueryWrapper<FactoryMonthPlanMouldDayResult> wrapper =
                        Wrappers.lambdaQuery(FactoryMonthPlanMouldDayResult.class)
                                .in(FactoryMonthPlanMouldDayResult::getMonthPlanVersion, batchVersions)
                                .eq(FactoryMonthPlanMouldDayResult::getIsDelete, ApsConstant.APS_YES_NO_0);
                result.addAll(resultMapper.selectList(wrapper));
            }
        }
        return result;
    }

    private Map<String, List<FactoryMonthPlanProductionFinalResult>> getGroupMonthProdFinalPlanByMaterialCode(List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals) {
        if (CollectionUtils.isEmpty(factoryMonthPlanProdFinals)) {
            return Collections.emptyMap();
        }
        return factoryMonthPlanProdFinals.stream().collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getGroupKey));
    }

    /**
     * 解析不排产原因
     *
     * @param list
     */
    private void dealList(List<FactoryMonthPlanProductionFinalResult> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        Locale language = SecurityUtils.getUserLang();
        JsonUtils.parseJsonRemarkList(list, language.toString(), "reason");
    }

    /**
     * 构建查询条件
     *
     * @param queryWrapper 查询构建器
     * @param condition    查询条件值对象
     */
    protected void builderCondition(QueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper, FactoryMonthPlanProductionFinalResult condition) {
        /**
         * 工厂、年份、月份、需求版本、排产版本、产品品类
         */
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getFactoryCode()), "FACTORY_CODE", condition.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getYear()), "YEAR", condition.getYear());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getMonth()), "MONTH", condition.getMonth());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getMonthPlanVersion()), "MONTH_PLAN_VERSION", condition.getMonthPlanVersion());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getProductionVersion()), "PRODUCTION_VERSION", condition.getProductionVersion());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getProductTypeCode()), "PRODUCT_TYPE_CODE", condition.getProductTypeCode());
        /**
         * 物料相关
         */
        queryWrapper.like(PubUtil.isNotEmpty(condition.getMaterialCode()), "MATERIAL_CODE", condition.getMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getMaterialDesc()), "MATERIAL_DESC", condition.getMaterialDesc());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getMainMaterialDesc()), "MAIN_MATERIAL_DESC", condition.getMainMaterialDesc());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getConstructionStage()), "CONSTRUCTION_STAGE", condition.getConstructionStage());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getBrand()), "BRAND", condition.getBrand());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getProSize()), "PRO_SIZE", condition.getProSize());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getSpecifications()), "SPECIFICATIONS", condition.getSpecifications());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getMainPattern()), "MAIN_PATTERN", condition.getMainPattern());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getPattern()), "PATTERN", condition.getPattern());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getStructureName()), "STRUCTURE_NAME", condition.getStructureName());

    }

    /**
     * 定稿
     *
     * @param param 分厂年月
     * @return 结果
     */
    @Override
    public AjaxResult finalized(FactoryMonthPlanProductionFinalResult param) {
        // 1、校验参数合法
        this.checkFinalizedParam(param);
        // 2、更新版本表=定稿
        this.updateProVersion(param);
        //20260518+ 备份定稿版本的结构分配及排产统计数据
        backUpGroupAllocationInfo(param);
        // 3、将排产结果表数据新增到定稿表
        List<FactoryMonthPlanProductionFinalResult> finalList = insertFinalList(param);
        // 4、调用世超的分摊接口
        // 4.1、OrderAllocationServiceImpl.allocateProductionByMonth
        // 4.2、调用生成原材料需求计划 -- TODO
        // 5、写入月度硫化监控表
        // t_mp_month_plan_monitor
        // 上机日期 = 排产周期的开始日 +  (startDay -1 )
        this.publishFinalizedEvent(param, finalList);
        return AjaxResult.success();
    }

    @Override
    public List<FactoryMonthPlanMouldDayResult> findFinalProductionResult(MpFactoryProductionVersion finalVersion) {
        List<FactoryMonthPlanMouldDayResult> result = new ArrayList<>();
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper =
                Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                        .eq(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, finalVersion.getMonthPlanVersion())
                        .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<FactoryMonthPlanProductionFinalResult> list = this.finalMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(list)) {
            list.forEach(item -> {
                FactoryMonthPlanMouldDayResult entity = BeanCopyUtils.copyBean(item, FactoryMonthPlanMouldDayResult.class);
                result.add(entity);
            });
        }
        return result;
    }

    /**
     * 校验参数合法
     *
     * @param param 参数
     */
    private void checkFinalizedParam(FactoryMonthPlanProductionFinalResult param) {
        // 保证填写完整：年月、分厂、需求计划版本、分厂月计划版本
        if (param.getYear() == null || param.getMonth() == null || StringUtils.isBlank(param.getFactoryCode())
                || StringUtils.isBlank(param.getMonthPlanVersion()) || StringUtils.isBlank(param.getProductionVersion())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.finalized.checkParam"));
        }

        // 如果对应年月、分厂的最终计划数据已经存在
        Long finalCount = finalMapper.selectCount(Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                .eq(FactoryMonthPlanProductionFinalResult::getYear, param.getYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, param.getMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, param.getFactoryCode()));
        if (finalCount > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.finalized.exist"));
        }
    }

    /**
     * 更新版本表
     *
     * @param param 参数
     */
    private void updateProVersion(FactoryMonthPlanProductionFinalResult param) {
        // 更新版本表-是否定稿
        MpFactoryProductionVersion productionVersion = new MpFactoryProductionVersion();
        productionVersion.setIsFinal(YesOrNoEnum.YES.getCode());
        factoryProductionVersionMapper.update(productionVersion, Wrappers.lambdaQuery(MpFactoryProductionVersion.class)
                .eq(MpFactoryProductionVersion::getYear, param.getYear())
                .eq(MpFactoryProductionVersion::getMonth, param.getMonth())
                .eq(MpFactoryProductionVersion::getFactoryCode, param.getFactoryCode())
                .eq(MpFactoryProductionVersion::getMonthPlanVersion, param.getMonthPlanVersion())
                .eq(MpFactoryProductionVersion::getProductionVersion, param.getProductionVersion()));
    }

    /**
     * 新增定稿表数据
     *
     * @param param 参数
     * @return 定稿数据
     */
    private List<FactoryMonthPlanProductionFinalResult> insertFinalList(FactoryMonthPlanProductionFinalResult param) {
        // 前缀Key
        String prefixKey = IncrementConstant.MONTH_FINAL + com.ruoyi.common.core.utils.DateUtils.dateTimeNow("yyMMdd");
        // 批次号
        String batchNo = String.format("%02d", incrementService.getIncrementNumber(prefixKey));
        // 1.从版本排产结果表：t_mp_moulding_day_result 获取对应年月、版本号的数据存入 t_mp_month_plan_prod_final，自动生成排产单号
        // LAST_MONTH_PLAN_VERSION = MONTH_PLAN_VERSION
        // IS_RELEASE = 未发布
        LambdaQueryWrapper<FactoryMonthPlanMouldDayResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FactoryMonthPlanMouldDayResult::getYear, param.getYear())
                .eq(FactoryMonthPlanMouldDayResult::getMonth, param.getMonth())
                .eq(FactoryMonthPlanMouldDayResult::getFactoryCode, param.getFactoryCode())
                .eq(FactoryMonthPlanMouldDayResult::getMonthPlanVersion, param.getMonthPlanVersion())
                .eq(FactoryMonthPlanMouldDayResult::getProductionVersion, param.getProductionVersion());
        List<FactoryMonthPlanMouldDayResult> dayResultList = resultMapper.selectList(wrapper);
        List<FactoryMonthPlanProductionFinalResult> finalList = new ArrayList<>();
        for (FactoryMonthPlanMouldDayResult dayResult : dayResultList) {
            FactoryMonthPlanProductionFinalResult finalResult = new FactoryMonthPlanProductionFinalResult();
            // 工单号
            String productionNo = incrementService.getBillNoSequenceByExpire(prefixKey + batchNo, 5, 60 * 24 * 7);
            finalResult.setProductionNo(productionNo);
            Field[] fields = ReflectUtil.getFields(BaseEntity.class);
            String[] ignoreFieldArr = Arrays.stream(fields).map(Field::getName).toArray(items -> new String[fields.length]);
            BeanUtil.copyProperties(dayResult, finalResult, ignoreFieldArr);
            finalResult.setBaseVale(null);
            finalResult.setLastMonthPlanVersion(dayResult.getMonthPlanVersion());
            finalResult.setIsRelease(ReleaseStatusEnum.UN_RELEASE.getCode());
            finalResult.setRemark(dayResult.getRemark());
            finalResult.setOriginalTotalQty(dayResult.getTotalQty());
            finalResult.setAdjustQty1(0);
            finalResult.setAdjustQty2(0);
            finalResult.setAdjustQty3(0);
            finalResult.setAdjustQty4(0);
            finalList.add(finalResult);
        }
        // 新增到定稿表
        baseDao.insertBatch(finalList);
        return finalList;
    }

    /**
     * 对定稿版本的结构分配及排产统计进行备份
     * 用以调整后，查询结构分配及排产统计信息还能正确
     *
     * @param param
     */
    private void backUpGroupAllocationInfo(FactoryMonthPlanProductionFinalResult param) {
        resultMapper.deletedOldBackUp(param);
        resultMapper.insertAllocationBackUp(param);
        resultMapper.insertStatisticsBackUp(param);
    }

    /**
     * 发布定稿事件
     *
     * @param param     参数
     * @param finalList 定稿数据
     */
    private void publishFinalizedEvent(FactoryMonthPlanProductionFinalResult param, List<FactoryMonthPlanProductionFinalResult> finalList) {
        MonthPlanFinalizedEventDto eventDto = new MonthPlanFinalizedEventDto();
        // 在内存中按物料编码分组并汇总
        Map<String, Integer> materialTotalQtyMap = new HashMap<>();
        for (FactoryMonthPlanProductionFinalResult result : finalList) {
            String materialCode = result.getMaterialCode();
            Integer totalQty = result.getTotalQty() != null ? result.getTotalQty() : 0;
            materialTotalQtyMap.put(
                    materialCode,
                    materialTotalQtyMap.getOrDefault(materialCode, 0) + totalQty
            );
        }
        eventDto.setFactoryCode(param.getFactoryCode());
        eventDto.setYear(param.getYear());
        eventDto.setMonth(param.getMonth());
        eventDto.setMonthPlanVersion(param.getMonthPlanVersion());
        eventDto.setProductionVersion(param.getProductionVersion());
        eventDto.setMaterialTotalQtyMap(materialTotalQtyMap);
        eventDto.setParam(param);
        eventDto.setFinalList(finalList);
        log.info("发布月计划定稿事件开始 ==> 参数={}", JSON.toJSONString(eventDto));
        // 发布月计划定稿事件
        MonthPlanFinalizedEvent event = new MonthPlanFinalizedEvent(
                this,
                EventModuleTypeEnum.MONTH_PLAN.getCode(),
                SecurityUtils.getUsername(),
                eventDto
        );
        eventPublisher.publish(event);
        log.info("发布月计划定稿事件完成");
    }

    /**
     * 获取定稿版本的月度计划
     *
     * @param param
     * @return
     */
    @Override
    public List<FactoryMonthPlanProductionFinalResult> listMonthProdFinalPlans(FactoryMonthPlanProductionFinalResult param) {
        if (StringUtils.isEmpty(param.getFactoryCode()) || param.getYear() == null || param.getMonth() == null) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.finalized.checkEmptyParam"));
        }
        // 根据分厂编码，及日期，获取定稿版本信息
        MpFactoryProductionVersion version = factoryProductionVersionService.getFinalVersionByYearMonth(param.getFactoryCode(), param.getYear(), param.getMonth());
        if (version == null) {
            return Collections.emptyList();
        }
        param.setMonthPlanVersion(version.getMonthPlanVersion());
        return getDataList(param);
    }

    @Override
    public Map<String, Integer> calculateStructureFrequency(String factoryCode, Set<String> skus) {
        // 获取当前年月
        YearMonth currentYearMonth = YearMonth.now();
        YearMonth startYearMonth = currentYearMonth.minusMonths(12);
        String yearMonth = String.format("%s%02d", startYearMonth.getYear(), startYearMonth.getMonthValue());
        List<FactoryMonthPlanProductionFinalResult> list = Lists.newArrayList();
        final int batchSize = 1000;
        List<String> skuList = new ArrayList<>(skus);
        for (int i = 0; i < skus.size(); i += batchSize) {
            int end = Math.min(i + batchSize, skus.size());
            List<String> batchSkus = skuList.subList(i, end);
            LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> wrapper =
                    Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                            .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, factoryCode)
                            .in(FactoryMonthPlanProductionFinalResult::getMaterialCode, batchSkus)
                            .ge(FactoryMonthPlanProductionFinalResult::getYearMonth, Integer.valueOf(yearMonth))
                            .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
            list.addAll(finalMapper.selectList(wrapper));
        }
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> structureFrequencyMap = Maps.newHashMap();
        Map<String, List<FactoryMonthPlanProductionFinalResult>> map = list.stream().collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getMaterialCode));
        map.forEach((materialCode, value) -> {
            Set<Integer> yearMonths = value.stream().map(FactoryMonthPlanProductionFinalResult::getYearMonth).collect(Collectors.toSet());
            structureFrequencyMap.put(materialCode, yearMonths.size());
        });
        return structureFrequencyMap;
    }

    @Override
    public void listExportData(MpSimulatedResult queryVO, String batchNumber, List<WorksheetData> result) {
        MpFactoryProductionVersion finalProductionVersion = this.getProductionVersionFinalized(queryVO);
        if (null == finalProductionVersion) {
            return;
        }
        LambdaQueryWrapper<MpPredictionDetail> wrapper =
                Wrappers.lambdaQuery(MpPredictionDetail.class)
                        .eq(MpPredictionDetail::getBatchNumber, batchNumber)
                        .eq(MpPredictionDetail::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<MpPredictionDetail> predictionDetailList = this.mpPredictionDetailEntityMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(predictionDetailList)) {
            return;
        }
        List<FactoryMonthPlanMouldDayResult> finalMouldDayResultList = this.getFinalExportData(queryVO, finalProductionVersion);
        if (!CollectionUtils.isEmpty(finalMouldDayResultList)) {
            WorksheetData worksheetData = new WorksheetData();
            worksheetData.setSheetName(String.format(SHEET_NAME, finalProductionVersion.getYear(), finalProductionVersion.getMonth()));
            worksheetData.setMouldDayResults(finalMouldDayResultList);
            result.add(worksheetData);
        }
        List<FactoryMonthPlanMouldDayResult> notFinalMouldDayResultList = this.findNotFinalMouldDayResult(queryVO, finalProductionVersion, predictionDetailList);
        if (!CollectionUtils.isEmpty(notFinalMouldDayResultList)) {
            addNotFinalExportData(notFinalMouldDayResultList, result);
        }
    }

    private void addNotFinalExportData(List<FactoryMonthPlanMouldDayResult> notFinalMouldDayResultList, List<WorksheetData> result) {
        Map<String, List<FactoryMonthPlanMouldDayResult>> map = GroupedMapWithOrder.groupWithOrder(notFinalMouldDayResultList);
        map.forEach((yearMonth, value) -> result.add(this.buildSimulatedResult(value)));
    }

    private WorksheetData buildSimulatedResult(List<FactoryMonthPlanMouldDayResult> value) {
        WorksheetData worksheetData = new WorksheetData();
        FactoryMonthPlanMouldDayResult mouldDayResult = value.get(0);
        worksheetData.setSheetName(String.format(SHEET_NAME, mouldDayResult.getYear(), mouldDayResult.getMonth()));
        worksheetData.setMouldDayResults(value);
        return worksheetData;
    }

    private List<FactoryMonthPlanMouldDayResult> getFinalExportData(MpSimulatedResult queryVO, MpFactoryProductionVersion finalProductionVersion) {
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> wrapper =
                Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                        .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, finalProductionVersion.getFactoryCode())
                        .eq(FactoryMonthPlanProductionFinalResult::getYear, finalProductionVersion.getYear())
                        .eq(FactoryMonthPlanProductionFinalResult::getMonth, finalProductionVersion.getMonth())
                        .eq(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, finalProductionVersion.getMonthPlanVersion())
                        .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, finalProductionVersion.getProductionVersion())
                        .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        if (StringUtils.isNotBlank(queryVO.getStructureName())) {
            wrapper.eq(FactoryMonthPlanProductionFinalResult::getStructureName, queryVO.getStructureName());
        }
        if (StringUtils.isNotBlank(queryVO.getProductTypeCode())) {
            wrapper.eq(FactoryMonthPlanProductionFinalResult::getProductTypeCode, queryVO.getProductTypeCode());
        }
        if (StringUtils.isNotBlank(queryVO.getSpecifications())) {
            wrapper.like(FactoryMonthPlanProductionFinalResult::getSpecifications, queryVO.getSpecifications());
        }
        if (StringUtils.isNotBlank(queryVO.getPattern())) {
            wrapper.like(FactoryMonthPlanProductionFinalResult::getPattern, queryVO.getPattern());
        }
        if (StringUtils.isNotBlank(queryVO.getMainPattern())) {
            wrapper.like(FactoryMonthPlanProductionFinalResult::getMainPattern, queryVO.getMainPattern());
        }
        if (StringUtils.isNotBlank(queryVO.getMaterialCode())) {
            wrapper.like(FactoryMonthPlanProductionFinalResult::getMaterialCode, queryVO.getMaterialCode());
        }
        if (StringUtils.isNotBlank(queryVO.getMaterialDesc())) {
            wrapper.like(FactoryMonthPlanProductionFinalResult::getMaterialDesc, queryVO.getMaterialDesc());
        }
        if (StringUtils.isNotBlank(queryVO.getBrand())) {
            wrapper.eq(FactoryMonthPlanProductionFinalResult::getBrand, queryVO.getBrand());
        }
        List<FactoryMonthPlanProductionFinalResult> list = this.finalMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        List<FactoryMonthPlanMouldDayResult> result = Lists.newArrayList();
        list.forEach(item -> {
            FactoryMonthPlanMouldDayResult entity = BeanCopyUtils.copyBean(item, FactoryMonthPlanMouldDayResult.class);
            result.add(entity);
        });
        return result;
    }

    private List<FactoryMonthPlanMouldDayResult> findNotFinalMouldDayResult(MpSimulatedResult queryVO, MpFactoryProductionVersion finalProductionVersion, List<MpPredictionDetail> predictionDetailList) {
        Set<String> finalProductionVersions = predictionDetailList.stream().map(MpPredictionDetail::getProductionVersion).filter(productionVersion -> !finalProductionVersion.getProductionVersion().equals(productionVersion)).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(finalProductionVersions)) {
            return Collections.emptyList();
        }
        List<FactoryMonthPlanMouldDayResult> list = new ArrayList<>();
        final int batchSize = 1000;
        List<String> versionList = new ArrayList<>(finalProductionVersions);
        for (int i = 0; i < versionList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, versionList.size());
            List<String> batchVersions = versionList.subList(i, end);
            LambdaQueryWrapper<FactoryMonthPlanMouldDayResult> wrapper =
                    Wrappers.lambdaQuery(FactoryMonthPlanMouldDayResult.class)
                            .in(FactoryMonthPlanMouldDayResult::getProductionVersion, batchVersions)
                            .eq(FactoryMonthPlanMouldDayResult::getIsDelete, ApsConstant.APS_YES_NO_0);
            if (StringUtils.isNotBlank(queryVO.getStructureName())) {
                wrapper.eq(FactoryMonthPlanMouldDayResult::getStructureName, queryVO.getStructureName());
            }
            if (StringUtils.isNotBlank(queryVO.getProductTypeCode())) {
                wrapper.eq(FactoryMonthPlanMouldDayResult::getProductTypeCode, queryVO.getProductTypeCode());
            }
            if (StringUtils.isNotBlank(queryVO.getSpecifications())) {
                wrapper.like(FactoryMonthPlanMouldDayResult::getSpecifications, queryVO.getSpecifications());
            }
            if (StringUtils.isNotBlank(queryVO.getPattern())) {
                wrapper.like(FactoryMonthPlanMouldDayResult::getPattern, queryVO.getPattern());
            }
            if (StringUtils.isNotBlank(queryVO.getMainPattern())) {
                wrapper.like(FactoryMonthPlanMouldDayResult::getMainPattern, queryVO.getMainPattern());
            }
            if (StringUtils.isNotBlank(queryVO.getMaterialCode())) {
                wrapper.like(FactoryMonthPlanMouldDayResult::getMaterialCode, queryVO.getMaterialCode());
            }
            if (StringUtils.isNotBlank(queryVO.getMaterialDesc())) {
                wrapper.like(FactoryMonthPlanMouldDayResult::getMaterialDesc, queryVO.getMaterialDesc());
            }
            if (StringUtils.isNotBlank(queryVO.getBrand())) {
                wrapper.eq(FactoryMonthPlanMouldDayResult::getBrand, queryVO.getBrand());
            }
            list.addAll(resultMapper.selectList(wrapper));
        }
        return list;
    }


    private MpFactoryProductionVersion getProductionVersionFinalized(MpSimulatedResult queryVO) {
        List<MpFactoryProductionVersion> finalProductionVersions = factoryProductionVersionMapper.selectList(
                Wrappers.<MpFactoryProductionVersion>lambdaQuery()
                        .eq(MpFactoryProductionVersion::getFactoryCode, queryVO.getFactoryCode())
                        .eq(MpFactoryProductionVersion::getYear, queryVO.getYear())
                        .eq(MpFactoryProductionVersion::getMonth, queryVO.getMonth())
                        .eq(MpFactoryProductionVersion::getIsFinal, YesOrNoEnum.YES.getCode())
        );
        return CollectionUtils.isEmpty(finalProductionVersions) ? null : finalProductionVersions.get(0);
    }

    @Autowired
    private IMesItfService mesItfService;

    @Autowired
    private FactoryMonthPlanProductionFinalResultEntityMapper factoryMonthPlanProductionFinalResultEntityMapper;

    /**
     * 下发月计划
     *
     * @param param 参数
     * @return 结果
     */
    @Override
    public AjaxResult issueMonthPlan(FactoryMonthPlanProductionFinalResult param) {
        // 保证填写完整：年月、分厂、需求计划版本、分厂月计划版本
        if (param.getYear() == null || param.getMonth() == null || StringUtils.isBlank(param.getFactoryCode())
                || StringUtils.isBlank(param.getMonthPlanVersion()) || StringUtils.isBlank(param.getProductionVersion())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.finalized.checkParam"));
        }
        log.info("月计划下发MES参数：年：{}，月：{}，分厂：{}，需求计划版本：{}，分厂月计划版本：{}", param.getYear(), param.getMonth(), param.getFactoryCode(), param.getMonthPlanVersion(), param.getProductionVersion());
        // 查询可发布的数据
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, param.getFactoryCode());
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getYear, param.getYear());
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getMonth, param.getMonth());
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, param.getMonthPlanVersion());
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, param.getProductionVersion());
        wrapper.in(FactoryMonthPlanProductionFinalResult::getIsRelease,
                Arrays.asList(ReleaseStatusEnum.UN_RELEASE.getCode(), ReleaseStatusEnum.RELEASE_FAIL.getCode(),
                        ReleaseStatusEnum.TIME_OUT_FAIL.getCode(), ReleaseStatusEnum.WAIT_RELEASE.getCode()));
        List<FactoryMonthPlanProductionFinalResult> monthPlanProdFinalList = factoryMonthPlanProductionFinalResultEntityMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(monthPlanProdFinalList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.finalized.noData"));
        }
        // 更新发布状态=发布中
        LambdaUpdateWrapper<FactoryMonthPlanProductionFinalResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .eq(FactoryMonthPlanProductionFinalResult::getYear, param.getYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, param.getMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, param.getMonthPlanVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, param.getProductionVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, param.getFactoryCode())
                .set(FactoryMonthPlanProductionFinalResult::getIsRelease, ReleaseStatusEnum.RELEASING.getCode());
        factoryMonthPlanProductionFinalResultEntityMapper.update(null, updateWrapper);
        log.info("月计划下发MES更新发布状态->发布中");
        AjaxResult ajaxResult = mesItfService.issueMonthPlan(monthPlanProdFinalList);
        if (AjaxResultUtils.checkAjaxError(ajaxResult)) {
            throw new RuntimeException(String.valueOf(ajaxResult.get(AjaxResult.MSG_TAG)));
        }
        // 更新发布状态=已发布
        updateWrapper.clear();
        updateWrapper
                .eq(FactoryMonthPlanProductionFinalResult::getYear, param.getYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, param.getMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, param.getMonthPlanVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, param.getProductionVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, param.getFactoryCode())
                .set(FactoryMonthPlanProductionFinalResult::getIsRelease, ReleaseStatusEnum.RELEASE.getCode());
        factoryMonthPlanProductionFinalResultEntityMapper.update(null, updateWrapper);
        log.info("月计划下发MES更新发布状态->已发布");
        return AjaxResult.success();
    }

    /**
     * 导入定稿
     *
     * @param list          列表数据
     * @param updateSupport 覆盖
     * @param importLogId   导入日志ID
     * @param params        导入参数
     * @return 结果
     */
    @Override
    public AjaxResult importDataFinalResult(List<FactoryMonthPlanProductionFinalResult> list, boolean updateSupport,
                                            Long importLogId, FactoryMonthPlanProductionFinalResult params) {
        // 1、加载定稿版本，只加载指定结构
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, params.getFactoryCode());
        queryWrapper.eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, params.getProductionVersion());
        queryWrapper.eq(FactoryMonthPlanProductionFinalResult::getStructureName, params.getStructureName());
        List<FactoryMonthPlanProductionFinalResult> finalList = factoryMonthPlanProductionFinalResultEntityMapper
                .selectList(queryWrapper);

        // 2、把导入数据合并到原版本中
        List<FactoryMonthPlanProductionFinalResult> updateList = new ArrayList<>();
        Map<String, FactoryMonthPlanProductionFinalResult> importMap = list.stream().collect(Collectors.toMap(FactoryMonthPlanProductionFinalResult::getMaterialCode, Function.identity(), (p1, p2) -> p1));
        int successNum = 0;
        for (FactoryMonthPlanProductionFinalResult finalResult : finalList) {
            String materialCode = finalResult.getMaterialCode();
            FactoryMonthPlanProductionFinalResult finalImport = importMap.get(materialCode);
            if (finalImport == null) {
                continue;
            }
            boolean isChange = false;
            for (int day = FactoryConstant.MONTH_START_DAY; day < FactoryConstant.MONTH_MAX_DAY; day++) {
                String fieldName = FactoryConstant.DAY_FIELD + day;
                int oldValue = intValue(finalResult.getFieldValueByFieldName(fieldName));
                int newValue = intValue(finalImport.getFieldValueByFieldName(fieldName));
                if (oldValue != newValue) {
                    if (newValue > 0) {
                        finalResult.setFieldValueByFieldName(fieldName, newValue);
                    } else {
                        finalResult.setFieldValueByFieldName(fieldName, null);
                    }
                    isChange = true;
                    successNum++;
                }
            }
            if (isChange) { // 如果有有更新，需要重算部分栏位并添加到更新列表中
                finalResult.statisticsTotalQty();
                finalResult.setBaseVale(finalResult.getId());
                updateList.add(finalResult);
            }
        }
        baseDao.updateBatch(updateList);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 根据条件，列表查询
     *
     * @param condition 查询条件
     * @return 结果
     */
    @Override
    public List<FactoryMonthPlanFinalAdjustVo> list4Adjust(FactoryMonthPlanProductionFinalResult condition) {
        // 调整列表以传入的调整版本号优先匹配；没有调整版本号时使用排产版本号匹配。
        String matchVersion = StringUtils.defaultIfBlank(condition.getVersion(), condition.getProductionVersion());
        List<FactoryMonthPlanFinalAdjustVo> dataList = this.finalMapper.list4Adjust(condition);
        // 设置是否特殊材料
        setSpecialMaterial(condition.getFactoryCode(), dataList);
        // 先放入定稿数据，保证定稿独有数据不会丢失，并保留原列表顺序。
        Map<String, FactoryMonthPlanFinalAdjustVo> finalResultMap = new LinkedHashMap<>();
        if (CollectionUtils.isNotEmpty(dataList)) {
            FactoryMonthPlanFinalAdjustVo firstAdjust = dataList.get(0);
            if (Objects.equals(condition.getVersion(), firstAdjust.getProductionVersion())) {
                condition.setVersion(firstAdjust.getLastMonthPlanVersion()); // 如果查询版本是生产版本号，则把版本号更新成月计划版本再继续往下处理
            }
            condition.setLastMonthPlanVersion(firstAdjust.getLastMonthPlanVersion());
            condition.setProductionVersion(firstAdjust.getProductionVersion());
            condition.setProductTypeCode(firstAdjust.getProductTypeCode());
            for (FactoryMonthPlanFinalAdjustVo adjustVo : dataList) {
                finalResultMap.putIfAbsent(buildAdjustMapKey(matchVersion, adjustVo), adjustVo);
            }
        } else {
            FactoryMonthPlanProductionFinalResult param = new FactoryMonthPlanProductionFinalResult();
            param.setYear(condition.getYear());
            param.setMonth(condition.getMonth());
            List<FactoryMonthPlanFinalAdjustVo> versionList = this.finalMapper.list4Adjust(param);
            if (CollectionUtils.isNotEmpty(versionList)) {
                FactoryMonthPlanFinalAdjustVo firstAdjust = versionList.get(0);
                condition.setLastMonthPlanVersion(firstAdjust.getLastMonthPlanVersion());
                condition.setProductionVersion(firstAdjust.getProductionVersion());
                condition.setProductTypeCode(firstAdjust.getProductTypeCode());
            }
        }

        // 查询调整
        LambdaQueryWrapper<MpAdjustResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpAdjustResult::getFactoryCode, condition.getFactoryCode());
        queryWrapper.eq(MpAdjustResult::getYear, condition.getYear());
        queryWrapper.eq(MpAdjustResult::getMonth, condition.getMonth());
        // 前端传入 version 时表示调整版本号，对应调整结果表 VERSION 字段。
        queryWrapper.eq(StringUtils.isNotBlank(matchVersion), MpAdjustResult::getVersion, matchVersion);
        queryWrapper.like(StringUtils.isNotBlank(condition.getCxMachineCode()), MpAdjustResult::getCxMachineCode, condition.getCxMachineCode());
        queryWrapper.like(StringUtils.isNotBlank(condition.getStructureName()), MpAdjustResult::getStructureName, condition.getStructureName());
        queryWrapper.like(StringUtils.isNotBlank(condition.getMaterialCode()), MpAdjustResult::getMaterialCode, condition.getMaterialCode());
        queryWrapper.like(StringUtils.isNotBlank(condition.getMaterialDesc()), MpAdjustResult::getMaterialDesc, condition.getMaterialDesc());
        queryWrapper.eq(MpAdjustResult::getIsDelete, YesOrNoEnum.NO.getCode());
        List<MpAdjustResult> mpAdjustResultList = mpAdjustResultEntityMapper.selectList(queryWrapper);

        // 加载月计划版本
        Map<String, DpDemandPlanSum> demandPlanSumMap = new HashMap<>(this.buildDemandPlanSumMap(condition, matchVersion));
        // 尝试额外加载结构内的最新版本对应的需求计划，如果版本不一样，需要关联出新物料并且并入需求计划列表中
        LambdaQueryWrapper<MpAdjustStructureIn> adjustStructureInQueryWrapper = new LambdaQueryWrapper<>();
        adjustStructureInQueryWrapper.select(MpAdjustStructureIn::getVersion);
        adjustStructureInQueryWrapper.groupBy(Arrays.asList(MpAdjustStructureIn::getVersion));
        adjustStructureInQueryWrapper.eq(MpAdjustStructureIn::getFactoryCode, condition.getFactoryCode());
        adjustStructureInQueryWrapper.eq(MpAdjustStructureIn::getYear, condition.getYear());
        adjustStructureInQueryWrapper.eq(MpAdjustStructureIn::getMonth, condition.getMonth());
        String newVersion = mpAdjustStructureInEntityMapper.selectList(adjustStructureInQueryWrapper).stream().map(MpAdjustStructureIn::getVersion).max(String::compareTo).orElse(null);
        if (StringUtils.isNotEmpty(newVersion) && !Objects.equals(newVersion, matchVersion)) {
            // 两个版本的数据统一合并至demandPlanSumMap
            Map<String, DpDemandPlanSum> dpDemandPlanSumMap = this.buildDemandPlanSumMap(condition, newVersion);
            if (PubUtil.isNotEmpty(dpDemandPlanSumMap)) {
                dpDemandPlanSumMap.values().stream().forEach(plan -> {
                    demandPlanSumMap.putIfAbsent(this.buildDemandPlanSumMapKey(matchVersion, plan.getMaterialCode()), plan);
                });
            }
        }

        Map<String, MpAdjustResult> mpAdjustResultMap = new LinkedHashMap<>();
        if (CollectionUtils.isNotEmpty(mpAdjustResultList)) {
            for (MpAdjustResult mpAdjustResult : mpAdjustResultList) {
                mpAdjustResultMap.putIfAbsent(buildAdjustMapKey(matchVersion, mpAdjustResult), mpAdjustResult);
            }
        }
        for (Map.Entry<String, MpAdjustResult> entry : mpAdjustResultMap.entrySet()) {
            FactoryMonthPlanFinalAdjustVo adjustVo = finalResultMap.get(entry.getKey());
            if (adjustVo == null) {
                adjustVo = new FactoryMonthPlanFinalAdjustVo();
                finalResultMap.put(entry.getKey(), adjustVo);
                // 相同业务Key时以调整结果为准；调整独有数据转换为同一VO后追加返回。
                BeanUtil.copyProperties(entry.getValue(), adjustVo);
            } else {
                BeanUtil.copyProperties(entry.getValue(), adjustVo);
            }
            MpAdjustResult value = entry.getValue();
            String defaultVersion = StringUtils.defaultIfBlank(value.getLastMonthPlanVersion(), value.getLastMonthPlanVersion());
            fillDemandQty(adjustVo, demandPlanSumMap.get(this.buildDemandPlanSumMapKey(defaultVersion, entry.getValue().getMaterialCode())));
            // 日硫化量固定*2
            /*int dayVulcanizationQty = adjustVo.getDayVulcanizationQty() == null ? 0 : adjustVo.getDayVulcanizationQty();
            adjustVo.setDayVulcanizationQty(dayVulcanizationQty * 2);*/
        }

        // 尝试把试制量试、以及有订单的SKU补充到列表中
        Map<String, FactoryMonthPlanFinalAdjustVo> copyResultMap = finalResultMap.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        // 加载结构转产表的结构清单
        Map<String, String> structureAllocationMap = this.loadStructureAllocationMap(condition);
        addStructureAlloction(structureAllocationMap);

        // 补充试制量试计划SKU
        Map<String, FactoryMonthPlanFinalAdjustVo> trialResultMap = this.fillTrialPlanSku(matchVersion, condition, copyResultMap);
        // 补充有需求计划但是没有定稿、没有调整的SKU
        Map<String, FactoryMonthPlanFinalAdjustVo> noPlanResultMap = this.fillNoPlanDemandPlanSku(matchVersion, demandPlanSumMap, copyResultMap, condition, structureAllocationMap);
        // 合并两种新增的SKU
        Map<String, FactoryMonthPlanFinalAdjustVo> newSkuResultMap = new HashMap<>();
        newSkuResultMap.putAll(trialResultMap);
        newSkuResultMap.putAll(noPlanResultMap);
        // 新增SKU填充必要栏位
        this.fillMonthPlanMouldResult(condition, demandPlanSumMap, newSkuResultMap, structureAllocationMap);
        finalResultMap.putAll(newSkuResultMap);
        List<FactoryMonthPlanFinalAdjustVo> resultList = new ArrayList<>(finalResultMap.values());

        if (CollectionUtils.isNotEmpty(resultList)) {
            //20260605+ 补充待调整量
            Map<String, MpSkuAdjustInfoVo> skuAdjustInfoMap = mpSkuAdjustInfoService.getPendingQtyInfo(condition, matchVersion);
            if (!org.springframework.util.CollectionUtils.isEmpty(skuAdjustInfoMap)) {
                resultList.forEach(single -> {
                    String groupKey = single.getPendingQtyKey();
                    MpSkuAdjustInfoVo info = skuAdjustInfoMap.get(groupKey);
                    if (null == info) {
                        return;
                    }
                    single.setPendingQty(info.getPendingQty());
                });
            }
            Locale language = SecurityUtils.getUserLang();
            JsonUtils.parseJsonRemarkList(resultList, language.toString(), "reason");
        }
        return resultList;
    }

    /**
     * 增加新增结构
     *
     * @param structureAllocationMap
     */
    private void addStructureAlloction(Map<String, String> structureAllocationMap) {
       /* if (PubUtil.isEmpty(structureAllocationMap)){
            return;
        }*/
        //补充新增结构的机台信息
        AdjustsCxMachineVo cxMachineVo = mpStructureAllocationService.getAdjustsCxMachineFromRedis();
        if (cxMachineVo != null && structureAllocationMap.get(cxMachineVo.getStructureName()) == null) {
            MpStructureAllocation newStructureAlloction = new MpStructureAllocation();
            newStructureAlloction.setStructureName(cxMachineVo.getStructureName());
            newStructureAlloction.setCxMachineCode(cxMachineVo.getCxMachineCode());
            structureAllocationMap.put(cxMachineVo.getStructureName(), cxMachineVo.getCxMachineCode());
        }
    }

    /**
     * 加载结构与成型机台的对应关系
     *
     * @param condition
     * @return
     */
    protected Map<String, String> loadStructureAllocationMap(FactoryMonthPlanProductionFinalResult condition) {
        LambdaQueryWrapper<MpStructureAllocation> structureAllocationQueryWrapper = new LambdaQueryWrapper<>();
        structureAllocationQueryWrapper.eq(MpStructureAllocation::getFactoryCode, condition.getFactoryCode());
        structureAllocationQueryWrapper.eq(MpStructureAllocation::getProductionVersion, condition.getProductionVersion());
        structureAllocationQueryWrapper.eq(StringUtils.isNotEmpty(condition.getStructureName()), MpStructureAllocation::getStructureName, condition.getStructureName());

        List<MpStructureAllocation> allocationList = mpStructureAllocationEntityMapper.selectList(structureAllocationQueryWrapper);
        return allocationList.stream()
                .collect(Collectors.groupingBy(MpStructureAllocation::getStructureName,
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream().map(MpStructureAllocation::getCxMachineCode).distinct()
                                        .sorted(String::compareTo).collect(Collectors.joining(",")))));
    }

    /**
     * 填充导入月计划数据的关联栏位
     *
     * @param insertList
     */
    private void fillMonthPlanMouldResult(FactoryMonthPlanProductionFinalResult condition,
                                          Map<String, DpDemandPlanSum> demandPlanSumMap,
                                          Map<String, FactoryMonthPlanFinalAdjustVo> resultMap,
                                          Map<String, String> structureAllocationMap) {
        if (resultMap.isEmpty()) {
            return;
        }
        Map<String, DpDemandPlanSum> demandPlanMap = demandPlanSumMap.values().stream().collect(Collectors.toMap(DpDemandPlanSum::getMaterialCode, Function.identity(), (p1, p2) -> p1));
        List<Entry<String, FactoryMonthPlanFinalAdjustVo>> insertList = new ArrayList<>(resultMap.entrySet());
        Set<String> materialCodeSet = resultMap.values().stream().map(FactoryMonthPlanFinalAdjustVo::getMaterialCode).distinct().collect(Collectors.toSet());
        // 计划类型、产品品类、MES物料编码、产品分类、排产分类、规格、花纹、品牌、SUM(高优先级数量)、月均销量、库销比、SUM(生产需求计划)、SUM(实际生产需求（含损耗）)、结构类型 --- 数据源：需求计划
        String monthPlanVersion = condition.getLastMonthPlanVersion();
        String productionVersion = condition.getProductionVersion();
        String factoryCode = condition.getFactoryCode();
        String productTypeCode = condition.getProductTypeCode();
        Integer year = condition.getYear();
        Integer month = condition.getMonth();
        Integer yearMonth = Convert.toInt(String.format("%s%02d", year, month));

        // 加载sku与施工关系，数据格式：map<物料号, map<施工阶段, 施工列表>
        LambdaQueryWrapper<MdmSkuConstructionRef> skuConstructionRefQueryWrapper = new LambdaQueryWrapper<>();
        skuConstructionRefQueryWrapper.eq(MdmSkuConstructionRef::getFactoryCode, factoryCode);
        skuConstructionRefQueryWrapper.in(MdmSkuConstructionRef::getMaterialCode, materialCodeSet);
        Map<String, Map<String, List<MdmSkuConstructionRef>>> constructionInfoMap = mdmSkuConstructionRefEntityMapper
                .selectList(skuConstructionRefQueryWrapper).stream()
                .collect(Collectors.groupingBy(MdmSkuConstructionRef::getMaterialCode,
                        Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                                .collect(Collectors.groupingBy(i -> this.transferTrialStatusToStage(i.getTrialStatus()))))));
        // 日硫化量获取
        DayVulcanizationModeEnum mode = null;
        FactoryParam dayVulcanizationParam = factoryParamService.getFactoryParamByCondition(factoryCode,
                productTypeCode, Collections.singletonList(MonthPlanEnums.DAY_VULCANIZATION_MODE.getCode())).get(0);
        if (dayVulcanizationParam != null) {
            String dayVulcanizationCode = StringUtils.isNoneEmpty(dayVulcanizationParam.getParamValue()) ? dayVulcanizationParam.getParamValue() : dayVulcanizationParam.getDefauleValue();
            mode = DayVulcanizationModeEnum.getInstance(dayVulcanizationCode);
        } else {
            mode = DayVulcanizationModeEnum.STANDARD_CAPACITY;
        }
        LambdaQueryWrapper<MdmSkuLhCapacity> skuLhCapacityQueryWrapper = new LambdaQueryWrapper<>();
        skuLhCapacityQueryWrapper.eq(MdmSkuLhCapacity::getFactoryCode, factoryCode);
        skuLhCapacityQueryWrapper.in(MdmSkuLhCapacity::getMaterialCode, materialCodeSet);
        Map<String, MdmSkuLhCapacity> productLhCapacityMap = mdmSkuLhCapacityEntityMapper
                .selectList(skuLhCapacityQueryWrapper).stream().collect(Collectors
                        .toMap(MdmSkuLhCapacity::getMaterialCode, Function.identity(), (m1, m2) -> m1));

        // 加载型腔活块数
        Map<String, Integer> cavityResults = new HashMap<>(0); // 型腔可用量（按结构+主花纹分组）
        Map<String, Integer> insertResults = new HashMap<>(0); // 活块可用量（按物料描述分组）
        List<DailyMouldAvailabilityResult> moldResult = moldCavityInsertMaxValueCalculator
                .moldCavityInsertMaxValueCalculator(year, month, factoryCode,
                        null, null, true);
        if (!CollectionUtils.isEmpty(moldResult)) {
            cavityResults = moldResult.get(0).getCavityResults();
            insertResults = moldResult.get(0).getInsertResults();
        }

        for (Entry<String, FactoryMonthPlanFinalAdjustVo> entry : insertList) {
            FactoryMonthPlanFinalAdjustVo insertItem = entry.getValue();
            String materialCode = insertItem.getMaterialCode();
            DpDemandPlanSum demandPlan = demandPlanMap.get(materialCode);
            String structureName = demandPlan != null ? demandPlan.getStructureName() : insertItem.getStructureName();
            String materialDesc = demandPlan != null ? demandPlan.getMaterialDesc() : insertItem.getMaterialDesc();
            // 成型机条件过滤
            String cxMachineCode = structureAllocationMap.get(structureName);
            if (StringUtils.isNotEmpty(condition.getCxMachineCode())) {
                if (StringUtils.isEmpty(cxMachineCode) || !cxMachineCode.contains(condition.getCxMachineCode())) {
                    structureAllocationMap.remove(entry.getKey());
                    continue;
                }
            }
            insertItem.setCxMachineCode(cxMachineCode);
            insertItem.setMonthPlanVersion(monthPlanVersion);
            insertItem.setLastMonthPlanVersion(monthPlanVersion);
            insertItem.setProductionVersion(productionVersion);
            insertItem.setStructureName(structureName);
            insertItem.setMaterialDesc(materialDesc);
            insertItem.setFactoryCode(factoryCode);
            insertItem.setYear(year);
            insertItem.setMonth(month);
            insertItem.setProductTypeCode(productTypeCode);
            insertItem.setBeginDay(0);
            insertItem.setEndDay(0);
            if (demandPlan != null) {
                insertItem.setMesMaterialCode(demandPlan.getMesMaterialCode());
                insertItem.setProductTypeCode(demandPlan.getProductTypeCode());
                insertItem.setSpecifications(demandPlan.getSpecifications());
                insertItem.setPattern(demandPlan.getPattern());
                insertItem.setBrand(demandPlan.getBrand());
                insertItem.setHeightQty(demandPlan.getHeightQty());
                insertItem.setConventionReserveQty(demandPlan.getConventionReserveQty());
                insertItem.setPostponeQty(demandPlan.getPostponeQty());
                insertItem.setAverageSaleQty(demandPlan.getAverageSaleQty());
                insertItem.setProdReqPlan(demandPlan.getNetQty());
                insertItem.setStructureType(demandPlan.getStructureType());
                insertItem.setMainPattern(demandPlan.getMainPattern());
                // 计算库销比
                insertItem.setInventorySalesRatio(BigDecimalUtils.div(demandPlan.getStockQty(), demandPlan.getAverageSaleQty(), 1));
            }
            // 胎胚号、施工阶段、是否零度材料、制造示方书号、文字示方书号、硫化示方书号---数据源：SKU与示方书关系，关联：SKU+胎胚描述
            Map<String, List<MdmSkuConstructionRef>> constructionStatusGroup = constructionInfoMap.get(materialCode);
            if (constructionStatusGroup != null) {
                List<MdmSkuConstructionRef> constructionConfigurationList = constructionStatusGroup.get(insertItem.getConstructionStage());
                if (!CollectionUtils.isEmpty(constructionConfigurationList)) {
                    MdmSkuConstructionRef constructionInfo = constructionConfigurationList.get(0);
                    insertItem.setEmbryoCode(constructionInfo.getEmbryoCode());
                    insertItem.setIsZeroRack(constructionInfo.getIsZeroRack());
                    insertItem.setEmbryoNo(constructionInfo.getEmbryoNo());
                    insertItem.setTextNo(constructionInfo.getTextNo());
                    insertItem.setLhNo(constructionInfo.getLhNo());
                    insertItem.setProductStatus(constructionInfo.getTrialStatus());
                    insertItem.setMainMaterialDesc(constructionInfo.getMainMaterialDesc());
                }
            }
            // 日硫化量（单模），单条硫化时间---数据源：SKU双模日硫化量， 日标准产量/2，硫化总时间(s)
            MdmSkuLhCapacity mdmSkuLhCapacity = productLhCapacityMap.get(materialCode);
            if (mdmSkuLhCapacity != null) {
                MonthPlanProductLhCapacityVo capacityVo = new MonthPlanProductLhCapacityVo();
                capacityVo.setMesCapacity(mdmSkuLhCapacity.getMesCapacity());
                capacityVo.setStandardCapacity(mdmSkuLhCapacity.getStandardCapacity());
                capacityVo.setApsCapacity(mdmSkuLhCapacity.getApsCapacity());
                capacityVo.calculateDayVulcanizationQty(mode);
                if (capacityVo.getDayVulcanizationQty() != null) {
                    insertItem.setDayVulcanizationQty(capacityVo.getDayVulcanizationQty() / 2);
                }
            }

            // 英寸---根据结构名称解析
            if (!StringUtil.isEmptyWithTrim(structureName)) {
                // 正则：R后面跟数字（可能带小数点）
                Pattern pattern = Pattern.compile("R\\d+(?:\\.\\d+)?");
                Matcher matcher = pattern.matcher(structureName);
                String proSize = matcher.find() ? matcher.group() : "";
                insertItem.setProSize(proSize);
            }

            // 型腔数量---同结构主花纹最大的型腔数量
            insertItem.setMouldCavityQty(cavityResults.getOrDefault(insertItem.getStructureName() + insertItem.getMainPattern(), 0));
            insertItem.setMaxMouldCavityQty(insertItem.getMouldCavityQty());
            insertItem.setTypeBlockQty(insertResults.getOrDefault(materialDesc, 0));
            insertItem.setYearMonth(yearMonth);
        }

        this.setSpecialMaterial(factoryCode, new ArrayList<>(resultMap.values()));
    }

    /**
     * 补充试制量试计划
     *
     * @param condition
     * @param matchVersion
     * @param finalResultMap
     */
    protected Map<String, FactoryMonthPlanFinalAdjustVo> fillTrialPlanSku(String matchVersion,
                                                                          FactoryMonthPlanProductionFinalResult condition,
                                                                          Map<String, FactoryMonthPlanFinalAdjustVo> finalResultMap) {
        Map<String, FactoryMonthPlanFinalAdjustVo> resultMap = new HashMap<>();
        LambdaQueryWrapper<MpTrialPlan> mpTrialPlanQueryWrapper = new LambdaQueryWrapper<>();
        mpTrialPlanQueryWrapper.eq(MpTrialPlan::getFactoryCode, condition.getFactoryCode());
        mpTrialPlanQueryWrapper.eq(MpTrialPlan::getYear, condition.getYear());
        mpTrialPlanQueryWrapper.eq(MpTrialPlan::getMonth, condition.getMonth());
        mpTrialPlanQueryWrapper.like(StringUtils.isNotEmpty(condition.getMaterialCode()), MpTrialPlan::getMaterialCode, condition.getMaterialCode());
        mpTrialPlanQueryWrapper.like(StringUtils.isNotEmpty(condition.getMaterialDesc()), MpTrialPlan::getMaterialDesc, condition.getMaterialDesc());
        mpTrialPlanQueryWrapper.isNull(MpTrialPlan::getProductionDate);
        List<MpTrialPlan> trialPlanList = mpTrialPlanEntityMapper.selectList(mpTrialPlanQueryWrapper);
        if (CollectionUtils.isEmpty(trialPlanList)) {
            return resultMap;
        }

        // 加载SKU与结构关系
        List<String> materialCodeList = trialPlanList.stream().map(MpTrialPlan::getMaterialCode).distinct().collect(Collectors.toList());
        LambdaQueryWrapper<MdmMaterialInfo> mdmMaterialInfoQueryWrapper = new LambdaQueryWrapper<>();
        mdmMaterialInfoQueryWrapper.eq(MdmMaterialInfo::getFactoryCode, condition.getFactoryCode());
        mdmMaterialInfoQueryWrapper.in(MdmMaterialInfo::getMaterialCode, materialCodeList);
//        mdmMaterialInfoQueryWrapper.isNotNull(MdmMaterialInfo::getStructureName);
        Map<String, MdmMaterialInfo> materialInfoMap = mdmMaterialInfoEntityMapper
                .selectList(mdmMaterialInfoQueryWrapper).stream()
                .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, Function.identity(), (m1, m2) -> m1));

        // 加载周期结构
        LambdaQueryWrapper<MdmCycleSchStruConf> mdmCycleSchStruConfQueryWrapper = new LambdaQueryWrapper<>();
        mdmCycleSchStruConfQueryWrapper.eq(MdmCycleSchStruConf::getFactoryCode, condition.getFactoryCode());
        Set<String> cycleSchStruSet = mdmCycleSchStruConfEntityMapper.selectList(mdmCycleSchStruConfQueryWrapper).stream().map(MdmCycleSchStruConf::getStructureName).distinct().collect(Collectors.toSet());

        for (MpTrialPlan trialPlan : trialPlanList) {
            String materialCode = trialPlan.getMaterialCode();
            // 产品状态与施工类型映射
            String constructionStage = this.transferTrialStatusToStage(trialPlan.getTrialStatus());
            String matchKey = matchVersion + "|" + materialCode + "|" + constructionStage;
            FactoryMonthPlanFinalAdjustVo adjustVo = finalResultMap.get(matchKey);
            if (adjustVo != null) {
                continue;
            }
            MdmMaterialInfo mdmMaterialInfo = materialInfoMap.get(materialCode);
            FactoryMonthPlanFinalAdjustVo noPlanVo = new FactoryMonthPlanFinalAdjustVo();
            noPlanVo.setMaterialCode(materialCode);
            noPlanVo.setMaterialDesc(trialPlan.getMaterialDesc());
            // 物料基础信息
            if (mdmMaterialInfo != null) {
                noPlanVo.setStructureName(mdmMaterialInfo.getStructureName());
                noPlanVo.setMesMaterialCode(mdmMaterialInfo.getMesMaterialCode());
                noPlanVo.setProductTypeCode(mdmMaterialInfo.getProductTypeCode());
                noPlanVo.setSpecifications(mdmMaterialInfo.getSpecifications());
                noPlanVo.setPattern(mdmMaterialInfo.getPattern());
                noPlanVo.setBrand(mdmMaterialInfo.getBrand());
                noPlanVo.setMainPattern(mdmMaterialInfo.getMainPattern());
            }

            // 过滤条件有结构的，结构要匹配上
            if (StringUtils.isNotEmpty(condition.getStructureName())
                    && !Objects.equals(condition.getStructureName(), noPlanVo.getStructureName())) {
                continue;
            }
            // 结构类型
            String structureType;
            if (CollectionUtils.isNotEmpty(cycleSchStruSet) && cycleSchStruSet.contains(noPlanVo.getStructureName())) {
                structureType = ProductionGroupTypeEnum.CYCLE.getGroupType();
            } else {
                structureType = ProductionGroupTypeEnum.CONVENTION.getGroupType();
            }
            noPlanVo.setStructureType(structureType);

            noPlanVo.setConstructionStage(constructionStage);
            finalResultMap.put(matchKey, noPlanVo);
            resultMap.put(matchKey, noPlanVo);
        }
        return resultMap;
    }

    /**
     * 产品状态与施工类型映射
     *
     * @param trialStatus
     * @return
     */
    protected String transferTrialStatusToStage(String trialStatus) {
        String constructionStage;
        if (com.zlt.aps.enums.ConstructionStageEnum.TRIAL_FLAG.equals(trialStatus)) {
            constructionStage = ConstructionStageEnum.MASS_TRIAL.getCode();
        } else if (com.zlt.aps.enums.ConstructionStageEnum.MEASUREMENT_FLAG.equals(trialStatus)) {
            constructionStage = ConstructionStageEnum.TRIAL.getCode();
        } else if (com.zlt.aps.enums.ConstructionStageEnum.FORMAL_FLAG.equals(trialStatus)) {
            constructionStage = ConstructionStageEnum.FORMAL.getCode();
        } else {
            constructionStage = ConstructionStageEnum.NO_PROCESS.getCode();
        }
        return constructionStage;
    }

    /**
     * 补充有需求计划但是没有定稿、没有调整的sku
     *
     * @param matchVersion
     * @param demandPlanSumMap
     * @param resultList
     * @param condition
     * @param structureAllocationMap
     */
    private Map<String, FactoryMonthPlanFinalAdjustVo> fillNoPlanDemandPlanSku(String matchVersion,
                                                                               Map<String, DpDemandPlanSum> demandPlanSumMap,
                                                                               Map<String, FactoryMonthPlanFinalAdjustVo> finalResultMap,
                                                                               FactoryMonthPlanProductionFinalResult condition,
                                                                               Map<String, String> structureAllocationMap) {

        Map<String, FactoryMonthPlanFinalAdjustVo> resultMap = new HashMap<>();
        for (DpDemandPlanSum demandPlan : demandPlanSumMap.values()) {
            String materialCode = demandPlan.getMaterialCode();
            String constructionStage = ConstructionStageEnum.FORMAL.getCode();
            String matchKey = matchVersion + "|" + materialCode + "|" + constructionStage;
            if (!structureAllocationMap.containsKey(demandPlan.getStructureName())) {
                continue;
            }
            if (finalResultMap.containsKey(matchKey)) {
                continue;
            }
            FactoryMonthPlanFinalAdjustVo noPlanVo = new FactoryMonthPlanFinalAdjustVo();
            noPlanVo.setMaterialCode(materialCode);
            noPlanVo.setMonthPlanVersion(demandPlan.getMonthPlanVersion());
            noPlanVo.setConstructionStage(constructionStage);
            this.fillDemandQty(noPlanVo, demandPlan);
            finalResultMap.put(matchKey, noPlanVo);
            resultMap.put(matchKey, noPlanVo);
        }
        return resultMap;
    }

    /**
     * 设置是否特殊材料
     *
     * @param factoryCode       分厂编号
     * @param mpFinalAdjustList 定稿列表
     */
    public void setSpecialMaterial(String factoryCode, List<FactoryMonthPlanFinalAdjustVo> mpFinalAdjustList) {
        if (com.zlt.aps.mp.common.utils.PubUtil.isEmpty(mpFinalAdjustList)) {
            return;
        }

        // 创建计时器
        StopWatch watch = new StopWatch();
        watch.start();

        // 查询BOM物料消耗明细
        CompletableFuture<List<MdmMaterialConsumeDetail>> materialConsumeDetailFuture = CompletableFuture.supplyAsync(
                () -> queryMaterialConsumeDetailList(factoryCode)
        );
        // 查询特殊材料记录
        CompletableFuture<List<RawSpecialMaterialRecord>> rawSpecialMaterialRecordFuture = CompletableFuture.supplyAsync(
                () -> querySpecialMaterialRecordList(factoryCode)
        );

        try {
            // 等待所有异步任务执行完成
            CompletableFuture.allOf(
                    materialConsumeDetailFuture,
                    rawSpecialMaterialRecordFuture
            ).join();

            log.info("设置是否特殊材料 ==> 并行查询数据执行完成");

        } catch (CompletionException e) {
            // 异常处理
            Throwable throwable = e.getCause();
            log.error("查询数据失败! 失败原因:{}", throwable.getMessage(), throwable);
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.initDataFailure"), throwable);
        } finally {
            watch.stop();
        }

        List<MdmMaterialConsumeDetail> mdmMaterialConsumeDetailList = materialConsumeDetailFuture.join();
        List<RawSpecialMaterialRecord> specialMaterialList = rawSpecialMaterialRecordFuture.join();

        for (FactoryMonthPlanFinalAdjustVo monthPlan : mpFinalAdjustList) {
            // 设置是否含有特殊材料
            boolean isHasSpecialMaterial = rawSpecialMaterialRecordService.hasSpecialMaterial(monthPlan.getEmbryoCode(), mdmMaterialConsumeDetailList, specialMaterialList);
            monthPlan.setHasSpecialMaterial(isHasSpecialMaterial ? ApsConstant.TRUE : ApsConstant.FALSE);
        }
    }

    /**
     * 查询BOM物料消耗明细
     *
     * @param factoryCode 分厂编号
     */
    private List<MdmMaterialConsumeDetail> queryMaterialConsumeDetailList(String factoryCode) {
        MdmMaterialConsumeDetail queryVO = new MdmMaterialConsumeDetail();
        queryVO.setFactoryCode(factoryCode);

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.TRUE);
        return dataManager.listMaterialConsumeDetails(dataDTO);
    }

    /**
     * 查询特殊材料记录
     *
     * @param factoryCode 分厂编号
     */
    private List<RawSpecialMaterialRecord> querySpecialMaterialRecordList(String factoryCode) {
        RawSpecialMaterialRecord queryVO = new RawSpecialMaterialRecord();
        queryVO.setFactoryCode(factoryCode);

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.TRUE);
        return dataManager.listSpecialMaterials(dataDTO);
    }

    /**
     * 构建月计划调整结果匹配键，用于定稿结果与调整结果按同一业务维度合并。
     *
     * @param matchVersion 查询传入的调整版本号，未传调整版本号时使用排产版本号
     * @param item         定稿结果或调整结果数据
     * @return 版本号、物料编码、施工阶段组成的匹配键
     */
    private String buildAdjustMapKey(String matchVersion, IFinalAndAdjustResultInterface item) {
        return matchVersion + "|" + item.getMaterialCode() + "|" + item.getConstructionStage();
    }

    /**
     * 批量查询调整结果对应的需求计划汇总数据，避免逐条查询数据库。
     *
     * @param condition 调整列表查询条件
     * @return 需求计划版本号和物料编码组成的需求计划汇总映射
     */
    private Map<String, DpDemandPlanSum> buildDemandPlanSumMap(FactoryMonthPlanProductionFinalResult condition, String matchVersion) {
        if (StringUtil.isEmptyWithTrim(matchVersion)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<DpDemandPlanSum> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DpDemandPlanSum::getFactoryCode, condition.getFactoryCode());
        queryWrapper.eq(DpDemandPlanSum::getYear, condition.getYear());
        queryWrapper.eq(DpDemandPlanSum::getMonth, condition.getMonth());
        queryWrapper.eq(DpDemandPlanSum::getMonthPlanVersion, matchVersion);
        queryWrapper.eq(DpDemandPlanSum::getIsDelete, YesOrNoEnum.NO.getCode());
        queryWrapper.eq(StringUtils.isNotEmpty(condition.getStructureName()), DpDemandPlanSum::getStructureName, condition.getStructureName());
        queryWrapper.like(StringUtils.isNotEmpty(condition.getMaterialCode()), DpDemandPlanSum::getMaterialCode, condition.getMaterialCode());
        queryWrapper.like(StringUtils.isNotEmpty(condition.getMaterialDesc()), DpDemandPlanSum::getMaterialDesc, condition.getMaterialDesc());
        List<DpDemandPlanSum> demandPlanSumList = dpDemandPlanSumEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(demandPlanSumList)) {
            return Collections.emptyMap();
        }
        return demandPlanSumList.stream().collect(Collectors.toMap(
                item -> buildDemandPlanSumMapKey(item.getMonthPlanVersion(), item.getMaterialCode()),
                Function.identity(),
                (first, second) -> first
        ));
    }

    /**
     * 将需求计划汇总中的需求量字段补充到调整列表返回对象。
     *
     * @param adjustVo      调整列表返回对象
     * @param demandPlanSum 需求计划汇总数据
     */
    private void fillDemandQty(FactoryMonthPlanFinalAdjustVo adjustVo, DpDemandPlanSum demandPlanSum) {
        if (demandPlanSum == null) {
            return;
        }
        adjustVo.setNetQty(demandPlanSum.getNetQty());
        adjustVo.setHeightQty(demandPlanSum.getHeightQty());
        adjustVo.setMidQty(demandPlanSum.getMidQty());
        adjustVo.setCycleReserveQty(demandPlanSum.getCycleReserveQty());
        adjustVo.setConventionReserveQty(demandPlanSum.getConventionReserveQty());
    }

    /**
     * 构建需求计划汇总匹配键，用于调整结果关联对应需求量字段。
     *
     * @param monthPlanVersion 需求计划版本号
     * @param materialCode     物料编码
     * @return 需求计划版本号和物料编码组成的匹配键
     */
    private String buildDemandPlanSumMapKey(String monthPlanVersion, String materialCode) {
        return monthPlanVersion + "|" + materialCode;
    }
}
