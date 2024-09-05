package com.zlt.aps.controller.tm;


import com.alibaba.csp.sentinel.util.StringUtil;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.tm.TmGlueGroupOrderTemp;
import com.zlt.aps.tm.api.domain.dto.MachineDto;
import com.zlt.aps.tm.api.domain.dto.MaintenanceLogDto;
import com.zlt.aps.tm.api.domain.dto.TmGlueGroupOrderDto;
import com.zlt.aps.tm.api.service.IMaintenanceLogService;
import com.zlt.aps.tm.api.service.ITmGlueGroupOrderService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.framework.utils.AuthorizationUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Api(tags = {"胎面胶料组别顺序维护接口"})
@Controller
@RequestMapping("/maintenanceLog")
public class MaintenanceLogController extends BaseController {

    private String prefix = "tm/maintenanceLog";

    @Resource
    private IMaintenanceLogService iMaintenanceLogService;

    @RequiresPermissions("maintenanceLog:view")
    @GetMapping()
    public String maintenanceLog() {
        return prefix + "/maintenanceLog";
    }

    @RequiresPermissions("maintenanceLog:list")
    @ApiOperation("根据查询条件查询运维操作日志")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MaintenanceLogDto dto) {
        return iMaintenanceLogService.listMaintenanceLog(dto);
    }

    @ApiOperation("根据id查询运维操作日志的明细信息，跳转到详情页面")
    @GetMapping("/detail/{id}")
    public String detail(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("logDetail", iMaintenanceLogService.getDetailInfo(id));
        return prefix + "/detail";
    }

    @ApiOperation("跳转到排程发布重置页面")
    @GetMapping("/toResetScheduleRelease")
    public String toResetScheduleRelease() {
        return prefix + "/resetScheduleRelease";
    }

    @ApiOperation("排程发布重置")
    @RequiresPermissions("maintenanceLog:resetScheduleRelease")
    @PostMapping("/resetScheduleRelease")
    @ResponseBody
    public AjaxResult resetScheduleRelease(MaintenanceLogDto dto) {
        return iMaintenanceLogService.resetScheduleRelease(dto);
    }

    @ApiOperation("跳转到排程删除重置页面")
    @GetMapping("/toDeleteSchedule")
    public String toDeleteSchedule() {
        return prefix + "/deleteSchedule";
    }

    @ApiOperation("排程删除")
    @RequiresPermissions("maintenanceLog:deleteSchedule")
    @PostMapping("/deleteSchedule")
    @ResponseBody
    public AjaxResult deleteSchedule(MaintenanceLogDto dto) {
        return iMaintenanceLogService.deleteSchedule(dto);
    }

    @ApiOperation("根据工序局部刷新机台下拉框")
    @GetMapping("/listMachineByProcedure")
    public String listMachineByProcedure(String procedureCode, ModelMap mmap) {
        if(StringUtil.isBlank(procedureCode)) {
            procedureCode = " ";
        }
        List<MachineDto> machineList = iMaintenanceLogService.listMachineByProcedure(procedureCode);
        mmap.put("machineList", machineList);
        return prefix + "/deleteSchedule::machine_list";
    }
}
