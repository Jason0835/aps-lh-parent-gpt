package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxProductMachineLimit;
import com.zlt.aps.cx.service.CxProductMachineLimitService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型投产班次同机台硫化班次限定设置Controller
 *
 * @author zlt
 * @date 2022-01-08
 */
@RestController
@RequestMapping("/machineLimit")
public class CxProductMachineLimitController extends BaseController
{
    @Autowired
    private CxProductMachineLimitService cxProductMachineLimitService;

    /**
     * 查询成型投产班次同机台硫化班次限定设置列表
     */
    @ApiOperation("查询成型投产班次同机台硫化班次限定设置列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxProductMachineLimit cxProductMachineLimit)
    {
        startPage();
        cxProductMachineLimit.setOrderStr(orderStr());
        List<CxProductMachineLimit> list = cxProductMachineLimitService.selectCxProductMachineLimitList(cxProductMachineLimit);
        return getDataTable(list);
    }

    /**
     * 获取成型投产班次同机台硫化班次限定设置详细信息
     */
    @ApiOperation("获取成型投产班次同机台硫化班次限定设置详细信息")
    @GetMapping(value = "/{id}")
    public CxProductMachineLimit getInfo(@PathVariable("id") Long id){
        return cxProductMachineLimitService.selectCxProductMachineLimitById(id);
    }

    /**
     * 新增成型投产班次同机台硫化班次限定设置
     */
    @Log(title = "ui.data.column.machineLimit.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增成型投产班次同机台硫化班次限定设置")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxProductMachineLimit cxProductMachineLimit){
        return toAjax(cxProductMachineLimitService.insertCxProductMachineLimit(cxProductMachineLimit));
    }

    /**
     * 修改成型投产班次同机台硫化班次限定设置
     */
    @Log(title = "ui.data.column.machineLimit.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型投产班次同机台硫化班次限定设置")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxProductMachineLimit cxProductMachineLimit){
        return toAjax(cxProductMachineLimitService.updateCxProductMachineLimit(cxProductMachineLimit));
    }

    /**
     * 删除成型投产班次同机台硫化班次限定设置
     */
    @Log(title = "ui.data.column.machineLimit.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除成型投产班次同机台硫化班次限定设置")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(cxProductMachineLimitService.deleteCxProductMachineLimitByIds(ids));
    }

    /**
     * 导出成型投产班次同机台硫化班次限定设置列表
     */
    @Log(title = "ui.data.column.machineLimit.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出成型投产班次同机台硫化班次限定设置列表")
    @PostMapping("/getList")
    public List<CxProductMachineLimit> getList(@RequestBody CxProductMachineLimit cxProductMachineLimit){
        startPage();
        cxProductMachineLimit.setOrderStr(orderStr());
        return  cxProductMachineLimitService.selectCxProductMachineLimitList(cxProductMachineLimit);
    }

    /**
     * 校验成型投产班次同机台硫化班次限定设置唯一性
     */
    @ApiOperation("校验成型投产班次同机台硫化班次限定设置唯一性")
    @PostMapping("/checkCxProductMachineLimitUnique")
    public String checkCxProductMachineLimitUnique(@RequestBody CxProductMachineLimit cxProductMachineLimit){
        return cxProductMachineLimitService.checkCxProductMachineLimitUnique(cxProductMachineLimit);
    }

    /**
     * 根据集合导入成型投产班次同机台硫化班次限定设置数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.machineLimit.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入成型投产班次同机台硫化班次限定设置数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxProductMachineLimit> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxProductMachineLimitService.importData(list, updateSupport, importLogId);
    }
}
