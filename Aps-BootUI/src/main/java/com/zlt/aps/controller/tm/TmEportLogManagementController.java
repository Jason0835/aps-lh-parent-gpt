package com.zlt.aps.controller.tm;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common4ui.utils.file.FileUtils;
import com.ruoyi.file.api.service.IApsFileService;
import com.zlt.aps.tm.api.domain.dto.TmExportLogManagementDto;
import com.zlt.aps.tm.api.service.ITmExportLogManagementService;
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
@RequestMapping("/tm/exportLogManagement")
@Api(tags = {"工序导出日志管理接口"})
public class TmEportLogManagementController extends BaseController {
    private final String prefix = "tm/exportLogManagement";

    @Autowired
    private ITmExportLogManagementService iTmExportLogManagementService;

    @Autowired
    private IApsFileService iApsFileService;

    @RequiresPermissions("tm:exportLogManagement:view")
    @GetMapping()
    public String exportLogManagement() {
        return prefix + "/exportLogManagement";
    }

    /**
     * 查询工序导出日志管理列表
     */
    @RequiresPermissions("tm:exportLogManagement:list")
    @ApiOperation("查询工序导出日志管理列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmExportLogManagementDto dto) {
        return iTmExportLogManagementService.list(dto);
    }

    /**
     * 获取工序导出日志管理详细信息
     */
    @ApiOperation("获取工序导出日志管理详细信息")
    @GetMapping("/{id}")
    public String getExportLogManagement(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Params", iTmExportLogManagementService.getExportLogManagement(id));
        return prefix + "/edit";
    }


    @GetMapping("/download")
    public void resourceDownload(String name, String url, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        response.setCharacterEncoding("utf-8");
        response.setContentType("multipart/form-data");
        response.setHeader("Content-Disposition", "attachment;fileName=" + FileUtils.setFileDownloadHeader(request, name));
        byte[] data = iApsFileService.downloadByteFile(url, "export");
        OutputStream outputStream = response.getOutputStream();
        IOUtils.write(data, outputStream);
        outputStream.close();
    }
}