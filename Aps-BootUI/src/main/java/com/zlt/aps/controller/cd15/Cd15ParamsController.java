package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.dto.Cd15ParamsDto;
import com.zlt.aps.cd15.api.service.ICd15ParamsService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
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
 * 15度裁断参数信息接口
 */
@Controller
@RequestMapping("/cd15/params")
@Api(tags = {"15度裁断参数信息接口"})
public class Cd15ParamsController extends BaseController {
    private final String prefix = "cd15/params";

    @Autowired
    private ICd15ParamsService iCd15ParamsService;

    @Autowired
    private IExportLogService iExportLogService;

    @RequiresPermissions("cd15:params:view")
    @GetMapping()
    public String ncParams() {
        return prefix + "/params";
    }

    /**
     * 查询15度裁断参数信息列表
     */
    @RequiresPermissions("cd15:params:list")
    @ApiOperation("查询15度裁断参数信息列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15ParamsDto dto) {
        return iCd15ParamsService.list(dto);
    }

    /**
     * 获取15度裁断参数信息详细信息
     */
    @ApiOperation("获取15度裁断参数信息详细信息")
    @GetMapping("/{id}")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Params", iCd15ParamsService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 修改15度裁断参数信息
     */
    @RequiresPermissions("cd15:params:edit")
    @ApiOperation("修改15度裁断参数信息")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@Validated Cd15ParamsDto dto) {
        return iCd15ParamsService.edit(dto);
    }


    /**
     * 导出15度裁断参数信息
     */
    @RequiresPermissions("cd15:params:export")
    @ApiOperation("导出15度裁断参数信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd15ParamsDto dto) throws IOException {
        List<Cd15ParamsDto> list = iCd15ParamsService.exportData(dto);
        ExcelUtil<Cd15ParamsDto> util = new ExcelUtil<>(Cd15ParamsDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.cd15.params.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CD15);
        iExportLogService.add(exportLog);
    }
}