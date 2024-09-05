package com.zlt.aps.controller.nc;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.nc.api.domain.dto.NcParamsDto;
import com.zlt.aps.nc.api.service.INcParamsService;
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
 * 内衬参数信息接口
 */
@Controller
@RequestMapping("/nc/params")
@Api(tags = {"内衬参数信息接口"})
public class NcParamsController extends BaseController {
    private final String prefix = "nc/params";

    @Autowired
    private INcParamsService iNcParamsService;

    @Autowired
    private IExportLogService iExportLogService;

    @RequiresPermissions("nc:params:view")
    @GetMapping()
    public String ncParams() {
        return prefix + "/params";
    }

    /**
     * 查询内衬参数信息列表
     */
    @RequiresPermissions("nc:params:list")
    @ApiOperation("查询内衬参数信息列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(NcParamsDto dto) {
        return iNcParamsService.list(dto);
    }

    /**
     * 获取内衬参数信息详细信息
     */
    @ApiOperation("获取内衬参数信息详细信息")
    @GetMapping("/{id}")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Params", iNcParamsService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 修改内衬参数信息
     */
    @RequiresPermissions("nc:params:edit")
    @ApiOperation("修改内衬参数信息")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@Validated NcParamsDto dto) {
        return iNcParamsService.edit(dto);
    }


    /**
     * 导出内衬参数信息
     */
    @RequiresPermissions("nc:params:export")
    @ApiOperation("导出内衬参数信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, NcParamsDto dto) throws IOException {
        List<NcParamsDto> list = iNcParamsService.exportData(dto);
        ExcelUtil<NcParamsDto> util = new ExcelUtil<>(NcParamsDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.nc.params.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_NC);
        iExportLogService.add(exportLog);
    }
}