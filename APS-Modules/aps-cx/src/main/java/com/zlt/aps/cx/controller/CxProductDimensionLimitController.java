package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxProductDimensionLimit;
import com.zlt.aps.cx.service.CxProductDimensionLimitService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型投产班次同寸口硫化班次限定设置Controller
 *
 * @author zlt
 * @date 2022-01-08
 */
@RestController
@RequestMapping("/dimensionLimit")
public class CxProductDimensionLimitController extends BaseController
{
    @Autowired
    private CxProductDimensionLimitService cxProductDimensionLimitService;

    /**
     * 查询成型投产班次同寸口硫化班次限定设置列表
     */
    @ApiOperation("查询成型投产班次同寸口硫化班次限定设置列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxProductDimensionLimit cxProductDimensionLimit)
    {
        startPage();
        cxProductDimensionLimit.setOrderStr(orderStr());
        List<CxProductDimensionLimit> list = cxProductDimensionLimitService.selectCxProductDimensionLimitList(cxProductDimensionLimit);
        return getDataTable(list);
    }

    /**
     * 获取成型投产班次同寸口硫化班次限定设置详细信息
     */
    @ApiOperation("获取成型投产班次同寸口硫化班次限定设置详细信息")
    @GetMapping(value = "/{id}")
    public CxProductDimensionLimit getInfo(@PathVariable("id") Long id){
        return cxProductDimensionLimitService.selectCxProductDimensionLimitById(id);
    }

    /**
     * 新增成型投产班次同寸口硫化班次限定设置
     */
    @Log(title = "ui.data.column.dimensionLimit.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增成型投产班次同寸口硫化班次限定设置")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxProductDimensionLimit cxProductDimensionLimit){
        return toAjax(cxProductDimensionLimitService.insertCxProductDimensionLimit(cxProductDimensionLimit));
    }

    /**
     * 修改成型投产班次同寸口硫化班次限定设置
     */
    @Log(title = "ui.data.column.dimensionLimit.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型投产班次同寸口硫化班次限定设置")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxProductDimensionLimit cxProductDimensionLimit){
        return toAjax(cxProductDimensionLimitService.updateCxProductDimensionLimit(cxProductDimensionLimit));
    }

    /**
     * 删除成型投产班次同寸口硫化班次限定设置
     */
    @Log(title = "ui.data.column.dimensionLimit.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除成型投产班次同寸口硫化班次限定设置")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(cxProductDimensionLimitService.deleteCxProductDimensionLimitByIds(ids));
    }

    /**
     * 导出成型投产班次同寸口硫化班次限定设置列表
     */
    @Log(title = "ui.data.column.dimensionLimit.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出成型投产班次同寸口硫化班次限定设置列表")
    @PostMapping("/getList")
    public List<CxProductDimensionLimit> getList(@RequestBody CxProductDimensionLimit cxProductDimensionLimit){
        startPage();
        cxProductDimensionLimit.setOrderStr(orderStr());
        return  cxProductDimensionLimitService.selectCxProductDimensionLimitList(cxProductDimensionLimit);
    }

    /**
     * 校验成型投产班次同寸口硫化班次限定设置唯一性
     */
    @ApiOperation("校验成型投产班次同寸口硫化班次限定设置唯一性")
    @PostMapping("/checkCxProductDimensionLimitUnique")
    public String checkCxProductDimensionLimitUnique(@RequestBody CxProductDimensionLimit cxProductDimensionLimit){
        return cxProductDimensionLimitService.checkCxProductDimensionLimitUnique(cxProductDimensionLimit);
    }

    /**
     * 根据集合导入成型投产班次同寸口硫化班次限定设置数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.dimensionLimit.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入成型投产班次同寸口硫化班次限定设置数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxProductDimensionLimit> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxProductDimensionLimitService.importData(list, updateSupport, importLogId);
    }
}
