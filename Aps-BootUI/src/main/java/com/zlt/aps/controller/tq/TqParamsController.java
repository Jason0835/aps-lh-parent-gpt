package com.zlt.aps.controller.tq;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.tq.api.domain.dto.TqParamsDto;
import com.zlt.aps.tq.api.service.ITqParamsService;
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
 * 胎圈参数信息接口
 */
@Controller
@RequestMapping("/tq/params")
@Api(tags = {"胎圈参数信息接口"})
public class TqParamsController extends BaseController {
    private final String prefix = "tq/params";

    @Autowired
    private ITqParamsService iTqParamsService;

    @Autowired
    private IExportLogService iExportLogService;

    @RequiresPermissions("tq:params:view")
    @GetMapping()
    public String ncParams() {
        return prefix + "/params";
    }

    /**
     * 查询胎圈参数信息列表
     */
    @RequiresPermissions("tq:params:list")
    @ApiOperation("查询胎圈参数信息列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TqParamsDto dto) {
        return iTqParamsService.list(dto);
    }

    /**
     * 获取胎圈参数信息详细信息
     */
    @ApiOperation("获取胎圈参数信息详细信息")
    @GetMapping("/{id}")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Params", iTqParamsService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 修改胎圈参数信息
     */
    @RequiresPermissions("tq:params:edit")
    @ApiOperation("修改胎圈参数信息")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@Validated TqParamsDto dto) {
        return iTqParamsService.edit(dto);
    }


    /**
     * 导出胎圈参数信息
     */
    @RequiresPermissions("tq:params:export")
    @ApiOperation("导出胎圈参数信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TqParamsDto dto) throws IOException {
        List<TqParamsDto> list = iTqParamsService.exportData(dto);
        ExcelUtil<TqParamsDto> util = new ExcelUtil<>(TqParamsDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.tq.params.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_TQ);
        iExportLogService.add(exportLog);
    }
}