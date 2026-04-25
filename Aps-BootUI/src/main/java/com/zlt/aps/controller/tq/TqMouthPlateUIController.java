package com.zlt.aps.controller.tq;


import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tq.api.domain.entity.TqMouthPlate;
import com.zlt.aps.tq.api.service.ITqMouthPlateService;
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
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/tq/mouthPlate")
@Api(tags = {"胎圈口型板信息接口"})
public class TqMouthPlateUIController extends BaseUIController<TqMouthPlate> {

    private final String prefix = "tq/mouthPlate";

    @Autowired
    private ITqMouthPlateService iTqMouthPlateService;

    @RequiresPermissions("tq:mouthPlate:view")
    @GetMapping()
    @ApiOperation("跳转到口型板信息首页")
    public String toIndex() {
        return prefix + "/mouthPlate";
    }

    @RequiresPermissions("tq:mouthPlate:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询胎圈口型板信息维护列表")
    public TableDataInfo list(TqMouthPlate entity) {
        return iTqMouthPlateService.list(entity);
    }

    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取胎圈口型板信息详细信息,跳转到编辑页面")
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("MouthPlate", iTqMouthPlateService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("跳转到胎圈口型板新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("MouthPlate", new TqMouthPlate());
        return prefix + "/edit";
    }

    @RequiresPermissions("tq:mouthPlate:edit")
    @PostMapping("/save")
    @ResponseBody
    @ApiOperation("保存胎圈口型板信息（id为空则新增，id不为空则修改）")
    public AjaxResult save(TqMouthPlate entity) {
        return iTqMouthPlateService.save(entity);
    }

    @RequiresPermissions("tq:mouthPlate:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除胎圈口型板信息")
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqMouthPlateService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("刪除全部")
    @RequiresPermissions("tq:mouthPlate:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iTqMouthPlateService.deleteAll();
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
        return I18nUtil.getMessage("ui.data.column.tq.mouthPlate.modelName");
    }

    @RequiresPermissions("tq:mouthPlate:export")
    @ApiOperation("导出胎圈口型板信息")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TqMouthPlate entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTqMouthPlateService.exportData(entity, fileName);
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
        ExcelUtil<TqMouthPlate> util = new ExcelUtil<>(TqMouthPlate.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("tq:mouthPlate:import")
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
        return iTqMouthPlateService.importData(context, updateSupport);
    }
}
