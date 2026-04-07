package com.zlt.aps.job.task;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.loginUtils.annotation.AutoLoginLog;
import com.zlt.aps.cx.api.service.ICxPrecisionPlanRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 成型精度计划定时任务
 *
 * @author APS Team
 */
@Slf4j
@Component("cxPrecisionPlanTask")
public class CxPrecisionPlanTask {

    @Autowired
    private ICxPrecisionPlanRemoteService cxPrecisionPlanRemoteService;

    /**
     * 从MES同步数据生成成型精度计划
     */
    @AutoLoginLog
    public void generateFromMes() {
        log.info("定时任务：从MES同步数据生成成型精度计划");
        try {
            AjaxResult result = cxPrecisionPlanRemoteService.generatePlansFromMes();
            log.info("定时任务执行结果：{}", result.get("msg"));
        } catch (Exception e) {
            log.error("定时任务执行失败", e);
        }
    }

    /**
     * 自动生成年度成型精度计划
     *
     * @param year 年份
     */
    @AutoLoginLog
    public void autoGenerateYearly(String year) {
        log.info("定时任务：自动生成{}年度成型精度计划", year);
        try {
            Integer yearInt = Integer.parseInt(year);
            AjaxResult result = cxPrecisionPlanRemoteService.autoGenerateYearlyPlans(yearInt);
            log.info("定时任务执行结果：{}", result.get("msg"));
        } catch (Exception e) {
            log.error("定时任务执行失败", e);
        }
    }

    /**
     * 批量更新到期天数
     */
    @AutoLoginLog
    public void batchUpdateDaysToDue() {
        log.info("定时任务：批量更新到期天数");
        try {
            AjaxResult result = cxPrecisionPlanRemoteService.batchUpdateDaysToDue();
            log.info("定时任务执行结果：{}", result.get("msg"));
        } catch (Exception e) {
            log.error("定时任务执行失败", e);
        }
    }

    /**
     * MES回传实际完成时间
     *
     * @param mesSourceId MES来源ID
     * @param actualDate  实际日期
     */
    @AutoLoginLog
    public void updateActualDate(String mesSourceId, String actualDate) {
        log.info("定时任务：MES回传实际完成时间，mesSourceId={}, actualDate={}", mesSourceId, actualDate);
        try {
            Long mesSourceIdLong = Long.parseLong(mesSourceId);
            AjaxResult result = cxPrecisionPlanRemoteService.updateActualDate(mesSourceIdLong, actualDate);
            log.info("定时任务执行结果：{}", result.get("msg"));
        } catch (Exception e) {
            log.error("定时任务执行失败", e);
        }
    }
}
