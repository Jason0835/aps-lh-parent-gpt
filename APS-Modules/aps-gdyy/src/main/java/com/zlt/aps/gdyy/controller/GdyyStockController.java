package com.zlt.aps.gdyy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import com.zlt.aps.gdyy.mapper.GdyyStockMapper;
import com.zlt.aps.gdyy.service.IGdyyStockService;
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

/**
 * 钢带压延库存 控制层。
 */
@Api(tags = "钢带压延库存管理")
@RestController
@RequestMapping("/gdyy/stock")
public class GdyyStockController extends AbstractDocBizController<GdyyStock> {

    @Resource
    private IGdyyStockService gdyyStockService;

    @Resource
    private GdyyStockMapper gdyyStockMapper;

    @ApiOperation("查询钢带压延库存列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GdyyStock queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.gdyyStock.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增钢带压延库存")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody GdyyStock entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.gdyyStock.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑钢带压延库存")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody GdyyStock entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.gdyyStock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除钢带压延库存")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取钢带压延库存详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public GdyyStock getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验钢带压延库存唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody GdyyStock entity) {
        return gdyyStockService.checkUnique(entity);
    }

    @Log(title = "ui.data.column.gdyyStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入钢带压延库存")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.gdyyStock.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢带压延库存")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody GdyyStock queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<GdyyStock> listExportData(GdyyStock obj) {
        QueryWrapper<GdyyStock> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<GdyyStock> list = gdyyStockMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return gdyyStockService;
    }

    @Override
    protected void builderCondition(QueryWrapper<GdyyStock> queryWrapper, GdyyStock queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(queryVO.getStockDate() != null, "STOCK_DATE", queryVO.getStockDate());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBigRollCode()), "BIG_ROLL_CODE", queryVO.getBigRollCode());
    }

    @Override
    protected String getTypeCode() {
        return "GDYY_STOCK";
    }

    @Override
    protected String getOrderBy() {
        return "STOCK_DATE desc, BIG_ROLL_CODE asc";
    }
}
