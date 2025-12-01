package com.zlt.aps.job.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.job.service.ILhMonthPlanSurplusTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;

/**
 * 硫化月度剩余量
 *
 * @author Liam
 * @since 2025/4/3
 */
@Slf4j
@Component("lhMonthSurplusTask")
public class LhMonthSurplusTask {

    @Resource
    private ILhMonthPlanSurplusTaskService iLhMonthPlanSurplusTaskService;

    /**
     * 更新昨天的月度外胎完成量
     */
    public void updateMonthPlanSurplus() {
        // 更新昨天的月度外胎完成量
        Date yesterday = DateUtils.addDays(DateUtils.getNowDate(), -1);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(yesterday);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        log.info("更新月度外胎完成量,{}年{}月.start", year, month);
        AjaxResult result = iLhMonthPlanSurplusTaskService.updateMonthPlanSurplus(year, month);
        log.info("更新月度外胎完成量,{}年{}月.result{}", year, month, result);
    }

}
