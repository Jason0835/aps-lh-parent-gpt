package com.zlt.mix.setting.controller;

import java.util.List;

import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import javax.annotation.Resource;

import com.zlt.mix.setting.api.domain.entity.GlueLossSetting;
import com.zlt.mix.setting.service.GlueLossSettingService;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 胶料损耗率设定Controller
 *
 * @author Joran.zhang
 * @date 2022-05-23
 */
@RestController
@RequestMapping("/glueLossSetting")
public class GlueLossSettingController extends BaseController {
    @Resource
    private GlueLossSettingService glueLossSettingService;

    /**
     * 查询胶料损耗率设定列表
     */
    @ApiOperation("查询胶料损耗率设定列表")
    @PostMapping("/list")
    public TableDataInfo listGlueLossSetting(@RequestBody GlueLossSetting glueLossSetting) {
        startPage(false);
        glueLossSetting.setOrderStr(orderStr());
        List<GlueLossSetting> list = glueLossSettingService.selectGlueLossSettingList(glueLossSetting);
        return getDataTable(list);
    }

    @ApiOperation("获取胶料损耗率设定详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GlueLossSetting getGlueLossSettingInfo(@PathVariable("id") Long id){
        return glueLossSettingService.getById(id);
    }

    @Log(title = "setting.setting.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存胶料损耗率设定信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveGlueLossSetting(@RequestBody GlueLossSetting glueLossSetting) {
        glueLossSettingService.saveGlueLossSetting(glueLossSetting);
        return AjaxResult.success();
    }

    @Log(title = "setting.setting.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除胶料损耗率设定")
	@PostMapping("/delete/{ids}")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGlueLossSetting(@PathVariable Long[] ids){
        return toAjax(glueLossSettingService.deleteGlueLossSettingByIds(ids));
    }

    @Log(title = "setting.setting.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出胶料损耗率设定列表")
    @PostMapping("/exportData")
    public List<GlueLossSetting> exportData(@RequestBody GlueLossSetting glueLossSetting){
        startPage(false);
        glueLossSetting.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return  glueLossSettingService.selectGlueLossSettingList(glueLossSetting);
    }

    @ApiOperation("校验胶料损耗率设定唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkGlueLossSettingUnique")
    public String checkGlueLossSettingUnique(@RequestBody GlueLossSetting glueLossSetting){
        return glueLossSettingService.checkGlueLossSettingUnique(glueLossSetting);
    }

    @Log(title = "setting.setting.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入胶料损耗率设定数据")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
        @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
        @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<GlueLossSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return glueLossSettingService.importData(list, updateSupport, importLogId);
    }
}
