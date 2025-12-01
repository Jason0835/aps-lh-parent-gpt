package com.zlt.mix.setting.controller;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.GlueCommonDemand;
import com.zlt.mix.setting.service.GlueCommonDemandService;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * 密炼机常用大规格设置Controller
 *
 * @author zlt
 * @date 2023-02-05
 */
@RestController
@RequestMapping("/glueCommonDemand")
public class GlueCommonDemandController extends BaseController {
    @Resource
    private GlueCommonDemandService glueCommonDemandService;

    /**
     * 查询密炼机常用大规格设置列表
     */
    @ApiOperation("查询密炼机常用大规格设置列表")
    @PostMapping("/list")
    public TableDataInfo listGlueCommonDemand(@RequestBody GlueCommonDemand glueCommonDemand) {
        startPage(false);
        glueCommonDemand.setOrderStr(orderStr());
        List<GlueCommonDemand> list = glueCommonDemandService.selectGlueCommonDemandList(glueCommonDemand);
        return getDataTable(list);
    }

    @ApiOperation("获取密炼机常用大规格设置详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GlueCommonDemand getGlueCommonDemandInfo(@PathVariable("id") Long id){
        return glueCommonDemandService.getById(id);
    }

    @Log(title = "setting.glueCommonDemand.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存密炼机常用大规格设置信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveGlueCommonDemand(@RequestBody GlueCommonDemand glueCommonDemand) {
        glueCommonDemandService.saveGlueCommonDemand(glueCommonDemand);
        return AjaxResult.success();
    }

    @Log(title = "setting.glueCommonDemand.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除密炼机常用大规格设置")
	@PostMapping("/delete/{ids}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueCommonDemand(@PathVariable Long[] ids){
        return toAjax(glueCommonDemandService.deleteGlueCommonDemandByIds(ids));
    }

    @Log(title = "setting.glueCommonDemand.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出密炼机常用大规格设置列表")
    @PostMapping("/exportData")
    public List<GlueCommonDemand> exportData(@RequestBody GlueCommonDemand glueCommonDemand){
        startPage(false);
        glueCommonDemand.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return  glueCommonDemandService.selectGlueCommonDemandList(glueCommonDemand);
    }

    @ApiOperation("校验密炼机常用大规格设置唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkGlueCommonDemandUnique")
    public String checkGlueCommonDemandUnique(@RequestBody GlueCommonDemand glueCommonDemand){
        return glueCommonDemandService.checkGlueCommonDemandUnique(glueCommonDemand);
    }

    @Log(title = "setting.glueCommonDemand.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入密炼机常用大规格设置数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
        @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
        @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<GlueCommonDemand> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return glueCommonDemandService.importData(list, updateSupport, importLogId);
    }
}
