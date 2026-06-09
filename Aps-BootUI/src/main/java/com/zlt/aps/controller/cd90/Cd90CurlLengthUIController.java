package com.zlt.aps.controller.cd90;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90CurlLength;
import com.zlt.aps.cd90.api.service.ICd90CurlLengthRemoteService;
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
 * 直裁卷曲长度 UI 控制层。
 */
@Api(tags = "直裁卷曲长度")
@Controller
@RequestMapping("/cd90/cd90CurlLength")
public class Cd90CurlLengthUIController extends BaseUIController<Cd90CurlLength> {

    @Resource
    private ICd90CurlLengthRemoteService cd90CurlLengthRemoteService;

    /** 查询直裁卷曲长度列表 */
    @ApiOperation("查询直裁卷曲长度列表")
    @RequiresPermissions("cd90:cd90CurlLength:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90CurlLength queryVO) {
        return cd90CurlLengthRemoteService.list(queryVO);
    }

    /** 获取直裁卷曲长度详情 */
    @ApiOperation("获取直裁卷曲长度详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd90CurlLength getInfo(@PathVariable("id") Long id) {
        return cd90CurlLengthRemoteService.getInfo(id);
    }

    /** 新增直裁卷曲长度 */
    @ApiOperation("新增直裁卷曲长度")
    @RequiresPermissions("cd90:cd90CurlLength:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd90CurlLength entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd90CurlLengthRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90CurlLength.checkUnique"));
        }
        return cd90CurlLengthRemoteService.add(entity);
    }

    /** 编辑直裁卷曲长度 */
    @ApiOperation("编辑直裁卷曲长度")
    @RequiresPermissions("cd90:cd90CurlLength:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd90CurlLength entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd90CurlLengthRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90CurlLength.checkUnique"));
        }
        return cd90CurlLengthRemoteService.edit(entity);
    }

    /** 删除直裁卷曲长度 */
    @ApiOperation("删除直裁卷曲长度")
    @RequiresPermissions("cd90:cd90CurlLength:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cd90CurlLengthRemoteService.removeByIds(Arrays.asList(arr));
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "CD90_CURL_LENGTH";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.cd90CurlLength.modelName");
    }

    /** 下载导入模板 */
    @ApiOperation("下载直裁卷曲长度导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd90CurlLength> util = new ExcelUtil<>(Cd90CurlLength.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出直裁卷曲长度 */
    @ApiOperation("导出直裁卷曲长度")
    @RequiresPermissions("cd90:cd90CurlLength:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd90CurlLength entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = cd90CurlLengthRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入直裁卷曲长度 */
    @ApiOperation("导入直裁卷曲长度")
    @RequiresPermissions("cd90:cd90CurlLength:import")
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
        return cd90CurlLengthRemoteService.importData(context, updateSupport);
    }
}