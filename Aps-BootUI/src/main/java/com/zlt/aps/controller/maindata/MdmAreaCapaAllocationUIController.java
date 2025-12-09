package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MdmAreaCapaAllocation;
import com.zlt.aps.monthplan.api.service.IMdmAreaCapaAllocationRemoteService;
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
 * 文件名称：MdmAreaCapaAllocationUIController.java
 * 描    述：区域产能分配 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@Slf4j
@Api(tags = "区域产能分配")
@Controller
@RequestMapping("/monthplan/mdmAreaCapaAllocation")
public class MdmAreaCapaAllocationUIController extends BaseUIController<MdmAreaCapaAllocation> {

    private final String prefix = "aps/monthplan/mdmAreaCapaAllocation";
    @Autowired
    private IMdmAreaCapaAllocationRemoteService iMdmAreaCapaAllocationService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:mdmAreaCapaAllocation:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/mdmAreaCapaAllocation";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mdmAreaCapaAllocation", new MdmAreaCapaAllocation());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mdmAreaCapaAllocation", iMdmAreaCapaAllocationService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:mdmAreaCapaAllocation:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmAreaCapaAllocation mdmAreaCapaAllocation) {
        return iMdmAreaCapaAllocationService.list(mdmAreaCapaAllocation);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:mdmAreaCapaAllocation:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MdmAreaCapaAllocation mdmAreaCapaAllocation) {
        if (UserConstants.NOT_UNIQUE.equals(iMdmAreaCapaAllocationService.checkUnique(mdmAreaCapaAllocation))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.mdmAreaCapaAllocation.notUnique"));
        }

        return iMdmAreaCapaAllocationService.save(mdmAreaCapaAllocation);
    }

    /**
     * 删除区域产能分配
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:mdmAreaCapaAllocation:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmAreaCapaAllocationService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验区域产能分配唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MdmAreaCapaAllocation mdmAreaCapaAllocation) {
        return iMdmAreaCapaAllocationService.checkUnique(mdmAreaCapaAllocation);
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
        return I18nUtil.getMessage("ui.data.column.mdmAreaCapaAllocation.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmAreaCapaAllocation> util = new ExcelUtil<>(MdmAreaCapaAllocation.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmAreaCapaAllocation entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMdmAreaCapaAllocationService.exportData(entity, fileName);
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
        AjaxResult ajaxResult = iMdmAreaCapaAllocationService.importData(context, false);
        return ajaxResult;
    }

    /**
     * 复制
     *
     * @param entity 参数
     * @return 结果
     */
    @PostMapping({"/copy"})
    @ApiOperation("复制")
    @ResponseBody
    public AjaxResult copy(MdmAreaCapaAllocation entity) {
        return iMdmAreaCapaAllocationService.copy(entity);
    }

    /**
     * 复制前校验
     *
     * @param entity 参数
     * @return 结果
     */
    @PostMapping({"/checkBeforeCopy"})
    @ApiOperation("复制前校验")
    @ResponseBody
    public AjaxResult checkBeforeCopy(MdmAreaCapaAllocation entity) {
        return iMdmAreaCapaAllocationService.checkBeforeCopy(entity);
    }
}
