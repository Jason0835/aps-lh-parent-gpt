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
import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialRecord;

import com.zlt.aps.monthplan.api.service.IRawSpecialMaterialRecordRemoteService;

import java.math.BigDecimal;
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
 * 文件名称：RawSpecialMaterialRecordUIController.java
 * 描    述：特殊材料清单 UI控制层类：....
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
@Api(tags = "特殊材料清单")
@Controller
@RequestMapping("/maindata/rawSpecialMaterialRecord")
public class RawSpecialMaterialRecordUIController extends BaseUIController<RawSpecialMaterialRecord> {

    @Autowired
    private IRawSpecialMaterialRecordRemoteService iRawSpecialMaterialRecordService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("maindata:rawSpecialMaterialRecord:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(RawSpecialMaterialRecord rawSpecialMaterialRecord) {
        return iRawSpecialMaterialRecordService.list(rawSpecialMaterialRecord);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("maindata:rawSpecialMaterialRecord:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(RawSpecialMaterialRecord rawSpecialMaterialRecord) {
        if (UserConstants.NOT_UNIQUE.equals(iRawSpecialMaterialRecordService.checkUnique(rawSpecialMaterialRecord))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.rawSpecialMaterialRecord.checkUnique"));
        }

        if (rawSpecialMaterialRecord.getQuota() != null && rawSpecialMaterialRecord.getQuota().compareTo(BigDecimal.valueOf(99999999.99)) > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.rawSpecialMaterialRecord.quota.max"));
        }

        return iRawSpecialMaterialRecordService.save(rawSpecialMaterialRecord);
    }

    /**
     * 删除特殊材料清单
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("maindata:rawSpecialMaterialRecord:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iRawSpecialMaterialRecordService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验特殊材料清单唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(RawSpecialMaterialRecord rawSpecialMaterialRecord) {
        return iRawSpecialMaterialRecordService.checkUnique(rawSpecialMaterialRecord);
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
        return I18nUtil.getMessage("ui.data.column.rawSpecialMaterialRecord.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<RawSpecialMaterialRecord> util = new ExcelUtil<>(RawSpecialMaterialRecord.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, RawSpecialMaterialRecord entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iRawSpecialMaterialRecordService.exportData(entity,fileName);
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
        AjaxResult ajaxResult = iRawSpecialMaterialRecordService.importData(context,true);
        return ajaxResult;
    }
}
