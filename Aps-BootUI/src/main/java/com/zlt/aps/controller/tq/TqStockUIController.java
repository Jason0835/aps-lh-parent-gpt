package com.zlt.aps.controller.tq;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import com.zlt.aps.tq.api.service.ITqStockService;
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
@Api(tags = "胎圈库存信息")
@Controller
@RequestMapping("/tq/stock")
public class TqStockUIController extends BaseUIController<TqStock> {

    @Autowired
    private ITqStockService iTqStockService;

    private final String prefix = "tq/stock";

    @RequiresPermissions("tq:stock:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/stock";
    }

    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("stock", new TqStock());
        return prefix + "/edit";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", iTqStockService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("查询胎圈库存信息列表")
    @RequiresPermissions("tq:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TqStock stock) {
        return iTqStockService.list(stock);
    }

    @ApiOperation("修改或新增")
    @RequiresPermissions({"tq:stock:edit", "tq:stock:add"})
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(TqStock stock) {
        if (UserConstants.NOT_UNIQUE.equals(iTqStockService.checkUnique(stock))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.tqStock.notUnique"));
        }
        return iTqStockService.save(stock);
    }

    @ApiOperation("删除胎圈库存信息")
    @RequiresPermissions("tq:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTqStockService.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(TqStock stock) {
        return iTqStockService.checkUnique(stock);
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
        return I18nUtil.getMessage("ui.tq.stock.export.fileName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<TqStock> util = new ExcelUtil<>(TqStock.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("导出胎圈库存信息")
    @GetMapping("/export")
    @RequiresPermissions("tq:stock:export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TqStock entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTqStockService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping("/importData")
    @RequiresPermissions("tq:stock:import")
    @ResponseBody
    @ApiOperation("导入胎圈库存信息")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return iTqStockService.importData(context, updateSupport);
    }
}
