package com.zlt.aps.controller.tq;


import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tq.api.domain.entity.TqMachineChuck;
import com.zlt.aps.tq.api.service.ITqMachineChuckService;
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
@Controller
@RequestMapping("/tq/machineChuck")
@Api(tags = {"胎圈机台寸口对应接口"})
public class TqMachineChuckUIController extends BaseUIController<TqMachineChuck> {

    private final String prefix = "tq/machineChuck";

    @Autowired
    private ITqMachineChuckService iTqMachineChuckService;

    @RequiresPermissions("tq:machineChuck:view")
    @GetMapping()
    @ApiOperation("跳转到机台寸口对应首页")
    public String toIndex() {
        return prefix + "/machineChuck";
    }

    @RequiresPermissions("tq:machineChuck:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询胎圈机台寸口对应列表")
    public TableDataInfo list(TqMachineChuck entity) {
        return iTqMachineChuckService.list(entity);
    }

    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取胎圈机台寸口对应详细信息,跳转到编辑页面")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("MachineChuck", iTqMachineChuckService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("跳转到机台寸口对应新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("MachineChuck", new TqMachineChuck());
        return prefix + "/edit";
    }

    @RequiresPermissions("tq:machineChuck:edit")
    @PostMapping("/save")
    @ResponseBody
    @ApiOperation("保存胎圈机台寸口对应（id为空则新增，id不为空则修改）")
    public AjaxResult save(TqMachineChuck entity) {
        return iTqMachineChuckService.save(entity);
    }

    @RequiresPermissions("tq:machineChuck:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除胎圈机台寸口对应")
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqMachineChuckService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("刪除全部")
    @RequiresPermissions("tq:machineChuck:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iTqMachineChuckService.deleteAll();
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
        return I18nUtil.getMessage("ui.tq.machineChuck.column.modalName");
    }

    @RequiresPermissions("tq:machineChuck:export")
    @ApiOperation("导出胎圈机台寸口对应")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TqMachineChuck entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTqMachineChuckService.exportData(entity, fileName);
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
        ExcelUtil<TqMachineChuck> util = new ExcelUtil<>(TqMachineChuck.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("tq:machineChuck:import")
    @ApiOperation("导入数据")
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
        return iTqMachineChuckService.importData(context, updateSupport);
    }
}
