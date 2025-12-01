package com.zlt.mix.setting.controller;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.SettingGlueWorkmanship;
import com.zlt.mix.setting.service.SettingGlueWorkmanshipService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Liam
 * @date 2022-03-18
 */
@Api(tags = {"分厂胶料工艺信息管理接口"})
@RestController
@RequestMapping("/setting/glueWorkmanship")
public class SettingGlueWorkmanshipController extends BaseController {

    @Autowired
    private SettingGlueWorkmanshipService settingGlueWorkmanshipService;

    @PostMapping("/list")
    @ApiOperation("获取分厂胶料工艺信息表格数据")
    public TableDataInfo list(@RequestBody SettingGlueWorkmanship entity) {
        startPage(false);
        entity.setOrderStr(orderStr());
        List<SettingGlueWorkmanship> list = settingGlueWorkmanshipService.selectSettingGlueWorkmanshipList(entity);
        return getDataTable(list);
    }

    @GetMapping("/edit/{id}")
    @ApiOperation("获取分厂胶料工艺详细信息")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "id", dataType = "long", value = "主键id", paramType = "query")
    )
    public SettingGlueWorkmanship getInfo(@PathVariable("id") Long id) {
        return settingGlueWorkmanshipService.getById(id);
    }

    @Log(title = "setting.workmanship.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @PostMapping("/save")
    @ApiOperation("保存分厂胶料工艺信息（id为空则新增，id不为空则修改）")
    public AjaxResult save(@RequestBody SettingGlueWorkmanship entity) {
        settingGlueWorkmanshipService.saveGlueWorkmanship(entity);
        return AjaxResult.success();
    }

    @Log(title = "setting.workmanship.modelName", newBusinessType = BusinessConstant.DELETE)
    @PostMapping("/remove/{ids}")
    @ApiOperation("删除分厂胶料工艺信息")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    )
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        return toAjax(settingGlueWorkmanshipService.deleteByIds(ids));
    }

    @Log(title = "setting.workmanship.modelName", newBusinessType = BusinessConstant.EXPORT)
    @GetMapping("/export")
    @ApiOperation("导出分厂胶料工艺信息")
    public List<SettingGlueWorkmanship> export(@SpringQueryMap SettingGlueWorkmanship entity) {
        startPage(false);
        entity.setOrderStr(orderStr());
        return settingGlueWorkmanshipService.selectSettingGlueWorkmanshipList(entity);
    }


    @Log(title = "setting.workmanship.modelName", newBusinessType = BusinessConstant.IMPORT)
    @PostMapping("/import")
    @ApiOperation("导入分厂胶料工艺信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    public AjaxResult importData(@RequestBody List<SettingGlueWorkmanship> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return settingGlueWorkmanshipService.importData(list, updateSupport, importLogId);
    }
}
