package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.cx.api.domain.entity.PlmConstructionInfo;
import com.zlt.aps.cx.api.service.IPlmParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * PLM参数信息接口
 */
@Controller
@RequestMapping("/cx/plm")
@Api(tags = {"PLM参数信息接口"})
public class PlmParamsController extends BaseController {
    private String prefix = "cx/plm";

    @Autowired
    private IPlmParamsService iPlmParamsService;

    @Autowired
    private IExportLogService iExportLogService;


    @RequiresPermissions("cx:plm:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/plm";
    }

    /**
     * 查询PLM参数信息列表
     */
    @RequiresPermissions("cx:plm:list")
    @ApiOperation("查询PLM参数信息列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(PlmConstructionInfo plm) {
        return iPlmParamsService.list(plm);
    }

    /**
     * 获取PLM参数信息详细信息
     */
    @ApiOperation("获取PLM参数信息详细信息")
    @GetMapping("/{id}")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("entity", iPlmParamsService.getInfo(id));
        return prefix + "/detail";
    }


    /**
     * 导出PLM参数信息
     */
    @RequiresPermissions("cx:plm:export")
    @ApiOperation("导出PLM参数信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, PlmConstructionInfo plm) throws IOException {
        List<PlmConstructionInfo> list = iPlmParamsService.exportData(plm);
        ExcelUtil<PlmConstructionInfo> util = new ExcelUtil<>(PlmConstructionInfo.class);
        String fileName = I18nUtil.getMessage("ui.data.column.plm.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, plm.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }
}