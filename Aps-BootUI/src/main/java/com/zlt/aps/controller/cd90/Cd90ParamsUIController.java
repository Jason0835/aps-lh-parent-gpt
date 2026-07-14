package com.zlt.aps.controller.cd90;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90Params;
import com.zlt.aps.cd90.api.service.ICd90ParamsRemoteService;
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

@Api(tags = "直裁参数设置")
@Controller
@RequestMapping("/cd90/cd90Params")
public class Cd90ParamsUIController extends BaseUIController<Cd90Params> {

    @Resource
    private ICd90ParamsRemoteService remoteService;

    @ApiOperation("查询列表")
    @RequiresPermissions("cd90:params:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90Params queryVO) {
        return remoteService.list(queryVO);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd90Params getInfo(@PathVariable("id") Long id) {
        return remoteService.getInfo(id);
    }

    @ApiOperation("获取参数值")
    @GetMapping("/getParamValue/{factoryCode}/{paramCode}")
    @ResponseBody
    public AjaxResult getParamValue(@PathVariable("factoryCode") String factoryCode, @PathVariable("paramCode") String paramCode) {
        return remoteService.getParamValue(factoryCode, paramCode);
    }


    @ApiOperation("新增")
    @RequiresPermissions("cd90:params:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd90Params entity) {
        if (UserConstants.NOT_UNIQUE.equals(remoteService.checkUnique(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90Params.checkUnique"));
        return remoteService.add(entity);
    }

    @ApiOperation("编辑")
    @RequiresPermissions("cd90:params:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd90Params entity) {
        if (UserConstants.NOT_UNIQUE.equals(remoteService.checkUnique(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90Params.checkUnique"));
        return remoteService.edit(entity);
    }

    @ApiOperation("删除")
    @RequiresPermissions("cd90:params:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return remoteService.removeByIds(Arrays.asList(Convert.toLongArray(ids)));
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
        return I18nUtil.getMessage("ui.data.column.cd90Params.modelName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<Cd90Params> util = new ExcelUtil<>(Cd90Params.class);
        util.exportExcel(response, null, getExportTemplateFileName(), getExportTemplateFileName());
        return AjaxResult.success();
    }

    @ApiOperation("导出")
    @RequiresPermissions("cd90:params:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd90Params entity) throws IOException {
        byte[] excelBytes = remoteService.exportData(entity, getExportTemplateFileName());
        ExcelUtil.setResponseHeader(response, getExportTemplateFileName(), ".xlsx");
        IOUtils.copy(new ByteArrayInputStream(excelBytes), response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入")
    @RequiresPermissions("cd90:params:import")
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