package com.zlt.mix.setting.controller;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.SettingFormulaInfo;
import com.zlt.mix.setting.service.SettingFormulaInfoService;
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
 * @date 2022-03-22
 */
@Api(tags = {"配方信息管理接口"})
@RestController
@RequestMapping("/setting/formulaInfo")
public class SettingFormulaInfoController extends BaseController {

    @Autowired
    private SettingFormulaInfoService settingFormulaInfoService;


    @PostMapping("/list")
    @ApiOperation("查询配方信息表格数据")
    public TableDataInfo list(@RequestBody SettingFormulaInfo entity) {
        startPage(false);
        entity.setOrderStr(orderStr());
        List<SettingFormulaInfo> list = settingFormulaInfoService.selectSettingFormulaInfoList(entity);
        return getDataTable(list);
    }

    @GetMapping("/edit/{id}")
    @ApiOperation("查询配方信息详细信息")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "id", dataType = "long", value = "主键id", paramType = "query")
    )
    public SettingFormulaInfo getInfo(@PathVariable("id") Long id) {
        return settingFormulaInfoService.getById(id);
    }


    @Log(title = "setting.formulaInfo.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @PostMapping("/save")
    @ApiOperation("保存配方信息（id为空则新增，id不为空则修改）")
    public AjaxResult save(@RequestBody SettingFormulaInfo entity) {
        settingFormulaInfoService.saveSettingFormulaInfo(entity);
        return AjaxResult.success();
    }


    @Log(title = "setting.formulaInfo.modelName", newBusinessType = BusinessConstant.DELETE)
    @PostMapping("/remove/{ids}")
    @ApiOperation("批量删除配方信息")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    )
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        return toAjax(settingFormulaInfoService.deleteByIds(ids));
    }

    @Log(title = "setting.formulaInfo.modelName", newBusinessType = BusinessConstant.EXPORT)
    @GetMapping("/export")
    @ApiOperation("导出配方信息")
    public List<SettingFormulaInfo> export(@SpringQueryMap SettingFormulaInfo entity) {
        startPage(false);
        entity.setOrderStr(orderStr());
        return settingFormulaInfoService.selectSettingFormulaInfoList(entity);
    }


    @Log(title = "setting.formulaInfo.modelName", newBusinessType = BusinessConstant.IMPORT)
    @PostMapping("/import")
    @ApiOperation("导入配方信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    public AjaxResult importData(@RequestBody List<SettingFormulaInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return settingFormulaInfoService.importData(list, updateSupport, importLogId);
    }

    @PostMapping("/checkGlueUnique")
    @ApiOperation("判断胶料名称是否已经存在")
    public String checkGlueUnique(@RequestBody SettingFormulaInfo entity) {
        return settingFormulaInfoService.checkGlueUnique(entity);
    }

}
