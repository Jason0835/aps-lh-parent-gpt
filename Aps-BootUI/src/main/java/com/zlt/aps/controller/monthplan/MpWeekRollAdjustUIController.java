package com.zlt.aps.controller.monthplan;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.mp.api.domain.dto.MpWeekRollAdjustDTO;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.service.IFactoryMonthPlanMouldDayResultRemoteService;
import com.zlt.aps.mp.api.service.IMpWeekRollAdjustRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;

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
    @Autowired
    private IFactoryMonthPlanMouldDayResultRemoteService iFactoryMonthPlanMouldDayResultService;

    /**
     * 获取调整明细列表
     */
    @RequiresPermissions("monthplan:mpWeekRollAdjust:getAdjustDetailList")
    @ApiOperation("获取调整明细列表")
    @PostMapping("/getAdjustDetailList")
    @ResponseBody
    public TableDataInfo getAdjustDetailList(MpWeekRollAdjustDTO weekRollAdjustDTO) {
        return mpWeekRollAdjustRemoteService.getAdjustDetailList(weekRollAdjustDTO);
    }

    /**
     * 自动调整
     */
    @RequiresPermissions("monthplan:mpWeekRollAdjust:autoAdjust")
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

    @ApiOperation("导出调整版本")
    @RequiresPermissions("monthplan:mpWeekRollAdjust:export")
    @GetMapping({"/export"})
    @ResponseBody
    public void export(HttpServletResponse response, FactoryMonthPlanProductionFinalResult entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.mpWeekRollAdjust.modelName") + DateUtil.format(LocalDateTime.now(),"yyyyMMdd");
        FactoryMonthPlanMouldDayResult result = new FactoryMonthPlanMouldDayResult();
        result.setFactoryCode(entity.getFactoryCode());
        result.setProductionVersion(entity.getProductionVersion());
        result.setStructureName(entity.getStructureName());
        result.setYear(entity.getYear());
        result.setMonth(entity.getMonth());
        byte[] excelBytes = iFactoryMonthPlanMouldDayResultService.exportAdjuest(result, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 重新计算
     */
    @ApiOperation("重新计算")
    @PostMapping("/recalculate")
    @ResponseBody
    public AjaxResult recalculate(MpWeekRollAdjustDTO weekRollAdjustDTO) {
        return mpWeekRollAdjustRemoteService.recalculate(weekRollAdjustDTO);
    }
}
