package com.zlt.aps.cd90.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleRollingAdjustLog;
import com.zlt.aps.cd90.api.domain.vo.Cd90InsertOrderRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90RollingCheckRequest;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleRollingAdjustLogMapper;
import com.zlt.aps.cd90.service.Cd90ScheduleResultPublishService;
import com.zlt.aps.cd90.service.ICd90ScheduleResultService;
import com.zlt.aps.cd90.service.Cd90ScheduleTaskRecoveryService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Api(tags = "直裁排程结果")
@RestController
@RequestMapping("/cd90ScheduleResult")
public class Cd90ScheduleResultController extends AbstractDocBizController<Cd90ScheduleResult> {

    @Resource
    private ICd90ScheduleResultService cd90ScheduleResultService;
    @Resource
    private Cd90ScheduleResultMapper cd90ScheduleResultMapper;
    @Resource
    private Cd90ScheduleTaskService cd90ScheduleTaskService;
    @Resource
    private Cd90ScheduleTaskRecoveryService cd90ScheduleTaskRecoveryService;
    @Resource
    private Cd90ScheduleResultPublishService cd90ScheduleResultPublishService;
    @Resource
    private Cd90ScheduleRollingAdjustLogMapper cd90ScheduleRollingAdjustLogMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd90ScheduleResult queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.scheduleResult.modelName", businessType = BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody Cd90ScheduleResult dto,
                              @RequestParam(value = "ids", required = false) String ids) {
        return cd90ScheduleResultPublishService.publish(dto, ids);
    }

    @Log(title = "ui.data.column.scheduleResult.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            LambdaQueryWrapper<Cd90ScheduleResult> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Cd90ScheduleResult::getId, ids);
            wrapper.gt(Cd90ScheduleResult::getPublishSuccessCount, 0);
            if (cd90ScheduleResultMapper.selectCount(wrapper) > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90ScheduleResult.hasPublishedCanNotDelete"));
            }
        }
        return super.removeByIds(ids);
    }

    /**
     * 自动生成直裁排程结果。
     *
     * @param scheduleResult 自动排程条件，当前使用工厂编码和排程日期
     * @return 自动排程结果
     */
    @ApiOperation("自动排程")
    @PostMapping("/autoSchedule")
    public AjaxResult autoSchedule(@RequestBody Cd90ScheduleResult scheduleResult) {
        return cd90ScheduleResultService.autoSchedule(scheduleResult);
    }

    /** 查询插单弹窗班次日期。 */
    @ApiOperation("查询插单班次日期")
    @PostMapping("/shiftDates")
    public AjaxResult shiftDates(@RequestBody Cd90InsertOrderRequest request) {
        return cd90ScheduleResultService.shiftDates(request);
    }

    /** 插单预校验。 */
    @ApiOperation("插单预校验")
    @PostMapping("/validateInsert")
    public AjaxResult validateInsert(@RequestBody Cd90InsertOrderRequest request) {
        return cd90ScheduleResultService.validateInsert(request);
    }

    /** 提交插单滚动重排任务。 */
    @ApiOperation("提交插单滚动重排")
    @PostMapping("/insert")
    public AjaxResult insertOrder(@RequestBody Cd90InsertOrderRequest request) {
        return cd90ScheduleResultService.insertOrder(request);
    }

    /** 查询插单滚动重排任务。 */
    @ApiOperation("查询插单滚动重排任务")
    @GetMapping("/insert/task/{taskId}")
    public AjaxResult getInsertTask(@PathVariable("taskId") String taskId) {
        return cd90ScheduleResultService.getInsertTask(taskId);
    }

    /** 供aps-job每5分钟检查交班滚动窗口。 */
    @ApiOperation("检查定时滚动排程窗口")
    @PostMapping("/rollingSchedule/check")
    public AjaxResult checkTimedRolling(@RequestBody Cd90RollingCheckRequest request) {
        return cd90ScheduleResultService.checkTimedRolling(request);
    }

    /** 查询定时滚动排程任务。 */
    @ApiOperation("查询定时滚动排程任务")
    @GetMapping("/rollingSchedule/task/{taskId}")
    public AjaxResult getTimedRollingTask(@PathVariable("taskId") String taskId) {
        return cd90ScheduleResultService.getTimedRollingTask(taskId);
    }

    /** 查询定时滚动调整日志列表，列表不加载大JSON快照。 */
    @ApiOperation("查询定时滚动调整日志列表")
    @PostMapping("/rollingSchedule/adjustLog/list")
    public TableDataInfo listRollingAdjustLogs(
            @RequestBody Cd90ScheduleRollingAdjustLog queryVO) {
        this.startPage();
        LambdaQueryWrapper<Cd90ScheduleRollingAdjustLog> wrapper =
                new LambdaQueryWrapper<>();
        this.selectRollingAdjustLogListFields(wrapper);
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()),
                        Cd90ScheduleRollingAdjustLog::getFactoryCode,
                        queryVO.getFactoryCode())
                .eq(queryVO.getScheduleDate() != null,
                        Cd90ScheduleRollingAdjustLog::getScheduleDate,
                        queryVO.getScheduleDate())
                .eq(PubUtil.isNotEmpty(queryVO.getBatchNo()),
                        Cd90ScheduleRollingAdjustLog::getBatchNo,
                        queryVO.getBatchNo())
                .eq(PubUtil.isNotEmpty(queryVO.getTaskId()),
                        Cd90ScheduleRollingAdjustLog::getTaskId,
                        queryVO.getTaskId())
                .like(PubUtil.isNotEmpty(queryVO.getClothCode()),
                        Cd90ScheduleRollingAdjustLog::getClothCode,
                        queryVO.getClothCode())
                .eq(PubUtil.isNotEmpty(queryVO.getAdjustType()),
                        Cd90ScheduleRollingAdjustLog::getAdjustType,
                        queryVO.getAdjustType())
                .orderByDesc(Cd90ScheduleRollingAdjustLog::getCreateTime)
                .orderByDesc(Cd90ScheduleRollingAdjustLog::getId);
        return this.getDataTable(
                cd90ScheduleRollingAdjustLogMapper.selectList(wrapper));
    }

    /** 查询定时滚动调整日志详情，详情包含调整前后快照。 */
    @ApiOperation("查询定时滚动调整日志详情")
    @GetMapping("/rollingSchedule/adjustLog/{id}")
    public Cd90ScheduleRollingAdjustLog getRollingAdjustLog(
            @PathVariable("id") Long id) {
        return cd90ScheduleRollingAdjustLogMapper.selectById(id);
    }

    /** 指定列表字段，避免默认传输前后快照大字段。 */
    private void selectRollingAdjustLogListFields(
            LambdaQueryWrapper<Cd90ScheduleRollingAdjustLog> wrapper) {
        wrapper.select(
                Cd90ScheduleRollingAdjustLog::getId,
                Cd90ScheduleRollingAdjustLog::getFactoryCode,
                Cd90ScheduleRollingAdjustLog::getTaskId,
                Cd90ScheduleRollingAdjustLog::getBatchNo,
                Cd90ScheduleRollingAdjustLog::getScheduleDate,
                Cd90ScheduleRollingAdjustLog::getTargetShiftCode,
                Cd90ScheduleRollingAdjustLog::getRollingItemKey,
                Cd90ScheduleRollingAdjustLog::getScheduleResultId,
                Cd90ScheduleRollingAdjustLog::getClothCode,
                Cd90ScheduleRollingAdjustLog::getBigRollCode,
                Cd90ScheduleRollingAdjustLog::getAdjustType,
                Cd90ScheduleRollingAdjustLog::getOldClassIndex,
                Cd90ScheduleRollingAdjustLog::getNewClassIndex,
                Cd90ScheduleRollingAdjustLog::getOldProduceOrder,
                Cd90ScheduleRollingAdjustLog::getNewProduceOrder,
                Cd90ScheduleRollingAdjustLog::getOldPlanQty,
                Cd90ScheduleRollingAdjustLog::getNewPlanQty,
                Cd90ScheduleRollingAdjustLog::getOldMachineCode,
                Cd90ScheduleRollingAdjustLog::getNewMachineCode,
                Cd90ScheduleRollingAdjustLog::getReasonCode,
                Cd90ScheduleRollingAdjustLog::getReasonDetail,
                Cd90ScheduleRollingAdjustLog::getInputVersion,
                Cd90ScheduleRollingAdjustLog::getSnapshotSchemaVersion,
                Cd90ScheduleRollingAdjustLog::getCreateBy,
                Cd90ScheduleRollingAdjustLog::getCreateTime,
                Cd90ScheduleRollingAdjustLog::getUpdateBy,
                Cd90ScheduleRollingAdjustLog::getUpdateTime,
                Cd90ScheduleRollingAdjustLog::getRemark);
    }

    /**
     * 查询自动排程任务状态。
     *
     * @param taskId 对外任务ID
     * @return 任务状态
     */
    @ApiOperation("查询自动排程任务状态")
    @GetMapping("/autoSchedule/task/{taskId}")
    public AjaxResult getAutoScheduleTask(@PathVariable("taskId") String taskId) {
        Cd90ScheduleTask task = cd90ScheduleTaskService.findByTaskId(taskId);
        return task == null ? AjaxResult.error("未找到自动排程任务") : AjaxResult.success(task);
    }

    /**
     * 查询指定工厂和排程日期的最近自动排程任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务
     */
    @ApiOperation("查询最近自动排程任务")
    @GetMapping("/autoSchedule/task/latest")
    public AjaxResult getLatestAutoScheduleTask(@RequestParam("factoryCode") String factoryCode,
                                                @RequestParam("scheduleDate")
                                                @DateTimeFormat(pattern = "yyyy-MM-dd") Date scheduleDate) {
        Cd90ScheduleTask task = cd90ScheduleTaskService.findLatest(factoryCode, scheduleDate);
        return AjaxResult.success(task);
    }

    /**
     * 供外部Job服务补偿心跳超时且执行锁已不存在的自动排程任务。
     *
     * @param timeoutMinutes 可选覆盖超时分钟数
     * @return 补偿汇总
     */
    @ApiOperation("补偿自动排程超时任务")
    @PostMapping("/autoSchedule/recoverTimeoutTasks")
    public AjaxResult recoverAutoScheduleTimeoutTasks(
            @RequestParam(value = "timeoutMinutes", required = false) Integer timeoutMinutes) {
        return AjaxResult.success(cd90ScheduleTaskRecoveryService.recover(timeoutMinutes));
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd90ScheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "ui.data.column.scheduleResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.scheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd90ScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd90ScheduleResult> listExportData(Cd90ScheduleResult obj) {
        QueryWrapper<Cd90ScheduleResult> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, obj);
        List<Cd90ScheduleResult> list = cd90ScheduleResultMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd90ScheduleResultService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd90ScheduleResult> qw, Cd90ScheduleResult vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.eq(vo.getScheduleDate() != null, "SCHEDULE_DATE", vo.getScheduleDate());
        qw.eq(PubUtil.isNotEmpty(vo.getClothCode()), "CLOTH_CODE", vo.getClothCode());
        qw.eq(PubUtil.isNotEmpty(vo.getMachineCode()), "MACHINE_CODE", vo.getMachineCode());
        qw.eq(PubUtil.isNotEmpty(vo.getIsRelease()), "IS_RELEASE", vo.getIsRelease());
    }

    @Override
    protected String getTypeCode() {
        return "CD90_SCHEDULE_RESULT";
    }

    @Override
    protected String getOrderBy() {
        return " MACHINE_CODE ASC,BIG_ROLL_CODE ASC,CLASS1_PRODUCE_ORDER IS NULL ASC,CLASS1_PRODUCE_ORDER ASC";
    }
}
