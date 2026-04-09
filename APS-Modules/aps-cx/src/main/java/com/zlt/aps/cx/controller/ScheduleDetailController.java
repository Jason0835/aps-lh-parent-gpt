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
@RequestMapping("/schedule/detail")
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

    /**
     * 更新完成量
     * 业务规则：
     * 1. 只能更新完成数量
     * 2. 完成量不能超过计划量
     */
    @ApiOperation("更新完成量")
    @PutMapping("/updateCompletedQty")
    public AjaxResult updateCompletedQty(
            @RequestParam("detailId") Long detailId,
            @RequestParam("completedQuantity") Integer completedQuantity) {
        if (detailId == null || completedQuantity == null) {
            return AjaxResult.error("明细ID和完成数量不能为空");
        }

        CxScheduleDetail detail = cxScheduleDetailService.getById(detailId);
        if (detail == null) {
            return AjaxResult.error("排程明细不存在");
        }

        // 校验完成量不能超过计划量
        if (detail.getPlanQty() != null && completedQuantity > detail.getPlanQty()) {
            return AjaxResult.error("完成量不能超过计划量：" + detail.getPlanQty());
        }

        boolean result = cxScheduleDetailService.updateCompletedQuantity(detailId, completedQuantity);
        if (result) {
            log.info("更新完成量成功，明细ID：{}，完成量：{}", detailId, completedQuantity);
            return AjaxResult.success("更新完成量成功");
        } else {
            return AjaxResult.error("更新完成量失败");
        }
    }

    /**
     * 更新车次状态
     */
    @ApiOperation("更新车次状态")
    @PutMapping("/updateTripStatus")
    public AjaxResult updateTripStatus(
            @RequestParam("detailId") Long detailId,
            @RequestParam("tripStatus") String tripStatus) {
        if (detailId == null || tripStatus == null) {
            return AjaxResult.error("明细ID和车次状态不能为空");
        }

        boolean result = cxScheduleDetailService.updateTripStatus(detailId, tripStatus);
        if (result) {
            log.info("更新车次状态成功，明细ID：{}，状态：{}", detailId, tripStatus);
            return AjaxResult.success("更新车次状态成功");
        } else {
            return AjaxResult.error("更新车次状态失败");
        }
    }

    /**
     * 修改排程明细
     * 可修改字段：计划数量、备注、原因分析等
     */
    @ApiOperation("修改排程明细")
    @PutMapping("/update")
    public AjaxResult update(@RequestBody CxScheduleDetail detail) {
        if (detail.getId() == null) {
            return AjaxResult.error("明细ID不能为空");
        }

        CxScheduleDetail existDetail = cxScheduleDetailService.getById(detail.getId());
        if (existDetail == null) {
            return AjaxResult.error("排程明细不存在");
        }

        // 校验计划量不能小于完成量
        if (detail.getPlanQty() != null && existDetail.getActualQty() != null 
                && detail.getPlanQty() < existDetail.getActualQty()) {
            return AjaxResult.error("计划量不能小于已完成量：" + existDetail.getActualQty());
        }

        boolean result = cxScheduleDetailService.updateById(detail);
        if (result) {
            log.info("修改排程明细成功，明细ID：{}", detail.getId());
            return AjaxResult.success("修改成功");
        } else {
            return AjaxResult.error("修改失败");
        }
    }

    /**
     * 删除排程明细（单个）
     * 业务规则：
     * 1. 已完成状态的明细不允许删除
     */
    @ApiOperation("删除排程明细")
    @DeleteMapping("/remove/{detailId}")
    public AjaxResult remove(@PathVariable("detailId") Long detailId) {
        if (detailId == null) {
            return AjaxResult.error("明细ID不能为空");
        }

        CxScheduleDetail detail = cxScheduleDetailService.getById(detailId);
        if (detail == null) {
            return AjaxResult.error("排程明细不存在");
        }

        // 已完成状态的明细不允许删除
        if ("COMPLETED".equals(detail.getStatus())) {
            return AjaxResult.error("已完成状态的明细不允许删除");
        }

        boolean result = cxScheduleDetailService.removeById(detailId);
        if (result) {
            log.info("删除排程明细成功，明细ID：{}", detailId);
            return AjaxResult.success("删除成功");
        } else {
            return AjaxResult.error("删除失败");
        }
    }

    /**
     * 批量删除排程明细
     * 业务规则：
     * 1. 已完成状态的明细不允许删除
     */
    @ApiOperation("批量删除排程明细")
    @DeleteMapping("/batchRemove")
    public AjaxResult batchRemove(@RequestBody List<Long> detailIds) {
        if (detailIds == null || detailIds.isEmpty()) {
            return AjaxResult.error("请选择需要删除的明细记录");
        }

        List<CxScheduleDetail> details = cxScheduleDetailService.listByIds(detailIds);
        for (CxScheduleDetail detail : details) {
            if ("COMPLETED".equals(detail.getStatus())) {
                return AjaxResult.error("删除失败：存在已完成状态的明细记录，不允许删除");
            }
        }

        boolean result = cxScheduleDetailService.removeByIds(detailIds);
        if (result) {
            log.info("批量删除排程明细成功，数量：{}", detailIds.size());
            return AjaxResult.success("批量删除成功");
        } else {
            return AjaxResult.error("批量删除失败");
        }
    }
}
