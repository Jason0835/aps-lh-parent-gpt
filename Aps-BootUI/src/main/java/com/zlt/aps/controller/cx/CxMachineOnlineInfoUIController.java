package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.api.service.ICxMachineOnlineInfoRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 成型在机信息 UI 控制层
 */
@Slf4j
@Api(tags = "成型在机信息")
@Controller
@RequestMapping("/cx/cxMachineOnlineInfo")
public class CxMachineOnlineInfoUIController extends BaseUIController<CxMachineOnlineInfo> {

    @Autowired
    private ICxMachineOnlineInfoRemoteService iCxMachineOnlineInfoService;

    private final String prefix = "aps/cx/cxMachineOnlineInfo";

    @RequiresPermissions("cx:cxMachineOnlineInfo:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/cxMachineOnlineInfo";
    }

    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxMachineOnlineInfo", new CxMachineOnlineInfo());
        return prefix + "/add";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxMachineOnlineInfo", iCxMachineOnlineInfoService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("查询列表")
    @RequiresPermissions("cx:cxMachineOnlineInfo:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxMachineOnlineInfo cxMachineOnlineInfo) {
        return iCxMachineOnlineInfoService.list(cxMachineOnlineInfo);
    }

    @ApiOperation("保存")
    @RequiresPermissions("cx:cxMachineOnlineInfo:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(CxMachineOnlineInfo cxMachineOnlineInfo) {
        if (UserConstants.NOT_UNIQUE.equals(iCxMachineOnlineInfoService.checkUnique(cxMachineOnlineInfo))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cxMachineOnlineInfo.checkUnique"));
        }
        return iCxMachineOnlineInfoService.save(cxMachineOnlineInfo);
    }

    @ApiOperation("删除")
    @RequiresPermissions("cx:cxMachineOnlineInfo:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxMachineOnlineInfoService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("\u6821\u9A8C\u552F\u4E00\u6027")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(CxMachineOnlineInfo cxMachineOnlineInfo) {
        return iCxMachineOnlineInfoService.checkUnique(cxMachineOnlineInfo);
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "0";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.cxMachineOnlineInfo.modelName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<CxMachineOnlineInfo> util = new ExcelUtil<>(CxMachineOnlineInfo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("导出")
    @GetMapping({"/export"})
    @RequiresPermissions("cx:cxMachineOnlineInfo:export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, CxMachineOnlineInfo entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iCxMachineOnlineInfoService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
    @RequiresPermissions("cx:cxMachineOnlineInfo:import")
    @ResponseBody
    @ApiOperation("导入")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return iCxMachineOnlineInfoService.importData(context, updateSupport);
    }
}

