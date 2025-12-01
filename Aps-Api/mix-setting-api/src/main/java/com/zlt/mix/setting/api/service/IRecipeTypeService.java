package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.RecipeType;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 配方类型Service接口
 * @author Joran.zhang
 * @date 2022-05-31
 */
@FeignClient(contextId = "IRecipeTypeService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IRecipeTypeService {

    /**
     * 查询配方类型列表
     */
    @PostMapping("/type/list")
    TableDataInfo listRecipeType(@RequestBody RecipeType recipeType);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/type/{id}")
    RecipeType getRecipeTypeInfo(@PathVariable("id") Long id);

    /**
    * 保存配方类型信息（id为空则新增，id不为空则修改）
    */
    @PostMapping("/type/save")
    AjaxResult saveRecipeType(@RequestBody RecipeType recipeType);

    /**
     * 批量删除配方类型
     */
    @PostMapping("/type/delete/{ids}")
    AjaxResult deleteRecipeType(@PathVariable("ids") Long[] ids);

    /**
     * 校验配方类型唯一性
     */
    @ApiOperation("校验配方类型唯一性")
    @PostMapping("/type/checkRecipeTypeUnique")
    String checkRecipeTypeUnique(@RequestBody RecipeType recipeType);

    /**
     * 导出配方类型列表
     */
    @PostMapping("/type/exportData")
    List<RecipeType> exportData(@RequestBody RecipeType recipeType);

    /**
     * 导入配方类型数据
     */
    @ApiOperation("导入配方类型")
    @PostMapping("/type/importData")
    public AjaxResult importData(@RequestBody List<RecipeType> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
