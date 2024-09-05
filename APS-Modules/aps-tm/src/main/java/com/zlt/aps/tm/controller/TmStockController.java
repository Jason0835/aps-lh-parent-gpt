package com.zlt.aps.tm.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.entity.TmStock;
import com.zlt.aps.tm.service.TmStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎面库存信息Controller
 *
 * @author zlt
 * @date 2021-05-25
 */
@Api(tags = "胎面库存信息维护接口")
@RestController
@RequestMapping("/stock")
public class TmStockController extends BaseController {
    @Autowired
    private TmStockService tTmStockService;

    /**
     * 查询胎面库存信息列表
     */
    @ApiOperation("根据条件查询库存列表信息")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TmStock tTmStock) {
        startPage();
        tTmStock.setOrderStr(orderStr());
        List<TmStock> list = tTmStockService.selectTmStockList(tTmStock);
        return getDataTable(list);
    }

    /**
     * 获取胎面库存信息详细信息
     */
    @ApiOperation("根据id查询胎面库存信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    @GetMapping(value = "/selectTmStockById/{id}")
    public TmStock selectTmStockById(@PathVariable("id") Long id) {
        return tTmStockService.selectTmStockById(id);
    }


    /**
     * 新增胎面库存信息
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.INSERT)
    @ApiOperation("新增胎面库存信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody TmStock tTmStock) {
        //唯一性校验（使用库存日期+物料编号为逻辑主键）
        List<TmStock> list = tTmStockService.checkTmStockListUnic(tTmStock);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.stock.message.unique"));
        } else {
            return toAjax(tTmStockService.insertTmStock(tTmStock));
        }
    }

    /**
     * 修改胎面库存信息
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.UPDATE)
    @ApiOperation("修改胎面库存信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody TmStock tTmStock) {
        //唯一性校验（使用库存日期+物料编号为逻辑主键）
        List<TmStock> list = tTmStockService.checkTmStockListUnic(tTmStock);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.stock.message.unique"));
        } else {
            return toAjax(tTmStockService.updateTmStock(tTmStock));
        }
    }

    /**
     * 删除胎面库存信息
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.DELETE)
    @ApiOperation("根据id批量删除胎面库存信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Long[]", value = "主键ids", paramType = "remove")
    })
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {

        return toAjax(tTmStockService.deleteTmStockByIds(ids));
    }

    /**
     * 查询列表
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.EXPORT)
    @PostMapping("/exportList")
    public List<TmStock> exportList(@RequestBody TmStock stock) {
        startPage();
        stock.setOrderStr(orderStr());
        List<TmStock> list = tTmStockService.selectTmStockList(stock);
        return list;
    }

    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<TmStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tTmStockService.importData(list, updateSupport, importLogId);
    }
}
