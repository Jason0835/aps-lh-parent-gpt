package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipe;
import com.zlt.mix.setting.api.domain.vo.MesPmtRecipeTemplateVo;
import com.zlt.mix.setting.service.MesPmtRecipeService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 配方信息Controller
 *
 * @author chen
 * @date 2022-06-01
 */
@RestController
@RequestMapping("/MesPmtRecipe")
public class MesPmtRecipeController extends BaseController {
    @Resource
    private MesPmtRecipeService mesPmtRecipeService;

    /**
     * 查询配方信息列表
     */
    @ApiOperation("查询配方信息列表")
    @PostMapping("/list")
    public TableDataInfo listMesPmtRecipe(@RequestBody MesPmtRecipe mesPmtRecipe) {
        startPage(false);
        mesPmtRecipe.setOrderStr(orderStr());
        List<MesPmtRecipe> list = mesPmtRecipeService.selectMesPmtRecipeList(mesPmtRecipe);
        return getDataTable(list);
    }

    @ApiOperation("获取配方信息详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public MesPmtRecipe getMesPmtRecipeInfo(@PathVariable("id") Long id) {
        return mesPmtRecipeService.getById(id);
    }

    /**
     * 根据机台名称和胶料名称查询配方信息
     *
     * @param mesPmtRecipe 机台名称和胶料名称
     * @return 配方集合
     */
    @ApiOperation("根据机台名称和胶料名称查询配方信息")
    @PostMapping("/selectMesPmtRecipeByParams")
    public TableDataInfo selectMesPmtRecipeByParams(@RequestBody MesPmtRecipe mesPmtRecipe) {
        startPage(false);
        mesPmtRecipe.setOrderStr(orderStr());
        List<MesPmtRecipe> list = mesPmtRecipeService.selectMesPmtRecipeByParams(mesPmtRecipe);
        return getDataTable(list);
    }

    /**
     * 根据密炼区、胶料名称，查询对应配方的机台信息
     *
     * @param mesPmtRecipe 密炼区、胶料名称
     * @return 对应配方的机台信息
     */
    @ApiOperation("根据密炼区、胶料名称，查询对应配方的机台信息")
    @PostMapping("/selectMesPmtRecipeMachine")
    public ArrayList<MesPmtRecipe> selectMesPmtRecipeMachine(@RequestBody MesPmtRecipe mesPmtRecipe) {
        return mesPmtRecipeService.selectMesPmtRecipeMachine(mesPmtRecipe);
    }

    @Log(title = "setting.MesPmtRecipe.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入物料数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MesPmtRecipeTemplateVo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return mesPmtRecipeService.importData(list, updateSupport, importLogId);
    }
}
