package com.zlt.aps.controller.xwyy;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import com.zlt.aps.xwyy.api.service.IXwyyScheduleResultRemoteService;
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

@Api(tags = "纤维压延排程结果")
@Controller
@RequestMapping("/xwyy/xwyyScheduleResult")
public class XwyyScheduleResultUIController extends BaseUIController<XwyyScheduleResult> {
    @Resource
    private IXwyyScheduleResultRemoteService xwyyScheduleResultRemoteService;

    @ApiOperation("查询列表")
    @RequiresPermissions("xwyy:scheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyScheduleResult query) {
        return xwyyScheduleResultRemoteService.list(query);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public XwyyScheduleResult getInfo(@PathVariable("id") Long id) {
        return xwyyScheduleResultRemoteService.getInfo(id);
    }

    @ApiOperation("删除")
    @RequiresPermissions("xwyy:scheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return xwyyScheduleResultRemoteService.removeByIds(Arrays.asList(Convert.toLongArray(ids)));
    }

    @ApiOperation("自动排程")
    @RequiresPermissions("xwyy:scheduleResult:autoSchedule")
    @PostMapping("/autoSchedule")
    @ResponseBody
    public AjaxResult autoSchedule(@RequestBody XwyyScheduleResult entity) {
        return xwyyScheduleResultRemoteService.autoSchedule(entity);
    }

    @ApiOperation("插单")
    @RequiresPermissions("xwyy:scheduleResult:insert")
    @PostMapping("/insert")
    @ResponseBody
    public AjaxResult insert(@RequestBody XwyyScheduleResult entity) {
        return xwyyScheduleResultRemoteService.insert(entity);
    }

    @ApiOperation("转机台")
    @RequiresPermissions("xwyy:scheduleResult:changeMachine")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(@RequestBody XwyyScheduleResult entity) {
        return xwyyScheduleResultRemoteService.changeMachine(entity);
    }

    @ApiOperation("调量")
    @RequiresPermissions("xwyy:scheduleResult:adjustQty")
    @PostMapping("/adjustQty")
    @ResponseBody
    public AjaxResult adjustQty(@RequestBody XwyyScheduleResult entity) {
        return xwyyScheduleResultRemoteService.adjustQty(entity);
    }

    @ApiOperation("发布")
    @RequiresPermissions("xwyy:scheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(@RequestBody XwyyScheduleResult entity) {
        return xwyyScheduleResultRemoteService.publish(entity);
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
        return I18nUtil.getMessage("ui.data.column.xwyyScheduleResult.modelName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<XwyyScheduleResult> excelUtil = new ExcelUtil<>(XwyyScheduleResult.class);
        excelUtil.exportExcel(response, null, getExportTemplateFileName(), getExportTemplateFileName());
        return AjaxResult.success();
    }

    @ApiOperation("导出")
    @RequiresPermissions("xwyy:scheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, XwyyScheduleResult entity) throws IOException {
        byte[] data = xwyyScheduleResultRemoteService.exportData(entity, getExportTemplateFileName());
        ExcelUtil.setResponseHeader(response, getExportTemplateFileName(), ".xlsx");
        IOUtils.copy(new ByteArrayInputStream(data), response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入")
    @RequiresPermissions("xwyy:scheduleResult:import")
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
        return xwyyScheduleResultRemoteService.importData(context, updateSupport);
    }
}
