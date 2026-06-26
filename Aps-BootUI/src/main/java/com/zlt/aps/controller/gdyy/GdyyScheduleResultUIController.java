package com.zlt.aps.controller.gdyy;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.api.service.IGdyyScheduleResultRemoteService;
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
 * 钢带压延排程结果 UI 控制层。
 */
@Api(tags = "钢带压延排程结果")
@Controller
@RequestMapping("/gdyy/scheduleResult")
public class GdyyScheduleResultUIController extends BaseUIController<GdyyScheduleResult> {

    @Resource
    private IGdyyScheduleResultRemoteService remote;

    @ApiOperation("查询钢带压延排程结果列表")
    @RequiresPermissions("gdyy:scheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GdyyScheduleResult queryVO) {
        return remote.list(queryVO);
    }

    @ApiOperation("获取钢带压延排程结果详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public GdyyScheduleResult getInfo(@PathVariable("id") Long id) {
        return remote.getInfo(id);
    }

    @ApiOperation("新增/编辑钢带压延排程结果")
    @RequiresPermissions("gdyy:scheduleResult:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody GdyyScheduleResult entity) {
        if (UserConstants.NOT_UNIQUE.equals(remote.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gdyyScheduleResult.checkUnique"));
        }
        return remote.edit(entity);
    }

    @ApiOperation("删除钢带压延排程结果")
    @RequiresPermissions("gdyy:scheduleResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return remote.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("调量")
    @RequiresPermissions("gdyy:scheduleResult:changePlan")
    @PostMapping("/changeQty")
    @ResponseBody
    public AjaxResult changeQty(@RequestBody GdyyScheduleResult entity) {
        return remote.changeQty(entity);
    }

    @ApiOperation("转机台")
    @RequiresPermissions("gdyy:scheduleResult:changeMachine")
    @PostMapping("/changeMachine")
    @ResponseBody
    public AjaxResult changeMachine(@RequestBody GdyyScheduleResult entity) {
        return remote.changeMachine(entity);
    }

    @ApiOperation("发布")
    @RequiresPermissions("gdyy:scheduleResult:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(@RequestBody GdyyScheduleResult entity) {
        return remote.publish(entity);
    }

    @ApiOperation("更改发布状态")
    @RequiresPermissions("admin")
    @PostMapping("/changeReleaseStatus")
    @ResponseBody
    public AjaxResult changeReleaseStatus(@RequestBody GdyyScheduleResult entity) {
        return remote.changeReleaseStatus(entity);
    }

    @ApiOperation("获取合计信息")
    @RequiresPermissions("gdyy:scheduleResult:list")
    @PostMapping("/getSummaryVo")
    @ResponseBody
    public AjaxResult getSummaryVo(@RequestBody GdyyScheduleResult queryVO) {
        return remote.getSummaryVo(queryVO);
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "GDYY";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.gdyyScheduleResult.modelName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<GdyyScheduleResult> util = new ExcelUtil<>(GdyyScheduleResult.class);
        util.exportExcel(response, null, getExportTemplateFileName(), getExportTemplateFileName());
        return AjaxResult.success();
    }

    @ApiOperation("导出钢带压延排程结果")
    @RequiresPermissions("gdyy:scheduleResult:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, GdyyScheduleResult entity) throws IOException {
        byte[] excelBytes = remote.exportData(entity, getExportTemplateFileName());
        ExcelUtil.setResponseHeader(response, getExportTemplateFileName(), ".xlsx");
        IOUtils.copy(new ByteArrayInputStream(excelBytes), response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入钢带压延排程结果")
    @RequiresPermissions("gdyy:scheduleResult:import")
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
        return remote.importData(context, updateSupport);
    }

    @ApiOperation("导入完成量")
    @RequiresPermissions("gdyy:finishQty:import")
    @PostMapping("/importFinishQty")
    @ResponseBody
    public AjaxResult importFinishQty(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(getFunctionName());
        context.setProcedureCode(getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return remote.importFinishQty(context, updateSupport);
    }
}
