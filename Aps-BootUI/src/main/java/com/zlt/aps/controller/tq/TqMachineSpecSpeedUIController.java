package com.zlt.aps.controller.tq;


import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tq.api.domain.entity.TqMachineSpecSpeed;
import com.zlt.aps.tq.api.service.ITqMachineSpecSpeedService;
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
@Api(tags = {"胎圈机台生产速度接口"})
@Controller
@RequestMapping("/tq/machineSpecSpeed")
public class TqMachineSpecSpeedUIController extends BaseUIController<TqMachineSpecSpeed> {

    private String prefix = "tq/machineSpecSpeed";

    @Autowired
    private ITqMachineSpecSpeedService iTqMachineSpecSpeedService;

    @RequiresPermissions("tq:machineSpecSpeed:view")
    @GetMapping()
    public String machineSpecSpeed() {
        return prefix + "/machineSpecSpeed";
    }

    @ApiOperation("根据条件查询胎圈机台生产速度列表")
    @RequiresPermissions("tq:machineSpecSpeed:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TqMachineSpecSpeed entity) {
        return iTqMachineSpecSpeedService.list(entity);
    }

    @ApiOperation("跳转到胎圈机台生产速度新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("machineSpecSpeed", new TqMachineSpecSpeed());
        return prefix + "/edit";
    }

    @ApiOperation("获取胎圈机台生产速度信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("machineSpecSpeed", iTqMachineSpecSpeedService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("保存胎圈机台生产速度(id为空则新增，id不为空则修改)")
    @RequiresPermissions({"tq:machineSpecSpeed:add", "tq:machineSpecSpeed:edit"})
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(TqMachineSpecSpeed entity) {
        return iTqMachineSpecSpeedService.save(entity);
    }

    @ApiOperation("删除胎圈机台生产速度")
    @RequiresPermissions("tq:machineSpecSpeed:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqMachineSpecSpeedService.removeByIds(Arrays.asList(arr));
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "TQ";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.tq.machineSpecSpeed.column.modalName");
    }

    @RequiresPermissions("tq:machineSpecSpeed:export")
    @ApiOperation("导出胎圈机台生产速度")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TqMachineSpecSpeed entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTqMachineSpecSpeedService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("下载模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<TqMachineSpecSpeed> util = new ExcelUtil<>(TqMachineSpecSpeed.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("tq:machineSpecSpeed:import")
    @ApiOperation("数据导入")
    @PostMapping("/importData")
    @ResponseBody
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return iTqMachineSpecSpeedService.importData(context, updateSupport);
    }
}
