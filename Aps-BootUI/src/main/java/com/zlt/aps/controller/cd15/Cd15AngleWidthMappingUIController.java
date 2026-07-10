package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd15.api.domain.entity.Cd15AngleWidthMapping;
import com.zlt.aps.cd15.api.service.ICd15AngleWidthMappingRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

@Api(tags = "CD15角度宽度对应关系")
@Controller
@RequestMapping("/cd15/angleWidthMapping")
public class Cd15AngleWidthMappingUIController extends BaseUIController<Cd15AngleWidthMapping> {

    @Resource
    private ICd15AngleWidthMappingRemoteService remoteService;

    @ApiOperation("查询列表")
    @RequiresPermissions("cd15:angleWidthMapping:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15AngleWidthMapping queryVO) {
        return remoteService.list(queryVO);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd15AngleWidthMapping getInfo(@PathVariable("id") Long id) {
        return remoteService.getInfo(id);
    }

    @ApiOperation("新增")
    @RequiresPermissions("cd15:angleWidthMapping:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd15AngleWidthMapping entity) {
        AjaxResult validationResult = this.validateBeforeSave(entity);
        if (validationResult != null) {
            return validationResult;
        }
        if (UserConstants.NOT_UNIQUE.equals(remoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15AngleWidthMapping.checkUnique"));
        }
        return remoteService.add(entity);
    }

    @ApiOperation("编辑")
    @RequiresPermissions("cd15:angleWidthMapping:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd15AngleWidthMapping entity) {
        AjaxResult validationResult = this.validateBeforeSave(entity);
        if (validationResult != null) {
            return validationResult;
        }
        if (UserConstants.NOT_UNIQUE.equals(remoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15AngleWidthMapping.checkUnique"));
        }
        return remoteService.edit(entity);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(@RequestBody Cd15AngleWidthMapping entity) {
        return remoteService.checkUnique(entity);
    }

    @ApiOperation("删除")
    @RequiresPermissions("cd15:angleWidthMapping:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return remoteService.removeByIds(Arrays.asList(Convert.toLongArray(ids)));
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
        return I18nUtil.getMessage("ui.data.column.cd15AngleWidthMapping.modelName");
    }

    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil<Cd15AngleWidthMapping> util = new ExcelUtil<>(Cd15AngleWidthMapping.class);
        util.exportExcel(response, null, getExportTemplateFileName(), getExportTemplateFileName());
        return AjaxResult.success();
    }

    @ApiOperation("导出")
    @RequiresPermissions("cd15:angleWidthMapping:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd15AngleWidthMapping entity) throws IOException {
        byte[] excelBytes = remoteService.exportData(entity, getExportTemplateFileName());
        ExcelUtil.setResponseHeader(response, getExportTemplateFileName(), ".xlsx");
        IOUtils.copy(new ByteArrayInputStream(excelBytes), response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导入")
    @RequiresPermissions("cd15:angleWidthMapping:import")
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
        return remoteService.importData(context, updateSupport);
    }

    /**
     * 保存前校验必要字段和最大宽度取值。
     */
    private AjaxResult validateBeforeSave(Cd15AngleWidthMapping entity) {
        if (entity == null || entity.getFactoryCode() == null || entity.getCutAngle() == null || entity.getClothWidthMax() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15AngleWidthMapping.required"));
        }
        if (entity.getClothWidthMax().compareTo(BigDecimal.ZERO) <= 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd15AngleWidthMapping.widthPositive"));
        }
        return null;
    }
}
