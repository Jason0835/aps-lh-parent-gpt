package com.zlt.aps.controller.cd90;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.service.ICd90ScheduleResultRemoteService;
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

@Api(tags = "直裁排程结果")
@Controller
@RequestMapping("/cd90/cd90ScheduleResult")
public class Cd90ScheduleResultUIController extends BaseUIController<Cd90ScheduleResult> {

    @Resource private ICd90ScheduleResultRemoteService remoteService;

    @ApiOperation("查询列表") @RequiresPermissions("cd90:scheduleResult:list") @PostMapping("/list") @ResponseBody
    public TableDataInfo list(Cd90ScheduleResult queryVO) { return remoteService.list(queryVO); }

    @ApiOperation("获取详情") @GetMapping("/getInfo/{id}") @ResponseBody
    public Cd90ScheduleResult getInfo(@PathVariable("id") Long id) { return remoteService.getInfo(id); }

    @ApiOperation("删除") @RequiresPermissions("cd90:scheduleResult:remove") @PostMapping("/remove") @ResponseBody
    public AjaxResult remove(String ids) { return remoteService.removeByIds(Arrays.asList(Convert.toLongArray(ids))); }

    /**
     * 自动生成直裁排程结果。
     *
     * @param scheduleResult 自动排程条件，当前使用工厂编码和排程日期
     * @return 自动排程调用结果
     */
    @ApiOperation("自动排程")
    @RequiresPermissions("cd90:scheduleResult:autoSchedule")
    @PostMapping("/autoSchedule")
    @ResponseBody
    public AjaxResult autoSchedule(Cd90ScheduleResult scheduleResult) {
        return remoteService.autoSchedule(scheduleResult);
    }

    @Override public String getExportTemplateFileName() { return getFunctionName(); }
    @Override public String getProcedureCode() { return "CD90"; }
    @Override public String getFunctionName() { return I18nUtil.getMessage("ui.data.column.cd90ScheduleResult.modelName"); }

    @ApiOperation("下载导入模板") @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<Cd90ScheduleResult> util = new ExcelUtil<>(Cd90ScheduleResult.class);
        util.exportExcel(response, null, getExportTemplateFileName(), getExportTemplateFileName());
        return AjaxResult.success();
    }

    @ApiOperation("导出") @RequiresPermissions("cd90:scheduleResult:export") @GetMapping("/export") @ResponseBody @Override
    public void export(HttpServletResponse response, Cd90ScheduleResult entity) throws IOException {
        byte[] excelBytes = remoteService.exportData(entity, getExportTemplateFileName());
        ExcelUtil.setResponseHeader(response, getExportTemplateFileName(), ".xlsx");
        IOUtils.copy(new ByteArrayInputStream(excelBytes), response.getOutputStream()); response.flushBuffer();
    }

    @ApiOperation("导入") @RequiresPermissions("cd90:scheduleResult:import") @PostMapping("/importData") @ResponseBody @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath); context.setFunctionName(getFunctionName());
        context.setProcedureCode(getProcedureCode()); context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return remoteService.importData(context, updateSupport);
    }
}
