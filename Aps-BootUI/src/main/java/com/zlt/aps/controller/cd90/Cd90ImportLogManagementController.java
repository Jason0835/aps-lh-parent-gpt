package com.zlt.aps.controller.cd90;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common4ui.utils.file.FileUtils4UI;
import com.ruoyi.file.api.service.IApsFileService;
import com.zlt.aps.cd90.api.domain.dto.Cd90ImportErrorLogManagementDto;
import com.zlt.aps.cd90.api.domain.dto.Cd90ImportLogManagementDto;
import com.zlt.aps.cd90.api.service.ICd90ImportLogManagementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;


/**
 * 工序导入日志管理信息接口
 */
@Controller
@RequestMapping("/cd90/importLogManagement")
@Api(tags = {"工序导入日志管理接口"})
public class Cd90ImportLogManagementController extends BaseController {
    private final String prefix = "cd90/importLogManagement";

    @Autowired
    private ICd90ImportLogManagementService iCd90ImportLogManagementService;

    @Autowired
    private IApsFileService iApsFileService;

    @RequiresPermissions("cd90:importLogManagement:view")
    @GetMapping()
    public String ImportLogManagement() {
        return prefix + "/importLogManagement";
    }

    /**
     * 查询工序导入日志管理列表
     */
    @RequiresPermissions("cd90:importLogManagement:list")
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90ImportLogManagementDto dto) {
        return iCd90ImportLogManagementService.list(dto);
    }

    /**
     * 获取工序导入日志管理详细信息
     */
    @ApiOperation("获取工序导入日志管理详细信息")
    @GetMapping("/{id}")
    public String getImportLogManagement(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Params", iCd90ImportLogManagementService.getImportLogManagement(id));
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
    public TableDataInfo detailList(Cd90ImportErrorLogManagementDto dto) {
        return iCd90ImportLogManagementService.getImportErrorLogManagement(dto);
    }

    @GetMapping("/download")
    public void resourceDownload(String name, String url, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        response.setCharacterEncoding("utf-8");
        response.setContentType("multipart/form-data");
        response.setHeader("Content-Disposition", "attachment;fileName=" + FileUtils4UI.setFileDownloadHeader(request, name));
        byte[] data = iApsFileService.downloadByteFile(url, "import");
        OutputStream outputStream = response.getOutputStream();
        IOUtils.write(data, outputStream);
        outputStream.close();
    }
}