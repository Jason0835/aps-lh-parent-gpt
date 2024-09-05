package com.zlt.aps.controller.tc;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.tc.api.domain.dto.TcParamsDto;
import com.zlt.aps.tc.api.service.ITcParamsService;
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
 * 胎侧参数信息接口
 */
@Controller
@RequestMapping("/tc/params")
@Api(tags = {"胎侧参数信息接口"})
public class TcParamsController extends BaseController {
    private final String prefix = "tc/params";

    @Autowired
    private ITcParamsService iTcParamsService;

    @Autowired
    private IExportLogService iExportLogService;

    @RequiresPermissions("tc:params:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/params";
    }

    /**
     * 查询胎侧参数信息列表
     */
    @RequiresPermissions("tc:params:list")
    @ApiOperation("查询胎侧参数信息列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcParamsDto dto) {
        return iTcParamsService.list(dto);
    }

    /**
     * 获取胎侧参数信息详细信息
     */
    @ApiOperation("获取胎侧参数信息详细信息")
    @GetMapping("/{id}")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tcParams", iTcParamsService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 修改胎侧参数信息
     */
    @RequiresPermissions("tc:params:edit")
    @ApiOperation("修改胎侧参数信息")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@Validated TcParamsDto dto) {
        return iTcParamsService.edit(dto);
    }


    /**
     * 导出胎侧参数信息
     */
    @RequiresPermissions("tc:params:export")
    @ApiOperation("导出胎面参数信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TcParamsDto dto) throws IOException {
        List<TcParamsDto> list = iTcParamsService.exportData(dto);
        ExcelUtil<TcParamsDto> util = new ExcelUtil<>(TcParamsDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.tc.params.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_TC);
        iExportLogService.add(exportLog);
    }
}