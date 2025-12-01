package com.zlt.mix.setting.controller;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.zlt.mix.setting.api.domain.entity.GlueSpanSetting;
import com.zlt.mix.setting.service.GlueSpanSettingService;
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
 * 终炼母炼胶料跨区设置Controller
 *
 * @author chen
 * @date 2022-08-12
 */
@RestController
@RequestMapping("/glueSpanSetting")
public class GlueSpanSettingController extends BaseController {
    @Resource
    private GlueSpanSettingService glueSpanSettingService;

    /**
     * 查询终炼母炼胶料跨区设置列表
     */
    @ApiOperation("查询终炼母炼胶料跨区设置列表")
    @PostMapping("/list")
    public TableDataInfo listGlueSpanSetting(@RequestBody GlueSpanSetting glueSpanSetting) {
        startPage(false);
        glueSpanSetting.setOrderStr(orderStr());
        List<GlueSpanSetting> list = glueSpanSettingService.selectGlueSpanSettingList(glueSpanSetting);
        return getDataTable(list);
    }

    @ApiOperation("获取终炼母炼胶料跨区设置详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GlueSpanSetting getGlueSpanSettingInfo(@PathVariable("id") Long id){
        return glueSpanSettingService.getById(id);
    }

    @Log(title = "setting.glueSpanSetting.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存终炼母炼胶料跨区设置信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveGlueSpanSetting(@RequestBody GlueSpanSetting glueSpanSetting) {
        glueSpanSettingService.saveGlueSpanSetting(glueSpanSetting);
        return AjaxResult.success();
    }

    @Log(title = "setting.glueSpanSetting.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除终炼母炼胶料跨区设置")
	@PostMapping("/delete/{ids}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueSpanSetting(@PathVariable Long[] ids){
        return toAjax(glueSpanSettingService.deleteGlueSpanSettingByIds(ids));
    }

    @Log(title = "setting.glueSpanSetting.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出终炼母炼胶料跨区设置列表")
    @PostMapping("/exportData")
    public List<GlueSpanSetting> exportData(@RequestBody GlueSpanSetting glueSpanSetting){
        startPage(false);
        glueSpanSetting.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return  glueSpanSettingService.selectGlueSpanSettingList(glueSpanSetting);
    }

    @ApiOperation("校验终炼母炼胶料跨区设置唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkGlueSpanSettingUnique")
    public String checkGlueSpanSettingUnique(@RequestBody GlueSpanSetting glueSpanSetting){
        return glueSpanSettingService.checkGlueSpanSettingUnique(glueSpanSetting);
    }

    @Log(title = "setting.glueSpanSetting.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入终炼母炼胶料跨区设置数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
        @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
        @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<GlueSpanSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return glueSpanSettingService.importData(list, updateSupport, importLogId);
    }
}
