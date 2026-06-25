package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.api.service.ICd15CurlLengthRemoteService;
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
 * 斜裁卷曲长度 UI 控制层。
 */
@Api(tags = "斜裁卷曲长度")
@Controller
@RequestMapping("/cd15/curlLength")
public class Cd15CurlLengthUIController extends BaseUIController<Cd15CurlLength> {

    @Resource
    private ICd15CurlLengthRemoteService cd15CurlLengthRemoteService;

    /** 查询斜裁卷曲长度列表 */
    @ApiOperation("查询斜裁卷曲长度列表")
    @RequiresPermissions("cd15:curlLength:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15CurlLength queryVO) {
        return cd15CurlLengthRemoteService.list(queryVO);
    }

    /** 获取斜裁卷曲长度详情 */
    @ApiOperation("获取斜裁卷曲长度详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd15CurlLength getInfo(@PathVariable("id") Long id) {
        return cd15CurlLengthRemoteService.getInfo(id);
    }

    /** 新增斜裁卷曲长度 */
    @ApiOperation("新增斜裁卷曲长度")
    @RequiresPermissions("cd15:curlLength:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd15CurlLength entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd15CurlLengthRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15CurlLength.checkUnique"));
        }
        return cd15CurlLengthRemoteService.add(entity);
    }

    /** 编辑斜裁卷曲长度 */
    @ApiOperation("编辑斜裁卷曲长度")
    @RequiresPermissions("cd15:curlLength:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd15CurlLength entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd15CurlLengthRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15CurlLength.checkUnique"));
        }
        return cd15CurlLengthRemoteService.edit(entity);
    }

    /** 删除斜裁卷曲长度 */
    @ApiOperation("删除斜裁卷曲长度")
    @RequiresPermissions("cd15:curlLength:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cd15CurlLengthRemoteService.removeByIds(Arrays.asList(arr));
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
        return I18nUtil.getMessage("ui.data.column.cd15CurlLength.modelName");
    }

    /** 下载导入模板 */
    @ApiOperation("下载斜裁卷曲长度导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd15CurlLength> util = new ExcelUtil<>(Cd15CurlLength.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出斜裁卷曲长度 */
    @ApiOperation("导出斜裁卷曲长度")
    @RequiresPermissions("cd15:curlLength:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd15CurlLength entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = cd15CurlLengthRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入斜裁卷曲长度 */
    @ApiOperation("导入斜裁卷曲长度")
    @RequiresPermissions("cd15:curlLength:import")
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
        return cd15CurlLengthRemoteService.importData(context, updateSupport);
    }
}