package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd15.api.domain.entity.Cd15LossSetting;
import com.zlt.aps.cd15.api.service.ICd15LossSettingRemoteService;
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
 * 斜裁损耗率设定 UI 控制层。
 */
@Api(tags = "斜裁损耗率设定")
@Controller
@RequestMapping("/cd15/cd15LossSetting")
public class Cd15LossSettingUIController extends BaseUIController<Cd15LossSetting> {

    @Resource
    private ICd15LossSettingRemoteService cd15LossSettingRemoteService;

    @ApiOperation("查询斜裁损耗率列表")
    @RequiresPermissions("cd15:lossSetting:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15LossSetting queryVO) {
        return cd15LossSettingRemoteService.list(queryVO);
    }

    @ApiOperation("获取斜裁损耗率详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd15LossSetting getInfo(@PathVariable("id") Long id) {
        return cd15LossSettingRemoteService.getInfo(id);
    }

    @ApiOperation("新增斜裁损耗率")
    @RequiresPermissions("cd15:lossSetting:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd15LossSetting entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd15LossSettingRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15LossSetting.checkUnique"));
        }
        return cd15LossSettingRemoteService.add(entity);
    }

    @ApiOperation("编辑斜裁损耗率")
    @RequiresPermissions("cd15:lossSetting:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd15LossSetting entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd15LossSettingRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15LossSetting.checkUnique"));
        }
        return cd15LossSettingRemoteService.edit(entity);
    }

    @ApiOperation("删除斜裁损耗率")
    @RequiresPermissions("cd15:lossSetting:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cd15LossSettingRemoteService.removeByIds(Arrays.asList(arr));
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "CD15";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.cd15LossSetting.modelName");
    }

    @ApiOperation("下载斜裁损耗率导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd15LossSetting> util = new ExcelUtil<>(Cd15LossSetting.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("导出斜裁损耗率")
    @RequiresPermissions("cd15:lossSetting:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd15LossSetting entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = cd15LossSettingRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入斜裁损耗率")
    @RequiresPermissions("cd15:lossSetting:import")
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
        return cd15LossSettingRemoteService.importData(context, updateSupport);
    }
}
