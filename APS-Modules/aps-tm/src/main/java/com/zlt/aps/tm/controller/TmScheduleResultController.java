package com.zlt.aps.tm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.redissonLock.annotation.DistributedLock;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleRequestVo;
import com.zlt.aps.tm.api.domain.vo.TmScheduleShiftDateVO;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.service.ITmScheduleResultService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
        return super.removeByIds(ids);
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
     * @param scheduleDate 排程日期
     * @return 班次日期列表
     */
    @ApiOperation("获取胎面排程班次日期列表")
    @PostMapping("/listScheduleShiftDates")
    public List<TmScheduleShiftDateVO> listScheduleShiftDates(@RequestBody TmScheduleResult scheduleResult) {
        return tmScheduleResultService.listScheduleShiftDates(scheduleResult.getScheduleDate());
    }

    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TmScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
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
