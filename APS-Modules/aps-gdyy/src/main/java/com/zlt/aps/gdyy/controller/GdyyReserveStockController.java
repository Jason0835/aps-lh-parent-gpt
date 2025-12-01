package com.zlt.aps.gdyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.gdyy.api.domain.dto.GdyyReserveStockDto;
import com.zlt.aps.gdyy.entity.GdyyReserveStock;
import com.zlt.aps.gdyy.service.GdyyReserveStockService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 钢带压延预生产库存倍数设定Controller
 *
 * @author hak
 * @date 2025-02-11
 */
@RestController
@RequestMapping("/gdyy/reserveStock")
public class GdyyReserveStockController extends BaseController {
    @Autowired
    private GdyyReserveStockService gdyyReserveStockService;

    /**
     * 查询钢带压延预生产库存倍数设定列表
     */
    @ApiOperation("根据条件查询钢带压延预生产库存倍数设定列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GdyyReserveStockDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        GdyyReserveStock setting = new GdyyReserveStock();
        BeanUtils.copyProperties(dto, setting);
        List<GdyyReserveStockDto> list = gdyyReserveStockService.selectReserveStockList(setting);
        return getDataTable(list);
    }

    /**
     * 获取钢带压延预生产库存倍数设定详细信息
     */
    @ApiOperation("根据id查询钢带压延预生产库存倍数设定详细信息")
    @GetMapping(value = "/{id}")
    public GdyyReserveStockDto getInfo(@PathVariable("id") Long id) {
        GdyyReserveStock setting = gdyyReserveStockService.selectReserveStockById(id);
        GdyyReserveStockDto dto = new GdyyReserveStockDto();
        BeanUtils.copyProperties(setting, dto);
        return dto;
    }

    /**
     * 修改或新增钢带压延预生产库存倍数设定
     */
    @Log(title = "ui.data.column.gdyy.reserveStock.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("修改或新增钢带压延预生产库存倍数设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody GdyyReserveStockDto dto) {
        GdyyReserveStock setting = new GdyyReserveStock();
        BeanUtils.copyProperties(dto, setting);
        return gdyyReserveStockService.saveReserveStock(setting);
    }

    /**
     * 删除钢带压延预生产库存倍数设定
     */
    @Log(title = "ui.data.column.gdyy.reserveStock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除钢带压延预生产库存倍数设定（id不为空）")
    @PostMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        gdyyReserveStockService.deleteReserveStockByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出钢带压延预生产库存倍数设定列表
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.gdyy.reserveStock.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢带压延预生产库存倍数设定")
    @PostMapping("/export")
    public List<GdyyReserveStockDto> export(@RequestBody GdyyReserveStockDto dto) {
        dto.setOrderStr(orderStr());
        GdyyReserveStock setting = new GdyyReserveStock();
        BeanUtils.copyProperties(dto, setting);
        return gdyyReserveStockService.selectReserveStockList(setting);
    }

    @Log(title = "ui.data.column.gdyy.reserveStock.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢带压延预生产库存倍数设定信息")
    public AjaxResult importData(@RequestBody List<GdyyReserveStockDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return gdyyReserveStockService.importData(list, updateSupport, importLogId);
    }
}
