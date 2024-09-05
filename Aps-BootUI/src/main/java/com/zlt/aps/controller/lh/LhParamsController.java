package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.lh.api.domain.dto.LhParamsDto;
import com.zlt.aps.lh.api.service.ILhParamsService;
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
 * 硫化参数信息接口
 */
@Controller
@RequestMapping("/lh/params")
@Api(tags = {"硫化参数信息接口"})
public class LhParamsController extends BaseController {
    private final String prefix = "lh/params";

    @Autowired
    private ILhParamsService iLhParamsService;

    @Autowired
    private IExportLogService iExportLogService;

    @RequiresPermissions("lh:params:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/params";
    }

    /**
     * 查询硫化参数信息列表
     */
    @RequiresPermissions("lh:params:list")
    @ApiOperation("查询硫化参数信息列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhParamsDto dto) {
        return iLhParamsService.list(dto);
    }

    /**
     * 获取硫化参数信息详细信息
     */
    @ApiOperation("获取硫化参数信息详细信息")
    @GetMapping("/{id}")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Params", iLhParamsService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 修改硫化参数信息
     */
    @RequiresPermissions("lh:params:edit")
    @ApiOperation("修改硫化参数信息")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@Validated LhParamsDto dto) {
        return iLhParamsService.edit(dto);
    }


    /**
     * 导出硫化参数信息
     */
    @RequiresPermissions("lh:params:export")
    @ApiOperation("导出硫化参数信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, LhParamsDto dto) throws IOException {
        List<LhParamsDto> list = iLhParamsService.exportData(dto);
        ExcelUtil<LhParamsDto> util = new ExcelUtil<>(LhParamsDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.lh.params.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_LH);
        iExportLogService.add(exportLog);
    }
}