package com.zlt.aps.gsq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.gsq.api.domain.dto.GsqReserveStockDto;
import com.zlt.aps.gsq.entity.GsqReserveStock;
import com.zlt.aps.gsq.service.GsqReserveStockService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 钢丝圈预生产库存倍数设定Controller
 *
 * @author hak
 * @date 2025-02-11
 */
@RestController
@RequestMapping("/gsq/reserveStock")
public class GsqReserveStockController extends BaseController {
    @Autowired
    private GsqReserveStockService gsqReserveStockService;

    /**
     * 查询钢丝圈预生产库存倍数设定列表
     */
    @ApiOperation("根据条件查询钢丝圈预生产库存倍数设定列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GsqReserveStockDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        GsqReserveStock setting = new GsqReserveStock();
        BeanUtils.copyProperties(dto, setting);
        List<GsqReserveStockDto> list = gsqReserveStockService.selectReserveStockList(setting);
        return getDataTable(list);
    }

    /**
     * 获取钢丝圈预生产库存倍数设定详细信息
     */
    @ApiOperation("根据id查询钢丝圈预生产库存倍数设定详细信息")
    @GetMapping(value = "/{id}")
    public GsqReserveStockDto getInfo(@PathVariable("id") Long id) {
        GsqReserveStock setting = gsqReserveStockService.selectReserveStockById(id);
        GsqReserveStockDto dto = new GsqReserveStockDto();
        BeanUtils.copyProperties(setting, dto);
        return dto;
    }

    /**
     * 修改或新增钢丝圈预生产库存倍数设定
     */
    @Log(title = "ui.data.column.gsq.reserveStock.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("修改或新增钢丝圈预生产库存倍数设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody GsqReserveStockDto dto) {
        GsqReserveStock setting = new GsqReserveStock();
        BeanUtils.copyProperties(dto, setting);
        return gsqReserveStockService.saveReserveStock(setting);
    }

    /**
     * 删除钢丝圈预生产库存倍数设定
     */
    @Log(title = "ui.data.column.gsq.reserveStock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除钢丝圈预生产库存倍数设定（id不为空）")
    @PostMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        gsqReserveStockService.deleteReserveStockByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出钢丝圈预生产库存倍数设定列表
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.gsq.reserveStock.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢丝圈预生产库存倍数设定")
    @PostMapping("/export")
    public List<GsqReserveStockDto> export(@RequestBody GsqReserveStockDto dto) {
        dto.setOrderStr(orderStr());
        GsqReserveStock setting = new GsqReserveStock();
        BeanUtils.copyProperties(dto, setting);
        return gsqReserveStockService.selectReserveStockList(setting);
    }

    @Log(title = "ui.data.column.gsq.reserveStock.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢丝圈预生产库存倍数设定信息")
    public AjaxResult importData(@RequestBody List<GsqReserveStockDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return gsqReserveStockService.importData(list, updateSupport, importLogId);
    }
}
