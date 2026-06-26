package com.zlt.aps.controller.xwyy;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.xwyy.api.domain.entity.XwyyShiftConfig;
import com.zlt.aps.xwyy.api.service.IXwyyShiftConfigRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

@Api(tags = "纤维压延班次配置")
@Controller
@RequestMapping("/xwyy/xwyyShiftConfig")
public class XwyyShiftConfigUIController extends BaseUIController<XwyyShiftConfig> {
    @Resource
    private IXwyyShiftConfigRemoteService xwyyShiftConfigRemoteService;



    @ApiOperation("查询列表")
    @RequiresPermissions("xwyy:shiftConfig:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyShiftConfig query) {
        return xwyyShiftConfigRemoteService.list(query);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public XwyyShiftConfig getInfo(@PathVariable("id") Long id) {
        return xwyyShiftConfigRemoteService.getInfo(id);
    }

    @ApiOperation("新增")
    @RequiresPermissions("xwyy:shiftConfig:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody XwyyShiftConfig entity) {
        if (UserConstants.NOT_UNIQUE.equals(xwyyShiftConfigRemoteService.checkUnique(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.xwyyShiftConfig.checkUnique"));
        return xwyyShiftConfigRemoteService.add(entity);
    }

    @ApiOperation("编辑")
    @RequiresPermissions("xwyy:shiftConfig:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody XwyyShiftConfig entity) {
        if (UserConstants.NOT_UNIQUE.equals(xwyyShiftConfigRemoteService.checkUnique(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.xwyyShiftConfig.checkUnique"));
        return xwyyShiftConfigRemoteService.edit(entity);
    }

    @ApiOperation("删除")
    @RequiresPermissions("xwyy:shiftConfig:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return xwyyShiftConfigRemoteService.removeByIds(Arrays.asList(Convert.toLongArray(ids)));
    }

    @ApiOperation("启用/禁用")
    @RequiresPermissions("xwyy:shiftConfig:edit")
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(@RequestBody XwyyShiftConfig entity) {
        return xwyyShiftConfigRemoteService.changeStatus(entity);
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "XWYY";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.xwyyShiftConfig.modelName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<XwyyShiftConfig> excelUtil = new ExcelUtil<>(XwyyShiftConfig.class);
        excelUtil.exportExcel(response, null, getExportTemplateFileName(), getExportTemplateFileName());
        return AjaxResult.success();
    }

    @ApiOperation("导出")
    @RequiresPermissions("xwyy:shiftConfig:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, XwyyShiftConfig entity) throws IOException {
        byte[] data = xwyyShiftConfigRemoteService.exportData(entity, getExportTemplateFileName());
        ExcelUtil.setResponseHeader(response, getExportTemplateFileName(), ".xlsx");
        IOUtils.copy(new ByteArrayInputStream(data), response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入")
    @RequiresPermissions("xwyy:shiftConfig:import")
    @PostMapping("/importData")
    @ResponseBody
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] decodedBytes = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(getFunctionName());
        context.setProcedureCode(getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(decodedBytes);
        return xwyyShiftConfigRemoteService.importData(context, updateSupport);
    }
}
