package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.ProductVulcanizingLimit;
import com.zlt.aps.monthplan.api.service.IProductVulcanizingLimitRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
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
 * 文件名称：ProductVulcanizingLimitUIController.java
 * 描    述：基础数据-品种限制硫化机 UI控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@Controller
@RequestMapping("/mdm/productVulcanizingLimit")
@Api(tags = "基础数据-品种限制硫化机")
public class ProductVulcanizingLimitUIController extends BaseUIController<ProductVulcanizingLimit> {

    private final IProductVulcanizingLimitRemoteService iProductVulcanizingLimitService;

    public ProductVulcanizingLimitUIController(IProductVulcanizingLimitRemoteService iProductVulcanizingLimitService) {
        this.iProductVulcanizingLimitService = iProductVulcanizingLimitService;
    }

    private final String prefix = "aps/monthplan/ProductVulcanizingLimit";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:ProductVulcanizingLimit:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/ProductVulcanizingLimit";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("productVulcanizingLimit", new ProductVulcanizingLimit());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("productVulcanizingLimit", iProductVulcanizingLimitService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:ProductVulcanizingLimit:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ProductVulcanizingLimit productVulcanizingLimit) {
        return iProductVulcanizingLimitService.list(productVulcanizingLimit);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:ProductVulcanizingLimit:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(ProductVulcanizingLimit productVulcanizingLimit) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(iProductVulcanizingLimitService.checkUnique(productVulcanizingLimit))) {
            return ajaxResult.error(I18nUtil.getMessage("ui.data.column.productVulcanizingLimit.checkUnique"));
        }

        return iProductVulcanizingLimitService.save(productVulcanizingLimit);
    }

    /**
     * 删除基础数据-品种限制硫化机
     */
    @ApiOperation("删除,id不为空）")
    @RequiresPermissions("monthplan:ProductVulcanizingLimit:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iProductVulcanizingLimitService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验基础数据-品种限制硫化机唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(ProductVulcanizingLimit productVulcanizingLimit) {
        return iProductVulcanizingLimitService.checkUnique(productVulcanizingLimit);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
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
    public void export(HttpServletResponse response, ProductVulcanizingLimit entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iProductVulcanizingLimitService.exportData(entity, fileName);
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
        AjaxResult ajaxResult = iProductVulcanizingLimitService.importData(context, updateSupport);
        return ajaxResult;
    }
}
