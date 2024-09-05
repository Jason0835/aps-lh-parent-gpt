package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.lh.api.domain.entity.MixRecipeInfo;


/**
 * 密炼配方信息Service接口
 * @author zlt
 * @date 2021-11-09
 */
@FeignClient(contextId = "IMixRecipeInfoService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface IMixRecipeInfoService {

    /**
     * 查询密炼配方信息列表
     */
    @ApiOperation("查询密炼配方信息列表")
    @PostMapping("/recipeInfo/list")
    TableDataInfo list(@RequestBody MixRecipeInfo mixRecipeInfo);

    /**
    * 新增密炼配方信息
    */
    @ApiOperation("新增密炼配方信息")
    @PostMapping("/recipeInfo/add")
    AjaxResult add(@RequestBody MixRecipeInfo mixRecipeInfo);

    /**
     * 修改密炼配方信息
     */
    @ApiOperation("修改密炼配方信息")
    @PostMapping("/recipeInfo/edit")
    AjaxResult edit(@RequestBody MixRecipeInfo mixRecipeInfo);

    /**
     * 删除密炼配方信息
     */
    @ApiOperation("删除密炼配方信息")
    @DeleteMapping("/recipeInfo/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/recipeInfo/{id}")
    MixRecipeInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验密炼配方信息唯一性
     */
    @ApiOperation("校验密炼配方信息唯一性")
    @PostMapping("/recipeInfo/checkMixRecipeInfoUnique")
    String checkMixRecipeInfoUnique(@RequestBody MixRecipeInfo mixRecipeInfo);

    /**
     * 导出密炼配方信息列表
     */
    @ApiOperation("导出密炼配方信息列表")
    @PostMapping("/recipeInfo/getList")
    List<MixRecipeInfo> getList(@RequestBody MixRecipeInfo mixRecipeInfo);

    /**
     * 导入密炼配方信息数据
     */
    @ApiOperation("导入密炼配方信息")
    @PostMapping("/recipeInfo/importData")
    public AjaxResult importData(@RequestBody List<MixRecipeInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
