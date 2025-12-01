package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cx.api.service.ICxParamsService;
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
 * 成型参数信息接口
 */
@Controller
@RequestMapping("/cx/params")
@Api(tags = {"成型参数信息接口"})
public class CxParamsController extends BaseController {
    private String prefix = "cx/params";

    @Autowired
    private ICxParamsService iCxParamsService;

    @Autowired
    private IExportLogService iExportLogService;


    @RequiresPermissions("cx:params:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/params";
    }

    /**
     * 查询成型参数信息列表
     */
    @RequiresPermissions("cx:params:list")
    @ApiOperation("查询成型参数信息列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxParamsDto dto) {
        return iCxParamsService.list(dto);
    }

    /**
     * 获取成型参数信息详细信息
     */
    @ApiOperation("获取成型参数信息详细信息")
    @GetMapping("/{id}")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Params", iCxParamsService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 修改成型参数信息
     */
    @RequiresPermissions("cx:params:edit")
    @ApiOperation("修改成型参数信息")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@Validated CxParamsDto dto) {
        return iCxParamsService.edit(dto);
    }

    /**
     * 导出成型参数信息
     */
    @RequiresPermissions("cx:params:export")
    @ApiOperation("导出成型参数信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxParamsDto dto) throws IOException {
        List<CxParamsDto> list = iCxParamsService.exportData(dto);
        ExcelUtil<CxParamsDto> util = new ExcelUtil<>(CxParamsDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.cx.params.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }
}