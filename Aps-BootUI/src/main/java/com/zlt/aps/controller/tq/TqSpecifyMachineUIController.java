package com.zlt.aps.controller.tq;


import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tq.api.domain.entity.TqSpecifyMachine;
import com.zlt.aps.tq.api.service.ITqSpecifyMachineService;
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
@Api(tags = {"胎圈定点机台维护接口"})
@Controller
@RequestMapping("/tq/specifyMachine")
public class TqSpecifyMachineUIController extends BaseUIController<TqSpecifyMachine> {

    private String prefix = "tq/specifyMachine";

    @Autowired
    private ITqSpecifyMachineService iTqSpecifyMachineService;

    @RequiresPermissions("tq:specifyMachine:view")
    @GetMapping()
    public String specifyMachine() {
        return prefix + "/specifyMachine";
    }

    @ApiOperation("根据条件查询定点机台列表")
    @RequiresPermissions("tq:specifyMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TqSpecifyMachine entity) {
        return iTqSpecifyMachineService.list(entity);
    }

    @ApiOperation("跳转到定点机台新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("specifyMachine", new TqSpecifyMachine());
        return prefix + "/edit";
    }

    @ApiOperation("获取定点机台信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("specifyMachine", iTqSpecifyMachineService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改定点机台(id为空则进行新增，id不为空则进行修改)")
    @RequiresPermissions("tq:specifyMachine:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(TqSpecifyMachine entity) {
        return iTqSpecifyMachineService.save(entity);
    }

    @ApiOperation("刪除定点机台")
    @RequiresPermissions("tq:specifyMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqSpecifyMachineService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("刪除全部定点机台")
    @RequiresPermissions("tq:specifyMachine:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iTqSpecifyMachineService.deleteAll();
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
        return I18nUtil.getMessage("ui.tq.specifyMachine.column.modalName");
    }

    @RequiresPermissions("tq:specifyMachine:export")
    @ApiOperation("导出定点机台")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TqSpecifyMachine entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTqSpecifyMachineService.exportData(entity, fileName);
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
        ExcelUtil<TqSpecifyMachine> util = new ExcelUtil<>(TqSpecifyMachine.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("tq:specifyMachine:import")
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
        return iTqSpecifyMachineService.importData(context, updateSupport);
    }
}
