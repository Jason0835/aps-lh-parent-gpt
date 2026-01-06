package com.zlt.aps.monthplan.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.zlt.aps.monthplan.api.domain.dto.MpWeekRollAdjustDTO;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpWeekRollAdjustRemoteService.java
 * 描    述：IMpWeekRollAdjustRemoteService-周滚动调整前端接口
 *@author zlt
 *@date 2025-12-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMpWeekRollAdjustRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpWeekRollAdjustRemoteService {

    /**
     * 获取调整明细列表
     */
    @ApiOperation("获取调整明细列表")
    @PostMapping("/mpWeekRollAdjust/getAdjustDetailList")
    TableDataInfo getAdjustDetailList(@RequestBody MpWeekRollAdjustDTO weekRollAdjustDTO);

    /**
     * 自动调整
     */
    @ApiOperation("自动调整")
    @PostMapping("/mpWeekRollAdjust/autoAdjust")
    AjaxResult autoAdjust(@RequestBody MpWeekRollAdjustDTO weekRollAdjustDTO);

    /**
     * 确认调整结果
     */
    @ApiOperation("确认调整结果")
    @PostMapping("/mpWeekRollAdjust/confirmAdjust")
    AjaxResult confirmAdjust(@RequestBody MpWeekRollAdjustDTO weekRollAdjustDTO);

}
