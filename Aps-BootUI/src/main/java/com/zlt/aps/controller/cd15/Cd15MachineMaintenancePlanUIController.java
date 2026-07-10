package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineMaintenancePlan;
import com.zlt.aps.cd15.api.service.ICd15MachineMaintenancePlanRemoteService;
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
 * 斜裁机台检修计划 UI 控制层。
 */
@Api(tags = "斜裁机台检修计划")
@Controller
@RequestMapping("/cd15/cd15MachineMaintenance")
public class Cd15MachineMaintenancePlanUIController extends BaseUIController<Cd15MachineMaintenancePlan> {

    @Resource
    private ICd15MachineMaintenancePlanRemoteService remoteService;

    /** 查询斜裁机台检修计划列表 */
    @ApiOperation("查询斜裁机台检修计划列表")
    @RequiresPermissions("cd15:machineMaintenancePlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15MachineMaintenancePlan queryVO) {
        return remoteService.list(queryVO);
    }

    /** 获取斜裁机台检修计划详情 */
    @ApiOperation("获取斜裁机台检修计划详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd15MachineMaintenancePlan getInfo(@PathVariable("id") Long id) {
        return remoteService.getInfo(id);
    }

    /** 新增斜裁机台检修计划 */
    @ApiOperation("新增斜裁机台检修计划")
    @RequiresPermissions("cd15:machineMaintenancePlan:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd15MachineMaintenancePlan entity) {
        AjaxResult validateResult = validateUniqueAndOverlap(entity);
        if (validateResult != null) {
            return validateResult;
        }
        return remoteService.add(entity);
    }

    /** 编辑斜裁机台检修计划 */
    @ApiOperation("编辑斜裁机台检修计划")
    @RequiresPermissions("cd15:machineMaintenancePlan:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd15MachineMaintenancePlan entity) {
        AjaxResult validateResult = validateUniqueAndOverlap(entity);
        if (validateResult != null) {
            return validateResult;
        }
        return remoteService.edit(entity);
    }

    /** 删除斜裁机台检修计划 */
    @ApiOperation("删除斜裁机台检修计划")
    @RequiresPermissions("cd15:machineMaintenancePlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return remoteService.removeByIds(Arrays.asList(arr));
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
        return I18nUtil.getMessage("ui.data.column.cd15MachineMaintenancePlan.modelName");
    }

    /** 下载斜裁机台检修计划导入模板 */
    @ApiOperation("下载斜裁机台检修计划导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd15MachineMaintenancePlan> util = new ExcelUtil<>(Cd15MachineMaintenancePlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出斜裁机台检修计划 */
    @ApiOperation("导出斜裁机台检修计划")
    @RequiresPermissions("cd15:machineMaintenancePlan:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd15MachineMaintenancePlan entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = remoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入斜裁机台检修计划 */
    @ApiOperation("导入斜裁机台检修计划")
    @RequiresPermissions("cd15:machineMaintenancePlan:import")
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

    private AjaxResult validateUniqueAndOverlap(Cd15MachineMaintenancePlan entity) {
        if (UserConstants.NOT_UNIQUE.equals(remoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15MachineMaintenancePlan.checkUnique"));
        }
        if (UserConstants.NOT_UNIQUE.equals(remoteService.checkOverlap(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15MachineMaintenancePlan.timeOverlap"));
        }
        return null;
    }
}