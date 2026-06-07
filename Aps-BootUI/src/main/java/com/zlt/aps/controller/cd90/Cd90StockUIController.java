package com.zlt.aps.controller.cd90;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.cd90.api.service.ICd90StockRemoteService;
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

@Api(tags = "直裁库存管理")
@Controller
@RequestMapping("/cd90/cd90Stock")
public class Cd90StockUIController extends BaseUIController<Cd90Stock> {
    @Resource private ICd90StockRemoteService remote;

    @ApiOperation("查询列表") @RequiresPermissions("cd90:stock:list") @PostMapping("/list") @ResponseBody public TableDataInfo list(Cd90Stock q) { return remote.list(q); }
    @ApiOperation("获取详情") @GetMapping("/getInfo/{id}") @ResponseBody public Cd90Stock getInfo(@PathVariable("id") Long id) { return remote.getInfo(id); }
    @ApiOperation("新增") @RequiresPermissions("cd90:stock:add") @PostMapping("/add") @ResponseBody
    public AjaxResult add(@RequestBody Cd90Stock e) { if (UserConstants.NOT_UNIQUE.equals(remote.checkUnique(e))) return AjaxResult.error(I18nUtil.getMessage("ui.data.column.stock.checkUnique")); return remote.add(e); }
    @ApiOperation("编辑") @RequiresPermissions("cd90:stock:edit") @PostMapping("/edit") @ResponseBody
    public AjaxResult edit(@RequestBody Cd90Stock e) { if (UserConstants.NOT_UNIQUE.equals(remote.checkUnique(e))) return AjaxResult.error(I18nUtil.getMessage("ui.data.column.stock.checkUnique")); return remote.edit(e); }
    @ApiOperation("删除") @RequiresPermissions("cd90:stock:remove") @PostMapping("/remove") @ResponseBody public AjaxResult remove(String ids) { return remote.removeByIds(Arrays.asList(Convert.toLongArray(ids))); }
    @Override public String getExportTemplateFileName() { return getFunctionName(); }
    @Override public String getProcedureCode() { return "CD90_STOCK"; }
    @Override public String getFunctionName() { return I18nUtil.getMessage("ui.data.column.stock.modelName"); }
    @ApiOperation("下载导入模板") @Override public AjaxResult importTemplate(HttpServletResponse r) throws IOException { ExcelUtil<Cd90Stock> u = new ExcelUtil<>(Cd90Stock.class); u.exportExcel(r, null, getExportTemplateFileName(), getExportTemplateFileName()); return AjaxResult.success(); }
    @ApiOperation("导出") @RequiresPermissions("cd90:stock:export") @GetMapping("/export") @ResponseBody @Override public void export(HttpServletResponse r, Cd90Stock e) throws IOException { byte[] b = remote.exportData(e, getExportTemplateFileName()); IOUtils.copy(new ByteArrayInputStream(b), r.getOutputStream()); r.flushBuffer(); }
    @ApiOperation("导入") @RequiresPermissions("cd90:stock:import") @PostMapping("/importData") @ResponseBody @Override public AjaxResult importData(@RequestPart("file") MultipartFile f, boolean updateSupport) throws Exception { byte[] d = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(f) : f.getBytes(); ImportContext c = new ImportContext(); c.setImportFilePath(this.importFilePath); c.setFunctionName(getFunctionName()); c.setProcedureCode(getProcedureCode()); c.setOriFileName(f.getOriginalFilename()); c.setFileBytes(d); return remote.importData(c, updateSupport); }
}