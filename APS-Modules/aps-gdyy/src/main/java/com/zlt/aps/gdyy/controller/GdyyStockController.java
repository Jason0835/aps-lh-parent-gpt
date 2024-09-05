package com.zlt.aps.gdyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import com.zlt.aps.gdyy.service.GdyyStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢带压延库存信息Controller
 *
 * @author zlt
 * @date 2021-05-31
 */
@RestController
@RequestMapping("/stock")
@Api(tags = "钢带压延库存信息维护接口")
public class GdyyStockController extends BaseController {
    @Autowired
    private GdyyStockService stockService;

    /**
     * 查询钢带压延库存信息列表
     */
    @PostMapping("/list")
    @ApiOperation("根据条件查询库存列表信息")
    public TableDataInfo list(@RequestBody GdyyStock stock) {
        startPage();
        stock.setOrderStr(orderStr());
        List<GdyyStock> list = stockService.selectStockList(stock);
        return getDataTable(list);
    }

    /**
     * 获取钢带压延库存信息详细信息
     */
    @ApiOperation("根据id查询钢带压延库存信息")
    @ApiImplicitParams({@ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")})
    @GetMapping(value = "/selectStockById/{id}")
    public GdyyStock selectStockById(@PathVariable("id") Long id) {
        return stockService.selectStockById(id);
    }


    /**
     * 新增钢带压延库存信息
     */
    @Log(title = "ui.frame.page.gdyy.stock.title", businessType = BusinessType.INSERT)
    @ApiOperation("新增钢带压延库存信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody GdyyStock stock) {
        //唯一性校验（使用库存日期+物料编号为逻辑主键）
        List<GdyyStock> list = stockService.checkStockListUnic(stock);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.stock.message.unique"));
        } else {
            return toAjax(stockService.insertStock(stock));
        }
    }

    /**
     * 修改钢带压延库存信息
     */
    @Log(title = "ui.frame.page.gdyy.stock.title", businessType = BusinessType.UPDATE)
    @ApiOperation("修改钢带压延库存信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody GdyyStock stock) {
        //唯一性校验（使用库存日期+物料编号为逻辑主键）
        List<GdyyStock> list = stockService.checkStockListUnic(stock);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.stock.message.unique"));
        } else {
            return toAjax(stockService.updateStock(stock));
        }
    }

    /**
     * 删除钢带压延库存信息
     */
    @Log(title = "ui.frame.page.gdyy.stock.title", businessType = BusinessType.DELETE)
    @ApiOperation("根据id批量删除钢带压延库存信息")
    @ApiImplicitParams({@ApiImplicitParam(name = "ids", dataType = "Long[]", value = "主键ids", paramType = "remove")})
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(stockService.deleteStockByIds(ids));
    }

    /**
     * 查询列表
     */
    @Log(title = "ui.frame.page.gdyy.stock.title", businessType = BusinessType.EXPORT)
    @PostMapping("/exportList")
    public List<GdyyStock> exportList(@RequestBody GdyyStock stock) {
        stock.setOrderStr(orderStr());
        List<GdyyStock> list = stockService.selectStockList(stock);
        return list;
    }

    @Log(title = "ui.frame.page.gdyy.stock.title", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢带压延库存信息")
    public AjaxResult importData(@RequestBody List<GdyyStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return stockService.importData(list, updateSupport, importLogId);
    }
    

    /**
     * 判断是否按大卷计算库存
     */
    @ApiOperation("判断是否按大卷计算库存")
    @GetMapping(value = "/isRollStock")
    public Boolean isRollStock() {
        return stockService.isRollStock();
    }
}
