package com.zlt.mix.controller.setting;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.setting.api.domain.entity.SettingScheduleParams;
import com.zlt.mix.setting.api.service.ISettingScheduleParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * @author Liam
 * @date 2022-03-14
 */
@Controller
@RequestMapping("/setting/scheduleParams" )
@Api(tags = {"密炼参数信息接口"})
public class SettingScheduleParamsController extends BaseController {
    private final String prefix = "setting/scheduleParams";

    @Autowired
    private ISettingScheduleParamsService iSettingScheduleParamsService;

    @Autowired
    private IExportLogService iExportLogService;

    @RequiresPermissions("setting:scheduleParams:view" )
    @GetMapping()
    public String toIndex() {
        return prefix + "/params";
    }

    @RequiresPermissions("setting:scheduleParams:list" )
    @PostMapping("/list" )
    @ResponseBody
    @ApiOperation("查询密炼参数信息列表" )
    public TableDataInfo list(SettingScheduleParams settingScheduleParams) {
        return iSettingScheduleParamsService.list(settingScheduleParams);
    }

    @PostMapping("/find" )
    @ResponseBody
    @ApiOperation("查询指定条件的密炼参数信息列表" )
    public AjaxResult find(SettingScheduleParams settingScheduleParams) {
        return iSettingScheduleParamsService.find(settingScheduleParams);
    }

    @GetMapping("/{id}" )
    @ApiOperation("获取密炼参数信息详细信息，进行修改" )
    public String getInfo(@PathVariable("id" ) Long id, ModelMap modelMap) {
        modelMap.put("settingScheduleParams", iSettingScheduleParamsService.getInfo(id));
        modelMap.put("editType", "0" );
        return prefix + "/edit";
    }

    @GetMapping("/toCopy/{id}" )
    @ApiOperation("获取密炼参数信息详细信息，进行复制" )
    public String toCopy(@PathVariable("id" ) Long id, ModelMap modelMap) {
        modelMap.put("settingScheduleParams", iSettingScheduleParamsService.getInfo(id));
        modelMap.put("editType", "1" );
        return prefix + "/edit";
    }

    @RequiresPermissions("setting:scheduleParams:edit" )
    @PostMapping("/edit" )
    @ResponseBody
    @ApiOperation("修改密炼参数信息" )
    public AjaxResult edit(@Validated SettingScheduleParams settingScheduleParams) {
        return iSettingScheduleParamsService.edit(settingScheduleParams);
    }

    @RequiresPermissions("setting:scheduleParams:copy" )
    @PostMapping("/copy" )
    @ResponseBody
    @ApiOperation("复制密炼参数信息" )
    public AjaxResult copy(@Validated SettingScheduleParams settingScheduleParams) {
        return iSettingScheduleParamsService.copy(settingScheduleParams);
    }

    @RequiresPermissions("setting:scheduleParams:export" )
    @GetMapping("/export" )
    @ResponseBody
    @ApiOperation("导出密炼参数信息" )
    public void export(HttpServletResponse response, SettingScheduleParams settingScheduleParams) throws IOException {
        List<SettingScheduleParams> list = iSettingScheduleParamsService.exportData(settingScheduleParams);

        //仅在导出做处理,避免污染数据,但是增加了O(N)的时间复杂度
        for (SettingScheduleParams i : list) {
            if ("0".equals(i.getMixArea())) {
                i.setMixArea(I18nUtil.getMessage("ui.data.column.default" ));
            }
        }

        ExcelUtil<SettingScheduleParams> excelUtil = new ExcelUtil<>(SettingScheduleParams.class);
        String fileName = I18nUtil.getMessage("ui.data.column.schedule.params.modelName" );
        Workbook workbook = excelUtil.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, settingScheduleParams.toString(), ZltConstant.PROCEDURE_CODE_SETTING);
        iExportLogService.add(exportLog);

    }
}
