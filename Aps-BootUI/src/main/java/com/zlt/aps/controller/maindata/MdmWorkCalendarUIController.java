package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.mp.api.service.IMdmWorkCalendarRemoteService;
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
 * 文件名称：MdmWorkCalendarUIController.java
 * 描    述：工作日历 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-03
 */
@Slf4j
@Api(tags = "工作日历")
@Controller
@RequestMapping("/maindata/mdmWorkCalendar")
public class MdmWorkCalendarUIController extends BaseUIController<MdmWorkCalendar> {

    private final String prefix = "aps/maindata/mdmWorkCalendar";
    @Autowired
    private IMdmWorkCalendarRemoteService iMdmWorkCalendarService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("maindata:mdmWorkCalendar:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/mdmWorkCalendar";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mdmWorkCalendar", new MdmWorkCalendar());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mdmWorkCalendar", iMdmWorkCalendarService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("maindata:mdmWorkCalendar:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmWorkCalendar mdmWorkCalendar) {
        return iMdmWorkCalendarService.list(mdmWorkCalendar);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("maindata:mdmWorkCalendar:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MdmWorkCalendar mdmWorkCalendar) {
        if (UserConstants.NOT_UNIQUE.equals(iMdmWorkCalendarService.checkUnique(mdmWorkCalendar))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mdmWorkCalendar.checkUnique"));
        }

        return iMdmWorkCalendarService.save(mdmWorkCalendar);
    }

    /**
     * 删除工作日历
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("maindata:mdmWorkCalendar:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmWorkCalendarService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验工作日历唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MdmWorkCalendar mdmWorkCalendar) {
        return iMdmWorkCalendarService.checkUnique(mdmWorkCalendar);
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
        return I18nUtil.getMessage("ui.data.column.mdmWorkCalendar.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmWorkCalendar> util = new ExcelUtil<>(MdmWorkCalendar.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("monthplan:mdmWorkCalendar:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmWorkCalendar entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMdmWorkCalendarService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @RequiresPermissions("monthplan:mdmWorkCalendar:import")
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
        AjaxResult ajaxResult = iMdmWorkCalendarService.importData(context, updateSupport);
        return ajaxResult;
    }

    /**
     * 根据用户名称过滤出可查看的工序列表
     *
     * @return 结果
     */
    @ApiOperation("根据用户名称过滤出可查看的工序列表")
    @PostMapping("/selectProcCodeList")
    @ResponseBody
    public AjaxResult selectProcCodeList() {
        return iMdmWorkCalendarService.selectProcCodeList("");
    }

    /**
     * 生成全年工作日历
     *
     * @param entity 条件
     * @return 结果
     */
    @RequiresPermissions("maindata:mdmWorkCalendar:genAnnualPlan")
    @ApiOperation("生成全年工作日历")
    @PostMapping("/genAnnualPlan")
    @ResponseBody
    public AjaxResult genAnnualPlan(MdmWorkCalendar entity) {
        return iMdmWorkCalendarService.genAnnualPlan(entity);
    }
}
