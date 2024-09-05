package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.service.CxStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型库存信息Controller
 *
 * @author zlt
 * @date 2021-05-25
 */
@Api(tags = "成型库存信息维护接口")
@RestController
@RequestMapping("cx/stock")
public class CxStockController extends BaseController {
    @Autowired
    private CxStockService cxStockService;

    /**
     * 查询成型库存信息列表
     */
    @ApiOperation("根据条件查询库存列表信息")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxStock cxStock) {
        startPage();
        cxStock.setOrderStr(orderStr());
        List<CxStock> list = cxStockService.selectCxStockList(cxStock);
        return getDataTable(list);
    }

    /**
     * 获取成型库存信息详细信息
     */
    @ApiOperation("根据id查询成型库存信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    @GetMapping(value = "/selectCxStockById/{id}")
    public CxStock selectCxStockById(@PathVariable("id") Long id) {
        return cxStockService.selectCxStockById(id);
    }


    /**
     * 新增成型库存信息
     */
    @Log(title = "ui.cx.stock.export.fileName", businessType = BusinessType.INSERT)
    @ApiOperation("新增成型库存信息（id不为空）")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxStock cxStock) {
        //唯一性校验（使用库存日期+物料编号为逻辑主键）
        List<CxStock> list = cxStockService.checkCxStockListUnic(cxStock);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.stock.message.unique"));
        } else {
            return toAjax(cxStockService.insertCxStock(cxStock));
        }
    }

    /**
     * 修改成型库存信息
     */
    @Log(title = "ui.cx.stock.export.fileName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型库存信息（id不为空）")
    @PutMapping("/edit")
    public AjaxResult edit(@RequestBody CxStock cxStock) {
        //唯一性校验（使用库存日期+物料编号为逻辑主键）
        List<CxStock> list = cxStockService.checkCxStockListUnic(cxStock);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.stock.message.unique"));
        } else {
            return toAjax(cxStockService.updateCxStock(cxStock));
        }
    }

    /**
     * 删除成型库存信息
     */
    @Log(title = "ui.cx.stock.export.fileName", businessType = BusinessType.DELETE)
    @ApiOperation("根据id批量删除成型库存信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Long[]", value = "主键ids", paramType = "remove")
    })
    @DeleteMapping("/remove/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(cxStockService.deleteCxStockByIds(ids));
    }

    /**
     * 查询列表
     */
    @Log(title = "ui.cx.stock.export.fileName", businessType = BusinessType.EXPORT)
    @PostMapping("/exportList")
    public List<CxStock> exportList(@RequestBody CxStock cxStock) {
        startPage();
        cxStock.setOrderStr(orderStr());
        List<CxStock> list = cxStockService.selectCxStockList(cxStock);
        return list;
    }

    @Log(title = "ui.cx.stock.export.fileName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxStockService.importData(list, updateSupport, importLogId);
    }

    /**
     * 释放不可用库存
     */
    @Log(title = "ui.cx.stock.export.fileName", businessType = BusinessType.UPDATE)
    @ApiOperation("根据id批量释放不可用库存")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Long[]", value = "主键ids", paramType = "update")
    })
    @PostMapping("/releaseStock")
    public AjaxResult releaseStock(@RequestBody Long[] ids) {

        return toAjax(cxStockService.releaseStock(ids));
    }
}
