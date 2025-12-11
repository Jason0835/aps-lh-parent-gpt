package com.zlt.aps.controller.maindata;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
import com.zlt.aps.monthplan.api.service.IMpMonthlySaleQtyRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.ruoyi.common4ui.exception.base.BaseException;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import org.apache.commons.io.IOUtils;

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
 * 文件名称：MpMonthlySaleQtyUIController.java
 * 描    述：月均销量 UI控制层类：....
 *@author yelq
 *@date 2025-12-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Api(tags = "月均销量")
@Controller
@RequestMapping("/monthplan/monthlySaleQty")
public class MpMonthlySaleQtyUIController extends BaseUIController<MpMonthlySaleQty> {

    @Autowired
    private IMpMonthlySaleQtyRemoteService iMpMonthlySaleQtyService;

    private final String prefix = "monthplan/monthplan/monthlySaleQty";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:monthlySaleQty:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/monthlySaleQty";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mpMonthlySaleQty", new MpMonthlySaleQty());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mpMonthlySaleQty", iMpMonthlySaleQtyService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询月均销量列表
     */
    @ApiOperation("根据条件查询月均销量列表")
    @RequiresPermissions("monthplan:monthlySaleQty:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MpMonthlySaleQty entity) {
        return iMpMonthlySaleQtyService.list(entity);
    }

    /**
     * 修改或新增月均销量
     */
    @ApiOperation("修改或新增月均销量")
    @RequiresPermissions("monthplan:monthlySaleQty:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MpMonthlySaleQty mpMonthlySaleQty) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(iMpMonthlySaleQtyService.checkMpMonthlySaleQtyUnique(mpMonthlySaleQty))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mpMonthlySaleQty.checkUnique"));
        }
        if (mpMonthlySaleQty.getId() != null){
            ajaxResult = iMpMonthlySaleQtyService.edit(mpMonthlySaleQty);
        } else{
            ajaxResult = iMpMonthlySaleQtyService.add(mpMonthlySaleQty);
        }
        return ajaxResult;
    }

    /**
     * 删除月均销量
     */
    @ApiOperation("删除月均销量（id不为空）")
    @RequiresPermissions("monthplan:monthlySaleQty:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMpMonthlySaleQtyService.remove(arr);
    }

    /**
     * 校验月均销量唯一性
     */
    @ApiOperation("校验月均销量唯一性")
    @PostMapping("/checkMpMonthlySaleQtyUnique")
    @ResponseBody
    public String checkMpMonthlySaleQtyUnique(MpMonthlySaleQty mpMonthlySaleQty) {
        return iMpMonthlySaleQtyService.checkMpMonthlySaleQtyUnique(mpMonthlySaleQty);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
        throw new BaseException("没有定义导出模板的文件名");
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
    public void export(HttpServletResponse response, MpMonthlySaleQty entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMpMonthlySaleQtyService.exportData(entity,fileName);
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
        AjaxResult ajaxResult = iMpMonthlySaleQtyService.importData(context,false);
        return ajaxResult;
    }
}
