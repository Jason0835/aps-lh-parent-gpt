package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.GlueFinish;
import com.zlt.mix.setting.service.GlueFinishService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 胶料完成量Controller
 *
 * @author Gim
 * @date 2022-03-29
 */
@RestController
@RequestMapping("/glueFinish")
public class GlueFinishController extends BaseController {
    @Resource
    private GlueFinishService glueFinishService;

    /**
     * 查询炼胶时间信息列表
     */
    @ApiOperation("查询炼胶时间信息列表")
    @PostMapping("/list")
    public TableDataInfo listGlueFinish(@RequestBody GlueFinish glueFinish) {
        startPage(false);
        glueFinish.setOrderStr(orderStr());
        List<GlueFinish> list = glueFinishService.selectGlueFinishList(glueFinish);
        return getDataTable(list);
    }

    @ApiOperation("获取炼胶时间信息详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GlueFinish getGlueFinishInfo(@PathVariable("id") Long id) {
        return glueFinishService.getById(id);
    }

    @Log(title = "setting.glueFinish.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出炼胶时间信息列表")
    @PostMapping("/exportData")
    public List<GlueFinish> exportData(@RequestBody GlueFinish glueFinish) {
        startPage(false);
        glueFinish.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return glueFinishService.selectGlueFinishList(glueFinish);
    }

    @ApiOperation("校验炼胶时间信息唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkGlueFinishUnique")
    public String checkGlueFinishUnique(@RequestBody GlueFinish glueFinish) {
        return glueFinishService.checkGlueFinishUnique(glueFinish);
    }

    @Log(title = "setting.glueFinish.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入炼胶时间信息数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<GlueFinish> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return glueFinishService.importData(list, updateSupport, importLogId);
    }
}
