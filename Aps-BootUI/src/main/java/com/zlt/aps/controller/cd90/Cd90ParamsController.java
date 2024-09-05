package com.zlt.aps.controller.cd90;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.dto.Cd90ParamsDto;
import com.zlt.aps.cd90.api.service.ICd90ParamsService;
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
 * 90度裁断参数信息接口
 */
@Controller
@RequestMapping("/cd90/params")
@Api(tags = {"90度裁断参数信息接口"})
public class Cd90ParamsController extends BaseController {
    private final String prefix = "cd90/params";

    @Autowired
    private ICd90ParamsService iCd90ParamsService;

    @Autowired
    private IExportLogService iExportLogService;

    @RequiresPermissions("cd90:params:view")
    @GetMapping()
    public String ncParams() {
        return prefix + "/params";
    }

    /**
     * 查询90度裁断参数信息列表
     */
    @RequiresPermissions("cd90:params:list")
    @ApiOperation("查询90度裁断参数信息列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90ParamsDto dto) {
        return iCd90ParamsService.list(dto);
    }

    /**
     * 获取90度裁断参数信息详细信息
     */
    @ApiOperation("获取90度裁断参数信息详细信息")
    @GetMapping("/{id}")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Params", iCd90ParamsService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 修改90度裁断参数信息
     */
    @RequiresPermissions("cd90:params:edit")
    @ApiOperation("修改90度裁断参数信息")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@Validated Cd90ParamsDto dto) {
        return iCd90ParamsService.edit(dto);
    }


    /**
     * 导出90度裁断参数信息
     */
    @RequiresPermissions("cd90:params:export")
    @ApiOperation("导出90度裁断参数信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd90ParamsDto dto) throws IOException {
        List<Cd90ParamsDto> list = iCd90ParamsService.exportData(dto);
        ExcelUtil<Cd90ParamsDto> util = new ExcelUtil<>(Cd90ParamsDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.cd90.params.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CD90);
        iExportLogService.add(exportLog);
    }
}