package com.zlt.aps.controller.cx;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cxlh.cx.api.service.ICxEmbryoMonthPlanSurplusRemoteService;
import com.zlt.aps.monthplan.api.domain.entity.CxEmbryoMonthPlanSurplus;
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
 * 文件名称：CxEmbryoMonthPlanSurplusUIController.java
 * 描    述：成型工序胎胚计划量汇总表 UI控制层类：....
 *@author zlt
 *@date 2025-03-07
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "成型工序胎胚计划量汇总表")
@Controller
@RequestMapping("/cx/cxEmbryoMonthPlanSurplus")
public class CxEmbryoMonthPlanSurplusUIController extends BaseUIController<CxEmbryoMonthPlanSurplus> {

    @Autowired
    private ICxEmbryoMonthPlanSurplusRemoteService iCxEmbryoMonthPlanSurplusService;

    private final String prefix = "aps/cx/cxEmbryoMonthPlanSurplus";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:cxEmbryoMonthPlanSurplus:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/cxEmbryoMonthPlanSurplus";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxEmbryoMonthPlanSurplus", new CxEmbryoMonthPlanSurplus());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxEmbryoMonthPlanSurplus", iCxEmbryoMonthPlanSurplusService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("cx:cxEmbryoMonthPlanSurplus:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxEmbryoMonthPlanSurplus cxEmbryoMonthPlanSurplus) {
        return iCxEmbryoMonthPlanSurplusService.list(cxEmbryoMonthPlanSurplus);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("cx:cxEmbryoMonthPlanSurplus:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(CxEmbryoMonthPlanSurplus cxEmbryoMonthPlanSurplus) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(iCxEmbryoMonthPlanSurplusService.checkUnique(cxEmbryoMonthPlanSurplus))) {
            return ajaxResult.error(I18nUtil.getMessage("ui.data.column.cxEmbryoMonthPlanSurplus.checkUnique"));
        }

        return iCxEmbryoMonthPlanSurplusService.save(cxEmbryoMonthPlanSurplus);
    }

    /**
     * 删除成型工序胎胚计划量汇总表
     */
    @ApiOperation("删除,id不为空）")
    @RequiresPermissions("cx:cxEmbryoMonthPlanSurplus:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxEmbryoMonthPlanSurplusService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验成型工序胎胚计划量汇总表唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(CxEmbryoMonthPlanSurplus cxEmbryoMonthPlanSurplus) {
        return iCxEmbryoMonthPlanSurplusService.checkUnique(cxEmbryoMonthPlanSurplus);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
        throw new ServiceException("没有定义导出模板的文件名");
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
        return I18nUtil.getMessage("ui.no.export.sheetName");
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, CxEmbryoMonthPlanSurplus entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iCxEmbryoMonthPlanSurplusService.exportData(entity,fileName);
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
        AjaxResult ajaxResult = iCxEmbryoMonthPlanSurplusService.importData(context,false);
        return ajaxResult;
    }
}
