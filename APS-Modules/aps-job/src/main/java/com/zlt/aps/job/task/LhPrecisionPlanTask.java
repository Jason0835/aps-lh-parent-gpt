package com.zlt.aps.job.task;

import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.lh.api.service.ILhPrecisionPlanRemoteService;
import com.zlt.aps.itf.mes.IMesItfService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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

    @Autowired
    private IMesItfService iMesItfService;

    /**
     * 从MES同步数据生成硫化精度计划
     * 调用综合接口，内部按最新版本号增量同步设备保养计划、回填实际执行日期、生成精度计划、自动推算下一年度
     */
    public void generateFromMes() {
        log.info("定时任务：从MES同步数据生成硫化精度计划");
        FeignTokenHelper.runWithToken(() -> {
            try {
                Integer currentYear = LocalDate.now().getYear();
                AjaxResult result = iMesItfService.syncAndGenerateLhPrecisionPlan(currentYear);
                log.info("定时任务执行结果：{}", result.get("msg"));
            } catch (Exception e) {
                log.error("定时任务执行失败", e);
            }
        });
    }

    /**
     * 自动生成年度硫化精度计划
     *
     * @param year 年份
     */
    public void autoGenerateYearly(String year) {
        log.info("定时任务：自动生成{}年度硫化精度计划", year);
        FeignTokenHelper.runWithToken(() -> {
            try {
                Integer yearInt = Integer.parseInt(year);
                AjaxResult result = lhPrecisionPlanRemoteService.autoCalculateLhPrecisionPlan(yearInt);
                log.info("定时任务执行结果：{}", result.get("msg"));
            } catch (Exception e) {
                log.error("定时任务执行失败", e);
            }
        });
    }

    /**
     * 执行30天预警检查
     */
    public void checkWarning() {
        log.info("定时任务：执行30天预警检查");
        FeignTokenHelper.runWithToken(() -> {
            try {
                AjaxResult result = lhPrecisionPlanRemoteService.checkWarning();
                log.info("定时任务执行结果：{}", result.get("msg"));
            } catch (Exception e) {
                log.error("定时任务执行失败", e);
            }
        });
    }

    /**
     * 批量更新到期天数
     */
    public void batchUpdateDaysToDue() {
        log.info("定时任务：批量更新到期天数");
        FeignTokenHelper.runWithToken(() -> {
            try {
                AjaxResult result = lhPrecisionPlanRemoteService.batchUpdateDaysToDue();
                log.info("定时任务执行结果：{}", result.get("msg"));
            } catch (Exception e) {
                log.error("定时任务执行失败", e);
            }
        });
    }

    /**
     * 下发硫化精度计划到MES
     * 每天定时查询计划排程精度日期有值且实际执行日期为空的数据，下发到MES中间表并通知MES
     */
    public void issueLhPrecisionPlanToMes() {
        log.info("定时任务：下发硫化精度计划到MES");
        FeignTokenHelper.runWithToken(() -> {
            try {
                AjaxResult result = iMesItfService.issueLhPrecisionPlan(null);
                log.info("定时任务执行结果：{}", result.get("msg"));
            } catch (Exception e) {
                log.error("定时任务执行失败", e);
            }
        });
    }
}
