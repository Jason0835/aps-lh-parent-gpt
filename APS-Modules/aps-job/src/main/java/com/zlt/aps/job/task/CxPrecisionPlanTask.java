package com.zlt.aps.job.task;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.loginUtils.annotation.AutoLoginLog;
import com.zlt.aps.cx.api.service.ICxPrecisionPlanRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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

            Integer currentYear = LocalDate.now().getYear();
            AjaxResult result15 = cxPrecisionPlanRemoteService.autoCalculateCxPrecisionPlan15Days(currentYear);
            log.info("成型精度计划（15天）自动推算结果：{}", result15.get("msg"));

            AjaxResult result60 = cxPrecisionPlanRemoteService.autoCalculateCxPrecisionPlan60Days(currentYear);
            log.info("成型精度计划（60天）自动推算结果：{}", result60.get("msg"));
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
            AjaxResult result15 = cxPrecisionPlanRemoteService.autoCalculateCxPrecisionPlan15Days(yearInt);
            log.info("成型精度计划（15天）执行结果：{}", result15.get("msg"));

            AjaxResult result60 = cxPrecisionPlanRemoteService.autoCalculateCxPrecisionPlan60Days(yearInt);
            log.info("成型精度计划（60天）执行结果：{}", result60.get("msg"));
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
}
