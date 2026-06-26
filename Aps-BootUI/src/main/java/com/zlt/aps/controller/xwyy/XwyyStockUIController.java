package com.zlt.aps.controller.xwyy;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;
import com.zlt.aps.xwyy.api.service.IXwyyStockRemoteService;
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

@Api(tags = "纤维压延库存管理")
@Controller
@RequestMapping("/xwyy/xwyyStock")
public class XwyyStockUIController extends BaseUIController<XwyyStock> {
    @Resource
    private IXwyyStockRemoteService xwyyStockRemoteService;

    @ApiOperation("查询列表")
    @RequiresPermissions("xwyy:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyStock query) {
        return xwyyStockRemoteService.list(query);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public XwyyStock getInfo(@PathVariable("id") Long id) {
        return xwyyStockRemoteService.getInfo(id);
    }

    @ApiOperation("新增")
    @RequiresPermissions("xwyy:stock:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody XwyyStock entity) {
        if (UserConstants.NOT_UNIQUE.equals(xwyyStockRemoteService.checkUnique(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.xwyyStock.checkUnique"));
        return xwyyStockRemoteService.add(entity);
    }

    @ApiOperation("编辑")
    @RequiresPermissions("xwyy:stock:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody XwyyStock entity) {
        if (UserConstants.NOT_UNIQUE.equals(xwyyStockRemoteService.checkUnique(entity)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.xwyyStock.checkUnique"));
        return xwyyStockRemoteService.edit(entity);
    }

    @ApiOperation("删除")
    @RequiresPermissions("xwyy:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return xwyyStockRemoteService.removeByIds(Arrays.asList(Convert.toLongArray(ids)));
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
        return I18nUtil.getMessage("ui.data.column.xwyyStock.modelName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<XwyyStock> excelUtil = new ExcelUtil<>(XwyyStock.class);
        excelUtil.exportExcel(response, null, getExportTemplateFileName(), getExportTemplateFileName());
        return AjaxResult.success();
    }

    @ApiOperation("导出")
    @RequiresPermissions("xwyy:stock:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, XwyyStock entity) throws IOException {
        byte[] data = xwyyStockRemoteService.exportData(entity, getExportTemplateFileName());
        ExcelUtil.setResponseHeader(response, getExportTemplateFileName(), ".xlsx");
        IOUtils.copy(new ByteArrayInputStream(data), response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入")
    @RequiresPermissions("xwyy:stock:import")
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
        return xwyyStockRemoteService.importData(context, updateSupport);
    }
}