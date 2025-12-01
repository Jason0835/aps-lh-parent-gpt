package com.zlt.aps.gsq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqStock;
import com.zlt.aps.gsq.service.GsqStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈库存信息Controller
 *
 * @author zlt
 * @date 2021-05-31
 */
@RestController
@RequestMapping("/gsq/stock")
@Api(tags = "钢丝圈库存信息维护接口")
public class GsqStockController extends BaseController {
    @Autowired
    private GsqStockService stockService;

    /**
     * 查询钢丝圈库存信息列表
     */
    @PostMapping("/list")
    @ApiOperation("根据条件查询库存列表信息")
    public TableDataInfo list(@RequestBody GsqStock stock) {
        startPage();
        stock.setOrderStr(orderStr());
        List<GsqStock> list = stockService.selectStockList(stock);
        return getDataTable(list);
    }

    /**
     * 获取钢丝圈库存信息详细信息
     */
    @ApiOperation("根据id查询钢丝圈库存信息")
    @ApiImplicitParams({@ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")})
    @GetMapping(value = "/selectStockById/{id}")
    public GsqStock selectStockById(@PathVariable("id") Long id) {
        return stockService.selectStockById(id);
    }


    /**
     * 新增钢丝圈库存信息
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.INSERT)
    @ApiOperation("新增钢丝圈库存信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody GsqStock stock) {
        //唯一性校验（使用库存日期+物料编号为逻辑主键）
        List<GsqStock> list = stockService.checkStockListUnic(stock);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.stock.message.unique"));
        } else {
            return toAjax(stockService.insertStock(stock));
        }
    }

    /**
     * 修改钢丝圈库存信息
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.UPDATE)
    @ApiOperation("修改钢丝圈库存信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody GsqStock stock) {
        //唯一性校验（使用库存日期+物料编号为逻辑主键）
        List<GsqStock> list = stockService.checkStockListUnic(stock);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.stock.message.unique"));
        } else {
            return toAjax(stockService.updateStock(stock));
        }
    }

    /**
     * 删除钢丝圈库存信息
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.DELETE)
    @ApiOperation("根据id批量删除钢丝圈库存信息")
    @ApiImplicitParams({@ApiImplicitParam(name = "ids", dataType = "Long[]", value = "主键ids")})
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(stockService.deleteStockByIds(ids));
    }

    /**
     * 查询列表
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.EXPORT)
    @ApiOperation("查询钢丝圈库存信息列表")
    @PostMapping("/exportList")
    public List<GsqStock> exportList(@RequestBody GsqStock stock) {
        stock.setOrderStr(orderStr());
        List<GsqStock> list = stockService.selectStockList(stock);
        return list;
    }

    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢丝圈库存信息")
    public AjaxResult importData(@RequestBody List<GsqStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtil.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return stockService.importData(list, updateSupport, importLogId);
    }
}
