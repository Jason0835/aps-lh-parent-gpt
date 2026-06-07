package com.zlt.aps.cd90.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90Params;
import com.zlt.aps.cd90.mapper.Cd90ParamsMapper;
import com.zlt.aps.cd90.service.ICd90ParamsService;
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

@Api(tags = "直裁参数设置")
@RestController
@RequestMapping("/cd90Params")
public class Cd90ParamsController extends AbstractDocBizController<Cd90Params> {

    @Resource private ICd90ParamsService cd90ParamsService;
    @Resource private Cd90ParamsMapper cd90ParamsMapper;

    @ApiOperation("查询列表") @PostMapping("/list") @Override
    public TableDataInfo list(@RequestBody Cd90Params queryVO) { return super.list(queryVO); }
    @Log(title = "ui.data.column.params.modelName", businessType = BusinessType.INSERT) @ApiOperation("新增") @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd90Params entity) { return super.save(entity); }
    @Log(title = "ui.data.column.params.modelName", businessType = BusinessType.UPDATE) @ApiOperation("编辑") @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd90Params entity) { return super.save(entity); }
    @Log(title = "ui.data.column.params.modelName", businessType = BusinessType.DELETE) @ApiOperation("删除") @PostMapping("/remove") @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) { return super.removeByIds(ids); }
    @ApiOperation("获取详情") @GetMapping("/getInfo/{id}") @Override
    public Cd90Params getInfo(@PathVariable("id") Long id) { return super.getInfo(id); }
    @ApiOperation("校验唯一性") @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd90Params entity) { return cd90ParamsService.checkUnique(entity); }
    @Log(title = "ui.data.column.params.modelName", businessType = BusinessType.IMPORT) @ApiOperation("导入") @PostMapping("/importData") @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception { return super.importData(importContext, updateSupport); }
    @Log(title = "ui.data.column.params.modelName", businessType = BusinessType.EXPORT) @ApiOperation("导出") @PostMapping("/exportData/{fileName}") @Override
    public byte[] exportData(@RequestBody Cd90Params queryVO, @PathVariable("fileName") String fileName, HttpServletResponse response) throws IOException { return super.exportData(queryVO, fileName, response); }

    @Override protected List<Cd90Params> listExportData(Cd90Params obj) { QueryWrapper<Cd90Params> w = new QueryWrapper<>(); builderCondition(w, obj); List<Cd90Params> list = cd90ParamsMapper.selectList(w); AppUtils.formatData(list, getQueryFormulas()); return list; }
    @Override protected IDocService getDocService() { return cd90ParamsService; }
    @Override protected void builderCondition(QueryWrapper<Cd90Params> qw, Cd90Params vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.like(PubUtil.isNotEmpty(vo.getParamCode()), "PARAM_CODE", vo.getParamCode());
        qw.like(PubUtil.isNotEmpty(vo.getParamName()), "PARAM_NAME", vo.getParamName());
    }
    @Override protected String getTypeCode() { return "CD90_PARAMS"; }
    @Override protected String getOrderBy() { return "PARAM_CODE asc, UPDATE_TIME desc"; }
}