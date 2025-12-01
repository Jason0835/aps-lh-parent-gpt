package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import com.zlt.aps.cx.service.CxProductStockLimitService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxProductStockLimit;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型投产班次库存限定设置Controller
 *
 * @author zlt
 * @date 2022-01-07
 */
@RestController
@RequestMapping("/shiftLimit")
public class CxProductStockLimitController extends BaseController
{
    @Autowired
    private CxProductStockLimitService cxProductStockLimitService;

    /**
     * 查询成型投产班次库存限定设置列表
     */
    @ApiOperation("查询成型投产班次库存限定设置列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxProductStockLimit cxProductStockLimit)
    {
        startPage();
        cxProductStockLimit.setOrderStr(orderStr());
        List<CxProductStockLimit> list = cxProductStockLimitService.selectCxProductStockLimitList(cxProductStockLimit);
        return getDataTable(list);
    }

    /**
     * 获取成型投产班次库存限定设置详细信息
     */
    @ApiOperation("获取成型投产班次库存限定设置详细信息")
    @GetMapping(value = "/{id}")
    public CxProductStockLimit getInfo(@PathVariable("id") Long id){
        return cxProductStockLimitService.selectCxProductStockLimitById(id);
    }

    /**
     * 新增成型投产班次库存限定设置
     */
    @Log(title = "ui.data.column.shiftLimit.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增成型投产班次库存限定设置")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxProductStockLimit cxProductStockLimit){
        return toAjax(cxProductStockLimitService.insertCxProductStockLimit(cxProductStockLimit));
    }

    /**
     * 修改成型投产班次库存限定设置
     */
    @Log(title = "ui.data.column.shiftLimit.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型投产班次库存限定设置")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxProductStockLimit cxProductStockLimit){
        return toAjax(cxProductStockLimitService.updateCxProductStockLimit(cxProductStockLimit));
    }

    /**
     * 删除成型投产班次库存限定设置
     */
    @Log(title = "ui.data.column.shiftLimit.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除成型投产班次库存限定设置")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(cxProductStockLimitService.deleteCxProductStockLimitByIds(ids));
    }

    /**
     * 导出成型投产班次库存限定设置列表
     */
    @Log(title = "ui.data.column.shiftLimit.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出成型投产班次库存限定设置列表")
    @PostMapping("/getList")
    public List<CxProductStockLimit> getList(@RequestBody CxProductStockLimit cxProductStockLimit){
        startPage();
        cxProductStockLimit.setOrderStr(orderStr());
        return  cxProductStockLimitService.selectCxProductStockLimitList(cxProductStockLimit);
    }

    /**
     * 校验成型投产班次库存限定设置唯一性
     */
    @ApiOperation("校验成型投产班次库存限定设置唯一性")
    @PostMapping("/checkCxProductStockLimitUnique")
    public List<CxProductStockLimit> checkCxProductStockLimitUnique(@RequestBody CxProductStockLimit cxProductStockLimit){
        return cxProductStockLimitService.checkCxProductStockLimitUnique(cxProductStockLimit);
    }

    /**
     * 根据集合导入成型投产班次库存限定设置数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.shiftLimit.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入成型投产班次库存限定设置数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxProductStockLimit> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxProductStockLimitService.importData(list, updateSupport, importLogId);
    }
}
