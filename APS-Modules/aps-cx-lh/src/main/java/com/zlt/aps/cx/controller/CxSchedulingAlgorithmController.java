package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.CommonRedisService;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.service.CxSchedulingAlgorithmResultService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.mapper.LhScheduleResultEntityMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 * 成型算法引擎后端统一入口
 *
 * @author LTL-Nick
 */
@Slf4j
@Api("成型算法")
@RestController
@RequestMapping("/cxSchedulingAlgorithm")
public class CxSchedulingAlgorithmController {

    /**
     * 自动排程抓取redis key 前缀
     */
    public static final String AUTO_SCHEDULE_PREFIX = "auto:schedule:cx:";

    @Resource
    private CommonRedisService commonRedisService;
    @Resource
    private LhScheduleResultEntityMapper lhScheduleResultEntityMapper;
    @Resource
    private CxSchedulingAlgorithmResultService cxSchedulingAlgorithmResultService;

    /**
     * title：成型自动排程
     *
     * @param cxScheduleResult 排程日期对象
     * @return AjaxResult 执行结果
     */
    @Log(title = "ui.data.column.cx.calculateCarbonationPlan", businessType = BusinessType.GENERATE)
    @ApiOperation("成型自动排程")
    @PutMapping("/calculateCarbonationPlan")
    public AjaxResult calculateCarbonationPlan(@RequestBody CxScheduleResult cxScheduleResult) {
        if (cxScheduleResult == null || cxScheduleResult.getScheduleDate() == null) {
            return AjaxResult.error("排程日期为空,停止排程！");
        }

        String scheduleDateStr = DateUtils.parseDateToStr("yyyyMMdd", cxScheduleResult.getScheduleDate());
        String redisKey = AUTO_SCHEDULE_PREFIX + scheduleDateStr;
        if (Boolean.TRUE.equals(commonRedisService.hasKey(redisKey))) {
            return AjaxResult.error("【成型工序】正在进行自动排程，请5分钟后重试！");
        }

        QueryWrapper<LhScheduleResult> lhScheduleResultQueryWrapper = new QueryWrapper<>();
        lhScheduleResultQueryWrapper.eq("schedule_date", cxScheduleResult.getScheduleDate());
        Long lhScheduleResultsCount = lhScheduleResultEntityMapper.selectCount(lhScheduleResultQueryWrapper);
        if (lhScheduleResultsCount <= 0)  {
            return AjaxResult.error("今日硫化计划不存在，无法排程！");
        }

        try {
            commonRedisService.setIfAbsent(redisKey, "1", 10L, TimeUnit.SECONDS);
            cxSchedulingAlgorithmResultService.calculateMoldingPlan(cxScheduleResult.getScheduleDate(), 1 , cxScheduleResult.getFactoryCode());
            return AjaxResult.success();
        } finally {
            //移除redis锁
            commonRedisService.delRedisKey(redisKey);
        }
    }
}
