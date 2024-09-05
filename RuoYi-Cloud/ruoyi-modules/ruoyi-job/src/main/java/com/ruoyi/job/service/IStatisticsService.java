package com.ruoyi.job.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 数据汇总统计服务对外暴露接口
 * @Description
 * @Author hakimryan
 * @Date 2021-9-15 9:21:55
 */
@FeignClient(contextId = "IStatisticsService", value = ServiceNameConstants.APS_MPS_SERVICE)
public interface IStatisticsService {

    String prefix = "/mps/statistics";

    /**
     * 统计月度计划的实际超欠产
     */
    @PostMapping(value = prefix + "/monthPlan/actualOverProduction")
    AjaxResult monthPlanActualOverProduction();

}
