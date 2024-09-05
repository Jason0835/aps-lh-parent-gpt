package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxStockLocationMapping;
import com.zlt.aps.cx.service.CxStockLocationMappingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存地点映射Controller
 *
 * @author zlt
 * @date 2021-11-15
 */
@RestController
@RequestMapping("/stockLocationMapping")
public class CxStockLocationMappingController extends BaseController
{
    @Autowired
    private CxStockLocationMappingService cxStockLocationMappingService;

    /**
     * 查询库存地点映射列表
     */
    @ApiOperation("查询库存地点映射列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxStockLocationMapping cxStockLocationMapping)
    {
        startPage();
        cxStockLocationMapping.setOrderStr(orderStr());
        List<CxStockLocationMapping> list = cxStockLocationMappingService.selectCxStockLocationMappingList(cxStockLocationMapping);
        return getDataTable(list);
    }

    /**
     * 获取库存地点映射详细信息
     */
    @ApiOperation("获取库存地点映射详细信息")
    @GetMapping(value = "/{id}")
    public CxStockLocationMapping getInfo(@PathVariable("id") Long id){
        return cxStockLocationMappingService.selectCxStockLocationMappingById(id);
    }

    /**
     * 新增库存地点映射
     */
    @Log(title = "ui.data.column.stockLocationMapping.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增库存地点映射")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxStockLocationMapping cxStockLocationMapping){
        return toAjax(cxStockLocationMappingService.insertCxStockLocationMapping(cxStockLocationMapping));
    }

    /**
     * 修改库存地点映射
     */
    @Log(title = "ui.data.column.stockLocationMapping.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改库存地点映射")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxStockLocationMapping cxStockLocationMapping){
        return toAjax(cxStockLocationMappingService.updateCxStockLocationMapping(cxStockLocationMapping));
    }

    /**
     * 删除库存地点映射
     */
    @Log(title = "ui.data.column.stockLocationMapping.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除库存地点映射")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(cxStockLocationMappingService.deleteCxStockLocationMappingByIds(ids));
    }

    /**
     * 导出库存地点映射列表
     */
    @Log(title = "ui.data.column.stockLocationMapping.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出库存地点映射列表")
    @PostMapping("/getList")
    public List<CxStockLocationMapping> getList(@RequestBody CxStockLocationMapping cxStockLocationMapping){
        startPage();
        cxStockLocationMapping.setOrderStr(orderStr());
        return  cxStockLocationMappingService.selectCxStockLocationMappingList(cxStockLocationMapping);
    }

    /**
     * 校验库存地点映射唯一性
     */
    @ApiOperation("校验库存地点映射唯一性")
    @PostMapping("/checkCxStockLocationMappingUnique")
    public String checkCxStockLocationMappingUnique(@RequestBody CxStockLocationMapping cxStockLocationMapping){
        return cxStockLocationMappingService.checkCxStockLocationMappingUnique(cxStockLocationMapping);
    }

    /**
     * 根据集合导入库存地点映射数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.stockLocationMapping.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入库存地点映射数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxStockLocationMapping> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxStockLocationMappingService.importData(list, updateSupport, importLogId);
    }
}
