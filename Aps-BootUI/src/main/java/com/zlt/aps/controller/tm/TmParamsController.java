package com.zlt.aps.controller.tm;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.tm.api.domain.dto.TmParamsDto;
import com.zlt.aps.tm.api.service.ITmParamsService;
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
 * 胎面参数信息接口
 */
@Controller
@RequestMapping("/tm/params")
@Api(tags = {"胎面参数信息接口"})
public class TmParamsController extends BaseController {
    private final String prefix = "tm/params";

    @Autowired
    private ITmParamsService iTmParamsService;

    @Autowired
    private IExportLogService iExportLogService;

    @RequiresPermissions("tm:params:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/params";
    }

    /**
     * 查询胎面参数信息列表
     */
    @RequiresPermissions("tm:params:list")
    @ApiOperation("查询胎面参数信息列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmParamsDto tmParamsDto) {
        return iTmParamsService.list(tmParamsDto);
    }

    /**
     * 获取胎面参数信息详细信息
     */
    @ApiOperation("获取胎面参数信息详细信息")
    @GetMapping("/{id}")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tmParams", iTmParamsService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 修改胎面参数信息
     */
    @RequiresPermissions("tm:params:edit")
    @ApiOperation("修改胎面参数信息")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@Validated TmParamsDto tmParamsDto) {
        return iTmParamsService.edit(tmParamsDto);
    }

    /**
     * 导出胎面参数信息
     */
    @RequiresPermissions("tm:params:export")
    @ApiOperation("导出胎面参数信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TmParamsDto dto) throws IOException {
        List<TmParamsDto> list = iTmParamsService.exportData(dto);
        ExcelUtil<TmParamsDto> util = new ExcelUtil<>(TmParamsDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.tm.params.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_TM);
        iExportLogService.add(exportLog);
    }
}