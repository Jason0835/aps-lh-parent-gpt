package com.zlt.aps.tc.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.aps.tc.service.TcStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧库存信息Controller
 *
 * @author zlt
 * @date 2021-05-31
 */
@RestController
@RequestMapping("/stock")
@Api(tags = "胎侧库存信息维护接口")
public class TcStockController extends BaseController {
    @Autowired
    private TcStockService tcStockService;

    /**
     * 查询胎侧库存信息列表
     */
    @PostMapping("/list")
    @ApiOperation("根据条件查询库存列表信息")
    public TableDataInfo list(@RequestBody TcStock tcStock) {
        startPage();
        tcStock.setOrderStr(orderStr());
        List<TcStock> list = tcStockService.selectTcStockList(tcStock);
        return getDataTable(list);
    }

    /**
     * 获取胎侧库存信息详细信息
     */
    @ApiOperation("根据id查询胎侧库存信息")
    @ApiImplicitParams({@ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")})
    @GetMapping(value = "/selectTcStockById/{id}")
    public TcStock selectTcStockById(@PathVariable("id") Long id) {
        return tcStockService.selectTcStockById(id);
    }


    /**
     * 新增胎侧库存信息
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.INSERT)
    @ApiOperation("新增胎侧库存信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody TcStock tcStock) {
        //唯一性校验（使用库存日期+物料编号为逻辑主键）
        List<TcStock> list = tcStockService.checkTcStockListUnic(tcStock);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.stock.message.unique"));
        } else {
            return toAjax(tcStockService.insertTcStock(tcStock));
        }
    }

    /**
     * 修改胎侧库存信息
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.UPDATE)
    @ApiOperation("修改胎侧库存信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody TcStock tcStock) {
        //唯一性校验（使用库存日期+物料编号为逻辑主键）
        List<TcStock> list = tcStockService.checkTcStockListUnic(tcStock);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.stock.message.unique"));
        } else {
            return toAjax(tcStockService.updateTcStock(tcStock));
        }
    }

    /**
     * 删除胎侧库存信息
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.DELETE)
    @ApiOperation("根据id批量删除胎侧库存信息")
    @ApiImplicitParams({@ApiImplicitParam(name = "ids", dataType = "Long[]", value = "主键ids", paramType = "remove")})
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(tcStockService.deleteTcStockByIds(ids));
    }

    /**
     * 查询列表
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.EXPORT)
    @PostMapping("/exportList")
    public List<TcStock> exportList(@RequestBody TcStock stock) {
        startPage();
        stock.setOrderStr(orderStr());
        List<TcStock> list = tcStockService.selectTcStockList(stock);
        return list;
    }

    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<TcStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tcStockService.importData(list, updateSupport, importLogId);
    }
}
