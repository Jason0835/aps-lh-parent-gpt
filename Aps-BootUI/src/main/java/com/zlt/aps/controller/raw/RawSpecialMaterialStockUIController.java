package com.zlt.aps.controller.raw;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;

import lombok.extern.slf4j.Slf4j;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import org.apache.commons.io.IOUtils;
import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialStock;

import com.zlt.aps.monthplan.api.service.IRawSpecialMaterialStockRemoteService;
import java.util.Arrays;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：RawSpecialMaterialStockUIController.java
 * 描    述：特殊材料库存 UI控制层类：....
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "特殊材料库存")
@Controller
@RequestMapping("/maindata/rawSpecialMaterialStock")
public class RawSpecialMaterialStockUIController extends BaseUIController<RawSpecialMaterialStock> {

    @Autowired
    private IRawSpecialMaterialStockRemoteService iRawSpecialMaterialStockService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("maindata:rawSpecialMaterialStock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(RawSpecialMaterialStock rawSpecialMaterialStock) {
        return iRawSpecialMaterialStockService.list(rawSpecialMaterialStock);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("maindata:rawSpecialMaterialStock:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(RawSpecialMaterialStock rawSpecialMaterialStock) {
        if (UserConstants.NOT_UNIQUE.equals(iRawSpecialMaterialStockService.checkUnique(rawSpecialMaterialStock))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.rawSpecialMaterialStock.checkUnique"));
        }

        return iRawSpecialMaterialStockService.save(rawSpecialMaterialStock);
    }

    /**
     * 删除特殊材料库存
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("maindata:rawSpecialMaterialStock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iRawSpecialMaterialStockService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验特殊材料库存唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(RawSpecialMaterialStock rawSpecialMaterialStock) {
        return iRawSpecialMaterialStockService.checkUnique(rawSpecialMaterialStock);
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
        return I18nUtil.getMessage("ui.data.column.rawSpecialMaterialStock.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<RawSpecialMaterialStock> util = new ExcelUtil<>(RawSpecialMaterialStock.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, RawSpecialMaterialStock entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iRawSpecialMaterialStockService.exportData(entity,fileName);
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
        AjaxResult ajaxResult = iRawSpecialMaterialStockService.importData(context,updateSupport);
        return ajaxResult;
    }
}
