package com.zlt.mix.controller.schedule;

import com.ruoyi.common.core.utils.file.FileUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.file.api.service.ISimpleFileService;
import com.zlt.mix.schedule.api.domain.dto.ScheduleImportErrorLogManagementDto;
import com.zlt.mix.schedule.api.domain.entity.ScheduleImportLogManagement;
import com.zlt.mix.schedule.api.service.IScheduleImportLogManagementService;
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
 * 工序导入日志管理信息接口
 */
@Controller
@RequestMapping("/schedule/importLogManagement")
@Api(tags = {"导入日志管理接口"})
public class ScheduleImportLogManagementController extends BaseController {
    private final String prefix = "schedule/importLogManagement";

    @Autowired
    private IScheduleImportLogManagementService iScheduleImportLogManagementService;

    @Autowired
    private ISimpleFileService iSimpleFileService;

    @RequiresPermissions("schedule:importLogManagement:view")
    @GetMapping()
    public String ImportLogManagement() {
        return prefix + "/importLogManagement";
    }

    /**
     * 查询工序导入日志管理列表
     */
    @RequiresPermissions("schedule:importLogManagement:list")
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ScheduleImportLogManagement dto) {
        return iScheduleImportLogManagementService.list(dto);
    }

    /**
     * 获取工序导入日志管理详细信息
     */
    @ApiOperation("获取工序导入日志管理详细信息")
    @GetMapping("/{id}")
    public String getImportLogManagement(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Params", iScheduleImportLogManagementService.getImportLogManagement(id));
        return prefix + "/edit";
    }

    /**
     * 跳转至错误日志详情页面
     */
    @ApiOperation("跳转至错误日志详情页面")
    @GetMapping("/errorView/{id}")
    public String gotoErrorDetail(@PathVariable("id") String id, ModelMap mmap) {
        mmap.put("importLogId", id);
        mmap.put("listUrl", prefix + "/errorDetailList");
        return "common/logDetail";
    }

    /**
     * 错误日志详情列表
     */
    @ApiOperation("错误日志详情列表")
    @PostMapping("/errorDetailList")
    @ResponseBody
    public TableDataInfo detailList(ScheduleImportErrorLogManagementDto dto) {
        return iScheduleImportLogManagementService.getImportErrorLogManagement(dto);
    }

    @GetMapping("/download")
    public void resourceDownload(String name, String url, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        response.setCharacterEncoding("utf-8");
        response.setContentType("multipart/form-data");
        response.setHeader("Content-Disposition", "attachment;fileName=" + FileUtils.setFileDownloadHeader(request, name));
        byte[] data = iSimpleFileService.downloadByteFile(url, "import");
        OutputStream outputStream = response.getOutputStream();
        IOUtils.write(data, outputStream);
        outputStream.close();
    }
}