package com.zlt.aps.xwyy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.utils.AppUtils;
import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;
import com.zlt.aps.xwyy.mapper.XwyyStockMapper;
import com.zlt.aps.xwyy.service.IXwyyStockService;
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

@Api(tags = "纤维压延库存管理")
@RestController
@RequestMapping("/xwyyStock")
public class XwyyStockController extends AbstractDocBizController<XwyyStock> {
    @Resource
    private IXwyyStockService service;
    @Resource
    private XwyyStockMapper mapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody XwyyStock query) {
        return super.list(query);
    }

    @Log(title = "ui.data.column.xwyyStock.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody XwyyStock entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.xwyyStock.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody XwyyStock entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.xwyyStock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public XwyyStock getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody XwyyStock entity) {
        return service.checkUnique(entity);
    }

    @Log(title = "ui.data.column.xwyyStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext c, @RequestParam("updateSupport") boolean u) throws Exception {
        return super.importData(c, u);
    }

    @Log(title = "ui.data.column.xwyyStock.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody XwyyStock query, @PathVariable("fileName") String fileName, HttpServletResponse r) throws IOException {
        return super.exportData(query, fileName, r);
    }

    @Override
    protected List<XwyyStock> listExportData(XwyyStock output) {
        QueryWrapper<XwyyStock> w = new QueryWrapper<>();
        builderCondition(w, output);
        List<XwyyStock> l = mapper.selectList(w);
        AppUtils.formatData(l, getQueryFormulas());
        return l;
    }

    @Override
    protected IDocService getDocService() {
        return service;
    }

    @Override
    protected void builderCondition(QueryWrapper<XwyyStock> qw, XwyyStock vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.eq(vo.getStockDate() != null, "STOCK_DATE", vo.getStockDate());
        qw.like(PubUtil.isNotEmpty(vo.getBigRollCode()), "BIG_ROLL_CODE", vo.getBigRollCode());
    }

    @Override
    protected String getTypeCode() {
        return "XWYY_STOCK";
    }

    @Override
    protected String getOrderBy() {
        return "STOCK_DATE desc, BIG_ROLL_CODE asc";
    }
}