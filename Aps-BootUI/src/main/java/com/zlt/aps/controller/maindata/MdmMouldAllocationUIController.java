package com.zlt.aps.controller.maindata;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.MdmMouldAllocation;
import com.zlt.aps.mp.api.domain.vo.PeriodInfo;
import com.zlt.aps.mp.api.service.IMdmMouldAllocationRemoteService;
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
 * 文件名称：MdmMouldAllocationUIController.java
 * 描    述：模具分配比例(同结构/不同结构) UI控制层类：....
 *@author zlt
 *@date 2025-12-14
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "模具分配比例(同结构/不同结构)")
@Controller
@RequestMapping("/monthplan/mdmMouldAllocation")
public class MdmMouldAllocationUIController extends BaseUIController<MdmMouldAllocation> {

    @Autowired
    private IMdmMouldAllocationRemoteService iMdmMouldAllocationService;

    private final String prefix = "aps/monthplan/mdmMouldAllocation";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:mdmMouldAllocation:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/mdmMouldAllocation";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mdmMouldAllocation", new MdmMouldAllocation());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mdmMouldAllocation", iMdmMouldAllocationService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:mdmMouldAllocation:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmMouldAllocation mdmMouldAllocation) {
        return iMdmMouldAllocationService.list(mdmMouldAllocation);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:mdmMouldAllocation:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MdmMouldAllocation mdmMouldAllocation) {
        if (UserConstants.NOT_UNIQUE.equals(iMdmMouldAllocationService.checkUnique(mdmMouldAllocation))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mdmMouldAllocation.checkUnique"));
        }

        return iMdmMouldAllocationService.save(mdmMouldAllocation);
    }

    /**
     * 删除模具分配比例(同结构/不同结构)
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:mdmMouldAllocation:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmMouldAllocationService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验模具分配比例(同结构/不同结构)唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MdmMouldAllocation mdmMouldAllocation) {
        return iMdmMouldAllocationService.checkUnique(mdmMouldAllocation);
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
        return I18nUtil.getMessage("ui.data.column.mdmMouldAllocation.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmMouldAllocation> util = new ExcelUtil<>(MdmMouldAllocation.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("monthplan:mdmMouldAllocation:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmMouldAllocation entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMdmMouldAllocationService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @RequiresPermissions("monthplan:mdmMouldAllocation:import")
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
        AjaxResult ajaxResult = iMdmMouldAllocationService.importData(context,updateSupport);
        return ajaxResult;
    }

    /**
     * 复制模具分配比例
     */
    @RequiresPermissions("monthplan:mdmMouldAllocation:copy")
    @ApiOperation("复制模具分配比例")
    @PostMapping("/copy")
    @ResponseBody
    public AjaxResult copy(PeriodInfo vo) {
        return iMdmMouldAllocationService.copy(vo);
    }


}
