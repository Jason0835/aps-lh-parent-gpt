package com.zlt.aps.monthplan.factory.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.IncrementConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.IncrementService;
import com.tlt.aps.utils.JsonUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.factory.utils.DateUtils;
import com.zlt.aps.maindata.enums.EventModuleTypeEnum;
import com.zlt.aps.maindata.enums.ReleaseStatusEnum;
import com.zlt.aps.maindata.event.publisher.EventPublisher;
import com.zlt.aps.monthplan.api.domain.dto.MonthPlanFinalizedEventDto;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.factory.event.MonthPlanFinalizedEvent;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanMouldDayResultEntityMapper;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

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
public class FactoryMonthPlanProductionFinalResultServiceImpl extends ServiceImpl<FactoryMonthPlanProductionFinalResultEntityMapper, FactoryMonthPlanProductionFinalResult> implements IFactoryMonthPlanProductionFinalResultService {
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

    @Override
    public List<FactoryMonthPlanProductionFinalResult> getDataList(FactoryMonthPlanProductionFinalResult condition) {
        QueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, condition);
        List<FactoryMonthPlanProductionFinalResult> dataList = this.baseMapper.selectList(queryWrapper);
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
        List<FactoryMonthPlanProductionFinalResult> list = this.list(queryWrapper);
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
        List<FactoryMonthPlanProductionFinalResult> list = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(list)) {
            return BigDecimal.ZERO.intValue();
        }
        Set<Integer> yearMonths = list.stream().map(FactoryMonthPlanProductionFinalResult::getYearMonth).collect(Collectors.toSet());
        return yearMonths.size();
    }

    @Override
    public Map<String, Integer> calculateMonthSurplus(String requireVersion, List<MdmProductStock> finishedProductStocks) {
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
        List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals = this.list(queryWrapper);
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
            entity.setBrand(value.get(0).getBrand());
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

        List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals = this.list(queryWrapper);
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
    public List<FactoryMonthPlanProductionFinalResult> findProductionFinalResult(MpFactoryProductionVersion finalVersion) {
        if (null == finalVersion) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, finalVersion.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, finalVersion.getYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, finalVersion.getMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, finalVersion.getMonthPlanVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        return this.list(queryWrapper);
    }

    @Override
    public List<FactoryMonthPlanProductionFinalResult> findProductionFinalResult(Set<String> monthPlanVersions) {
        if (CollectionUtils.isEmpty(monthPlanVersions)) {
            return Collections.emptyList();
        }
        final int batchSize = 1000;
        List<FactoryMonthPlanProductionFinalResult> result = new ArrayList<>();
        List<String> versionList = new ArrayList<>(monthPlanVersions);
        for (int i = 0; i < versionList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, versionList.size());
            List<String> batchVersions = versionList.subList(i, end);
            LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper =
                Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                    .in(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, batchVersions)
                    .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
            result.addAll(this.list(queryWrapper));
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
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getConstructionStage()), "CONSTRUCTION_STAGE", condition.getConstructionStage());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getBrand()), "BRAND", condition.getBrand());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getProSize()), "PRO_SIZE", condition.getProSize());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getSpecifications()), "SPECIFICATIONS", condition.getSpecifications());
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

}
