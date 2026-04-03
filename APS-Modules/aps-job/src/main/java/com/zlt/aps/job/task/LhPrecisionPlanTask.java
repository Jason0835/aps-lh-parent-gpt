package com.zlt.aps.job.task;

import com.zlt.aps.lh.api.service.ILhPrecisionPlanRemoteService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 硫化精度计划定时任务
 *
 * @author APS Team
 */
@Slf4j
@Component("lhPrecisionPlanTask")
public class LhPrecisionPlanTask {

    @Autowired
    private ILhPrecisionPlanRemoteService lhPrecisionPlanRemoteService;

    /**
     * 从MES同步数据生成硫化精度计划
     */
    public void generateFromMes() {
        log.info("定时任务：从MES同步数据生成硫化精度计划");
        try {
            AjaxResult result = lhPrecisionPlanRemoteService.generatePlansFromMes();
            log.info("定时任务执行结果：{}", result.get("msg"));
        } catch (Exception e) {
            log.error("定时任务执行失败", e);
        }
    }

    /**
     * 自动生成年度硫化精度计划
     *
     * @param year 年份
     */
    public void autoGenerateYearly(String year) {
        log.info("定时任务：自动生成{}年度硫化精度计划", year);
        try {
            Integer yearInt = Integer.parseInt(year);
            AjaxResult result = lhPrecisionPlanRemoteService.autoGenerateYearlyPlans(yearInt);
            log.info("定时任务执行结果：{}", result.get("msg"));
        } catch (Exception e) {
            log.error("定时任务执行失败", e);
        }
    }

    /**
     * 执行30天预警检查
     */
    public void checkWarning() {
        log.info("定时任务：执行30天预警检查");
        try {
            AjaxResult result = lhPrecisionPlanRemoteService.checkWarning();
            log.info("定时任务执行结果：{}", result.get("msg"));
        } catch (Exception e) {
            log.error("定时任务执行失败", e);
        }
    }

    /**
     * 批量更新到期天数
     */
    public void batchUpdateDaysToDue() {
        log.info("定时任务：批量更新到期天数");
        try {
            AjaxResult result = lhPrecisionPlanRemoteService.batchUpdateDaysToDue();
            log.info("定时任务执行结果：{}", result.get("msg"));
        } catch (Exception e) {
            log.error("定时任务执行失败", e);
        }
    }
}
