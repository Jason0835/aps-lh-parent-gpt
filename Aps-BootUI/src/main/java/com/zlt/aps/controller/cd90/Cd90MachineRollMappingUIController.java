package com.zlt.aps.controller.cd90;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineRollMapping;
import com.zlt.aps.cd90.api.service.ICd90MachineRollMappingRemoteService;
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
 * 直裁大卷与机台映射 UI 控制层。
 */
@Api(tags = "直裁大卷与机台映射")
@Controller
@RequestMapping("/cd90/cd90MachineRollMapping")
public class Cd90MachineRollMappingUIController extends BaseUIController<Cd90MachineRollMapping> {

    @Resource
    private ICd90MachineRollMappingRemoteService cd90MachineRollMappingRemoteService;

    /** 查询直裁大卷与机台映射列表 */
    @ApiOperation("查询直裁大卷与机台映射列表")
    @RequiresPermissions("cd90:machineRollMapping:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90MachineRollMapping queryVO) {
        return cd90MachineRollMappingRemoteService.list(queryVO);
    }

    /** 获取直裁大卷与机台映射详情 */
    @ApiOperation("获取直裁大卷与机台映射详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd90MachineRollMapping getInfo(@PathVariable("id") Long id) {
        return cd90MachineRollMappingRemoteService.getInfo(id);
    }

    /** 新增直裁大卷与机台映射 */
    @ApiOperation("新增直裁大卷与机台映射")
    @RequiresPermissions("cd90:machineRollMapping:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd90MachineRollMapping entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd90MachineRollMappingRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90MachineRollMapping.checkUnique"));
        }
        return cd90MachineRollMappingRemoteService.add(entity);
    }

    /** 编辑直裁大卷与机台映射 */
    @ApiOperation("编辑直裁大卷与机台映射")
    @RequiresPermissions("cd90:machineRollMapping:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd90MachineRollMapping entity) {
        if (UserConstants.NOT_UNIQUE.equals(cd90MachineRollMappingRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90MachineRollMapping.checkUnique"));
        }
        return cd90MachineRollMappingRemoteService.edit(entity);
    }

    /** 校验直裁大卷与机台映射唯一性 */
    @ApiOperation("校验直裁大卷与机台映射唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(@RequestBody Cd90MachineRollMapping entity) {
        return cd90MachineRollMappingRemoteService.checkUnique(entity);
    }

    /** 删除直裁大卷与机台映射 */
    @ApiOperation("删除直裁大卷与机台映射")
    @RequiresPermissions("cd90:machineRollMapping:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cd90MachineRollMappingRemoteService.removeByIds(Arrays.asList(arr));
    }

    /** 清空直裁大卷与机台映射 */
    @ApiOperation("清空直裁大卷与机台映射")
    @RequiresPermissions("cd90:machineRollMapping:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll(Cd90MachineRollMapping queryVO) {
        return cd90MachineRollMappingRemoteService.removeAll(queryVO);
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
        return I18nUtil.getMessage("ui.data.column.cd90MachineRollMapping.modelName");
    }

    /** 下载导入模板 */
    @ApiOperation("下载直裁大卷与机台映射导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd90MachineRollMapping> util = new ExcelUtil<>(Cd90MachineRollMapping.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出直裁大卷与机台映射 */
    @ApiOperation("导出直裁大卷与机台映射")
    @RequiresPermissions("cd90:machineRollMapping:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd90MachineRollMapping entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = cd90MachineRollMappingRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入直裁大卷与机台映射 */
    @ApiOperation("导入直裁大卷与机台映射")
    @RequiresPermissions("cd90:machineRollMapping:import")
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
        return cd90MachineRollMappingRemoteService.importData(context, updateSupport);
    }
}
