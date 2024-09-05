package com.zlt.aps.controller.gsq;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.gsq.api.domain.dto.GsqParamsDto;
import com.zlt.aps.gsq.api.service.IGsqParamsService;
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
 * 钢丝圈参数信息接口
 *
 * @author 89875
 */
@Controller
@RequestMapping("/gsq/params")
@Api(tags = {"钢丝圈参数信息接口"})
public class GsqParamsController extends BaseController {
    private final String prefix = "gsq/params";

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IGsqParamsService iGsqParamsService;

    @RequiresPermissions("gsq:params:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/params";
    }

    /**
     * 查询钢丝圈参数信息列表
     */
    @RequiresPermissions("gsq:params:list")
    @ApiOperation("查询钢丝圈参数信息列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqParamsDto dto) {
        return iGsqParamsService.list(dto);
    }

    /**
     * 获取钢丝圈参数信息详细信息
     */
    @ApiOperation("获取钢丝圈参数信息详细信息")
    @GetMapping("/{id}")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Params", iGsqParamsService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 修改钢丝圈参数信息
     */
    @RequiresPermissions("gsq:params:edit")
    @ApiOperation("修改钢丝圈参数信息")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@Validated GsqParamsDto dto) {
        return iGsqParamsService.edit(dto);
    }


    /**
     * 导出钢丝圈参数信息
     */
    @RequiresPermissions("gsq:params:export")
    @ApiOperation("导出钢丝圈参数信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GsqParamsDto dto) throws IOException {
        List<GsqParamsDto> list = iGsqParamsService.exportData(dto);
        ExcelUtil<GsqParamsDto> util = new ExcelUtil<>(GsqParamsDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.gsq.params.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_GSQ);
        iExportLogService.add(exportLog);
    }
}