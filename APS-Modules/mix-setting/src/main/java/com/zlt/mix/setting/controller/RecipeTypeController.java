package com.zlt.mix.setting.controller;

import java.util.List;

import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import javax.annotation.Resource;

import com.zlt.mix.setting.api.domain.entity.RecipeType;
import com.zlt.mix.setting.service.RecipeTypeService;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 配方类型Controller
 *
 * @author Joran.zhang
 * @date 2022-05-31
 */
@RestController
@RequestMapping("/type")
public class RecipeTypeController extends BaseController {
    @Resource
    private RecipeTypeService recipeTypeService;

    /**
     * 查询配方类型列表
     */
    @ApiOperation("查询配方类型列表")
    @PostMapping("/list")
    public TableDataInfo listRecipeType(@RequestBody RecipeType recipeType) {
        startPage(false);
        recipeType.setOrderStr(orderStr());
        List<RecipeType> list = recipeTypeService.selectRecipeTypeList(recipeType);
        return getDataTable(list);
    }

    @ApiOperation("获取配方类型详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public RecipeType getRecipeTypeInfo(@PathVariable("id") Long id){
        return recipeTypeService.getById(id);
    }

    @Log(title = "setting.type.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存配方类型信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveRecipeType(@RequestBody RecipeType recipeType) {
        recipeTypeService.saveRecipeType(recipeType);
        return AjaxResult.success();
    }

    @Log(title = "setting.type.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除配方类型")
	@PostMapping("/delete/{ids}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteRecipeType(@PathVariable Long[] ids){
        return toAjax(recipeTypeService.deleteRecipeTypeByIds(ids));
    }

    @Log(title = "setting.type.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出配方类型列表")
    @PostMapping("/exportData")
    public List<RecipeType> exportData(@RequestBody RecipeType recipeType){
        startPage(false);
        recipeType.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return  recipeTypeService.selectRecipeTypeList(recipeType);
    }

    @ApiOperation("校验配方类型唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkRecipeTypeUnique")
    public String checkRecipeTypeUnique(@RequestBody RecipeType recipeType){
        return recipeTypeService.checkRecipeTypeUnique(recipeType);
    }

    @Log(title = "setting.type.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入配方类型数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
        @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
        @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<RecipeType> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return recipeTypeService.importData(list, updateSupport, importLogId);
    }
}
