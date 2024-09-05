package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.SapSpecMoldUse;
import com.zlt.aps.cx.service.SapSpecMoldUseService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 规格使用模数Controller
 *
 * @author zlt
 * @date 2022-01-18
 */
@RestController
@RequestMapping("/sapSpecMoldUse")
public class SapSpecMoldUseController extends BaseController
{
    @Autowired
    private SapSpecMoldUseService sapSpecMoldUseService;

    /**
     * 查询规格使用模数列表
     */
    @ApiOperation("查询规格使用模数列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody SapSpecMoldUse sapSpecMoldUse)
    {
        startPage();
        sapSpecMoldUse.setOrderStr(orderStr());
        List<SapSpecMoldUse> list = sapSpecMoldUseService.selectSapSpecMoldUseList(sapSpecMoldUse);
        return getDataTable(list);
    }

    /**
     * 获取规格使用模数详细信息
     */
    @ApiOperation("获取规格使用模数详细信息")
    @GetMapping(value = "/{id}")
    public SapSpecMoldUse getInfo(@PathVariable("id") Long id){
        return sapSpecMoldUseService.selectSapSpecMoldUseById(id);
    }

    /**
     * 新增规格使用模数
     */
    @Log(title = "ui.data.column.sapSpecMoldUse.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增规格使用模数")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody SapSpecMoldUse sapSpecMoldUse){
        return toAjax(sapSpecMoldUseService.insertSapSpecMoldUse(sapSpecMoldUse));
    }

    /**
     * 修改规格使用模数
     */
    @Log(title = "ui.data.column.sapSpecMoldUse.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改规格使用模数")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody SapSpecMoldUse sapSpecMoldUse){
        return toAjax(sapSpecMoldUseService.updateSapSpecMoldUse(sapSpecMoldUse));
    }

    /**
     * 删除规格使用模数
     */
    @Log(title = "ui.data.column.sapSpecMoldUse.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除规格使用模数")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(sapSpecMoldUseService.deleteSapSpecMoldUseByIds(ids));
    }

    /**
     * 导出规格使用模数列表
     */
    @Log(title = "ui.data.column.sapSpecMoldUse.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出规格使用模数列表")
    @PostMapping("/getList")
    public List<SapSpecMoldUse> getList(@RequestBody SapSpecMoldUse sapSpecMoldUse){
        startPage();
        sapSpecMoldUse.setOrderStr(orderStr());
        return  sapSpecMoldUseService.selectSapSpecMoldUseList(sapSpecMoldUse);
    }

    /**
     * 校验规格使用模数唯一性
     */
    @ApiOperation("校验规格使用模数唯一性")
    @PostMapping("/checkSapSpecMoldUseUnique")
    public String checkSapSpecMoldUseUnique(@RequestBody SapSpecMoldUse sapSpecMoldUse){
        return sapSpecMoldUseService.checkSapSpecMoldUseUnique(sapSpecMoldUse);
    }

    /**
     * 根据集合导入规格使用模数数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.sapSpecMoldUse.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入规格使用模数数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<SapSpecMoldUse> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return sapSpecMoldUseService.importData(list, updateSupport, importLogId);
    }

    /**
     * 根据SAP查找规格信息
     */
    @ApiOperation("根据SAP查找规格信息")
    @PostMapping("/getSpecDesc")
    public List<SapSpecMoldUse> getSpecDesc(@RequestBody SapSpecMoldUse sapSpecMoldUse){
        return sapSpecMoldUseService.getSpecDesc(sapSpecMoldUse);
    }



}
