package com.zlt.aps.controller.cd90;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineRollMapping;
import com.zlt.aps.cd90.api.service.ICd90MachineRollMappingRemoteService;
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

@Api(tags = "直裁大卷与机台映射")
@Controller
@RequestMapping("/cd90/cd90MachineRollMapping")
public class Cd90MachineRollMappingUIController extends BaseUIController<Cd90MachineRollMapping> {

    @Resource
    private ICd90MachineRollMappingRemoteService remoteService;

    @ApiOperation("查询列表") @RequiresPermissions("cd90:machineRollMapping:list")
    @PostMapping("/list") @ResponseBody
    public TableDataInfo list(Cd90MachineRollMapping queryVO) { return remoteService.list(queryVO); }

    @ApiOperation("获取详情") @GetMapping("/getInfo/{id}") @ResponseBody
    public Cd90MachineRollMapping getInfo(@PathVariable("id") Long id) { return remoteService.getInfo(id); }

    @ApiOperation("新增") @RequiresPermissions("cd90:machineRollMapping:add")
    @PostMapping("/add") @ResponseBody
    public AjaxResult add(@RequestBody Cd90MachineRollMapping entity) {
        if (UserConstants.NOT_UNIQUE.equals(remoteService.checkUnique(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.machineRollMapping.checkUnique"));
        return remoteService.add(entity);
    }

    @ApiOperation("编辑") @RequiresPermissions("cd90:machineRollMapping:edit")
    @PostMapping("/edit") @ResponseBody
    public AjaxResult edit(@RequestBody Cd90MachineRollMapping entity) {
        if (UserConstants.NOT_UNIQUE.equals(remoteService.checkUnique(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.machineRollMapping.checkUnique"));
        return remoteService.edit(entity);
    }

    @ApiOperation("删除") @RequiresPermissions("cd90:machineRollMapping:remove")
    @PostMapping("/remove") @ResponseBody
    public AjaxResult remove(String ids) { return remoteService.removeByIds(Arrays.asList(Convert.toLongArray(ids))); }

    @Override public String getExportTemplateFileName() { return getFunctionName(); }
    @Override public String getProcedureCode() { return "CD90_MACHINE_ROLL_MAPPING"; }
    @Override public String getFunctionName() { return I18nUtil.getMessage("ui.data.column.machineRollMapping.modelName"); }

    @ApiOperation("下载导入模板") @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<Cd90MachineRollMapping> util = new ExcelUtil<>(Cd90MachineRollMapping.class);
        util.exportExcel(response, null, getExportTemplateFileName(), getExportTemplateFileName());
        return AjaxResult.success();
    }

    @ApiOperation("导出") @RequiresPermissions("cd90:machineRollMapping:export")
    @GetMapping("/export") @ResponseBody @Override
    public void export(HttpServletResponse response, Cd90MachineRollMapping entity) throws IOException {
        byte[] excelBytes = remoteService.exportData(entity, getExportTemplateFileName());
        ExcelUtil.setResponseHeader(response, getExportTemplateFileName(), ".xlsx");
        IOUtils.copy(new ByteArrayInputStream(excelBytes), response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入") @RequiresPermissions("cd90:machineRollMapping:import")
    @PostMapping("/importData") @ResponseBody @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath); context.setFunctionName(getFunctionName());
        context.setProcedureCode(getProcedureCode()); context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return remoteService.importData(context, updateSupport);
    }
}