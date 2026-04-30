package com.zlt.aps.controller.tq;


import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tq.api.domain.entity.TqTooling;
import com.zlt.aps.tq.api.service.ITqToolingService;
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
@RequestMapping("/tq/tooling")
@Api(tags = {"胎圈工装管理接口"})
public class TqToolingUIController extends BaseUIController<TqTooling> {

    private final String prefix = "tq/tooling";

    @Autowired
    private ITqToolingService iTqToolingService;

    @RequiresPermissions("tq:tooling:view")
    @GetMapping()
    @ApiOperation("跳转到胎圈工装管理首页")
    public String toIndex() {
        return prefix + "/tooling";
    }

    @RequiresPermissions("tq:tooling:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询胎圈工装管理列表")
    public TableDataInfo list(TqTooling entity) {
        return iTqToolingService.list(entity);
    }

    @PostMapping("/listAllTooling")
    @ResponseBody
    @ApiOperation("查询所有未删除的工装列表")
    public AjaxResult listAllTooling() {
        return iTqToolingService.listAllTooling();
    }

    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取胎圈工装管理详细信息,跳转到编辑页面")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("Tooling", iTqToolingService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("跳转到胎圈工装管理新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("Tooling", new TqTooling());
        return prefix + "/edit";
    }

    @RequiresPermissions("tq:tooling:edit")
    @PostMapping("/save")
    @ResponseBody
    @ApiOperation("保存胎圈工装管理（id为空则新增，id不为空则修改）")
    public AjaxResult save(TqTooling entity) {
        return iTqToolingService.save(entity);
    }

    @RequiresPermissions("tq:tooling:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除胎圈工装管理")
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqToolingService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("刪除全部")
    @RequiresPermissions("tq:tooling:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iTqToolingService.deleteAll();
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
        return I18nUtil.getMessage("ui.tq.tooling.column.modalName");
    }

    @RequiresPermissions("tq:tooling:export")
    @ApiOperation("导出胎圈工装管理")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TqTooling entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTqToolingService.exportData(entity, fileName);
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
        ExcelUtil<TqTooling> util = new ExcelUtil<>(TqTooling.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("tq:tooling:import")
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
        return iTqToolingService.importData(context, updateSupport);
    }
}
