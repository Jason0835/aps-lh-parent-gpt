package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd15.api.domain.entity.Cd15DepthConfig;
import com.zlt.aps.cd15.api.service.ICd15DepthConfigRemoteService;
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
 * 斜裁备库班数与供成型机数配置 UI 控制层。
 */
@Api(tags = "斜裁备库班数与供成型机数配置")
@Controller
@RequestMapping("/cd15/cd15DepthConfig")
public class Cd15DepthConfigUIController extends BaseUIController<Cd15DepthConfig> {
    @Resource
    private ICd15DepthConfigRemoteService cd15DepthConfigRemoteService;

    @ApiOperation("查询斜裁备库班数列表")
    @RequiresPermissions("cd15:depthConfig:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15DepthConfig queryVO) {
        return cd15DepthConfigRemoteService.list(queryVO);
    }

    @ApiOperation("获取斜裁备库班数详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd15DepthConfig getInfo(@PathVariable("id") Long id) {
        return cd15DepthConfigRemoteService.getInfo(id);
    }

    @ApiOperation("新增斜裁备库班数")
    @RequiresPermissions("cd15:depthConfig:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd15DepthConfig entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd15DepthConfigRemoteService.checkUnique(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15DepthConfig.checkUnique"));
        if (UserConstants.NOT_UNIQUE.equals(cd15DepthConfigRemoteService.checkRangeCross(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15DepthConfig.rangeCross"));
        return cd15DepthConfigRemoteService.add(entity);
    }

    @ApiOperation("编辑斜裁备库班数")
    @RequiresPermissions("cd15:depthConfig:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd15DepthConfig entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd15DepthConfigRemoteService.checkUnique(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15DepthConfig.checkUnique"));
        if (UserConstants.NOT_UNIQUE.equals(cd15DepthConfigRemoteService.checkRangeCross(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15DepthConfig.rangeCross"));
        return cd15DepthConfigRemoteService.edit(entity);
    }

    @ApiOperation("删除斜裁备库班数")
    @RequiresPermissions("cd15:depthConfig:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return cd15DepthConfigRemoteService.removeByIds(Arrays.asList(Convert.toLongArray(ids)));
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
        return I18nUtil.getMessage("ui.data.column.cd15DepthConfig.modelName");
    }

    @ApiOperation("下载斜裁备库班数导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<Cd15DepthConfig> excelUtil = new ExcelUtil<>(Cd15DepthConfig.class);
        excelUtil.exportExcel(response, null, getExportTemplateFileName(), getExportTemplateFileName());
        return AjaxResult.success();
    }

    @ApiOperation("导出斜裁备库班数")
    @RequiresPermissions("cd15:depthConfig:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd15DepthConfig entity) throws IOException {
        byte[] bytes = cd15DepthConfigRemoteService.exportData(entity, getExportTemplateFileName());
        ExcelUtil.setResponseHeader(response, getExportTemplateFileName(), ".xlsx");
        IOUtils.copy(new ByteArrayInputStream(bytes), response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入斜裁备库班数")
    @RequiresPermissions("cd15:depthConfig:import")
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
        return cd15DepthConfigRemoteService.importData(context, updateSupport);
    }
}
