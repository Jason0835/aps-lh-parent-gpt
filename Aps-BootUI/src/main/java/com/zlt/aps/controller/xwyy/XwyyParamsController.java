package com.zlt.aps.controller.xwyy;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.xwyy.api.domain.dto.XwyyParamsDto;
import com.zlt.aps.xwyy.api.service.IXwyyParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 纤维压延参数信息接口
 */
@Controller
@RequestMapping("/xwyy/params")
@Api(tags = {"纤维压延参数信息接口"})
public class XwyyParamsController extends BaseController {
    private final String prefix = "xwyy/params";

    @Autowired
    private IXwyyParamsService iXwyyParamsService;

    @Autowired
    private IExportLogService iExportLogService;

    @RequiresPermissions("xwyy:params:view")
    @GetMapping()
    public String ncParams() {
        return prefix + "/params";
    }

    /**
     * 查询纤维压延参数信息列表
     */
    @RequiresPermissions("xwyy:params:list")
    @ApiOperation("查询纤维压延参数信息列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyParamsDto dto) {
        return iXwyyParamsService.list(dto);
    }

    /**
     * 获取纤维压延参数信息详细信息
     */
    @ApiOperation("获取纤维压延参数信息详细信息")
    @GetMapping("/{id}")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Params", iXwyyParamsService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 修改纤维压延参数信息
     */
    @RequiresPermissions("xwyy:params:edit")
    @ApiOperation("修改纤维压延参数信息")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@Validated XwyyParamsDto dto) {
        return iXwyyParamsService.edit(dto);
    }


    /**
     * 导出纤维压延参数信息
     */
    @RequiresPermissions("xwyy:params:export")
    @ApiOperation("导出纤维压延参数信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, XwyyParamsDto dto) throws IOException {
        List<XwyyParamsDto> list = iXwyyParamsService.exportData(dto);
        ExcelUtil<XwyyParamsDto> util = new ExcelUtil<>(XwyyParamsDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.xwyy.params.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_XWYY);
        iExportLogService.add(exportLog);
    }
}