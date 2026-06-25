package com.zlt.aps.job.task;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.mp.api.service.IFactoryMonthPlanProductionFinalResultRemoteService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 上月超欠产定时任务
 * 每月1号凌晨3点执行，自动根据上月计划排产量和上月硫化日完成量(合格品)计算超欠产，
 * 并置上月超欠产有效标志=是
 *
 * @author APS Team
 */
@Slf4j
@Component("monthOverProdTask")
public class MonthOverProdTask {

    @Autowired
    private IFactoryMonthPlanProductionFinalResultRemoteService factoryMonthPlanProdFinalRemoteService;

    /**
     * 定时计算上月超欠产
     * 根据上月计划排产量和上月硫化日完成量(合格品)计算超欠产，
     * 并置上月超欠产有效标志=是
     */
    @ApiOperation("定时计算上月超欠产")
    public void calcLastMonthOverProd() {
        log.info("定时任务-开始计算上月超欠产");
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult result = factoryMonthPlanProdFinalRemoteService.calcLastMonthOverProd();
                log.info("定时任务-计算上月超欠产结果：{}", result);
            });
        } catch (Exception e) {
            log.error("定时任务-计算上月超欠产异常", e);
        }
        log.info("定时任务-计算上月超欠产完成");
    }
}
