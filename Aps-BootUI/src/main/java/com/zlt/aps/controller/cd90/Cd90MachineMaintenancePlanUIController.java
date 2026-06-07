package com.zlt.aps.controller.cd90;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineMaintenancePlan;
import com.zlt.aps.cd90.api.service.ICd90MachineMaintenancePlanRemoteService;
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

@Api(tags = "直裁机台检修计划")
@Controller
@RequestMapping("/cd90/cd90MachineMaintenance")
public class Cd90MachineMaintenancePlanUIController extends BaseUIController<Cd90MachineMaintenancePlan> {

    @Resource
    private ICd90MachineMaintenancePlanRemoteService remoteService;

    @ApiOperation("查询列表")
    @RequiresPermissions("cd90:machineMaintenance:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90MachineMaintenancePlan queryVO) { return remoteService.list(queryVO); }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd90MachineMaintenancePlan getInfo(@PathVariable("id") Long id) { return remoteService.getInfo(id); }

    @ApiOperation("新增")
    @RequiresPermissions("cd90:machineMaintenance:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd90MachineMaintenancePlan entity) {
        if (UserConstants.NOT_UNIQUE.equals(remoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.machineMaintenancePlan.checkUnique"));
        }
        return remoteService.add(entity);
    }

    @ApiOperation("编辑")
    @RequiresPermissions("cd90:machineMaintenance:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd90MachineMaintenancePlan entity) {
        if (UserConstants.NOT_UNIQUE.equals(remoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.machineMaintenancePlan.checkUnique"));
        }
        return remoteService.edit(entity);
    }

    @ApiOperation("删除")
    @RequiresPermissions("cd90:machineMaintenance:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return remoteService.removeByIds(Arrays.asList(arr));
    }

    @Override public String getExportTemplateFileName() { return getFunctionName(); }
    @Override public String getProcedureCode() { return "CD90_MACHINE_MAINTENANCE_PLAN"; }
    @Override public String getFunctionName() { return I18nUtil.getMessage("ui.data.column.machineMaintenancePlan.modelName"); }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd90MachineMaintenancePlan> util = new ExcelUtil<>(Cd90MachineMaintenancePlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("导出")
    @RequiresPermissions("cd90:machineMaintenance:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd90MachineMaintenancePlan entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = remoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入")
    @RequiresPermissions("cd90:machineMaintenance:import")
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
        return remoteService.importData(context, updateSupport);
    }
}