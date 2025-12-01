package com.zlt.mix.controller.setting;

import com.ruoyi.common.core.utils.file.FileUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.file.api.service.ISimpleFileService;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.setting.api.domain.entity.SettingExportLogManagement;
import com.zlt.mix.setting.api.service.ISettingExportLogManagementService;
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
 * 硫磺辅料工序导出日志管理信息接口
 */
@Controller
@RequestMapping("/setting/lhflExportLogManagement")
@Api(tags = {"硫磺辅料导出日志管理接口"})
public class LhflExportLogManagementController extends BaseController {
    private final String prefix = "setting/lhflExportLogManagement";

    @Autowired
    private ISettingExportLogManagementService iSettingExportLogManagementService;

    @Autowired
    private ISimpleFileService iSimpleFileService;

    @RequiresPermissions("setting:lhflExportLogManagement:view")
    @GetMapping()
    public String exportLogManagement() {
        return prefix + "/exportLogManagement";
    }

    /**
     * 查询工序导出日志管理列表
     */
    @RequiresPermissions("setting:lhflExportLogManagement:list")
    @ApiOperation("查询工序导出日志管理列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SettingExportLogManagement dto) {
        dto.setProcedureCode(ZltConstant.PROCEDURE_CODE_FL_SETTING);
        return iSettingExportLogManagementService.list(dto);
    }

    /**
     * 获取工序导出日志管理详细信息
     */
    @ApiOperation("获取工序导出日志管理详细信息")
    @GetMapping("/{id}")
    public String getExportLogManagement(@PathVariable("id") Long id, ModelMap mmap) {

        mmap.put("Params", iSettingExportLogManagementService.getExportLogManagement(id));
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