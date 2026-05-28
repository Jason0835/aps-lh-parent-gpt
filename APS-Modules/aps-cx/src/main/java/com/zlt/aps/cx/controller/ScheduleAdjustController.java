package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.service.ScheduleAdjustService;
import com.zlt.aps.cx.vo.ScheduleAdjustResultVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型排程计划调整控制器
 *
 * <p>提供排程执行后的动态调整功能，由外部定时任务服务调用。
 * 无需在本文件中实现定时触发逻辑。
 *
 * <p>主要功能：
 * <ul>
 *   <li>交班库存时长调整计划车数</li>
 *   <li>计划滚动调整（胎面供应判断 + 顺位重置 + 时间重算）</li>
 *   <li>8个班滚动调整范围查询</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Api(tags = "排程计划调整")
@RestController
@RequestMapping("/schedule/adjust")
public class ScheduleAdjustController {

    @Autowired
    private ScheduleAdjustService scheduleAdjustService;

    @ApiOperation(value = "交班库存时长调整", notes = "根据交班库存时长调整计划车数：库存时长<阈值补1车，同时对库存时长最多的胎胚减1车")
    @PostMapping("/stock-hours")
    public AjaxResult adjustByStockHours(
            @ApiParam(value = "工厂编码") @RequestParam(required = false) String factoryCode,
            @ApiParam(value = "排程日期（yyyy-MM-dd）") @RequestParam String scheduleDate,
            @ApiParam(value = "当前班次（CLASS1~CLASS8）") @RequestParam String shiftClass) {

        if (scheduleDate == null || scheduleDate.isEmpty()) {
            return AjaxResult.error("排程日期不能为空");
        }
        if (shiftClass == null || shiftClass.isEmpty()) {
            return AjaxResult.error("当前班次不能为空");
        }

        try {
            ScheduleAdjustResultVo result = scheduleAdjustService.adjustByStockHours(
                    factoryCode, scheduleDate, shiftClass);
            if (result.isSuccess()) {
                return AjaxResult.success(result);
            } else {
                return AjaxResult.error(result.getMessage(), result);
            }
        } catch (Exception e) {
            log.error("交班库存时长调整失败：{}", e.getMessage(), e);
            return AjaxResult.error("交班库存时长调整失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "计划滚动调整", notes = "在交接班前进行计划滚动调整：重置顺位、计算成型时间、判断胎面供应")
    @PostMapping("/rolling")
    public AjaxResult rollingAdjust(
            @ApiParam(value = "工厂编码") @RequestParam(required = false) String factoryCode,
            @ApiParam(value = "排程日期（yyyy-MM-dd）") @RequestParam String scheduleDate,
            @ApiParam(value = "当前班次（CLASS1~CLASS8）") @RequestParam String shiftClass) {

        if (scheduleDate == null || scheduleDate.isEmpty()) {
            return AjaxResult.error("排程日期不能为空");
        }
        if (shiftClass == null || shiftClass.isEmpty()) {
            return AjaxResult.error("当前班次不能为空");
        }

        try {
            ScheduleAdjustResultVo result = scheduleAdjustService.rollingAdjust(
                    factoryCode, scheduleDate, shiftClass);
            if (result.isSuccess()) {
                return AjaxResult.success(result);
            } else {
                return AjaxResult.error(result.getMessage(), result);
            }
        } catch (Exception e) {
            log.error("计划滚动调整失败：{}", e.getMessage(), e);
            return AjaxResult.error("计划滚动调整失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "获取调整班次范围", notes = "根据当前班次获取8班滚动调整中需要调整的班次列表")
    @GetMapping("/shift-range")
    public AjaxResult getAdjustShiftRange(
            @ApiParam(value = "当前班次（CLASS1~CLASS8）") @RequestParam String shiftClass) {

        if (shiftClass == null || shiftClass.isEmpty()) {
            return AjaxResult.error("当前班次不能为空");
        }

        try {
            List<String> shiftRange = scheduleAdjustService.getAdjustShiftRange(shiftClass);
            return AjaxResult.success(shiftRange);
        } catch (Exception e) {
            log.error("获取调整班次范围失败：{}", e.getMessage(), e);
            return AjaxResult.error("获取调整班次范围失败：" + e.getMessage());
        }
    }

    @ApiOperation(value = "机台滚动重排", notes = "以机台维度滚动重排程：阶段A应用完工量→阶段B库存时长补减车→阶段C后续班次重排")
    @PostMapping("/reschedule")
    public AjaxResult rescheduleByMachine(
            @ApiParam(value = "工厂编码") @RequestParam(required = false) String factoryCode,
            @ApiParam(value = "排程日期（yyyy-MM-dd）") @RequestParam String scheduleDate,
            @ApiParam(value = "触发班次（CLASS1~CLASS7）") @RequestParam String triggerShiftClass,
            @ApiParam(value = "成型机台编码") @RequestParam String machineCode) {

        if (scheduleDate == null || scheduleDate.isEmpty()) {
            return AjaxResult.error("排程日期不能为空");
        }
        if (triggerShiftClass == null || triggerShiftClass.isEmpty()) {
            return AjaxResult.error("触发班次不能为空");
        }
        if (machineCode == null || machineCode.isEmpty()) {
            return AjaxResult.error("机台编码不能为空");
        }

        try {
            ScheduleAdjustResultVo result = scheduleAdjustService.rescheduleByMachine(
                    factoryCode, scheduleDate, triggerShiftClass, machineCode);
            if (result.isSuccess()) {
                return AjaxResult.success(result);
            } else {
                return AjaxResult.error(result.getMessage(), result);
            }
        } catch (Exception e) {
            log.error("机台滚动重排失败：{}", e.getMessage(), e);
            return AjaxResult.error("机台滚动重排失败：" + e.getMessage());
        }
    }
}
