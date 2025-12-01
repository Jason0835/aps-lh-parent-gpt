package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.SettingScheduleParams;
import com.zlt.mix.setting.service.SettingScheduleParamsService;
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
 * @date 2022-03-14
 */
@RestController
@RequestMapping("/setting/scheduleParams")
@Api(tags = {"密炼参数信息维护接口"})
public class SettingScheduleParamsController extends BaseController {

    @Autowired
    private SettingScheduleParamsService settingScheduleParamsService;

    @PostMapping("/list" )
    @ApiOperation("查询密炼参数信息列表" )
    public TableDataInfo list(@RequestBody SettingScheduleParams settingScheduleParams) {
        startPage(false);
        settingScheduleParams.setOrderStr(orderStr());
        List<SettingScheduleParams> list = settingScheduleParamsService.selectParamsList(settingScheduleParams);
        return getDataTable(list);
    }
    
    @PostMapping("/find" )
    @ApiOperation("查询密炼参数信息列表" )
    public AjaxResult find(@RequestBody SettingScheduleParams settingScheduleParams) {
        return settingScheduleParamsService.selectParamsListMixArea(settingScheduleParams);
    }

    @GetMapping("/{id}" )
    @ApiOperation("获取密炼参数信息详细信息,跳转到编辑界面" )
    @ApiImplicitParams(
            @ApiImplicitParam(name = "id", dataType = "long", value = "主键id", paramType = "query" )
    )
    public SettingScheduleParams getInfo(@PathVariable("id" ) String id) {
        return settingScheduleParamsService.selectParamsById(Long.parseLong(id));
    }

    @Log(title = "ui.data.column.schedule.params.modelName", newBusinessType = BusinessConstant.UPDATE)
    @PostMapping("/edit" )
    @ApiOperation("修改密炼参数信息" )
    public AjaxResult edit(@RequestBody SettingScheduleParams settingScheduleParams) {
        return settingScheduleParamsService.updateParams(settingScheduleParams);
    }

    @Log(title = "ui.data.column.schedule.params.modelName", newBusinessType = BusinessConstant.INSERT)
    @PostMapping("/copy" )
    @ApiOperation("复制密炼参数信息" )
    public AjaxResult copy(@RequestBody SettingScheduleParams settingScheduleParams) {
        return settingScheduleParamsService.copyScheduleParams(settingScheduleParams);
    }

    @Log(title = "ui.data.column.schedule.params.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出密炼参数信息" )
    @GetMapping("/exportData" )
    public List<SettingScheduleParams> exportData(@SpringQueryMap SettingScheduleParams settingScheduleParams) {
        startPage(false);
        settingScheduleParams.setOrderStr(orderStr());
        return settingScheduleParamsService.selectParamsList(settingScheduleParams);
    }

}
