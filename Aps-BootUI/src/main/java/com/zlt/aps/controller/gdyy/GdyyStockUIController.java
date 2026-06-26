package com.zlt.aps.controller.gdyy;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import com.zlt.aps.gdyy.api.service.IGdyyStockRemoteService;
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
 * 钢带压延库存 UI 控制层。
 */
@Api(tags = "钢带压延库存管理")
@Controller
@RequestMapping("/gdyy/stock")
public class GdyyStockUIController extends BaseUIController<GdyyStock> {

    @Resource
    private IGdyyStockRemoteService remote;

    @ApiOperation("查询钢带压延库存列表")
    @RequiresPermissions("gdyy:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GdyyStock queryVO) {
        return remote.list(queryVO);
    }

    @ApiOperation("获取钢带压延库存详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public GdyyStock getInfo(@PathVariable("id") Long id) {
        return remote.getInfo(id);
    }

    @ApiOperation("新增钢带压延库存")
    @RequiresPermissions("gdyy:stock:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody GdyyStock entity) {
        if (UserConstants.NOT_UNIQUE.equals(remote.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gdyyStock.checkUnique"));
        }
        return remote.add(entity);
    }

    @ApiOperation("编辑钢带压延库存")
    @RequiresPermissions("gdyy:stock:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody GdyyStock entity) {
        if (UserConstants.NOT_UNIQUE.equals(remote.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gdyyStock.checkUnique"));
        }
        return remote.edit(entity);
    }

    @ApiOperation("删除钢带压延库存")
    @RequiresPermissions("gdyy:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return remote.removeByIds(Arrays.asList(arr));
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
        return I18nUtil.getMessage("ui.data.column.gdyyStock.modelName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<GdyyStock> util = new ExcelUtil<>(GdyyStock.class);
        util.exportExcel(response, null, getExportTemplateFileName(), getExportTemplateFileName());
        return AjaxResult.success();
    }

    @ApiOperation("导出钢带压延库存")
    @RequiresPermissions("gdyy:stock:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, GdyyStock entity) throws IOException {
        byte[] excelBytes = remote.exportData(entity, getExportTemplateFileName());
        ExcelUtil.setResponseHeader(response, getExportTemplateFileName(), ".xlsx");
        IOUtils.copy(new ByteArrayInputStream(excelBytes), response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入钢带压延库存")
    @RequiresPermissions("gdyy:stock:import")
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
}
