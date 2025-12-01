package com.zlt.aps.controller.cd15;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common4ui.utils.file.FileUtils4UI;
import com.ruoyi.file.api.service.IApsFileService;
import com.zlt.aps.cd15.api.domain.dto.Cd15ImportErrorLogManagementDto;
import com.zlt.aps.cd15.api.domain.dto.Cd15ImportLogManagementDto;
import com.zlt.aps.cd15.api.service.ICd15ImportLogManagementService;
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
@RequestMapping("/cd15/importLogManagement")
@Api(tags = {"工序导入日志管理接口"})
public class Cd15ImportLogManagementController extends BaseController {
    private final String prefix = "cd15/importLogManagement";

    @Autowired
    private IApsFileService iApsFileService;

    @Autowired
    private ICd15ImportLogManagementService iCd15ImportLogManagementService;

    @RequiresPermissions("cd15:importLogManagement:view")
    @GetMapping()
    public String ImportLogManagement() {
        return prefix + "/importLogManagement";
    }

    /**
     * 查询工序导入日志管理列表
     */
    @RequiresPermissions("cd15:importLogManagement:list")
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15ImportLogManagementDto dto) {
        return iCd15ImportLogManagementService.list(dto);
    }

    /**
     * 获取工序导入日志管理详细信息
     */
    @ApiOperation("获取工序导入日志管理详细信息")
    @GetMapping("/{id}")
    public String getImportLogManagement(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Params", iCd15ImportLogManagementService.getImportLogManagement(id));
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
    public TableDataInfo detailList(Cd15ImportErrorLogManagementDto dto) {
        return iCd15ImportLogManagementService.getImportErrorLogManagement(dto);
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