package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15RollingCheckRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.service.Cd15ScheduleResultPublishService;
import com.zlt.aps.cd15.service.Cd15ScheduleTaskRecoveryService;
import com.zlt.aps.cd15.service.ICd15ScheduleResultService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 斜裁排程结果控制层。
 */
@Api(tags = "斜裁排程结果")
@RestController
@RequestMapping("/cd15ScheduleResult")
public class Cd15ScheduleResultController extends AbstractDocBizController<Cd15ScheduleResult> {

    @Resource
    private ICd15ScheduleResultService cd15ScheduleResultService;

    @Resource
    private Cd15ScheduleResultMapper cd15ScheduleResultMapper;

    @Resource
    private Cd15ScheduleResultPublishService scheduleResultPublishService;

    @Resource
    private Cd15ScheduleTaskRecoveryService scheduleTaskRecoveryService;

    @ApiOperation("查询斜裁排程结果列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15ScheduleResult queryVO) {
        return super.list(queryVO);
    }

    @ApiOperation("获取斜裁排程结果详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15ScheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除斜裁排程结果")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        AjaxResult validation = this.validateDelete(ids);
        if (validation != null) {
            return validation;
        }
        return super.removeByIds(ids);
    }

    @ApiOperation("斜裁自动排程")
    @PostMapping("/autoSchedule")
    public AjaxResult autoSchedule(@RequestBody Cd15ScheduleResult scheduleResult) {
        return cd15ScheduleResultService.autoSchedule(scheduleResult);
    }

    @ApiOperation("查询斜裁自动排程任务")
    @GetMapping("/autoSchedule/task/{taskId}")
    public AjaxResult getAutoScheduleTask(@PathVariable("taskId") String taskId) {
        return cd15ScheduleResultService.getAutoScheduleTask(taskId);
    }

    @ApiOperation("查询斜裁班次日期")
    @PostMapping("/shiftDates")
    public AjaxResult shiftDates(@RequestBody Cd15InsertOrderRequest request) {
        return cd15ScheduleResultService.shiftDates(request);
    }

    @ApiOperation("斜裁插单预校验")
    @PostMapping("/validateInsert")
    public AjaxResult validateInsert(@RequestBody Cd15InsertOrderRequest request) {
        return cd15ScheduleResultService.validateInsert(request);
    }

    @ApiOperation("提交斜裁插单")
    @PostMapping("/insert")
    public AjaxResult insert(@RequestBody Cd15InsertOrderRequest request) {
        return cd15ScheduleResultService.insert(request);
    }

    @ApiOperation("查询斜裁插单任务")
    @GetMapping("/insert/task/{taskId}")
    public AjaxResult getInsertTask(@PathVariable("taskId") String taskId) {
        return cd15ScheduleResultService.getInsertTask(taskId);
    }

    @ApiOperation("斜裁转机台预校验")
    @PostMapping("/validateTransferMachine")
    public AjaxResult validateTransferMachine(@RequestBody Cd15TransferMachineRequest request) {
        return cd15ScheduleResultService.validateTransferMachine(request);
    }

    @ApiOperation("提交斜裁转机台")
    @PostMapping("/transferMachine")
    public AjaxResult transferMachine(@RequestBody Cd15TransferMachineRequest request) {
        return cd15ScheduleResultService.transferMachine(request);
    }

    @ApiOperation("查询斜裁转机台任务")
    @GetMapping("/transferMachine/task/{taskId}")
    public AjaxResult getTransferMachineTask(@PathVariable("taskId") String taskId) {
        return cd15ScheduleResultService.getTransferMachineTask(taskId);
    }

    @ApiOperation("斜裁调量预校验")
    @PostMapping("/validateChangeQty")
    public AjaxResult validateChangeQty(@RequestBody Cd15ChangeQtyRequest request) {
        return cd15ScheduleResultService.validateChangeQty(request);
    }

    @ApiOperation("提交斜裁调量")
    @PostMapping("/changeQty")
    public AjaxResult changeQty(@RequestBody Cd15ChangeQtyRequest request) {
        return cd15ScheduleResultService.changeQty(request);
    }

    @ApiOperation("查询斜裁调量任务")
    @GetMapping("/changeQty/task/{taskId}")
    public AjaxResult getChangeQtyTask(@PathVariable("taskId") String taskId) {
        return cd15ScheduleResultService.getChangeQtyTask(taskId);
    }

    @ApiOperation("CD15定时滚动排程检查")
    @PostMapping("/rollingSchedule/check")
    public AjaxResult checkTimedRolling(@RequestBody Cd15RollingCheckRequest request) {
        return cd15ScheduleResultService.checkTimedRolling(request);
    }

    @ApiOperation("查询CD15定时滚动排程任务")
    @GetMapping("/rollingSchedule/task/{taskId}")
    public AjaxResult getTimedRollingTask(@PathVariable("taskId") String taskId) {
        return cd15ScheduleResultService.getTimedRollingTask(taskId);
    }

    @ApiOperation("补偿斜裁自动排程超时任务")
    @PostMapping("/autoSchedule/recoverTimeoutTasks")
    public AjaxResult recoverAutoScheduleTimeoutTasks(
            @RequestParam(value = "timeoutMinutes", required = false)
            Integer timeoutMinutes) {
        if (timeoutMinutes != null && timeoutMinutes <= 0) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.taskRecovery.invalidTimeout"));
        }
        return AjaxResult.success(scheduleTaskRecoveryService.recover(timeoutMinutes));
    }

    @Log(title = "ui.data.column.cd15ScheduleResult.modalName",
            businessType = BusinessType.PUBLISH)
    @ApiOperation("发布斜裁排程结果")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody Cd15ScheduleResult dto,
                              @RequestParam(value = "ids", required = false) String ids) {
        return scheduleResultPublishService.publish(dto, ids);
    }

    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入斜裁排程结果")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.cd15ScheduleResult.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出斜裁排程结果")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15ScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd15ScheduleResult> listExportData(Cd15ScheduleResult obj) {
        QueryWrapper<Cd15ScheduleResult> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd15ScheduleResult> list = cd15ScheduleResultMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd15ScheduleResultService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15ScheduleResult> queryWrapper, Cd15ScheduleResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getScheduleDate()), "SCHEDULE_DATE", queryVO.getScheduleDate());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSteelStripCode()), "STEEL_STRIP_CODE", queryVO.getSteelStripCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getGroupNo()), "GROUP_NO", queryVO.getGroupNo());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBigRollCode()), "BIG_ROLL_CODE", queryVO.getBigRollCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getReleaseStatus()), "RELEASE_STATUS", queryVO.getReleaseStatus());
    }

    /**
     * 已成功发布的结果不得删除；分裁组合必须两条一起删除。
     *
     * @param ids 待删除结果主键
     * @return 校验失败结果；通过时返回 null
     */
    private AjaxResult validateDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        List<Cd15ScheduleResult> selected = cd15ScheduleResultMapper.selectList(
                new LambdaQueryWrapper<Cd15ScheduleResult>()
                        .in(Cd15ScheduleResult::getId, ids));
        boolean published = selected.stream().anyMatch(result ->
                result.getPublishSuccessCount() != null
                        && result.getPublishSuccessCount() > 0);
        if (published) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.scheduleResult.publishedCannotDelete"));
        }
        boolean missingGroupNo = selected.stream()
                .filter(result -> "SPLIT".equalsIgnoreCase(result.getCutMode()))
                .anyMatch(result -> result.getGroupNo() == null
                        || result.getGroupNo().trim().isEmpty());
        if (missingGroupNo) {
            return AjaxResult.error(I18nUtil.getMessage(
                    "ui.cd15.scheduleResult.splitDeleteTogether"));
        }
        Map<String, List<Cd15ScheduleResult>> splitGroups = selected.stream()
                .filter(result -> "SPLIT".equalsIgnoreCase(result.getCutMode()))
                .collect(Collectors.groupingBy(this::splitGroupKey,
                        LinkedHashMap::new, Collectors.toList()));
        for (List<Cd15ScheduleResult> selectedGroup : splitGroups.values()) {
            Cd15ScheduleResult sample = selectedGroup.get(0);
            List<Cd15ScheduleResult> completeGroup =
                    cd15ScheduleResultMapper.selectList(
                            new LambdaQueryWrapper<Cd15ScheduleResult>()
                                    .eq(Cd15ScheduleResult::getFactoryCode,
                                            sample.getFactoryCode())
                                    .eq(Cd15ScheduleResult::getScheduleDate,
                                            sample.getScheduleDate())
                                    .eq(Cd15ScheduleResult::getGroupNo,
                                            sample.getGroupNo())
                                    .eq(Cd15ScheduleResult::getCutMode, "SPLIT"));
            Set<Long> selectedIds = selectedGroup.stream()
                    .map(Cd15ScheduleResult::getId)
                    .collect(Collectors.toSet());
            boolean selectedAll = completeGroup.size() == selectedGroup.size()
                    && completeGroup.stream()
                    .map(Cd15ScheduleResult::getId)
                    .allMatch(selectedIds::contains)
                    && (completeGroup.size() == 1
                    || completeGroup.size() == 2
                    && completeGroup.stream()
                    .map(Cd15ScheduleResult::getSteelStripCode)
                    .filter(code -> code != null && !code.trim().isEmpty())
                    .distinct().count() == 2L);
            if (!selectedAll) {
                return AjaxResult.error(I18nUtil.getMessage(
                        "ui.cd15.scheduleResult.splitDeleteTogether"));
            }
        }
        return null;
    }

    /** 构造分裁组合删除校验键。 */
    private String splitGroupKey(Cd15ScheduleResult result) {
        return String.valueOf(result.getFactoryCode()) + "|"
                + String.valueOf(result.getScheduleDate()) + "|"
                + String.valueOf(result.getGroupNo());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_SCHEDULE_RESULT";
    }

    @Override
    protected String getOrderBy() {
        return " MACHINE_CODE ASC,BIG_ROLL_CODE ASC,CLASS1_PRODUCE_ORDER IS NULL ASC,CLASS1_PRODUCE_ORDER ASC";
    }
}
