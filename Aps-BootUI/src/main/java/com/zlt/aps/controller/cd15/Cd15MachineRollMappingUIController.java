package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import com.zlt.aps.cd15.api.service.ICd15MachineRollMappingRemoteService;
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
 * 斜裁大卷与机台映射 UI 控制层。
 */
@Api(tags = "斜裁大卷与机台映射")
@Controller
@RequestMapping("/cd15/machineRollMapping")
public class Cd15MachineRollMappingUIController extends BaseUIController<Cd15MachineRollMapping> {

    @Resource
    private ICd15MachineRollMappingRemoteService cd15MachineRollMappingRemoteService;

    /** 查询斜裁大卷与机台映射列表 */
    @ApiOperation("查询斜裁大卷与机台映射列表")
    @RequiresPermissions("cd15:machineRollMapping:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15MachineRollMapping queryVO) {
        return cd15MachineRollMappingRemoteService.list(queryVO);
    }

    /** 获取斜裁大卷与机台映射详情 */
    @ApiOperation("获取斜裁大卷与机台映射详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd15MachineRollMapping getInfo(@PathVariable("id") Long id) {
        return cd15MachineRollMappingRemoteService.getInfo(id);
    }

    /** 新增斜裁大卷与机台映射 */
    @ApiOperation("新增斜裁大卷与机台映射")
    @RequiresPermissions("cd15:machineRollMapping:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd15MachineRollMapping entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd15MachineRollMappingRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15MachineRollMapping.checkUnique"));
        }
        return cd15MachineRollMappingRemoteService.add(entity);
    }

    /** 编辑斜裁大卷与机台映射 */
    @ApiOperation("编辑斜裁大卷与机台映射")
    @RequiresPermissions("cd15:machineRollMapping:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd15MachineRollMapping entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd15MachineRollMappingRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15MachineRollMapping.checkUnique"));
        }
        return cd15MachineRollMappingRemoteService.edit(entity);
    }

    /** 校验斜裁大卷与机台映射唯一性 */
    @ApiOperation("校验斜裁大卷与机台映射唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(@RequestBody Cd15MachineRollMapping entity) {
        return cd15MachineRollMappingRemoteService.checkUnique(entity);
    }

    /** 删除斜裁大卷与机台映射 */
    @ApiOperation("删除斜裁大卷与机台映射")
    @RequiresPermissions("cd15:machineRollMapping:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cd15MachineRollMappingRemoteService.removeByIds(Arrays.asList(arr));
    }

    /** 清空斜裁大卷与机台映射 */
    @ApiOperation("清空斜裁大卷与机台映射")
    @RequiresPermissions("cd15:machineRollMapping:remove")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll(Cd15MachineRollMapping queryVO) {
        return cd15MachineRollMappingRemoteService.removeAll(queryVO);
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
        return I18nUtil.getMessage("ui.data.column.cd15MachineRollMapping.modelName");
    }

    /** 下载导入模板 */
    @ApiOperation("下载斜裁大卷与机台映射导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd15MachineRollMapping> util = new ExcelUtil<>(Cd15MachineRollMapping.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出斜裁大卷与机台映射 */
    @ApiOperation("导出斜裁大卷与机台映射")
    @RequiresPermissions("cd15:machineRollMapping:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd15MachineRollMapping entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = cd15MachineRollMappingRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入斜裁大卷与机台映射 */
    @ApiOperation("导入斜裁大卷与机台映射")
    @RequiresPermissions("cd15:machineRollMapping:import")
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
        return cd15MachineRollMappingRemoteService.importData(context, updateSupport);
    }
}
