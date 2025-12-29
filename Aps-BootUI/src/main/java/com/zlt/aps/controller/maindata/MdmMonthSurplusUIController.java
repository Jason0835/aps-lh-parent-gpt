package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.monthplan.api.service.IMdmMonthSurplusRemoteService;
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
 * 文件名称：MdmMonthSurplusUIController.java
 * 描    述：0140基础数据_月底计划余量 UI控制层类：....
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
@Api(tags = "月底计划余量")
@Controller
@RequestMapping("/monthplan/mdmMonthSurplus")
public class MdmMonthSurplusUIController extends BaseUIController<MdmMonthSurplus> {

    private final String prefix = "aps/monthplan/mdmMonthSurplus";
    @Autowired
    private IMdmMonthSurplusRemoteService iMdmMonthSurplusService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:mdmMonthSurplus:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/mdmMonthSurplus";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mdmMonthSurplus", new MdmMonthSurplus());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mdmMonthSurplus", iMdmMonthSurplusService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:mdmMonthSurplus:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmMonthSurplus mdmMonthSurplus) {
        return iMdmMonthSurplusService.list(mdmMonthSurplus);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:mdmMonthSurplus:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MdmMonthSurplus mdmMonthSurplus) {
        if (UserConstants.NOT_UNIQUE.equals(iMdmMonthSurplusService.checkUnique(mdmMonthSurplus))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.mdmMonthSurplus.notUnique"));
        }

        return iMdmMonthSurplusService.save(mdmMonthSurplus);
    }

    /**
     * 删除0140基础数据_月底计划余量
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:mdmMonthSurplus:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmMonthSurplusService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验0140基础数据_月底计划余量唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MdmMonthSurplus mdmMonthSurplus) {
        return iMdmMonthSurplusService.checkUnique(mdmMonthSurplus);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     */
    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    /**
     * 继承时重写方法。
     */
    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 继承时重写方法。
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.mdmMonthSurplus.modelName");
    }

    /**
     * 下载导入模板
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmMonthSurplus> util = new ExcelUtil<>(MdmMonthSurplus.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导出
     *
     * @param response 响应
     * @param entity   查询条件
     * @throws IOException 异常
     */
    @RequiresPermissions("monthplan:mdmMonthSurplus:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmMonthSurplus entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMdmMonthSurplusService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 数据导入
     *
     * @param file          文件
     * @param updateSupport 是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("monthplan:mdmMonthSurplus:import")
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
        return iMdmMonthSurplusService.importData(context, updateSupport);
    }
}
