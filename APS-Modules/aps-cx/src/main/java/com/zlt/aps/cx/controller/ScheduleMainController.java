package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.dto.ScheduleGenerateDTO;
import com.zlt.aps.cx.dto.ScheduleQueryDTO;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.service.ScheduleService;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.mp.api.domain.entity.CxScheduleResultIssue;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 排程管理Controller
 *
 * @author APS Team
 */
@Api(tags = "排程管理")
@RestController
@RequestMapping("/schedule")
public class ScheduleMainController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private CxScheduleResultService cxScheduleResultService;

    @Autowired
    private IMesItfService mesItfService;

    @ApiOperation(value = "生成排程", notes = "根据日期和天数生成排程")
    @PostMapping("/generate")
    public AjaxResult generateSchedule(@RequestBody ScheduleGenerateDTO dto) {
        // TODO: 待实现 generateSchedule
        return AjaxResult.success(new ArrayList<>());
    }

    @ApiOperation(value = "生成单日排程", notes = "生成指定日期的排程")
    @PostMapping("/generate/{date}")
    public AjaxResult generateDailySchedule(
            @ApiParam(value = "排程日期") @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        // TODO: 待实现 generateDailySchedule
        return AjaxResult.success(new ArrayList<>());
    }

    @ApiOperation(value = "确认排程", notes = "确认指定排程")
    @PostMapping("/confirm/{id}")
    public AjaxResult confirmSchedule(
            @ApiParam(value = "排程ID") @PathVariable Long id) {
        // TODO: 待实现 confirmSchedule
        return AjaxResult.success(true);
    }

    @ApiOperation(value = "发布排程", notes = "发布指定排程")
    @PostMapping("/release/{id}")
    public AjaxResult releaseSchedule(
            @ApiParam(value = "排程ID") @PathVariable Long id) {
        // TODO: 待实现 releaseSchedule
        return AjaxResult.success(true);
    }

    @ApiOperation(value = "批量发布排程", notes = "批量发布排程")
    @PostMapping("/release/batch")
    public AjaxResult batchReleaseSchedule(@RequestBody List<Long> ids) {
        // TODO: 待实现 batchReleaseSchedule
        return AjaxResult.success(true);
    }

    @ApiOperation(value = "取消排程", notes = "取消指定排程")
    @PostMapping("/cancel/{id}")
    public AjaxResult cancelSchedule(
            @ApiParam(value = "排程ID") @PathVariable Long id) {
        // TODO: 待实现 cancelSchedule
        return AjaxResult.success(true);
    }

    @ApiOperation(value = "调整排程", notes = "调整排程（插单、换班等）")
    @PostMapping("/adjust/{id}")
    public AjaxResult adjustSchedule(
            @ApiParam(value = "排程ID") @PathVariable Long id,
            @ApiParam(value = "调整类型") @RequestParam String adjustType,
            @ApiParam(value = "调整参数") @RequestParam(required = false) String adjustParam) {
        // TODO: 待实现 adjustSchedule
        return AjaxResult.success(null);
    }

    @ApiOperation(value = "删除排程", notes = "删除指定ID的排程")
    @DeleteMapping("/{id}")
    public AjaxResult delete(
            @ApiParam(value = "排程ID") @PathVariable Long id) {
        // TODO: 待实现 deleteSchedule
        return AjaxResult.success(true);
    }

    @ApiOperation(value = "删除日期排程", notes = "删除指定日期的排程")
    @DeleteMapping("/date/{date}")
    public AjaxResult deleteByDate(
            @ApiParam(value = "排程日期") @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        // TODO: 待实现 deleteScheduleByDate
        return AjaxResult.success(true);
    }

    @ApiOperation(value = "分页查询排程", notes = "分页查询排程列表")
    @PostMapping("/page")
    public AjaxResult pageList(@RequestBody ScheduleQueryDTO queryDTO) {
        return AjaxResult.success(cxScheduleResultService.pageList(queryDTO));
    }

    @ApiOperation(value = "根据日期获取排程", notes = "根据日期查询排程")
    @GetMapping("/date/{scheduleDate}")
    public AjaxResult getByScheduleDate(
            @ApiParam(value = "计划日期")
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate) {
        return AjaxResult.success(cxScheduleResultService.listByScheduleDate(scheduleDate));
    }

    @ApiOperation(value = "根据机台和日期获取排程", notes = "根据机台编号和日期查询排程")
    @GetMapping("/machine")
    public AjaxResult getByMachineAndDate(
            @ApiParam(value = "机台编号") @RequestParam String machineCode,
            @ApiParam(value = "计划日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate) {
        return AjaxResult.success(cxScheduleResultService.listByMachineAndDate(machineCode, scheduleDate));
    }

    @ApiOperation(value = "获取排程详情", notes = "根据排程ID查询排程详情（含明细）")
    @GetMapping("/main/detail/{id}")
    public AjaxResult getDetailById(
            @ApiParam(value = "排程ID") @PathVariable Long id) {
        // TODO: 待实现 getScheduleDetail
        return AjaxResult.success(null);
    }

    @ApiOperation(value = "根据ID获取排程", notes = "根据排程ID查询排程")
    @GetMapping("/{id}")
    public AjaxResult getById(
            @ApiParam(value = "排程ID") @PathVariable Long id) {
        return AjaxResult.success(cxScheduleResultService.getById(id));
    }

    @ApiOperation(value = "获取今日排程状态", notes = "获取今日排程状态摘要")
    @GetMapping("/today/status")
    public AjaxResult getTodayStatus() {
        // TODO: 待实现 getTodayScheduleStatus
        return AjaxResult.success(null);
    }

    @ApiOperation(value = "刷新库存预警状态", notes = "刷新所有库存的预警状态")
    @PostMapping("/refresh-alert")
    public AjaxResult refreshStockAlert() {
        // TODO: 待实现 refreshStockAlertStatus
        return AjaxResult.success(true);
    }

    @ApiOperation(value = "更新生产状态", notes = "更新排程的生产状态")
    @PutMapping("/status/{id}")
    public AjaxResult updateProductionStatus(
            @ApiParam(value = "排程ID") @PathVariable Long id,
            @ApiParam(value = "生产状态") @RequestParam String productionStatus) {
        return AjaxResult.success(cxScheduleResultService.updateProductionStatus(id, productionStatus));
    }

    @ApiOperation(value = "更新班次计划量", notes = "更新排程的班次计划量")
    @PutMapping("/shift-plan/{id}")
    public AjaxResult updateShiftPlanQty(
            @ApiParam(value = "排程ID") @PathVariable Long id,
            @ApiParam(value = "班次编码") @RequestParam String shiftCode,
            @ApiParam(value = "计划量") @RequestParam java.math.BigDecimal planQty) {
        return AjaxResult.success(cxScheduleResultService.updateShiftPlanQty(id, shiftCode, planQty));
    }

    @ApiOperation(value = "更新班次完成量", notes = "更新排程的班次完成量")
    @PutMapping("/shift-finish/{id}")
    public AjaxResult updateShiftFinishQty(
            @ApiParam(value = "排程ID") @PathVariable Long id,
            @ApiParam(value = "班次编码") @RequestParam String shiftCode,
            @ApiParam(value = "完成量") @RequestParam java.math.BigDecimal finishQty) {
        return AjaxResult.success(cxScheduleResultService.updateShiftFinishQty(id, shiftCode, finishQty));
    }

    /**
     * 成型排程结果下发到MES中间表
     * 业务规则：
     * 1. 查询当天的排程日期，获取8班数据
     * 2. 8班对应关系：
     *    - 1-2班：当天的早、中班（夜班已生产）
     *    - 3-5班：第二天的夜、早、中班
     *    - 6-8班：第三天的夜、早、中班
     * 3. 中间表映射：1班=夜班，2班=早班，3班=中班
     * 4. 当天、隔天数据更新（存在则更新，不存在则插入）
     * 5. 第三天数据下发（插入）
     *
     * @return 下发结果
     */
    @ApiOperation(value = "成型排程结果下发到MES", notes = "将成型排程结果下发到MES中间表，8班数据对应3天班次")
    @Log(title = "成型排程结果下发", businessType = BusinessType.PUBLISH)
    @PostMapping("/issueToMes")
    public AjaxResult issueCxScheduleResultToMes() {

        // 获取今天、明天、后天的日期
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);

        // 只查询当天的排程结果数据（包含8班数据）
        List<CxScheduleResult> scheduleResultList = cxScheduleResultService.listByScheduleDate(today);

        if (scheduleResultList.isEmpty()) {
            return AjaxResult.error("没有需要下发的成型排程结果数据");
        }

        // 转换为3天的下发数据
        List<CxScheduleResultIssue> day1IssueList = new ArrayList<>();    // 当天（更新）
        List<CxScheduleResultIssue> day2IssueList = new ArrayList<>();    // 隔天（更新）
        List<CxScheduleResultIssue> day3IssueList = new ArrayList<>();    // 后天（插入）

        for (CxScheduleResult source : scheduleResultList) {
            // 第1天（当天）- 更新2班数据（早中班）
            CxScheduleResultIssue day1Issue = convertToDay1IssueEntity(source, today);
            if (day1Issue != null) {
                day1IssueList.add(day1Issue);
            }

            // 第2天（隔天）- 更新3班数据（夜早中班）
            CxScheduleResultIssue day2Issue = convertToDay2IssueEntity(source, tomorrow);
            if (day2Issue != null) {
                day2IssueList.add(day2Issue);
            }

            // 第3天（后天）- 下发3班数据（夜早中班）
            CxScheduleResultIssue day3Issue = convertToDay3IssueEntity(source, dayAfterTomorrow);
            if (day3Issue != null) {
                day3IssueList.add(day3Issue);
            }
        }

        if (day1IssueList.isEmpty() && day2IssueList.isEmpty() && day3IssueList.isEmpty()) {
            return AjaxResult.error("没有需要下发的成型排程结果数据");
        }

        // 合并所有数据并调用下发接口
        List<CxScheduleResultIssue> allIssueList = new ArrayList<>();
        allIssueList.addAll(day1IssueList);
        allIssueList.addAll(day2IssueList);
        allIssueList.addAll(day3IssueList);

        // 通过Feign客户端调用itf模块的下发接口
        return mesItfService.issueCxScheduleResult(allIssueList);
    }

    /**
     * 转换为第1天（当天）的下发实体
     * 8班数据：1班(早)、2班(中) -> 中间表：1班(夜)=空, 2班(早)=1班, 3班(中)=2班
     * 业务规则：只更新2班数据（早中班），即中间表的2班(早)和3班(中)
     */
    private CxScheduleResultIssue convertToDay1IssueEntity(CxScheduleResult source, LocalDate scheduleDate) {
        if (source == null) {
            return null;
        }

        CxScheduleResultIssue target = new CxScheduleResultIssue();

        // 基础字段映射
        target.setId(source.getId());
        target.setCxBatchNo(source.getCxBatchNo());
        target.setOrderNo(source.getOrderNo());
        target.setScheduleDate(scheduleDate.atStartOfDay());

        // 机台信息
        target.setMachineCode(source.getCxMachineCode());
        target.setMachineName(source.getCxMachineName());
        target.setLhMachineCode(source.getLhMachineCode());
        target.setLhMachineName(source.getLhMachineName());
        target.setAvailableMoldQty(source.getLhMachineQty());

        // 物料信息
        target.setMaterialCode(source.getSapCode());
        target.setMesMaterialCode(null); // MES物料编码需要另外查询
        target.setSpecDesc(source.getSpecDesc());
        target.setEmbryoCode(source.getEmbryoCode());
        target.setEmbryoSpecDesc(null); // 胎胚物料描述需要另外查询

        // 中间表1班 = 夜班（当天夜班已生产，清空）
        target.setClass1PlanQtySeq(BigDecimal.valueOf(1));
        target.setClass1AnalysisInput(null);
        target.setClass1Analysis(null);
        target.setClass1PlanQty(BigDecimal.ZERO);
        target.setClass1ExampleType(null);
        target.setClass1ExampleNo(null);

        // 中间表2班 = 早班（1班数据）
        target.setClass2PlanQtySeq(BigDecimal.valueOf(2));
        target.setClass2AnalysisInput(source.getClass1AnalysisInput());
        target.setClass2Analysis(source.getClass1Analysis());
        target.setClass2PlanQty(source.getClass1PlanQty());
        target.setClass2ExampleType(null); // 示方类型需要另外查询
        target.setClass2ExampleNo(null); // 示方号需要另外查询

        // 中间表3班 = 中班（2班数据）
        target.setClass3PlanQtySeq(BigDecimal.valueOf(3));
        target.setClass3AnalysisInput(source.getClass2AnalysisInput());
        target.setClass3Analysis(source.getClass2Analysis());
        target.setClass3PlanQty(source.getClass2PlanQty());
        target.setClass3ExampleType(null);
        target.setClass3ExampleNo(null);

        return target;
    }

    /**
     * 转换为第2天（隔天）的下发实体
     * 8班数据：3班(夜)、4班(早)、5班(中) -> 中间表：1班(夜)=3班, 2班(早)=4班, 3班(中)=5班
     * 业务规则：更新3班数据（夜早中班）
     */
    private CxScheduleResultIssue convertToDay2IssueEntity(CxScheduleResult source, LocalDate scheduleDate) {
        if (source == null) {
            return null;
        }

        CxScheduleResultIssue target = new CxScheduleResultIssue();

        // 基础字段映射
        target.setId(source.getId());
        target.setCxBatchNo(source.getCxBatchNo());
        target.setOrderNo(source.getOrderNo());
        target.setScheduleDate(scheduleDate.atStartOfDay());

        // 机台信息
        target.setMachineCode(source.getCxMachineCode());
        target.setMachineName(source.getCxMachineName());
        target.setLhMachineCode(source.getLhMachineCode());
        target.setLhMachineName(source.getLhMachineName());
        target.setAvailableMoldQty(source.getLhMachineQty());

        // 物料信息
        target.setMaterialCode(source.getSapCode());
        target.setMesMaterialCode(null);
        target.setSpecDesc(source.getSpecDesc());
        target.setEmbryoCode(source.getEmbryoCode());
        target.setEmbryoSpecDesc(null);

        // 中间表1班 = 夜班（3班数据）
        target.setClass1PlanQtySeq(BigDecimal.valueOf(1));
        target.setClass1AnalysisInput(source.getClass3AnalysisInput());
        target.setClass1Analysis(source.getClass3Analysis());
        target.setClass1PlanQty(source.getClass3PlanQty());
        target.setClass1ExampleType(null);
        target.setClass1ExampleNo(null);

        // 中间表2班 = 早班（4班数据）
        target.setClass2PlanQtySeq(BigDecimal.valueOf(2));
        target.setClass2AnalysisInput(source.getClass4AnalysisInput());
        target.setClass2Analysis(source.getClass4Analysis());
        target.setClass2PlanQty(source.getClass4PlanQty());
        target.setClass2ExampleType(null);
        target.setClass2ExampleNo(null);

        // 中间表3班 = 中班（5班数据）
        target.setClass3PlanQtySeq(BigDecimal.valueOf(3));
        target.setClass3AnalysisInput(source.getClass5AnalysisInput());
        target.setClass3Analysis(source.getClass5Analysis());
        target.setClass3PlanQty(source.getClass5PlanQty());
        target.setClass3ExampleType(null);
        target.setClass3ExampleNo(null);

        return target;
    }

    /**
     * 转换为第3天（后天）的下发实体
     * 8班数据：6班(夜)、7班(早)、8班(中) -> 中间表：1班(夜)=6班, 2班(早)=7班, 3班(中)=8班
     * 业务规则：下发3班数据（夜早中班）
     */
    private CxScheduleResultIssue convertToDay3IssueEntity(CxScheduleResult source, LocalDate scheduleDate) {
        if (source == null) {
            return null;
        }

        CxScheduleResultIssue target = new CxScheduleResultIssue();

        // 基础字段映射
        target.setId(source.getId());
        target.setCxBatchNo(source.getCxBatchNo());
        target.setOrderNo(source.getOrderNo());
        target.setScheduleDate(scheduleDate.atStartOfDay());

        // 机台信息
        target.setMachineCode(source.getCxMachineCode());
        target.setMachineName(source.getCxMachineName());
        target.setLhMachineCode(source.getLhMachineCode());
        target.setLhMachineName(source.getLhMachineName());
        target.setAvailableMoldQty(source.getLhMachineQty());

        // 物料信息
        target.setMaterialCode(source.getSapCode());
        target.setMesMaterialCode(null);
        target.setSpecDesc(source.getSpecDesc());
        target.setEmbryoCode(source.getEmbryoCode());
        target.setEmbryoSpecDesc(null);

        // 中间表1班 = 夜班（6班数据）
        target.setClass1PlanQtySeq(BigDecimal.valueOf(1));
        target.setClass1AnalysisInput(source.getClass6AnalysisInput());
        target.setClass1Analysis(source.getClass6Analysis());
        target.setClass1PlanQty(source.getClass6PlanQty());
        target.setClass1ExampleType(null);
        target.setClass1ExampleNo(null);

        // 中间表2班 = 早班（7班数据）
        target.setClass2PlanQtySeq(BigDecimal.valueOf(2));
        target.setClass2AnalysisInput(source.getClass7AnalysisInput());
        target.setClass2Analysis(source.getClass7Analysis());
        target.setClass2PlanQty(source.getClass7PlanQty());
        target.setClass2ExampleType(null);
        target.setClass2ExampleNo(null);

        // 中间表3班 = 中班（8班数据）
        target.setClass3PlanQtySeq(BigDecimal.valueOf(3));
        target.setClass3AnalysisInput(source.getClass8AnalysisInput());
        target.setClass3Analysis(source.getClass8Analysis());
        target.setClass3PlanQty(source.getClass8PlanQty());
        target.setClass3ExampleType(null);
        target.setClass3ExampleNo(null);

        return target;
    }
}
