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
import com.zlt.aps.monthplan.api.domain.entity.RawMaterialRequirePlan;

import com.zlt.aps.monthplan.api.service.IRawMaterialRequirePlanRemoteService;
import java.util.Arrays;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：RawMaterialRequirePlanUIController.java
 * 描    述：原材料需求计划 UI控制层类：....
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
@Api(tags = "原材料需求计划")
@Controller
@RequestMapping("/maindata/rawMaterialRequirePlan")
public class RawMaterialRequirePlanUIController extends BaseUIController<RawMaterialRequirePlan> {

    @Autowired
    private IRawMaterialRequirePlanRemoteService iRawMaterialRequirePlanService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("maindata:rawMaterialRequirePlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(RawMaterialRequirePlan rawMaterialRequirePlan) {
        return iRawMaterialRequirePlanService.list(rawMaterialRequirePlan);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("maindata:rawMaterialRequirePlan:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(RawMaterialRequirePlan rawMaterialRequirePlan) {
        if (UserConstants.NOT_UNIQUE.equals(iRawMaterialRequirePlanService.checkUnique(rawMaterialRequirePlan))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.rawMaterialRequirePlan.checkUnique"));
        }

        return iRawMaterialRequirePlanService.save(rawMaterialRequirePlan);
    }

    /**
     * 删除原材料需求计划
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("maindata:rawMaterialRequirePlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iRawMaterialRequirePlanService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验原材料需求计划唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(RawMaterialRequirePlan rawMaterialRequirePlan) {
        return iRawMaterialRequirePlanService.checkUnique(rawMaterialRequirePlan);
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
        return I18nUtil.getMessage("ui.data.column.rawMaterialRequirePlan.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<RawMaterialRequirePlan> util = new ExcelUtil<>(RawMaterialRequirePlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, RawMaterialRequirePlan entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iRawMaterialRequirePlanService.exportData(entity,fileName);
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
        AjaxResult ajaxResult = iRawMaterialRequirePlanService.importData(context,updateSupport);
        return ajaxResult;
    }


    @PostMapping("/generate")
    @ApiOperation("生成原材料需求计划")
    @ResponseBody
    public AjaxResult generate(@RequestParam("factoryCode") String factoryCode,
                               @RequestParam("year") Integer year,
                               @RequestParam("month") Integer month) {
        return iRawMaterialRequirePlanService.generate(factoryCode, year, month);
    }
}
