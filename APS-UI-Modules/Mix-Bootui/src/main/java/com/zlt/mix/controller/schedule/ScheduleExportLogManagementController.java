package com.zlt.mix.controller.schedule;

import com.ruoyi.common.core.utils.file.FileUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.file.api.service.ISimpleFileService;
import com.zlt.mix.schedule.api.domain.entity.ScheduleExportLogManagement;
import com.zlt.mix.schedule.api.service.IScheduleExportLogManagementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

/**
 * 工序导出日志管理信息接口
 */
@Controller
@RequestMapping("/schedule/exportLogManagement")
@Api(tags = {"导出日志管理接口"})
public class ScheduleExportLogManagementController extends BaseController {
    private final String prefix = "schedule/exportLogManagement";

    @Autowired
    private IScheduleExportLogManagementService iScheduleExportLogManagementService;

    @Autowired
    private ISimpleFileService iSimpleFileService;

    @RequiresPermissions("schedule:exportLogManagement:view")
    @GetMapping()
    public String exportLogManagement() {
        return prefix + "/exportLogManagement";
    }

    /**
     * 查询工序导出日志管理列表
     */
    @RequiresPermissions("schedule:exportLogManagement:list")
    @ApiOperation("查询工序导出日志管理列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ScheduleExportLogManagement dto) {
        return iScheduleExportLogManagementService.list(dto);
    }

    /**
     * 获取工序导出日志管理详细信息
     */
    @ApiOperation("获取工序导出日志管理详细信息")
    @GetMapping("/{id}")
    public String getExportLogManagement(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Params", iScheduleExportLogManagementService.getExportLogManagement(id));
        return prefix + "/edit";
    }


    @GetMapping("/download")
    public void resourceDownload(String name, String url, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        response.setCharacterEncoding("utf-8");
        response.setContentType("multipart/form-data");
        response.setHeader("Content-Disposition", "attachment;fileName=" + FileUtils.setFileDownloadHeader(request, name));
        byte[] data = iSimpleFileService.downloadByteFile(url, "export");
        OutputStream outputStream = response.getOutputStream();
        IOUtils.write(data, outputStream);
        outputStream.close();
    }
}