package com.zlt.aps.mp.raw.controller;


import com.zlt.aps.mp.raw.service.IRawWarningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * @author Nick
 */
@Component
@Slf4j
public class RawWarningSchedule {

    @Autowired
    private IRawWarningService rawWarningService;

    /**
     * 每天凌晨1点执行用量偏差预警
     */
    //@Scheduled(cron = "0 0 1 * * ?")
    public void executeDailyUsageWarning() {
        try {
            log.info("开始执行每日用量偏差预警");

            // 获取上周的周次（假设处理上周的数据）
            LocalDate lastWeek = LocalDate.now().minusWeeks(1);
            int year = lastWeek.getYear();
            int month = lastWeek.getMonthValue();
            int week = getWeekOfYear(lastWeek);

            // 假设工厂编码列表
            String[] factoryCodes = {"FACTORY001", "FACTORY002", "FACTORY003"};

            for (String factoryCode : factoryCodes) {
                try {
                    // 同步实际用量数据
                    rawWarningService.syncWeekActualUsage(factoryCode, year, week, month);

                    // 执行用量偏差预警
                    rawWarningService.executeUsageDeviationWarning(factoryCode, year, week, month);
                } catch (Exception e) {
                    log.error("执行用量偏差预警失败，工厂：{}", factoryCode, e);
                }
            }

            log.info("每日用量偏差预警执行完成");
        } catch (Exception e) {
            log.error("执行每日用量偏差预警失败", e);
        }
    }

    /**
     * 每月1号凌晨2点执行新材料预警
     */
    //@Scheduled(cron = "0 0 2 1 * ?")
    public void executeMonthlyNewMaterialWarning() {
        try {
            log.info("开始执行每月新材料预警");

            // 获取上个月的数据
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            int year = lastMonth.getYear();
            int month = lastMonth.getMonthValue();

            // 假设工厂编码列表
            String[] factoryCodes = {"FACTORY001", "FACTORY002", "FACTORY003"};

            for (String factoryCode : factoryCodes) {
                try {
                    rawWarningService.executeNewMaterialWarning(factoryCode, year, month);
                } catch (Exception e) {
                    log.error("执行新材料预警失败，工厂：{}", factoryCode, e);
                }
            }

            log.info("每月新材料预警执行完成");
        } catch (Exception e) {
            log.error("执行每月新材料预警失败", e);
        }
    }

    /**
     * 计算一年中的第几周
     */
    private int getWeekOfYear(LocalDate date) {
        // 简单实现，实际应该根据公司的周定义来计算
        return date.get(java.time.temporal.WeekFields.ISO.weekOfYear());
    }
}