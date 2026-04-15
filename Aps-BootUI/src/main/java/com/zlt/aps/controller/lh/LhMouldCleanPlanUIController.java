package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanPlan;
import com.zlt.aps.lh.api.service.ILhMachineInfoRemoteService;
import com.zlt.aps.lh.api.service.ILhMouldCleanPlanRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@Api(tags = "模具清洗计划")
@Controller
@RequestMapping("/lh/mouldCleanPlan")
public class LhMouldCleanPlanUIController extends BaseUIController<LhMouldCleanPlan> {

    @Autowired
    private ILhMouldCleanPlanRemoteService iLhMouldCleanPlanService;

    @Autowired
    private ILhMachineInfoRemoteService iLhMachineInfoService;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhMouldCleanPlan query) {
        return iLhMouldCleanPlanService.list(query);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iLhMouldCleanPlanService.getInfo(id));
    }

    @ApiOperation("保存")
    @PostMapping("/save")
    @RequiresPermissions("lh:mouldCleanPlan:edit")
    @ResponseBody
    public AjaxResult save(LhMouldCleanPlan lhMouldCleanPlan) {
        return iLhMouldCleanPlanService.save(lhMouldCleanPlan);
    }

    @ApiOperation("删除")
    @PostMapping("/remove")
    @RequiresPermissions("lh:mouldCleanPlan:remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhMouldCleanPlanService.removeByIds(arr);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("lh:mouldCleanPlan:export")
    public void export(HttpServletResponse response, LhMouldCleanPlan entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.mouldCleanPlan.modelName");
        byte[] excelBytes = iLhMouldCleanPlanService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(@RequestParam("file") MultipartFile file, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setFunctionName(I18nUtil.getMessage("ui.data.column.mouldCleanPlan.modelName"));
        context.setProcedureCode(I18nUtil.getMessage("ui.data.column.mouldCleanPlan.modelName"));
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iLhMouldCleanPlanService.importData(context, updateSupport);
        return ajaxResult;
    }

    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.mouldCleanPlan.modelName");
        ExcelUtil<LhMouldCleanPlan> util = new ExcelUtil<>(LhMouldCleanPlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("获取机台下拉列表")
    @PostMapping("/getMachineList")
    @ResponseBody
    public AjaxResult getMachineList(LhMachineInfo query) {
        TableDataInfo tableDataInfo = iLhMachineInfoService.list(query);
        return AjaxResult.success(tableDataInfo.getRows());
    }

    @ApiOperation("从模具清洗预警同步生成计划")
    @PostMapping("/syncFromWarn")
    @RequiresPermissions("lh:mouldCleanPlan:sync")
    @ResponseBody
    public AjaxResult syncFromWarn() {
        return iLhMouldCleanPlanService.syncFromWarn();
    }
}
