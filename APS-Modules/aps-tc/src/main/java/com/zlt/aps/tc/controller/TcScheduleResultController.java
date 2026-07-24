package com.zlt.aps.tc.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultExplain;
import com.zlt.aps.tc.api.domain.entity.TcScheduleUnplanned;
import com.zlt.aps.tc.api.domain.vo.*;
import com.zlt.aps.tc.service.*;
import com.zlt.aps.tc.service.impl.TcManualScheduleApplicationService;
import com.zlt.aps.tc.service.query.TcManualOptionsService;
import com.zlt.aps.tc.service.query.TcScheduleBoardQueryService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 胎侧自动排程结果只读查询与自动排程任务接口。
 */
@Api(tags = "胎侧排程结果")
@RestController
@RequestMapping("/tcScheduleResult")
public class TcScheduleResultController extends AbstractDocBizController<TcScheduleResult> {

    @Resource
    private ITcScheduleResultService tcScheduleResultService;

    @Resource
    private TcScheduleBoardQueryService tcScheduleBoardQueryService;

    @Resource
    private TcManualOptionsService tcManualOptionsService;

    @Resource
    private TcManualScheduleApplicationService tcManualScheduleApplicationService;

    @Resource
    private TcReleaseApplicationService tcReleaseApplicationService;

    @Resource
    private TcReleaseRecoveryService tcReleaseRecoveryService;

    @Resource
    private TcAutoRollingApplicationService tcAutoRollingApplicationService;

    @Resource
    private TcOperationTaskApplicationService tcOperationTaskApplicationService;

    /**
     * 校验所选胎侧结果是否允许发布。
     *
     * @param requestVO 发布请求
     * @return 校验结果
     */
    @ApiOperation("校验胎侧排程发布")
    @PostMapping("/release/validate")
    public TcReleaseValidateVo validateRelease(@RequestBody TcReleaseRequestVo requestVO) {
        return this.tcReleaseApplicationService.validate(requestVO);
    }

    /**
     * 创建胎侧排程异步发布任务。
     *
     * @param requestVO 发布请求
     * @return 发布任务
     */
    @ApiOperation("发布胎侧排程结果")
    @PostMapping("/release")
    public TcReleaseTaskVo release(@RequestBody TcReleaseRequestVo requestVO) {
        return this.tcReleaseApplicationService.publish(requestVO);
    }

    /**
     * 查询指定发布任务。
     *
     * @param taskId 发布任务ID
     * @return 发布任务
     */
    @ApiOperation("查询胎侧发布任务")
    @GetMapping("/release/task/{taskId}")
    public TcReleaseTaskVo getReleaseTask(@PathVariable("taskId") String taskId) {
        return this.tcReleaseApplicationService.getTask(taskId);
    }

    /**
     * 查询指定工厂日期最近发布任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近发布任务
     */
    @ApiOperation("查询最近胎侧发布任务")
    @GetMapping("/release/task/latest")
    public TcReleaseTaskVo getLatestReleaseTask(
            @RequestParam("factoryCode") String factoryCode,
            @RequestParam("scheduleDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date scheduleDate) {
        return this.tcReleaseApplicationService.getLatestTask(factoryCode, scheduleDate);
    }

    /**
     * 恢复超过超时时间仍处于发布中的任务，供定时任务调用。
     *
     * @return 恢复任务数量
     */
    @ApiOperation("恢复胎侧发布超时任务")
    @PostMapping("/internal/recoverReleaseTimeout")
    public AjaxResult recoverReleaseTimeout() {
        int recoveredCount = this.tcReleaseRecoveryService.recoverTimeoutTasks();
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.release.recoverSuccess"), recoveredCount);
    }

    /**
     * 检查班次开始窗口并提交自动滚动任务，供平台定时任务调用。
     *
     * @param requestVO 窗口检查请求
     * @return 创建或复用的滚动任务
     */
    @ApiOperation("检查胎侧自动滚动窗口")
    @PostMapping("/internal/checkTimedRolling")
    public AjaxResult checkTimedRolling(@RequestBody TcRollingCheckRequestVo requestVO) {
        List<TcRollingTaskVo> taskList = this.tcAutoRollingApplicationService.checkAndSubmit(requestVO);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.rolling.checkSuccess"), taskList);
    }

    /**
     * 查询胎侧排程平铺看板。
     *
     * @param queryVO 看板查询条件
     * @return 已排分页、日期列、批次、汇总和未排数量
     */
    @ApiOperation("查询胎侧排程看板")
    @PostMapping("/board")
    public TcScheduleBoardVo board(@RequestBody TcScheduleBoardQueryVo queryVO) {
        return this.tcScheduleBoardQueryService.queryBoard(queryVO);
    }

    /**
     * 分页查询当前有效批次未排任务。
     *
     * @param queryVO 看板查询条件
     * @return 未排任务分页
     */
    @ApiOperation("查询胎侧未排任务")
    @PostMapping("/unplanned/list")
    public Page<TcScheduleUnplanned> listUnplanned(@RequestBody TcScheduleBoardQueryVo queryVO) {
        return this.tcScheduleBoardQueryService.listUnplanned(queryVO);
    }

    /**
     * 懒加载已排结果解释。
     *
     * @param resultId 排程结果 ID
     * @return 解释明细
     */
    @ApiOperation("查询胎侧已排结果解释")
    @GetMapping("/explain/result/{resultId}")
    public List<TcScheduleResultExplain> listResultExplain(@PathVariable("resultId") Long resultId) {
        return this.tcScheduleBoardQueryService.listResultExplain(resultId);
    }

    /**
     * 懒加载未排任务解释。
     *
     * @param unplannedId 未排任务 ID
     * @return 解释明细
     */
    @ApiOperation("查询胎侧未排任务解释")
    @GetMapping("/explain/unplanned/{unplannedId}")
    public List<TcScheduleResultExplain> listUnplannedExplain(@PathVariable("unplannedId") Long unplannedId) {
        return this.tcScheduleBoardQueryService.listUnplannedExplain(unplannedId);
    }

    /**
     * 查询人工插单和普通转机可用选项。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 施工、机台和六班选项
     */
    @ApiOperation("查询胎侧人工排程选项")
    @GetMapping("/manual/options")
    public TcManualOptionsVo manualOptions(@RequestParam("factoryCode") String factoryCode,
                                           @RequestParam("scheduleDate")
                                           @DateTimeFormat(pattern = "yyyy-MM-dd") Date scheduleDate) {
        return this.tcManualOptionsService.listOptions(factoryCode, scheduleDate);
    }

    /**
     * 执行胎侧人工插单。
     *
     * @param requestVO 插单请求
     * @return 新增结果行数
     */
    @ApiOperation("胎侧人工插单")
    @PostMapping("/insertTask")
    public AjaxResult insertTask(@RequestBody TcInsertTaskRequestVo requestVO) {
        int affectedCount = this.tcManualScheduleApplicationService.insertTask(requestVO);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.insert.success"), affectedCount);
    }

    /**
     * 调整胎侧选中班次计划量。
     *
     * @param requestVO 调量请求
     * @return 受影响行数
     */
    @ApiOperation("调整胎侧班次计划量")
    @PostMapping("/changeQty")
    public AjaxResult changeQty(@RequestBody TcChangeQtyRequestVo requestVO) {
        int affectedCount = this.tcManualScheduleApplicationService.changeQty(requestVO);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.changeQty.success"), affectedCount);
    }

    /**
     * 原子批量执行胎侧普通转机台。
     *
     * @param requestVO 转机台请求
     * @return 受影响行数
     */
    @ApiOperation("胎侧普通转机台")
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody TcChangeMachineRequestVo requestVO) {
        int affectedCount = this.tcManualScheduleApplicationService.changeMachine(requestVO);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.changeMachine.success"), affectedCount);
    }

    /**
     * 按结果 ID 整行删除胎侧六班排程结果。
     *
     * @param resultIdList 排程结果 ID
     * @return 删除行数
     */
    @ApiOperation("整行删除胎侧排程结果")
    @DeleteMapping("/remove")
    public AjaxResult remove(@RequestBody List<Long> resultIdList) {
        int affectedCount = this.tcManualScheduleApplicationService.remove(resultIdList);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.remove.success"), affectedCount);
    }

    /**
     * 提交胎侧人工插单异步任务。
     *
     * @param requestVO 插单请求
     * @return 初始任务
     */
    @ApiOperation("提交胎侧人工插单异步任务")
    @PostMapping("/operation/insertTask")
    public TcOperationTaskVo submitInsertTask(@RequestBody TcInsertTaskRequestVo requestVO) {
        return this.tcOperationTaskApplicationService.submitInsert(requestVO);
    }

    /**
     * 提交胎侧调量异步任务。
     *
     * @param requestVO 调量请求
     * @return 初始任务
     */
    @ApiOperation("提交胎侧调量异步任务")
    @PostMapping("/operation/changeQty")
    public TcOperationTaskVo submitChangeQty(@RequestBody TcChangeQtyRequestVo requestVO) {
        return this.tcOperationTaskApplicationService.submitChangeQty(requestVO);
    }

    /**
     * 提交胎侧单条或批量转机台异步任务。
     *
     * @param requestVO 转机台请求
     * @return 初始任务
     */
    @ApiOperation("提交胎侧转机台异步任务")
    @PostMapping("/operation/changeMachine")
    public TcOperationTaskVo submitChangeMachine(@RequestBody TcChangeMachineRequestVo requestVO) {
        return this.tcOperationTaskApplicationService.submitChangeMachine(requestVO);
    }

    /**
     * 提交胎侧删除异步任务。
     *
     * @param resultIdList 结果ID
     * @return 初始任务
     */
    @ApiOperation("提交胎侧删除异步任务")
    @DeleteMapping("/operation/remove")
    public TcOperationTaskVo submitRemove(@RequestBody List<Long> resultIdList) {
        return this.tcOperationTaskApplicationService.submitDelete(resultIdList);
    }

    /**
     * 查询胎侧人工操作任务。
     *
     * @param taskId 任务编号
     * @return 任务状态
     */
    @ApiOperation("查询胎侧人工操作任务")
    @GetMapping("/operation/task/{taskId}")
    public TcOperationTaskVo getOperationTask(@PathVariable("taskId") String taskId) {
        return this.tcOperationTaskApplicationService.getTask(taskId);
    }

    /**
     * 查询最近胎侧人工操作任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务
     */
    @ApiOperation("查询最近胎侧人工操作任务")
    @GetMapping("/operation/task/latest")
    public TcOperationTaskVo getLatestOperationTask(
            @RequestParam("factoryCode") String factoryCode,
            @RequestParam("scheduleDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date scheduleDate) {
        return this.tcOperationTaskApplicationService.getLatestTask(factoryCode, scheduleDate);
    }

    /**
     * 查询胎侧排程结果列表。
     *
     * @param queryVO 查询条件
     * @return 分页结果
     */
    @ApiOperation("查询胎侧排程结果列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TcScheduleResult queryVO) {
        return super.list(queryVO);
    }

    /**
     * 查询胎侧排程结果详情。
     *
     * @param id 排程结果主键
     * @return 排程结果详情
     */
    @ApiOperation("查询胎侧排程结果详情")
    @GetMapping("/{id}")
    @Override
    public TcScheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 校验自动排程请求及旧结果覆盖条件。
     *
     * @param request 自动排程请求
     * @return 校验响应
     */
    @ApiOperation("校验胎侧自动排程请求")
    @PostMapping("/validateAutoPlan")
    public TcAutoScheduleResponseVo validateAutoPlan(@RequestBody TcAutoScheduleRequestVo request) {
        return tcScheduleResultService.validateAutoPlan(request);
    }

    /**
     * 提交胎侧自动排程异步任务。
     *
     * @param request 自动排程请求
     * @return 待执行任务响应
     */
    @ApiOperation("提交胎侧自动排程任务")
    @PostMapping("/autoPlan")
    public TcAutoScheduleResponseVo autoPlan(@RequestBody TcAutoScheduleRequestVo request) {
        return tcScheduleResultService.autoPlan(request);
    }

    /**
     * 查询指定胎侧自动排程任务。
     *
     * @param taskId 对外任务编号
     * @return 任务进度和结果摘要
     */
    @ApiOperation("查询胎侧自动排程任务")
    @GetMapping("/autoPlan/task/{taskId}")
    public TcAutoScheduleResponseVo getAutoPlanTask(@PathVariable("taskId") String taskId) {
        return tcScheduleResultService.getAutoPlanTask(taskId);
    }

    /**
     * 查询指定工厂和排程日期最近一次任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务进度和结果摘要
     */
    @ApiOperation("查询最近一次胎侧自动排程任务")
    @GetMapping("/autoPlan/task/latest")
    public TcAutoScheduleResponseVo getLatestAutoPlanTask(
            @RequestParam("factoryCode") String factoryCode,
            @RequestParam("scheduleDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date scheduleDate) {
        return tcScheduleResultService.getLatestAutoPlanTask(factoryCode, scheduleDate);
    }

    /**
     * 清理胎侧自动排程 Redis 基础资料缓存。
     *
     * @param factoryCode 工厂编码，为空时清理全部胎侧自动排程缓存
     * @param scheduleDate 排程日期
     * @return 清理数量
     */
    @ApiOperation("清理胎侧自动排程Redis缓存")
    @PostMapping("/clearAutoPlanRedisCache")
    public AjaxResult clearAutoPlanRedisCache(
            @RequestParam(value = "factoryCode", required = false) String factoryCode,
            @RequestParam(value = "scheduleDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date scheduleDate) {
        long deleteCount = tcScheduleResultService.clearAutoPlanRedisCache(factoryCode, scheduleDate);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.cacheCleared"), deleteCount);
    }

    /**
     * 获取胎侧排程结果单据服务。
     *
     * @return 胎侧排程结果服务
     */
    @Override
    protected IDocService getDocService() {
        return tcScheduleResultService;
    }

    /**
     * 构建胎侧排程结果列表查询条件。
     *
     * @param queryWrapper MyBatis 查询包装器
     * @param queryVO 查询条件
     */
    @Override
    protected void builderCondition(QueryWrapper<TcScheduleResult> queryWrapper, TcScheduleResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getBatchNo()), "BATCH_NO", queryVO.getBatchNo());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getScheduleDate()), "SCHEDULE_DATE", queryVO.getScheduleDate());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSidewallCode()), "SIDEWALL_CODE", queryVO.getSidewallCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getOrderNo()), "ORDER_NO", queryVO.getOrderNo());
    }

    /**
     * 获取胎侧排程结果类型编码。
     *
     * @return 类型编码
     */
    @Override
    protected String getTypeCode() {
        return "TC0815";
    }

    /**
     * 获取默认排序。
     *
     * @return 默认排序表达式
     */
    @Override
    protected String getOrderBy() {
        return "schedule_date desc,machine_code asc,create_time desc";
    }
}
