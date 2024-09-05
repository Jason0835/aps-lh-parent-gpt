package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.MixRecipeInfo;
import com.zlt.aps.lh.service.MixRecipeInfoService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 密炼配方信息Controller
 *
 * @author zlt
 * @date 2021-11-09
 */
@RestController
@RequestMapping("/recipeInfo")
public class MixRecipeInfoController extends BaseController
{
    @Autowired
    private MixRecipeInfoService mixRecipeInfoService;

    /**
     * 查询密炼配方信息列表
     */
    @ApiOperation("查询密炼配方信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MixRecipeInfo mixRecipeInfo)
    {
        startPage();
        mixRecipeInfo.setOrderStr(orderStr());
        List<MixRecipeInfo> list = mixRecipeInfoService.selectMixRecipeInfoList(mixRecipeInfo);
        return getDataTable(list);
    }

    /**
     * 获取密炼配方信息详细信息
     */
    @ApiOperation("获取密炼配方信息详细信息")
    @GetMapping(value = "/{id}")
    public MixRecipeInfo getInfo(@PathVariable("id") Long id){
        return mixRecipeInfoService.selectMixRecipeInfoById(id);
    }

    /**
     * 新增密炼配方信息
     */
    @Log(title = "ui.data.column.recipeInfo.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增密炼配方信息")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MixRecipeInfo mixRecipeInfo){
        return toAjax(mixRecipeInfoService.insertMixRecipeInfo(mixRecipeInfo));
    }

    /**
     * 修改密炼配方信息
     */
    @Log(title = "ui.data.column.recipeInfo.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改密炼配方信息")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MixRecipeInfo mixRecipeInfo){
        return toAjax(mixRecipeInfoService.updateMixRecipeInfo(mixRecipeInfo));
    }

    /**
     * 删除密炼配方信息
     */
    @Log(title = "ui.data.column.recipeInfo.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除密炼配方信息")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(mixRecipeInfoService.deleteMixRecipeInfoByIds(ids));
    }

    /**
     * 导出密炼配方信息列表
     */
    @Log(title = "ui.data.column.recipeInfo.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出密炼配方信息列表")
    @PostMapping("/getList")
    public List<MixRecipeInfo> getList(@RequestBody MixRecipeInfo mixRecipeInfo){
        startPage();
        mixRecipeInfo.setOrderStr(orderStr());
        return  mixRecipeInfoService.selectMixRecipeInfoList(mixRecipeInfo);
    }

    /**
     * 校验密炼配方信息唯一性
     */
    @ApiOperation("校验密炼配方信息唯一性")
    @PostMapping("/checkMixRecipeInfoUnique")
    public String checkMixRecipeInfoUnique(@RequestBody MixRecipeInfo mixRecipeInfo){
        return mixRecipeInfoService.checkMixRecipeInfoUnique(mixRecipeInfo);
    }

    /**
     * 根据集合导入密炼配方信息数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.recipeInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入密炼配方信息数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MixRecipeInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mixRecipeInfoService.importData(list, updateSupport, importLogId);
    }
}
