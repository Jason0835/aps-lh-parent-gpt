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
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultMapper;
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

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd90ScheduleResult queryVO) {
        return super.list(queryVO);
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
        return " MACHINE_CODE asc,CLOTH_CODE asc,SCHEDULE_DATE desc";
    }
}
