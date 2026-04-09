package com.zlt.aps.controller.cx;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.service.ICxScheduleDetailRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxScheduleDetailUIController.java
 * 描    述：成型排程明细 UI控制层类（子表）
 * @author APS Team
 * @date 2026-04-09
 * @version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@Slf4j
@Api(tags = "成型排程明细管理")
@Controller
@RequestMapping("/cx/cxScheduleDetail")
public class CxScheduleDetailUIController {

    @Autowired
    private ICxScheduleDetailRemoteService iCxScheduleDetailService;

    private final String prefix = "aps/cx/cxScheduleDetail";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:cxScheduleDetail:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/cxScheduleDetail";
    }

    /**
     * 根据主表ID查询明细列表
     */
    @ApiOperation("根据主表ID查询明细列表")
    @RequiresPermissions("cx:cxScheduleDetail:list")
    @GetMapping("/listByMainId/{mainId}")
    @ResponseBody
    public AjaxResult listByMainId(@PathVariable("mainId") Long mainId) {
        return iCxScheduleDetailService.listByMainId(mainId);
    }

    /**
     * 根据机台和日期查询明细
     */
    @ApiOperation("根据机台和日期查询明细")
    @RequiresPermissions("cx:cxScheduleDetail:list")
    @GetMapping("/listByMachineAndDate")
    @ResponseBody
    public AjaxResult listByMachineAndDate(
            @RequestParam("cxMachineCode") String cxMachineCode,
            @RequestParam("scheduleDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate) {
        return iCxScheduleDetailService.listByMachineAndDate(cxMachineCode, scheduleDate);
    }

    /**
     * 根据班次查询明细
     */
    @ApiOperation("根据班次查询明细")
    @RequiresPermissions("cx:cxScheduleDetail:list")
    @GetMapping("/listByShift")
    @ResponseBody
    public AjaxResult listByShift(
            @RequestParam("mainId") Long mainId,
            @RequestParam("shiftCode") String shiftCode) {
        return iCxScheduleDetailService.listByShift(mainId, shiftCode);
    }

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("获取详细信息")
    @RequiresPermissions("cx:cxScheduleDetail:view")
    @GetMapping("/{detailId}")
    @ResponseBody
    public AjaxResult getById(@PathVariable("detailId") Long detailId) {
        return iCxScheduleDetailService.getById(detailId);
    }

    /**
     * 更新完成量
     */
    @ApiOperation("更新完成量")
    @RequiresPermissions("cx:cxScheduleDetail:edit")
    @PutMapping("/updateCompletedQty")
    @ResponseBody
    public AjaxResult updateCompletedQty(
            @RequestParam("detailId") Long detailId,
            @RequestParam("completedQuantity") Integer completedQuantity) {
        return iCxScheduleDetailService.updateCompletedQty(detailId, completedQuantity);
    }

    /**
     * 更新车次状态
     */
    @ApiOperation("更新车次状态")
    @RequiresPermissions("cx:cxScheduleDetail:edit")
    @PutMapping("/updateTripStatus")
    @ResponseBody
    public AjaxResult updateTripStatus(
            @RequestParam("detailId") Long detailId,
            @RequestParam("tripStatus") String tripStatus) {
        return iCxScheduleDetailService.updateTripStatus(detailId, tripStatus);
    }

    /**
     * 修改排程明细
     */
    @ApiOperation("修改排程明细")
    @RequiresPermissions("cx:cxScheduleDetail:edit")
    @PutMapping("/update")
    @ResponseBody
    public AjaxResult update(@RequestBody CxScheduleDetail detail) {
        return iCxScheduleDetailService.update(detail);
    }

    /**
     * 删除排程明细（单个）
     */
    @ApiOperation("删除排程明细")
    @RequiresPermissions("cx:cxScheduleDetail:remove")
    @DeleteMapping("/remove/{detailId}")
    @ResponseBody
    public AjaxResult remove(@PathVariable("detailId") Long detailId) {
        return iCxScheduleDetailService.remove(detailId);
    }

    /**
     * 批量删除排程明细
     */
    @ApiOperation("批量删除排程明细")
    @RequiresPermissions("cx:cxScheduleDetail:remove")
    @DeleteMapping("/batchRemove")
    @ResponseBody
    public AjaxResult batchRemove(@RequestBody List<Long> detailIds) {
        return iCxScheduleDetailService.batchRemove(detailIds);
    }
}
