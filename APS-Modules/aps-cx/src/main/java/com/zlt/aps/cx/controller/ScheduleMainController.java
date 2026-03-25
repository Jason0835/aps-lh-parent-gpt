package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.dto.ScheduleGenerateDTO;
import com.zlt.aps.cx.dto.ScheduleQueryDTO;
import com.zlt.aps.cx.dto.ScheduleResultDTO;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 排程管理Controller
 *
 * @author APS Team
 */
@Tag(name = "排程管理", description = "排程相关接口")
@RestController
@RequestMapping("/schedule")
public class ScheduleMainController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private CxScheduleResultService cxScheduleResultService;

    @Operation(summary = "生成排程", description = "根据日期和天数生成排程")
    @PostMapping("/generate")
    public AjaxResult generateSchedule(@RequestBody ScheduleGenerateDTO dto) {
        return AjaxResult.success(scheduleService.generateSchedule(dto));
    }

    @Operation(summary = "生成单日排程", description = "生成指定日期的排程")
    @PostMapping("/generate/{date}")
    public AjaxResult generateDailySchedule(
            @Parameter(description = "排程日期") @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return AjaxResult.success(scheduleService.generateDailySchedule(date));
    }

    @Operation(summary = "确认排程", description = "确认指定排程")
    @PostMapping("/confirm/{id}")
    public AjaxResult confirmSchedule(
            @Parameter(description = "排程ID") @PathVariable Long id) {
        return AjaxResult.success(scheduleService.confirmSchedule(id));
    }

    @Operation(summary = "发布排程", description = "发布指定排程")
    @PostMapping("/release/{id}")
    public AjaxResult releaseSchedule(
            @Parameter(description = "排程ID") @PathVariable Long id) {
        return AjaxResult.success(scheduleService.releaseSchedule(id));
    }

    @Operation(summary = "批量发布排程", description = "批量发布排程")
    @PostMapping("/release/batch")
    public AjaxResult batchReleaseSchedule(@RequestBody List<Long> ids) {
        return AjaxResult.success(scheduleService.batchReleaseSchedule(ids));
    }

    @Operation(summary = "取消排程", description = "取消指定排程")
    @PostMapping("/cancel/{id}")
    public AjaxResult cancelSchedule(
            @Parameter(description = "排程ID") @PathVariable Long id) {
        return AjaxResult.success(scheduleService.cancelSchedule(id));
    }

    @Operation(summary = "调整排程", description = "调整排程（插单、换班等）")
    @PostMapping("/adjust/{id}")
    public AjaxResult adjustSchedule(
            @Parameter(description = "排程ID") @PathVariable Long id,
            @Parameter(description = "调整类型") @RequestParam String adjustType,
            @Parameter(description = "调整参数") @RequestParam(required = false) String adjustParam) {
        return AjaxResult.success(scheduleService.adjustSchedule(id, adjustType, adjustParam));
    }

    @Operation(summary = "删除排程", description = "删除指定ID的排程")
    @DeleteMapping("/{id}")
    public AjaxResult delete(
            @Parameter(description = "排程ID") @PathVariable Long id) {
        return AjaxResult.success(scheduleService.deleteSchedule(id));
    }

    @Operation(summary = "删除日期排程", description = "删除指定日期的排程")
    @DeleteMapping("/date/{date}")
    public AjaxResult deleteByDate(
            @Parameter(description = "排程日期") @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return AjaxResult.success(scheduleService.deleteScheduleByDate(date));
    }

    @Operation(summary = "分页查询排程", description = "分页查询排程列表")
    @PostMapping("/page")
    public AjaxResult pageList(@RequestBody ScheduleQueryDTO queryDTO) {
        return AjaxResult.success(cxScheduleResultService.pageList(queryDTO));
    }

    @Operation(summary = "根据日期获取排程", description = "根据日期查询排程")
    @GetMapping("/date/{scheduleDate}")
    public AjaxResult getByScheduleDate(
            @Parameter(description = "计划日期")
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate) {
        return AjaxResult.success(cxScheduleResultService.listByScheduleDate(scheduleDate));
    }

    @Operation(summary = "根据机台和日期获取排程", description = "根据机台编号和日期查询排程")
    @GetMapping("/machine")
    public AjaxResult getByMachineAndDate(
            @Parameter(description = "机台编号") @RequestParam String machineCode,
            @Parameter(description = "计划日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate) {
        return AjaxResult.success(cxScheduleResultService.listByMachineAndDate(machineCode, scheduleDate));
    }

    @Operation(summary = "获取排程详情", description = "根据排程ID查询排程详情（含明细）")
    @GetMapping("/detail/{id}")
    public AjaxResult getDetailById(
            @Parameter(description = "排程ID") @PathVariable Long id) {
        return AjaxResult.success(scheduleService.getScheduleDetail(id));
    }

    @Operation(summary = "根据ID获取排程", description = "根据排程ID查询排程")
    @GetMapping("/{id}")
    public AjaxResult getById(
            @Parameter(description = "排程ID") @PathVariable Long id) {
        return AjaxResult.success(cxScheduleResultService.getById(id));
    }

    @Operation(summary = "获取今日排程状态", description = "获取今日排程状态摘要")
    @GetMapping("/today/status")
    public AjaxResult getTodayStatus() {
        return AjaxResult.success(scheduleService.getTodayScheduleStatus());
    }

    @Operation(summary = "刷新库存预警状态", description = "刷新所有库存的预警状态")
    @PostMapping("/refresh-alert")
    public AjaxResult refreshStockAlert() {
        return AjaxResult.success(scheduleService.refreshStockAlertStatus());
    }

    @Operation(summary = "更新生产状态", description = "更新排程的生产状态")
    @PutMapping("/status/{id}")
    public AjaxResult updateProductionStatus(
            @Parameter(description = "排程ID") @PathVariable Long id,
            @Parameter(description = "生产状态") @RequestParam String productionStatus) {
        return AjaxResult.success(cxScheduleResultService.updateProductionStatus(id, productionStatus));
    }

    @Operation(summary = "更新班次计划量", description = "更新排程的班次计划量")
    @PutMapping("/shift-plan/{id}")
    public AjaxResult updateShiftPlanQty(
            @Parameter(description = "排程ID") @PathVariable Long id,
            @Parameter(description = "班次编码") @RequestParam String shiftCode,
            @Parameter(description = "计划量") @RequestParam java.math.BigDecimal planQty) {
        return AjaxResult.success(cxScheduleResultService.updateShiftPlanQty(id, shiftCode, planQty));
    }

    @Operation(summary = "更新班次完成量", description = "更新排程的班次完成量")
    @PutMapping("/shift-finish/{id}")
    public AjaxResult updateShiftFinishQty(
            @Parameter(description = "排程ID") @PathVariable Long id,
            @Parameter(description = "班次编码") @RequestParam String shiftCode,
            @Parameter(description = "完成量") @RequestParam java.math.BigDecimal finishQty) {
        return AjaxResult.success(cxScheduleResultService.updateShiftFinishQty(id, shiftCode, finishQty));
    }
}
