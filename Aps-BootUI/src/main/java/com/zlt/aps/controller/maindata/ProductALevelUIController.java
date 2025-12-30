package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.ProductALevel;
import com.zlt.aps.monthplan.api.service.IProductALevelRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.framework.utils.AuthorizationUtils;
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
 * 文件名称：ProductALevelUIController.java
 * 描    述：基础数据-SAP-OEE率 UI控制层类：....
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
@RequestMapping("/mdm/productALevel")
@Api(tags = "SAP代码OEE率配置")
public class ProductALevelUIController extends BaseUIController<ProductALevel> {

    private final IProductALevelRemoteService iProductALevelService;

    public ProductALevelUIController(IProductALevelRemoteService iProductALevelService) {
        this.iProductALevelService = iProductALevelService;
    }

    private final String prefix = "aps/monthplan/ProductALevel";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:ProductALevel:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/ProductALevel";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("productALevel", new ProductALevel());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("productALevel", iProductALevelService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:ProductALevel:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ProductALevel productALevel) {
        return iProductALevelService.list(productALevel);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:ProductALevel:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(ProductALevel productALevel) {
        if (UserConstants.NOT_UNIQUE.equals(iProductALevelService.checkUnique(productALevel))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.productALevel.checkUnique"));
        }

        return iProductALevelService.save(productALevel);
    }

    /**
     * 删除基础数据-SAP-OEE率
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:ProductALevel:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iProductALevelService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验基础数据-SAP-OEE率唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(ProductALevel productALevel) {
        return iProductALevelService.checkUnique(productALevel);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.docProductALevel.modelName");
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
        return I18nUtil.getMessage("ui.data.column.docProductALevel.modelName");
    }

    @Override
    protected Long getUserId() {
        return AuthorizationUtils.getUserId();
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<ProductALevel> util = new ExcelUtil<>(ProductALevel.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, ProductALevel entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iProductALevelService.exportData(entity, fileName);
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
        AjaxResult ajaxResult = iProductALevelService.importData(context, updateSupport);
        return ajaxResult;
    }

    /**
     * 不备货
     * @param ids 集合
     * @param year 年
     * @param month 月
     * @return 结果
     */
    @RequiresPermissions("monthplan:ProductALevel:noStockUp")
    @ApiOperation("不备货")
    @PostMapping("/noStockUp")
    @ResponseBody
    public AjaxResult noStockUp(String ids, Integer year, Integer month) {
        Long[] arr = Convert.toLongArray(ids);
        return iProductALevelService.noStockUp(Arrays.asList(arr), year, month);
    }
}
