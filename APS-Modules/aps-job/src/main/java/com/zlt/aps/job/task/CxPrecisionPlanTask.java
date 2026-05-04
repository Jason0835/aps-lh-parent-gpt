package com.zlt.aps.job.task;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.cx.api.service.ICxPrecisionPlanRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component("cxPrecisionPlanTask")
public class CxPrecisionPlanTask {

    @Autowired
    private ICxPrecisionPlanRemoteService cxPrecisionPlanRemoteService;

    public void generateFromMes() {
        log.info("定时任务：从MES同步数据生成成型精度计划");
        FeignTokenHelper.runWithToken(() -> {
            try {
                Integer currentYear = LocalDate.now().getYear();
                AjaxResult result = cxPrecisionPlanRemoteService.generatePlansFromMes(currentYear);
                log.info("定时任务执行结果：{}", result.get("msg"));
            } catch (Exception e) {
                log.error("定时任务执行失败", e);
            }
        });
    }

    public void autoGenerateYearly(String year) {
        log.info("定时任务：自动生成{}年度成型精度计划", year);
        FeignTokenHelper.runWithToken(() -> {
            try {
                Integer yearInt = Integer.parseInt(year);
                AjaxResult result = cxPrecisionPlanRemoteService.autoGenerateYearlyPlans(yearInt);
                log.info("定时任务执行结果：{}", result.get("msg"));
            } catch (Exception e) {
                log.error("定时任务执行失败", e);
            }
        });
    }

    public void checkWarning() {
        log.info("定时任务：执行30天预警检查");
        FeignTokenHelper.runWithToken(() -> {
            try {
                AjaxResult result = cxPrecisionPlanRemoteService.checkWarning();
                log.info("定时任务执行结果：{}", result.get("msg"));
            } catch (Exception e) {
                log.error("定时任务执行失败", e);
            }
        });
    }

    public void batchUpdateDaysToDue() {
        log.info("定时任务：批量更新到期天数");
        FeignTokenHelper.runWithToken(() -> {
            try {
                AjaxResult result = cxPrecisionPlanRemoteService.batchUpdateDaysToDue();
                log.info("定时任务执行结果：{}", result.get("msg"));
            } catch (Exception e) {
                log.error("定时任务执行失败", e);
            }
        });
    }

    public void updateActualDate(String mesSourceId, String actualDate) {
        log.info("定时任务：MES回传实际完成时间，mesSourceId={}, actualDate={}", mesSourceId, actualDate);
        FeignTokenHelper.runWithToken(() -> {
            try {
                Long mesSourceIdLong = Long.parseLong(mesSourceId);
                AjaxResult result = cxPrecisionPlanRemoteService.updateActualDate(mesSourceIdLong, actualDate);
                log.info("定时任务执行结果：{}", result.get("msg"));
            } catch (Exception e) {
                log.error("定时任务执行失败", e);
            }
        });
    }
}
