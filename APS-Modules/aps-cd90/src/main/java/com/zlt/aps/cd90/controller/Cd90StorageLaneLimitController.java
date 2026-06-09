package com.zlt.aps.cd90.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.aps.cd90.mapper.Cd90StorageLaneLimitMapper;
import com.zlt.aps.cd90.service.ICd90StorageLaneLimitService;
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

@Api(tags = "直裁库排限制")
@RestController
@RequestMapping("/cd90StorageLaneLimit")
public class Cd90StorageLaneLimitController extends AbstractDocBizController<Cd90StorageLaneLimit> {
    @Resource private ICd90StorageLaneLimitService service;
    @Resource private Cd90StorageLaneLimitMapper mapper;

    @ApiOperation("查询列表") @PostMapping("/list") @Override public TableDataInfo list(@RequestBody Cd90StorageLaneLimit q) { return super.list(q); }
    @Log(title = "ui.data.column.storageLaneLimit.modelName", businessType = BusinessType.INSERT) @ApiOperation("新增") @PostMapping("/add") public AjaxResult add(@RequestBody Cd90StorageLaneLimit e) { return super.save(e); }
    @Log(title = "ui.data.column.storageLaneLimit.modelName", businessType = BusinessType.UPDATE) @ApiOperation("编辑") @PostMapping("/edit") public AjaxResult edit(@RequestBody Cd90StorageLaneLimit e) { return super.save(e); }
    @Log(title = "ui.data.column.storageLaneLimit.modelName", businessType = BusinessType.DELETE) @ApiOperation("删除") @PostMapping("/remove") @Override public AjaxResult removeByIds(@RequestBody List<Long> ids) { return super.removeByIds(ids); }
    @ApiOperation("获取详情") @GetMapping("/getInfo/{id}") @Override public Cd90StorageLaneLimit getInfo(@PathVariable("id") Long id) { return super.getInfo(id); }
    @ApiOperation("校验唯一性") @PostMapping("/checkUnique") public String checkUnique(@RequestBody Cd90StorageLaneLimit e) { return service.checkUnique(e); }
    @Log(title = "ui.data.column.storageLaneLimit.modelName", businessType = BusinessType.IMPORT) @ApiOperation("导入") @PostMapping("/importData") @Override public AjaxResult importData(@RequestBody ImportContext c, @RequestParam("updateSupport") boolean u) throws Exception { return super.importData(c, u); }
    @Log(title = "ui.data.column.storageLaneLimit.modelName", businessType = BusinessType.EXPORT) @ApiOperation("导出") @PostMapping("/exportData/{fileName}") @Override public byte[] exportData(@RequestBody Cd90StorageLaneLimit q, @PathVariable("fileName") String n, HttpServletResponse r) throws IOException { return super.exportData(q, n, r); }
    @Override protected List<Cd90StorageLaneLimit> listExportData(Cd90StorageLaneLimit o) { QueryWrapper<Cd90StorageLaneLimit> w = new QueryWrapper<>(); builderCondition(w, o); List<Cd90StorageLaneLimit> l = mapper.selectList(w); AppUtils.formatData(l, getQueryFormulas()); return l; }
    @Override protected IDocService getDocService() { return service; }
    @Override protected void builderCondition(QueryWrapper<Cd90StorageLaneLimit> qw, Cd90StorageLaneLimit vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.eq(vo.getLaneDate() != null, "LANE_DATE", vo.getLaneDate());
        qw.eq(PubUtil.isNotEmpty(vo.getShiftCode()), "SHIFT_CODE", vo.getShiftCode());
        qw.like(PubUtil.isNotEmpty(vo.getStorageLaneCode()), "STORAGE_LANE_CODE", vo.getStorageLaneCode());
    }
    @Override protected String getTypeCode() { return "CD90_STORAGE_LANE_LIMIT"; }
    @Override protected String getOrderBy() { return "LANE_DATE desc, SHIFT_CODE asc, STORAGE_LANE_CODE asc"; }
}