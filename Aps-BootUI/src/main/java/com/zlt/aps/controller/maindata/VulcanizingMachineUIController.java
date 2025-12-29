package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.VulcanizingMachine;
import com.zlt.aps.monthplan.api.service.IVulcanizingMachineRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：VulcanizingMachineUIController.java
 * 描    述：基础数据-硫化机档案 UI控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@Controller
@RequestMapping("/mdm/vulcanizingMachine")
@Api(tags = "硫化机信息管理")
public class VulcanizingMachineUIController extends BaseUIController<VulcanizingMachine> {

    private final IVulcanizingMachineRemoteService iVulcanizingMachineService;

    public VulcanizingMachineUIController(IVulcanizingMachineRemoteService iVulcanizingMachineService) {
        this.iVulcanizingMachineService = iVulcanizingMachineService;
    }

    private final String prefix = "aps/monthplan/VulcanizingMachine";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:VulcanizingMachine:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/VulcanizingMachine";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("vulcanizingMachine", new VulcanizingMachine());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("vulcanizingMachine", iVulcanizingMachineService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:VulcanizingMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(VulcanizingMachine vulcanizingMachine) {
        return iVulcanizingMachineService.list(vulcanizingMachine);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:VulcanizingMachine:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(VulcanizingMachine vulcanizingMachine) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(iVulcanizingMachineService.checkUnique(vulcanizingMachine))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.vulcanizingMachine.notUnique"));
        }

        return iVulcanizingMachineService.save(vulcanizingMachine);
    }

    /**
     * 删除基础数据-硫化机档案
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:VulcanizingMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iVulcanizingMachineService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验基础数据-硫化机档案唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(VulcanizingMachine vulcanizingMachine) {
        return iVulcanizingMachineService.checkUnique(vulcanizingMachine);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.VulcanizingMachine.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<VulcanizingMachine> util = new ExcelUtil<>(VulcanizingMachine.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, VulcanizingMachine entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iVulcanizingMachineService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("数据导入")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iVulcanizingMachineService.importData(context, updateSupport);
        return ajaxResult;
    }
}
