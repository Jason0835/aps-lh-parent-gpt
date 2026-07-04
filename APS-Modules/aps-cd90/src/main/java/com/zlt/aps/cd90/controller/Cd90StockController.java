package com.zlt.aps.cd90.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.cd90.mapper.Cd90StockMapper;
import com.zlt.aps.cd90.service.ICd90StockService;
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
import java.util.Date;
import java.util.List;

@Api(tags = "直裁库存管理")
@RestController
@RequestMapping("/cd90Stock")
public class Cd90StockController extends AbstractDocBizController<Cd90Stock> {
    @Resource
    private ICd90StockService service;
    @Resource
    private Cd90StockMapper mapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd90Stock query) {
        return super.list(query);
    }

    @Log(title = "ui.data.column.stock.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd90Stock entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.stock.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd90Stock entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.stock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd90Stock getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd90Stock entity) {
        return service.checkUnique(entity);
    }

    @Log(title = "ui.data.column.stock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext c, @RequestParam("updateSupport") boolean u) throws Exception {
        return super.importData(c, u);
    }

    @Log(title = "ui.data.column.stock.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd90Stock query, @PathVariable("fileName") String fileName, HttpServletResponse r) throws IOException {
        return super.exportData(query, fileName, r);
    }

    @ApiOperation("逻辑删除并批量保存直裁库存（MES同步专用，事务性操作）")
    @PostMapping("/logicDeleteAndSaveCd90StockByDataSource")
    public AjaxResult logicDeleteAndSaveCd90StockByDataSource(@RequestParam("factoryCode") String factoryCode,
                                                              @RequestParam("dataSource") String dataSource,
                                                              @RequestParam("stockDate") String stockDateStr,
                                                              @RequestParam("shiftCode") String shiftCode,
                                                              @RequestParam("updateBy") String updateBy,
                                                              @RequestBody List<Cd90Stock> list) {
        Date stockDate = cn.hutool.core.date.DateUtil.parse(stockDateStr);
        service.logicDeleteAndSaveBatch(factoryCode, dataSource, stockDate, shiftCode, updateBy, list);
        return AjaxResult.success();
    }

    @Override
    protected List<Cd90Stock> listExportData(Cd90Stock output) {
        QueryWrapper<Cd90Stock> w = new QueryWrapper<>();
        builderCondition(w, output);
        List<Cd90Stock> l = mapper.selectList(w);
        AppUtils.formatData(l, getQueryFormulas());
        return l;
    }

    @Override
    protected IDocService getDocService() {
        return service;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd90Stock> qw, Cd90Stock vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.eq(vo.getStockDate() != null, "STOCK_DATE", vo.getStockDate());
        qw.eq(PubUtil.isNotEmpty(vo.getShiftCode()), "SHIFT_CODE", vo.getShiftCode());
        qw.like(PubUtil.isNotEmpty(vo.getMaterialCode()), "MATERIAL_CODE", vo.getMaterialCode());
    }

    @Override
    protected String getTypeCode() {
        return "CD90_STOCK";
    }

    @Override
    protected String getOrderBy() {
        return "STOCK_DATE desc, MATERIAL_CODE asc";
    }
}