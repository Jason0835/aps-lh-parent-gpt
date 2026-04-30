package com.zlt.aps.controller.tq;


import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tq.api.domain.entity.TqToolingCartCapacity;
import com.zlt.aps.tq.api.service.ITqToolingCartCapacityService;
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
@RequestMapping("/tq/toolingCartCapacity")
@Api(tags = {"胎圈工装车容量管理接口"})
public class TqToolingCartCapacityUIController extends BaseUIController<TqToolingCartCapacity> {

    private final String prefix = "tq/toolingCartCapacity";

    @Autowired
    private ITqToolingCartCapacityService iTqToolingCartCapacityService;

    @RequiresPermissions("tq:toolingCartCapacity:view")
    @GetMapping()
    @ApiOperation("跳转到胎圈工装车容量管理首页")
    public String toIndex() {
        return prefix + "/toolingCartCapacity";
    }

    @RequiresPermissions("tq:toolingCartCapacity:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询胎圈工装车容量管理列表")
    public TableDataInfo list(TqToolingCartCapacity entity) {
        return iTqToolingCartCapacityService.list(entity);
    }

    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取胎圈工装车容量管理详细信息,跳转到编辑页面")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("ToolingCartCapacity", iTqToolingCartCapacityService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("跳转到胎圈工装车容量管理新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("ToolingCartCapacity", new TqToolingCartCapacity());
        return prefix + "/edit";
    }

    @RequiresPermissions("tq:toolingCartCapacity:edit")
    @PostMapping("/save")
    @ResponseBody
    @ApiOperation("保存胎圈工装车容量管理（id为空则新增，id不为空则修改）")
    public AjaxResult save(TqToolingCartCapacity entity) {
        return iTqToolingCartCapacityService.save(entity);
    }

    @RequiresPermissions("tq:toolingCartCapacity:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除胎圈工装车容量管理")
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqToolingCartCapacityService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("刪除全部")
    @RequiresPermissions("tq:toolingCartCapacity:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iTqToolingCartCapacityService.deleteAll();
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
        return I18nUtil.getMessage("ui.tq.toolingCartCapacity.column.modalName");
    }

    @RequiresPermissions("tq:toolingCartCapacity:export")
    @ApiOperation("导出胎圈工装车容量管理")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TqToolingCartCapacity entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTqToolingCartCapacityService.exportData(entity, fileName);
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
        ExcelUtil<TqToolingCartCapacity> util = new ExcelUtil<>(TqToolingCartCapacity.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("tq:toolingCartCapacity:import")
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
        return iTqToolingCartCapacityService.importData(context, updateSupport);
    }
}
