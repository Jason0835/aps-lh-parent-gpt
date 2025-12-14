package com.zlt.aps.controller.maindata;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MdmCapsuleChuck;
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
import java.util.List;
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
 * 文件名称：MdmCapsuleChuckUIController.java
 * 描    述：胶囊卡盘台账 UI控制层类：....
 *@author zlt
 *@date 2025-12-12
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "胶囊卡盘台账")
@Controller
@RequestMapping("/monthplan/mdmCapsuleChuck")
public class MdmCapsuleChuckUIController extends BaseUIController<MdmCapsuleChuck> {

    @Autowired
    private IMdmCapsuleChuckRemoteService iMdmCapsuleChuckService;

    private final String prefix = "aps/monthplan/mdmCapsuleChuck";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:mdmCapsuleChuck:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/mdmCapsuleChuck";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mdmCapsuleChuck", new MdmCapsuleChuck());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mdmCapsuleChuck", iMdmCapsuleChuckService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:mdmCapsuleChuck:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmCapsuleChuck mdmCapsuleChuck) {
        return iMdmCapsuleChuckService.list(mdmCapsuleChuck);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:mdmCapsuleChuck:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MdmCapsuleChuck mdmCapsuleChuck) {
        if (UserConstants.NOT_UNIQUE.equals(iMdmCapsuleChuckService.checkUnique(mdmCapsuleChuck))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mdmCapsuleChuck.checkUnique"));
        }

        return iMdmCapsuleChuckService.save(mdmCapsuleChuck);
    }

    /**
     * 删除胶囊卡盘台账
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:mdmCapsuleChuck:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmCapsuleChuckService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验胶囊卡盘台账唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MdmCapsuleChuck mdmCapsuleChuck) {
        return iMdmCapsuleChuckService.checkUnique(mdmCapsuleChuck);
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
        return I18nUtil.getMessage("ui.no.export.sheetName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmCapsuleChuck> util = new ExcelUtil<>(MdmCapsuleChuck.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("monthplan:mdmCapsuleChuck:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmCapsuleChuck entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMdmCapsuleChuckService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @RequiresPermissions("monthplan:mdmCapsuleChuck:import")
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
        AjaxResult ajaxResult = iMdmCapsuleChuckService.importData(context,false);
        return ajaxResult;
    }
}
