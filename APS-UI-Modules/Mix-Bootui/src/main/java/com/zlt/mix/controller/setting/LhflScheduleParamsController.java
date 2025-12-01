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
import com.zlt.mix.setting.api.domain.entity.LhflScheduleParams;
import com.zlt.mix.setting.api.service.ILhflScheduleParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 排程参数（硫磺辅料排程设置）Controller
 *
 * @author Liam
 * @date 2022-04-06
 */
@Api(tags = "排程参数（硫磺辅料排程设置）")
@Controller
@RequestMapping("/setting/lhflScheduleParams")
public class LhflScheduleParamsController extends BaseController {

    @Resource
    private ILhflScheduleParamsService iLhflScheduleParamsService;
    @Resource
    private IExportLogService iExportLogService;

    private final String prefix = "setting/lhflScheduleParams";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:lhflScheduleParams:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/lhflScheduleParams";
    }

    @ApiOperation("根据条件查询排程参数（硫磺辅料排程设置）列表")
    @RequiresPermissions("setting:lhflScheduleParams:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listLhflScheduleParams(LhflScheduleParams entity) {
        return iLhflScheduleParamsService.listLhflScheduleParams(entity);
    }

    @ApiOperation("跳转至修改页面，进行修改")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhflScheduleParams", iLhflScheduleParamsService.getLhflScheduleParamsInfo(id));
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面，进行复制")
    @GetMapping("/toCopy/{id}")
    public String toCopy(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhflScheduleParams", iLhflScheduleParamsService.getLhflScheduleParamsInfo(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增排程参数（硫磺辅料排程设置）")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveLhflScheduleParams(LhflScheduleParams lhflScheduleParams) {
        return iLhflScheduleParamsService.saveLhflScheduleParams(lhflScheduleParams);
    }

    @ApiOperation("复制排程参数（硫磺辅料排程设置）")
    @PostMapping("/copy")
    @ResponseBody
    public AjaxResult copyLhflScheduleParams(LhflScheduleParams lhflScheduleParams) {
        return iLhflScheduleParamsService.copyLhflScheduleParams(lhflScheduleParams);
    }

    /**
     * 导出排程参数（硫磺辅料排程设置）
     */
    @ApiOperation("导出排程参数（硫磺辅料排程设置）")
    @RequiresPermissions("setting:lhflScheduleParams:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, LhflScheduleParams lhflScheduleParams) throws IOException {
        String fileName = I18nUtil.getMessage("setting.lhflScheduleParams.modelName");
        List<LhflScheduleParams> list = iLhflScheduleParamsService.exportData(lhflScheduleParams);

        //仅在导出做处理,避免污染数据,但是增加了O(N)的时间复杂度
        for (LhflScheduleParams i : list) {
            if ("0".equals(i.getMixArea())) {
                i.setMixArea(I18nUtil.getMessage("ui.data.column.default"));
            }
        }

        ExcelUtil<LhflScheduleParams> util = new ExcelUtil<>(LhflScheduleParams.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, lhflScheduleParams.toString(), ZltConstant.PROCEDURE_CODE_FL_SETTING);
        iExportLogService.add(exportLog);
    }
}
