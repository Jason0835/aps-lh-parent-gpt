package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.lh.api.domain.entity.MixMachineRecipeInfo;


/**
 * 机台和配方对应及下车重量Service接口
 * @author zlt
 * @date 2021-11-09
 */
@FeignClient(contextId = "IMixMachineRecipeInfoService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface IMixMachineRecipeInfoService {

    /**
     * 查询机台和配方对应及下车重量列表
     */
    @ApiOperation("查询机台和配方对应及下车重量列表")
    @PostMapping("/recipe/list")
    TableDataInfo list(@RequestBody MixMachineRecipeInfo mixMachineRecipeInfo);

    /**
    * 新增机台和配方对应及下车重量
    */
    @ApiOperation("新增机台和配方对应及下车重量")
    @PostMapping("/recipe/add")
    AjaxResult add(@RequestBody MixMachineRecipeInfo mixMachineRecipeInfo);

    /**
     * 修改机台和配方对应及下车重量
     */
    @ApiOperation("修改机台和配方对应及下车重量")
    @PostMapping("/recipe/edit")
    AjaxResult edit(@RequestBody MixMachineRecipeInfo mixMachineRecipeInfo);

    /**
     * 删除机台和配方对应及下车重量
     */
    @ApiOperation("删除机台和配方对应及下车重量")
    @DeleteMapping("/recipe/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/recipe/{id}")
    MixMachineRecipeInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验机台和配方对应及下车重量唯一性
     */
    @ApiOperation("校验机台和配方对应及下车重量唯一性")
    @PostMapping("/recipe/checkMixMachineRecipeInfoUnique")
    String checkMixMachineRecipeInfoUnique(@RequestBody MixMachineRecipeInfo mixMachineRecipeInfo);

    /**
     * 导出机台和配方对应及下车重量列表
     */
    @ApiOperation("导出机台和配方对应及下车重量列表")
    @PostMapping("/recipe/getList")
    List<MixMachineRecipeInfo> getList(@RequestBody MixMachineRecipeInfo mixMachineRecipeInfo);

    /**
     * 导入机台和配方对应及下车重量数据
     */
    @ApiOperation("导入机台和配方对应及下车重量")
    @PostMapping("/recipe/importData")
    public AjaxResult importData(@RequestBody List<MixMachineRecipeInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
