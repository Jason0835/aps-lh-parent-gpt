package com.zlt.mix.setting.controller;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipeWeight;
import com.zlt.mix.setting.service.MesPmtRecipeWeightService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.common.core.utils.ExcelUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 配方称量明细Controller
 *
 * @author chen
 * @date 2022-06-01
 */
@RestController
@RequestMapping("/MesPmtRecipeWeight")
public class MesPmtRecipeWeightController extends BaseController {
    @Resource
    private MesPmtRecipeWeightService mesPmtRecipeWeightService;

    /**
     * 查询配方称量明细列表
     */
    @ApiOperation("查询配方称量明细列表")
    @PostMapping("/list")
    public TableDataInfo listMesPmtRecipeWeight(@RequestBody MesPmtRecipeWeight mesPmtRecipeWeight) {
        startPage(false);
        mesPmtRecipeWeight.setOrderStr(orderStr());
        List<MesPmtRecipeWeight> list = mesPmtRecipeWeightService.selectMesPmtRecipeWeightList(mesPmtRecipeWeight);
        return getDataTable(list);
    }

    @ApiOperation("获取配方称量明细详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public MesPmtRecipeWeight getMesPmtRecipeWeightInfo(@PathVariable("id") Long id){
        return mesPmtRecipeWeightService.getById(id);
    }

    @Log(title = "setting.MesPmtRecipeWeight.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出配方称量明细列表")
    @PostMapping("/exportData")
    public List<MesPmtRecipeWeight> exportData(@RequestBody MesPmtRecipeWeight mesPmtRecipeWeight){
        startPage(false);
        mesPmtRecipeWeight.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return  mesPmtRecipeWeightService.selectMesPmtRecipeWeightList(mesPmtRecipeWeight);
    }
}
