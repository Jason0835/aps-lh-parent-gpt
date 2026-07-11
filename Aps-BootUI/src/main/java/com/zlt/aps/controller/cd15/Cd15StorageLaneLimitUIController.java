package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.cd15.api.service.ICd15StorageLaneLimitRemoteService;
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
 * 斜裁库排限制 UI 控制层。
 */
@Api(tags = "斜裁库排限制")
@Controller
@RequestMapping("/cd15/cd15StorageLaneLimit")
public class Cd15StorageLaneLimitUIController extends BaseUIController<Cd15StorageLaneLimit> {

    @Resource
    private ICd15StorageLaneLimitRemoteService cd15StorageLaneLimitRemoteService;

    /** 查询斜裁库排限制列表 */
    @ApiOperation("查询斜裁库排限制列表")
    @RequiresPermissions("cd15:storageLaneLimit:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15StorageLaneLimit queryVO) {
        return cd15StorageLaneLimitRemoteService.list(queryVO);
    }

    /** 获取斜裁库排限制详情 */
    @ApiOperation("获取斜裁库排限制详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd15StorageLaneLimit getInfo(@PathVariable("id") Long id) {
        return cd15StorageLaneLimitRemoteService.getInfo(id);
    }

    /** 新增斜裁库排限制 */
    @ApiOperation("新增斜裁库排限制")
    @RequiresPermissions("cd15:storageLaneLimit:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd15StorageLaneLimit entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd15StorageLaneLimitRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15StorageLaneLimit.checkUnique"));
        }
        return cd15StorageLaneLimitRemoteService.add(entity);
    }

    /** 编辑斜裁库排限制 */
    @ApiOperation("编辑斜裁库排限制")
    @RequiresPermissions("cd15:storageLaneLimit:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd15StorageLaneLimit entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd15StorageLaneLimitRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15StorageLaneLimit.checkUnique"));
        }
        return cd15StorageLaneLimitRemoteService.edit(entity);
    }

    /** 删除斜裁库排限制 */
    @ApiOperation("删除斜裁库排限制")
    @RequiresPermissions("cd15:storageLaneLimit:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] idArray = Convert.toLongArray(ids);
        return cd15StorageLaneLimitRemoteService.removeByIds(Arrays.asList(idArray));
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
        return I18nUtil.getMessage("ui.data.column.cd15StorageLaneLimit.modelName");
    }

    /** 下载导入模板 */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd15StorageLaneLimit> util = new ExcelUtil<>(Cd15StorageLaneLimit.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出斜裁库排限制 */
    @ApiOperation("导出斜裁库排限制")
    @RequiresPermissions("cd15:storageLaneLimit:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd15StorageLaneLimit entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = cd15StorageLaneLimitRemoteService.exportData(entity, fileName);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入斜裁库排限制 */
    @ApiOperation("导入斜裁库排限制")
    @RequiresPermissions("cd15:storageLaneLimit:import")
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
        return cd15StorageLaneLimitRemoteService.importData(context, updateSupport);
    }
}