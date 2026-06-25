package com.zlt.aps.controller.cd90;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90DepthConfig;
import com.zlt.aps.cd90.api.service.ICd90DepthConfigRemoteService;
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

@Api(tags = "直裁备库班数与供成型机数配置")
@Controller
@RequestMapping("/cd90/cd90DepthConfig")
public class Cd90DepthConfigUIController extends BaseUIController<Cd90DepthConfig> {
    @Resource
    private ICd90DepthConfigRemoteService cd90DepthConfigRemoteService;

    @ApiOperation("查询列表")
    @RequiresPermissions("cd90:depthConfig:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90DepthConfig queryVO) {
        return cd90DepthConfigRemoteService.list(queryVO);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd90DepthConfig getInfo(@PathVariable("id") Long id) {
        return cd90DepthConfigRemoteService.getInfo(id);
    }

    @ApiOperation("新增")
    @RequiresPermissions("cd90:depthConfig:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd90DepthConfig entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd90DepthConfigRemoteService.checkUnique(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90DepthConfig.checkUnique"));
        if (UserConstants.NOT_UNIQUE.equals(cd90DepthConfigRemoteService.checkRangeCross(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90DepthConfig.rangeCross"));
        return cd90DepthConfigRemoteService.add(entity);
    }

    @ApiOperation("编辑")
    @RequiresPermissions("cd90:depthConfig:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd90DepthConfig entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd90DepthConfigRemoteService.checkUnique(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90DepthConfig.checkUnique"));
        if (UserConstants.NOT_UNIQUE.equals(cd90DepthConfigRemoteService.checkRangeCross(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90DepthConfig.rangeCross"));
        return cd90DepthConfigRemoteService.edit(entity);
    }

    @ApiOperation("删除")
    @RequiresPermissions("cd90:depthConfig:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return cd90DepthConfigRemoteService.removeByIds(Arrays.asList(Convert.toLongArray(ids)));
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
        return I18nUtil.getMessage("ui.data.column.cd90DepthConfig.modelName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<Cd90DepthConfig> excelUtil = new ExcelUtil<>(Cd90DepthConfig.class);
        excelUtil.exportExcel(response, null, getExportTemplateFileName(), getExportTemplateFileName());
        return AjaxResult.success();
    }

    @ApiOperation("导出")
    @RequiresPermissions("cd90:depthConfig:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd90DepthConfig entity) throws IOException {
        byte[] bytes = cd90DepthConfigRemoteService.exportData(entity, getExportTemplateFileName());
        ExcelUtil.setResponseHeader(response, getExportTemplateFileName(), ".xlsx");
        IOUtils.copy(new ByteArrayInputStream(bytes), response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入")
    @RequiresPermissions("cd90:depthConfig:import")
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
        return cd90DepthConfigRemoteService.importData(context, updateSupport);
    }
}