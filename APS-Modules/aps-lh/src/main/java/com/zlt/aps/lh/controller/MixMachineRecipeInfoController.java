package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.MixMachineRecipeInfo;
import com.zlt.aps.lh.service.MixMachineRecipeInfoService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 机台和配方对应及下车重量Controller
 *
 * @author zlt
 * @date 2021-11-09
 */
@RestController
@RequestMapping("/recipe")
public class MixMachineRecipeInfoController extends BaseController
{
    @Autowired
    private MixMachineRecipeInfoService mixMachineRecipeInfoService;

    /**
     * 查询机台和配方对应及下车重量列表
     */
    @ApiOperation("查询机台和配方对应及下车重量列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MixMachineRecipeInfo mixMachineRecipeInfo)
    {
        startPage();
        mixMachineRecipeInfo.setOrderStr(orderStr());
        List<MixMachineRecipeInfo> list = mixMachineRecipeInfoService.selectMixMachineRecipeInfoList(mixMachineRecipeInfo);
        return getDataTable(list);
    }

    /**
     * 获取机台和配方对应及下车重量详细信息
     */
    @ApiOperation("获取机台和配方对应及下车重量详细信息")
    @GetMapping(value = "/{id}")
    public MixMachineRecipeInfo getInfo(@PathVariable("id") Long id){
        return mixMachineRecipeInfoService.selectMixMachineRecipeInfoById(id);
    }

    /**
     * 新增机台和配方对应及下车重量
     */
    @Log(title = "ui.data.column.recipe.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增机台和配方对应及下车重量")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MixMachineRecipeInfo mixMachineRecipeInfo){
        return toAjax(mixMachineRecipeInfoService.insertMixMachineRecipeInfo(mixMachineRecipeInfo));
    }

    /**
     * 修改机台和配方对应及下车重量
     */
    @Log(title = "ui.data.column.recipe.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改机台和配方对应及下车重量")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MixMachineRecipeInfo mixMachineRecipeInfo){
        return toAjax(mixMachineRecipeInfoService.updateMixMachineRecipeInfo(mixMachineRecipeInfo));
    }

    /**
     * 删除机台和配方对应及下车重量
     */
    @Log(title = "ui.data.column.recipe.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除机台和配方对应及下车重量")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(mixMachineRecipeInfoService.deleteMixMachineRecipeInfoByIds(ids));
    }

    /**
     * 导出机台和配方对应及下车重量列表
     */
    @Log(title = "ui.data.column.recipe.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出机台和配方对应及下车重量列表")
    @PostMapping("/getList")
    public List<MixMachineRecipeInfo> getList(@RequestBody MixMachineRecipeInfo mixMachineRecipeInfo){
        startPage();
        mixMachineRecipeInfo.setOrderStr(orderStr());
        return  mixMachineRecipeInfoService.selectMixMachineRecipeInfoList(mixMachineRecipeInfo);
    }

    /**
     * 校验机台和配方对应及下车重量唯一性
     */
    @ApiOperation("校验机台和配方对应及下车重量唯一性")
    @PostMapping("/checkMixMachineRecipeInfoUnique")
    public String checkMixMachineRecipeInfoUnique(@RequestBody MixMachineRecipeInfo mixMachineRecipeInfo){
        return mixMachineRecipeInfoService.checkMixMachineRecipeInfoUnique(mixMachineRecipeInfo);
    }

    /**
     * 根据集合导入机台和配方对应及下车重量数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.recipe.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入机台和配方对应及下车重量数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MixMachineRecipeInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mixMachineRecipeInfoService.importData(list, updateSupport, importLogId);
    }
}
