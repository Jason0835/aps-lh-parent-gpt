package com.zlt.aps.controller.monthplan;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.dto.MpWeekRollAdjustDTO;
import com.zlt.aps.monthplan.api.service.IMpWeekRollAdjustRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpWeekRollAdjustUIController.java
 * 描    述：周程滚动调整 UI控制层类：....
 *@author zlt
 *@date 2025-12-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "周程滚动调整")
@Controller
@RequestMapping("/monthplan/mpWeekRollAdjust")
public class MpWeekRollAdjustUIController extends BaseController {

    @Autowired
    private IMpWeekRollAdjustRemoteService mpWeekRollAdjustRemoteService;

    /**
     * 获取调整明细列表
     */
    @ApiOperation("获取调整明细列表")
    @PostMapping("/getAdjustDetailList")
    @ResponseBody
    public TableDataInfo getAdjustDetailList(MpWeekRollAdjustDTO weekRollAdjustDTO) {
        return mpWeekRollAdjustRemoteService.getAdjustDetailList(weekRollAdjustDTO);
    }

    /**
     * 自动调整
     */
    @ApiOperation("自动调整")
    @PostMapping("/autoAdjust")
    @ResponseBody
    public AjaxResult autoAdjust(MpWeekRollAdjustDTO weekRollAdjustDTO) {
        return mpWeekRollAdjustRemoteService.autoAdjust(weekRollAdjustDTO);
    }

    /**
     * 确认调整结果
     */
    @ApiOperation("确认调整结果")
    @PostMapping("/confirmAdjust")
    @ResponseBody
    public AjaxResult confirmAdjust(MpWeekRollAdjustDTO weekRollAdjustDTO) {
        return mpWeekRollAdjustRemoteService.confirmAdjust(weekRollAdjustDTO);
    }


}
