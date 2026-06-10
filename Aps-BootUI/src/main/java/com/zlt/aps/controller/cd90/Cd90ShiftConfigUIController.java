package com.zlt.aps.controller.cd90;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.api.service.ICd90ShiftConfigRemoteService;
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
 * 班次配置 UI 控制层。
 */
@Api(tags = "班次配置")
@Controller
@RequestMapping("/cd90/cd90ShiftConfig")
public class Cd90ShiftConfigUIController extends BaseUIController<Cd90ShiftConfig> {

    @Resource
    private ICd90ShiftConfigRemoteService cd90ShiftConfigRemoteService;

    /** 查询班次配置列表 */
    @ApiOperation("查询班次配置列表")
    @RequiresPermissions("cd90:shiftConfig:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90ShiftConfig queryVO) {
        return cd90ShiftConfigRemoteService.list(queryVO);
    }

    /** 获取班次配置详情 */
    @ApiOperation("获取班次配置详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd90ShiftConfig getInfo(@PathVariable("id") Long id) {
        return cd90ShiftConfigRemoteService.getInfo(id);
    }

    /** 新增班次配置 */
    @ApiOperation("新增班次配置")
    @RequiresPermissions("cd90:shiftConfig:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd90ShiftConfig shiftConfig) {
        if (UserConstants.NOT_UNIQUE.equals(cd90ShiftConfigRemoteService.checkUnique(shiftConfig))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90ShiftConfig.checkUnique"));
        }
        return cd90ShiftConfigRemoteService.add(shiftConfig);
    }

    /** 编辑班次配置 */
    @ApiOperation("编辑班次配置")
    @RequiresPermissions("cd90:shiftConfig:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd90ShiftConfig shiftConfig) {
        if (UserConstants.NOT_UNIQUE.equals(cd90ShiftConfigRemoteService.checkUnique(shiftConfig))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90ShiftConfig.checkUnique"));
        }
        return cd90ShiftConfigRemoteService.edit(shiftConfig);
    }

    /** 删除班次配置 */
    @ApiOperation("删除班次配置")
    @RequiresPermissions("cd90:shiftConfig:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cd90ShiftConfigRemoteService.removeByIds(Arrays.asList(arr));
    }

    /** 修改班次启用状态 */
    @ApiOperation("修改班次启用状态")
    @RequiresPermissions("cd90:shiftConfig:edit")
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(@RequestBody Cd90ShiftConfig shiftConfig) {
        return cd90ShiftConfigRemoteService.changeStatus(shiftConfig);
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
        return I18nUtil.getMessage("ui.data.column.cd90ShiftConfig.modelName");
    }

    /** 下载导入模板 */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd90ShiftConfig> util = new ExcelUtil<>(Cd90ShiftConfig.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出班次配置 */
    @ApiOperation("导出班次配置")
    @RequiresPermissions("cd90:shiftConfig:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd90ShiftConfig entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = cd90ShiftConfigRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入班次配置 */
    @ApiOperation("导入班次配置")
    @RequiresPermissions("cd90:shiftConfig:import")
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
        return cd90ShiftConfigRemoteService.importData(context, updateSupport);
    }
}