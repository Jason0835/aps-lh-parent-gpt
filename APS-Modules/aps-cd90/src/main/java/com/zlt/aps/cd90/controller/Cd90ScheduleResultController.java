package com.zlt.aps.cd90.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultMapper;
import com.zlt.aps.cd90.service.ICd90ScheduleResultService;
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

@Api(tags = "直裁排程结果")
@RestController
@RequestMapping("/cd90ScheduleResult")
public class Cd90ScheduleResultController extends AbstractDocBizController<Cd90ScheduleResult> {

    @Resource private ICd90ScheduleResultService cd90ScheduleResultService;
    @Resource private Cd90ScheduleResultMapper cd90ScheduleResultMapper;

    @ApiOperation("查询列表") @PostMapping("/list") @Override
    public TableDataInfo list(@RequestBody Cd90ScheduleResult queryVO) { return super.list(queryVO); }

    @Log(title = "ui.data.column.scheduleResult.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除") @PostMapping("/remove") @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) { return super.removeByIds(ids); }

    @ApiOperation("获取详情") @GetMapping("/getInfo/{id}") @Override
    public Cd90ScheduleResult getInfo(@PathVariable("id") Long id) { return super.getInfo(id); }

    @Log(title = "ui.data.column.scheduleResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入") @PostMapping("/importData") @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception { return super.importData(importContext, updateSupport); }

    @Log(title = "ui.data.column.scheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出") @PostMapping("/exportData/{fileName}") @Override
    public byte[] exportData(@RequestBody Cd90ScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException { return super.exportData(queryVO, fileName, response); }

    @Override protected List<Cd90ScheduleResult> listExportData(Cd90ScheduleResult obj) {
        QueryWrapper<Cd90ScheduleResult> wrapper = new QueryWrapper<>(); builderCondition(wrapper, obj);
        List<Cd90ScheduleResult> list = cd90ScheduleResultMapper.selectList(wrapper); AppUtils.formatData(list, getQueryFormulas()); return list;
    }
    @Override protected IDocService getDocService() { return cd90ScheduleResultService; }
    @Override protected void builderCondition(QueryWrapper<Cd90ScheduleResult> qw, Cd90ScheduleResult vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.eq(vo.getScheduleDate() != null, "SCHEDULE_DATE", vo.getScheduleDate());
        qw.like(PubUtil.isNotEmpty(vo.getClothCode()), "CLOTH_CODE", vo.getClothCode());
        qw.eq(PubUtil.isNotEmpty(vo.getMachineCode()), "MACHINE_CODE", vo.getMachineCode());
    }
    @Override protected String getTypeCode() { return "CD90_SCHEDULE_RESULT"; }
    @Override protected String getOrderBy() { return "SCHEDULE_DATE desc, CLOTH_CODE asc, MACHINE_CODE asc"; }
}