package com.zlt.aps.controller.cd90;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd90.api.domain.entity.Cd90SpecifyMachine;
import com.zlt.aps.cd90.api.service.ICd90SpecifyMachineRemoteService;
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
 * 直裁定点机台 UI 控制层。
 */
@Api(tags = "直裁定点机台")
@Controller
@RequestMapping("/cd90/specifyMachine")
public class Cd90SpecifyMachineUIController extends BaseUIController<Cd90SpecifyMachine> {

    @Resource
    private ICd90SpecifyMachineRemoteService cd90SpecifyMachineRemoteService;

    /** 查询直裁定点机台列表 */
    @ApiOperation("查询直裁定点机台列表")
    @RequiresPermissions("cd90:specifyMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90SpecifyMachine queryVO) {
        return cd90SpecifyMachineRemoteService.list(queryVO);
    }

    /** 获取直裁定点机台详情 */
    @ApiOperation("获取直裁定点机台详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public Cd90SpecifyMachine getInfo(@PathVariable("id") Long id) {
        return cd90SpecifyMachineRemoteService.getInfo(id);
    }

    /** 新增直裁定点机台 */
    @ApiOperation("新增直裁定点机台")
    @RequiresPermissions("cd90:specifyMachine:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Cd90SpecifyMachine specifyMachine) {
        if (UserConstants.NOT_UNIQUE.equals(cd90SpecifyMachineRemoteService.checkUnique(specifyMachine))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90SpecifyMachine.checkUnique"));
        }
        return cd90SpecifyMachineRemoteService.add(specifyMachine);
    }

    /** 编辑直裁定点机台 */
    @ApiOperation("编辑直裁定点机台")
    @RequiresPermissions("cd90:specifyMachine:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody Cd90SpecifyMachine specifyMachine) {
        if (UserConstants.NOT_UNIQUE.equals(cd90SpecifyMachineRemoteService.checkUnique(specifyMachine))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cd90SpecifyMachine.checkUnique"));
        }
        return cd90SpecifyMachineRemoteService.edit(specifyMachine);
    }

    /** 校验直裁定点机台唯一性 */
    @ApiOperation("校验直裁定点机台唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(@RequestBody Cd90SpecifyMachine specifyMachine) {
        return cd90SpecifyMachineRemoteService.checkUnique(specifyMachine);
    }

    /** 删除直裁定点机台 */
    @ApiOperation("删除直裁定点机台")
    @RequiresPermissions("cd90:specifyMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cd90SpecifyMachineRemoteService.removeByIds(Arrays.asList(arr));
    }

    /** 清空直裁定点机台 */
    @ApiOperation("清空直裁定点机台")
    @RequiresPermissions("cd90:specifyMachine:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll(Cd90SpecifyMachine queryVO) {
        return cd90SpecifyMachineRemoteService.removeAll(queryVO);
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
        return I18nUtil.getMessage("ui.data.column.cd90SpecifyMachine.modelName");
    }

    /** 下载导入模板 */
    @ApiOperation("下载直裁定点机台导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<Cd90SpecifyMachine> util = new ExcelUtil<>(Cd90SpecifyMachine.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出直裁定点机台 */
    @ApiOperation("导出直裁定点机台")
    @RequiresPermissions("cd90:specifyMachine:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, Cd90SpecifyMachine entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = cd90SpecifyMachineRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入直裁定点机台 */
    @ApiOperation("导入直裁定点机台")
    @RequiresPermissions("cd90:specifyMachine:import")
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
        return cd90SpecifyMachineRemoteService.importData(context, updateSupport);
    }
}
