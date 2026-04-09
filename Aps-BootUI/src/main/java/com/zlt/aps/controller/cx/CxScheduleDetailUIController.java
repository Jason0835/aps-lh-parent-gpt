package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cx.api.domain.entity.CxScheduleDetail;
import com.zlt.aps.cx.api.service.ICxScheduleDetailRemoteService;
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
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxScheduleDetailUIController.java
 * 描    述：成型排程明细 UI控制层类
 * @author APS Team
 * @date 2026-04-09
 * @version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@Slf4j
@Api(tags = "成型排程明细管理")
@Controller
@RequestMapping("/cx/cxScheduleDetail")
public class CxScheduleDetailUIController extends BaseUIController<CxScheduleDetail> {

    @Autowired
    private ICxScheduleDetailRemoteService iCxScheduleDetailService;

    private final String prefix = "aps/cx/cxScheduleDetail";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:cxScheduleDetail:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/cxScheduleDetail";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxScheduleDetail", new CxScheduleDetail());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxScheduleDetail", iCxScheduleDetailService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("cx:cxScheduleDetail:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxScheduleDetail cxScheduleDetail) {
        return iCxScheduleDetailService.list(cxScheduleDetail);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("cx:cxScheduleDetail:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(CxScheduleDetail cxScheduleDetail) {
        if (UserConstants.NOT_UNIQUE.equals(iCxScheduleDetailService.checkUnique(cxScheduleDetail))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.cxScheduleDetail.notUnique"));
        }

        return iCxScheduleDetailService.save(cxScheduleDetail);
    }

    /**
     * 删除成型排程明细
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("cx:cxScheduleDetail:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxScheduleDetailService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验成型排程明细唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(CxScheduleDetail cxScheduleDetail) {
        return iCxScheduleDetailService.checkUnique(cxScheduleDetail);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
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
        return I18nUtil.getMessage("ui.data.column.cxScheduleDetail.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<CxScheduleDetail> util = new ExcelUtil<>(CxScheduleDetail.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, CxScheduleDetail entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iCxScheduleDetailService.exportData(entity,fileName);
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
        AjaxResult ajaxResult = iCxScheduleDetailService.importData(context, updateSupport);
        return ajaxResult;
    }
}
