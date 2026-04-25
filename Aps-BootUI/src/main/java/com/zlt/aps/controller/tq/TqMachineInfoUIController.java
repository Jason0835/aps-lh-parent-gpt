package com.zlt.aps.controller.tq;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.service.ITqMachineInfoService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Api(tags = "胎圈机台信息")
@Controller
@RequestMapping("/tq/machine")
public class TqMachineInfoUIController extends BaseUIController<TqMachineInfo> {

    @Autowired
    private ITqMachineInfoService iTqMachineInfoService;

    private final String prefix = "tq/machine";

    @RequiresPermissions("tq:machine:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/machine";
    }

    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("machineInfo", new TqMachineInfo());
        return prefix + "/edit";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("machineInfo", iTqMachineInfoService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("查询胎圈机台信息列表")
    @RequiresPermissions("tq:machine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TqMachineInfo machineInfo) {
        return iTqMachineInfoService.list(machineInfo);
    }

    @ApiOperation("修改或新增")
    @RequiresPermissions({"tq:machine:edit", "tq:machine:add"})
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(TqMachineInfo machineInfo) {
        if (UserConstants.NOT_UNIQUE.equals(iTqMachineInfoService.checkUnique(machineInfo))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.tqMachineInfo.notUnique"));
        }
        return iTqMachineInfoService.save(machineInfo);
    }

    @ApiOperation("删除胎圈机台信息")
    @RequiresPermissions("tq:machine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqMachineInfoService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(TqMachineInfo machineInfo) {
        return iTqMachineInfoService.checkUnique(machineInfo);
    }

    @ApiOperation("获取机台信息列表")
    @PostMapping("/listMachineInfo")
    @ResponseBody
    public AjaxResult listMachineInfo(TqMachineInfo machineInfo) {
        List<TqMachineInfo> list = iTqMachineInfoService.listMachineInfo(machineInfo);
        return AjaxResult.success(list);
    }

    @ApiOperation("查询未删除且启用的机台列表")
    @PostMapping("/listEnabledMachines")
    @ResponseBody
    public AjaxResult listEnabledMachines() {
        List<TqMachineInfo> list = iTqMachineInfoService.listEnabledMachines();
        return AjaxResult.success(list);
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "TQ";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.tq.machine.export.fileName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<TqMachineInfo> util = new ExcelUtil<>(TqMachineInfo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("导出胎圈机台信息")
    @GetMapping("/export")
    @RequiresPermissions("tq:machine:export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TqMachineInfo entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTqMachineInfoService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping("/importData")
    @RequiresPermissions("tq:machine:import")
    @ResponseBody
    @ApiOperation("导入胎圈机台信息")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return iTqMachineInfoService.importData(context, updateSupport);
    }
}
