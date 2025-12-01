package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cd15.api.domain.entity.HalfCdImportBak;
import com.zlt.aps.cd15.api.service.IHalfCdImportBakRemoteService;
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
 * 文件名称：HalfCdImportBakUIController.java
 * 描    述：裁断线下计划导入导出 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-29
 */
@Slf4j
@Api(tags = "裁断线下计划导入导出")
@Controller
@RequestMapping("/cd15/halfCdImportBak")
public class HalfCdImportBakUIController extends BaseUIController<HalfCdImportBak> {

    private final String prefix = "aps/cd15/halfCdImportBak";
    @Autowired
    private IHalfCdImportBakRemoteService iHalfCdImportBakService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cd15:halfCdImportBak:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/halfCdImportBak";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("halfCdImportBak", new HalfCdImportBak());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("halfCdImportBak", iHalfCdImportBakService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("cd15:halfCdImportBak:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(HalfCdImportBak halfCdImportBak) {
        return iHalfCdImportBakService.list(halfCdImportBak);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("cd15:halfCdImportBak:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(HalfCdImportBak halfCdImportBak) {
        if (UserConstants.NOT_UNIQUE.equals(iHalfCdImportBakService.checkUnique(halfCdImportBak))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.halfCdImportBak.checkUnique"));
        }

        return iHalfCdImportBakService.save(halfCdImportBak);
    }

    /**
     * 删除裁断线下计划导入导出
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("cd15:halfCdImportBak:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iHalfCdImportBakService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验裁断线下计划导入导出唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(HalfCdImportBak halfCdImportBak) {
        return iHalfCdImportBakService.checkUnique(halfCdImportBak);
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
        return I18nUtil.getMessage("ui.data.column.halfCdImportBak.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<HalfCdImportBak> util = new ExcelUtil<>(HalfCdImportBak.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("cd15:halfCdImportBak:importExcelToListAndExport")
    @ApiOperation("数据导出")
    @PostMapping({"/importExcelToListAndExport"})
    @ResponseBody
    public void importExcelToListAndExport(@RequestPart("file") MultipartFile file, HttpServletResponse response) throws IOException {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iHalfCdImportBakService.importExcelToListAndExport(context);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @RequiresPermissions("cd15:halfCdImportBak:importData")
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
        AjaxResult ajaxResult = iHalfCdImportBakService.importData(context, true);
        return ajaxResult;
    }

    /**
     * 导入线下模板调整
     *
     * @param file          文件
     * @param updateSupport 是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("cd15:halfCdImportBak:import4OfflineTemplate")
    @PostMapping("/import4OfflineTemplate")
    @ResponseBody
    @ApiOperation("导入线下模板调整")
    public AjaxResult import4OfflineTemplate(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iHalfCdImportBakService.import4OfflineTemplate(context);
        return ajaxResult;
    }
}
