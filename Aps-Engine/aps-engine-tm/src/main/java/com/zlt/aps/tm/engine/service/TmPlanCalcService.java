package com.zlt.aps.tm.engine.service;

import cn.hutool.core.collection.CollUtil;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmStockForecast;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 胎面需求量和计划量默认计算步骤服务。
 *
 * <p>当前不实现第15章未确认算法。若任务已有计划量则保持不变；若计划量为空且需求量存在，
 * 使用需求量作为骨架阶段计划量；需求量也缺失时标记未排原因。
 * 计划量计算使用库存预测中的 rollingStockQty（14点预计库存）。</p>
 */
@Service
public class TmPlanCalcService implements ITmPlanCalcService {

    @Override
    public void calculate(TmScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("胎面排程上下文不能为空");
        }
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }

        // 获取库存预测结果
        Map<String, TmStockForecast> stockForecastMap = context.getStockForecastMap();

        for (TmTaskDraft task : context.getTaskDraftList()) {
            // 如果已有计划量则保持不变
            if (task.getPlanQty() != null) {
                continue;
            }

            // 从库存预测结果中获取 rollingStockQty 并设置到任务中
            if (stockForecastMap != null && task.getTreadCode() != null) {
                TmStockForecast forecast = stockForecastMap.get(task.getTreadCode());
                if (forecast != null) {
                    task.setRollingStockQty(forecast.getRollingStockQty());
                    task.setSixClockStockQty(forecast.getSixClockStockQty());
                }
            }

            // 如果当前班需求量存在，使用需求量作为计划量
            if (task.getCurrentShiftDemandQty() != null) {
                task.setPlanQty(task.getCurrentShiftDemandQty());
                continue;
            }

            // 如果保证范围内需求量存在，使用需求量作为计划量
            if (task.getGuardDemandQty() != null) {
                task.setPlanQty(task.getGuardDemandQty());
                continue;
            }

            // 兜底：使用总需求量作为计划量
            if (task.getDemandQty() != null) {
                task.setPlanQty(task.getDemandQty());
                continue;
            }
        }
    }
}
