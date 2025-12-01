package com.zlt.mix.setting.controller;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.zlt.mix.setting.api.domain.entity.RemindSetting;
import com.zlt.mix.setting.service.RemindSettingService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 提醒设备Controller
 *
 * @author Gim
 * @date 2022-03-23
 */
@RestController
@RequestMapping("/remindSetting")
public class RemindSettingController extends BaseController {
    @Resource
    private RemindSettingService remindSettingService;

    /**
     * 查询提醒设备列表
     */
    @ApiOperation("查询提醒设备列表")
    @PostMapping("/list")
    public TableDataInfo listRemindSetting(@RequestBody RemindSetting remindSetting) {
        startPage(false);
        remindSetting.setOrderStr(orderStr());
        List<RemindSetting> list = remindSettingService.selectRemindSettingList(remindSetting);
        return getDataTable(list);
    }

    @ApiOperation("获取提醒设备详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public RemindSetting getRemindSettingInfo(@PathVariable("id") Long id) {
        return remindSettingService.getById(id);
    }

    @Log(title = "setting.remindSetting.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存提醒设备信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveRemindSetting(@RequestBody RemindSetting remindSetting) {
        remindSettingService.saveRemindSetting(remindSetting);
        return AjaxResult.success();
    }

    @Log(title = "setting.remindSetting.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除提醒设备")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteRemindSetting(@PathVariable Long[] ids) {
        return toAjax(remindSettingService.deleteRemindSettingByIds(ids));
    }

    @Log(title = "setting.remindSetting.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出提醒设备列表")
    @PostMapping("/exportData")
    public List<RemindSetting> exportData(@RequestBody RemindSetting remindSetting) {
        startPage(false);
        remindSetting.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return remindSettingService.selectRemindSettingList(remindSetting);
    }

    @ApiOperation("校验提醒设备唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkRemindSettingUnique")
    public String checkRemindSettingUnique(@RequestBody RemindSetting remindSetting) {
        return remindSettingService.checkRemindSettingUnique(remindSetting);
    }

    @Log(title = "setting.remindSetting.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入提醒设备数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<RemindSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return remindSettingService.importData(list, updateSupport, importLogId);
    }
}
