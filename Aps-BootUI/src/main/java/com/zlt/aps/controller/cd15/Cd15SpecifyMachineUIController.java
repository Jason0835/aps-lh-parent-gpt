package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import com.zlt.aps.cd15.api.service.ICd15SpecifyMachineRemoteService;
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
 * 斜裁定点机台 UI 控制层。
 */
@Api(tags = "斜裁定点机台")
@Controller
@RequestMapping("/cd15/specifyMachine")
public class Cd15SpecifyMachineUIController extends BaseUIController<Cd15SpecifyMachine> {

    @Resource
    private ICd15SpecifyMachineRemoteService cd15SpecifyMachineRemoteService;

    /** 查询斜裁定点机台列表 */
    @ApiOperation("查询斜裁定点机台列表")
    @RequiresPermissions("cd15:specifyMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15SpecifyMachine queryVO) {
        return cd15SpecifyMachineRemoteService.list(queryVO);
    }

    /** 获取斜裁定点机台详情 */
    @ApiOperation("获取斜裁定点机台详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd15SpecifyMachine getInfo(@PathVariable("id") Long id) {
        return cd15SpecifyMachineRemoteService.getInfo(id);
    }

    /** 新增斜裁定点机台 */
    @ApiOperation("新增斜裁定点机台")
    @RequiresPermissions("cd15:specifyMachine:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd15SpecifyMachine specifyMachine) {
        if (UserConstants.NOT_UNIQUE.equals(cd15SpecifyMachineRemoteService.checkUnique(specifyMachine))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15SpecifyMachine.checkUnique"));
        }
        return cd15SpecifyMachineRemoteService.add(specifyMachine);
    }

    /** 编辑斜裁定点机台 */
    @ApiOperation("编辑斜裁定点机台")
    @RequiresPermissions("cd15:specifyMachine:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd15SpecifyMachine specifyMachine) {
        if (UserConstants.NOT_UNIQUE.equals(cd15SpecifyMachineRemoteService.checkUnique(specifyMachine))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15SpecifyMachine.checkUnique"));
        }
        return cd15SpecifyMachineRemoteService.edit(specifyMachine);
    }

    /** 校验斜裁定点机台唯一性 */
    @ApiOperation("校验斜裁定点机台唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(@RequestBody Cd15SpecifyMachine specifyMachine) {
        return cd15SpecifyMachineRemoteService.checkUnique(specifyMachine);
    }

    /** 删除斜裁定点机台 */
    @ApiOperation("删除斜裁定点机台")
    @RequiresPermissions("cd15:specifyMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cd15SpecifyMachineRemoteService.removeByIds(Arrays.asList(arr));
    }

    /** 清空斜裁定点机台 */
    @ApiOperation("清空斜裁定点机台")
    @RequiresPermissions("cd15:specifyMachine:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll(Cd15SpecifyMachine queryVO) {
        return cd15SpecifyMachineRemoteService.removeAll(queryVO);
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
        return I18nUtil.getMessage("ui.data.column.cd15SpecifyMachine.modelName");
    }

    /** 下载导入模板 */
    @ApiOperation("下载斜裁定点机台导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd15SpecifyMachine> util = new ExcelUtil<>(Cd15SpecifyMachine.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出斜裁定点机台 */
    @ApiOperation("导出斜裁定点机台")
    @RequiresPermissions("cd15:specifyMachine:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd15SpecifyMachine entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = cd15SpecifyMachineRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入斜裁定点机台 */
    @ApiOperation("导入斜裁定点机台")
    @RequiresPermissions("cd15:specifyMachine:import")
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
        return cd15SpecifyMachineRemoteService.importData(context, updateSupport);
    }
}