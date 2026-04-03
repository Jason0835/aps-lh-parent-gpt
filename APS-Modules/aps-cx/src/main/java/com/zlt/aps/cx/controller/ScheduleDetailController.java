package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 排程明细Controller
 *
 * @author APS Team
 */
@Api(tags = "排程明细管理")
@RestController
@RequestMapping("/schedule/detail")
public class ScheduleDetailController {

    @Autowired
    private CxScheduleDetailService cxScheduleDetailService;
}
