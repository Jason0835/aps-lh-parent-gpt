package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.dto.CxStockLocationSortDto;
import com.zlt.aps.cx.entity.CxStockLocationSort;
import com.zlt.aps.cx.service.CxStockLocationSortService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存地点生产顺序Controller
 *
 * @author chen
 * @date 2021-07-22
 */
@RestController
@RequestMapping("/stockLocationSort")
public class CxStockLocationSortController extends BaseController {
    @Autowired
    private CxStockLocationSortService cxStockLocationSortService;

    /**
     * 查询库存地点生产顺序列表
     */
    //@PreAuthorize(hasPermi = "cx:stockLocationSort:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxStockLocationSortDto dto) {
        CxStockLocationSort cxStockLocationSort = new CxStockLocationSort();
        BeanUtils.copyProperties(dto, cxStockLocationSort);
        startPage();
        cxStockLocationSort.setOrderStr(orderStr());
        List<CxStockLocationSortDto> list = cxStockLocationSortService.selectCxStockLocationSortList(cxStockLocationSort);
        return getDataTable(list);
    }

    /**
     * 获取库存地点生产顺序详细信息
     */
    //@PreAuthorize(hasPermi = "cx:stockLocationSort:query")
    @GetMapping(value = "/{id}")
    public CxStockLocationSortDto getInfo(@PathVariable("id") Long id) {
        return cxStockLocationSortService.selectCxStockLocationSortById(id);
    }

    /**
     * 新增库存地点生产顺序
     */
    //@PreAuthorize(hasPermi = "cx:stockLocationSort:add")
    @Log(title = "ui.data.column.stockLocationSort.modalName", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxStockLocationSortDto dto) {
        CxStockLocationSort cxStockLocationSort = new CxStockLocationSort();
        BeanUtils.copyProperties(dto, cxStockLocationSort);
        return toAjax(cxStockLocationSortService.insertCxStockLocationSort(cxStockLocationSort));
    }

    /**
     * 修改库存地点生产顺序
     */
    //@PreAuthorize(hasPermi = "cx:stockLocationSort:edit")
    @Log(title = "ui.data.column.stockLocationSort.modalName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxStockLocationSortDto dto) {
        CxStockLocationSort cxStockLocationSort = new CxStockLocationSort();
        BeanUtils.copyProperties(dto, cxStockLocationSort);
        return toAjax(cxStockLocationSortService.updateCxStockLocationSort(cxStockLocationSort));
    }

    /**
     * 删除库存地点生产顺序
     */
    //@PreAuthorize(hasPermi = "cx:stockLocationSort:remove")
    @Log(title = "ui.data.column.stockLocationSort.modalName", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cxStockLocationSortService.deleteCxStockLocationSortByIds(ids));
    }

    /**
     * 导出库存地点生产顺序列表
     */
    //@PreAuthorize(hasPermi = "cx:stockLocationSort:export")
    @Log(title = "ui.data.column.stockLocationSort.modalName", businessType = BusinessType.EXPORT)
    @PostMapping("/getList")
    public List<CxStockLocationSortDto> getList(@RequestBody CxStockLocationSortDto dto) {
        CxStockLocationSort cxStockLocationSort = new CxStockLocationSort();
        BeanUtils.copyProperties(dto, cxStockLocationSort);
        startPage();
        cxStockLocationSort.setOrderStr(orderStr());
        return cxStockLocationSortService.selectCxStockLocationSortList(cxStockLocationSort);
    }

    /**
     * 校验库存地点生产顺序唯一性
     */
    @ApiOperation("校验库存地点生产顺序唯一性")
    @PostMapping("/checkCxStockLocationSortUnique")
    public String checkCxStockLocationSortUnique(@RequestBody CxStockLocationSortDto dto) {
        CxStockLocationSort cxStockLocationSort = new CxStockLocationSort();
        BeanUtils.copyProperties(dto, cxStockLocationSort);
        return cxStockLocationSortService.checkCxStockLocationSortUnique(cxStockLocationSort);
    }

    @Log(title = "ui.data.column.stockLocationSort.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxStockLocationSortDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxStockLocationSortService.importData(list, updateSupport, importLogId);
    }
}
