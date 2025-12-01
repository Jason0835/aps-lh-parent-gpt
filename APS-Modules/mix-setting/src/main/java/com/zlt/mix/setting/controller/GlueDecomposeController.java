package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.GlueDecompose;
import com.zlt.mix.setting.service.GlueDecomposeService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 终炼母炼分解Controller
 *
 * @author Liam
 * @date 2022-03-28
 */
@RestController
@RequestMapping("/decompose")
public class GlueDecomposeController extends BaseController {
    @Resource
    private GlueDecomposeService glueDecomposeService;

    /**
     * 查询终炼母炼分解列表
     */
    @ApiOperation("查询终炼母炼分解列表")
    @PostMapping("/list")
    public TableDataInfo listGlueDecompose(@RequestBody GlueDecompose glueDecompose) {
        startPage(false);
        glueDecompose.setOrderStr(orderStr());
        List<GlueDecompose> list = glueDecomposeService.selectGlueDecomposeList(glueDecompose);
        return getDataTable(list);
    }

    @ApiOperation("获取终炼母炼分解详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GlueDecompose getGlueDecomposeInfo(@PathVariable("id") Long id) {
        return glueDecomposeService.getById(id);
    }

    @Log(title = "setting.decompose.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存终炼母炼分解信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveGlueDecompose(@RequestBody GlueDecompose glueDecompose) {
        glueDecomposeService.saveGlueDecompose(glueDecompose);
        return AjaxResult.success();
    }

    @Log(title = "setting.decompose.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除终炼母炼分解")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueDecompose(@PathVariable Long[] ids) {
        return toAjax(glueDecomposeService.deleteGlueDecomposeByIds(ids));
    }

    @Log(title = "setting.decompose.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出终炼母炼分解列表")
    @PostMapping("/exportData")
    public List<GlueDecompose> exportData(@RequestBody GlueDecompose glueDecompose) {
        startPage(false);
        glueDecompose.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return glueDecomposeService.selectGlueDecomposeList(glueDecompose);
    }

    @ApiOperation("校验终炼母炼分解唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkGlueDecomposeUnique")
    public String checkGlueDecomposeUnique(@RequestBody GlueDecompose glueDecompose) {
        return glueDecomposeService.checkGlueDecomposeUnique(glueDecompose);
    }

    @Log(title = "setting.decompose.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入终炼母炼分解数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<GlueDecompose> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return glueDecomposeService.importData(list, updateSupport, importLogId);
    }
}
