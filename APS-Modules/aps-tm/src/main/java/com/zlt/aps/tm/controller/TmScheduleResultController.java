package com.zlt.aps.tm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.redissonLock.annotation.DistributedLock;
import com.zlt.aps.tm.api.domain.dto.TmRollingRecalcRequestDTO;
import com.zlt.aps.tm.api.domain.dto.TmScheduleResultImportDTO;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleRequestVo;
import com.zlt.aps.tm.api.domain.vo.TmScheduleShiftDateVO;
import com.zlt.aps.tm.domain.TmAutoScheduleTask;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.service.ITmScheduleResultExcelService;
import com.zlt.aps.tm.service.ITmScheduleResultService;
import com.zlt.aps.tm.service.TmAutoScheduleTaskService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 胎面排程结果表 控制层
 */
@Slf4j
@Api(tags = "胎面排程结果表")
@RestController
@RequestMapping("/tmScheduleResult")
public class TmScheduleResultController extends AbstractDocBizController<TmScheduleResult> {

    @Autowired
    private ITmScheduleResultService tmScheduleResultService;

    @Resource
    private TmScheduleResultMapper tmScheduleResultMapper;

    @Resource
    private TmAutoScheduleTaskService tmAutoScheduleTaskService;

    @Resource
    private ITmScheduleResultExcelService tmScheduleResultExcelService;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TmScheduleResult queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TmScheduleResult billVO) {
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return super.save(billVO);
    }

    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return toAjax(tmScheduleResultService.removeScheduleResults(ids));
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public TmScheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TmScheduleResult query) {
        return tmScheduleResultService.checkUnique(query);
    }

    /**
     * 校验自动排程请求。
     *
     * @param request 自动排程请求
     * @return 校验结果，包含批次号和追踪号
     */
    @ApiOperation("校验自动排程")
    @PostMapping("/validateAutoPlan")
    public AjaxResult validateAutoPlan(@RequestBody TmAutoScheduleRequestVo request) {
        return AjaxResult.success(tmScheduleResultService.validateTmAutoPlan(request));
    }

    /**
     * 执行自动排程结构闭环。
     *
     * @param request 自动排程请求
     * @return 自动排程结构化响应
     */
    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("自动排程")
    @PostMapping("/autoPlan")
    @DistributedLock(key = "'TM_SCHEDULE:' + #request.factoryCode + ':' + T(cn.hutool.core.date.DateUtil).formatDate(#request.scheduleDate)",
            waitTime = 0, leaseTime = -1, failMsg = "ui.data.alert.tm.schedule.running")
    public AjaxResult autoPlan(@RequestBody TmAutoScheduleRequestVo request) {
        return AjaxResult.success(tmScheduleResultService.tmAutoPlan(request));
    }

    /**
     * 查询胎面自动排程任务状态。
     *
     * @param taskId 自动排程任务 ID
     * @return 自动排程任务状态和异常明细
     */
    @ApiOperation("查询胎面自动排程任务状态")
    @GetMapping("/autoPlan/task/{taskId}")
    public AjaxResult getAutoPlanTask(@PathVariable("taskId") String taskId) {
        TmAutoScheduleTask task = tmAutoScheduleTaskService.findByTaskId(taskId);
        return task == null ? AjaxResult.error(I18nUtil.getMessage("ui.data.alert.tm.schedule.taskNotFound"))
                : AjaxResult.success(tmAutoScheduleTaskService.toResponse(task));
    }

    /**
     * 查询指定工厂和排程日期最近的胎面自动排程任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近自动排程任务状态
     */
    @ApiOperation("查询最近胎面自动排程任务")
    @GetMapping("/autoPlan/task/latest")
    public AjaxResult getLatestAutoPlanTask(@RequestParam("factoryCode") String factoryCode,
                                           @RequestParam("scheduleDate")
                                           @DateTimeFormat(pattern = "yyyy-MM-dd") Date scheduleDate) {
        TmAutoScheduleTask task = tmAutoScheduleTaskService.findLatest(factoryCode, scheduleDate);
        return AjaxResult.success(task == null ? null : tmAutoScheduleTaskService.toResponse(task));
    }

    /**
     * 清除胎面自动排程 Redis 缓存。
     *
     * @param request 自动排程请求，可选传入工厂和排程日期
     * @return 清理结果，返回实际删除的 Redis key 数量
     */
    @ApiOperation("清除胎面自动排程Redis缓存")
    @PostMapping("/clearAutoPlanRedisCache")
    public AjaxResult clearAutoPlanRedisCache(@RequestBody(required = false) TmAutoScheduleRequestVo request) {
        String factoryCode = request == null ? null : request.getFactoryCode();
        java.util.Date scheduleDate = request == null ? null : request.getScheduleDate();
        return AjaxResult.success(tmScheduleResultService.clearAutoPlanRedisCache(factoryCode, scheduleDate));
    }

    /**
     * 查询排程看板数据。
     *
     * @param query 查询条件
     * @return 看板数据列表
     */
    @ApiOperation("查询排程看板")
    @PostMapping("/board")
    public AjaxResult board(@RequestBody TmScheduleResult query) {
        return AjaxResult.success(tmScheduleResultService.listBoard(query));
    }

    /**
     * 人工插单。
     *
     * @param scheduleResult 插单排程结果
     * @return 插入结果
     */
    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("人工插单")
    @PostMapping("/insertTask")
    public AjaxResult insertTask(@RequestBody TmScheduleResult scheduleResult) {
        return toAjax(tmScheduleResultService.insertTask(scheduleResult));
    }

    /**
     * 转机台
     */
    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.UPDATE)
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody TmScheduleResult scheduleResult) {
        return toAjax(tmScheduleResultService.changeMachine(scheduleResult));
    }

    /**
     * 在单个事务中批量转机台。
     *
     * @param machineCode 目标机台编码
     * @param scheduleResultList 待转机的排程结果
     * @return 批量转机结果
     */
    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("批量转机台")
    @PostMapping("/batchChangeMachine/{machineCode}")
    public AjaxResult batchChangeMachine(@PathVariable("machineCode") String machineCode,
                                         @RequestBody List<TmScheduleResult> scheduleResultList) {
        return toAjax(tmScheduleResultService.batchChangeMachine(machineCode, scheduleResultList));
    }

    /**
     * 单步撤销最近一次人工操作。
     *
     * @param dispatcherLogId 调度日志 ID
     * @return 撤销结果
     */
    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("撤销人工操作")
    @PostMapping("/undoLastOperation")
    public AjaxResult undoLastOperation(@RequestParam("dispatcherLogId") Long dispatcherLogId) {
        int restored = tmScheduleResultService.undoLastOperation(dispatcherLogId);
        return AjaxResult.success(I18nUtil.getMessage("ui.data.alert.tm.schedule.undoSuccess"), restored);
    }

    /**
     * 调整计划量。
     *
     * @param scheduleResult 调量后的排程结果
     * @return 调量结果
     */
    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("调整计划量")
    @PostMapping("/changeQty")
    public AjaxResult changeQty(@RequestBody TmScheduleResult scheduleResult) {
        return toAjax(tmScheduleResultService.changeQty(scheduleResult));
    }

    /**
     * 手动触发胎面自动滚动重算。
     *
     * <p>操作人只从微服务安全上下文读取，覆盖请求体中的同名字段，防止外部伪造审计用户。</p>
     *
     * @param request 滚动重算请求
     * @return 滚动重算统计
     */
    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("胎面自动滚动重算")
    @PostMapping("/rollingRecalc")
    public AjaxResult rollingRecalc(@RequestBody TmRollingRecalcRequestDTO request) {
        request.setOperator(SecurityUtils.getUsername());
        return AjaxResult.success(tmScheduleResultService.rollingRecalc(request));
    }

    /**
     * 校验胎面发布。
     *
     * @param ids 排程结果 ID 列表
     * @return 校验结果
     */
    @ApiOperation("校验胎面发布")
    @PostMapping("/publishValidate")
    public AjaxResult publishValidate(@RequestBody List<Long> ids) {
        return AjaxResult.success(tmScheduleResultService.publishValidate(ids));
    }

    /**
     * 发布胎面排程。
     *
     * @param ids 排程结果 ID 列表
     * @return 发布结果
     */
    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("发布胎面排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody List<Long> ids) {
        return toAjax(tmScheduleResultService.publish(ids));
    }

    /**
     * 获取胎面排程班次日期列表
     * 根据排程日期构建6个班次的日期展示列表
     *
     * @param scheduleResult 排程日期
     * @return 班次日期列表
     */
    @ApiOperation("获取胎面排程班次日期列表")
    @PostMapping("/listScheduleShiftDates")
    public List<TmScheduleShiftDateVO> listScheduleShiftDates(@RequestBody TmScheduleResult scheduleResult) {
        return tmScheduleResultService.listScheduleShiftDates(scheduleResult.getScheduleDate());
    }

    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("更改发布状态")
    @PostMapping("/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestParam("ids") String ids, @RequestParam("isRelease") String isRelease) {
        return toAjax(tmScheduleResultService.changeReleaseStatus(ids, isRelease));
    }

    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 按专用模板导入胎面排程结果。
     *
     * @param importDTO 导入文件和工厂、模板日期上下文
     * @param updateSupport 已存在记录是否更新
     * @return 导入结果和错误明细
     * @throws Exception 文件解析或日志处理失败时抛出
     */
    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("按专用模板导入胎面排程结果")
    @PostMapping("/importDataScheduleResult")
    public AjaxResult importDataScheduleResult(@RequestBody TmScheduleResultImportDTO importDTO,
                                               @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return tmScheduleResultExcelService.importDataScheduleResult(importDTO, updateSupport);
    }

    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TmScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    /**
     * 按专用模板导出胎面排程结果。
     *
     * @param queryVO 查询条件，必须包含工厂和排程日期
     * @param fileName 导出文件名称
     * @return Excel 文件字节
     */
    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("按专用模板导出胎面排程结果")
    @PostMapping("/exportDataScheduleResult/{fileName}")
    public byte[] exportDataScheduleResult(@RequestBody TmScheduleResult queryVO,
                                           @PathVariable("fileName") String fileName) {
        return tmScheduleResultExcelService.exportDataScheduleResult(queryVO, fileName);
    }

    @Override
    protected List<TmScheduleResult> listExportData(TmScheduleResult obj) {
        QueryWrapper<TmScheduleResult> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return tmScheduleResultMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return tmScheduleResultService;
    }

    @Override
    protected void builderCondition(QueryWrapper<TmScheduleResult> queryWrapper, TmScheduleResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("batchNo")), "BATCH_NO", queryVO.getFieldValueByFieldName("batchNo"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderNo")), "ORDER_NO", queryVO.getFieldValueByFieldName("orderNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineCode")), "MACHINE_CODE", queryVO.getFieldValueByFieldName("machineCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("treadCode")), "TREAD_CODE", queryVO.getFieldValueByFieldName("treadCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("glueCode")), "GLUE_CODE", queryVO.getFieldValueByFieldName("glueCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("releaseStatus")), "RELEASE_STATUS", queryVO.getFieldValueByFieldName("releaseStatus"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dataSource")), "DATA_SOURCE", queryVO.getFieldValueByFieldName("dataSource"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tailFlag")), "TAIL_FLAG", queryVO.getFieldValueByFieldName("tailFlag"));
    }

    @Override
    protected String getTypeCode() {
        return "TM0815";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}
