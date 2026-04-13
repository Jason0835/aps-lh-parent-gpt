package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 排程明细Controller
 * 成型排程子表，主要用于查询、修改和删除明细记录
 *
 * @author APS Team
 */
@Slf4j
@Api(tags = "排程明细管理")
@RestController
@RequestMapping("/cxScheduleDetail")
public class ScheduleDetailController {

    @Autowired
    private CxScheduleDetailService cxScheduleDetailService;

    /**
     * 根据主表ID查询明细列表
     */
    @ApiOperation("根据主表ID查询明细列表")
    @GetMapping("/listByMainId/{mainId}")
    public AjaxResult listByMainId(@PathVariable("mainId") Long mainId) {
        if (mainId == null) {
            return AjaxResult.error("主表ID不能为空");
        }
        List<CxScheduleDetail> details = cxScheduleDetailService.listByMainId(mainId);
        return AjaxResult.success(details);
    }

    /**
     * 根据机台和日期查询明细
     */
    @ApiOperation("根据机台和日期查询明细")
    @GetMapping("/listByMachineAndDate")
    public AjaxResult listByMachineAndDate(
            @RequestParam("cxMachineCode") String cxMachineCode,
            @RequestParam("scheduleDate") LocalDate scheduleDate) {
        if (cxMachineCode == null || scheduleDate == null) {
            return AjaxResult.error("机台编码和排程日期不能为空");
        }
        List<CxScheduleDetail> details = cxScheduleDetailService.listByMachineAndDate(cxMachineCode, scheduleDate);
        return AjaxResult.success(details);
    }

    /**
     * 根据班次查询明细
     */
    @ApiOperation("根据班次查询明细")
    @GetMapping("/listByShift")
    public AjaxResult listByShift(
            @RequestParam("mainId") Long mainId,
            @RequestParam("shiftCode") String shiftCode) {
        if (mainId == null || shiftCode == null) {
            return AjaxResult.error("主表ID和班次编码不能为空");
        }
        List<CxScheduleDetail> details = cxScheduleDetailService.listByShift(mainId, shiftCode);
        return AjaxResult.success(details);
    }

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping("/{detailId}")
    public AjaxResult getById(@PathVariable("detailId") Long detailId) {
        if (detailId == null) {
            return AjaxResult.error("明细ID不能为空");
        }
        CxScheduleDetail detail = cxScheduleDetailService.getById(detailId);
        if (detail == null) {
            return AjaxResult.error("排程明细不存在");
        }
        return AjaxResult.success(detail);
    }
}
