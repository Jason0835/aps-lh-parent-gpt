package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.engine.domain.LhSkuSurplusControlParam;
import com.zlt.aps.common.engine.domain.LhSurplusResultVo;
import com.zlt.aps.common.engine.domain.LhSurplusSkuInfo;
import com.zlt.aps.common.engine.utils.LhSkuSurplusCalculator;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.LhScheduleRequestDTO;
import com.zlt.aps.lh.api.domain.entity.LhParams;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.handler.LhSkuSurplusDataInfo;
import com.zlt.aps.lh.handler.LhSkuSurplusDataLoadHandler;
import com.zlt.aps.lh.handler.SkuMonthPlanCalculator;
import com.zlt.aps.lh.mapper.LhParamsMapper;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化余量计算服务接口
 * 用以测试用
 * @author ZLT
 * @date 20261105
 */
@Slf4j
@Api(tags = "硫化余量计算测试服务")
@RestController
@RequestMapping("/skuLhSurplus")
@RequiredArgsConstructor
public class SkuLhSurplusCalculatorController {

    private final LhParamsMapper lhParamsMapper;

    private final LhSkuSurplusDataLoadHandler dataLoadHandler;

    @ResponseBody
    @ApiOperation("硫化余量测试")
    @PostMapping("/getSkuLhSurplus")
    public AjaxResult getSkuLhSurplus(@RequestBody LhScheduleRequestDTO lhScheduleRequestDTO) {
        String factoryCode = lhScheduleRequestDTO.getFactoryCode();
        Date scheduleDate = lhScheduleRequestDTO.getScheduleDate();
        Map<String, Object> paramMap = getParamInfo(factoryCode);
        List<Date> allProductionDate = getAllProductionDateInfo(scheduleDate, factoryCode);
        LhSkuSurplusDataInfo dataInfo = dataLoadHandler.loadData(factoryCode, allProductionDate);
        List<FactoryMonthPlanProductionFinalResult> allMonthPlanList = dataInfo.getAllMonthPlanList();
        if (CollectionUtils.isEmpty(allMonthPlanList)) {
            return AjaxResult.success("无数据，无需计算余量");
        }
        Integer extendDays = LhScheduleConstant.DEFAULT_EARLY_PRODUCTION_DAYS_THRESHOLD;
        Integer skuMaxDiscontinueDays = null;
        if (!CollectionUtils.isEmpty(paramMap)) {
            Integer extendDayValue = (Integer) paramMap.get(LhScheduleParamConstant.EARLY_PRODUCTION_DAYS_THRESHOLD);
            if (null != extendDayValue) {
                extendDays = extendDayValue;
            }
            skuMaxDiscontinueDays = (Integer) paramMap.get(LhScheduleParamConstant.SKU_MAX_DISCONTINUE_DAYS);
        }
        Integer realExtendDays = extendDays;
        Integer realSkuMaxDiscontinueDays = skuMaxDiscontinueDays;
        Map<String, List<FactoryMonthPlanProductionFinalResult>> singleSkuGroupMap = allMonthPlanList.stream().collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getMaterialPlanKey));
        List<LhSurplusResultVo> skuSurplusResultList = Lists.newArrayList();
        singleSkuGroupMap.forEach((key, planList) -> {
            FactoryMonthPlanProductionFinalResult plan = planList.get(BigDecimal.ZERO.intValue());
            Integer actualFinishedQty = dataInfo.getFinishQty(plan);
            LhSkuSurplusControlParam productionDateInfo = new LhSkuSurplusControlParam(dataInfo.getPlanStartDate(), false, allProductionDate, realExtendDays, realSkuMaxDiscontinueDays);
            LhSurplusSkuInfo calculateSkuInfo = new LhSurplusSkuInfo(plan, null, allMonthPlanList, Collections.emptyMap(), actualFinishedQty, dataInfo.getAllLhDayPlanAdjustList());
            LhSurplusResultVo lhSurplusResult = LhSkuSurplusCalculator.getSurplusInfo(productionDateInfo, calculateSkuInfo);
            skuSurplusResultList.add(lhSurplusResult);
        });
        return AjaxResult.success("硫化余量获取成功", skuSurplusResultList);
    }

    /**
     * 获取所有的排产日期
     *
     * @param scheduleDate 排产日
     * @param factoryCode  工厂编号
     * @return
     */
    private List<Date> getAllProductionDateInfo(Date scheduleDate, String factoryCode) {
        Set<Date> allProductionDate = getCurrentProductionDateInfo(scheduleDate);
        return Lists.newArrayList(allProductionDate);
    }

    /**
     * 获取当前排产周期日(3天8个班)
     *
     * @param scheduleDate
     * @return
     */
    private Set<Date> getCurrentProductionDateInfo(Date scheduleDate) {
        Set<Date> allProductionDate = Sets.newHashSet();
        LocalDate currentDate = SkuMonthPlanCalculator.getDate(scheduleDate);
        //当天-调整
        LocalDate before = currentDate.plusDays(-BigDecimal.ONE.intValue());
        allProductionDate.add(SkuMonthPlanCalculator.getDate(before));
        //下一天-排产
        allProductionDate.add(scheduleDate);
        //下两天-预排
        LocalDate after = currentDate.plusDays(BigDecimal.ONE.intValue());
        allProductionDate.add(SkuMonthPlanCalculator.getDate(after));
        return allProductionDate;
    }

    /**
     * 获取参数信息
     *
     * @param factoryCode
     * @return
     */
    private Map<String, Object> getParamInfo(String factoryCode) {
        List<String> paramCodeList = Lists.newArrayList();
        paramCodeList.add(LhScheduleParamConstant.EARLY_PRODUCTION_DAYS_THRESHOLD);
        paramCodeList.add(LhScheduleParamConstant.SKU_MAX_DISCONTINUE_DAYS);
        LambdaQueryWrapper<LhParams> paramsQuery = new LambdaQueryWrapper<>();
        paramsQuery.eq(LhParams::getFactoryCode, factoryCode)
                .in(LhParams::getParamCode, paramCodeList)
                .eq(LhParams::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        List<LhParams> dataList = lhParamsMapper.selectList(paramsQuery);
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyMap();
        }
        Map<String, Object> paramMap = Maps.newHashMap();
        dataList.forEach(singleParam -> {
            String key = singleParam.getParamCode();
            String configurationValue = singleParam.getParamValue();
            if (StringUtils.isBlank(configurationValue)) {
                return;
            }
            if (LhScheduleParamConstant.EARLY_PRODUCTION_DAYS_THRESHOLD.equals(key) || LhScheduleParamConstant.SKU_MAX_DISCONTINUE_DAYS.equals(key)) {
                try {
                    Integer paramValue = Integer.parseInt(configurationValue.trim());
                    paramMap.put(key, paramValue);
                } catch (NumberFormatException e) {
                    log.warn("硫化参数解析失败, paramCode={}, value={}", key, configurationValue);
                }
                return;
            }
        });
        if (CollectionUtils.isEmpty(paramMap)) {
            return Collections.emptyMap();
        }
        return paramMap;
    }

}
