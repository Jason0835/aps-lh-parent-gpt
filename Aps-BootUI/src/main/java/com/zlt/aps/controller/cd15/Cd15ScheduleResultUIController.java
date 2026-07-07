package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import com.zlt.aps.cd15.api.service.ICd15ScheduleResultRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 斜裁排程结果 UI 控制层。
 */
@Api(tags = "斜裁排程结果")
@Controller
@RequestMapping("/cd15/cd15ScheduleResult")
public class Cd15ScheduleResultUIController extends BaseUIController<Cd15ScheduleResult> {

    @Resource
    private ICd15ScheduleResultRemoteService cd15ScheduleResultRemoteService;

    @ApiOperation("查询斜裁排程结果列表")
    @RequiresPermissions("cd15:cd15ScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15ScheduleResult queryVO) {
        return cd15ScheduleResultRemoteService.list(queryVO);
    }

    @ApiOperation("获取斜裁排程结果详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd15ScheduleResult getInfo(@PathVariable("id") Long id) {
        return cd15ScheduleResultRemoteService.getInfo(id);
    }

    @ApiOperation("删除斜裁排程结果")
    @RequiresPermissions("cd15:cd15ScheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return cd15ScheduleResultRemoteService.removeByIds(Arrays.asList(Convert.toLongArray(ids)));
    }

    @ApiOperation("斜裁自动排程")
    @RequiresPermissions("cd15:cd15ScheduleResult:autoSchedule")
    @PostMapping("/autoSchedule")
    @ResponseBody
    public AjaxResult autoSchedule(Cd15ScheduleResult scheduleResult) {
        return cd15ScheduleResultRemoteService.autoSchedule(scheduleResult);
    }

    @ApiOperation("查询斜裁自动排程任务")
    @RequiresPermissions("cd15:cd15ScheduleResult:autoSchedule")
    @GetMapping("/autoSchedule/task/{taskId}")
    @ResponseBody
    public AjaxResult getAutoScheduleTask(@PathVariable("taskId") String taskId) {
        return cd15ScheduleResultRemoteService.getAutoScheduleTask(taskId);
    }

    @ApiOperation("斜裁插单预校验")
    @RequiresPermissions("cd15:cd15ScheduleResult:insert")
    @PostMapping("/validateInsert")
    @ResponseBody
    public AjaxResult validateInsert(@RequestBody Cd15InsertOrderRequest request) {
        return cd15ScheduleResultRemoteService.validateInsert(request);
    }

    @ApiOperation("提交斜裁插单")
    @RequiresPermissions("cd15:cd15ScheduleResult:insert")
    @PostMapping("/insert")
    @ResponseBody
    public AjaxResult insert(@RequestBody Cd15InsertOrderRequest request) {
        return cd15ScheduleResultRemoteService.insert(request);
    }

    @ApiOperation("查询斜裁插单任务")
    @RequiresPermissions("cd15:cd15ScheduleResult:insert")
    @GetMapping("/insert/task/{taskId}")
    @ResponseBody
    public AjaxResult getInsertTask(@PathVariable("taskId") String taskId) {
        return cd15ScheduleResultRemoteService.getInsertTask(taskId);
    }

    @ApiOperation("斜裁转机台预校验")
    @RequiresPermissions("cd15:cd15ScheduleResult:changeMachine")
    @PostMapping("/validateTransferMachine")
    @ResponseBody
    public AjaxResult validateTransferMachine(@RequestBody Cd15TransferMachineRequest request) {
        return cd15ScheduleResultRemoteService.validateTransferMachine(request);
    }

    @ApiOperation("提交斜裁转机台")
    @RequiresPermissions("cd15:cd15ScheduleResult:changeMachine")
    @PostMapping("/transferMachine")
    @ResponseBody
    public AjaxResult transferMachine(@RequestBody Cd15TransferMachineRequest request) {
        return cd15ScheduleResultRemoteService.transferMachine(request);
    }

    @ApiOperation("查询斜裁转机台任务")
    @RequiresPermissions("cd15:cd15ScheduleResult:changeMachine")
    @GetMapping("/transferMachine/task/{taskId}")
    @ResponseBody
    public AjaxResult getTransferMachineTask(@PathVariable("taskId") String taskId) {
        return cd15ScheduleResultRemoteService.getTransferMachineTask(taskId);
    }

    @ApiOperation("斜裁调量预校验")
    @RequiresPermissions("cd15:cd15ScheduleResult:adjustQty")
    @PostMapping("/validateChangeQty")
    @ResponseBody
    public AjaxResult validateChangeQty(@RequestBody Cd15ChangeQtyRequest request) {
        return cd15ScheduleResultRemoteService.validateChangeQty(request);
    }

    @ApiOperation("提交斜裁调量")
    @RequiresPermissions("cd15:cd15ScheduleResult:adjustQty")
    @PostMapping("/changeQty")
    @ResponseBody
    public AjaxResult changeQty(@RequestBody Cd15ChangeQtyRequest request) {
        return cd15ScheduleResultRemoteService.changeQty(request);
    }

    @ApiOperation("查询斜裁调量任务")
    @RequiresPermissions("cd15:cd15ScheduleResult:adjustQty")
    @GetMapping("/changeQty/task/{taskId}")
    @ResponseBody
    public AjaxResult getChangeQtyTask(@PathVariable("taskId") String taskId) {
        return cd15ScheduleResultRemoteService.getChangeQtyTask(taskId);
    }

    @ApiOperation("发布斜裁排程结果")
    @RequiresPermissions("cd15:cd15ScheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(Cd15ScheduleResult dto, String ids) {
        return cd15ScheduleResultRemoteService.publish(dto, ids);
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
        return I18nUtil.getMessage("ui.data.column.cd15ScheduleResult.modalName");
    }

    @ApiOperation("下载斜裁排程结果导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd15ScheduleResult> util = new ExcelUtil<>(Cd15ScheduleResult.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("导出斜裁排程结果")
    @RequiresPermissions("cd15:cd15ScheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd15ScheduleResult entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = cd15ScheduleResultRemoteService.exportData(entity, fileName);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(new ByteArrayInputStream(excelBytes), response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入斜裁排程结果")
    @RequiresPermissions("cd15:cd15ScheduleResult:import")
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
        return cd15ScheduleResultRemoteService.importData(context, updateSupport);
    }
}
