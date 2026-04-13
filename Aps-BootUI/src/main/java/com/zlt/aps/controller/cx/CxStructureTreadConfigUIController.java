package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.api.service.ICxStructureTreadConfigRemoteService;
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

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxStructureTreadConfigUIController.java
 * 描    述：胎面整车配置 UI控制层类
 *@author APS Team
 *@date 2026-04-02
 *@version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@Slf4j
@Api(tags = "胎面整车配置")
@Controller
@RequestMapping("/cx/cxStructureTreadConfig")
public class CxStructureTreadConfigUIController extends BaseUIController<CxStructureTreadConfig> {

    @Autowired
    private ICxStructureTreadConfigRemoteService iCxStructureTreadConfigService;

    private final String prefix = "aps/cx/cxStructureTreadConfig";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:cxStructureTreadConfig:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/cxStructureTreadConfig";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxStructureTreadConfig", new CxStructureTreadConfig());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxStructureTreadConfig", iCxStructureTreadConfigService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("cx:cxStructureTreadConfig:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxStructureTreadConfig cxStructureTreadConfig) {
        return iCxStructureTreadConfigService.list(cxStructureTreadConfig);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("cx:cxStructureTreadConfig:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(CxStructureTreadConfig cxStructureTreadConfig) {
        if (UserConstants.NOT_UNIQUE.equals(iCxStructureTreadConfigService.checkUnique(cxStructureTreadConfig))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.cxStructureTreadConfig.structureCodeNotUnique"));
        }

        return iCxStructureTreadConfigService.save(cxStructureTreadConfig);
    }

    /**
     * 删除胎面整车配置
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("cx:cxStructureTreadConfig:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxStructureTreadConfigService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验胎面整车配置唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(CxStructureTreadConfig cxStructureTreadConfig) {
        return iCxStructureTreadConfigService.checkUnique(cxStructureTreadConfig);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
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
        return I18nUtil.getMessage("ui.data.column.cxStructureTreadConfig.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<CxStructureTreadConfig> util = new ExcelUtil<>(CxStructureTreadConfig.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, CxStructureTreadConfig entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iCxStructureTreadConfigService.exportData(entity,fileName);
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
        AjaxResult ajaxResult = iCxStructureTreadConfigService.importData(context, updateSupport);
        return ajaxResult;
    }
}
