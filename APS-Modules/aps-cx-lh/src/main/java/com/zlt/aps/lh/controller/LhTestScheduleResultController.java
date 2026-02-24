package com.zlt.aps.lh.controller;

import com.zlt.aps.lh.api.domain.entity.LhTestScheduleResult;
import com.zlt.aps.lh.service.LhTestScheduleResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.lh.api.domain.dto.AutoLhScheduleResultDTO;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2025/2/13
 */
@Slf4j
@Api(tags = "硫化Test排程")
@RestController
@RequestMapping("/lhTestScheduleResult")
public class LhTestScheduleResultController extends AbstractDocBizController<LhTestScheduleResult> {


    @Autowired
    private LhTestScheduleResultService lhTestScheduleResultService;

    @Autowired
    RedisService redisService;


    @ApiOperation("自动排程")
    @PostMapping("/autoLhTestScheduleResult")
    public AjaxResult autoLhTestScheduleResult(@RequestBody AutoLhScheduleResultDTO autoLhScheduleResultDTO){
        lhTestScheduleResultService.autoLhTestScheduleResult(autoLhScheduleResultDTO);
        return AjaxResult.success();
    }

    @Override
    protected IDocService getDocService() {
        return lhTestScheduleResultService;
    }

    @Override
    protected String getTypeCode() {
        return "LH2025213";
    }
}
