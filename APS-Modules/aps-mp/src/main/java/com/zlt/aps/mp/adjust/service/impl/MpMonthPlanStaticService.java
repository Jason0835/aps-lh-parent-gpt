package com.zlt.aps.mp.adjust.service.impl;

import cn.hutool.core.convert.Convert;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.domain.vo.MpDayProductionStatisticsDetailVo;
import com.zlt.aps.mp.engine.adjust.MpWeekRollAdjustEngine;
import com.zlt.aps.mp.engine.capacity.MpMonthPlanDailyCapacityLimit;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.common.utils.PubUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 月计划统计服务
 *
 * @author zlt
 *
 */
@Service
public class MpMonthPlanStaticService extends AbstractBaseWeekAdjustServiceMonthAdapter {
    /**
     * 生成统计记录
     *
     * @param resultList
     */
    public void handleMonthPlanStatistics(List<FactoryMonthPlanMouldDayResult> resultList, boolean isAdjust) {
        FactoryMonthPlanMouldDayResult monthPlan = CollectionUtils.firstElement(resultList);
        String factoryCode = monthPlan.getFactoryCode();
        String productType = monthPlan.getProductTypeCode();
        String productionVersion = monthPlan.getProductionVersion();
        Integer mpYear = monthPlan.getYear();
        Integer mpMonth = monthPlan.getMonth();
        MpRollAdjustContextDTO contextDTO = new MpRollAdjustContextDTO();
        contextDTO.setMpYear(mpYear);
        contextDTO.setMpMonth(mpMonth);
        contextDTO.setFactoryCode(factoryCode);
        contextDTO.setProductionVersion(productionVersion);

        QueryWrapper<MpStructureAllocation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("PRODUCTION_VERSION", productionVersion);
        List<MpStructureAllocation> structureAllocationList = mpStructureAllocationEntityMapper.selectList(queryWrapper);

        // 设置周程滚动参数
        contextDTO
                .setParamMap(mpAdjustStructureInService.getMpWeekAdjustParam(contextDTO.getFactoryCode(), productType));
        // 设置工作日历
        contextDTO.setWorkCalendarMap(mpAdjustStructureInService.getWorkCalendarMap(contextDTO));
        // 设置月计划结构转产表-单结构
        contextDTO.setOneStructureAllocationList(structureAllocationList);
        // 设置总的硫化机台数
        contextDTO.setTotalLhMachines(mpAdjustStructureInService.getLhMachineCount(contextDTO));
        // 设置OEM配置集合
        initOemParam(contextDTO);
        // 设置结构统计
        contextDTO.setStructureStatisticMap(mpAdjustStructureInService.loadMpMonthPlanStatistics(contextDTO));

        Map<String, List<FactoryMonthPlanMouldDayResult>> monthPlanMap = resultList.stream()
                .collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getStructureName));
        Map<String, List<MpStructureAllocation>> structureAllocationMap = structureAllocationList.stream()
                .collect(Collectors.groupingBy(MpStructureAllocation::getStructureName));

        // 月计划统计结果列表
        List<MpMonthPlanStatistics> monthPlanStatisticsList = new ArrayList<>();
        MpMonthPlanDailyCapacityLimit mpMonthPlanDailyCapacityLimit = new MpMonthPlanDailyCapacityLimit();
        for (Entry<String, List<FactoryMonthPlanMouldDayResult>> entry : monthPlanMap.entrySet()) {
            String structureName = entry.getKey();
            contextDTO.setStructureName(structureName);
            List<FactoryMonthPlanMouldDayResult> monthList = entry.getValue();
            List<MpStructureAllocation> structureList = structureAllocationMap.get(structureName);
//            this.handleMonthPlanStatistics(contextDTO, monthList, structureList);
            try {
                contextDTO.setOneStructureAllocationList(structureList);

                // 初始结构开始日\收尾日
                initStructureStartAndEndDay(contextDTO);

                // 初始化日产信息
                MpWeekRollAdjustEngine weekRollAdjustEngine = new MpWeekRollAdjustEngine();
                Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = mpMonthPlanDailyCapacityLimit
                        .getDailyCapacityLimitMap(contextDTO);
                weekRollAdjustEngine.initDayProductionInfo(contextDTO, dailyCapacityLimitVoMap);
                // 设置日产能限制Map
                contextDTO.setDailyCapacityLimitVoMap(
                        ObjectUtils.defaultIfNull(dailyCapacityLimitVoMap, new HashMap<Integer, MpDailyCapacityLimitVo>()));

                // 重算每日产能限制，包括硫化机台数、胎胚种类数、换模次数
                reCalcMonthPlanDailyCapacityLimit(contextDTO, monthList, mpMonthPlanDailyCapacityLimit);

                // 构建月计划统计结果
                MpMonthPlanStatistics monthPlanStatistics = this.buildMpMonthPlanStatistics(contextDTO, monthList,
                        YesOrNoEnum.NO.getCode());
                if (Objects.nonNull(monthPlanStatistics)) {
                    monthPlanStatistics.setTempFlag(isAdjust? YesOrNoEnum.YES.getCode(): YesOrNoEnum.NO.getCode());
                    monthPlanStatisticsList.add(monthPlanStatistics);
                }

            } catch (Exception e) {
                throw new BusinessException("重算每日产能限制执行异常,原因:" + e.getMessage(), e);
            }
        }

        contextDTO.setMonthPlanStatisticsList(monthPlanStatisticsList);
        // 保存月计划统计结果
        saveMonthPlanStatisticsResult(contextDTO, null);
    }

    /**
     * 重算每日产能限制，包括硫化机台数、胎胚种类数、换模次数
     *
     * @param contextDTO      周程滚动上下文
     * @param mpProdFinalList 定稿记录列表
     */
    public void reCalcMonthPlanDailyCapacityLimit(MpRollAdjustContextDTO contextDTO,
                                                  List<FactoryMonthPlanMouldDayResult> mpProdFinalList,
                                                  MpMonthPlanDailyCapacityLimit adjustDailyCapacityLimitObj) {

        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = contextDTO.getDailyCapacityLimitVoMap();
        for (int i = contextDTO.getStructureStartDay(); i <= contextDTO.getStructureDeadLine(); i++) {
            if (dailyCapacityLimitVoMap.get(i) == null) {
                continue;
            }
            adjustDailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mpProdFinalList, i,
                    dailyCapacityLimitVoMap.get(i), contextDTO.getParamMap(), null, null);
        }
    }

    /**
     * 构建月计划统计结果
     *
     * @param mpProdFinalList 月计划定稿列表
     * @return 统计结果列表
     */
    public MpMonthPlanStatistics buildMpMonthPlanStatistics(MpRollAdjustContextDTO contextDTO,
                                                            List<FactoryMonthPlanMouldDayResult> mpProdFinalList,
                                                            String tempFlag) {

        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityMap = contextDTO.getDailyCapacityLimitVoMap();
        List<MpStructureAllocation> oneStructureAllocationList = contextDTO.getOneStructureAllocationList();
        if (PubUtil.isEmpty(dailyCapacityMap) || PubUtil.isEmpty(oneStructureAllocationList)
                || PubUtil.isEmpty(mpProdFinalList)) {
            return null;
        }
        FactoryMonthPlanMouldDayResult monthPlan = CollectionUtils.firstElement(mpProdFinalList);
        MpMonthPlanStatistics statistics = new MpMonthPlanStatistics();
        // 设置月计划统计相关字段
        MpStructureAllocation structureAllocation = oneStructureAllocationList.get(0);
        statistics.setFactoryCode(structureAllocation.getFactoryCode());
        statistics.setYear(structureAllocation.getYear());
        statistics.setMonth(structureAllocation.getMonth());
        statistics.setProductionVersion(structureAllocation.getProductionVersion());
        statistics.setMonthPlanVersion(structureAllocation.getMonthPlanVersion());
        statistics.setStructureName(structureAllocation.getStructureName());
        statistics.setYearMonth(monthPlan.getYearMonth());
        statistics.setProSize(monthPlan.getProSize());
        statistics.setStructureType(monthPlan.getStructureType());
        statistics.setLastMonthPlanVersion(monthPlan.getProductionVersion());
        statistics.setProductTypeCode(monthPlan.getProductTypeCode());

        // 遍历日期，设置每个dayN字段
        String dayField;
        int totalQty, oemQty;
        for (int day = ProductionConstant.MONTH_START_DAY; day <= ProductionConstant.MONTH_MAX_DAY; day++) {
            totalQty = 0;
            oemQty = 0;
            for (FactoryMonthPlanMouldDayResult prodFinal : mpProdFinalList) {
                dayField = FactoryConstant.DAY_FIELD + day;
                if (prodFinal.getFieldValueByFieldName(dayField) == null) {
                    continue;
                }
                totalQty += (Integer) prodFinal.getFieldValueByFieldName(dayField);
//                if (YesOrNoEnum.YES.getCode().equals(prodFinal.getOemFlag())){
//                    //若是贴牌，计划量进行累计
//                    oemQty += (Integer) prodFinal.getFieldValueByFieldName(dayField);
//                }
            }

            MpDailyCapacityLimitVo capacityVo = dailyCapacityMap.get(day);
            if (capacityVo != null) {
                MpDayProductionStatisticsDetailVo dayProductionStatisticsDetailVo = new MpDayProductionStatisticsDetailVo();
                dayProductionStatisticsDetailVo
                        .setMaxLhMachines(Convert.toInt(capacityVo.getMaxLhMachines(), 0).equals(0) ? null
                                : capacityVo.getMaxLhMachines());
                dayProductionStatisticsDetailVo
                        .setMaxEmbryoTypes(Convert.toInt(capacityVo.getMaxEmbryoTypes(), 0).equals(0) ? null
                                : capacityVo.getMaxEmbryoTypes());
                dayProductionStatisticsDetailVo
                        .setLhMachines(Convert.toInt(capacityVo.getUsedLhMachines(), 0).equals(0) ? null
                                : capacityVo.getUsedLhMachines());
                dayProductionStatisticsDetailVo
                        .setEmbryoCount(Convert.toInt(capacityVo.getUsedEmbryoTypes(), 0).equals(0) ? null
                                : capacityVo.getUsedEmbryoTypes());
                dayProductionStatisticsDetailVo
                        .setChangeMould(Convert.toInt(capacityVo.getUsedChangeMould(), 0).equals(0) ? null
                                : capacityVo.getUsedChangeMould());
                dayProductionStatisticsDetailVo.setTotalQty(totalQty);
                dayProductionStatisticsDetailVo.setOemQty(oemQty);
                statistics.setFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + day,
                        JSONObject.toJSONString(dayProductionStatisticsDetailVo));
            }
            statistics.setTempFlag(tempFlag);
        }
        // 同步更新上下文的结构统计
        contextDTO.getStructureStatisticMap().put(contextDTO.getStructureName(), statistics);
        return statistics;
    }
}
