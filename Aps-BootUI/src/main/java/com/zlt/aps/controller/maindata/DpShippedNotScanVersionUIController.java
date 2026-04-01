package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.DpShippedNotScanVersion;
import com.zlt.aps.mp.api.service.IDpShippedNotScanVersionRemoteService;
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

@Slf4j
@Api(tags = "已出库未扫描版本")
@Controller
@RequestMapping("/monthplan/dpShippedNotScanVersion")
public class DpShippedNotScanVersionUIController extends BaseUIController<DpShippedNotScanVersion> {

    @Autowired
    private IDpShippedNotScanVersionRemoteService remoteService;

    @ApiOperation("根据条件查询主表数据")
//    @RequiresPermissions("monthplan:dpShippedNotScanVersion:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(DpShippedNotScanVersion entity) {
        return remoteService.list(entity);
    }

    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:dpShippedNotScanVersion:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(DpShippedNotScanVersion entity) {
        if (UserConstants.NOT_UNIQUE.equals(remoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.dpShippedNotScanVersion.notUnique"));
        }
        return remoteService.save(entity);
    }

    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:dpShippedNotScanVersion:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return remoteService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(DpShippedNotScanVersion entity) {
        return remoteService.checkUnique(entity);
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
        return I18nUtil.getMessage("ui.data.column.dpShippedNotScanVersion.modelName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<DpShippedNotScanVersion> util = new ExcelUtil<>(DpShippedNotScanVersion.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("monthplan:dpShippedNotScanVersion:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, DpShippedNotScanVersion entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = remoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("数据导入")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = remoteService.importData(context, updateSupport);
        return ajaxResult;
    }

    @ApiOperation("查询需求计划版本号")
    @PostMapping("/findMonthPlanVersion")
    @ResponseBody
    public AjaxResult findMonthPlanVersion(DpShippedNotScanVersion queryCondition) {
        return remoteService.findMonthPlanVersion(queryCondition);
    }

    @ApiOperation("生成已出库未扫描版本")
//    @RequiresPermissions("monthplan:dpShippedNotScanVersion:generate")
    @PostMapping("/generate")
    @ResponseBody
    public AjaxResult generate(DpShippedNotScanVersion queryCondition) {
        return remoteService.generate(queryCondition);
    }
}
