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
 * 包含3个定时触发入口，分别对应3个cron表达式：
 * 1. calcLastMonthOverProd()        — 每月1号凌晨3点（cron: 0 0 3 1 * ?）
 *    用上月数据写入当月月计划的"上月超欠产"栏位
 * 2. calcOverProdOnSecondLastDay()  — 每月倒数第2天凌晨3点（cron: 0 0 3 L-1 * ?）
 *    用当月数据写入下月月计划的"上月超欠产"栏位
 * 3. calcOverProdOnLastDay()        — 每月最后一天凌晨3点（cron: 0 0 3 L * ?）
 *    用当月数据写入下月月计划的"上月超欠产"栏位
 *
 * 公式：上月超欠产 = 定稿需求版本对应的月计划月底余量 - (库存抓取日 ~ 月底)的硫化日完成量
 *   - 月底余量(PLAN_SURPLUS_QTY)、库存抓取日(STOCK_CAPTURE_DATE) 取自 T_MDM_MONTH_SURPLUS，
 *     按需求版本号(MONTH_PLAN_VERSION=REQUIRE_VERSION) 匹配
 *   - 已完成量取自硫化日完成量表，日期范围 = IFNULL(STOCK_CAPTURE_DATE, 月初) ~ 月底
 * 并按阈值参数(SYS0206009)判定上月超欠产有效标志：
 * |超欠产值|(绝对值) > 阈值 → 否('0')，否则 → 是('1')；无月底余量记录 → 否('0')
 * 三次触发天然幂等，UPDATE直接覆盖写入
 *
 * @author APS Team
 */
@Slf4j
@Component("monthOverProdTask")
public class MonthOverProdTask {

    @Autowired
    private IFactoryMonthPlanProductionFinalResultRemoteService factoryMonthPlanProdFinalRemoteService;

    /**
     * 定时计算上月超欠产（每月1号凌晨3点触发，cron: 0 0 3 1 * ?）
     * 用上月数据写入当月月计划的"上月超欠产"栏位
     */
    @ApiOperation("定时计算上月超欠产-1号触发")
    public void calcLastMonthOverProd() {
        log.info("定时任务-开始计算上月超欠产(1号触发)");
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult result = factoryMonthPlanProdFinalRemoteService.calcLastMonthOverProd();
                log.info("定时任务-计算上月超欠产(1号触发)结果：{}", result);
            });
        } catch (Exception e) {
            log.error("定时任务-计算上月超欠产(1号触发)异常", e);
        }
        log.info("定时任务-计算上月超欠产(1号触发)完成");
    }

    /**
     * 定时计算超欠产（每月倒数第2天凌晨3点触发，cron: 0 0 3 L-1 * ?）
     * 用当月数据写入下月月计划的"上月超欠产"栏位
     */
    @ApiOperation("定时计算超欠产-倒数第2天触发")
    public void calcOverProdOnSecondLastDay() {
        log.info("定时任务-开始计算超欠产(倒数第2天触发)");
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult result = factoryMonthPlanProdFinalRemoteService.calcCurrentMonthOverProdForNextMonth();
                log.info("定时任务-计算超欠产(倒数第2天触发)结果：{}", result);
            });
        } catch (Exception e) {
            log.error("定时任务-计算超欠产(倒数第2天触发)异常", e);
        }
        log.info("定时任务-计算超欠产(倒数第2天触发)完成");
    }

    /**
     * 定时计算超欠产（每月最后一天凌晨3点触发，cron: 0 0 3 L * ?）
     * 用当月数据写入下月月计划的"上月超欠产"栏位
     */
    @ApiOperation("定时计算超欠产-最后一天触发")
    public void calcOverProdOnLastDay() {
        log.info("定时任务-开始计算超欠产(最后一天触发)");
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult result = factoryMonthPlanProdFinalRemoteService.calcCurrentMonthOverProdForNextMonth();
                log.info("定时任务-计算超欠产(最后一天触发)结果：{}", result);
            });
        } catch (Exception e) {
            log.error("定时任务-计算超欠产(最后一天触发)异常", e);
        }
        log.info("定时任务-计算超欠产(最后一天触发)完成");
    }
}
