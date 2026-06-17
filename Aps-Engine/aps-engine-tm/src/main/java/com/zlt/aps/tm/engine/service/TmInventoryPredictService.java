package com.zlt.aps.tm.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tm.api.domain.entity.TmStock;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmStockForecast;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.mapper.TmEngineStockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面库存预测默认步骤服务。
 *
 * <p>负责读取库存和损耗相关数据并计算供应时长。
 * 第一个班的滚动库存使用14点预计库存，计算公式：
 * 14点预计库存 = 6点库存 - 早班需求量 + 早班计划量</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmInventoryPredictService implements ITmInventoryPredictService {

    private final TmEngineStockMapper tmStockMapper;

    // TODO: 注入成型计划Mapper，用于获取早班需求量
    // private final CxScheduleResultMapper cxScheduleResultMapper;

    @Override
    public void predict(TmScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("胎面排程上下文不能为空");
        }

        Date scheduleDate = context.getScheduleDate();
        if (scheduleDate == null) {
            throw new IllegalArgumentException("排程日期不能为空");
        }

        // 获取待排任务中的胎面编码列表
        List<String> treadCodes = context.getTaskDraftList().stream()
                .map(TmTaskDraft::getTreadCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (treadCodes.isEmpty()) {
            log.warn("没有待排任务，跳过库存预测");
            return;
        }

        // 查询每个胎面的6点库存
        Map<String, BigDecimal> sixClockStockMap = querySixClockStock(scheduleDate, treadCodes);

        // 查询早班需求量（从成型计划表）
        Map<String, BigDecimal> firstShiftDemandMap = queryFirstShiftDemand(scheduleDate, treadCodes);

        // 查询早班计划量（从T_TM_SCHEDULE_RESULT获取已排产的早班计划量）
        Map<String, BigDecimal> firstShiftPlanMap = queryFirstShiftPlan(scheduleDate, treadCodes);

        // 构建库存预测结果
        Map<String, TmStockForecast> stockForecastMap = new HashMap<>();
        for (String treadCode : treadCodes) {
            TmStockForecast forecast = new TmStockForecast();
            forecast.setTreadCode(treadCode);
            forecast.setSixClockStockQty(sixClockStockMap.getOrDefault(treadCode, BigDecimal.ZERO));
            forecast.setFirstShiftDemandQty(firstShiftDemandMap.getOrDefault(treadCode, BigDecimal.ZERO));
            forecast.setFirstShiftPlanQty(firstShiftPlanMap.getOrDefault(treadCode, BigDecimal.ZERO));
            forecast.calculateRollingStockQty();
            stockForecastMap.put(treadCode, forecast);
        }

        context.setStockForecastMap(stockForecastMap);
        log.info("库存预测完成，共预测{}个胎面规格", stockForecastMap.size());
    }

    /**
     * 查询6点库存。
     *
     * @param scheduleDate 排程日期
     * @param treadCodes   胎面编码列表
     * @return 胎面编码 -> 6点库存 的映射
     */
    private Map<String, BigDecimal> querySixClockStock(Date scheduleDate, List<String> treadCodes) {
        LambdaQueryWrapper<TmStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmStock::getStockDate, scheduleDate)
                .in(TmStock::getTreadCode, treadCodes);

        List<TmStock> stockList = tmStockMapper.selectList(wrapper);
        return stockList.stream()
                .collect(Collectors.toMap(
                        TmStock::getTreadCode,
                        stock -> stock.getStockQty() != null ? stock.getStockQty() : BigDecimal.ZERO,
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 查询早班需求量（从成型计划表）。
     *
     * @param scheduleDate 排程日期
     * @param treadCodes   胎面编码列表
     * @return 胎面编码 -> 早班需求量 的映射
     */
    private Map<String, BigDecimal> queryFirstShiftDemand(Date scheduleDate, List<String> treadCodes) {
        // TODO: 实现从成型计划表获取早班需求量
        // 需要根据胎面编码关联成型计划，获取早班（CLASS1）的成型需求量
        // 暂时返回空映射，后续接入成型计划数据后补充实现
        log.warn("早班需求量查询暂未实现，使用默认值0");
        return new HashMap<>();
    }

    /**
     * 查询早班计划量（从T_TM_SCHEDULE_RESULT获取已排产的早班计划量）。
     *
     * @param scheduleDate 排程日期
     * @param treadCodes   胎面编码列表
     * @return 胎面编码 -> 早班计划量 的映射
     */
    private Map<String, BigDecimal> queryFirstShiftPlan(Date scheduleDate, List<String> treadCodes) {
        // TODO: 实现从T_TM_SCHEDULE_RESULT获取早班计划量
        // 需要查询同一天已排产的早班（CLASS1）计划量
        // 暂时返回空映射，后续接入排程结果数据后补充实现
        log.warn("早班计划量查询暂未实现，使用默认值0");
        return new HashMap<>();
    }
}
