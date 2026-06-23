package com.zlt.aps.tq.task;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.tq.service.ITqWarningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 胎圈排程预警定时任务
 *
 * <p>定时检查胎圈库存和班次完成量，发现异常时发送预警消息。</p>
 *
 * <p>使用说明：</p>
 * <ol>
 *   <li>在定时任务管理界面配置任务，调用目标示例：tqWarningTask.checkStockWarning()</li>
 *   <li>预警频率通过系统定时任务的cron表达式配置</li>
 *   <li>完成量预警建议在班次完全结束后调用：tqWarningTask.checkFinishQtyWarning(1)</li>
 * </ol>
 *
 * <p>班次索引说明：</p>
 * <ul>
 *   <li>1-夜班（22:00-06:00）</li>
 *   <li>2-早班（06:00-14:00）</li>
 *   <li>3-中班（14:00-22:00）</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Component("tqWarningTask")
public class TqWarningTask {

    @Autowired
    private ITqWarningService tqWarningService;

    /**
     * 库存预警检查
     *
     * <p>扫描当天最新库存，低于阈值则发送预警。</p>
     * <p>调用目标：tqWarningTask.checkStockWarning()</p>
     */
    public void checkStockWarning() {
        log.info("胎圈库存预警定时任务开始执行");
        long startTime = System.currentTimeMillis();
        try {
            tqWarningService.checkStockWarning();
        } catch (Exception e) {
            log.error("胎圈库存预警定时任务执行失败", e);
        }
        log.info("胎圈库存预警定时任务执行结束，耗时{}ms", System.currentTimeMillis() - startTime);
    }

    /**
     * 班次完成量预警检查（默认检查当天指定班次）
     *
     * <p>在班次完全结束后调用，对比计划量与实际完成量。</p>
     * <p>调用目标：tqWarningTask.checkFinishQtyWarning(1) - 检查当天1班</p>
     *
     * @param shiftIndex 班次索引（1~6）
     */
    public void checkFinishQtyWarning(int shiftIndex) {
        log.info("胎圈班次完成量预警定时任务开始执行：班次={}", shiftIndex);
        long startTime = System.currentTimeMillis();
        try {
            Date today = new Date();
            tqWarningService.checkFinishQtyWarning(today, shiftIndex);
        } catch (Exception e) {
            log.error("胎圈班次完成量预警定时任务执行失败：班次={}", shiftIndex, e);
        }
        log.info("胎圈班次完成量预警定时任务执行结束，班次={}，耗时{}ms",
                shiftIndex, System.currentTimeMillis() - startTime);
    }

    /**
     * 班次完成量预警检查（指定日期和班次）
     *
     * <p>调用目标：tqWarningTask.checkFinishQtyWarning('2026-06-22', 1)</p>
     *
     * @param scheduleDateStr 排程日期字符串（yyyy-MM-dd格式）
     * @param shiftIndex      班次索引（1~6）
     */
    public void checkFinishQtyWarning(String scheduleDateStr, int shiftIndex) {
        log.info("胎圈班次完成量预警定时任务开始执行：日期={}，班次={}", scheduleDateStr, shiftIndex);
        long startTime = System.currentTimeMillis();
        try {
            Date scheduleDate = DateUtil.parseDate(scheduleDateStr);
            tqWarningService.checkFinishQtyWarning(scheduleDate, shiftIndex);
        } catch (Exception e) {
            log.error("胎圈班次完成量预警定时任务执行失败：日期={}，班次={}", scheduleDateStr, shiftIndex, e);
        }
        log.info("胎圈班次完成量预警定时任务执行结束：日期={}，班次={}，耗时{}ms",
                scheduleDateStr, shiftIndex, System.currentTimeMillis() - startTime);
    }
}
