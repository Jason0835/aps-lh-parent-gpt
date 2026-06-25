package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.service.ICd15MachineInfoRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 斜裁机台基础信息 UI 控制层。
 */
@Api(tags = "斜裁机台基础信息")
@Controller
@RequestMapping("/cd15/cd15MachineInfo")
public class Cd15MachineInfoUIController extends BaseUIController<Cd15MachineInfo> {

    @Resource
    private ICd15MachineInfoRemoteService cd15MachineInfoRemoteService;

    /** 查询斜裁机台列表 */
    @ApiOperation("查询斜裁机台列表")
    @RequiresPermissions("cd15:machineInfo:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15MachineInfo queryVO) {
        return cd15MachineInfoRemoteService.list(queryVO);
    }

    /** 获取斜裁机台详情 */
    @ApiOperation("获取斜裁机台详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd15MachineInfo getInfo(@PathVariable("id") Long id) {
        return cd15MachineInfoRemoteService.getInfo(id);
    }

    /** 新增斜裁机台 */
    @ApiOperation("新增斜裁机台")
    @RequiresPermissions("cd15:machineInfo:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd15MachineInfo machineInfo) {
        if (UserConstants.NOT_UNIQUE.equals(cd15MachineInfoRemoteService.checkUnique(machineInfo))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15MachineInfo.checkUnique"));
        }
        return cd15MachineInfoRemoteService.add(machineInfo);
    }

    /** 编辑斜裁机台 */
    @ApiOperation("编辑斜裁机台")
    @RequiresPermissions("cd15:machineInfo:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd15MachineInfo machineInfo) {
        if (UserConstants.NOT_UNIQUE.equals(cd15MachineInfoRemoteService.checkUnique(machineInfo))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15MachineInfo.checkUnique"));
        }
        return cd15MachineInfoRemoteService.edit(machineInfo);
    }

    /** 删除斜裁机台 */
    @ApiOperation("删除斜裁机台")
    @RequiresPermissions("cd15:machineInfo:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cd15MachineInfoRemoteService.removeByIds(Arrays.asList(arr));
    }

    /** 启用机台下拉 */
    @ApiOperation("启用机台下拉")
    @PostMapping("/enableOptions")
    @ResponseBody
    public AjaxResult enableOptions(Cd15MachineInfo queryVO) {
        return cd15MachineInfoRemoteService.enableOptions(queryVO);
    }

    /** 修改机台状态 */
    @ApiOperation("修改机台状态")
    @RequiresPermissions("cd15:machineInfo:edit")
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(@RequestBody Cd15MachineInfo machineInfo) {
        return cd15MachineInfoRemoteService.changeStatus(machineInfo);
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "CD15";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.cd15MachineInfo.modelName");
    }

    /** 下载导入模板 */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd15MachineInfo> util = new ExcelUtil<>(Cd15MachineInfo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出斜裁机台 */
    @ApiOperation("导出斜裁机台")
    @RequiresPermissions("cd15:machineInfo:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd15MachineInfo entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = cd15MachineInfoRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入斜裁机台 */
    @ApiOperation("导入斜裁机台")
    @RequiresPermissions("cd15:machineInfo:import")
    @PostMapping("/importData")
    @ResponseBody
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(getFunctionName());
        context.setProcedureCode(getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return cd15MachineInfoRemoteService.importData(context, updateSupport);
    }
}
