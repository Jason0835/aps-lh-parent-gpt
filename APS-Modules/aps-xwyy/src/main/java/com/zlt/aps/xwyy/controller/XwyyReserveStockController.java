package com.zlt.aps.xwyy.controller;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.xwyy.api.domain.dto.XwyyReserveStockDto;
import com.zlt.aps.xwyy.entity.XwyyReserveStock;
import com.zlt.aps.xwyy.service.XwyyReserveStockService;

import io.swagger.annotations.ApiOperation;

/**
 * 纤维压延预生产库存倍数设定Controller
 *
 * @author hak
 * @date 2025-02-11
 */
@RestController
@RequestMapping("/xwyy/reserveStock")
public class XwyyReserveStockController extends BaseController {
    @Autowired
    private XwyyReserveStockService xwyyReserveStockService;

    /**
     * 查询纤维压延预生产库存倍数设定列表
     */
    @ApiOperation("根据条件查询纤维压延预生产库存倍数设定列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody XwyyReserveStockDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        XwyyReserveStock setting = new XwyyReserveStock();
        BeanUtils.copyProperties(dto, setting);
        List<XwyyReserveStockDto> list = xwyyReserveStockService.selectReserveStockList(setting);
        return getDataTable(list);
    }

    /**
     * 获取纤维压延预生产库存倍数设定详细信息
     */
    @ApiOperation("根据id查询纤维压延预生产库存倍数设定详细信息")
    @GetMapping(value = "/{id}")
    public XwyyReserveStockDto getInfo(@PathVariable("id") Long id) {
        XwyyReserveStock setting = xwyyReserveStockService.selectReserveStockById(id);
        XwyyReserveStockDto dto = new XwyyReserveStockDto();
        BeanUtils.copyProperties(setting, dto);
        return dto;
    }

    /**
     * 修改或新增纤维压延预生产库存倍数设定
     */
    @Log(title = "ui.data.column.xwyy.reserveStock.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("修改或新增纤维压延预生产库存倍数设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody XwyyReserveStockDto dto) {
        XwyyReserveStock setting = new XwyyReserveStock();
        BeanUtils.copyProperties(dto, setting);
        return xwyyReserveStockService.saveReserveStock(setting);
    }

    /**
     * 删除纤维压延预生产库存倍数设定
     */
    @Log(title = "ui.data.column.xwyy.reserveStock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除纤维压延预生产库存倍数设定（id不为空）")
    @PostMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        xwyyReserveStockService.deleteReserveStockByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出纤维压延预生产库存倍数设定列表
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.xwyy.reserveStock.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出纤维压延预生产库存倍数设定")
    @PostMapping("/export")
    public List<XwyyReserveStockDto> export(@RequestBody XwyyReserveStockDto dto) {
        dto.setOrderStr(orderStr());
        XwyyReserveStock setting = new XwyyReserveStock();
        BeanUtils.copyProperties(dto, setting);
        return xwyyReserveStockService.selectReserveStockList(setting);
    }

    @Log(title = "ui.data.column.xwyy.reserveStock.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入纤维压延预生产库存倍数设定信息")
    public AjaxResult importData(@RequestBody List<XwyyReserveStockDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return xwyyReserveStockService.importData(list, updateSupport, importLogId);
    }
}
