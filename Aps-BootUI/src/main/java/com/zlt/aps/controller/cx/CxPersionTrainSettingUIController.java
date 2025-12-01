package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxPersionTrainSetting;
import com.zlt.aps.cxlh.cx.api.service.ICxPersionTrainSettingRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.Logical;
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
 * 文件名称：CxPersionTrainSettingUIController.java
 * 描    述：成型工序开机档数 UI控制层类：....
 *@author zlt
 *@date 2025-03-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "成型工序开机档数")
@Controller
@RequestMapping("/cx/cxPersionTrainSetting")
public class CxPersionTrainSettingUIController extends BaseUIController<CxPersionTrainSetting> {

    @Autowired
    private ICxPersionTrainSettingRemoteService iCxPersionTrainSettingService;

    private final String prefix = "aps/cx/cxPersionTrainSetting";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:cxPersionTrainSetting:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/cxPersionTrainSetting";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxPersionTrainSetting", new CxPersionTrainSetting());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxPersionTrainSetting", iCxPersionTrainSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("cx:cxPersionTrainSetting:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxPersionTrainSetting cxPersionTrainSetting) {
        return iCxPersionTrainSettingService.list(cxPersionTrainSetting);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions(value = {"cx:cxPersionTrainSetting:edit", "cx:cxPersionTrainSetting:add"}, logical = Logical.OR)
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(CxPersionTrainSetting cxPersionTrainSetting) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(iCxPersionTrainSettingService.checkUnique(cxPersionTrainSetting))) {
            return ajaxResult.error(I18nUtil.getMessage("ui.data.column.cxPersionTrainSetting.checkUnique"));
        }

        return iCxPersionTrainSettingService.save(cxPersionTrainSetting);
    }

    /**
     * 批量修改或新增（页面上需要两条记录同时新增和编辑）
     */
    @ApiOperation("批量修改或新增")
    @RequiresPermissions(value = {"cx:cxPersionTrainSetting:edit", "cx:cxPersionTrainSetting:add"}, logical = Logical.OR)
    @PostMapping("/saveList")
    @ResponseBody
    public AjaxResult saveList(@RequestBody List<CxPersionTrainSetting> settingList) {
        return iCxPersionTrainSettingService.saveList(settingList);
    }

    /**
     * 删除成型工序开机档数
     */
    @ApiOperation("删除,id不为空）")
    @RequiresPermissions("cx:cxPersionTrainSetting:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxPersionTrainSettingService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验成型工序开机档数唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(CxPersionTrainSetting cxPersionTrainSetting) {
        return iCxPersionTrainSettingService.checkUnique(cxPersionTrainSetting);
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
    public void export(HttpServletResponse response, CxPersionTrainSetting entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iCxPersionTrainSettingService.exportData(entity,fileName);
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
        AjaxResult ajaxResult = iCxPersionTrainSettingService.importData(context,false);
        return ajaxResult;
    }
}
