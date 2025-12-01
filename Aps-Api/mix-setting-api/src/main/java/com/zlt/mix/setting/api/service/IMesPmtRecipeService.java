package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipe;
import com.zlt.mix.setting.api.domain.vo.MesPmtRecipeTemplateVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方信息Service接口
 *
 * @author chen
 * @date 2022-06-01
 */
@FeignClient(contextId = "IMesPmtRecipeService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IMesPmtRecipeService {

    /**
     * 查询配方信息列表
     */
    @PostMapping("/MesPmtRecipe/list")
    TableDataInfo listMesPmtRecipe(@RequestBody MesPmtRecipe mesPmtRecipe);

    /**
     * 导出配方信息列表
     */
    @PostMapping("/MesPmtRecipe/exportData")
    List<MesPmtRecipe> exportData(@RequestBody MesPmtRecipe mesPmtRecipe);

    /**
     * 根据机台名称和胶料名称查询配方信息
     *
     * @param mesPmtRecipe 机台名称和胶料名称
     * @return 配方集合
     */
    @ApiOperation("根据机台名称和胶料名称查询配方信息")
    @PostMapping("/MesPmtRecipe/selectMesPmtRecipeByParams")
    public TableDataInfo selectMesPmtRecipeByParams(@RequestBody MesPmtRecipe mesPmtRecipe);

    /**
     * 根据密炼区、胶料名称，查询对应配方的机台信息
     *
     * @param mesPmtRecipe 密炼区、胶料名称
     * @return 对应配方的机台信息
     */
    @ApiOperation("根据密炼区、胶料名称，查询对应配方的机台信息")
    @PostMapping("/MesPmtRecipe/selectMesPmtRecipeMachine")
    public ArrayList<MesPmtRecipe> selectMesPmtRecipeMachine(@RequestBody MesPmtRecipe mesPmtRecipe);

    /**
     * 导入配方信息
     */
    @ApiOperation("导入配方信息")
    @PostMapping("/MesPmtRecipe/importData")
    AjaxResult importData(@RequestBody List<MesPmtRecipeTemplateVo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long id);
}
