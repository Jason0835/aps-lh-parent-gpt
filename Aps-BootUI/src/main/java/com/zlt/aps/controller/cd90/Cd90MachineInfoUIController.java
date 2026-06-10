package com.zlt.aps.controller.cd90;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.api.service.ICd90MachineInfoRemoteService;
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
 * 直裁机台基础信息 UI 控制层。
 */
@Api(tags = "直裁机台基础信息")
@Controller
@RequestMapping("/cd90/cd90MachineInfo")
public class Cd90MachineInfoUIController extends BaseUIController<Cd90MachineInfo> {

    @Resource
    private ICd90MachineInfoRemoteService cd90MachineInfoRemoteService;

    /** 查询直裁机台列表 */
    @ApiOperation("查询直裁机台列表")
    @RequiresPermissions("cd90:machineInfo:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90MachineInfo queryVO) {
        return cd90MachineInfoRemoteService.list(queryVO);
    }

    /** 获取直裁机台详情 */
    @ApiOperation("获取直裁机台详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd90MachineInfo getInfo(@PathVariable("id") Long id) {
        return cd90MachineInfoRemoteService.getInfo(id);
    }

    /** 新增直裁机台 */
    @ApiOperation("新增直裁机台")
    @RequiresPermissions("cd90:machineInfo:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd90MachineInfo machineInfo) {
        if (UserConstants.NOT_UNIQUE.equals(cd90MachineInfoRemoteService.checkUnique(machineInfo))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90MachineInfo.checkUnique"));
        }
        return cd90MachineInfoRemoteService.add(machineInfo);
    }

    /** 编辑直裁机台 */
    @ApiOperation("编辑直裁机台")
    @RequiresPermissions("cd90:machineInfo:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd90MachineInfo machineInfo) {
        if (UserConstants.NOT_UNIQUE.equals(cd90MachineInfoRemoteService.checkUnique(machineInfo))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90MachineInfo.checkUnique"));
        }
        return cd90MachineInfoRemoteService.edit(machineInfo);
    }

    /** 删除直裁机台 */
    @ApiOperation("删除直裁机台")
    @RequiresPermissions("cd90:machineInfo:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cd90MachineInfoRemoteService.removeByIds(Arrays.asList(arr));
    }

    /** 启用机台下拉 */
    @ApiOperation("启用机台下拉")
    @PostMapping("/enableOptions")
    @ResponseBody
    public AjaxResult enableOptions(Cd90MachineInfo queryVO) {
        return cd90MachineInfoRemoteService.enableOptions(queryVO);
    }

    /** 修改机台状态 */
    @ApiOperation("修改机台状态")
    @RequiresPermissions("cd90:machineInfo:edit")
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(@RequestBody Cd90MachineInfo machineInfo) {
        return cd90MachineInfoRemoteService.changeStatus(machineInfo);
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "CD90";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.cd90MachineInfo.modelName");
    }

    /** 下载导入模板 */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd90MachineInfo> util = new ExcelUtil<>(Cd90MachineInfo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出直裁机台 */
    @ApiOperation("导出直裁机台")
    @RequiresPermissions("cd90:machineInfo:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd90MachineInfo entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = cd90MachineInfoRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入直裁机台 */
    @ApiOperation("导入直裁机台")
    @RequiresPermissions("cd90:machineInfo:import")
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
        return cd90MachineInfoRemoteService.importData(context, updateSupport);
    }
}
