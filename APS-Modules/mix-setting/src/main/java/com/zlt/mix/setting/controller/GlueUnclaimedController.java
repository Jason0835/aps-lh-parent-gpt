package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.GlueUnclaimed;
import com.zlt.mix.setting.service.GlueUnclaimedService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 胶料白班待支领Controller
 *
 * @author zlt
 * @date 2022-09-05
 */
@RestController
@RequestMapping("/unclaimed")
public class GlueUnclaimedController extends BaseController {
    @Resource
    private GlueUnclaimedService glueUnclaimedService;

    /**
     * 查询胶料白班待支领列表
     */
    @ApiOperation("查询胶料白班待支领列表")
    @PostMapping("/list")
    public TableDataInfo listGlueUnclaimed(@RequestBody GlueUnclaimed glueUnclaimed) {
        startPage(false);
        glueUnclaimed.setOrderStr(orderStr());
        List<GlueUnclaimed> list = glueUnclaimedService.selectGlueUnclaimedList(glueUnclaimed);
        return getDataTable(list);
    }

    @ApiOperation("获取胶料白班待支领详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GlueUnclaimed getGlueUnclaimedInfo(@PathVariable("id") Long id){
        return glueUnclaimedService.getById(id);
    }

    @Log(title = "setting.unclaimed.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存胶料白班待支领信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveGlueUnclaimed(@RequestBody GlueUnclaimed glueUnclaimed) {
        glueUnclaimedService.saveGlueUnclaimed(glueUnclaimed);
        return AjaxResult.success();
    }

    @Log(title = "setting.unclaimed.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除胶料白班待支领")
	@PostMapping("/delete/{ids}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueUnclaimed(@PathVariable Long[] ids){
        return toAjax(glueUnclaimedService.deleteGlueUnclaimedByIds(ids));
    }

    @Log(title = "setting.unclaimed.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出胶料白班待支领列表")
    @PostMapping("/exportData")
    public List<GlueUnclaimed> exportData(@RequestBody GlueUnclaimed glueUnclaimed){
        startPage(false);
        glueUnclaimed.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return  glueUnclaimedService.selectGlueUnclaimedList(glueUnclaimed);
    }

    @ApiOperation("校验胶料白班待支领唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkGlueUnclaimedUnique")
    public String checkGlueUnclaimedUnique(@RequestBody GlueUnclaimed glueUnclaimed){
        return glueUnclaimedService.checkGlueUnclaimedUnique(glueUnclaimed);
    }

    @Log(title = "setting.unclaimed.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入胶料白班待支领数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
        @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
        @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<GlueUnclaimed> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return glueUnclaimedService.importData(list, updateSupport, importLogId);
    }
}
