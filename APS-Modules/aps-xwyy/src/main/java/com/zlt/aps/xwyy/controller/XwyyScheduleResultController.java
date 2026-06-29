package com.zlt.aps.xwyy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.utils.AppUtils;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import com.zlt.aps.xwyy.mapper.XwyyScheduleResultMapper;
import com.zlt.aps.xwyy.service.IXwyyScheduleResultService;
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

@Api(tags = "纤维压延排程结果")
@RestController
@RequestMapping("/xwyyScheduleResult")
public class XwyyScheduleResultController extends AbstractDocBizController<XwyyScheduleResult> {
    @Resource
    private IXwyyScheduleResultService service;
    @Resource
    private XwyyScheduleResultMapper mapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody XwyyScheduleResult query) {
        return super.list(query);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public XwyyScheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.OTHER)
    @ApiOperation("自动排程")
    @PostMapping("/autoSchedule")
    public AjaxResult autoSchedule(@RequestBody XwyyScheduleResult entity) {
        return service.autoSchedule(entity);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.OTHER)
    @ApiOperation("插单")
    @PostMapping("/insert")
    public AjaxResult insert(@RequestBody XwyyScheduleResult entity) {
        return service.insert(entity);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody XwyyScheduleResult entity) {
        return service.changeMachine(entity);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("调量")
    @PostMapping("/adjustQty")
    public AjaxResult adjustQty(@RequestBody XwyyScheduleResult entity) {
        return service.adjustQty(entity);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.OTHER)
    @ApiOperation("发布")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody XwyyScheduleResult entity) {
        return service.publish(entity);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext c, @RequestParam("updateSupport") boolean u) throws Exception {
        return super.importData(c, u);
    }

    @Log(title = "ui.data.column.xwyyScheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody XwyyScheduleResult query, @PathVariable("fileName") String fileName, HttpServletResponse r) throws IOException {
        return super.exportData(query, fileName, r);
    }

    @Override
    protected List<XwyyScheduleResult> listExportData(XwyyScheduleResult output) {
        QueryWrapper<XwyyScheduleResult> w = new QueryWrapper<>();
        builderCondition(w, output);
        List<XwyyScheduleResult> l = mapper.selectList(w);
        AppUtils.formatData(l, getQueryFormulas());
        return l;
    }

    @Override
    protected IDocService getDocService() {
        return service;
    }

    @Override
    protected void builderCondition(QueryWrapper<XwyyScheduleResult> qw, XwyyScheduleResult vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.eq(vo.getScheduleDate() != null, "SCHEDULE_DATE", vo.getScheduleDate());
        qw.like(PubUtil.isNotEmpty(vo.getBigRollCode()), "BIG_ROLL_CODE", vo.getBigRollCode());
        qw.like(PubUtil.isNotEmpty(vo.getMachineId()), "MACHINE_ID", vo.getMachineId());
        qw.eq(PubUtil.isNotEmpty(vo.getIsRelease()), "IS_RELEASE", vo.getIsRelease());
        qw.eq(PubUtil.isNotEmpty(vo.getBatchNo()), "BATCH_NO", vo.getBatchNo());
        qw.eq(PubUtil.isNotEmpty(vo.getOrderNo()), "ORDER_NO", vo.getOrderNo());
    }

    @Override
    protected String getTypeCode() {
        return "XWYY_SCHEDULE_RESULT";
    }

    @Override
    protected String getOrderBy() {
        return "SCHEDULE_DATE desc, BIG_ROLL_CODE asc";
    }
}
