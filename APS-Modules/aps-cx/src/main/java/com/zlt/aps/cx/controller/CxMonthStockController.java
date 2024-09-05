package com.zlt.aps.cx.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.dto.CxMonthStockDto;
import com.zlt.aps.cx.entity.CxMonthStock;
import com.zlt.aps.cx.service.CxMonthStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型月结库存Controller
 *
 * @author chen
 * @date 2021-06-17
 */
@RestController
@RequestMapping("/cx/monthStock")
@Api(tags = {"成型月结库存信息维护接口"})
public class CxMonthStockController extends BaseController {
    @Autowired
    private CxMonthStockService cxMonthStockService;

    /**
     * 查询成型月结库存列表
     */
    //@PreAuthorize(hasPermi = "cx:monthStock:list")
    @PostMapping("/list")
    @ApiOperation("查询成型月结库存列表")
    public TableDataInfo list(@RequestBody CxMonthStockDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<CxMonthStockDto> list = cxMonthStockService.selectCxMonthStockList(dto);
        return getDataTable(list);
    }

    /**
     * 获取成型月结库存详细信息
     */
    @GetMapping("/{id}")
    @ApiOperation("获取成型月结库存详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public CxMonthStockDto getInfo(@PathVariable("id") Long id) {
        CxMonthStock monthStock = cxMonthStockService.selectCxMonthStockById(id);
        CxMonthStockDto dto = new CxMonthStockDto();
        BeanUtils.copyProperties(monthStock, dto);
        return dto;
    }

    /**
     * 修改成型月结库存
     */
    //@PreAuthorize(hasPermi = "cx:monthStock:edit")
    @Log(title = "ui.data.column.cx.monthStock.modelName", businessType = BusinessType.INSERT)
    @PostMapping("/edit")
    @ApiOperation("修改成型排产限制（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody CxMonthStockDto dto) {
        CxMonthStock monthStock = new CxMonthStock();
        BeanUtils.copyProperties(dto, monthStock);
        String unique = cxMonthStockService.checkUnique(monthStock);
        if (UserConstants.UNIQUE.equals(unique)) {
            cxMonthStockService.saveCxMonthStock(monthStock);
            return AjaxResult.success();
        }
        return AjaxResult.error(I18nUtil.getMessage("ui.error.message.monthStock.unique"));
    }

    /**
     * 删除成型月结库存
     */
    //@PreAuthorize(hasPermi = "cx:monthStock:remove")
    @Log(title = "ui.data.column.cx.monthStock.modelName", businessType = BusinessType.DELETE)
    @PostMapping("/{ids}")
    @ApiOperation("删除成型月结库存")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        cxMonthStockService.deleteCxMonthStockByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出成型月结库存列表
     */
    //@PreAuthorize(hasPermi = "cx:monthStock:export")
    @Log(title = "ui.data.column.cx.monthStock.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ApiOperation("导出成型月结库存列表")
    public List<CxMonthStockDto> export(@SpringQueryMap CxMonthStockDto dto) {
        CxMonthStock monthStock = new CxMonthStock();
        BeanUtils.copyProperties(dto, monthStock);
        startPage();
        dto.setOrderStr(orderStr());
        return cxMonthStockService.selectCxMonthStockList(dto);
    }

    @Log(title = "ui.data.column.cx.monthStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxMonthStockDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxMonthStockService.importData(list, updateSupport, importLogId);
    }
}
