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
    private IXwyyShiftConfigRemoteService remote;

    @ApiOperation("查询列表")
    @RequiresPermissions("xwyy:shiftConfig:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyShiftConfig q) {
        return remote.list(q);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public XwyyShiftConfig getInfo(@PathVariable("id") Long id) {
        return remote.getInfo(id);
    }

    @ApiOperation("新增")
    @RequiresPermissions("xwyy:shiftConfig:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody XwyyShiftConfig e) {
        if (UserConstants.NOT_UNIQUE.equals(remote.checkUnique(e)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.xwyyShiftConfig.checkUnique"));
        return remote.add(e);
    }

    @ApiOperation("编辑")
    @RequiresPermissions("xwyy:shiftConfig:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody XwyyShiftConfig e) {
        if (UserConstants.NOT_UNIQUE.equals(remote.checkUnique(e)))
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.xwyyShiftConfig.checkUnique"));
        return remote.edit(e);
    }

    @ApiOperation("删除")
    @RequiresPermissions("xwyy:shiftConfig:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return remote.removeByIds(Arrays.asList(Convert.toLongArray(ids)));
    }

    @ApiOperation("启用/禁用")
    @RequiresPermissions("xwyy:shiftConfig:edit")
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(@RequestBody XwyyShiftConfig e) {
        return remote.changeStatus(e);
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
    public AjaxResult importTemplate(HttpServletResponse r) throws IOException {
        ExcelUtil<XwyyShiftConfig> u = new ExcelUtil<>(XwyyShiftConfig.class);
        u.exportExcel(r, null, getExportTemplateFileName(), getExportTemplateFileName());
        return AjaxResult.success();
    }

    @ApiOperation("导出")
    @RequiresPermissions("xwyy:shiftConfig:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse r, XwyyShiftConfig e) throws IOException {
        byte[] b = remote.exportData(e, getExportTemplateFileName());
        ExcelUtil.setResponseHeader(r, getExportTemplateFileName(), ".xlsx");
        IOUtils.copy(new ByteArrayInputStream(b), r.getOutputStream());
        r.flushBuffer();
    }

    @ApiOperation("导入")
    @RequiresPermissions("xwyy:shiftConfig:import")
    @PostMapping("/importData")
    @ResponseBody
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile f, boolean updateSupport) throws Exception {
        byte[] d = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(f) : f.getBytes();
        ImportContext c = new ImportContext();
        c.setImportFilePath(this.importFilePath);
        c.setFunctionName(getFunctionName());
        c.setProcedureCode(getProcedureCode());
        c.setOriFileName(f.getOriginalFilename());
        c.setFileBytes(d);
        return remote.importData(c, updateSupport);
    }
}
