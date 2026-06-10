package com.zlt.aps.controller.cd90;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90LossSetting;
import com.zlt.aps.cd90.api.service.ICd90LossSettingRemoteService;
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
 * 直裁损耗率设定 UI 控制层。
 */
@Api(tags = "直裁损耗率设定")
@Controller
@RequestMapping("/cd90/cd90LossSetting")
public class Cd90LossSettingUIController extends BaseUIController<Cd90LossSetting> {

    @Resource
    private ICd90LossSettingRemoteService cd90LossSettingRemoteService;

    @ApiOperation("查询直裁损耗率列表")
    @RequiresPermissions("cd90:loss:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90LossSetting queryVO) {
        return cd90LossSettingRemoteService.list(queryVO);
    }

    @ApiOperation("获取直裁损耗率详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd90LossSetting getInfo(@PathVariable("id") Long id) {
        return cd90LossSettingRemoteService.getInfo(id);
    }

    @ApiOperation("新增直裁损耗率")
    @RequiresPermissions("cd90:loss:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd90LossSetting entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd90LossSettingRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90LossSetting.checkUnique"));
        }
        return cd90LossSettingRemoteService.add(entity);
    }

    @ApiOperation("编辑直裁损耗率")
    @RequiresPermissions("cd90:loss:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd90LossSetting entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd90LossSettingRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90LossSetting.checkUnique"));
        }
        return cd90LossSettingRemoteService.edit(entity);
    }

    @ApiOperation("删除直裁损耗率")
    @RequiresPermissions("cd90:loss:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cd90LossSettingRemoteService.removeByIds(Arrays.asList(arr));
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "CD90";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.cd90LossSetting.modelName");
    }

    @ApiOperation("下载直裁损耗率导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd90LossSetting> util = new ExcelUtil<>(Cd90LossSetting.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("导出直裁损耗率")
    @RequiresPermissions("cd90:loss:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd90LossSetting entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = cd90LossSettingRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入直裁损耗率")
    @RequiresPermissions("cd90:loss:import")
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
        return cd90LossSettingRemoteService.importData(context, updateSupport);
    }
}