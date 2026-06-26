package com.zlt.aps.controller.gdyy;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.gdyy.api.domain.entity.GdyyShiftConfig;
import com.zlt.aps.gdyy.api.service.IGdyyShiftConfigRemoteService;
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
 * 钢带压延班次配置 UI 控制层。
 */
@Api(tags = "钢带压延班次配置")
@Controller
@RequestMapping("/gdyy/gdyyShiftConfig")
public class GdyyShiftConfigUIController extends BaseUIController<GdyyShiftConfig> {

    @Resource
    private IGdyyShiftConfigRemoteService remote;

    @ApiOperation("查询钢带压延班次配置列表")
    @RequiresPermissions("gdyy:shiftConfig:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GdyyShiftConfig queryVO) {
        return remote.list(queryVO);
    }

    @ApiOperation("获取钢带压延班次配置详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public GdyyShiftConfig getInfo(@PathVariable("id") Long id) {
        return remote.getInfo(id);
    }

    @ApiOperation("新增钢带压延班次配置")
    @RequiresPermissions("gdyy:shiftConfig:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody GdyyShiftConfig shiftConfig) {
        if (UserConstants.NOT_UNIQUE.equals(remote.checkUnique(shiftConfig))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gdyyShiftConfig.checkUnique"));
        }
        return remote.add(shiftConfig);
    }

    @ApiOperation("编辑钢带压延班次配置")
    @RequiresPermissions("gdyy:shiftConfig:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody GdyyShiftConfig shiftConfig) {
        if (UserConstants.NOT_UNIQUE.equals(remote.checkUnique(shiftConfig))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gdyyShiftConfig.checkUnique"));
        }
        return remote.edit(shiftConfig);
    }

    @ApiOperation("删除钢带压延班次配置")
    @RequiresPermissions("gdyy:shiftConfig:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return remote.removeByIds(Arrays.asList(arr));
    }

    @ApiOperation("修改钢带压延班次启用状态")
    @RequiresPermissions("gdyy:shiftConfig:edit")
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(@RequestBody GdyyShiftConfig shiftConfig) {
        return remote.changeStatus(shiftConfig);
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
        return I18nUtil.getMessage("ui.data.column.gdyyShiftConfig.modelName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<GdyyShiftConfig> util = new ExcelUtil<>(GdyyShiftConfig.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("导出钢带压延班次配置")
    @RequiresPermissions("gdyy:shiftConfig:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, GdyyShiftConfig entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = remote.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入钢带压延班次配置")
    @RequiresPermissions("gdyy:shiftConfig:import")
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
