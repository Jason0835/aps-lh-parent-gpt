package com.zlt.aps.job.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 定时任务-硫化月度完成量相关
 *
 * @author Liam
 * @since 2025/4/3
 */
@FeignClient(contextId = "ILhMonthPlanSurplusTaskService", name = "${remoteApi.value.cxlh:aps-cxlh}")
public interface ILhMonthPlanSurplusTaskService {

    /**
     * 更新对应年月的月度外胎完成量
     * @param year 年
     * @param month 月
     * @return 结果
     */
    @PostMapping("/lhMonthPlanSurplus/updateMonthPlanSurplus/{year}/{month}")
    AjaxResult updateMonthPlanSurplus(@PathVariable("year") int year, @PathVariable("month") int month);
}
