package com.zlt.aps.cd90.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResultLog;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultLogMapper;
import com.zlt.aps.cd90.service.ICd90ScheduleResultLogService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Api(tags = "直裁排程结果日志")
@RestController
@RequestMapping("/cd90ScheduleResultLog")
public class Cd90ScheduleResultLogController extends AbstractDocBizController<Cd90ScheduleResultLog> {

    @Resource private ICd90ScheduleResultLogService service;
    @Resource private Cd90ScheduleResultLogMapper mapper;

    @ApiOperation("查询列表") @PostMapping("/list") @Override
    public TableDataInfo list(@RequestBody Cd90ScheduleResultLog q) { return super.list(q); }
    @Log(title = "ui.data.column.scheduleResultLog.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除") @PostMapping("/remove") @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) { return super.removeByIds(ids); }
    @ApiOperation("获取详情") @GetMapping("/getInfo/{id}") @Override
    public Cd90ScheduleResultLog getInfo(@PathVariable("id") Long id) { return super.getInfo(id); }
    @Log(title = "ui.data.column.scheduleResultLog.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出") @PostMapping("/exportData/{fileName}") @Override
    public byte[] exportData(@RequestBody Cd90ScheduleResultLog q, @PathVariable("fileName") String n, HttpServletResponse r) throws IOException { return super.exportData(q, n, r); }
    @Override protected List<Cd90ScheduleResultLog> listExportData(Cd90ScheduleResultLog obj) { QueryWrapper<Cd90ScheduleResultLog> w = new QueryWrapper<>(); builderCondition(w, obj); List<Cd90ScheduleResultLog> list = mapper.selectList(w); AppUtils.formatData(list, getQueryFormulas()); return list; }
    @Override protected IDocService getDocService() { return service; }
    @Override protected void builderCondition(QueryWrapper<Cd90ScheduleResultLog> qw, Cd90ScheduleResultLog vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.eq(vo.getScheduleDate() != null, "SCHEDULE_DATE", vo.getScheduleDate());
        qw.like(PubUtil.isNotEmpty(vo.getClothCode()), "CLOTH_CODE", vo.getClothCode());
    }
    @Override protected String getTypeCode() { return "CD90_SCHEDULE_RESULT_LOG"; }
    @Override protected String getOrderBy() { return "LOG_TIME desc"; }
}